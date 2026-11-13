package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.inventory.dto.CreateInventorySessionRequest;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateInventoryItemCountRequest;
import mz.multicore.erp.modules.inventory.repository.InventoryPhysicalItemRepository;
import mz.multicore.erp.modules.inventory.repository.InventoryPhysicalSessionRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Orquestrador tenant-scoped de sessões de inventário físico e contagem de stock. */
@Service
public class InventoryPhysicalCountingService {

    private final InventoryCountingLifecycleService lifecycleService;
    private final InventoryCountingQueryService queryService;

    @Autowired
    public InventoryPhysicalCountingService(InventoryCountingLifecycleService lifecycleService,
                                            InventoryCountingQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public InventoryPhysicalCountingService(
            InventoryPhysicalSessionRepository sessionRepository,
            InventoryPhysicalItemRepository itemRepository,
            ProductLookup productLookup,
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService
    ) {
        InventoryCountingQueryService query = new InventoryCountingQueryService(sessionRepository, stockRepository);
        this.queryService = query;
        this.lifecycleService = new InventoryCountingLifecycleService(
                sessionRepository, itemRepository, productLookup, warehouseRepository, inventoryService, query);
    }

    @Transactional
    public InventorySessionDTO createSession(CreateInventorySessionRequest request, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return lifecycleService.createSession(request, companyId);
    }

    @Transactional
    public InventorySessionDTO startCounting(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return lifecycleService.startCounting(sessionId, companyId);
    }

    @Transactional
    public InventorySessionDTO recordCount(Long sessionId, UpdateInventoryItemCountRequest request, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return lifecycleService.recordCount(sessionId, request, companyId);
    }

    @Transactional
    public InventorySessionDTO closeAndAdjustStock(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return lifecycleService.closeAndAdjustStock(sessionId, companyId);
    }

    @Transactional
    public InventorySessionDTO cancelSession(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return lifecycleService.cancelSession(sessionId, companyId);
    }

    @Transactional(readOnly = true)
    public InventorySessionDTO getSessionById(Long sessionId, Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.getSessionById(sessionId, companyId);
    }

    @Transactional(readOnly = true)
    public List<InventorySessionDTO> listSessions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.listSessions(companyId);
    }
}
