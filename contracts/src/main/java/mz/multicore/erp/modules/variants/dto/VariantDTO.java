package mz.multicore.erp.modules.variants.dto;

import java.math.BigDecimal;

/** One variant: an ordinary product with its own code, price and stock, plus the values that tell it apart. */
public record VariantDTO(Long id, Long productId, String sku, String name, String value1, String value2,
                         BigDecimal unitPrice, BigDecimal stock) {}
