package com.phcpro.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.modules.company.model.Company;

/**
 * Builds the document header block: company name, NUIT, contact, address.
 * One renderer used by every printable document so a company-detail change
 * applies everywhere (DRY).
 */
public final class CompanyHeaderRenderer {

    private CompanyHeaderRenderer() {}

    public static PdfPTable build(Company company, String documentTitle, String documentNumber) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{60f, 40f});
        } catch (Exception ignored) {}
        table.setSpacingAfter(14f);

        table.addCell(companyCell(company));
        table.addCell(documentCell(documentTitle, documentNumber));
        return table;
    }

    private static PdfPCell companyCell(Company company) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);

        Paragraph name = new Paragraph(safe(company == null ? null : company.getName(), "Empresa"), PdfTheme.titleFont());
        name.setSpacingAfter(2f);
        cell.addElement(name);

        if (company != null) {
            if (company.getTaxId() != null) {
                cell.addElement(new Paragraph("NUIT: " + company.getTaxId(), PdfTheme.bodyFont()));
            }
            if (company.getAddress() != null && !company.getAddress().isBlank()) {
                cell.addElement(new Paragraph(company.getAddress(), PdfTheme.bodyFont()));
            }
            if (company.getEmail() != null && !company.getEmail().isBlank()) {
                cell.addElement(new Paragraph(company.getEmail(), PdfTheme.bodyFont()));
            }
        }
        return cell;
    }

    private static PdfPCell documentCell(String title, String number) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph titleP = new Paragraph(title == null ? "" : title.toUpperCase(), PdfTheme.titleFont());
        titleP.setAlignment(Element.ALIGN_RIGHT);
        titleP.setSpacingAfter(2f);
        cell.addElement(titleP);

        if (number != null) {
            Paragraph num = new Paragraph(new Phrase(new Chunk(number, PdfTheme.subtitleFont())));
            num.setAlignment(Element.ALIGN_RIGHT);
            cell.addElement(num);
        }
        return cell;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
