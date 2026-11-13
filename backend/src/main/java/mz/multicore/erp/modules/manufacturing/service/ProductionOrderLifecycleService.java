package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.architecture.events.ProductionCompletedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest;
import mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO;
import mz.multicore.erp.modules.manufacturing.model.*;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Gestor transacional do ciclo de vida das ordens de produção e cálculo do Custo Industrial de Produção (CIP).
 */
@Service
public class ProductionOrderLifecycleService {

    private final ProductionOrderRepository productionOrderRepository;
    private final BillOfMaterialsRepository bomRepository;
    private final CompanyLookup companyLookup;
    private final WarehouseLookup warehouseLookup;
    private final StockLookup stockLookup;
    private final ApplicationEventPublisher eventPublisher;
    private final mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService movementService;
    private final mz.multicore.erp.modules.numbering.service.DocumentNumberService numbers;

    @Autowired
    public ProductionOrderLifecycleService(
            ProductionOrderRepository productionOrderRepository,
            BillOfMaterialsRepository bomRepository,
            CompanyLookup companyLookup,
            WarehouseLookup warehouseLookup,
            StockLookup stockLookup,
            @Autowired(required = false) ApplicationEventPublisher eventPublisher,
            mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService movementService,
            @Autowired(required = false) mz.multicore.erp.modules.numbering.service.DocumentNumberService numbers) {
        this.numbers = numbers;
        this.movementService = movementService;
        this.productionOrderRepository = productionOrderRepository;
        this.bomRepository = bomRepository;
        this.companyLookup = companyLookup;
        this.warehouseLookup = warehouseLookup;
        this.stockLookup = stockLookup;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductionOrderDTO createProductionOrder(Long companyId, CreateProductionOrderRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");

        Company company = companyLookup.require(companyId);

        BillOfMaterials bom = bomRepository.findByIdAndCompanyId(request.bomId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Ficha técnica não encontrada."));

        Warehouse sourceWh = warehouseLookup.find(request.sourceWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de matérias-primas não encontrado."));

        Warehouse targetWh = warehouseLookup.find(request.targetWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de produto acabado não encontrado."));

        String orderNumber = numbers != null ? numbers.next(mz.multicore.erp.modules.numbering.service.DocumentSeries.PRODUCTION_ORDER)
                : "OP-" + System.currentTimeMillis();
        BigDecimal plannedQty = request.plannedQuantity();
        BigDecimal baseRatio = plannedQty.divide(bom.getBaseQuantity(), 6, RoundingMode.HALF_UP);

        ProductionOrder order = new ProductionOrder();
        order.setCompany(company);
        order.setBom(bom);
        order.setOrderNumber(orderNumber);
        order.setTargetProduct(bom.getFinishedProduct());
        order.setSourceWarehouse(sourceWh);
        order.setTargetWarehouse(targetWh);
        order.setPlannedQuantity(plannedQty);
        order.setProducedQuantity(BigDecimal.ZERO);
        order.setStatus(ProductionOrderStatus.PLANNED);
        order.setPlannedStartDate(LocalDate.now());
        order.setPlannedEndDate(request.plannedEndDate());

        BigDecimal estimatedRawMaterialsCost = BigDecimal.ZERO;
        for (BomItem item : bom.getItems()) {
            BigDecimal scrapMultiplier = BigDecimal.ONE.add(item.getScrapPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal lineQty = item.getRequiredQuantity().multiply(baseRatio).multiply(scrapMultiplier).setScale(4, RoundingMode.HALF_UP);

            BigDecimal componentCost = item.getRawMaterialProduct().getPurchasePrice() != null
                    ? item.getRawMaterialProduct().getPurchasePrice()
                    : BigDecimal.ZERO;
            BigDecimal lineTotal = componentCost.multiply(lineQty).setScale(2, RoundingMode.HALF_UP);

            ProductionOrderLine line = new ProductionOrderLine();
            line.setComponentProduct(item.getRawMaterialProduct());
            line.setPlannedQuantity(lineQty);
            line.setConsumedQuantity(BigDecimal.ZERO);
            line.setUnitCost(componentCost);
            line.setTotalCost(lineTotal);
            order.addLine(line);

            estimatedRawMaterialsCost = estimatedRawMaterialsCost.add(lineTotal);
        }

        BigDecimal totalEstimated = estimatedRawMaterialsCost
                .add(bom.getEstimatedLaborCost().multiply(baseRatio))
                .add(bom.getEstimatedOverheadCost().multiply(baseRatio))
                .setScale(2, RoundingMode.HALF_UP);

        order.setTotalProductionCost(totalEstimated);
        order.setUnitProductionCost(plannedQty.compareTo(BigDecimal.ZERO) > 0
                ? totalEstimated.divide(plannedQty, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        ProductionOrder saved = productionOrderRepository.save(order);
        return toOrderDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderDTO> listProductionOrders(Long companyId) {
        if (companyId == null) return List.of();
        return productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toOrderDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductionOrderDTO getProductionOrder(Long companyId, Long id) {
        ProductionOrder order = productionOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Ordem de produção não encontrada."));
        return toOrderDTO(order);
    }

    @Transactional
    public ProductionOrderDTO startProduction(Long companyId, Long id) {
        ProductionOrder order = productionOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Ordem de produção não encontrada."));

        if (order.getStatus() != ProductionOrderStatus.PLANNED) {
            throw new BusinessRuleException("Apenas ordens no estado PLANEADA podem ser iniciadas.");
        }

        order.setStatus(ProductionOrderStatus.IN_PROGRESS);
        order.setActualStartDate(LocalDate.now());
        return toOrderDTO(productionOrderRepository.save(order));
    }

    @Transactional
    public ProductionOrderDTO completeProduction(Long companyId, Long id, BigDecimal actualProducedQty) {
        ProductionOrder order = productionOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Ordem de produção não encontrada."));

        if (order.getStatus() != ProductionOrderStatus.IN_PROGRESS && order.getStatus() != ProductionOrderStatus.PLANNED) {
            throw new BusinessRuleException("A ordem de produção já foi concluída ou cancelada.");
        }

        BigDecimal qtyProduced = actualProducedQty != null && actualProducedQty.compareTo(BigDecimal.ZERO) > 0
                ? actualProducedQty
                : order.getPlannedQuantity();

        if (movementService == null) {
            throw new BusinessRuleException("Serviço de stock indisponível: a ordem de produção não pode ser concluída.");
        }
        Long sourceWhId = order.getSourceWarehouse() != null ? order.getSourceWarehouse().getId() : null;
        String description = "Ordem de produção " + order.getOrderNumber();
        ProductionShortageCheck.assertAvailable(stockLookup, order);
        BigDecimal actualRawMaterialCost = BigDecimal.ZERO;
        for (ProductionOrderLine line : order.getLines()) {
            line.setConsumedQuantity(line.getPlannedQuantity());
            BigDecimal lineCost = line.getUnitCost().multiply(line.getConsumedQuantity()).setScale(2, RoundingMode.HALF_UP);
            line.setTotalCost(lineCost);
            actualRawMaterialCost = actualRawMaterialCost.add(lineCost);
            if (sourceWhId != null) {
                movementService.registerMovement(line.getComponentProduct(), order.getSourceWarehouse(),
                        line.getConsumedQuantity().negate(), "PRODUCTION", null, null, description, null);
            }
        }

        if (order.getTargetWarehouse() != null) {
            movementService.registerMovement(order.getTargetProduct(), order.getTargetWarehouse(), qtyProduced, "PRODUCTION",
                    order.getOrderNumber(), null, description, null);
        }

        BigDecimal laborOverhead = order.getBom() != null
                ? order.getBom().getEstimatedLaborCost().add(order.getBom().getEstimatedOverheadCost())
                : BigDecimal.ZERO;
        BigDecimal totalActualCost = actualRawMaterialCost.add(laborOverhead).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitCost = qtyProduced.compareTo(BigDecimal.ZERO) > 0
                ? totalActualCost.divide(qtyProduced, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        order.setProducedQuantity(qtyProduced);
        order.setTotalProductionCost(totalActualCost);
        order.setUnitProductionCost(unitCost);
        order.setStatus(ProductionOrderStatus.COMPLETED);
        order.setCompletionDate(LocalDate.now());

        ProductionOrder saved = productionOrderRepository.save(order);
        if (eventPublisher != null && sourceWhId != null) {
            // The finished product is posted by ProductionCompletedEvent; this posts the raw material consumed.
            eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.InventoryValueChangedEvent(companyId,
                    mz.multicore.erp.modules.accounting.model.JournalSource.PRODUCTION_CONSUMPTION, saved.getId(),
                    saved.getOrderNumber(), saved.getCompletionDate(), actualRawMaterialCost, BigDecimal.ZERO, null));
        }
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ProductionCompletedEvent(
                    companyId,
                    saved.getId(),
                    saved.getOrderNumber(),
                    saved.getCompletionDate(),
                    saved.getTotalProductionCost()
            ));
        }

        return toOrderDTO(saved);
    }

    @Transactional
    public ProductionOrderDTO cancelProduction(Long companyId, Long id) {
        ProductionOrder order = productionOrderRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Ordem de produção não encontrada."));

        if (order.getStatus() == ProductionOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Não é possível cancelar uma ordem de produção já concluída.");
        }

        order.setStatus(ProductionOrderStatus.CANCELLED);
        return toOrderDTO(productionOrderRepository.save(order));
    }

    public ProductionOrderDTO toOrderDTO(ProductionOrder o) {
        return ProductionOrderMapper.toOrderDTO(o);
    }
}
