package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.LandedCostCharge;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo puro dos custos de importação (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.2, §4.3): soma, em moeda
 * base, o que entra no custo dos artigos e o que vai a IVA dedutível. Sem estado nem persistência, para que a
 * pré-visualização e a confirmação usem exactamente as mesmas contas.
 */
public class LandedCostCalculator {

    private static final int MONEY_SCALE = 2;

    /** Totais em moeda base de um documento CI. */
    public record Totals(BigDecimal capitalizableCost, BigDecimal deductibleVat) {

        /** Total das dívidas aos fornecedores do documento, em moeda base. */
        public BigDecimal payableTotal() {
            return capitalizableCost.add(deductibleVat);
        }
    }

    /**
     * Linha de compra a receber custo: valor em moeda base, quantidade recebida e peso líquido unitário
     * (pode faltar, excepto na repartição por PESO).
     */
    public record AllocationLine(Long lineId, BigDecimal baseLineValue, BigDecimal quantity,
                                 BigDecimal netUnitWeightKg) {

        BigDecimal basis(LandedCostAllocationMethod method) {
            return switch (method) {
                case VALOR -> orZero(baseLineValue);
                case QUANTIDADE -> orZero(quantity);
                case PESO -> {
                    if (netUnitWeightKg == null || netUnitWeightKg.signum() <= 0) {
                        throw new BusinessRuleException(
                                "Para repartir por peso, todas as linhas têm de ter o peso líquido do artigo.");
                    }
                    yield netUnitWeightKg.multiply(orZero(quantity));
                }
            };
        }

        private static BigDecimal orZero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    /** Quota do custo, em moeda base, atribuída a uma linha de compra. */
    public record Allocation(Long lineId, BigDecimal share) {
    }

    /**
     * Reparte {@code totalCost} pelas linhas, proporcionalmente à base do método (VALOR por omissão), com
     * 2 casas HALF_UP; o resíduo de cêntimos vai para a maior quota, para a soma ser exactamente o total.
     */
    public List<Allocation> allocate(BigDecimal totalCost, LandedCostAllocationMethod method,
                                     List<AllocationLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos uma compra recebida com artigos para repartir o custo.");
        }
        LandedCostAllocationMethod effective = LandedCostAllocationMethod.orDefault(method);
        List<BigDecimal> bases = lines.stream().map(line -> line.basis(effective)).toList();
        BigDecimal totalBasis = bases.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalBasis.signum() <= 0) {
            throw new BusinessRuleException("Não há base para repartir o custo por "
                    + effective.label().toLowerCase() + ": as linhas somam zero.");
        }
        BigDecimal total = totalCost.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        List<BigDecimal> shares = new ArrayList<>(lines.size());
        for (BigDecimal basis : bases) {
            shares.add(total.multiply(basis).divide(totalBasis, MONEY_SCALE, RoundingMode.HALF_UP));
        }
        int largest = indexOfLargest(bases);
        BigDecimal residue = total.subtract(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        shares.set(largest, shares.get(largest).add(residue));

        List<Allocation> allocations = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            allocations.add(new Allocation(lines.get(i).lineId(), shares.get(i)));
        }
        return List.copyOf(allocations);
    }

    /** Linha com a maior quota (a maior base); em empate, a primeira — para o resíduo ser determinístico. */
    private static int indexOfLargest(List<BigDecimal> bases) {
        int largest = 0;
        for (int i = 1; i < bases.size(); i++) {
            if (bases.get(i).compareTo(bases.get(largest)) > 0) {
                largest = i;
            }
        }
        return largest;
    }

    /** Quota de uma linha dividida entre o stock que ainda existe e a parte já vendida (CMV). */
    public record StockSplit(BigDecimal stockAmount, BigDecimal soldAmount) {
    }

    /**
     * Parte para o stock = {@code C × min(1, S / Q)} (2 casas HALF_UP, nunca negativa); o resto da quota é a parte
     * já vendida (§4.4). Sem stock, ou sem quantidade recebida, a quota toda vai a CMV.
     */
    public StockSplit splitByStock(BigDecimal share, BigDecimal receivedQuantity, BigDecimal stockOnHand) {
        BigDecimal quota = share.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal stock = stockOnHand == null ? BigDecimal.ZERO : stockOnHand.max(BigDecimal.ZERO);
        if (receivedQuantity == null || receivedQuantity.signum() <= 0 || stock.signum() == 0) {
            return new StockSplit(BigDecimal.ZERO.setScale(MONEY_SCALE), quota);
        }
        BigDecimal stockAmount = stock.compareTo(receivedQuantity) >= 0
                ? quota
                : quota.multiply(stock).divide(receivedQuantity, MONEY_SCALE, RoundingMode.HALF_UP);
        return new StockSplit(stockAmount, quota.subtract(stockAmount));
    }

    public Totals totals(List<LandedCostCharge> charges) {
        BigDecimal cost = BigDecimal.ZERO.setScale(2);
        BigDecimal vat = BigDecimal.ZERO.setScale(2);
        for (LandedCostCharge charge : charges) {
            cost = cost.add(charge.baseCostAmount());
            vat = vat.add(charge.baseVatAmount());
        }
        return new Totals(cost, vat);
    }
}
