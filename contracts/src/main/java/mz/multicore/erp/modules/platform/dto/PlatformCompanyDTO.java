package mz.multicore.erp.modules.platform.dto;

/** Superadmin view of a company: base data, professional profile, status and user count. */
public record PlatformCompanyDTO(
        Long id,
        String name,
        String taxId,
        String email,
        String address,
        String phone,
        boolean hasLogo,
        boolean active,
        long userCount,
        CompanyProfile profile
) {}
