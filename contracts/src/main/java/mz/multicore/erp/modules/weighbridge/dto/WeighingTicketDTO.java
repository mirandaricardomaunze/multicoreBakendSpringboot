package mz.multicore.erp.modules.weighbridge.dto;

import mz.multicore.erp.modules.weighbridge.model.WeighingDirection;
import mz.multicore.erp.modules.weighbridge.model.WeighingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A weighing ticket (TP). {@code manual} is true when any weight was typed instead of read from the scale. */
public record WeighingTicketDTO(Long id, String number, WeighingStatus status, WeighingDirection direction, String plate,
                                String driver, String counterparty, Long productId, String productName, String reference,
                                BigDecimal firstWeight, LocalDateTime firstAt, BigDecimal secondWeight, LocalDateTime secondAt,
                                BigDecimal netWeight, boolean manual, String notes, String createdBy, String closedBy) {}
