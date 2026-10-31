package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gestor transacional de emissão e cálculo financeiro de notas de crédito.
 */
@Service
public class CreditNoteCreationService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final WarehouseRepository warehouseRepository;
    private final DocumentNumberService documentNumberService;
    private final CreditNoteQueryService queryService;

    @Autowired
    public CreditNoteCreationService(
            CreditNoteRepository creditNoteRepository,
            InvoiceRepository invoiceRepository,
            WarehouseRepository warehouseRepository,
            DocumentNumberService documentNumberService,
            CreditNoteQueryService queryService
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.warehouseRepository = warehouseRepository;
        this.documentNumberService = documentNumberService;
        this.queryService = queryService;
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
                warehouse = invoice.getWarehouse();
            } else {
                warehouse = warehouseRepository.findById(request.warehouseId())
                        .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
            }
            if (warehouse == null) {
                throw new BusinessRuleException("Notas de crédito de devolução requerem um armazém de destino.");
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
            BigDecimal alreadyReturned = creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(sourceLine.getId());
            if (alreadyReturned == null) alreadyReturned = BigDecimal.ZERO;
            BigDecimal remaining = qtySold.subtract(alreadyReturned);
            if (qtyToReturn.compareTo(remaining) > 0) {
                throw new BusinessRuleException(String.format(
                        "Quantidade a devolver (%s) excede o disponível para a linha do produto '%s' "
                                + "(vendido: %s, já devolvido: %s, restante: %s).",
                        qtyToReturn, sourceLine.getProduct().getName(), qtySold, alreadyReturned, remaining));
            }

            BigDecimal price = sourceLine.getUnitPrice();
            BigDecimal taxRate = sourceLine.getTaxRate() == null ? BigDecimal.ZERO : sourceLine.getTaxRate();

            LineCalculator.LineAmounts amounts = LineCalculator.compute(price, qtyToReturn, BigDecimal.ZERO, taxRate);

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

        return queryService.toDTO(creditNoteRepository.save(note));
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
}
