package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
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

    private final InvoiceRepository invoiceRepository;
    private final PaymentEntryRepository paymentEntryRepository;
    private final DocumentConfigService documentConfigService;
    private final ReceiptDocumentRenderer documentRenderer;

    @Autowired
    public ReceiptPrintService(
            InvoiceRepository invoiceRepository,
            PaymentEntryRepository paymentEntryRepository,
            DocumentConfigService documentConfigService,
            ReceiptDocumentRenderer documentRenderer
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentEntryRepository = paymentEntryRepository;
        this.documentConfigService = documentConfigService;
        this.documentRenderer = documentRenderer;
    }

    public ReceiptPrintService(
            InvoiceRepository invoiceRepository,
            PaymentEntryRepository paymentEntryRepository,
            DocumentConfigService documentConfigService
    ) {
        this(invoiceRepository, paymentEntryRepository, documentConfigService, new ReceiptDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada para emissão de recibo."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        DocumentColumnsDTO cols = documentConfigService.getColumns(invoice.getCompany().getId(), DocumentType.POS_RECEIPT);
        List<PaymentEntry> entries = paymentEntryRepository.findByInvoiceIdOrderByPaidAtAsc(invoice.getId());
        return documentRenderer.renderDocument(invoice, cols, entries);
    }

    static String vatRateLabel(BigDecimal rate) {
        return ReceiptDocumentRenderer.vatRateLabel(rate);
    }

    static String lineDetailsLabel(InvoiceLine line, DocumentColumnsDTO cols) {
        return ReceiptDocumentRenderer.lineDetailsLabel(line, cols);
    }
}
