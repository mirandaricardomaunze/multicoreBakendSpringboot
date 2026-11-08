package mz.multicore.erp.modules.productionplan.dto;

import mz.multicore.erp.modules.productionplan.model.ProductionPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The production plan of one day and warehouse. {@code id == null} means nothing was planned yet. */
public record ProductionPlanDTO(Long id, LocalDate planDate, Long warehouseId, String warehouseName,
                                ProductionPlanStatus status, String notes, List<ProductionPlanLineDTO> lines,
                                List<ProductionRunDTO> runs, BigDecimal producedCost, String closedBy) {}
