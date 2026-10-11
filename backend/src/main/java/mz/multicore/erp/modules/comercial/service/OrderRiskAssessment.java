package mz.multicore.erp.modules.comercial.service;

import java.util.List;

/**
 * Resultado da avaliação de risco e alçada de uma encomenda (Padrão Grandes Sistemas ERP:
 * SAP S/4HANA, Dynamics 365, Oracle NetSuite).
 *
 * @param autoApproved Se a encomenda cumpre os critérios da "Green Lane" (auto-aprovação imediata)
 * @param requiredRole Perfil mínimo requerido para autorizar a exceção ("NONE", "MANAGER", "ADMIN")
 * @param triggers Lista descritiva dos gatilhos de exceção ativados (mora, crédito, desconto, alçada de valor)
 * @param description Justificação consolidada para o pedido de aprovação ou auditoria
 */
public record OrderRiskAssessment(
        boolean autoApproved,
        String requiredRole,
        List<String> triggers,
        String description
) {
    public static OrderRiskAssessment greenLane(String details) {
        return new OrderRiskAssessment(true, "NONE", List.of(), details);
    }

    public static OrderRiskAssessment exception(String requiredRole, List<String> triggers, String description) {
        return new OrderRiskAssessment(false, requiredRole, triggers != null ? triggers : List.of(), description);
    }
}
