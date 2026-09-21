package mz.multicore.erp.modules.audit.dto;

import java.math.BigDecimal;
import java.util.List;

public record ForensicAuditSummaryDTO(
    int totalAnomalies,
    int criticalCount,
    int suspiciousCount,
    int infoCount,
    BigDecimal totalFinancialRisk,
    String complianceScore,
    List<ForensicAnomalyDTO> anomalies
) {}
