package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Compõe exclusivamente a versão térmica informativa da cotação. */
@Component
public class QuotationThermalRenderer {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    byte[] render(Quotation quotation) {
        return PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, quotation.getCompany());
            ThermalReceiptRenderer.centered(doc, "COTAÇÃO COMERCIAL (80mm)", PdfTheme.boldFont());
            ThermalReceiptRenderer.centered(doc, quotation.getQuotationNumber(), PdfTheme.bodyFont());
            if (quotation.getQuotationDate() != null) ThermalReceiptRenderer.centered(doc, "Data: " + quotation.getQuotationDate().format(DATE_FMT), PdfTheme.smallFont());
            addValidity(doc, quotation);
            String client = quotation.getClient() != null ? quotation.getClient().getName() : quotation.getWalkInName();
            if (client != null && !client.isBlank()) ThermalReceiptRenderer.centered(doc, "Cliente: " + client, PdfTheme.smallFont());
            ThermalReceiptRenderer.dottedLine(doc);
            doc.add(lines(quotation));
            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.right(doc, "Subtotal: " + MoneyFormat.format(quotation.getTotalBeforeTax()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "IVA:      " + MoneyFormat.format(quotation.getTaxAmount()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "TOTAL:    " + MoneyFormat.format(quotation.getTotalAmount()), PdfTheme.boldFont());
            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.centered(doc, "PROPOSTA COMERCIAL INFORMATIVA", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "NÃO CONSTITUI DOCUMENTO FISCAL", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "Processado por Computador / Multicore ERP", PdfTheme.smallFont());
        });
    }

    private void addValidity(com.lowagie.text.Document doc, Quotation quotation) {
        if (quotation.getValidUntil() == null) return;
        boolean expired = quotation.getValidUntil().isBefore(LocalDate.now());
        String text = "Válida até: " + quotation.getValidUntil().format(DATE_FMT) + (expired ? " (EXPIRADA)" : "");
        ThermalReceiptRenderer.centered(doc, text, expired ? PdfTheme.boldFont() : PdfTheme.smallFont());
    }

    private PdfPTable lines(Quotation quotation) {
        PdfPTable table = new PdfPTable(new float[]{65f, 35f});
        table.setWidthPercentage(100);
        ThermalReceiptRenderer.cell(table, "Artigo", PdfTheme.tableHeaderFont(), Element.ALIGN_LEFT, true);
        ThermalReceiptRenderer.cell(table, "Total", PdfTheme.tableHeaderFont(), Element.ALIGN_RIGHT, true);
        for (QuotationLine line : quotation.getLines()) {
            Phrase phrase = new Phrase();
            phrase.add(new Chunk(line.getProduct().getName(), PdfTheme.bodyFont()));
            String detail = line.getQuantity() + " un. × " + MoneyFormat.formatPlain(line.getUnitPrice());
            if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0) detail += " (-" + MoneyFormat.formatQty(line.getDiscountPercentage()) + "%)";
            phrase.add(new Chunk("\n" + detail, PdfTheme.smallFont()));
            PdfPCell cell = new PdfPCell(phrase);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(ThermalReceiptRenderer.DOTTED_BOTTOM);
            cell.setPadding(3f);
            table.addCell(cell);
            ThermalReceiptRenderer.cell(table, MoneyFormat.formatPlain(line.getLineTotal()), PdfTheme.boldFont(), Element.ALIGN_RIGHT, false);
        }
        return table;
    }
}
