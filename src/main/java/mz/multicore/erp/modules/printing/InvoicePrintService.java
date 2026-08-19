package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a full A4 invoice PDF — company header, client block, line items,
 * totals. Single responsibility: render one Invoice as a fiscal-style PDF.
 */
@Service
public class InvoicePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InvoiceRepository invoiceRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public InvoicePrintService(InvoiceRepository invoiceRepository, LineRowMapper lineRowMapper,
                               DocumentConfigService documentConfigService) {
        this.invoiceRepository = invoiceRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    invoice.getCompany(),
                    "Fatura",
                    invoice.getInvoiceNumber()
            ));
            doc.add(ClientBlockRenderer.build(invoice.getClient(), invoice.getCreatedAt(), invoice.getWarehouse()));
            doc.add(LineItemsTableRenderer.build(toRows(invoice.getLines()),
                    documentConfigService.getColumns(invoice.getCompany().getId(), mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL)));
            doc.add(TotalsBlockRenderer.build(
                    invoice.getTotalBeforeTax(),
                    invoice.getTaxAmount(),
                    invoice.getTotalAmount()
            ));
            doc.add(PdfDocumentBuilder.spacer(10f));
            Paragraph status = new Paragraph("Estado: " + invoice.getStatus(), PdfTheme.boldFont());
            doc.add(status);
        });
    }

    private List<LineItemsTableRenderer.Row> toRows(List<InvoiceLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(),
                l.getBatchNumber(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}
