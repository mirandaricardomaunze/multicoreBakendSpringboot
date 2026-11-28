package mz.multicore.erp.modules.printing;

import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;

/**
 * Issuer block of the commercial A4 documents (invoice, order, quotation): logo, name in brand
 * colour, registered name, legal registration, address, NUIT/phones and e-mail/website.
 * One implementation for the three documents, which used to keep identical copies.
 */
final class CompanyDetailsBlock {

    private CompanyDetailsBlock() {}

    static void addTo(PdfPCell cell, Company company) {
        addTo(cell, company, DocumentTemplateDTO.defaults());
    }

    /** Bloco do emitente com o modelo da empresa: logótipo só quando à esquerda, nome na cor de destaque. */
    static void addTo(PdfPCell cell, Company company, DocumentTemplateDTO template) {
        DocumentTemplateBlocks.addLogoIfAt(cell, DocumentLogoPosition.LEFT, company, template, DocumentTemplateBlocks.LogoSize.COMPACT);
        Paragraph name = new Paragraph(CompanyIdentityText.displayName(company, "Empresa").toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, DocumentTemplateBlocks.accent(template)));
        name.setSpacingAfter(2f);
        cell.addElement(name);
        if (company == null) return;

        add(cell, CompanyIdentityText.legalNameLine(company), PdfTheme.bodyFont());
        add(cell, CompanyIdentityText.legalLine(company),
                FontFactory.getFont(FontFactory.HELVETICA, 7.5f, PdfTheme.MUTED));
        add(cell, company.formattedAddress(), PdfTheme.bodyFont());
        add(cell, CompanyIdentityText.taxAndPhoneLine(company), PdfTheme.bodyFont());
        add(cell, CompanyIdentityText.emailAndWebLine(company), PdfTheme.bodyFont());
    }

    private static void add(PdfPCell cell, String text, com.lowagie.text.Font font) {
        if (text != null && !text.isBlank()) cell.addElement(new Paragraph(text, font));
    }
}
