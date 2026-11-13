package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.modules.accounting.service.FinishedGoodsLookup;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A product is manufactured when it is the finished product of an active technical sheet (BOM) of the company. */
@Component
public class BomFinishedGoodsLookup implements FinishedGoodsLookup {

    private final BillOfMaterialsRepository bomRepository;
    private final InvoiceLookup invoiceLookup;
    private final InvoiceNoteLookup noteLookup;

    public BomFinishedGoodsLookup(BillOfMaterialsRepository bomRepository, InvoiceLookup invoiceLookup,
                                  InvoiceNoteLookup noteLookup) {
        this.bomRepository = bomRepository;
        this.invoiceLookup = invoiceLookup;
        this.noteLookup = noteLookup;
    }

    @Override
    public boolean isManufactured(Long companyId, Long productId) {
        return companyId != null && productId != null
                && bomRepository.existsByCompanyIdAndFinishedProductIdAndActiveTrue(companyId, productId);
    }

    @Override
    public BigDecimal finishedCostOfInvoice(Long companyId, Long invoiceId) {
        return invoiceLookup.find(invoiceId)
                .filter(invoice -> invoice.getCompany() != null && companyId.equals(invoice.getCompany().getId()))
                .map(invoice -> invoice.getLines().stream()
                        .filter(line -> manufactured(companyId, line.getProduct()))
                        .map(line -> line.lineCost())
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal finishedCostOfCreditNote(Long companyId, Long noteId) {
        return noteLookup.findCreditNoteWithLinesForCompany(noteId, companyId)
                .map(note -> note.getLines().stream()
                        .filter(line -> line.getInvoiceLine() != null && line.getInvoiceLine().getUnitCost() != null)
                        .filter(line -> manufactured(companyId, line.getInvoiceLine().getProduct()))
                        .map(line -> line.getInvoiceLine().getUnitCost().multiply(line.getQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);
    }

    private boolean manufactured(Long companyId, Product product) {
        return product != null && isManufactured(companyId, product.getId());
    }
}
