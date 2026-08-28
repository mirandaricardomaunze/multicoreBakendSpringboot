package mz.multicore.erp.modules.licensing.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record AcceptLicenseRequest(
        @NotBlank String version,
        @NotBlank String sha256,
        @AssertTrue(message = "Confirme que possui poderes para representar a empresa.") boolean authorizedRepresentative,
        @NotBlank String declaration
) {
}
