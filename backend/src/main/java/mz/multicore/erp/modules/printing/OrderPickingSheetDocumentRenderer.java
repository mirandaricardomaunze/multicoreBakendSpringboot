package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Component
public class OrderPickingSheetDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] renderDocument(Order order, boolean reprint) {
        return PdfDocumentBuilder.build(PageSize.A4, doc -> render(doc, order, reprint));
    }

    private void render(Document document, Order order, boolean reprint) {
        String statusLabel = OrderStatusLabel.of(order.getStatus());
        String docTitle = reprint ? "GUIA DE SEPARAÇÃO (REIMPRESSÃO)" : "GUIA DE SEPARAÇÃO / PICKING LIST";
        document.add(CompanyHeaderRenderer.build(order.getCompany(), docTitle, order.getOrderNumber(), statusLabel));
        document.add(buildMetadataTable(order));
        document.add(PdfDocumentBuilder.spacer(10f));
        document.add(buildLinesTable(order));
        document.add(PdfDocumentBuilder.spacer(14f));
        document.add(buildSignaturesBlock());
    }

    private PdfPTable buildMetadataTable(Order order) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("CLIENTE: " + (order.getClient() != null ? order.getClient().getName() : "Consumidor Final"), PdfTheme.boldFont()));
        if (order.getClient() != null && order.getClient().getTaxId() != null) {
            left.addElement(new Paragraph("NUIT: " + order.getClient().getTaxId(), PdfTheme.bodyFont()));
        }
        if (order.getClient() != null && order.getClient().getAddress() != null) {
            left.addElement(new Paragraph("Morada: " + order.getClient().getAddress(), PdfTheme.smallFont()));
        }

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("ARMAZÉM DE EXPEDIÇÃO: " + (order.getWarehouse() != null ? order.getWarehouse().getName() : "Central"), PdfTheme.boldFont()));
        if (order.getCreatedAt() != null) {
            right.addElement(new Paragraph("Data de Emissão: " + order.getCreatedAt().format(DATE_FMT), PdfTheme.bodyFont()));
        }
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            right.addElement(new Paragraph("Observações: " + order.getNotes().trim(), PdfTheme.smallFont()));
        }

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildLinesTable(Order order) {
        PdfPTable table = new PdfPTable(new float[]{5f, 8f, 13f, 32f, 9f, 7f, 8f, 18f});
        table.setWidthPercentage(100);
        addHeader(table, "OK", Element.ALIGN_CENTER);
        addHeader(table, "Ref.", Element.ALIGN_LEFT);
        addHeader(table, "Cód. Barras", Element.ALIGN_LEFT);
        addHeader(table, "Designação do Artigo", Element.ALIGN_LEFT);
        addHeader(table, "Qtd.", Element.ALIGN_RIGHT);
        addHeader(table, "Emb.", Element.ALIGN_RIGHT);
        addHeader(table, "Cx.", Element.ALIGN_RIGHT);
        addHeader(table, "Lote / Validade", Element.ALIGN_LEFT);

        BigDecimal totalQty = BigDecimal.ZERO, totalBoxes = BigDecimal.ZERO;
        boolean alt = false;

        for (OrderLine line : order.getLines()) {
            var product = line.getProduct();
            String ref = product != null && product.getReference() != null ? product.getReference() : "—";
            String barcode = product != null && product.getBarcode() != null ? product.getBarcode() : "—";
            String name = product != null ? product.getName() : "Artigo";
            String qty = MoneyFormat.formatQty(line.getQuantity());
            String emb = calculatePackages(line);
            String cx = calculateBoxes(line);
            String batch = line.getBatchNumber() != null ? line.getBatchNumber() : "Lote padrão / FEFO";

            addCheckboxCell(table, alt);
            addBodyCell(table, ref, Element.ALIGN_LEFT, alt);
            addBodyCell(table, barcode, Element.ALIGN_LEFT, alt);
            addBodyCell(table, name, Element.ALIGN_LEFT, alt);
            addBodyCell(table, qty, Element.ALIGN_RIGHT, alt);
            addBodyCell(table, emb, Element.ALIGN_RIGHT, alt);
            addBodyCell(table, cx, Element.ALIGN_RIGHT, alt);
            addBodyCell(table, batch, Element.ALIGN_LEFT, alt);

            if (line.getQuantity() != null) totalQty = totalQty.add(line.getQuantity());
            try { totalBoxes = totalBoxes.add(new BigDecimal(cx)); } catch (Exception ignored) {}
            alt = !alt;
        }

        addSummaryCell(table, "", Element.ALIGN_CENTER, 3);
        addSummaryCell(table, "TOTAL DE EXPEDIÇÃO:", Element.ALIGN_RIGHT, 1);
        addSummaryCell(table, MoneyFormat.formatQty(totalQty), Element.ALIGN_RIGHT, 1);
        addSummaryCell(table, "—", Element.ALIGN_RIGHT, 1);
        addSummaryCell(table, MoneyFormat.formatPlain(totalBoxes), Element.ALIGN_RIGHT, 1);
        addSummaryCell(table, order.getLines().size() + " artigos", Element.ALIGN_LEFT, 1);
        return table;
    }

    private String calculatePackages(OrderLine line) {
        if (line.getProduct() == null) return "0";
        int pkgsPerBox = line.getProduct().getPackagesPerBox();
        int unitsPerPkg = line.getProduct().getUnitsPerPackage();
        if (pkgsPerBox <= 1 && unitsPerPkg <= 1) return "0";
        int divisor = unitsPerPkg > 0 ? unitsPerPkg : 1;
        BigDecimal packages = (line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity())
                .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
        return MoneyFormat.formatQty(packages);
    }

    private String calculateBoxes(OrderLine line) {
        if (line.getProduct() == null) return "0.00";
        int pkgsPerBox = line.getProduct().getPackagesPerBox();
        int unitsPerPkg = line.getProduct().getUnitsPerPackage();
        if (pkgsPerBox <= 1 && unitsPerPkg <= 1) return "0.00";
        long unitsPerBox = (long) pkgsPerBox * (unitsPerPkg > 0 ? unitsPerPkg : 1);
        if (unitsPerBox <= 0) return "0.00";
        BigDecimal boxes = (line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity())
                .divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
        return boxes.signum() == 0 ? "0,00" : MoneyFormat.formatQty(boxes);
    }

    private void addHeader(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addCheckboxCell(PdfPTable table, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase("[  ]", PdfTheme.tableBodyFont()));
        if (alt) cell.setBackgroundColor(PdfTheme.ROW_ALT);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int align, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, PdfTheme.tableBodyFont()));
        if (alt) cell.setBackgroundColor(PdfTheme.ROW_ALT);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private void addSummaryCell(PdfPTable table, String text, int align, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.boldFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.75f);
        cell.setHorizontalAlignment(align);
        cell.setColspan(colspan);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        PdfPCell c1 = new PdfPCell();
        c1.setBorder(PdfPCell.NO_BORDER);
        c1.addElement(new Paragraph("Separado por: _________________________________", PdfTheme.smallFont()));
        c1.addElement(new Paragraph("Data / Hora: ____/____/________  ____:____", PdfTheme.smallFont()));
        PdfPCell c2 = new PdfPCell();
        c2.setBorder(PdfPCell.NO_BORDER);
        c2.addElement(new Paragraph("Conferido por: ________________________________", PdfTheme.smallFont()));
        c2.addElement(new Paragraph("Viatura / Motorista: ____________________________", PdfTheme.smallFont()));
        table.addCell(c1);
        table.addCell(c2);
        return table;
    }
}
