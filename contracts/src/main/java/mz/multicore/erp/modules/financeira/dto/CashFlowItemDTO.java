package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowItemDTO(
    Long id,
    String type,
    String documentNumber,
    String entityName,
    LocalDate dueDate,
    BigDecimal amount,
    String bucketCode
) {}
