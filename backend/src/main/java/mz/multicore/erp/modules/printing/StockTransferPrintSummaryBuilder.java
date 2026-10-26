package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Construtor dos blocos de resumo/totais, carga e assinaturas dos documentos de transferência.
 */
@Component
public class StockTransferPrintSummaryBuilder {

    public PdfPTable buildProfessionalSummary(StockTransfer transfer) {
        PdfPTable wrapper = new PdfPTable(new float[]{67f, 33f});
        wrapper.setWidthPercentage(100);

        PdfPCell notes = new PdfPCell();
        notes.setBorder(PdfPCell.TOP);
        notes.setBorderColor(PdfTheme.BRAND);
        notes.setPaddingTop(8f);
        notes.addElement(new Paragraph("OBSERVAÇÕES", PdfTheme.tableHeaderFont()));
        notes.addElement(new Paragraph(StockTransferPrintRouteBuilder.valueOrLine(transfer.getNotes()), PdfTheme.bodyFont()));
        wrapper.addCell(notes);

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalBoxes = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        boolean hasWeight = false;

        if (transfer.getLines() != null) {
            for (StockTransferLine line : transfer.getLines()) {
                BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
                totalQuantity = totalQuantity.add(quantity);
                Product product = line.getProduct();
                totalValue = totalValue.add(StockTransferPrintLinesTableBuilder.effectiveUnitPrice(product, quantity).multiply(quantity));
                if (StockTransferPrintLinesTableBuilder.hasPackaging(product)) {
                    long unitsPerBox = (long) product.getPackagesPerBox() * product.getUnitsPerPackage();
                    totalBoxes = totalBoxes.add(quantity.divide(BigDecimal.valueOf(unitsPerBox), 3, RoundingMode.HALF_UP));
                }
                BigDecimal unitWeight = StockTransferPrintLinesTableBuilder.unitWeight(product);
                if (unitWeight != null) {
                    totalWeight = totalWeight.add(unitWeight.multiply(quantity));
                    hasWeight = true;
                }
            }
        }

        PdfPTable totals = new PdfPTable(new float[]{65f, 35f});
        int lineCount = transfer.getLines() != null ? transfer.getLines().size() : 0;
        addSummaryLine(totals, "Total de artigos (linhas)", String.valueOf(lineCount), false);
        addSummaryLine(totals, "Total de caixas", StockTransferPrintLinesTableBuilder.formatQty(totalBoxes), false);
        addSummaryLine(totals, "Quantidade total", StockTransferPrintLinesTableBuilder.formatQty(totalQuantity), false);
        addSummaryLine(totals, "PESO TOTAL (kg)", hasWeight ? StockTransferPrintLinesTableBuilder.formatQty(totalWeight.setScale(3, RoundingMode.HALF_UP)) : "Por aferir", true);
        addSummaryLine(totals, "VALOR TOTAL", MoneyFormat.format(totalValue.setScale(2, RoundingMode.HALF_UP)), true);

        PdfPCell totalsCell = new PdfPCell(totals);
        totalsCell.setBorder(PdfPCell.TOP);
        totalsCell.setBorderColor(PdfTheme.BRAND);
        totalsCell.setPaddingTop(5f);
        wrapper.addCell(totalsCell);
        return wrapper;
    }

    public Paragraph buildLoadSummary(List<StockTransferLine> lines) {
        if (lines == null || lines.isEmpty()) return null;
        return LoadSummaryRenderer.build(lines.stream()
                .map(line -> {
                    Product p = line.getProduct();
                    BigDecimal weight = p != null
                            ? (p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg())
                            : null;
                    int unitsPerBox = p != null ? p.getUnitsPerBox() : 1;
                    String name = p != null ? p.getName() : "Artigo";
                    return new LoadSummaryRenderer.Item(name, line.getQuantity(), weight, unitsPerBox);
                })
                .toList());
    }

    public PdfPTable buildProfessionalSignatureBlock() {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.addCell(professionalSignatureCell("EXPEDIDO POR (ARMAZÉM DE ORIGEM)"));
        table.addCell(professionalSignatureCell("TRANSPORTADOR"));
        table.addCell(professionalSignatureCell("RECEBIDO POR (ARMAZÉM DE DESTINO)"));
        return table;
    }

    public PdfPTable buildPickingSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        table.addCell(signatureCell("Separado por (Operador de Armazém)"));
        table.addCell(signatureCell("Conferido por (Encarregado / Gestor)"));
        return table;
    }

    private void addSummaryLine(PdfPTable table, String label, String value, boolean strong) {
        PdfPCell left = new PdfPCell(new Phrase(label, strong ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell right = new PdfPCell(new Phrase(value, strong ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        left.setBorder(strong ? PdfPCell.TOP | PdfPCell.BOTTOM : PdfPCell.BOTTOM);
        right.setBorder(strong ? PdfPCell.TOP | PdfPCell.BOTTOM : PdfPCell.BOTTOM);
        left.setBorderColor(PdfTheme.BORDER);
        right.setBorderColor(PdfTheme.BORDER);
        left.setPadding(5f);
        right.setPadding(5f);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private PdfPCell professionalSignatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingRight(18f);
        cell.addElement(new Paragraph(label, PdfTheme.tableHeaderFont()));
        cell.addElement(new Paragraph("Nome: ______________________________", PdfTheme.bodyFont()));
        cell.addElement(new Paragraph("Data: ____ / ____ / ________", PdfTheme.bodyFont()));
        return cell;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);

        Paragraph line = new Paragraph("____________________________", PdfTheme.bodyFont());
        Paragraph caption = new Paragraph(label, PdfTheme.smallFont());
        cell.addElement(line);
        cell.addElement(caption);
        return cell;
    }
}
