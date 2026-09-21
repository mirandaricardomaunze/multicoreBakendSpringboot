package mz.multicore.erp.modules.audit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ForensicAnomalyDTO(
    Long id,
    LocalDateTime timestamp,
    String operator,
    ForensicCategory category,
    String categoryLabel,
    ForensicSeverity severity,
    String documentOrReference,
    BigDecimal financialImpact,
    String details,
    String justification,
    String recommendation
) {}
