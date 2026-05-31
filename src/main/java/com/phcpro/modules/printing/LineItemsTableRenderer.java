package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Renders the document-line table shared by invoices, orders and purchases.
 * Caller provides rows in the canonical order; alignment and styling are
 * decided here so all documents match.
 */
public final class LineItemsTableRenderer {

    public record Row(
            String sku,
            String description,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountPercentage,
            BigDecimal lineTotal
    ) {}

    private LineItemsTableRenderer() {}

    public static PdfPTable build(List<Row> rows) {
        PdfPTable table = new PdfPTable(new float[]{12f, 38f, 8f, 14f, 8f, 8f, 14f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        header(table, "Código", Element.ALIGN_LEFT);
        header(table, "Descrição", Element.ALIGN_LEFT);
        header(table, "Qtd", Element.ALIGN_RIGHT);
        header(table, "Preço Unit.", Element.ALIGN_RIGHT);
        header(table, "IVA", Element.ALIGN_RIGHT);
        header(table, "Desc.", Element.ALIGN_RIGHT);
        header(table, "Total", Element.ALIGN_RIGHT);

        for (Row row : rows) {
            body(table, row.sku() == null ? "" : row.sku(), Element.ALIGN_LEFT);
            body(table, row.description() == null ? "" : row.description(), Element.ALIGN_LEFT);
            body(table, String.valueOf(row.quantity()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(row.unitPrice()), Element.ALIGN_RIGHT);
            body(table, formatRate(row.taxRate()), Element.ALIGN_RIGHT);
            body(table, formatRate(row.discountPercentage() == null ? BigDecimal.ZERO : row.discountPercentage().movePointLeft(2)), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(row.lineTotal()), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private static void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private static void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private static String formatRate(BigDecimal rate) {
        if (rate == null) return "0%";
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100));
        return percent.stripTrailingZeros().toPlainString() + "%";
    }
}
