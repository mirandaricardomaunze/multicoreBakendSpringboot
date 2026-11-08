package mz.multicore.erp.modules.platform.dto;

import mz.multicore.erp.architecture.validation.ValidNuit;
import mz.multicore.erp.architecture.validation.ValidPhoneMZ;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration of a new company by the superadmin. {@code address} is the street line; the rest of
 * the address, the legal identity and the extra contacts live in the optional {@code profile}.
 */
public record CreateCompanyRequest(
        @NotBlank(message = "O nome da empresa é obrigatório.")
        @Size(max = 200, message = "O nome da empresa não pode exceder 200 caracteres.")
        String name,

        @NotBlank(message = "O NUIT da empresa é obrigatório.")
        @ValidNuit
        String taxId,

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

    /** Base data only, without a professional profile. */
    public CreateCompanyRequest(String name, String taxId, String email, String address, String phone) {
        this(name, taxId, email, address, phone, null);
    }
}
