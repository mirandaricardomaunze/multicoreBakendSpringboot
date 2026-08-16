package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.purchases.dto.GoodsReceiptDiscrepancyDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDiscrepancyDTO;
import mz.multicore.erp.modules.purchases.model.DiscrepancyType;
import mz.multicore.erp.modules.purchases.model.GoodsReceiptDiscrepancy;
import mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitura e resolução das divergências da conferência à chegada.
 *
 * <p>Serviço próprio: o {@code PurchaseOrderService} <b>regista</b> a ocorrência no acto da
 * recepção; quem a lê, agrega e fecha é este (SRP).
 */
@Service
public class GoodsReceiptDiscrepancyService {

    private final GoodsReceiptDiscrepancyRepository repository;
    private final AuditLogService auditLogService;

    public GoodsReceiptDiscrepancyService(GoodsReceiptDiscrepancyRepository repository,
                                          AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    /** Ocorrências de um período, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<GoodsReceiptDiscrepancyDTO> list(LocalDate from, LocalDate to) {
        return between(from, to).stream().map(GoodsReceiptDiscrepancyService::toDTO).toList();
    }

    /** Só o que ainda está por resolver com o fornecedor. */
    @Transactional(readOnly = true)
    public List<GoodsReceiptDiscrepancyDTO> listOpen() {
        return repository.findByCompanyIdAndResolvedFalseOrderByOccurredOnDesc(
                        CurrentUserContext.getCurrentCompanyId())
                .stream().map(GoodsReceiptDiscrepancyService::toDTO).toList();
    }

    /**
     * Resumo por fornecedor, do que mais custou para o que menos custou.
     *
     * <p>É esta ordenação que faz o trabalho: o fornecedor que aparece em cima é aquele com quem
     * vale a pena ter a conversa.
     */
    @Transactional(readOnly = true)
    public List<SupplierDiscrepancyDTO> summaryBySupplier(LocalDate from, LocalDate to) {
        Map<Long, Accumulator> bySupplier = new LinkedHashMap<>();
        for (GoodsReceiptDiscrepancy item : between(from, to)) {
            bySupplier.computeIfAbsent(item.getSupplierId(), id -> new Accumulator(id, item.getSupplierName()))
                    .add(item);
        }
        List<SupplierDiscrepancyDTO> summary = new ArrayList<>(bySupplier.values().stream()
                .map(Accumulator::toDTO).toList());
        summary.sort(Comparator.comparing(SupplierDiscrepancyDTO::totalAmount).reversed());
        return summary;
    }

    /** Fecha uma ocorrência: o fornecedor creditou, substituiu, ou perdoou-se. */
    @Transactional
    public GoodsReceiptDiscrepancyDTO resolve(Long id, String resolutionNotes) {
        PermissionGuard.requireManagerOrAdmin("resolver divergência de recepção");
        GoodsReceiptDiscrepancy item = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Divergência não encontrada."));
        CurrentUserContext.requireCompany(item.getCompany().getId());
        if (item.isResolved()) {
            throw new BusinessRuleException("Esta divergência já está resolvida.");
        }
        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            // Sem explicação, "resolvido" não vale nada daqui a seis meses.
            throw new BusinessRuleException("Indique como a divergência foi resolvida.");
        }
        item.setResolved(true);
        item.setResolutionNotes(resolutionNotes);
        GoodsReceiptDiscrepancy saved = repository.save(item);
        auditLogService.logCurrent("PURCHASE_DISCREPANCY_RESOLVE",
                "Divergência " + id + " (" + item.getSupplierName() + ") resolvida: " + resolutionNotes);
        return toDTO(saved);
    }

    private List<GoodsReceiptDiscrepancy> between(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Indique o período (data inicial e final).");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("A data final não pode ser anterior à data inicial.");
        }
        return repository.findByCompanyIdAndOccurredOnBetweenOrderByOccurredOnDesc(
                CurrentUserContext.getCurrentCompanyId(), from, to);
    }

    static GoodsReceiptDiscrepancyDTO toDTO(GoodsReceiptDiscrepancy item) {
        return new GoodsReceiptDiscrepancyDTO(
                item.getId(),
                item.getPurchaseOrder() == null ? null : item.getPurchaseOrder().getOrderNumber(),
                item.getSupplierName(),
                item.getProduct() == null ? null : item.getProduct().getName(),
                item.getType(),
                item.getType() == null ? null : item.getType().label(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.amount(),
                item.getNotes(),
                item.getOccurredOn(),
                item.isResolved(),
                item.getResolutionNotes());
    }

    /** Acumulador por fornecedor. */
    private static final class Accumulator {
        private final Long supplierId;
        private final String supplierName;
        private int occurrences;
        private BigDecimal damagedQty = BigDecimal.ZERO;
        private BigDecimal missingQty = BigDecimal.ZERO;
        private BigDecimal damagedAmount = BigDecimal.ZERO;
        private BigDecimal missingAmount = BigDecimal.ZERO;
        private BigDecimal openAmount = BigDecimal.ZERO;

        private Accumulator(Long supplierId, String supplierName) {
            this.supplierId = supplierId;
            this.supplierName = supplierName == null ? "(sem fornecedor)" : supplierName;
        }

        private void add(GoodsReceiptDiscrepancy item) {
            occurrences++;
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            if (item.getType() == DiscrepancyType.DAMAGED) {
                damagedQty = damagedQty.add(qty);
                damagedAmount = damagedAmount.add(item.amount());
            } else {
                missingQty = missingQty.add(qty);
                missingAmount = missingAmount.add(item.amount());
            }
            if (!item.isResolved()) {
                openAmount = openAmount.add(item.amount());
            }
        }

        private SupplierDiscrepancyDTO toDTO() {
            return new SupplierDiscrepancyDTO(supplierId, supplierName, occurrences,
                    damagedQty, missingQty, damagedAmount, missingAmount,
                    damagedAmount.add(missingAmount), openAmount);
        }
    }
}
