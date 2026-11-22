package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;

/**
 * Efeito de um documento CI numa linha de compra, em moeda base (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4):
 * quota repartida, quantidade recebida, stock no armazém, parte que subiu o custo médio, parte já vendida (CMV) e o
 * custo médio do artigo antes e depois.
 */
public record LandedCostAllocationDTO(
        Long purchaseLineId,
        String purchaseNumber,
        Long productId,
        String productName,
        BigDecimal receivedQuantity,
        BigDecimal stockQuantity,
        BigDecimal shareAmount,
        BigDecimal stockAmount,
        BigDecimal soldAmount,
        BigDecimal unitCostBefore,
        BigDecimal unitCostAfter
) {}
