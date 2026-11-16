package mz.multicore.erp.modules.printing;

import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import mz.multicore.erp.modules.company.model.Company;

/**
 * Issuer block of the commercial A4 documents (invoice, order, quotation): logo, name in brand
 * colour, registered name, legal registration, address, NUIT/phones and e-mail/website.
 * One implementation for the three documents, which used to keep identical copies.
 */
final class CompanyDetailsBlock {

    private CompanyDetailsBlock() {}

    static void addTo(PdfPCell cell, Company company) {
        addLogo(cell, company);
        Paragraph name = new Paragraph(CompanyIdentityText.displayName(company, "Empresa").toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, PdfTheme.BRAND));
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

    /** Logo scaled to a fixed box. An unreadable image is skipped: the document still prints. */
    private static void addLogo(PdfPCell cell, Company company) {
        if (company == null || company.getLogo() == null || company.getLogo().length == 0) return;
        try {
            Image logo = Image.getInstance(company.getLogo());
            logo.scaleToFit(140f, 45f);
            logo.setSpacingAfter(4f);
            cell.addElement(logo);
        } catch (Exception ignored) {
            // Unreadable logo: print without it.
        }
    }

    private static void add(PdfPCell cell, String text, com.lowagie.text.Font font) {
        if (text != null && !text.isBlank()) cell.addElement(new Paragraph(text, font));
    }
}
