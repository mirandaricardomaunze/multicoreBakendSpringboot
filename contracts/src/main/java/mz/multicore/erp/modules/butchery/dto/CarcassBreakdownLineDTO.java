package mz.multicore.erp.modules.butchery.dto;

import java.math.BigDecimal;

/** A cut obtained: weight, the share of the cost it carries and how it compares to the model. */
public record CarcassBreakdownLineDTO(Long productId, String productName, BigDecimal kg, BigDecimal unitSalePrice,
                                      BigDecimal allocatedCost, BigDecimal costPerKg, BigDecimal expectedYieldPct,
                                      BigDecimal realYieldPct) {}
