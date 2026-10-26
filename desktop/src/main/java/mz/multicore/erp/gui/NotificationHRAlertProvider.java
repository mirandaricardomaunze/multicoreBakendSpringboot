package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.quality.dto.ExpiredWorkerAlertDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Provedor especializado de alertas operacionais de RH, contratos, retenções, documentos e qualidade/EPI.
 */
final class NotificationHRAlertProvider {

    private NotificationHRAlertProvider() {}

    static void addContracts(List<NotificationItem> items, HRApiClient hrApiClient) {
        if (hrApiClient == null) return;
        ContractAlertsDTO alerts = hrApiClient.getContractAlerts();
        if (alerts == null) return;
        LocalDate today = LocalDate.now();
        for (EmploymentContractDTO contract : alerts.endingSoon()) {
            long days = contract.daysUntilEnd() == null ? 0 : contract.daysUntilEnd();
            items.add(new NotificationItem("Contratos",
                    days <= 0 ? "Contrato termina hoje: " + contract.employeeName()
                            : "Contrato termina em " + days + " dia(s): " + contract.employeeName(),
                    contract.contractNumber() + " · " + contract.contractTypeLabel(),
                    contract.endDate() == null ? "Sem termo" : contract.endDate().format(UIHelper.DATE_FMT),
                    "hr", days <= 7 ? 3 : 2));
        }
        for (EmploymentContractDTO contract : alerts.probationEndingSoon()) {
            long days = contract.probationEndDate() == null
                    ? 0 : java.time.temporal.ChronoUnit.DAYS.between(today, contract.probationEndDate());
            items.add(new NotificationItem("Contratos",
                    days <= 0 ? "Período experimental termina hoje: " + contract.employeeName()
                            : "Período experimental termina em " + days + " dia(s): " + contract.employeeName(),
                    contract.contractNumber() + " · decidir a confirmação",
                    contract.probationEndDate() == null ? "" : contract.probationEndDate().format(UIHelper.DATE_FMT),
                    "hr", 3));
        }
    }

    static void addPayrollLiabilities(List<NotificationItem> items, HRApiClient hrApiClient) {
        if (hrApiClient == null) return;
        for (PayrollLiabilityDTO liability : hrApiClient.getPayrollLiabilityAlerts()) {
            String period = String.format("%02d/%d", liability.month(), liability.year());
            String title;
            int priority;
            if (liability.overdue()) {
                title = "Retenção em atraso: " + liability.liabilityTypeLabel() + " de " + period;
                priority = 3;
            } else if (liability.dueDate() == null) {
                title = "Retenção sem prazo definido: " + liability.liabilityTypeLabel() + " de " + period;
                priority = 2;
            } else {
                long days = liability.daysUntilDue() == null ? 0 : liability.daysUntilDue();
                title = days <= 0
                        ? "Retenção a entregar hoje: " + liability.liabilityTypeLabel() + " de " + period
                        : "Retenção a entregar em " + days + " dia(s): " + liability.liabilityTypeLabel() + " de " + period;
                priority = 3;
            }
            String detail = liability.dueDate() == null
                    ? liability.amount() + " MT — configure o prazo legal em RH › Valores Legais"
                    : liability.amount() + " MT a entregar";
            items.add(new NotificationItem("Retenções", title, detail,
                    liability.dueDate() == null ? "Prazo por configurar" : liability.dueDate().format(UIHelper.DATE_FMT),
                    "hr", priority));
        }
    }

    static void addEmployeeDocuments(List<NotificationItem> items, HRApiClient hrApiClient) {
        if (hrApiClient == null) return;
        for (EmployeeDocumentDTO document : hrApiClient.getExpiringEmployeeDocuments()) {
            if (document.expiryDate() == null) continue;
            long days = document.daysUntilExpiry() == null ? 0 : document.daysUntilExpiry();
            String title = document.expired()
                    ? document.documentType() + " caducado: " + document.employeeName()
                    : days == 0
                    ? document.documentType() + " caduca hoje: " + document.employeeName()
                    : document.documentType() + " caduca em " + days + " dia(s): " + document.employeeName();
            String detail = document.documentNumber() == null
                    ? "Documento do colaborador" : "Nº " + document.documentNumber();
            items.add(new NotificationItem("Documentos", title, detail,
                    document.expiryDate().format(UIHelper.DATE_FMT), "hr",
                    document.expired() || days <= 15 ? 3 : 2));
        }
    }

    static void addOccupationalHealth(List<NotificationItem> items, HRApiClient hrApiClient) {
        if (hrApiClient == null || !SignedInUser.isManagerOrAdmin()) return;
        for (OccupationalHealthExamDTO exam : hrApiClient.getExpiringOccupationalHealthExams()) {
            long days = exam.daysUntilExpiry();
            String title = days < 0
                    ? "Exame ocupacional expirado: " + exam.employeeName()
                    : days == 0
                    ? "Exame ocupacional expira hoje: " + exam.employeeName()
                    : "Renovar exame em " + days + " dia(s): " + exam.employeeName();
            items.add(new NotificationItem("Saúde Ocupacional", title,
                    "Agendar exame periódico · " + fitnessLabel(exam.fitnessResult()),
                    exam.expiryDate().format(UIHelper.DATE_FMT), "hr", days <= 15 ? 3 : 2));
        }
        for (MissingHealthExamDTO missing : hrApiClient.getEmployeesMissingHealthExam()) {
            items.add(new NotificationItem("Saúde Ocupacional",
                    "Sem exame de aptidão: " + missing.employeeName(),
                    missing.daysSinceHire() == null
                            ? "Admissão por registar · exame de admissão nunca realizado"
                            : "Admitido há " + missing.daysSinceHire() + " dia(s) sem exame de admissão",
                    missing.hireDate() == null ? "—" : missing.hireDate().format(UIHelper.DATE_FMT), "hr", 3));
        }
    }

    static void addQualityAlerts(List<NotificationItem> items, Long companyId, QualityApiClient qualityApiClient) {
        if (qualityApiClient == null || companyId == null) return;
        try {
            List<ExpiredWorkerAlertDTO> alerts = qualityApiClient.getExpiredWorkerAlerts(companyId);
            if (alerts == null) return;
            for (ExpiredWorkerAlertDTO alert : alerts) {
                String worker = alert.employeeName() != null ? alert.employeeName() : "Colaborador";
                String role = alert.employeeRole() != null && !alert.employeeRole().isBlank() ? " (" + alert.employeeRole() + ")" : "";

                for (ExpiredWorkerAlertDTO.ExpiredEquipmentItemDTO eqItem : alert.items()) {
                    String equip = eqItem.equipmentName() != null ? eqItem.equipmentName() : "Equipamento";
                    long days = eqItem.daysOverdueOrRemaining();
                    String title;
                    int priority;
                    String when;

                    if (days < 0) {
                        long overdueDays = Math.abs(days);
                        title = "EPI / Equipamento Expirado: " + worker;
                        priority = 3;
                        when = overdueDays == 1 ? "Expirou há 1 dia" : "Expirou há " + overdueDays + " dias";
                    } else if (days == 0) {
                        title = "EPI / Equipamento Expira Hoje: " + worker;
                        priority = 3;
                        when = "Expira hoje";
                    } else {
                        title = "EPI / Equipamento a Expirar: " + worker;
                        priority = 2;
                        when = "Expira em " + days + " dias";
                    }

                    String expDateStr = eqItem.expiryDate() != null ? eqItem.expiryDate().format(UIHelper.DATE_FMT) : "—";
                    String detail = equip + role + " · Vencimento: " + expDateStr;
                    items.add(new NotificationItem("Qualidade & HST", title, detail, when, "quality", priority));
                }
            }
        } catch (Exception ignored) {
            // Degrada suavemente se API estiver inacessível
        }
    }

    private static String fitnessLabel(String result) {
        return switch (result) {
            case "FIT" -> "Apto";
            case "FIT_WITH_RESTRICTIONS" -> "Apto com restrições";
            case "UNFIT" -> "Inapto";
            default -> "Sem resultado";
        };
    }
}
