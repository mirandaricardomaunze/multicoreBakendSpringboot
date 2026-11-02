package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.DebitNoteApprovedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateDebitNoteRequest;
import mz.multicore.erp.modules.comercial.dto.DebitNoteDTO;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DebitNoteReason;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DebitNoteLifecycleService {

    private final DebitNoteRepository debitNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final DebitNoteQueryService queryService;

    public DebitNoteLifecycleService(DebitNoteRepository debitNoteRepository,
                                     InvoiceRepository invoiceRepository,
                                     DocumentNumberService documentNumberService,
                                     AuditLogService auditLogService,
                                     ApplicationEventPublisher eventPublisher,
                                     DebitNoteQueryService queryService) {
        this.debitNoteRepository = debitNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.queryService = queryService;
    }

    public DebitNoteDTO create(CreateDebitNoteRequest request) {
        DebitNoteReason reason;
        try {
            reason = DebitNoteReason.valueOf(request.reason());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Motivo de nota de débito inválido: " + request.reason());
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        CommercialNoteRules.requireValidInvoice(invoice);
        DebitNote note = new DebitNote();
        note.setNoteNumber(documentNumberService.next(DocumentSeries.DEBIT_NOTE));
        note.setIssueDate(LocalDateTime.now());
        note.setCompany(invoice.getCompany());
        note.setClient(invoice.getClient());
        note.setInvoice(invoice);
        note.setReason(reason);
        note.setStatus(NoteStatus.PENDING_APPROVAL);
        note.setDescription(request.description());
        note.setCreatedBy(CurrentUserContext.getUsername());

        DebitNoteLineBuilder.addLines(note, request.lines());

        return queryService.toDTO(debitNoteRepository.save(note));
    }

    public DebitNoteDTO approve(Long id) {
        DebitNote note = debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() != NoteStatus.PENDING_APPROVAL && note.getStatus() != NoteStatus.DRAFT) {
            throw new BusinessRuleException("Apenas notas em rascunho ou pendentes podem ser aprovadas.");
        }
        CommercialNoteRules.requireValidInvoice(note.getInvoice());
        note.setStatus(NoteStatus.APPROVED);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        DebitNote saved = debitNoteRepository.save(note);
        if (saved.getInvoice() != null) saved.getInvoice().applyNoteAdjustment(saved.getTotalAmount());
        eventPublisher.publishEvent(new DebitNoteApprovedEvent(
                saved.getCompany().getId(), saved.getId(), saved.getNoteNumber(),
                saved.getIssueDate().toLocalDate(), saved.getTotalBeforeTax(),
                saved.getTaxAmount(), saved.getTotalAmount()));
        auditLogService.logCurrent("DEBIT_NOTE_APPROVE",
                "Nota de débito " + saved.getNoteNumber() + " aprovada.");
        return queryService.toDTO(saved);
    }

    public DebitNoteDTO reject(Long id, String rejectionReason) {
        DebitNote note = debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser rejeitadas.");
        }
        CommercialNoteRules.requirePending(note.getStatus());
        note.setStatus(NoteStatus.REJECTED);
        note.setRejectionReason(rejectionReason);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.logCurrent("DEBIT_NOTE_REJECT",
                "Nota de débito " + saved.getNoteNumber() + " rejeitada. Motivo: " + rejectionReason);
        return queryService.toDTO(saved);
    }

    public DebitNoteDTO cancel(Long id) {
        DebitNote note = debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser canceladas.");
        }
        CommercialNoteRules.requirePending(note.getStatus());
        note.setStatus(NoteStatus.CANCELLED);
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.logCurrent("DEBIT_NOTE_CANCEL",
                "Nota de débito " + saved.getNoteNumber() + " cancelada.");
        return queryService.toDTO(saved);
    }
}
