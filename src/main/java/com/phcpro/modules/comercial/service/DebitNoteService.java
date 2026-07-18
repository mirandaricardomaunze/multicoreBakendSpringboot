package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.CreateDebitNoteLineRequest;
import com.phcpro.modules.comercial.dto.CreateDebitNoteRequest;
import com.phcpro.modules.comercial.dto.DebitNoteDTO;
import com.phcpro.modules.comercial.dto.DebitNoteLineDTO;
import com.phcpro.modules.comercial.model.DebitNote;
import com.phcpro.modules.comercial.model.DebitNoteLine;
import com.phcpro.modules.comercial.model.DebitNoteReason;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.NoteStatus;
import com.phcpro.modules.comercial.repository.DebitNoteRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates debit-note lifecycle. Debit notes are purely financial — they
 * never touch inventory.
 */
@Service
public class DebitNoteService {

    private final DebitNoteRepository debitNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public DebitNoteService(
            DebitNoteRepository debitNoteRepository,
            InvoiceRepository invoiceRepository,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
        this.debitNoteRepository = debitNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
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

        BigDecimal totalBefore = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateDebitNoteLineRequest lineReq : request.lines()) {
            BigDecimal amount = lineReq.amount();
            BigDecimal taxRate = lineReq.taxRate() == null ? BigDecimal.ZERO : lineReq.taxRate();
            // Use LineCalculator with quantity=1 to get the same tax math as other modules.
            LineCalculator.LineAmounts amounts =
                    LineCalculator.compute(amount, BigDecimal.ONE, BigDecimal.ZERO, taxRate);

            DebitNoteLine line = new DebitNoteLine();
            line.setDescription(lineReq.description());
            line.setAmount(amount);
            line.setTaxRate(taxRate);
            line.setLineTotal(amounts.total());
            note.addLine(line);

            totalBefore = totalBefore.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        note.setTotalBeforeTax(totalBefore);
        note.setTaxAmount(totalTax);
        note.setTotalAmount(totalBefore.add(totalTax));

        return toDTO(debitNoteRepository.save(note));
    }

    @Transactional
    public DebitNoteDTO approve(Long id) {
        PermissionGuard.requireManagerOrAdmin("aprovar nota de débito");
        DebitNote note = debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() != NoteStatus.PENDING_APPROVAL && note.getStatus() != NoteStatus.DRAFT) {
            throw new BusinessRuleException("Apenas notas em rascunho ou pendentes podem ser aprovadas.");
        }
        note.setStatus(NoteStatus.APPROVED);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.logCurrent("DEBIT_NOTE_APPROVE",
                "Nota de débito " + saved.getNoteNumber() + " aprovada.");
        return toDTO(saved);
    }

    @Transactional
    public DebitNoteDTO reject(Long id, String rejectionReason) {
        PermissionGuard.requireManagerOrAdmin("rejeitar nota de débito");
        DebitNote note = debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser rejeitadas.");
        }
        note.setStatus(NoteStatus.REJECTED);
        note.setRejectionReason(rejectionReason);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.logCurrent("DEBIT_NOTE_REJECT",
                "Nota de débito " + saved.getNoteNumber() + " rejeitada. Motivo: " + rejectionReason);
        return toDTO(saved);
    }

    @Transactional
    public DebitNoteDTO cancel(Long id) {
        PermissionGuard.requireManagerOrAdmin("cancelar nota de débito");
        DebitNote note = debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser canceladas.");
        }
        note.setStatus(NoteStatus.CANCELLED);
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.logCurrent("DEBIT_NOTE_CANCEL",
                "Nota de débito " + saved.getNoteNumber() + " cancelada.");
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DebitNoteDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return debitNoteRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DebitNoteDTO findById(Long id) {
        return debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
    }

    @Transactional(readOnly = true)
    public DebitNote loadForPrint(Long id) {
        return debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
    }

    private DebitNoteDTO toDTO(DebitNote n) {
        List<DebitNoteLineDTO> lineDTOs = n.getLines().stream()
                .map(l -> new DebitNoteLineDTO(
                        l.getId(),
                        l.getDescription(),
                        l.getAmount(),
                        l.getTaxRate(),
                        l.getLineTotal()
                )).toList();
        return new DebitNoteDTO(
                n.getId(),
                n.getNoteNumber(),
                n.getIssueDate(),
                n.getCompany() != null ? n.getCompany().getId() : null,
                n.getClient().getId(),
                n.getClient().getName(),
                n.getInvoice().getId(),
                n.getInvoice().getInvoiceNumber(),
                n.getReason().name(),
                n.getStatus().name(),
                n.getTotalBeforeTax(),
                n.getTaxAmount(),
                n.getTotalAmount(),
                n.getDescription(),
                n.getApprovedBy(),
                n.getApprovedAt(),
                n.getRejectionReason(),
                lineDTOs
        );
    }
}
