package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regras puras de domínio do artigo. {@code effectiveTaxRate} é a <b>fonte única</b> da taxa de IVA
 * para POS, fatura e encomenda — se divergir, o mesmo artigo passa a ser tributado de forma
 * diferente conforme o ecrã que o vende. Cenários IV-04..IV-06 de
 * docs/IVA_TAXA_CANONICA_SPEC.md.
 */
class ProductTest {

    private static Product productWith(TaxRate rate) {
        Product p = new Product();
        p.setName("Artigo");
        p.setUnitPrice(new BigDecimal("100"));
        p.setTaxRate(rate);
        return p;
    }

    private static TaxRate rate(String value) {
        TaxRate taxRate = new TaxRate();
        taxRate.setRate(value == null ? null : new BigDecimal(value));
        return taxRate;
    }

    @Test // IV-04
    void semTaxaNoCadastro_aplicaAPadrao() {
        assertEquals(TaxRates.STANDARD_VAT, productWith(null).effectiveTaxRate());
    }

    @Test // IV-05
    void comTaxaIsenta_devolveZero_eNaoAPadrao() {
        // O caso que causava o bug: isento não pode cair no fallback de 16%.
        assertEquals(0, productWith(rate("0.00")).effectiveTaxRate().compareTo(BigDecimal.ZERO));
    }

    @Test // IV-06
    void comTaxaReduzidaOuNormal_devolveADoCadastro() {
        assertEquals(0, productWith(rate("0.05")).effectiveTaxRate().compareTo(new BigDecimal("0.05")));
        assertEquals(0, productWith(rate("0.16")).effectiveTaxRate().compareTo(new BigDecimal("0.16")));
    }

    @Test // IV-07
    void taxaConfiguradaSemValor_naoRebenta_eCaiNaPadrao() {
        assertEquals(TaxRates.STANDARD_VAT, productWith(rate(null)).effectiveTaxRate());
    }
}
