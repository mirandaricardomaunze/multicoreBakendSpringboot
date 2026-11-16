package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.units.model.UnitDimension;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SA-C02: pure conversions and the unit catalogue ({@code docs/UNIDADES_MEDIDA_SPEC.md}). */
class UnitConversionRulesTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void factorFixoNaMesmaGrandeza() {
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.MASS, bd("0.001"), UnitDimension.MASS, bd("1")))
                .isEqualByComparingTo("0.001");
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.MASS, bd("1000"), UnitDimension.MASS, bd("1")))
                .isEqualByComparingTo("1000");
        // Base in tonnes: 1 kg = 0.001 t.
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.MASS, bd("1"), UnitDimension.MASS, bd("1000")))
                .isEqualByComparingTo("0.001");
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.VOLUME, bd("1000"), UnitDimension.VOLUME, bd("1")))
                .isEqualByComparingTo("1000");
    }

    @Test
    void semFactorFixoEntreGrandezasOuEmbalagens() {
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.VOLUME, bd("1000"), UnitDimension.MASS, bd("1"))).isNull();
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.PACKAGE, null, UnitDimension.PACKAGE, null)).isNull();
        assertThat(UnitConversionRules.fixedFactor(UnitDimension.PACKAGE, null, UnitDimension.COUNT, bd("1"))).isNull();
    }

    @Test
    void factorIndicadoTemDeSerPositivo() {
        assertThatThrownBy(() -> UnitConversionRules.requireValidFactor(null, 0, "Saco", "Unidade"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> UnitConversionRules.requireValidFactor(bd("0"), 3, "Saco", "Quilograma"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> UnitConversionRules.requireValidFactor(bd("-5"), 3, "Saco", "Quilograma"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void baseInteiraExigeFactorInteiro() {
        assertThat(UnitConversionRules.requireValidFactor(bd("40"), 0, "Palete", "Saco")).isEqualByComparingTo("40");
        assertThatThrownBy(() -> UnitConversionRules.requireValidFactor(bd("2.5"), 0, "Palete", "Unidade"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inteiro");
        assertThat(UnitConversionRules.requireValidFactor(bd("1.6"), 3, "Metro cúbico", "Tonelada"))
                .isEqualByComparingTo("1.6");
    }

    @Test
    void converteParaBase() {
        // Three pallets of 40 bags.
        assertThat(UnitConversionRules.toBase(bd("3"), bd("40"), 0, "Saco")).isEqualByComparingTo("120");
        // 1.5 pallets of 40 = 60 bags (whole, accepted).
        assertThat(UnitConversionRules.toBase(bd("1.5"), bd("40"), 0, "Saco")).isEqualByComparingTo("60");
        // 250 g = 0.25 kg.
        assertThat(UnitConversionRules.toBase(bd("250"), bd("0.001"), 3, "Quilograma")).isEqualByComparingTo("0.250");
    }

    @Test
    void recusaFraccoesQueABaseNaoAceita() {
        assertThatThrownBy(() -> UnitConversionRules.toBase(bd("0.01"), bd("40"), 0, "Saco"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("casas decimais");
        assertThatThrownBy(() -> UnitConversionRules.toBase(bd("0"), bd("40"), 0, "Saco"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void catalogoDoSistemaTemCodigosUnicosECoerentes() {
        Set<String> codes = new HashSet<>();
        for (SystemUnitCatalog.Definition def : SystemUnitCatalog.units()) {
            assertThat(codes.add(def.code())).as("código repetido: %s", def.code()).isTrue();
            assertThat(def.code()).matches("[A-Z0-9_]{1,20}");
            if (def.dimension().hasFixedConversion()) {
                assertThat(def.dimensionFactor()).as(def.code()).isNotNull().isPositive();
            } else {
                assertThat(def.dimensionFactor()).as(def.code()).isNull();
            }
        }
        for (UnitDimension d : UnitDimension.values()) {
            if (d.hasFixedConversion()) {
                assertThat(codes).as("referência de %s", d).contains(d.referenceCode());
            }
        }
        assertThat(codes).contains("UN", "KG", "SACO", "PALETE", "M3", "BARRA", "GRADE", "BOTIJA", "TABULEIRO");
    }
}
