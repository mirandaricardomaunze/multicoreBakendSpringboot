package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.platform.dto.CompanyProfile;

/** Copies the professional profile between the {@link Company} entity and its contract. */
final class CompanyProfileMapper {

    private CompanyProfileMapper() {}

    /** Replaces the stored profile with {@code profile}. Blank strings are stored as {@code null}. */
    static void apply(Company company, CompanyProfile profile) {
        CompanyProfile p = profile == null ? CompanyProfile.empty() : profile;
        company.setTradeName(clean(p.tradeName()));
        company.setLegalForm(p.legalForm());
        company.setRegistryNumber(clean(p.registryNumber()));
        company.setShareCapital(p.shareCapital());
        company.setActivityLicense(clean(p.activityLicense()));
        company.setActivityCode(clean(p.activityCode()));
        company.setVatRegime(p.vatRegime());
        company.setTaxOffice(clean(p.taxOffice()));
        company.setNeighborhood(clean(p.neighborhood()));
        company.setCity(clean(p.city()));
        company.setProvince(p.province());
        company.setPostalCode(clean(p.postalCode()));
        company.setSecondaryPhone(clean(p.secondaryPhone()));
        company.setWebsite(clean(p.website()));
        company.setContactName(clean(p.contactName()));
        company.setContactPhone(clean(p.contactPhone()));
    }

    static CompanyProfile toProfile(Company company) {
        return new CompanyProfile(
                company.getTradeName(), company.getLegalForm(), company.getRegistryNumber(),
                company.getShareCapital(), company.getActivityLicense(), company.getActivityCode(),
                company.getVatRegime(), company.getTaxOffice(), company.getNeighborhood(),
                company.getCity(), company.getProvince(), company.getPostalCode(),
                company.getSecondaryPhone(), company.getWebsite(), company.getContactName(),
                company.getContactPhone());
    }

    static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
