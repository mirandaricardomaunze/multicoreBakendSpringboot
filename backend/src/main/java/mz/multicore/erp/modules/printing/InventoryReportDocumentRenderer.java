package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.format.DisplayLocale;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Renderizador A4 do relatório de Inventário de Stock. */
@Component
public class InventoryReportDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] render(Company company, Warehouse warehouseFilter, List<Stock> stocks) {
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Inventário de Stock",
                    "INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            ));
            doc.add(buildMetaBlock(warehouseFilter));
            doc.add(buildStockTable(stocks));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(buildSummary(stocks));
            doc.add(PdfDocumentBuilder.spacer(32f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildMetaBlock(Warehouse warehouseFilter) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Âmbito", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(
                warehouseFilter == null ? "Todos os armazéns da empresa" : "Armazém: " + warehouseFilter.getName(),
                PdfTheme.bodyFont()));
        if (warehouseFilter != null && warehouseFilter.getLocation() != null) {
            left.addElement(new Paragraph(warehouseFilter.getLocation(), PdfTheme.smallFont()));
        }
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph stamp = new Paragraph("Gerado em: " + LocalDateTime.now().format(DATE_FMT), PdfTheme.bodyFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildStockTable(List<Stock> stocks) {
        PdfPTable table = new PdfPTable(new float[]{15f, 16f, 33f, 12f, 11f, 13f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Código de Barras", Element.ALIGN_LEFT);
        header(table, "Designação", Element.ALIGN_LEFT);
        header(table, "Quantidade", Element.ALIGN_RIGHT);
        header(table, "Caixas", Element.ALIGN_RIGHT);
        header(table, "Preço", Element.ALIGN_RIGHT);

        for (Stock s : stocks) {
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            int unitsPerBox = s.getProduct().getUnitsPerBox() <= 0 ? 1 : s.getProduct().getUnitsPerBox();
            BigDecimal boxes = qty.divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
            BigDecimal price = s.getProduct().getUnitPrice() == null ? BigDecimal.ZERO : s.getProduct().getUnitPrice();

            String reference = s.getProduct().getReference();
            if (reference == null || reference.isBlank()) reference = s.getProduct().getSku();
            if (reference == null || reference.isBlank()) reference = "—";

            String barcode = s.getProduct().getBarcode();
            if (barcode == null || barcode.isBlank()) barcode = "—";

            body(table, reference, Element.ALIGN_LEFT);
            body(table, barcode, Element.ALIGN_LEFT);
            body(table, s.getProduct().getName(), Element.ALIGN_LEFT);
            body(table, formatNumber(qty), Element.ALIGN_RIGHT);
            body(table, formatNumber(boxes), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(price), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) return "0";
        if (value.stripTrailingZeros().scale() <= 0) {
            return String.format(DisplayLocale.NUMBERS, "%,.0f", value);
        }
        return String.format(DisplayLocale.NUMBERS, "%,.2f", value);
    }

    private PdfPTable buildSummary(List<Stock> stocks) {
        long total = stocks.size();
        BigDecimal totalQty = stocks.stream()
                .map(s -> s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = stocks.stream()
                .map(s -> {
                    BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
                    BigDecimal price = s.getProduct().getUnitPrice() == null ? BigDecimal.ZERO : s.getProduct().getUnitPrice();
                    return qty.multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable wrapper = new PdfPTable(new float[]{55f, 45f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        addSummaryLine(inner, "Artigos no inventário", String.valueOf(total), false);
        addSummaryLine(inner, "Total de Unidades", formatNumber(totalQty), false);
        addSummaryLine(inner, "VALOR TOTAL DO STOCK", MoneyFormat.format(totalValue), true);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Responsável pelo Inventário"));
        table.addCell(signatureCell("Conferido por"));
        return table;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);
        cell.addElement(new Paragraph("____________________________", PdfTheme.bodyFont()));
        cell.addElement(new Paragraph(label, PdfTheme.smallFont()));
        return cell;
    }

    private void addSummaryLine(PdfPTable t, String label, String value, boolean emphasised) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3f);
        v.setPadding(3f);
        if (emphasised) {
            l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        t.addCell(l);
        t.addCell(v);
    }

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }
}
