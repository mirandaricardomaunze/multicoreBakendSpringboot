package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
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
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor transacional de emissão e cálculo financeiro de notas de crédito.
 */
@Service
public class CreditNoteCreationService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final WarehouseLookup warehouseLookup;
    private final DocumentNumberService documentNumberService;
    private final CreditNoteQueryService queryService;
    private final List<CreditNoteLineGuard> lineGuards;

    @Autowired
    public CreditNoteCreationService(
            CreditNoteRepository creditNoteRepository,
            InvoiceRepository invoiceRepository,
            WarehouseLookup warehouseLookup,
            DocumentNumberService documentNumberService,
            CreditNoteQueryService queryService,
            List<CreditNoteLineGuard> lineGuards
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.warehouseLookup = warehouseLookup;
        this.documentNumberService = documentNumberService;
        this.queryService = queryService;
        this.lineGuards = lineGuards == null ? List.of() : lineGuards;
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
        CommercialNoteRules.requireValidInvoice(invoice);
        Warehouse warehouse = null;
        if (reason == CreditNoteReason.RETURN) {
            if (request.warehouseId() == null) {
                warehouse = invoice.getWarehouse();
            } else {
                warehouse = warehouseLookup.require(request.warehouseId());
            }
            if (warehouse == null) {
                throw new BusinessRuleException("Notas de crédito de devolução requerem um armazém de destino.");
            }
            if (!invoice.getCompany().getId().equals(warehouse.getCompany().getId())) {
                throw new BusinessRuleException("O armazém de devolução não pertence à empresa da fatura.");
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
        Map<Long, BigDecimal> requestedByLine = new HashMap<>();

        for (CreateCreditNoteLineRequest lineReq : request.lines()) {
            InvoiceLine sourceLine = invoice.getLines().stream()
                    .filter(l -> l.getId().equals(lineReq.invoiceLineId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Linha da fatura não encontrada (id=" + lineReq.invoiceLineId()
                                    + "). A nota de crédito tem de devolver linhas da fatura indicada."));

            BigDecimal qtyToReturn = lineReq.quantity();
            BigDecimal requested = requestedByLine.merge(sourceLine.getId(), qtyToReturn, BigDecimal::add);
            BigDecimal qtySold = sourceLine.getQuantity();
            BigDecimal alreadyReturned = creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(sourceLine.getId());
            if (alreadyReturned == null) alreadyReturned = BigDecimal.ZERO;
            BigDecimal remaining = qtySold.subtract(alreadyReturned);
            if (requested.compareTo(remaining) > 0) {
                throw new BusinessRuleException(String.format(
                        "Quantidade a devolver (%s) excede o disponível para a linha do produto '%s' "
                                + "(vendido: %s, já devolvido: %s, restante: %s).",
                        DisplayNumbers.qty(requested), sourceLine.getProduct().getName(), DisplayNumbers.qty(qtySold),
                        DisplayNumbers.qty(alreadyReturned), DisplayNumbers.qty(remaining)));
            }
            for (CreditNoteLineGuard guard : lineGuards) guard.check(invoice, sourceLine, requested, remaining);

            BigDecimal price = sourceLine.getUnitPrice();
            BigDecimal taxRate = sourceLine.getTaxRate() == null ? BigDecimal.ZERO : sourceLine.getTaxRate();

            BigDecimal discount = sourceLine.getDiscountPercentage() == null ? BigDecimal.ZERO : sourceLine.getDiscountPercentage();

            LineCalculator.LineAmounts amounts = LineCalculator.compute(price, qtyToReturn, discount, taxRate);

            CreditNoteLine line = new CreditNoteLine();
            line.setInvoiceLine(sourceLine);
            line.setProduct(sourceLine.getProduct());
            line.setQuantity(qtyToReturn);
            line.setUnitPrice(price);
            line.setTaxRate(taxRate);
            line.setDiscountPercentage(discount);
            line.setLineTotal(amounts.total());
            line.setBatchNumber(sourceLine.getBatchNumber());
            note.addLine(line);

            totalBefore = totalBefore.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        note.setTotalBeforeTax(totalBefore.setScale(2, RoundingMode.HALF_UP));
        note.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        note.setTotalAmount(totalBefore.add(totalTax).setScale(2, RoundingMode.HALF_UP));

        validateNotExceedsInvoiceValue(invoice, note);

        return queryService.toDTO(creditNoteRepository.save(note));
    }

    private void validateNotExceedsInvoiceValue(Invoice invoice, CreditNote candidate) {
        if (invoice.getTotalAmount() == null) return;
        BigDecimal cap = invoice.getTotalAmount();
        if (candidate.getTotalAmount().compareTo(cap) > 0) {
            throw new BusinessRuleException(String.format(
                    "O valor da nota de crédito (%s MT) excede o valor da fatura (%s MT).",
                    DisplayNumbers.money(candidate.getTotalAmount()), DisplayNumbers.money(cap)));
        }
    }
}
