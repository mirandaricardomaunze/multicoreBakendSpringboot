package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;

/**
 * Dívida a um fornecedor numa moeda: o valor na moeda dos documentos e o equivalente em moeda base, ao câmbio de
 * cada documento (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3).
 */
public record CurrencyBalanceDTO(
        String currency,
        BigDecimal outstanding,
        BigDecimal baseOutstanding
) {}
