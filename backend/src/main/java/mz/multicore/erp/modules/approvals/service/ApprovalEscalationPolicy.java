package mz.multicore.erp.modules.approvals.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Política corporativa de escalonamento e alçadas de aprovação.
 * Avalia limites financeiros e menções descritivas ("Green Lane", "Requer ADMIN", "Requer MANAGER").
 * Ver {@code docs/APPROVAL_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Component
public class ApprovalEscalationPolicy {

    public static final BigDecimal BASE_AUTO_APPROVE_LIMIT = new BigDecimal("50000.00");
    public static final BigDecimal BASE_MANAGER_LIMIT = new BigDecimal("250000.00");

    public record EscalationDecision(String requiredRole, boolean autoApproved) {}

    public EscalationDecision evaluate(BigDecimal amount, String description) {
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;

        if (description != null && description.contains("(Requer ADMIN)")) {
            return new EscalationDecision("ADMIN", false);
        } else if (description != null && description.contains("(Requer MANAGER)")) {
            return new EscalationDecision("MANAGER", false);
        } else if (description != null && description.contains("Green Lane")) {
            return new EscalationDecision("NONE", true);
        } else if (amt.compareTo(BASE_AUTO_APPROVE_LIMIT) <= 0) {
            return new EscalationDecision("NONE", true);
        } else if (amt.compareTo(BASE_MANAGER_LIMIT) <= 0) {
            return new EscalationDecision("MANAGER", false);
        } else {
            return new EscalationDecision("ADMIN", false);
        }
    }
}
