package mz.multicore.erp.modules.platform.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.ValidPhoneMZ;
import mz.multicore.erp.modules.company.model.CompanyLegalForm;
import mz.multicore.erp.modules.company.model.MozambiqueProvince;
import mz.multicore.erp.modules.company.model.VatRegime;

import java.math.BigDecimal;

/**
 * Optional professional profile of a company: legal identity, tax registration, structured
 * address and contacts. Shared by the create, update and read contracts so the three never drift.
 * Every field is optional; blank strings are stored as {@code null}.
 */
public record CompanyProfile(
        @Size(max = 200, message = "O nome comercial não pode exceder 200 caracteres.")
        String tradeName,

        CompanyLegalForm legalForm,

        @Size(max = 60, message = "A matrícula não pode exceder 60 caracteres.")
        String registryNumber,

        @PositiveOrZero(message = "O capital social não pode ser negativo.")
        @Digits(integer = 17, fraction = 2, message = "O capital social tem um formato inválido.")
        BigDecimal shareCapital,

        @Size(max = 60, message = "O alvará não pode exceder 60 caracteres.")
        String activityLicense,

        @Pattern(regexp = "^$|^\\d{2,5}$", message = "O CAE deve ter entre 2 e 5 dígitos.")
        String activityCode,

        VatRegime vatRegime,

        @Size(max = 120, message = "A repartição de finanças não pode exceder 120 caracteres.")
        String taxOffice,

        @Size(max = 120, message = "O bairro não pode exceder 120 caracteres.")
        String neighborhood,

        @Size(max = 120, message = "A cidade não pode exceder 120 caracteres.")
        String city,

        MozambiqueProvince province,

        @Size(max = 20, message = "O código postal não pode exceder 20 caracteres.")
        String postalCode,

        @ValidPhoneMZ
        String secondaryPhone,

        @Size(max = 200, message = "O website não pode exceder 200 caracteres.")
        @Pattern(regexp = "^$|^(https?://)?[\\w-]+(\\.[\\w-]+)+(/\\S*)?$",
                message = "O website deve ter um formato válido (ex.: www.empresa.co.mz).")
        String website,

        @Size(max = 120, message = "O nome do responsável não pode exceder 120 caracteres.")
        String contactName,

        @ValidPhoneMZ
        String contactPhone
) {

    public static CompanyProfile empty() {
        return new CompanyProfile(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}
