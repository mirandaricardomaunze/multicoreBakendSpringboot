package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

/**
 * Resultado do acerto de existências: valor do stock ao custo e saldo contabilístico, antes do acerto, das contas
 * Mercadorias (3201) e Produtos acabados (3401). {@code posted} é falso quando já estavam certas.
 */
public record InventoryAdjustmentDTO(
        BigDecimal goodsStockValue,
        BigDecimal goodsBookValue,
        BigDecimal finishedStockValue,
        BigDecimal finishedBookValue,
        boolean posted
) {}
