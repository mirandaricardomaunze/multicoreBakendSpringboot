package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Pedido de registo/atualização de quantidade contada de um item de inventário.
 */
public record UpdateInventoryItemCountRequest(
        Long itemId,

        @PositiveOrZero(message = "A quantidade contada não pode ser negativa.")
        BigDecimal countedQuantity,

        @Size(max = 50, message = "O código de barras não pode exceder 50 caracteres.")
        String barcode,

        boolean incrementMode,

        @Size(max = 255, message = "As observações não podem exceder 255 caracteres.")
        String notes
) {}
