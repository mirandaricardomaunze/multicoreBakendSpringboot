package mz.multicore.erp.modules.expiry.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A lot with expiry date and stock. {@code stage}: EXPIRED, TODAY, SOON (up to 2 days), WEEK (up to 7 days) or
 * LATER. {@code markdownPct} is the discount the POS already gives today; {@code suggestedPct} the one the
 * markdown rules ask for.
 */
public record ExpiryLotDTO(Long batchId, Long productId, String productName, String sku, String categoryName,
                           Long warehouseId, String warehouseName, String batchNumber, LocalDate expirationDate,
                           long daysLeft, BigDecimal quantity, BigDecimal unitCost, BigDecimal costValue,
                           BigDecimal unitPrice, BigDecimal saleValue, String stage, BigDecimal markdownPct,
                           BigDecimal suggestedPct) {}
