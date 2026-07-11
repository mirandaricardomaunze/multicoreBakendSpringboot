package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.inventory.dto.CreateStockAdjustmentRequest;
import com.phcpro.modules.inventory.dto.InventoryCountDTO;
import com.phcpro.modules.inventory.dto.InventoryCountLineDTO;
import com.phcpro.modules.inventory.model.InventoryCount;
import com.phcpro.modules.inventory.model.InventoryCountLine;
import com.phcpro.modules.inventory.model.InventoryCountStatus;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.InventoryCountRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sessões de contagem de inventário: criar (com uma linha por artigo do armazém), guardar contagens
 * (rascunho retomável), aplicar (gera os ajustes de stock via {@link InventoryService} e fecha a
 * sessão) e listar/consultar/cancelar. MANAGER/ADMIN + auditoria + isolamento por empresa.
 */
@Service
public class InventoryCountService {

    private final InventoryCountRepository inventoryCountRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public InventoryCountService(InventoryCountRepository inventoryCountRepository,
                                 WarehouseRepository warehouseRepository,
                                 InventoryService inventoryService,
                                 AuditLogService auditLogService) {
        this.inventoryCountRepository = inventoryCountRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
    }

    /** Inicia uma sessão DRAFT com uma linha (por contar) para cada artigo com stock no armazém. */
    @Transactional
    public InventoryCountDTO createSession(Long warehouseId, String note) {
        PermissionGuard.requireManagerOrAdmin("iniciar contagem de inventário");
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
        return toDetail(saved);
    }

    /** Guarda as contagens (por productId) numa sessão DRAFT — retomável mais tarde. */
    @Transactional
    public InventoryCountDTO saveCounts(Long sessionId, Map<Long, BigDecimal> countsByProduct) {
        PermissionGuard.requireManagerOrAdmin("guardar contagem de inventário");
        InventoryCount count = loadForCompany(sessionId);
        requireDraft(count);
        for (InventoryCountLine line : count.getLines()) {
            if (countsByProduct.containsKey(line.getProduct().getId())) {
                line.setCountedQuantity(countsByProduct.get(line.getProduct().getId()));
            }
        }
        inventoryCountRepository.save(count);
        auditLogService.logCurrent("INVENTORY_COUNT_SAVE", "Contagem de inventário #" + sessionId + " guardada.");
        return toDetail(count);
    }

    /** Aplica a sessão: cada linha contada gera um ajuste de stock; a sessão passa a APPLIED. */
    @Transactional
    public InventoryCountDTO applySession(Long sessionId) {
        PermissionGuard.requireManagerOrAdmin("aplicar contagem de inventário");
        InventoryCount count = loadForCompany(sessionId);
        requireDraft(count);
        Long companyId = count.getCompany().getId();
        Long warehouseId = count.getWarehouse().getId();

        Map<Long, BigDecimal> systemByProduct = new HashMap<>();
        for (Stock stock : inventoryService.getStocksByWarehouse(warehouseId)) {
            systemByProduct.put(stock.getProduct().getId(),
                    stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity());
        }
        for (InventoryCountLine line : count.getLines()) {
            if (line.getCountedQuantity() == null) continue; // não contado → não toca
            BigDecimal system = systemByProduct.getOrDefault(line.getProduct().getId(), BigDecimal.ZERO);
            line.setSystemQuantity(system);
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
        return toDetail(count);
    }

    /** Lista as sessões da empresa (sem linhas), mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<InventoryCountDTO> listSessions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return inventoryCountRepository.findByCompany_IdOrderByCreatedAtDesc(companyId)
                .stream().map(this::toSummary).toList();
    }

    /** Detalhe de uma sessão (com linhas). */
    @Transactional(readOnly = true)
    public InventoryCountDTO getSession(Long sessionId) {
        return toDetail(loadForCompany(sessionId));
    }

    /** Cancela uma sessão DRAFT (não toca no stock). */
    @Transactional
    public void cancelSession(Long sessionId) {
        PermissionGuard.requireManagerOrAdmin("cancelar contagem de inventário");
        InventoryCount count = loadForCompany(sessionId);
        requireDraft(count);
        count.setStatus(InventoryCountStatus.CANCELLED);
        inventoryCountRepository.save(count);
        auditLogService.logCurrent("INVENTORY_COUNT_CANCEL", "Contagem de inventário #" + sessionId + " cancelada.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InventoryCount loadForCompany(Long sessionId) {
        InventoryCount count = inventoryCountRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Contagem não encontrada."));
        Long companyId = count.getCompany() != null ? count.getCompany().getId() : null;
        CurrentUserContext.requireCompany(companyId);
        return count;
    }

    private static void requireDraft(InventoryCount count) {
        if (count.getStatus() != InventoryCountStatus.DRAFT) {
            throw new BusinessRuleException("Esta contagem já foi aplicada ou cancelada.");
        }
    }

    private InventoryCountDTO toDetail(InventoryCount count) {
        List<InventoryCountLineDTO> lines = count.getLines().stream()
                .map(l -> new InventoryCountLineDTO(l.getId(), l.getProduct().getId(),
                        l.getProduct().getSku(), l.getProduct().getName(),
                        l.getCountedQuantity(), l.getSystemQuantity()))
                .toList();
        return build(count, lines);
    }

    private InventoryCountDTO toSummary(InventoryCount count) {
        return build(count, List.of());
    }

    private InventoryCountDTO build(InventoryCount count, List<InventoryCountLineDTO> lines) {
        int total = count.getLines().size();
        int counted = (int) count.getLines().stream().filter(l -> l.getCountedQuantity() != null).count();
        return new InventoryCountDTO(
                count.getId(), count.getWarehouse().getId(), count.getWarehouse().getName(),
                count.getStatus().name(), count.getNote(), count.getCreatedBy(),
                count.getCreatedAt(), count.getAppliedAt(), total, counted, lines);
    }
}
