package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.dto.InventoryCountDTO;
import mz.multicore.erp.modules.inventory.repository.InventoryCountRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Sessões de contagem de inventário: criar (com uma linha por artigo do armazém), guardar contagens
 * (rascunho retomável), aplicar (gera os ajustes de stock via {@link InventoryService} e fecha a
 * sessão) e listar/consultar/cancelar. MANAGER/ADMIN + auditoria + isolamento por empresa.
 */
@Service
public class InventoryCountService {

    private final InventoryCountLifecycleService lifecycleService;
    private final InventoryCountQueryService queryService;

    @Autowired
    public InventoryCountService(InventoryCountLifecycleService lifecycleService,
                                 InventoryCountQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    public InventoryCountService(InventoryCountRepository inventoryCountRepository,
                                 WarehouseRepository warehouseRepository,
                                 InventoryService inventoryService,
                                 AuditLogService auditLogService) {
        InventoryCountQueryService query = new InventoryCountQueryService(inventoryCountRepository);
        this.queryService = query;
        this.lifecycleService = new InventoryCountLifecycleService(
                inventoryCountRepository, warehouseRepository, inventoryService, auditLogService, query);
    }

    @Transactional
    public InventoryCountDTO createSession(Long warehouseId, String note) {
        PermissionGuard.requireManagerOrAdmin("iniciar contagem de inventário");
        return lifecycleService.createSession(warehouseId, note);
    }

    @Transactional
    public InventoryCountDTO saveCounts(Long sessionId, Map<Long, BigDecimal> countsByProduct) {
        PermissionGuard.requireManagerOrAdmin("guardar contagem de inventário");
        return lifecycleService.saveCounts(sessionId, countsByProduct);
    }

    @Transactional
    public InventoryCountDTO applySession(Long sessionId) {
        PermissionGuard.requireManagerOrAdmin("aplicar contagem de inventário");
        return lifecycleService.applySession(sessionId);
    }

    @Transactional(readOnly = true)
    public List<InventoryCountDTO> listSessions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.listSessions(companyId);
    }

    @Transactional(readOnly = true)
    public InventoryCountDTO getSession(Long sessionId) {
        return queryService.getSession(sessionId);
    }

    @Transactional
    public void cancelSession(Long sessionId) {
        PermissionGuard.requireManagerOrAdmin("cancelar contagem de inventário");
        lifecycleService.cancelSession(sessionId);
    }
}
