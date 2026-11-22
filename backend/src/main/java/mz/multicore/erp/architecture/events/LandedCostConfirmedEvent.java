package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Documento "Custos de Importação" confirmado, com os valores do lançamento já em moeda base
 * (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4).
 *
 * @param stockAmount parte do custo que subiu o custo médio do stock existente (débito 3201)
 * @param soldAmount  parte do custo já vendida (débito 6101)
 * @param vatAmount   IVA aduaneiro e IVA dos serviços (débito 2432)
 * @param payables    dívida de cada linha ao seu fornecedor, em moeda base (crédito 2201)
 */
public record LandedCostConfirmedEvent(
        Long companyId,
        Long documentId,
        String documentNumber,
        LocalDate date,
        BigDecimal stockAmount,
        BigDecimal soldAmount,
        BigDecimal vatAmount,
        List<Payable> payables
) {
    public LandedCostConfirmedEvent {
        payables = payables == null ? List.of() : List.copyOf(payables);
    }

    /** Dívida de uma linha de custo: número {@code <CI>-n}, fornecedor e total com IVA em moeda base. */
    public record Payable(String number, String supplierName, BigDecimal amount) {}
}
