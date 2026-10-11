package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.manufacturing.dto.*;
import mz.multicore.erp.modules.manufacturing.model.*;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductionService {

    private final BillOfMaterialsRepository bomRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ProductionService(BillOfMaterialsRepository bomRepository,
                             ProductionOrderRepository productionOrderRepository,
                             CompanyRepository companyRepository,
                             ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             StockRepository stockRepository) {
        this(bomRepository, productionOrderRepository, companyRepository, productRepository, warehouseRepository, stockRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProductionService(BillOfMaterialsRepository bomRepository,
                             ProductionOrderRepository productionOrderRepository,
                             CompanyRepository companyRepository,
                             ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             StockRepository stockRepository,
                             org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.bomRepository = bomRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BillOfMaterialsDTO createBom(Long companyId, CreateBomRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        if (bomRepository.existsByCompanyIdAndBomCode(companyId, request.bomCode())) {
            throw new BusinessRuleException("Já existe uma ficha técnica registada com o código '" + request.bomCode() + "'.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        Product finishedProduct = productRepository.findById(request.finishedProductId())
                .orElseThrow(() -> new BusinessRuleException("Produto acabado não encontrado: " + request.finishedProductId()));

        BillOfMaterials bom = new BillOfMaterials();
        bom.setCompany(company);
        bom.setBomCode(request.bomCode().trim().toUpperCase());
        bom.setName(request.name().trim());
        bom.setFinishedProduct(finishedProduct);
        bom.setBaseQuantity(request.baseQuantity());
        bom.setBomType(request.bomType() != null ? request.bomType() : BomType.STANDARD);
        bom.setEstimatedLaborCost(request.estimatedLaborCost() != null ? request.estimatedLaborCost() : BigDecimal.ZERO);
        bom.setEstimatedOverheadCost(request.estimatedOverheadCost() != null ? request.estimatedOverheadCost() : BigDecimal.ZERO);
        bom.setActive(true);

        for (CreateBomRequest.CreateBomItemRequest itemReq : request.items()) {
            Product rawMaterial = productRepository.findById(itemReq.rawMaterialProductId())
                    .orElseThrow(() -> new BusinessRuleException("Matéria-prima não encontrada: " + itemReq.rawMaterialProductId()));

            BomItem item = new BomItem();
            item.setRawMaterialProduct(rawMaterial);
            item.setRequiredQuantity(itemReq.requiredQuantity());
            item.setScrapPercentage(itemReq.scrapPercentage() != null ? itemReq.scrapPercentage() : BigDecimal.ZERO);
            bom.addItem(item);
        }

        BillOfMaterials saved = bomRepository.save(bom);
        return toBomDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<BillOfMaterialsDTO> listBoms(Long companyId) {
        if (companyId == null) return List.of();
        return bomRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId)
                .stream()
                .map(this::toBomDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillOfMaterialsDTO getBom(Long companyId, Long id) {
        BillOfMaterials bom = bomRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Ficha técnica não encontrada."));
        return toBomDTO(bom);
    }

    @Transactional
    public ProductionOrderDTO createProductionOrder(Long companyId, CreateProductionOrderRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        BillOfMaterials bom = bomRepository.findByIdAndCompanyId(request.bomId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Ficha técnica não encontrada."));

        Warehouse sourceWh = warehouseRepository.findById(request.sourceWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de matérias-primas não encontrado."));

        Warehouse targetWh = warehouseRepository.findById(request.targetWarehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém de produto acabado não encontrado."));

        String orderNumber = "OP-" + System.currentTimeMillis() % 1000000;
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

        // 1. Dar baixa no stock de cada componente/matéria-prima no armazém de origem
        Long sourceWhId = order.getSourceWarehouse() != null ? order.getSourceWarehouse().getId() : null;
        BigDecimal actualRawMaterialCost = BigDecimal.ZERO;

        for (ProductionOrderLine line : order.getLines()) {
            line.setConsumedQuantity(line.getPlannedQuantity());
            BigDecimal lineCost = line.getUnitCost().multiply(line.getConsumedQuantity()).setScale(2, RoundingMode.HALF_UP);
            line.setTotalCost(lineCost);
            actualRawMaterialCost = actualRawMaterialCost.add(lineCost);

            if (sourceWhId != null) {
                Stock rawStock = stockRepository.findForReservation(line.getComponentProduct().getId(), sourceWhId)
                        .orElse(null);
                if (rawStock != null) {
                    BigDecimal newQty = rawStock.getQuantity().subtract(line.getConsumedQuantity());
                    rawStock.setQuantity(newQty.compareTo(BigDecimal.ZERO) >= 0 ? newQty : BigDecimal.ZERO);
                    stockRepository.save(rawStock);
                }
            }
        }

        // 2. Dar entrada de stock do produto acabado no armazém de destino
        Long targetWhId = order.getTargetWarehouse() != null ? order.getTargetWarehouse().getId() : null;
        if (targetWhId != null) {
            Stock finishedStock = stockRepository.findForReservation(order.getTargetProduct().getId(), targetWhId)
                    .orElseGet(() -> {
                        Stock s = new Stock();
                        s.setProduct(order.getTargetProduct());
                        s.setWarehouse(order.getTargetWarehouse());
                        s.setQuantity(BigDecimal.ZERO);
                        return s;
                    });
            finishedStock.setQuantity(finishedStock.getQuantity().add(qtyProduced));
            stockRepository.save(finishedStock);
        }

        // 3. Apuramento do Custo Industrial de Produção (CIP)
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
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.ProductionCompletedEvent(
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

    private BillOfMaterialsDTO toBomDTO(BillOfMaterials b) {
        List<BomItemDTO> itemDTOs = b.getItems().stream()
                .map(i -> {
                    BigDecimal unitCost = i.getRawMaterialProduct().getPurchasePrice() != null
                            ? i.getRawMaterialProduct().getPurchasePrice()
                            : BigDecimal.ZERO;
                    return new BomItemDTO(
                            i.getId(),
                            i.getRawMaterialProduct().getId(),
                            i.getRawMaterialProduct().getSku(),
                            i.getRawMaterialProduct().getName(),
                            i.getRequiredQuantity(),
                            i.getUnitOfMeasure() != null ? i.getUnitOfMeasure() : "UND",
                            i.getScrapPercentage(),
                            unitCost
                    );
                }).toList();

        BigDecimal rawTotal = itemDTOs.stream()
                .map(it -> it.estimatedUnitCost().multiply(it.requiredQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEst = rawTotal.add(b.getEstimatedLaborCost()).add(b.getEstimatedOverheadCost());

        return new BillOfMaterialsDTO(
                b.getId(),
                b.getCompany().getId(),
                b.getBomCode(),
                b.getName(),
                b.getFinishedProduct().getId(),
                b.getFinishedProduct().getSku(),
                b.getFinishedProduct().getName(),
                b.getBaseQuantity(),
                b.getBomType(),
                b.isActive(),
                b.getEstimatedLaborCost(),
                b.getEstimatedOverheadCost(),
                totalEst,
                itemDTOs
        );
    }

    private ProductionOrderDTO toOrderDTO(ProductionOrder o) {
        return new ProductionOrderDTO(
                o.getId(),
                o.getCompany().getId(),
                o.getOrderNumber(),
                o.getBom() != null ? o.getBom().getId() : null,
                o.getBom() != null ? o.getBom().getBomCode() : "—",
                o.getTargetProduct().getId(),
                o.getTargetProduct().getSku(),
                o.getTargetProduct().getName(),
                o.getPlannedQuantity(),
                o.getProducedQuantity(),
                o.getStatus(),
                o.getPlannedStartDate(),
                o.getPlannedEndDate(),
                o.getCompletionDate(),
                o.getSourceWarehouse() != null ? o.getSourceWarehouse().getId() : null,
                o.getTargetWarehouse() != null ? o.getTargetWarehouse().getId() : null,
                o.getTotalProductionCost(),
                o.getUnitProductionCost()
        );
    }
}
