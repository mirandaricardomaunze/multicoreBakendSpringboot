package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.pricing.LineCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders the document-line table shared by invoices, orders and credit notes.
 * Caller provides rows in the canonical order; columns, alignment and formatting
 * are decided here so every commercial document looks identical.
 *
 * Colunas canónicas (ver docs/DOCUMENT_LINE_COLUMNS_SPEC.md):
 * Cód. Barras · Referência · Descrição · Validade · Qtd · Preço Unit. · IVA · Subtotal.
 */
public final class LineItemsTableRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Row(
            String barcode,
            String reference,
            String description,
            LocalDate expiryDate,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountPercentage,
            BigDecimal lineTotal
    ) {}

    private LineItemsTableRenderer() {}

    public static PdfPTable build(List<Row> rows) {
        PdfPTable table = new PdfPTable(new float[]{14f, 10f, 24f, 11f, 7f, 12f, 6f, 16f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        header(table, "Cód. Barras", Element.ALIGN_LEFT);
        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Descrição", Element.ALIGN_LEFT);
        header(table, "Validade", Element.ALIGN_CENTER);
        header(table, "Qtd", Element.ALIGN_RIGHT);
        header(table, "Preço Unit.", Element.ALIGN_RIGHT);
        header(table, "IVA", Element.ALIGN_RIGHT);
        header(table, "Subtotal", Element.ALIGN_RIGHT);

        for (Row row : rows) {
            body(table, safe(row.barcode()), Element.ALIGN_LEFT);
            body(table, safe(row.reference()), Element.ALIGN_LEFT);
            body(table, safe(row.description()), Element.ALIGN_LEFT);
            body(table, formatExpiry(row.expiryDate()), Element.ALIGN_CENTER);
            body(table, formatQuantity(row.quantity()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(row.unitPrice()), Element.ALIGN_RIGHT);
            body(table, formatRate(row.taxRate()), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(subtotal(row)), Element.ALIGN_RIGHT);
        }
        return table;
    }

    /** Subtotal líquido da linha (antes de IVA), coerente com a matemática de negócio. */
    static BigDecimal subtotal(Row row) {
        return LineCalculator.compute(row.unitPrice(), row.quantity(), row.discountPercentage(), row.taxRate())
                .net().setScale(2, RoundingMode.HALF_UP);
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static String formatExpiry(LocalDate expiry) {
        return expiry == null ? "—" : expiry.format(DATE_FMT);
    }

    private static String formatRate(BigDecimal rate) {
        if (rate == null) return "0%";
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100));
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    private static String formatQuantity(BigDecimal quantity) {
        if (quantity == null) return "0";
        return quantity.stripTrailingZeros().toPlainString();
    }
}
