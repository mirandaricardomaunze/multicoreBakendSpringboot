package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.manufacturing.dto.*;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestrador transacional de produção industrial e fichas técnicas (BOM).
 * Delega o ciclo de vida das fichas técnicas a {@link BillOfMaterialsService} e das ordens a {@link ProductionOrderLifecycleService}.
 */
@Service
public class ProductionService {

    private final BillOfMaterialsService bomService;
    private final ProductionOrderLifecycleService orderService;

    @Autowired
    public ProductionService(BillOfMaterialsService bomService, ProductionOrderLifecycleService orderService) {
        this.bomService = bomService;
        this.orderService = orderService;
    }

    public ProductionService(BillOfMaterialsRepository bomRepository,
                             ProductionOrderRepository productionOrderRepository,
                             CompanyLookup companyLookup,
                             ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             StockRepository stockRepository) {
        this(bomRepository, productionOrderRepository, companyLookup, productRepository, warehouseRepository, stockRepository, null, null);
    }

    public ProductionService(BillOfMaterialsRepository bomRepository,
                             ProductionOrderRepository productionOrderRepository,
                             CompanyLookup companyLookup,
                             ProductRepository productRepository,
                             WarehouseRepository warehouseRepository,
                             StockRepository stockRepository,
                             ApplicationEventPublisher eventPublisher,
                             mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService movementService) {
        this(
                new BillOfMaterialsService(bomRepository, companyLookup, productRepository),
                new ProductionOrderLifecycleService(productionOrderRepository, bomRepository,
                        companyLookup, warehouseRepository, stockRepository, eventPublisher, movementService)
        );
    }

    @Transactional
    public BillOfMaterialsDTO createBom(Long companyId, CreateBomRequest request) {
        return bomService.createBom(companyId, request);
    }

    @Transactional(readOnly = true)
    public List<BillOfMaterialsDTO> listBoms(Long companyId) {
        return bomService.listBoms(companyId);
    }

    @Transactional(readOnly = true)
    public BillOfMaterialsDTO getBom(Long companyId, Long id) {
        return bomService.getBom(companyId, id);
    }

    @Transactional
    public ProductionOrderDTO createProductionOrder(Long companyId, CreateProductionOrderRequest request) {
        return orderService.createProductionOrder(companyId, request);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderDTO> listProductionOrders(Long companyId) {
        return orderService.listProductionOrders(companyId);
    }

    @Transactional(readOnly = true)
    public ProductionOrderDTO getProductionOrder(Long companyId, Long id) {
        return orderService.getProductionOrder(companyId, id);
    }

    @Transactional
    public ProductionOrderDTO startProduction(Long companyId, Long id) {
        return orderService.startProduction(companyId, id);
    }

    @Transactional
    public ProductionOrderDTO completeProduction(Long companyId, Long id, BigDecimal actualProducedQty) {
        return orderService.completeProduction(companyId, id, actualProducedQty);
    }

    @Transactional
    public ProductionOrderDTO cancelProduction(Long companyId, Long id) {
        return orderService.cancelProduction(companyId, id);
    }
}
