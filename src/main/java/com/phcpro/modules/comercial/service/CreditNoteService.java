package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest;
import com.phcpro.modules.comercial.dto.CreateCreditNoteRequest;
import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.dto.CreditNoteLineDTO;
import com.phcpro.modules.comercial.model.CreditNote;
import com.phcpro.modules.comercial.model.CreditNoteLine;
import com.phcpro.modules.comercial.model.CreditNoteReason;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.NoteStatus;
import com.phcpro.modules.comercial.repository.CreditNoteRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates credit-note lifecycle. Stock is returned to the warehouse only
 * when an APPROVED credit note has reason = RETURN — all the atomicity is
 * inside {@link #approve(Long)}.
 */
@Service
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public CreditNoteService(
            CreditNoteRepository creditNoteRepository,
            InvoiceRepository invoiceRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CreditNoteDTO create(CreateCreditNoteRequest request) {
        CreditNoteReason reason;
        try {
            reason = CreditNoteReason.valueOf(request.reason());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Motivo de nota de crédito inválido: " + request.reason());
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        Warehouse warehouse = null;
        if (reason == CreditNoteReason.RETURN) {
            if (request.warehouseId() == null) {
                // Fall back to the invoice's warehouse
                warehouse = invoice.getWarehouse();
            } else {
                warehouse = warehouseRepository.findById(request.warehouseId())
                        .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
            }
            if (warehouse == null) {
                throw new BusinessRuleException(
                        "Notas de crédito de devolução requerem um armazém de destino.");
            }
        }

        CreditNote note = new CreditNote();
        note.setNoteNumber(documentNumberService.next(DocumentSeries.CREDIT_NOTE));
        note.setIssueDate(LocalDateTime.now());
        note.setCompany(invoice.getCompany());
        note.setClient(invoice.getClient());
        note.setInvoice(invoice);
        note.setWarehouse(warehouse);
        note.setReason(reason);
        note.setStatus(NoteStatus.PENDING_APPROVAL);
        note.setDescription(request.description());
        note.setCreatedBy(CurrentUserContext.getUsername());

        BigDecimal totalBefore = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateCreditNoteLineRequest lineReq : request.lines()) {
            InvoiceLine sourceLine = invoice.getLines().stream()
                    .filter(l -> l.getId().equals(lineReq.invoiceLineId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Linha da fatura não encontrada (id=" + lineReq.invoiceLineId()
                                    + "). A nota de crédito tem de devolver linhas da fatura indicada."));

            BigDecimal qtyToReturn = lineReq.quantity();
            BigDecimal qtySold = sourceLine.getQuantity();
            BigDecimal alreadyReturned = creditNoteRepository
                    .sumNonVoidedReturnedByInvoiceLineId(sourceLine.getId());
            if (alreadyReturned == null) alreadyReturned = BigDecimal.ZERO;
            BigDecimal remaining = qtySold.subtract(alreadyReturned);
            if (qtyToReturn.compareTo(remaining) > 0) {
                throw new BusinessRuleException(String.format(
                        "Quantidade a devolver (%s) excede o disponível para a linha do produto '%s' "
                                + "(vendido: %s, já devolvido: %s, restante: %s).",
                        qtyToReturn, sourceLine.getProduct().getName(),
                        qtySold, alreadyReturned, remaining));
            }

            // Tudo é herdado da linha de fatura — operador só escolheu quantidade.
            BigDecimal price = sourceLine.getUnitPrice();
            BigDecimal taxRate = sourceLine.getTaxRate() == null ? BigDecimal.ZERO : sourceLine.getTaxRate();

            LineCalculator.LineAmounts amounts =
                    LineCalculator.compute(price, qtyToReturn, BigDecimal.ZERO, taxRate);

            CreditNoteLine line = new CreditNoteLine();
            line.setInvoiceLine(sourceLine);
            line.setProduct(sourceLine.getProduct());
            line.setQuantity(qtyToReturn);
            line.setUnitPrice(price);
            line.setTaxRate(taxRate);
            line.setLineTotal(amounts.total());
            line.setBatchNumber(sourceLine.getBatchNumber());
            note.addLine(line);

            totalBefore = totalBefore.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        note.setTotalBeforeTax(totalBefore);
        note.setTaxAmount(totalTax);
        note.setTotalAmount(totalBefore.add(totalTax));

        validateNotExceedsInvoiceValue(invoice, note);

        return toDTO(creditNoteRepository.save(note));
    }

    @Transactional
    public CreditNoteDTO approve(Long id) {
        PermissionGuard.requireManagerOrAdmin("aprovar nota de crédito");
        CreditNote note = creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));

        if (note.getStatus() != NoteStatus.PENDING_APPROVAL && note.getStatus() != NoteStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Apenas notas em rascunho ou pendentes podem ser aprovadas.");
        }

        if (note.getReason() == CreditNoteReason.RETURN) {
            returnStockForApprovedNote(note);
        }

        note.setStatus(NoteStatus.APPROVED);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.logCurrent("CREDIT_NOTE_APPROVE",
                "Nota de crédito " + saved.getNoteNumber() + " aprovada.");
        return toDTO(saved);
    }

    @Transactional
    public CreditNoteDTO reject(Long id, String rejectionReason) {
        PermissionGuard.requireManagerOrAdmin("rejeitar nota de crédito");
        CreditNote note = creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Uma nota já aprovada não pode ser rejeitada — emita uma nota de débito para reverter.");
        }
        note.setStatus(NoteStatus.REJECTED);
        note.setRejectionReason(rejectionReason);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.logCurrent("CREDIT_NOTE_REJECT",
                "Nota de crédito " + saved.getNoteNumber() + " rejeitada. Motivo: " + rejectionReason);
        return toDTO(saved);
    }

    @Transactional
    public CreditNoteDTO cancel(Long id) {
        PermissionGuard.requireManagerOrAdmin("cancelar nota de crédito");
        CreditNote note = creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser canceladas.");
        }
        note.setStatus(NoteStatus.CANCELLED);
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.logCurrent("CREDIT_NOTE_CANCEL",
                "Nota de crédito " + saved.getNoteNumber() + " cancelada.");
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return creditNoteRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CreditNoteDTO findById(Long id) {
        return creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
    }

    /**
     * Para uma fatura, devolve quanto já foi devolvido por cada linha (em NCs aprovadas
     * ou pendentes). Usado pelo UI para mostrar "Qty restante" ao criar nova NC.
     */
    @Transactional(readOnly = true)
    public java.util.Map<Long, BigDecimal> getReturnedQuantitiesByInvoiceLine(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        java.util.Map<Long, BigDecimal> result = new java.util.HashMap<>();
        for (InvoiceLine il : invoice.getLines()) {
            BigDecimal qty = creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(il.getId());
            result.put(il.getId(), qty == null ? BigDecimal.ZERO : qty);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CreditNote loadForPrint(Long id) {
        return creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
    }

    private void returnStockForApprovedNote(CreditNote note) {
        Warehouse warehouse = note.getWarehouse();
        if (warehouse == null) {
            throw new BusinessRuleException(
                    "Não é possível devolver stock sem armazém definido na nota.");
        }
        String description = String.format("Devolução — Nota de Crédito %s (Fatura %s)",
                note.getNoteNumber(),
                note.getInvoice() != null ? note.getInvoice().getInvoiceNumber() : "—");

        for (CreditNoteLine line : note.getLines()) {
            // Positive quantity = entry (return). Existing FEFO/batch logic in
            // InventoryService handles the batch/expiration ledger.
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

    private void validateNotExceedsInvoiceValue(Invoice invoice, CreditNote candidate) {
        if (invoice.getTotalAmount() == null) return;
        BigDecimal cap = invoice.getTotalAmount();
        if (candidate.getTotalAmount().compareTo(cap) > 0) {
            throw new BusinessRuleException(String.format(
                    "O valor da nota de crédito (%.2f) excede o valor da fatura (%.2f).",
                    candidate.getTotalAmount(), cap));
        }
    }

    private CreditNoteDTO toDTO(CreditNote n) {
        List<CreditNoteLineDTO> lineDTOs = n.getLines().stream()
                .map(l -> new CreditNoteLineDTO(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getTaxRate(),
                        l.getLineTotal(),
                        l.getBatchNumber()
                )).toList();
        return new CreditNoteDTO(
                n.getId(),
                n.getNoteNumber(),
                n.getIssueDate(),
                n.getCompany() != null ? n.getCompany().getId() : null,
                n.getClient().getId(),
                n.getClient().getName(),
                n.getInvoice().getId(),
                n.getInvoice().getInvoiceNumber(),
                n.getWarehouse() != null ? n.getWarehouse().getId() : null,
                n.getWarehouse() != null ? n.getWarehouse().getName() : null,
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
