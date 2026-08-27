package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.io.ByteArrayOutputStream;

/** Exportador local de tabelas Swing; não conhece entidades nem serviços do backend. */
public final class TablePdfExporter {

    private TablePdfExporter() {
    }

    public static byte[] renderFromSwing(String title, JTable table) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(title == null ? "Relatório" : title,
                    new Font(Font.HELVETICA, 16, Font.BOLD)));
            document.add(new Paragraph(" "));
            document.add(buildTable(table));
            Paragraph total = new Paragraph("Total de registos: " + table.getRowCount());
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF: " + exception.getMessage(), exception);
        }
    }

    private static PdfPTable buildTable(JTable table) {
        int columns = table.getColumnCount();
        PdfPTable pdfTable = new PdfPTable(columns);
        pdfTable.setWidthPercentage(100);
        for (int column = 0; column < columns; column++) {
            PdfPCell cell = new PdfPCell(new Phrase(table.getColumnName(column),
                    new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(new Color(37, 99, 235));
            cell.setPadding(5);
            pdfTable.addCell(cell);
        }
        TableModel model = table.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int column = 0; column < columns; column++) {
                Object value = model.getValueAt(row, column);
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value.toString(),
                        new Font(Font.HELVETICA, 8)));
                cell.setPadding(4);
                pdfTable.addCell(cell);
            }
        }
        return pdfTable;
    }
}
