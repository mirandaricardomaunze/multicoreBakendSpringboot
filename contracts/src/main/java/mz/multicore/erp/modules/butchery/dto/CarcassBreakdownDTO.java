package mz.multicore.erp.modules.butchery.dto;

import mz.multicore.erp.modules.butchery.model.CarcassBreakdownStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A posted carcass breakdown with yield, loss and the cost per kg of every cut. */
public record CarcassBreakdownDTO(Long id, String number, LocalDateTime createdAt, String createdBy, Long warehouseId,
                                  String warehouseName, Long templateId, String templateName, Long sourceProductId,
                                  String sourceProductName, BigDecimal inputKg, BigDecimal inputCost,
                                  BigDecimal outputKg, BigDecimal lossKg, BigDecimal lossPct, BigDecimal lossTolerancePct,
                                  boolean lossAboveTolerance, CarcassBreakdownStatus status, String reference,
                                  String notes, String cancellationReason, List<CarcassBreakdownLineDTO> lines) {}
