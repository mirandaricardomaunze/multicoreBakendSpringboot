package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CompanyLegalForm;
import mz.multicore.erp.modules.company.model.MozambiqueProvince;
import mz.multicore.erp.modules.company.model.VatRegime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Wording of the issuer identity on documents (docs/EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md CP-10..CP-14). */
class CompanyIdentityTextTest {

    private static Company company() {
        Company c = new Company();
        c.setName("Sol Comércio, Lda");
        c.setTaxId("400777001");
        return c;
    }

    @Test
    void cp10_empresaAntigaSemPerfil_mantemOsDadosDeSempre() {
        Company c = company();
        c.setAddress("Av. 24 de Julho 1500, Maputo");
        c.setPhone("+258 84 123 4567");

        assertEquals("Sol Comércio, Lda", CompanyIdentityText.displayName(c, "Empresa"));
        assertNull(CompanyIdentityText.legalNameLine(c));
        assertNull(CompanyIdentityText.legalLine(c));
        assertEquals("Av. 24 de Julho 1500, Maputo", c.formattedAddress());
        assertEquals("NUIT: 400777001 · Tel.: +258 84 123 4567", CompanyIdentityText.taxAndPhoneLine(c));
        assertNull(CompanyIdentityText.emailAndWebLine(c));
    }

    @Test
    void cp11_nomeComercial_eTituloERazaoSocialFicaPorBaixo() {
        Company c = company();
        c.setTradeName("Mercearia Sol");

        assertEquals("Mercearia Sol", CompanyIdentityText.displayName(c, "Empresa"));
        assertEquals("Sol Comércio, Lda", CompanyIdentityText.legalNameLine(c));

        c.setTradeName("sol comércio, lda");
        assertNull(CompanyIdentityText.legalNameLine(c), "nome comercial igual à razão social não se repete");
    }

    @Test
    void cp12_linhaLegal_juntaFormaMatriculaCapitalAlvaraCaeERegimeEspecial() {
        Company c = company();
        c.setLegalForm(CompanyLegalForm.PRIVATE_LIMITED);
        c.setRegistryNumber("100987654");
        c.setShareCapital(new BigDecimal("50000.00"));
        c.setActivityLicense("ALV-114");
        c.setActivityCode("47110");
        c.setVatRegime(VatRegime.EXEMPT);

        String line = CompanyIdentityText.legalLine(c);

        assertTrue(line.startsWith("Lda · Matrícula n.º 100987654 · Capital social "), line);
        assertTrue(line.contains("50.000,00"), line);
        assertTrue(line.endsWith("Alvará n.º ALV-114 · CAE 47110 · Isento de IVA"), line);

        c.setVatRegime(VatRegime.NORMAL);
        assertFalse(CompanyIdentityText.legalLine(c).contains("IVA"), "o regime normal não se imprime");
    }

    @Test
    void cp13_moradaEstruturada_omiteProvinciaQueRepeteACidade() {
        Company c = company();
        c.setAddress("Av. Julius Nyerere 1200");
        c.setNeighborhood("Polana Cimento");
        c.setCity("Maputo");
        c.setPostalCode("1100");
        c.setProvince(MozambiqueProvince.MAPUTO_CITY);
        assertEquals("Av. Julius Nyerere 1200, Polana Cimento, 1100 Maputo", c.formattedAddress());

        c.setCity("Matola");
        c.setPostalCode(null);
        c.setProvince(MozambiqueProvince.MAPUTO_PROVINCE);
        assertEquals("Av. Julius Nyerere 1200, Polana Cimento, Matola, Maputo Província", c.formattedAddress());

        Company empty = company();
        assertNull(empty.formattedAddress());
    }

    @Test
    void cp14_contactos_juntamTelefoneSecundarioEWebsite() {
        Company c = company();
        c.setPhone("+258 84 123 4567");
        c.setSecondaryPhone("+258 21 300 400");
        c.setEmail("geral@sol.co.mz");
        c.setWebsite("www.sol.co.mz");

        assertEquals("NUIT: 400777001 · Tel.: +258 84 123 4567 / +258 21 300 400",
                CompanyIdentityText.taxAndPhoneLine(c));
        assertEquals("E-mail: geral@sol.co.mz · www.sol.co.mz", CompanyIdentityText.emailAndWebLine(c));
    }
}
