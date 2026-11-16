package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Compõe exclusivamente a via térmica simplificada da factura. */
@Component
public class InvoiceThermalRenderer {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    byte[] render(Invoice invoice) {
        return PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, invoice.getCompany());
            ThermalReceiptRenderer.centered(doc, "FATURA SIMPLIFICADA", PdfTheme.boldFont());
            ThermalReceiptRenderer.centered(doc, "1.ª VIA — ORIGINAL (CLIENTE)", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, invoice.getInvoiceNumber(), PdfTheme.bodyFont());
            if (invoice.getCreatedAt() != null) {
                ThermalReceiptRenderer.centered(doc, "Data: " + invoice.getCreatedAt().format(DATE_FMT), PdfTheme.smallFont());
            }
            String buyer = invoice.getClient() != null ? invoice.getClient().getName() : invoice.getCustomerName();
            if (buyer != null && !buyer.isBlank()) ThermalReceiptRenderer.centered(doc, "Cliente: " + buyer, PdfTheme.smallFont());
            if (invoice.getClient() != null && invoice.getClient().getTaxId() != null) {
                ThermalReceiptRenderer.centered(doc, "NUIT Cliente: " + invoice.getClient().getTaxId(), PdfTheme.smallFont());
                String address = invoice.getClient().getAddress();
                if (address != null && !address.isBlank() && !"-".equals(address.trim()) && !"—".equals(address.trim())) {
                    ThermalReceiptRenderer.centered(doc, "Morada: " + address.trim(), PdfTheme.smallFont());
                }
            }
            ThermalReceiptRenderer.dottedLine(doc);
            doc.add(lines(invoice));
            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.right(doc, "Subtotal: " + MoneyFormat.format(invoice.getTotalBeforeTax()), PdfTheme.bodyFont());
            for (TotalsBlockRenderer.TaxBreakdown b : TotalsBlockRenderer.calculateBreakdowns(taxRows(invoice))) {
                ThermalReceiptRenderer.right(doc, b.taxRateLabel() + " s/ " + MoneyFormat.format(b.taxableAmount())
                        + ": " + MoneyFormat.format(b.taxAmount()), PdfTheme.smallFont());
            }
            ThermalReceiptRenderer.right(doc, "IVA:      " + MoneyFormat.format(invoice.getTaxAmount()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "TOTAL:    " + MoneyFormat.format(invoice.getTotalAmount()), PdfTheme.boldFont());
            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.centered(doc, "Os bens/serviços foram postos à disposição", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "na data da emissão do documento.", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "Processado por Computador / Multicore ERP", PdfTheme.smallFont());
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

    /** Per-rate VAT summary with the legal reason of each VAT-free group (Código do IVA). */
    private static List<LineItemsTableRenderer.Row> taxRows(Invoice invoice) {
        return invoice.getLines().stream().map(line -> new LineItemsTableRenderer.Row(null, null, null, null,
                line.getQuantity(), null, null, line.getUnitPrice(), line.getTaxRate(), line.getDiscountPercentage(),
                line.getLineTotal(), line.effectiveExemptionReason())).toList();
    }

    private static String rateLabel(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return "Isento";
        return "IVA " + MoneyFormat.formatQty(rate.multiply(BigDecimal.valueOf(100))) + "%";
    }
}
