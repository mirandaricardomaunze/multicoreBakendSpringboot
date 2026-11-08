package mz.multicore.erp.modules.pricing.dto;

import java.math.BigDecimal;

/**
 * Unit price the server applies to a line. {@code source} is {@code PRICE_LIST}, {@code WHOLESALE} or
 * {@code RETAIL}. It is the same calculation invoices, orders and quotations use when they are saved.
 */
public record ResolvedPriceDTO(
        Long productId,
        Long clientId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String source,
        String priceListName
) {}
