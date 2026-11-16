package mz.multicore.erp.gui.platform;

import mz.multicore.erp.modules.company.model.MozambiqueProvince;
import mz.multicore.erp.modules.platform.dto.CompanyProfile;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Fields of the company wizard (docs/EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md CP-42..CP-44). */
class CompanyFormFieldsTest {

    @Test
    void cp42_registo_constroiOsPassosEValidaNomeENuit() {
        CompanyFormFields fields = new CompanyFormFields(null);
        assertNotNull(fields.identityStep(null));
        assertNotNull(fields.addressStep());
        assertNotNull(fields.legalStep());
        assertNotNull(new CompanyAdminStep().panel());

        assertEquals("Indique a razão social da empresa.", fields.validateIdentity());
        fields.name.setText("Sol, Lda");
        fields.taxId.setText("1234");
        assertEquals("O NUIT deve ter 9 dígitos.", fields.validateIdentity());
        fields.taxId.setText("400123456");
        assertNull(fields.validateIdentity());

        fields.bankName.setText("BCI");
        assertNotNull(fields.validateLegal(), "banco sem conta é recusado");
    }

    @Test
    void cp43_edicao_preencheOPerfilEBloqueiaONuit() {
        CompanyProfile profile = new CompanyProfile("Mercearia Sol", null, null, new BigDecimal("50000.00"),
                null, null, null, null, null, "Beira", MozambiqueProvince.SOFALA, null, null, null, null, null);
        PlatformCompanyDTO dto = new PlatformCompanyDTO(5L, "Sol, Lda", "400123456", null, "Av. 1", null,
                false, true, 2, profile);

        CompanyFormFields fields = new CompanyFormFields(dto);

        assertFalse(fields.taxId.isEditable());
        assertEquals("Mercearia Sol", fields.displayName());
        assertEquals(MozambiqueProvince.SOFALA, CompanyFormFields.selected(fields.province));
        assertEquals(new BigDecimal("50000"), fields.toUpdateRequest().profile().shareCapital());
        assertEquals("Beira", fields.toUpdateRequest().profile().city());
    }

    @Test
    void cp44_valoresEmFormatoMocambicanoSaoAceites() {
        assertEquals(new BigDecimal("50000.50"), CompanyFormFields.parseAmount("50.000,50"));
        assertEquals(new BigDecimal("50000.50"), CompanyFormFields.parseAmount("50 000,50 MT"));
        assertEquals(new BigDecimal("2500"), CompanyFormFields.parseAmount("2500"));
        assertNull(CompanyFormFields.parseAmount("abc"));
        assertNull(CompanyFormFields.parseAmount("-5"));
    }

    @Test
    void cp45_sugereUtilizadorSemAcentos() {
        CompanyAdminStep step = new CompanyAdminStep();
        step.suggestUsername("Padaria Açucena, Lda");
        assertEquals("admin.padaria", step.username.getText());
    }
}
