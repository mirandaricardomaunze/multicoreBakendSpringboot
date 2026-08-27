package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;

/**
 * Exports tabular data to PDF with the standard company header.
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

}
