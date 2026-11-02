package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.CreditNoteLine;
import mz.multicore.erp.modules.comercial.model.CreditNoteReason;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gestor transacional do ciclo de vida das notas de crédito (aprovação com retorno de stock, rejeição e cancelamento).
 */
@Service
public class CreditNoteLifecycleService {

    private final CreditNoteRepository creditNoteRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final CreditNoteQueryService queryService;

    @Autowired
    public CreditNoteLifecycleService(
            CreditNoteRepository creditNoteRepository,
            InventoryService inventoryService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher,
            CreditNoteQueryService queryService
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.queryService = queryService;
    }

    @Transactional
    public CreditNoteDTO approve(Long id) {
        PermissionGuard.requireManagerOrAdmin("aprovar nota de crédito");
        CreditNote note = creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));

        if (note.getStatus() != NoteStatus.PENDING_APPROVAL && note.getStatus() != NoteStatus.DRAFT) {
            throw new BusinessRuleException("Apenas notas em rascunho ou pendentes podem ser aprovadas.");
        }
        CommercialNoteRules.requireValidInvoice(note.getInvoice());

        if (note.getReason() == CreditNoteReason.RETURN) {
            returnStockForApprovedNote(note);
        }

        note.setStatus(NoteStatus.APPROVED);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        CreditNote saved = creditNoteRepository.save(note);
        if (saved.getInvoice() != null) saved.getInvoice().applyNoteAdjustment(saved.getTotalAmount().negate());
        BigDecimal returnedCost = saved.getReason() == CreditNoteReason.RETURN
                ? saved.getLines().stream()
                    .map(line -> line.getInvoiceLine() == null || line.getInvoiceLine().getUnitCost() == null
                            ? BigDecimal.ZERO
                            : line.getInvoiceLine().getUnitCost().multiply(line.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new CreditNoteApprovedEvent(
                    saved.getCompany().getId(), saved.getId(), saved.getNoteNumber(),
                    saved.getIssueDate().toLocalDate(), saved.getTotalBeforeTax(),
                    saved.getTaxAmount(), saved.getTotalAmount(), returnedCost));
        }
        auditLogService.logCurrent("CREDIT_NOTE_APPROVE", "Nota de crédito " + saved.getNoteNumber() + " aprovada.");
        return queryService.toDTO(saved);
    }

    @Transactional
    public CreditNoteDTO reject(Long id, String rejectionReason) {
        PermissionGuard.requireManagerOrAdmin("rejeitar nota de crédito");
        CreditNote note = creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Uma nota já aprovada não pode ser rejeitada — emita uma nota de débito para reverter.");
        }
        CommercialNoteRules.requirePending(note.getStatus());
        note.setStatus(NoteStatus.REJECTED);
        note.setRejectionReason(rejectionReason);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.logCurrent("CREDIT_NOTE_REJECT",
                "Nota de crédito " + saved.getNoteNumber() + " rejeitada. Motivo: " + rejectionReason);
        return queryService.toDTO(saved);
    }

    @Transactional
    public CreditNoteDTO cancel(Long id) {
        PermissionGuard.requireManagerOrAdmin("cancelar nota de crédito");
        CreditNote note = creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser canceladas.");
        }
        CommercialNoteRules.requirePending(note.getStatus());
        note.setStatus(NoteStatus.CANCELLED);
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.logCurrent("CREDIT_NOTE_CANCEL", "Nota de crédito " + saved.getNoteNumber() + " cancelada.");
        return queryService.toDTO(saved);
    }

    private void returnStockForApprovedNote(CreditNote note) {
        Warehouse warehouse = note.getWarehouse();
        if (warehouse == null) {
            throw new BusinessRuleException("Não é possível devolver stock sem armazém definido na nota.");
        }
        String description = String.format("Devolução — Nota de Crédito %s (Fatura %s)",
                note.getNoteNumber(),
                note.getInvoice() != null ? note.getInvoice().getInvoiceNumber() : "—");

        for (CreditNoteLine line : note.getLines()) {
            inventoryService.registerMovement(
                    line.getProduct(),
                    warehouse,
                    line.getQuantity(),
                    "RETURN",
                    line.getBatchNumber(),
                    null,
                    description
            );
        }
    }
}
