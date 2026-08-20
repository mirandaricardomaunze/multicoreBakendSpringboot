package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

public record QuotationLineDTO(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal discountPercentage,
        BigDecimal lineTotal,
        int unitsPerBox
) {}
