package mz.multicore.erp.modules.butchery.dto;

import java.math.BigDecimal;

/** One cut expected from a carcass, with its expected share of the input weight. */
public record BreakdownTemplateLineDTO(Long productId, String productName, String productSku, BigDecimal expectedYieldPct,
                                       BigDecimal unitPrice) {}
