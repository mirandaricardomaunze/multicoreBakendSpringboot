package mz.multicore.erp.modules.pickup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SA-C07: pure balance rules for paid material awaiting pickup ({@code docs/MATERIAL_POR_LEVANTAR_SPEC.md}). */
class PickupBalanceRulesTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static PickupBalanceRules.Request req(long line, String qty) {
        return new PickupBalanceRules.Request(line, bd(qty));
    }

    @Test
    void balanceSubtractsCreditsAndPickupsAndNeverGoesNegative() {
        assertThat(PickupBalanceRules.balance(bd("200"), bd("0"), bd("20"))).isEqualByComparingTo("180");
        assertThat(PickupBalanceRules.balance(bd("200"), bd("30"), bd("20"))).isEqualByComparingTo("150");
        assertThat(PickupBalanceRules.balance(bd("10"), bd("8"), bd("5"))).isEqualByComparingTo("0");
        assertThat(PickupBalanceRules.balance(null, null, null)).isEqualByComparingTo("0");
    }

    @Test
    void acceptsAHandoverWithinTheBalance() {
        Map<Long, BigDecimal> balances = Map.of(1L, bd("180"), 2L, bd("5"));
        PickupBalanceRules.validate(List.of(req(1, "20"), req(2, "0")), balances, Map.of());
        PickupBalanceRules.validate(List.of(req(1, "180"), req(2, "5")), balances, Map.of());
    }

    @Test
    void rejectsMoreThanTheBalanceWithAClearMessage() {
        assertThatThrownBy(() -> PickupBalanceRules.validate(List.of(req(1, "181")), Map.of(1L, bd("180")),
                Map.of(1L, "Cimento 42.5")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Só restam 180 de Cimento 42.5")
                .hasMessageContaining("181");
    }

    @Test
    void rejectsForeignDuplicateNegativeAndEmptyLines() {
        Map<Long, BigDecimal> balances = Map.of(1L, bd("10"));
        assertThatThrownBy(() -> PickupBalanceRules.validate(List.of(req(9, "1")), balances, Map.of()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("não pertence");
        assertThatThrownBy(() -> PickupBalanceRules.validate(List.of(req(1, "1"), req(1, "1")), balances, Map.of()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("duas vezes");
        assertThatThrownBy(() -> PickupBalanceRules.validate(List.of(req(1, "-1")), balances, Map.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PickupBalanceRules.validate(List.of(req(1, "0")), balances, Map.of()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> PickupBalanceRules.validate(List.of(), balances, Map.of()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void completesOnlyWhenEveryBalanceIsZero() {
        assertThat(PickupBalanceRules.allCollected(List.of(bd("0"), bd("0")))).isTrue();
        assertThat(PickupBalanceRules.allCollected(List.of(bd("0"), bd("0.001")))).isFalse();
    }
}
