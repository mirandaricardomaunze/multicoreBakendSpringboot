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
 * Cód. · Ref. · Desc. · Val. · Qtd. · Emb. · Cx. · % Cx. · P. Unit. · IVA · Subt.
 */
public final class LineItemsTableRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Row(
            String barcode,
            String reference,
            String description,
            LocalDate expiryDate,
            BigDecimal quantity,
            Integer packagesPerBox,
            Integer unitsPerPackage,
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
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            boolean alternate = (rowIndex % 2 == 1);
            for (Column column : columns) {
                body(table, column.cell().apply(row), column.align(), alternate);
            }
        }
        return table;
    }

    private static List<Column> activeColumns(DocumentColumnsDTO cols) {
        List<Column> columns = new ArrayList<>();
        if (cols.barcode()) {
            columns.add(new Column(8.5f, "Cód.", Element.ALIGN_LEFT, r -> safe(r.barcode())));
        }
        if (cols.reference()) {
            columns.add(new Column(7.0f, "Ref.", Element.ALIGN_LEFT, r -> safe(r.reference())));
        }
        if (cols.description()) {
            boolean allCanonical = cols.barcode() && cols.reference() && cols.expiry() && cols.quantity()
                    && cols.packages() && cols.boxes() && cols.boxPercentage() && cols.unitPrice() && cols.tax() && cols.subtotal();
            float descWidth = 19.5f;
            if (!allCanonical) {
                float otherWidths = (cols.barcode() ? 8.5f : 0f)
                        + (cols.reference() ? 7.0f : 0f)
                        + (cols.expiry() ? 9.0f : 0f)
                        + (cols.quantity() ? 8.0f : 0f)
                        + (cols.packages() ? 6.5f : 0f)
                        + (cols.boxes() ? 6.5f : 0f)
                        + (cols.boxPercentage() ? 8.0f : 0f)
                        + (cols.unitPrice() ? 13.5f : 0f)
                        + (cols.tax() ? 4.0f : 0f)
                        + (cols.subtotal() ? 9.5f : 0f);
                descWidth = Math.max(19.5f, 100f - otherWidths);
            }
            columns.add(new Column(descWidth, "Desc.", Element.ALIGN_LEFT, r -> safe(r.description())));
        }
        if (cols.expiry()) {
            columns.add(new Column(9.0f, "Val.", Element.ALIGN_CENTER, r -> formatExpiry(r.expiryDate())));
        }
        if (cols.quantity()) {
            columns.add(new Column(8.0f, "Qtd.", Element.ALIGN_RIGHT, r -> formatQuantity(r.quantity())));
        }
        if (cols.packages()) {
            columns.add(new Column(6.5f, "Emb.", Element.ALIGN_RIGHT,
                    LineItemsTableRenderer::formatPackages));
        }
        if (cols.boxes()) {
            columns.add(new Column(6.5f, "Cx.", Element.ALIGN_RIGHT,
                    LineItemsTableRenderer::formatBoxes));
        }
        if (cols.boxPercentage()) {
            columns.add(new Column(8.0f, "% Cx.", Element.ALIGN_RIGHT,
                    LineItemsTableRenderer::formatBoxPercentage));
        }
        if (cols.unitPrice()) {
            columns.add(new Column(13.5f, "P. Unit.", Element.ALIGN_RIGHT, r -> MoneyFormat.formatPlain(r.unitPrice())));
        }
        if (cols.tax()) {
            columns.add(new Column(4.0f, "IVA", Element.ALIGN_RIGHT, r -> formatRate(r.taxRate())));
        }
        if (cols.subtotal()) {
            columns.add(new Column(9.5f, "Subt.", Element.ALIGN_RIGHT, r -> MoneyFormat.formatPlain(subtotal(r))));
        }
        if (columns.isEmpty()) {
            columns.add(new Column(30f, "Descrição", Element.ALIGN_LEFT, r -> safe(r.description())));
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
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPaddingTop(4.0f);
        cell.setPaddingBottom(4.0f);
        cell.setPaddingLeft(3.5f);
        cell.setPaddingRight(3.5f);
        cell.setNoWrap(true);
        table.addCell(cell);
    }

    private static void body(PdfPTable table, String text, int align, boolean alternate) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableBodyFont()));
        if (alternate) {
            cell.setBackgroundColor(PdfTheme.ROW_ALT);
        }
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(3.5f);
        cell.setPaddingRight(3.5f);
        table.addCell(cell);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static String formatExpiry(LocalDate expiry) {
        if (expiry == null || expiry.getYear() >= 9900) {
            return "—";
        }
        return expiry.format(DATE_FMT);
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

    /** Quantidade da linha expressa em embalagens equivalentes, incluindo fracções de embalagem. */
    static String formatPackages(Row row) {
        if (!hasPackaging(row)) return "—";
        BigDecimal packages = safeQuantity(row.quantity())
                .divide(BigDecimal.valueOf(row.unitsPerPackage()), 3, RoundingMode.HALF_UP);
        return packages.stripTrailingZeros().toPlainString();
    }

    /** Quantidade da linha expressa em caixas equivalentes, incluindo fracções de caixa. */
    static String formatBoxes(Row row) {
        if (!hasPackaging(row)) return "—";
        long unitsPerBox = (long) row.packagesPerBox() * row.unitsPerPackage();
        BigDecimal boxes = safeQuantity(row.quantity())
                .divide(BigDecimal.valueOf(unitsPerBox), 3, RoundingMode.HALF_UP);
        return boxes.stripTrailingZeros().toPlainString();
    }

    /** 100% corresponde exactamente a uma caixa; valores acima de 100% representam várias caixas. */
    static String formatBoxPercentage(Row row) {
        if (!hasPackaging(row)) return "—";
        long unitsPerBox = (long) row.packagesPerBox() * row.unitsPerPackage();
        BigDecimal percentage = safeQuantity(row.quantity())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
        return percentage.stripTrailingZeros().toPlainString() + "%";
    }

    private static boolean hasPackaging(Row row) {
        return row != null && row.packagesPerBox() != null && row.packagesPerBox() > 0
                && row.unitsPerPackage() != null && row.unitsPerPackage() > 0;
    }

    private static BigDecimal safeQuantity(BigDecimal quantity) {
        return quantity == null ? BigDecimal.ZERO : quantity;
    }
}
