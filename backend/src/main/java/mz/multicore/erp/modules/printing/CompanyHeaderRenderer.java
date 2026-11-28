package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;
import mz.multicore.erp.modules.printing.DocumentTemplateBlocks.LogoSize;

import java.awt.Color;

/**
 * Builds the document header block: company name, NUIT, contact, address.
 * One renderer used by every printable document so a company-detail change
 * applies everywhere (DRY).
 */
public final class CompanyHeaderRenderer {

    private CompanyHeaderRenderer() {}

    public static PdfPTable build(Company company, String documentTitle, String documentNumber) {
        return build(company, documentTitle, documentNumber, null, null);
    }

    public static PdfPTable build(Company company, String documentTitle, String documentNumber, String statusLabel) {
        return build(company, documentTitle, documentNumber, null, statusLabel);
    }

    public static PdfPTable build(Company company, String documentTitle, String documentNumber, String copyLabel, String statusLabel) {
        return build(company, documentTitle, documentNumber, copyLabel, statusLabel, 55f, 45f);
    }

    public static PdfPTable build(Company company, String documentTitle, String documentNumber,
                                  String copyLabel, String statusLabel,
                                  float companyWidth, float documentWidth) {
        return build(company, documentTitle, documentNumber, copyLabel, statusLabel,
                new float[]{companyWidth, documentWidth}, DocumentTemplateDTO.defaults());
    }

    /** Cabeçalho com o modelo da empresa: posição do logótipo e cor de destaque nos títulos (DT-04). */
    public static PdfPTable build(Company company, String documentTitle, String documentNumber,
                                  String copyLabel, String statusLabel, DocumentTemplateDTO template) {
        return build(company, documentTitle, documentNumber, copyLabel, statusLabel, new float[]{55f, 45f}, template);
    }

    private static PdfPTable build(Company company, String documentTitle, String documentNumber,
                                   String copyLabel, String statusLabel,
                                   float[] columnWidths, DocumentTemplateDTO template) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(columnWidths);
        } catch (Exception ignored) {}
        table.setSpacingAfter(14f);

        Color accent = DocumentTemplateBlocks.accent(template);
        DocumentTemplateBlocks.addCenteredLogoRow(table, 2, company, template, LogoSize.HEADER);
        table.addCell(companyCell(company, template, accent));
        table.addCell(documentCell(company, template, accent, documentTitle, documentNumber, copyLabel, statusLabel));
        return table;
    }

    private static PdfPCell companyCell(Company company, DocumentTemplateDTO template, Color accent) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);

        DocumentTemplateBlocks.addLogoIfAt(cell, DocumentLogoPosition.LEFT, company, template, LogoSize.HEADER);

        Paragraph name = new Paragraph(CompanyIdentityText.displayName(company, "Empresa"), titleFont(accent));
        name.setSpacingAfter(2f);
        cell.addElement(name);

        if (company != null) {
            addLine(cell, CompanyIdentityText.legalNameLine(company), PdfTheme.bodyFont());
            if (company.getTaxId() != null) {
                cell.addElement(new Paragraph("NUIT: " + company.getTaxId(), PdfTheme.bodyFont()));
            }
            addLine(cell, CompanyIdentityText.legalLine(company), PdfTheme.smallFont());
            addLine(cell, company.formattedAddress(), PdfTheme.bodyFont());
            String phones = CompanyIdentityText.phones(company);
            if (phones != null) {
                cell.addElement(new Paragraph("Tel: " + phones, PdfTheme.bodyFont()));
            }
            addLine(cell, company.getEmail(), PdfTheme.bodyFont());
            addLine(cell, company.getWebsite(), PdfTheme.bodyFont());
        }
        return cell;
    }

    private static Font titleFont(Color accent) {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, accent);
    }

    private static void addLine(PdfPCell cell, String text, com.lowagie.text.Font font) {
        if (text != null && !text.isBlank()) cell.addElement(new Paragraph(text.trim(), font));
    }

    private static PdfPCell documentCell(Company company, DocumentTemplateDTO template, Color accent,
                                         String title, String number, String copyLabel, String statusLabel) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        DocumentTemplateBlocks.addLogoIfAt(cell, DocumentLogoPosition.RIGHT, company, template, LogoSize.HEADER);

        Paragraph titleP = new Paragraph(title == null ? "" : title.toUpperCase(), titleFont(accent));
        titleP.setAlignment(Element.ALIGN_RIGHT);
        titleP.setSpacingAfter(2f);
        cell.addElement(titleP);

        if (number != null && !number.isBlank()) {
            Paragraph num = new Paragraph(new Phrase(new Chunk(number, PdfTheme.subtitleFont())));
            num.setAlignment(Element.ALIGN_RIGHT);
            cell.addElement(num);
        }
        if (copyLabel != null && !copyLabel.isBlank()) {
            Paragraph copy = new Paragraph(new Phrase(new Chunk(copyLabel.trim(), PdfTheme.copyBadgeFont())));
            copy.setAlignment(Element.ALIGN_RIGHT);
            copy.setSpacingBefore(3f);
            cell.addElement(copy);
        }
        if (statusLabel != null && !statusLabel.isBlank()) {
            Paragraph st = new Paragraph(new Phrase(new Chunk("Estado: " + statusLabel.trim(), PdfTheme.bodyFont())));
            st.setAlignment(Element.ALIGN_RIGHT);
            st.setSpacingBefore(3f);
            cell.addElement(st);
        }
        return cell;
    }

}
