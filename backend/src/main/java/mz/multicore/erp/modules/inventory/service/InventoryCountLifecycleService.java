package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.dto.InventoryCountDTO;
import mz.multicore.erp.modules.inventory.model.InventoryCount;
import mz.multicore.erp.modules.inventory.model.InventoryCountLine;
import mz.multicore.erp.modules.inventory.model.InventoryCountStatus;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.InventoryCountRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class InventoryCountLifecycleService {

    private final InventoryCountRepository inventoryCountRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final InventoryCountQueryService queryService;

    public InventoryCountLifecycleService(InventoryCountRepository inventoryCountRepository,
                                          WarehouseRepository warehouseRepository,
                                          InventoryService inventoryService,
                                          AuditLogService auditLogService,
                                          InventoryCountQueryService queryService) {
        this.inventoryCountRepository = inventoryCountRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
    }

    public InventoryCountDTO createSession(Long warehouseId, String note) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        Long companyId = warehouse.getCompany() != null ? warehouse.getCompany().getId() : null;
        CurrentUserContext.requireCompany(companyId);

        InventoryCount count = new InventoryCount();
        count.setCompany(warehouse.getCompany());
        count.setWarehouse(warehouse);
        count.setStatus(InventoryCountStatus.DRAFT);
        count.setNote(note);
        count.setCreatedBy(CurrentUserContext.getUsername());
        for (Stock stock : inventoryService.getStocksByWarehouse(warehouseId)) {
            InventoryCountLine line = new InventoryCountLine();
            line.setInventoryCount(count);
            line.setProduct(stock.getProduct());
            count.getLines().add(line);
        }
        InventoryCount saved = inventoryCountRepository.save(count);
        auditLogService.logCurrent("INVENTORY_COUNT_CREATE",
                "Contagem de inventário #" + saved.getId() + " iniciada em " + warehouse.getName() + ".");
        return queryService.toDetail(saved);
    }

    public InventoryCountDTO saveCounts(Long sessionId, Map<Long, BigDecimal> countsByProduct) {
        InventoryCount count = queryService.loadForCompanyForUpdate(sessionId);
        queryService.requireDraft(count);
        for (InventoryCountLine line : count.getLines()) {
            if (countsByProduct.containsKey(line.getProduct().getId())) {
                line.setCountedQuantity(countsByProduct.get(line.getProduct().getId()));
            }
        }
        inventoryCountRepository.save(count);
        auditLogService.logCurrent("INVENTORY_COUNT_SAVE", "Contagem de inventário #" + sessionId + " guardada.");
        return queryService.toDetail(count);
    }

    public InventoryCountDTO applySession(Long sessionId) {
        InventoryCount count = queryService.loadForCompanyForUpdate(sessionId);
        queryService.requireDraft(count);
        Long companyId = count.getCompany().getId();
        Long warehouseId = count.getWarehouse().getId();

        Map<Long, BigDecimal> systemByProduct = new HashMap<>();
        for (Stock stock : inventoryService.getStocksByWarehouse(warehouseId)) {
            systemByProduct.put(stock.getProduct().getId(),
                    stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity());
        }
        for (InventoryCountLine line : count.getLines()) {
            if (line.getCountedQuantity() == null) continue;
            BigDecimal system = systemByProduct.getOrDefault(line.getProduct().getId(), BigDecimal.ZERO);
            line.setSystemQuantity(system);
            if (line.getCountedQuantity().compareTo(system) == 0) {
                line.setApplied(true);
                continue;
            }
            inventoryService.adjustStock(new CreateStockAdjustmentRequest(
                    companyId, line.getProduct().getId(), warehouseId, line.getCountedQuantity(),
                    "Inventário físico (contagem #" + sessionId + ")"));
            line.setApplied(true);
        }
        count.setStatus(InventoryCountStatus.APPLIED);
        count.setAppliedAt(LocalDateTime.now());
        count.setAppliedBy(CurrentUserContext.getUsername());
        inventoryCountRepository.save(count);
        auditLogService.logCurrent("INVENTORY_COUNT_APPLY", "Contagem de inventário #" + sessionId + " aplicada.");
        return queryService.toDetail(count);
    }

    public void cancelSession(Long sessionId) {
        InventoryCount count = queryService.loadForCompanyForUpdate(sessionId);
        queryService.requireDraft(count);
        count.setStatus(InventoryCountStatus.CANCELLED);
        inventoryCountRepository.save(count);
        auditLogService.logCurrent("INVENTORY_COUNT_CANCEL", "Contagem de inventário #" + sessionId + " cancelada.");
    }
}
