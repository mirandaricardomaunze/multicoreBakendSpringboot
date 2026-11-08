package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CompanyLegalForm;
import mz.multicore.erp.modules.company.model.VatRegime;

import java.util.ArrayList;
import java.util.List;

/**
 * Text lines that identify the issuing company on a document. Every renderer takes these lines
 * from here so a legal or contact detail is worded the same way on every document. Each method
 * returns {@code null} when there is nothing to print, so callers never print empty lines.
 */
public final class CompanyIdentityText {

    private CompanyIdentityText() {}

    /** Registered name, shown under the trade name when the two differ. */
    public static String legalNameLine(Company company) {
        if (company == null || !has(company.getTradeName()) || !has(company.getName())) return null;
        if (company.getTradeName().trim().equalsIgnoreCase(company.getName().trim())) return null;
        return company.getName().trim();
    }

    /**
     * Legal registration: form, registry number, share capital, licence, CAE and, when it is not
     * the normal regime, the VAT regime (an exempt issuer must say so on its invoices).
     */
    public static String legalLine(Company company) {
        if (company == null) return null;
        List<String> parts = new ArrayList<>();
        CompanyLegalForm form = company.getLegalForm();
        if (form != null && form != CompanyLegalForm.OTHER) {
            parts.add(form.abbreviation() != null ? form.abbreviation() : form.label());
        }
        if (has(company.getRegistryNumber())) parts.add("Matrícula n.º " + company.getRegistryNumber().trim());
        if (company.getShareCapital() != null && company.getShareCapital().signum() > 0) {
            parts.add("Capital social " + MoneyFormat.format(company.getShareCapital()));
        }
        if (has(company.getActivityLicense())) parts.add("Alvará n.º " + company.getActivityLicense().trim());
        if (has(company.getActivityCode())) parts.add("CAE " + company.getActivityCode().trim());
        VatRegime vat = company.getVatRegime();
        if (vat != null && vat != VatRegime.NORMAL) parts.add(vat.label());
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /** "NUIT: … · Tel.: main / secondary". Always printed, with a dash when the NUIT is missing. */
    public static String taxAndPhoneLine(Company company) {
        String line = "NUIT: " + (company != null && has(company.getTaxId()) ? company.getTaxId().trim() : "—");
        String phones = phones(company);
        return phones == null ? line : line + " · Tel.: " + phones;
    }

    /** "E-mail: … · website", or {@code null}. */
    public static String emailAndWebLine(Company company) {
        if (company == null) return null;
        List<String> parts = new ArrayList<>();
        if (has(company.getEmail())) parts.add("E-mail: " + company.getEmail().trim());
        if (has(company.getWebsite())) parts.add(company.getWebsite().trim());
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /** Main and secondary phone joined by " / ", or {@code null}. */
    public static String phones(Company company) {
        if (company == null) return null;
        List<String> parts = new ArrayList<>();
        if (has(company.getPhone())) parts.add(company.getPhone().trim());
        if (has(company.getSecondaryPhone())) parts.add(company.getSecondaryPhone().trim());
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    /** Document title name: trade name when set, otherwise the registered name. */
    public static String displayName(Company company, String fallback) {
        if (company == null) return fallback;
        String name = company.displayName();
        return has(name) ? name.trim() : fallback;
    }

    private static boolean has(String value) {
        return value != null && !value.isBlank();
    }
}
