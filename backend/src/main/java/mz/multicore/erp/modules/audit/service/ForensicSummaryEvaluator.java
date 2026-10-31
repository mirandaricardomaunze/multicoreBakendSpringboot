package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Filtra anomalias forenses, agrega o risco financeiro e avalia o índice de conformidade. */
@Component
public class ForensicSummaryEvaluator {

    public ForensicAuditSummaryDTO evaluate(List<ForensicAnomalyDTO> allAnomalies,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            ForensicSeverity severityFilter,
                                            ForensicCategory categoryFilter,
                                            String operatorFilter) {
        List<ForensicAnomalyDTO> filtered = allAnomalies.stream()
                .filter(a -> {
                    if (startDate != null && a.timestamp().toLocalDate().isBefore(startDate)) return false;
                    if (endDate != null && a.timestamp().toLocalDate().isAfter(endDate)) return false;
                    if (severityFilter != null && a.severity() != severityFilter) return false;
                    if (categoryFilter != null && a.category() != categoryFilter) return false;
                    if (operatorFilter != null && !operatorFilter.isBlank()
                            && !a.operator().toLowerCase().contains(operatorFilter.toLowerCase())) return false;
                    return true;
                })
                .sorted(Comparator.comparing(ForensicAnomalyDTO::timestamp).reversed())
                .collect(Collectors.toList());

        int criticalCount = 0;
        int suspiciousCount = 0;
        int infoCount = 0;
        BigDecimal totalFinancialRisk = BigDecimal.ZERO;

        for (ForensicAnomalyDTO a : filtered) {
            if (a.severity() == ForensicSeverity.CRITICAL) {
                criticalCount++;
                totalFinancialRisk = totalFinancialRisk.add(a.financialImpact());
            } else if (a.severity() == ForensicSeverity.SUSPICIOUS) {
                suspiciousCount++;
                totalFinancialRisk = totalFinancialRisk.add(a.financialImpact());
            } else {
                infoCount++;
            }
        }

        String complianceScore = computeComplianceScore(totalFinancialRisk);
        return new ForensicAuditSummaryDTO(
                filtered.size(),
                criticalCount,
                suspiciousCount,
                infoCount,
                totalFinancialRisk,
                complianceScore,
                filtered
        );
    }

    private String computeComplianceScore(BigDecimal totalFinancialRisk) {
        if (totalFinancialRisk.compareTo(BigDecimal.ZERO) == 0) {
            return "100% — Excelente / Sem Desvios";
        }
        if (totalFinancialRisk.compareTo(new BigDecimal("10000.00")) <= 0) {
            return "88% — Bom / Baixo Risco";
        }
        if (totalFinancialRisk.compareTo(new BigDecimal("50000.00")) <= 0) {
            return "68% — Atenção / Risco Moderado";
        }
        return "42% — Crítico / Elevada Exposição";
    }
}
