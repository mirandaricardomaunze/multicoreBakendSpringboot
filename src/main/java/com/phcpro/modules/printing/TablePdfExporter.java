package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.modules.company.model.Company;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Exports any tabular dataset to PDF with the standard company header.
 * Two entry points: render(...) for in-memory String[][], and renderFromSwing(...)
 * to pipe straight from a JTable.
 */
public final class TablePdfExporter {

    private TablePdfExporter() {}

    public static byte[] render(Company company, String title, String[] headers, String[][] rows) {
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Relatório", title));
            doc.add(buildTable(headers, rows));
            doc.add(PdfDocumentBuilder.spacer(8f));
            Paragraph total = new Paragraph("Total de registos: " + rows.length, PdfTheme.smallFont());
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);
        });
    }

    public static byte[] renderFromSwing(Company company, String title, JTable table) {
        String[][] data = extract(table);
        String[] headers = extractHeaders(table);
        return render(company, title, headers, data);
    }

    private static PdfPTable buildTable(String[] headers, String[][] rows) {
        PdfPTable t = new PdfPTable(headers.length);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6f);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PdfTheme.tableHeaderFont()));
            cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
            cell.setBorderColor(PdfTheme.BORDER);
            cell.setPadding(5f);
            t.addCell(cell);
        }
        for (String[] row : rows) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, PdfTheme.bodyFont()));
                cell.setBorderColor(PdfTheme.BORDER);
                cell.setPadding(4f);
                t.addCell(cell);
            }
        }
        return t;
    }

    private static String[] extractHeaders(JTable table) {
        TableColumnModel cols = table.getColumnModel();
        String[] headers = new String[cols.getColumnCount()];
        for (int i = 0; i < cols.getColumnCount(); i++) {
            Object name = cols.getColumn(i).getHeaderValue();
            headers[i] = name == null ? "" : name.toString();
        }
        return headers;
    }

    private static String[][] extract(JTable table) {
        TableModel model = table.getModel();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();
        String[][] data = new String[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                Object v = model.getValueAt(r, c);
                data[r][c] = v == null ? "" : v.toString();
            }
        }
        return data;
    }
}
