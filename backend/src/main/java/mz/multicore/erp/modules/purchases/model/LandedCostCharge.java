package mz.multicore.erp.modules.purchases.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Uma linha de custo de importação (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1, §4.2): tipo, moeda e câmbio,
 * valor sem IVA na moeda da linha e, quando o serviço tem IVA, a taxa em percentagem.
 *
 * <p>Divide o valor entre o que entra no custo dos artigos e o que vai a IVA dedutível: os tipos capitalizáveis
 * levam o valor sem IVA ao custo e o IVA do serviço a dedutível; no {@code IVA_ADUANEIRO} o valor todo é imposto e
 * a taxa não se aplica. Tudo em moeda base com 2 casas HALF_UP.
 */
public record LandedCostCharge(LandedCostType type, DocumentCurrency currency, BigDecimal amount,
                               BigDecimal vatRatePercent) {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public LandedCostCharge {
        if (type == null) throw new BusinessRuleException("Indique o tipo da linha de custo de importação.");
        if (currency == null) throw new BusinessRuleException("Indique a moeda e o câmbio da linha de custo.");
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("O valor da linha de custo de importação tem de ser maior que zero.");
        }
        if (vatRatePercent != null && vatRatePercent.signum() < 0) {
            throw new BusinessRuleException("A taxa de IVA da linha de custo não pode ser negativa.");
        }
    }

    /** IVA na moeda da linha: o valor todo no IVA aduaneiro, valor × taxa nos serviços com IVA. */
    public BigDecimal documentVatAmount() {
        if (!type.capitalizable()) return money(amount);
        if (vatRatePercent == null || vatRatePercent.signum() == 0) return BigDecimal.ZERO.setScale(MONEY_SCALE);
        return money(amount.multiply(vatRatePercent).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP));
    }

    /** Dívida ao fornecedor na moeda da linha: valor sem IVA mais IVA do serviço (o IVA aduaneiro é o próprio valor). */
    public BigDecimal documentTotalAmount() {
        return type.capitalizable() ? money(amount).add(documentVatAmount()) : money(amount);
    }

    /** Parte que entra no custo dos artigos, em moeda base (zero no IVA aduaneiro). */
    public BigDecimal baseCostAmount() {
        return type.capitalizable() ? money(currency.toBaseAmount(amount)) : BigDecimal.ZERO.setScale(MONEY_SCALE);
    }

    /** Parte que vai a IVA dedutível (2432), em moeda base. */
    public BigDecimal baseVatAmount() {
        return money(currency.toBaseAmount(documentVatAmount()));
    }

    /** Dívida ao fornecedor em moeda base = custo + IVA dedutível. */
    public BigDecimal basePayableAmount() {
        return baseCostAmount().add(baseVatAmount());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
