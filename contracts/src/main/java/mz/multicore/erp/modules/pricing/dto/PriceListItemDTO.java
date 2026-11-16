package mz.multicore.erp.modules.pricing.dto;

import java.math.BigDecimal;

/**
 * Product price in a list, in the product base unit and on the same VAT basis as the normal selling price.
 * {@code minQuantity} creates tiers: the price applies from that quantity onwards.
 */
public record PriceListItemDTO(
        Long id,
        Long priceListId,
        Long productId,
        String productSku,
        String productName,
        String baseUnitCode,
        BigDecimal minQuantity,
        BigDecimal price,
        BigDecimal retailPrice
) {}
