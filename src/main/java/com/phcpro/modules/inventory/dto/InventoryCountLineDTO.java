package com.phcpro.modules.inventory.dto;

import java.math.BigDecimal;

/** Linha de uma sessão de contagem: artigo + contagem + (após aplicar) quantidade do sistema. */
public record InventoryCountLineDTO(
        Long lineId,
        Long productId,
        String sku,
        String name,
        BigDecimal countedQuantity,
        BigDecimal systemQuantity
) {
    /** Diferença contado − sistema (só disponível depois de aplicar); null caso contrário. */
    public BigDecimal difference() {
        if (countedQuantity == null || systemQuantity == null) return null;
        return countedQuantity.subtract(systemQuantity);
    }
}
