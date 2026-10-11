package mz.multicore.erp.modules.manufacturing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de lançamento de nova Ordem de Produção (OP).
 */
public record CreateProductionOrderRequest(
        @NotNull(message = "A ficha técnica (BOM) é obrigatória.")
        Long bomId,

        @NotNull(message = "A quantidade a produzir é obrigatória.")
        @Positive(message = "A quantidade deve ser positiva.")
        BigDecimal plannedQuantity,

        @NotNull(message = "O armazém de matérias-primas é obrigatório.")
        Long sourceWarehouseId,

        @NotNull(message = "O armazém de produto acabado é obrigatório.")
        Long targetWarehouseId,

        LocalDate plannedEndDate
) {}
