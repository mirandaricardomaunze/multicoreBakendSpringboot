package mz.multicore.erp.modules.licensing.dto;

public record LicenseAcceptanceDTO(
        Long id,
        Long companyId,
        String licenseVersion,
        String licenseSha256,
        String acceptedBy,
        String acceptedAt,
        String clientVersion
) {
}
