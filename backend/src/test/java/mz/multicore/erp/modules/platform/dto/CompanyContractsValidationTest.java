package mz.multicore.erp.modules.platform.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mz.multicore.erp.modules.company.model.CompanyLegalForm;
import mz.multicore.erp.modules.company.model.MozambiqueProvince;
import mz.multicore.erp.modules.company.model.VatRegime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Bean validation of the company contracts (docs/EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md CP-20..CP-23). */
class CompanyContractsValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static CompanyProfile profile(String cae, String website, String contactPhone, BigDecimal capital) {
        return new CompanyProfile("Sol", CompanyLegalForm.PRIVATE_LIMITED, "100", capital, "ALV", cae,
                VatRegime.NORMAL, "DAF", "Bairro", "Maputo", MozambiqueProvince.MAPUTO_CITY, "1100",
                null, website, "Ana", contactPhone);
    }

    private Set<String> fieldsWithErrors(Object bean) {
        return validator.validate(bean).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void cp20_perfilValidoEPerfilVazioPassam() {
        assertTrue(fieldsWithErrors(profile("47110", "https://www.sol.co.mz/contactos", "+258 84 333 4444",
                new BigDecimal("50000.00"))).isEmpty());
        assertTrue(fieldsWithErrors(CompanyProfile.empty()).isEmpty());
        assertTrue(fieldsWithErrors(profile("", "", "", null)).isEmpty(), "campos opcionais vazios são aceites");
    }

    @Test
    void cp21_caeWebsiteTelefoneECapitalInvalidosSaoRejeitados() {
        Set<String> errors = fieldsWithErrors(profile("47A", "não é um site", "123", new BigDecimal("-1")));

        assertEquals(Set.of("activityCode", "website", "contactPhone", "shareCapital"), errors);
    }

    @Test
    void cp22_perfilInvalidoDentroDoPedidoDeCriacaoTambemFalha() {
        CreateCompanyRequest request = new CreateCompanyRequest("Sol", "400123456", null, null, null,
                profile("1", null, null, null));

        assertEquals(Set.of("profile.activityCode"), fieldsWithErrors(request));
    }

    @Test
    void cp23_edicaoValidaEmailETelefoneComoACriacao() {
        UpdateCompanyRequest request = new UpdateCompanyRequest("Sol", "sem-arroba", null, "abc");

        assertEquals(Set.of("email", "phone"), fieldsWithErrors(request));
    }
}
