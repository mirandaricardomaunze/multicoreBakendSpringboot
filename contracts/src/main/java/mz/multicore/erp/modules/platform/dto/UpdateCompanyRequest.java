package mz.multicore.erp.modules.platform.dto;

import mz.multicore.erp.architecture.validation.ValidPhoneMZ;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Superadmin edit of a company. The NUIT is immutable. A {@code null} profile leaves the stored
 * profile untouched; a non-null profile replaces it.
 */
public record UpdateCompanyRequest(
        @NotBlank(message = "O nome da empresa é obrigatório.")
        @Size(max = 200, message = "O nome da empresa não pode exceder 200 caracteres.")
        String name,

        @Email(message = "O email da empresa deve ter um formato válido.")
        @Size(max = 200, message = "O email não pode exceder 200 caracteres.")
        String email,

        @Size(max = 300, message = "O endereço não pode exceder 300 caracteres.")
        String address,

        @ValidPhoneMZ
        String phone,

        @Valid
        CompanyProfile profile
) {

    /** Base data only; the stored profile is kept. */
    public UpdateCompanyRequest(String name, String email, String address, String phone) {
        this(name, email, address, phone, null);
    }
}
