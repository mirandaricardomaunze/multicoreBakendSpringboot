package mz.multicore.erp.modules.productionplan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One batch made (apontamento): ingredients consumed by FEFO, the batch entered with its expiry and real cost. */
public record ProductionRunDTO(Long id, Long lineId, String productName, BigDecimal quantity, String batchNumber,
                               LocalDate expirationDate, BigDecimal ingredientCost, BigDecimal totalCost,
                               BigDecimal unitCost, String createdBy, LocalDateTime createdAt) {}
