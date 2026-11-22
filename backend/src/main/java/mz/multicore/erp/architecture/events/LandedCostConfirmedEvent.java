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
 * @param soldByWarehouse parte vendida repartida pelo armazém de cada compra (CC-05): cada parte vai a 6101 no centro
 *                    desse armazém; vazia lança {@code soldAmount} inteiro no centro por omissão
 */
public record LandedCostConfirmedEvent(
        Long companyId,
        Long documentId,
        String documentNumber,
        LocalDate date,
        BigDecimal stockAmount,
        BigDecimal soldAmount,
        BigDecimal vatAmount,
        List<Payable> payables,
        List<WarehouseShare> soldByWarehouse
) {
    public LandedCostConfirmedEvent {
        payables = payables == null ? List.of() : List.copyOf(payables);
        soldByWarehouse = soldByWarehouse == null ? List.of() : List.copyOf(soldByWarehouse);
    }

    /** Documento sem repartição por armazém (parte vendida no centro por omissão). */
    public LandedCostConfirmedEvent(Long companyId, Long documentId, String documentNumber, LocalDate date,
                                    BigDecimal stockAmount, BigDecimal soldAmount, BigDecimal vatAmount,
                                    List<Payable> payables) {
        this(companyId, documentId, documentNumber, date, stockAmount, soldAmount, vatAmount, payables, List.of());
    }

    /** Parte de um valor que pertence a um armazém ({@code warehouseId} pode ser {@code null}: centro por omissão). */
    public record WarehouseShare(Long warehouseId, BigDecimal amount) {}

    /** Dívida de uma linha de custo: número {@code <CI>-n}, fornecedor e total com IVA em moeda base. */
    public record Payable(String number, String supplierName, BigDecimal amount) {}
}
