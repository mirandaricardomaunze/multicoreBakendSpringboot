package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.MissingHealthExamDTO;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Agrega alertas operacionais existentes para o bell e a página de notificações. */
public class NotificationFeed {

    private static final int EXPIRY_ALERT_DAYS = 30;
    private static final long SUBSCRIPTION_ALERT_DAYS = 7;
    private static final BigDecimal DEFAULT_LOW_STOCK = BigDecimal.valueOf(5);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MZN_FMT = new DecimalFormat("#,##0.00 MT", new DecimalFormatSymbols(new Locale("pt", "MZ")));

    private final ApprovalApiClient approvalApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final MySubscriptionApiClient subscriptionApiClient;
    private final HRApiClient hrApiClient;
    private final mz.multicore.erp.desktop.client.PerformanceApiClient performanceApiClient;
    private final CreditRiskApiClient creditRiskApiClient;
    private final StockWasteApiClient stockWasteApiClient;

    public NotificationFeed(ApprovalApiClient approvalApiClient,
                            InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient,
                            HRApiClient hrApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, null, null, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient,
                            InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient,
                            HRApiClient hrApiClient,
                            mz.multicore.erp.desktop.client.PerformanceApiClient performanceApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, performanceApiClient, null, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient,
                            InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient,
                            HRApiClient hrApiClient,
                            mz.multicore.erp.desktop.client.PerformanceApiClient performanceApiClient,
                            CreditRiskApiClient creditRiskApiClient,
                            StockWasteApiClient stockWasteApiClient) {
        this.approvalApiClient = approvalApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.subscriptionApiClient = subscriptionApiClient;
        this.hrApiClient = hrApiClient;
        this.performanceApiClient = performanceApiClient;
        this.creditRiskApiClient = creditRiskApiClient;
        this.stockWasteApiClient = stockWasteApiClient;
    }

    public List<NotificationItem> load(Long companyId) {
        List<NotificationItem> items = new ArrayList<>();
        addApprovals(items);
        addCreditRiskAlerts(items);
        addStockWasteAlerts(items, companyId);
        addLowStock(items, companyId);
        addExpiries(items, companyId);
        addSubscription(items);
        addContracts(items);
        addPayrollLiabilities(items);
        addEmployeeDocuments(items);
        addOccupationalHealth(items);
        addGoals(items, companyId);
        items.sort(Comparator.comparingInt(NotificationItem::priority).reversed()
                .thenComparing(NotificationItem::type)
                .thenComparing(NotificationItem::title));
        return List.copyOf(items);
    }

    private void addApprovals(List<NotificationItem> items) {
        for (ApprovalRequestDTO approval : approvalApiClient.getPendingRequests()) {
            String detail = humanDocumentType(approval.documentType());
            if (approval.description() != null && !approval.description().isBlank()) {
                detail += " — " + approval.description();
            }
            String when = approval.createdAt() == null ? "Pendente" : approval.createdAt().toLocalDate().format(DATE_FORMAT);
            items.add(new NotificationItem("Aprovações", "Pedido de aprovação pendente", detail,
                    when, "approvals", 2));
        }
    }

    private void addLowStock(List<NotificationItem> items, Long companyId) {
        for (StockDTO stock : inventoryApiClient.getStocksByCompany(companyId)) {
            BigDecimal threshold = stock.minStock() != null && stock.minStock().signum() > 0
                    ? stock.minStock() : DEFAULT_LOW_STOCK;
            if (stock.quantity() == null || stock.quantity().compareTo(threshold) >= 0) continue;
            String warehouse = stock.warehouseName() == null ? "Armazém" : stock.warehouseName();
            String detail = warehouse + " — disponível " + stock.quantity() + ", mínimo " + threshold;
            items.add(new NotificationItem("Stock", "Stock baixo: " + stock.productName(), detail,
                    "Repor stock", "stock", 2));
        }
    }

    private void addExpiries(List<NotificationItem> items, Long companyId) {
        LocalDate today = LocalDate.now();
        for (ProductBatchDTO batch : inventoryApiClient.findExpiringBatches(companyId, EXPIRY_ALERT_DAYS)) {
            if (batch.expirationDate() == null) continue;
            boolean expired = batch.expirationDate().isBefore(today);
            String batchNumber = batch.batchNumber() == null ? "sem número" : batch.batchNumber();
            String detail = "Lote " + batchNumber + " · "
                    + (batch.warehouseName() == null ? "Armazém" : batch.warehouseName());
            items.add(new NotificationItem("Validades",
                    (expired ? "Lote vencido: " : "Lote a vencer: ") + batch.productName(),
                    detail, batch.expirationDate().format(DATE_FORMAT), "stock", expired ? 3 : 2));
        }
    }

    private void addSubscription(List<NotificationItem> items) {
        MySubscriptionDTO subscription = subscriptionApiClient.getMySubscription();
        if (subscription == null || !subscription.hasSubscription()) return;
        boolean blocked = "EXPIRED".equals(subscription.status()) || "SUSPENDED".equals(subscription.status());
        Long days = subscription.daysRemaining();
        if (!blocked && (days == null || days < 0 || days > SUBSCRIPTION_ALERT_DAYS)) return;

        String statusLabel = subscription.statusLabel() == null ? "indisponível" : subscription.statusLabel();
        String title = blocked ? "Assinatura " + statusLabel.toLowerCase()
                : days == 0 ? "A assinatura expira hoje"
                : "A assinatura expira em " + days + " dia(s)";
        String detail = subscription.planLabel() == null ? "Consulte a sua assinatura" : "Plano " + subscription.planLabel();
        String when = subscription.validUntil() == null ? statusLabel
                : subscription.validUntil().format(DATE_FORMAT);
        items.add(new NotificationItem("Assinatura", title, detail, when, "config", blocked ? 3 : 2));
    }

    /**
     * Fins de contrato e de período experimental. Um contrato que acaba sem ninguém dar por isso é
     * das poucas coisas do RH que custa dinheiro nos dois sentidos: ou se paga a quem já não
     * trabalha (a folha já trava isso), ou se perde alguém por falta de renovação a tempo.
     */
    private void addContracts(List<NotificationItem> items) {
        ContractAlertsDTO alerts = hrApiClient.getContractAlerts();
        if (alerts == null) return;
        LocalDate today = LocalDate.now();
        for (EmploymentContractDTO contract : alerts.endingSoon()) {
            long days = contract.daysUntilEnd() == null ? 0 : contract.daysUntilEnd();
            items.add(new NotificationItem("Contratos",
                    days <= 0 ? "Contrato termina hoje: " + contract.employeeName()
                              : "Contrato termina em " + days + " dia(s): " + contract.employeeName(),
                    contract.contractNumber() + " · " + contract.contractTypeLabel(),
                    contract.endDate() == null ? "Sem termo" : contract.endDate().format(DATE_FORMAT),
                    "hr", days <= 7 ? 3 : 2));
        }
        for (EmploymentContractDTO contract : alerts.probationEndingSoon()) {
            long days = contract.probationEndDate() == null
                    ? 0 : java.time.temporal.ChronoUnit.DAYS.between(today, contract.probationEndDate());
            items.add(new NotificationItem("Contratos",
                    days <= 0 ? "Período experimental termina hoje: " + contract.employeeName()
                              : "Período experimental termina em " + days + " dia(s): " + contract.employeeName(),
                    contract.contractNumber() + " · decidir a confirmação",
                    contract.probationEndDate() == null ? "" : contract.probationEndDate().format(DATE_FORMAT),
                    "hr", 3));
        }
    }

    /**
     * Retenções da folha por entregar. Três situações, todas más de maneiras diferentes: já
     * atrasada, a chegar ao prazo, e — a que interessa mesmo — <b>sem prazo configurado</b>. Esta
     * última nunca chega a estar atrasada, pelo que sem uma linha própria no sino não apareceria em
     * lado nenhum e o dinheiro do Estado ficava outra vez invisível.
     */
    private void addPayrollLiabilities(List<NotificationItem> items) {
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
                        : "Retenção a entregar em " + days + " dia(s): "
                                + liability.liabilityTypeLabel() + " de " + period;
                priority = 3;
            }
            String detail = liability.dueDate() == null
                    ? liability.amount() + " MT — configure o prazo legal em RH › Valores Legais"
                    : liability.amount() + " MT a entregar";
            items.add(new NotificationItem("Retenções", title, detail,
                    liability.dueDate() == null ? "Prazo por configurar"
                            : liability.dueDate().format(DATE_FORMAT),
                    "hr", priority));
        }
    }

    /**
     * Documentos do colaborador a caducar — e os já caducados. Ver §B8.8.
     *
     * <p>É a única linha do sino cuja falha custa <b>multa</b>: o DIRE de um trabalhador
     * estrangeiro caducar sem ninguém dar por isso. Um documento já caducado continua a aparecer,
     * porque sair da janela nunca pode ser a forma de o alerta desaparecer.
     */
    private void addEmployeeDocuments(List<NotificationItem> items) {
        for (EmployeeDocumentDTO document : hrApiClient.getExpiringEmployeeDocuments()) {
            if (document.expiryDate() == null) {
                continue;
            }
            long days = document.daysUntilExpiry() == null ? 0 : document.daysUntilExpiry();
            String title = document.expired()
                    ? document.documentType() + " caducado: " + document.employeeName()
                    : days == 0
                            ? document.documentType() + " caduca hoje: " + document.employeeName()
                            : document.documentType() + " caduca em " + days + " dia(s): "
                                    + document.employeeName();
            String detail = document.documentNumber() == null
                    ? "Documento do colaborador" : "Nº " + document.documentNumber();
            items.add(new NotificationItem("Documentos", title, detail,
                    document.expiryDate().format(DATE_FORMAT), "hr",
                    document.expired() || days <= 15 ? 3 : 2));
        }
    }

    /** Alertas apenas para gestores/admin: o sino não expõe informação médica a outros perfis. */
    private void addOccupationalHealth(List<NotificationItem> items) {
        if (!SignedInUser.isManagerOrAdmin()) return;
        for (OccupationalHealthExamDTO exam : hrApiClient.getExpiringOccupationalHealthExams()) {
            long days = exam.daysUntilExpiry();
            String title = days < 0
                    ? "Exame ocupacional expirado: " + exam.employeeName()
                    : days == 0
                            ? "Exame ocupacional expira hoje: " + exam.employeeName()
                            : "Renovar exame em " + days + " dia(s): " + exam.employeeName();
            items.add(new NotificationItem("Saúde Ocupacional", title,
                    "Agendar exame periódico · " + fitnessLabel(exam.fitnessResult()),
                    exam.expiryDate().format(DATE_FORMAT), "hr", days <= 15 ? 3 : 2));
        }
        // Quem nunca fez exame não tem validade a caducar, logo nunca entrava no ciclo acima — e é
        // o caso mais grave dos dois perante a inspecção do trabalho. Mesma lição das obrigações
        // sem prazo do §B5: a ausência precisa de linha própria ou é a única coisa que desaparece.
        for (MissingHealthExamDTO missing : hrApiClient.getEmployeesMissingHealthExam()) {
            items.add(new NotificationItem("Saúde Ocupacional",
                    "Sem exame de aptidão: " + missing.employeeName(),
                    missing.daysSinceHire() == null
                            ? "Admissão por registar · exame de admissão nunca realizado"
                            : "Admitido há " + missing.daysSinceHire() + " dia(s) sem exame de admissão",
                    missing.hireDate() == null ? "—" : missing.hireDate().format(DATE_FORMAT), "hr", 3));
        }
    }

    private void addGoals(List<NotificationItem> items, Long companyId) {
        if (performanceApiClient == null || companyId == null) return;
        try {
            List<mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO> list = performanceApiClient.getActiveGoalsSummary(companyId);
            if (list == null) return;
            for (var p : list) {
                if (p.alertLevel() == mz.multicore.erp.modules.performance.model.AlertLevel.CRITICAL) {
                    items.add(new NotificationItem(
                            "Meta Comercial",
                            "Meta em Risco Crítico: " + p.goalName(),
                            "Progresso: " + p.progressPct() + "% · Alvo: " + (p.targetRevenue() != null ? p.targetRevenue() : "—") + " MZN",
                            null,
                            "desempenho",
                            3
                    ));
                } else if (p.alertLevel() == mz.multicore.erp.modules.performance.model.AlertLevel.LATE) {
                    items.add(new NotificationItem(
                            "Meta Comercial",
                            "Meta Atrasada: " + p.goalName(),
                            "Progresso: " + p.progressPct() + "% · Alvo: " + (p.targetRevenue() != null ? p.targetRevenue() : "—") + " MZN",
                            null,
                            "desempenho",
                            2
                    ));
                }
            }
        } catch (Exception ignored) {
            // Degrada suavemente se serviço de desempenho estiver temporariamente indisponível
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

    private static String humanDocumentType(String type) {
        if (type == null) return "Documento";
        return switch (type) {
            case "ORDER" -> "Encomenda";
            case "INVOICE" -> "Fatura";
            case "DELIVERY_GUIDE" -> "Guia de Remessa";
            case "EXPENSE" -> "Despesa";
            default -> type.replace('_', ' ');
        };
    }

    private void addCreditRiskAlerts(List<NotificationItem> items) {
        if (creditRiskApiClient == null) return;
        try {
            CreditRiskSummaryDTO summary = creditRiskApiClient.getSummary(LocalDate.now());
            if (summary == null) return;
            int criticalCount = summary.criticalRiskCount();
            int blockedCount = summary.blockedClientsCount();
            int highCount = summary.highRiskCount();
            BigDecimal overdue = summary.totalOverdue();

            if (criticalCount > 0 || blockedCount > 0) {
                int totalAffected = criticalCount + blockedCount;
                String title = String.format("Risco Crítico: %d cliente(s) bloqueado(s) ou em incumprimento", totalAffected);
                String detail = String.format("Total vencido em mora: %s. Ação de cobrança requerida.",
                        formatMoney(overdue != null ? overdue : BigDecimal.ZERO));
                items.add(new NotificationItem("Risco de Crédito", title, detail, "Cobrança urgente", "risco_credito", 3));
            } else if (highCount > 0 && overdue != null && overdue.signum() > 0) {
                String title = String.format("Risco Elevado: %d cliente(s) com faturas em mora", highCount);
                String detail = String.format("Total vencido em mora: %s.", formatMoney(overdue));
                items.add(new NotificationItem("Risco de Crédito", title, detail, "Acompanhar", "risco_credito", 2));
            }
        } catch (Exception ignored) {
            // Degrada suavemente se API estiver indisponível
        }
    }

    private void addStockWasteAlerts(List<NotificationItem> items, Long companyId) {
        if (stockWasteApiClient == null || companyId == null) return;
        try {
            List<StockWasteDTO> pending = stockWasteApiClient.findByCompany(companyId, WasteStatus.PENDING_APPROVAL);
            if (pending != null && !pending.isEmpty()) {
                BigDecimal totalLoss = pending.stream()
                        .map(StockWasteDTO::totalCost)
                        .filter(v -> v != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String title = String.format("%d Quebra(s) de Stock pendente(s) de validação", pending.size());
                String detail = String.format("Perda estimada: %s. Alçada gerencial requerida.", formatMoney(totalLoss));
                items.add(new NotificationItem("Quebras de Stock", title, detail, "Aprovação pendente", "stock_waste", 2));
            }
        } catch (Exception ignored) {
            // Degrada suavemente se API estiver indisponível
        }
    }

    private static String formatMoney(BigDecimal val) {
        if (val == null) return "0,00 MT";
        return MZN_FMT.format(val.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    public record NotificationItem(
            String type,
            String title,
            String detail,
            String when,
            String moduleCard,
            int priority
    ) {}
}
