package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.pricing.LineCalculator;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest;
import com.phcpro.modules.comercial.dto.CreateCreditNoteRequest;
import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.dto.CreditNoteLineDTO;
import com.phcpro.modules.comercial.model.CreditNote;
import com.phcpro.modules.comercial.model.CreditNoteLine;
import com.phcpro.modules.comercial.model.CreditNoteReason;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.NoteStatus;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.CreditNoteRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates credit-note lifecycle. Stock is returned to the warehouse only
 * when an APPROVED credit note has reason = RETURN — all the atomicity is
 * inside {@link #approve(Long)}.
 */
@Service
public class CreditNoteService {

    private static final DateTimeFormatter NUMBER_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    public CreditNoteService(
            CreditNoteRepository creditNoteRepository,
            InvoiceRepository invoiceRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
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
        note.setNoteNumber("NC-" + LocalDateTime.now().format(NUMBER_FMT));
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
            Product product = productRepository.findById(lineReq.productId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Produto não encontrado ID: " + lineReq.productId()));

            BigDecimal qty = lineReq.quantity();
            BigDecimal price = lineReq.unitPrice();
            BigDecimal taxRate = lineReq.taxRate() == null ? BigDecimal.ZERO : lineReq.taxRate();

            LineCalculator.LineAmounts amounts =
                    LineCalculator.compute(price, qty, BigDecimal.ZERO, taxRate);

            CreditNoteLine line = new CreditNoteLine();
            line.setProduct(product);
            line.setQuantity(qty);
            line.setUnitPrice(price);
            line.setTaxRate(taxRate);
            line.setLineTotal(amounts.total());
            line.setBatchNumber(lineReq.batchNumber());
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
        CreditNote note = creditNoteRepository.findByIdWithLines(id)
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
        return toDTO(creditNoteRepository.save(note));
    }

    @Transactional
    public CreditNoteDTO reject(Long id, String rejectionReason) {
        CreditNote note = creditNoteRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Uma nota já aprovada não pode ser rejeitada — emita uma nota de débito para reverter.");
        }
        note.setStatus(NoteStatus.REJECTED);
        note.setRejectionReason(rejectionReason);
        note.setApprovedBy(CurrentUserContext.getUsername());
        note.setApprovedAt(LocalDateTime.now());
        return toDTO(creditNoteRepository.save(note));
    }

    @Transactional
    public CreditNoteDTO cancel(Long id) {
        CreditNote note = creditNoteRepository.findByIdWithLines(id)
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
        if (note.getStatus() == NoteStatus.APPROVED) {
            throw new BusinessRuleException("Notas aprovadas não podem ser canceladas.");
        }
        note.setStatus(NoteStatus.CANCELLED);
        return toDTO(creditNoteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDTO> findByCompany(Long companyId) {
        return creditNoteRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CreditNoteDTO findById(Long id) {
        return creditNoteRepository.findByIdWithLines(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
    }

    @Transactional(readOnly = true)
    public CreditNote loadForPrint(Long id) {
        return creditNoteRepository.findByIdWithLines(id)
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
