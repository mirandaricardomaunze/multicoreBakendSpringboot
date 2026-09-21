package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;

/**
 * Pedido de registo/atualização de quantidade contada de um item de inventário.
 */
public record UpdateInventoryItemCountRequest(
        Long itemId,
        BigDecimal countedQuantity,
        String barcode,
        boolean incrementMode,
        String notes
) {}
