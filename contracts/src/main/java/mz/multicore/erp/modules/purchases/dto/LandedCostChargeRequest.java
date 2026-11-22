package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import mz.multicore.erp.modules.purchases.model.LandedCostType;

import java.math.BigDecimal;

/**
 * Linha de custo de um documento "Custos de Importação" (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1): tipo,
 * fornecedor, moeda (vazia = moeda base) e câmbio (vazio = o proposto pela tabela), valor sem IVA na moeda da linha
 * e, quando o serviço tem IVA, a taxa de IVA da empresa.
 */
public record LandedCostChargeRequest(
        @NotNull(message = "Indique o tipo da linha de custo de importação.") LandedCostType type,
        @NotNull(message = "Indique o fornecedor da linha de custo.") Long supplierId,
        String currency,
        BigDecimal exchangeRate,
        @NotNull(message = "Indique o valor da linha de custo.")
        @Positive(message = "O valor da linha de custo de importação tem de ser maior que zero.") BigDecimal amount,
        Long taxRateId
) {}
