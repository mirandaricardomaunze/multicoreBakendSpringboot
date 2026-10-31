package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class InventoryCountSheetDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] renderA4(Company company, Warehouse warehouseFilter, List<Stock> stocks) {
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Folha de Contagem de Inventário",
                    "CNT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            ));
            doc.add(buildMetaBlock(warehouseFilter, stocks.size()));
            doc.add(buildCountTable(stocks, warehouseFilter == null));
            doc.add(PdfDocumentBuilder.spacer(28f));
            doc.add(buildSignatureBlock());
        });
    }

    private PdfPTable buildMetaBlock(Warehouse warehouseFilter, int total) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Âmbito (contagem cega)", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(
                warehouseFilter == null ? "Todos os armazéns da empresa"
                        : "Armazém: " + warehouseFilter.getName(),
                PdfTheme.bodyFont()));
        left.addElement(new Paragraph(total + " artigo(s) a contar", PdfTheme.smallFont()));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        Paragraph stamp = new Paragraph("Gerado em: " + LocalDateTime.now().format(DATE_FMT), PdfTheme.bodyFont());
        stamp.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(stamp);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildCountTable(List<Stock> stocks, boolean showWarehouse) {
        PdfPTable table = showWarehouse
                ? new PdfPTable(new float[]{14f, 15f, 33f, 16f, 22f})
                : new PdfPTable(new float[]{16f, 18f, 42f, 24f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Cód. Barras", Element.ALIGN_LEFT);
        header(table, "Nome do Artigo", Element.ALIGN_LEFT);
        if (showWarehouse) header(table, "Armazém", Element.ALIGN_LEFT);
        header(table, "Contagem", Element.ALIGN_CENTER);

        for (Stock s : stocks) {
            String reference = s.getProduct().getReference();
            if (reference == null || reference.isBlank()) reference = s.getProduct().getSku();

            body(table, reference, Element.ALIGN_LEFT);
            body(table, s.getProduct().getBarcode(), Element.ALIGN_LEFT);
            body(table, s.getProduct().getName(), Element.ALIGN_LEFT);
            if (showWarehouse) body(table, s.getWarehouse().getName(), Element.ALIGN_LEFT);
            countBox(table);
        }
        return table;
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.addCell(signatureCell("Contado por"));
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
        cell.setMinimumHeight(18f);
        table.addCell(cell);
    }

    private void countBox(PdfPTable table) {
        PdfPCell cell = new PdfPCell(new Phrase(" ", PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setMinimumHeight(18f);
        table.addCell(cell);
    }
}
