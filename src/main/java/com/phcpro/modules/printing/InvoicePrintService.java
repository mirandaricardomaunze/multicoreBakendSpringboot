package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
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

    public InvoicePrintService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
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
            doc.add(buildClientBlock(invoice));
            doc.add(LineItemsTableRenderer.build(toRows(invoice.getLines())));
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

    private PdfPTable buildClientBlock(Invoice invoice) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell client = new PdfPCell();
        client.setBorder(PdfPCell.NO_BORDER);
        client.addElement(new Paragraph("Cliente", PdfTheme.subtitleFont()));
        if (invoice.getClient() != null) {
            client.addElement(new Paragraph(invoice.getClient().getName(), PdfTheme.bodyFont()));
            if (invoice.getClient().getTaxId() != null) {
                client.addElement(new Paragraph("NUIT: " + invoice.getClient().getTaxId(), PdfTheme.bodyFont()));
            }
            if (invoice.getClient().getAddress() != null) {
                client.addElement(new Paragraph(invoice.getClient().getAddress(), PdfTheme.bodyFont()));
            }
        }
        table.addCell(client);

        PdfPCell meta = new PdfPCell();
        meta.setBorder(PdfPCell.NO_BORDER);
        meta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (invoice.getCreatedAt() != null) {
            Paragraph date = new Paragraph("Data: " + invoice.getCreatedAt().format(DATE_FMT), PdfTheme.bodyFont());
            date.setAlignment(Element.ALIGN_RIGHT);
            meta.addElement(date);
        }
        if (invoice.getWarehouse() != null) {
            Paragraph wh = new Paragraph("Armazém: " + invoice.getWarehouse().getName(), PdfTheme.bodyFont());
            wh.setAlignment(Element.ALIGN_RIGHT);
            meta.addElement(wh);
        }
        table.addCell(meta);
        return table;
    }

    private List<LineItemsTableRenderer.Row> toRows(List<InvoiceLine> lines) {
        return lines.stream().map(l -> new LineItemsTableRenderer.Row(
                l.getProduct().getSku(),
                l.getProduct().getName(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}
