package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para registo de reconciliação de passagem de turno entre operadores.
 */
public record ShiftReconciliationDTO(
    Long id,
    Long sessionId,
    String outgoingOperator,
    String incomingOperator,
    LocalDateTime reconciledAt,
    BigDecimal expectedCash,
    BigDecimal countedCash,
    BigDecimal difference,
    String cashBreakdownJson,
    String notes
) {}
