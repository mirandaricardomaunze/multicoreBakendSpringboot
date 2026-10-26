package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Agrega alertas operacionais existentes para o bell e a página de notificações.
 */
public class NotificationFeed {

    private static final int EXPIRY_ALERT_DAYS = 30;
    private static final long SUBSCRIPTION_ALERT_DAYS = 7;
    private static final BigDecimal DEFAULT_LOW_STOCK = BigDecimal.valueOf(5);
    private static final long IN_TRANSIT_OVERDUE_HOURS = 48;

    private final ApprovalApiClient approvalApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final MySubscriptionApiClient subscriptionApiClient;
    private final HRApiClient hrApiClient;
    private final PerformanceApiClient performanceApiClient;
    private final CreditRiskApiClient creditRiskApiClient;
    private final StockWasteApiClient stockWasteApiClient;
    private final SystemMonitoringApiClient monitoringApiClient;
    private final StockTransferApiClient stockTransferApiClient;
    private final QualityApiClient qualityApiClient;

    public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient, HRApiClient hrApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, null, null, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient, HRApiClient hrApiClient,
                            PerformanceApiClient performanceApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, performanceApiClient, null, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient, HRApiClient hrApiClient,
                            PerformanceApiClient performanceApiClient, CreditRiskApiClient creditRiskApiClient,
                            StockWasteApiClient stockWasteApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, performanceApiClient, creditRiskApiClient, stockWasteApiClient, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient, HRApiClient hrApiClient,
                            PerformanceApiClient performanceApiClient, CreditRiskApiClient creditRiskApiClient,
                            StockWasteApiClient stockWasteApiClient, SystemMonitoringApiClient monitoringApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, performanceApiClient, creditRiskApiClient, stockWasteApiClient, monitoringApiClient, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient, HRApiClient hrApiClient,
                            PerformanceApiClient performanceApiClient, CreditRiskApiClient creditRiskApiClient,
                            StockWasteApiClient stockWasteApiClient, SystemMonitoringApiClient monitoringApiClient,
                            StockTransferApiClient stockTransferApiClient) {
        this(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient, performanceApiClient, creditRiskApiClient, stockWasteApiClient, monitoringApiClient, stockTransferApiClient, null);
    }

    public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient, HRApiClient hrApiClient,
                            PerformanceApiClient performanceApiClient, CreditRiskApiClient creditRiskApiClient,
                            StockWasteApiClient stockWasteApiClient, SystemMonitoringApiClient monitoringApiClient,
                            StockTransferApiClient stockTransferApiClient, QualityApiClient qualityApiClient) {
        this.approvalApiClient = approvalApiClient; this.inventoryApiClient = inventoryApiClient;
        this.subscriptionApiClient = subscriptionApiClient; this.hrApiClient = hrApiClient;
        this.performanceApiClient = performanceApiClient; this.creditRiskApiClient = creditRiskApiClient;
        this.stockWasteApiClient = stockWasteApiClient; this.monitoringApiClient = monitoringApiClient;
        this.stockTransferApiClient = stockTransferApiClient; this.qualityApiClient = qualityApiClient;
    }

    public List<NotificationItem> load(Long companyId) {
        List<NotificationItem> items = new ArrayList<>();
        addSystemAlerts(items);
        addApprovals(items);
        addCreditRiskAlerts(items);
        NotificationInventoryAlertProvider.addStockWasteAlerts(items, companyId, stockWasteApiClient);
        NotificationInventoryAlertProvider.addLowStock(items, companyId, inventoryApiClient, DEFAULT_LOW_STOCK);
        NotificationInventoryAlertProvider.addExpiries(items, companyId, inventoryApiClient, EXPIRY_ALERT_DAYS);
        addSubscription(items);
        NotificationHRAlertProvider.addContracts(items, hrApiClient);
        NotificationHRAlertProvider.addPayrollLiabilities(items, hrApiClient);
        NotificationHRAlertProvider.addEmployeeDocuments(items, hrApiClient);
        NotificationHRAlertProvider.addOccupationalHealth(items, hrApiClient);
        NotificationHRAlertProvider.addQualityAlerts(items, companyId, qualityApiClient);
        addGoals(items, companyId);
        NotificationInventoryAlertProvider.addInTransitOverdue(items, companyId, stockTransferApiClient, IN_TRANSIT_OVERDUE_HOURS);
        items.sort(Comparator.comparingInt(NotificationItem::priority).reversed()
                .thenComparing(NotificationItem::type).thenComparing(NotificationItem::title));
        return List.copyOf(items);
    }

    private void addApprovals(List<NotificationItem> items) {
        if (approvalApiClient == null) return;
        for (ApprovalRequestDTO approval : approvalApiClient.getPendingRequests()) {
            String detail = humanDocumentType(approval.documentType());
            if (approval.description() != null && !approval.description().isBlank()) detail += " — " + approval.description();
            String when = approval.createdAt() == null ? "Pendente" : approval.createdAt().toLocalDate().format(UIHelper.DATE_FMT);
            items.add(new NotificationItem("Aprovações", "Pedido de aprovação pendente", detail, when, "approvals", 2));
        }
    }

    private void addSubscription(List<NotificationItem> items) {
        if (subscriptionApiClient == null) return;
        MySubscriptionDTO sub = subscriptionApiClient.getMySubscription();
        if (sub == null || !sub.hasSubscription()) return;
        boolean blocked = "EXPIRED".equals(sub.status()) || "SUSPENDED".equals(sub.status());
        Long days = sub.daysRemaining();
        if (!blocked && (days == null || days < 0 || days > SUBSCRIPTION_ALERT_DAYS)) return;
        String status = sub.statusLabel() == null ? "indisponível" : sub.statusLabel();
        String title = blocked ? "Assinatura " + status.toLowerCase() : days == 0 ? "A assinatura expira hoje" : "A assinatura expira em " + days + " dia(s)";
        String detail = sub.planLabel() == null ? "Consulte a sua assinatura" : "Plano " + sub.planLabel();
        String when = sub.validUntil() == null ? status : sub.validUntil().format(UIHelper.DATE_FMT);
        items.add(new NotificationItem("Assinatura", title, detail, when, "config", blocked ? 3 : 2));
    }

    private void addGoals(List<NotificationItem> items, Long companyId) {
        if (performanceApiClient == null || companyId == null) return;
        try {
            var list = performanceApiClient.getActiveGoalsSummary(companyId);
            if (list == null) return;
            for (var p : list) {
                if (p.alertLevel() == mz.multicore.erp.modules.performance.model.AlertLevel.CRITICAL) {
                    items.add(new NotificationItem("Meta Comercial", "Meta em Risco Crítico: " + p.goalName(),
                            "Progresso: " + p.progressPct() + "% · Alvo: " + (p.targetRevenue() != null ? p.targetRevenue() : "—") + " MZN", null, "desempenho", 3));
                } else if (p.alertLevel() == mz.multicore.erp.modules.performance.model.AlertLevel.LATE) {
                    items.add(new NotificationItem("Meta Comercial", "Meta Atrasada: " + p.goalName(),
                            "Progresso: " + p.progressPct() + "% · Alvo: " + (p.targetRevenue() != null ? p.targetRevenue() : "—") + " MZN", null, "desempenho", 2));
                }
            }
        } catch (Exception ignored) {}
    }

    private static String humanDocumentType(String type) {
        if (type == null) return "Documento";
        return switch (type) {
            case "ORDER" -> "Encomenda"; case "INVOICE" -> "Fatura";
            case "DELIVERY_GUIDE" -> "Guia de Remessa"; case "EXPENSE" -> "Despesa";
            default -> type.replace('_', ' ');
        };
    }

    private void addCreditRiskAlerts(List<NotificationItem> items) {
        if (creditRiskApiClient == null) return;
        try {
            CreditRiskSummaryDTO s = creditRiskApiClient.getSummary(LocalDate.now());
            if (s == null) return;
            int crit = s.criticalRiskCount(); int blk = s.blockedClientsCount(); int high = s.highRiskCount(); BigDecimal overdue = s.totalOverdue();
            if (crit > 0 || blk > 0) {
                items.add(new NotificationItem("Risco de Crédito", String.format("Risco Crítico: %d cliente(s) bloqueado(s) ou em incumprimento", crit + blk),
                        String.format("Total vencido em mora: %s. Ação de cobrança requerida.", formatMoney(overdue != null ? overdue : BigDecimal.ZERO)),
                        "Cobrança urgente", "risco_credito", 3));
            } else if (high > 0 && overdue != null && overdue.signum() > 0) {
                items.add(new NotificationItem("Risco de Crédito", String.format("Risco Elevado: %d cliente(s) com faturas em mora", high),
                        String.format("Total vencido em mora: %s.", formatMoney(overdue)), "Acompanhar", "risco_credito", 2));
            }
        } catch (Exception ignored) {}
    }

    private void addSystemAlerts(List<NotificationItem> items) {
        if (monitoringApiClient == null || !SignedInUser.isManagerOrAdmin()) return;
        try {
            var incidents = monitoringApiClient.getIncidents();
            if (incidents == null) return;
            for (var inc : incidents) {
                if (!inc.resolved() && "CRITICAL".equalsIgnoreCase(inc.severity())) {
                    String when = inc.timestamp() != null ? inc.timestamp().atZone(java.time.ZoneId.systemDefault()).format(UIHelper.DATE_FMT) : "Agora";
                    items.add(new NotificationItem("Alerta do Sistema", "Falha Crítica: " + inc.subsystem(), inc.tenantName() + " — " + inc.message(), when, "config", 3));
                }
            }
        } catch (Exception ignored) {}
    }

    private static String formatMoney(BigDecimal val) { return UIHelper.formatMzn(val); }

    public record NotificationItem(String type, String title, String detail, String when, String moduleCard, int priority) {}
}
