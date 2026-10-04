package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;

/**
 * Representa a contagem física de uma denominação de moeda (cédula ou moeda metálica).
 */
public record CashDenominationDTO(
        BigDecimal denomination,
        int count,
        BigDecimal subtotal
) {
    public CashDenominationDTO(BigDecimal denomination, int count) {
        this(denomination, count, denomination.multiply(BigDecimal.valueOf(count)));
    }
}
