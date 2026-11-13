package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.service.PosPaymentLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Generates a compact (~80mm) PDF receipt for a POS sale.
 */
@Service
public class ReceiptPrintService {

    private final InvoiceLookup invoiceLookup;
    private final PosPaymentLookup paymentLookup;
    private final DocumentConfigService documentConfigService;
    private final ReceiptDocumentRenderer documentRenderer;

    @Autowired
    public ReceiptPrintService(
            InvoiceLookup invoiceLookup,
            PosPaymentLookup paymentLookup,
            DocumentConfigService documentConfigService,
            ReceiptDocumentRenderer documentRenderer
    ) {
        this.invoiceLookup = invoiceLookup;
        this.paymentLookup = paymentLookup;
        this.documentConfigService = documentConfigService;
        this.documentRenderer = documentRenderer;
    }

    public ReceiptPrintService(
            InvoiceLookup invoiceLookup,
            PosPaymentLookup paymentLookup,
            DocumentConfigService documentConfigService
    ) {
        this(invoiceLookup, paymentLookup, documentConfigService, new ReceiptDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceLookup.require(invoiceId);
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        DocumentColumnsDTO cols = documentConfigService.getColumns(invoice.getCompany().getId(), DocumentType.POS_RECEIPT);
        List<PaymentEntry> entries = paymentLookup.listForInvoice(invoice.getId());
        return documentRenderer.renderDocument(invoice, cols, entries);
    }

    static String vatRateLabel(BigDecimal rate) {
        return ReceiptDocumentRenderer.vatRateLabel(rate);
    }

    static String lineDetailsLabel(InvoiceLine line, DocumentColumnsDTO cols) {
        return ReceiptDocumentRenderer.lineDetailsLabel(line, cols);
    }
}
