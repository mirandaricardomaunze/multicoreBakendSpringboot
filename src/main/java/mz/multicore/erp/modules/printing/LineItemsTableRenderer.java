package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    /** Uma coluna da tabela: largura, cabeçalho, alinhamento e como formatar a célula a partir da linha. */
    private record Column(float width, String header, int align, Function<Row, String> cell) {}

    public static PdfPTable build(List<Row> rows) {
        return build(rows, DocumentColumnsDTO.all());
    }

    /**
     * Monta a tabela apenas com as colunas activas em {@code cols}, mantendo por coluna a largura,
     * o alinhamento e a formatação canónicos. Se nenhuma coluna estiver activa, mostra só Descrição
     * (fallback defensivo). A matemática (subtotal = {@code LineCalculator.net}) fica intacta.
     */
    public static PdfPTable build(List<Row> rows, DocumentColumnsDTO cols) {
        List<Column> columns = activeColumns(cols);

        float[] widths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).width();
        }

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        for (Column column : columns) {
            header(table, column.header(), column.align());
        }
        for (Row row : rows) {
            for (Column column : columns) {
                body(table, column.cell().apply(row), column.align());
            }
        }
        return table;
    }

    private static List<Column> activeColumns(DocumentColumnsDTO cols) {
        List<Column> columns = new ArrayList<>();
        if (cols.barcode()) {
            columns.add(new Column(14f, "Cód. Barras", Element.ALIGN_LEFT, r -> safe(r.barcode())));
        }
        if (cols.reference()) {
            columns.add(new Column(10f, "Referência", Element.ALIGN_LEFT, r -> safe(r.reference())));
        }
        if (cols.description()) {
            columns.add(new Column(24f, "Descrição", Element.ALIGN_LEFT, r -> safe(r.description())));
        }
        if (cols.expiry()) {
            columns.add(new Column(11f, "Validade", Element.ALIGN_CENTER, r -> formatExpiry(r.expiryDate())));
        }
        if (cols.quantity()) {
            columns.add(new Column(7f, "Qtd", Element.ALIGN_RIGHT, r -> formatQuantity(r.quantity())));
        }
        if (cols.unitPrice()) {
            columns.add(new Column(12f, "Preço Unit.", Element.ALIGN_RIGHT, r -> MoneyFormat.formatPlain(r.unitPrice())));
        }
        if (cols.tax()) {
            columns.add(new Column(6f, "IVA", Element.ALIGN_RIGHT, r -> formatRate(r.taxRate())));
        }
        if (cols.subtotal()) {
            columns.add(new Column(16f, "Subtotal", Element.ALIGN_RIGHT, r -> MoneyFormat.formatPlain(subtotal(r))));
        }
        if (columns.isEmpty()) {
            columns.add(new Column(24f, "Descrição", Element.ALIGN_LEFT, r -> safe(r.description())));
        }
        return columns;
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
