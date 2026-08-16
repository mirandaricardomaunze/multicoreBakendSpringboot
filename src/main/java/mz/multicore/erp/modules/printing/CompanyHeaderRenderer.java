package mz.multicore.erp.modules.printing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;

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

        addLogo(cell, company);

        Paragraph name = new Paragraph(safe(company == null ? null : company.getName(), "Empresa"), PdfTheme.titleFont());
        name.setSpacingAfter(2f);
        cell.addElement(name);

        if (company != null) {
            if (company.getTaxId() != null) {
                cell.addElement(new Paragraph("NUIT: " + company.getTaxId(), PdfTheme.bodyFont()));
            }
            if (notBlank(company.getAddress())) {
                cell.addElement(new Paragraph(company.getAddress(), PdfTheme.bodyFont()));
            }
            if (notBlank(company.getPhone())) {
                cell.addElement(new Paragraph("Tel: " + company.getPhone(), PdfTheme.bodyFont()));
            }
            if (notBlank(company.getEmail())) {
                cell.addElement(new Paragraph(company.getEmail(), PdfTheme.bodyFont()));
            }
        }
        return cell;
    }

    /** Logótipo (se existir), escalado a uma altura fixa. À prova de falha: bytes inválidos → sem imagem. */
    private static void addLogo(PdfPCell cell, Company company) {
        if (company == null || company.getLogo() == null || company.getLogo().length == 0) return;
        try {
            Image logo = Image.getInstance(company.getLogo());
            logo.scaleToFit(150f, 50f);
            logo.setSpacingAfter(4f);
            cell.addElement(logo);
        } catch (Exception ignored) {
            // logótipo ilegível — o documento sai na mesma, sem imagem.
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
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
