package mz.multicore.erp.modules.pricing.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** SA-C03: pure price-list rules ({@code docs/TABELAS_PRECO_SPEC.md}). */
class PriceListRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    private static PriceListRules.Tier tier(String min, String price) {
        return new PriceListRules.Tier(new BigDecimal(min), new BigDecimal(price));
    }

    @Test
    void validadeDaTabela() {
        assertThat(PriceListRules.isApplicable(true, null, null, TODAY)).isTrue();
        assertThat(PriceListRules.isApplicable(false, null, null, TODAY)).isFalse();
        assertThat(PriceListRules.isApplicable(true, TODAY, TODAY, TODAY)).as("limites inclusivos").isTrue();
        assertThat(PriceListRules.isApplicable(true, TODAY.plusDays(1), null, TODAY)).isFalse();
        assertThat(PriceListRules.isApplicable(true, null, TODAY.minusDays(1), TODAY)).isFalse();
    }

    @Test
    void escolheOMaiorEscalaoAtingido() {
        List<PriceListRules.Tier> tiers = List.of(tier("0", "600"), tier("40", "560"), tier("200", "540"));
        assertThat(PriceListRules.pickTier(tiers, new BigDecimal("1")).orElseThrow().price()).isEqualByComparingTo("600");
        assertThat(PriceListRules.pickTier(tiers, new BigDecimal("40")).orElseThrow().price()).isEqualByComparingTo("560");
        assertThat(PriceListRules.pickTier(tiers, new BigDecimal("199")).orElseThrow().price()).isEqualByComparingTo("560");
        assertThat(PriceListRules.pickTier(tiers, new BigDecimal("500")).orElseThrow().price()).isEqualByComparingTo("540");
    }

    @Test
    void semEscalaoAtingidoNaoHaPrecoDeTabela() {
        List<PriceListRules.Tier> tiers = List.of(tier("10", "90"));
        assertThat(PriceListRules.pickTier(tiers, new BigDecimal("9.999"))).isEmpty();
        assertThat(PriceListRules.pickTier(List.of(), BigDecimal.ONE)).isEmpty();
        assertThat(PriceListRules.pickTier(tiers, null)).isEmpty();
    }
}
