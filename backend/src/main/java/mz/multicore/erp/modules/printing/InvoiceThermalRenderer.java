package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Compõe exclusivamente a via térmica simplificada da factura. */
@Component
public class InvoiceThermalRenderer {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    byte[] render(Invoice invoice) {
        return render(invoice, null, null);
    }

    /** Talão com notas e rodapé do modelo da empresa; o logótipo sai sempre centrado no cabeçalho. */
    byte[] render(Invoice invoice, DocumentTemplateDTO template, String footer) {
        return PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, invoice.getCompany());
            ThermalReceiptRenderer.centered(doc, "FATURA SIMPLIFICADA", PdfTheme.boldFont());
            ThermalReceiptRenderer.centered(doc, "1.ª VIA — ORIGINAL (CLIENTE)", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, invoice.getInvoiceNumber(), PdfTheme.bodyFont());
            if (invoice.getCreatedAt() != null) {
                ThermalReceiptRenderer.centered(doc, "Data: " + invoice.getCreatedAt().format(DATE_FMT), PdfTheme.smallFont());
            }
            ThermalReceiptRenderer.centered(doc, "Cliente: "
                    + FiscalElements.buyerName(invoice.getCustomerName(), invoice.getClient()), PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "NUIT Cliente: " + FiscalElements.buyerTaxId(invoice.getClient()), PdfTheme.smallFont());
            if (invoice.getClient() != null) {
                String address = invoice.getClient().getAddress();
                if (address != null && !address.isBlank() && !"-".equals(address.trim()) && !"—".equals(address.trim())) {
                    ThermalReceiptRenderer.centered(doc, "Morada: " + address.trim(), PdfTheme.smallFont());
                }
            }
            ThermalReceiptRenderer.dottedLine(doc);
            doc.add(lines(invoice));
            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.right(doc, "Subtotal: " + MoneyFormat.format(invoice.getTotalBeforeTax()), PdfTheme.bodyFont());
            FiscalElements.thermalVatByRate(doc, FiscalElements.invoiceBreakdowns(invoice.getLines()));
            ThermalReceiptRenderer.right(doc, "IVA:      " + MoneyFormat.format(invoice.getTaxAmount()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "TOTAL:    " + MoneyFormat.format(invoice.getTotalAmount()), PdfTheme.boldFont());
            ThermalReceiptRenderer.dottedLine(doc);
            DocumentTemplateBlocks.thermalNotes(doc, template);
            ThermalReceiptRenderer.centered(doc, "Os bens/serviços foram postos à disposição", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "na data da emissão do documento.", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "Processado por Computador / Multicore ERP", PdfTheme.smallFont());
            ThermalReceiptRenderer.footer(doc, footer, null);
        });
    }

    private PdfPTable lines(Invoice invoice) {
        PdfPTable table = new PdfPTable(new float[]{65f, 35f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        ThermalReceiptRenderer.cell(table, "Artigo", PdfTheme.tableHeaderFont(), Element.ALIGN_LEFT, true);
        ThermalReceiptRenderer.cell(table, "Total", PdfTheme.tableHeaderFont(), Element.ALIGN_RIGHT, true);
        for (InvoiceLine line : invoice.getLines()) {
            Phrase description = new Phrase();
            description.add(new Chunk(line.getProduct().getName(), PdfTheme.bodyFont()));
            String detail = line.getQuantity() + " un. × " + MoneyFormat.formatPlain(line.getUnitPrice());
            if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0) {
                detail += " (-" + MoneyFormat.formatQty(line.getDiscountPercentage()) + "%)";
            }
            detail += " · " + rateLabel(line.getTaxRate());
            description.add(new Chunk("\n" + detail, PdfTheme.smallFont()));
            PdfPCell cell = new PdfPCell(description);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(ThermalReceiptRenderer.DOTTED_BOTTOM);
            cell.setPadding(3f);
            table.addCell(cell);
            ThermalReceiptRenderer.cell(table, MoneyFormat.formatPlain(line.getLineTotal()), PdfTheme.boldFont(), Element.ALIGN_RIGHT, false);
        }
        return table;
    }

    private static String rateLabel(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return "Isento";
        return "IVA " + MoneyFormat.formatQty(rate.multiply(BigDecimal.valueOf(100))) + "%";
    }
}
