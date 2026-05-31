package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.security.CurrentUserContext;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates debit-note lifecycle. Debit notes are purely financial — they
 * never touch inventory.
 */
@Service
public class DebitNoteService {

    private static final DateTimeFormatter NUMBER_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DebitNoteRepository debitNoteRepository;
    private final InvoiceRepository invoiceRepository;

    public DebitNoteService(
            DebitNoteRepository debitNoteRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.debitNoteRepository = debitNoteRepository;
        this.invoiceRepository = invoiceRepository;
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

        DebitNote note = new DebitNote();
        note.setNoteNumber("ND-" + LocalDateTime.now().format(NUMBER_FMT));
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
        DebitNote note = debitNoteRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() != NoteStatus.PENDING_APPROVAL && note.getStatus() != NoteStatus.DRAFT) {
            throw new BusinessRuleException("Apenas notas em rascunho ou pendentes podem ser aprovadas.");
        }
        note.setStatus(NoteStatus.APPROVED);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        return toDTO(debitNoteRepository.save(note));
    }

    @Transactional
    public DebitNoteDTO reject(Long id, String rejectionReason) {
        DebitNote note = debitNoteRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser rejeitadas.");
        }
        note.setStatus(NoteStatus.REJECTED);
        note.setRejectionReason(rejectionReason);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        return toDTO(debitNoteRepository.save(note));
    }

    @Transactional
    public DebitNoteDTO cancel(Long id) {
        DebitNote note = debitNoteRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser canceladas.");
        }
        note.setStatus(NoteStatus.CANCELLED);
        return toDTO(debitNoteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<DebitNoteDTO> findByCompany(Long companyId) {
        return debitNoteRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DebitNoteDTO findById(Long id) {
        return debitNoteRepository.findByIdWithLines(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
    }

    @Transactional(readOnly = true)
    public DebitNote loadForPrint(Long id) {
        return debitNoteRepository.findByIdWithLines(id)
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
