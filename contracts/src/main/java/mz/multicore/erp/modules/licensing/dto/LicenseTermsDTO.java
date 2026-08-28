package mz.multicore.erp.modules.licensing.dto;

public record LicenseTermsDTO(
        String version,
        String title,
        String content,
        String sha256,
        boolean accepted,
        String acceptedBy,
        String acceptedAt
) {
}
