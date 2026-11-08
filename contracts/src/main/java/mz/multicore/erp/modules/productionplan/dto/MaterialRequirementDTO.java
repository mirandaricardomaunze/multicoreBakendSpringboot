package mz.multicore.erp.modules.productionplan.dto;

import java.math.BigDecimal;

/** Raw material still needed for what is left to produce today, against the stock of the warehouse. */
public record MaterialRequirementDTO(Long productId, String productName, String unit, BigDecimal required,
                                     BigDecimal inStock, BigDecimal shortage) {}
