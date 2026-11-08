package mz.multicore.erp.modules.productionplan.dto;

import java.math.BigDecimal;

/** One product of the day plan: what was planned, what was made, what is left to make and the leftovers. */
public record ProductionPlanLineDTO(Long id, Long bomId, String bomName, Long productId, String productName,
                                    BigDecimal plannedQty, BigDecimal producedQty, BigDecimal remainingQty,
                                    BigDecimal leftoverQty, BigDecimal lastUnitCost, BigDecimal unitPrice) {}
