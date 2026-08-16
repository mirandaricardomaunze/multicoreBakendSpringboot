package com.phcpro.gui;

import com.phcpro.desktop.client.ApprovalApiClient;
import com.phcpro.desktop.client.InventoryApiClient;
import com.phcpro.desktop.client.MySubscriptionApiClient;
import com.phcpro.modules.approvals.dto.ApprovalRequestDTO;
import com.phcpro.modules.inventory.dto.ProductBatchDTO;
import com.phcpro.modules.inventory.dto.StockDTO;
import com.phcpro.modules.subscription.dto.MySubscriptionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Agrega alertas operacionais existentes para o bell e a página de notificações. */
public class NotificationFeed {

    private static final int EXPIRY_ALERT_DAYS = 30;
    private static final long SUBSCRIPTION_ALERT_DAYS = 7;
    private static final BigDecimal DEFAULT_LOW_STOCK = BigDecimal.valueOf(5);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ApprovalApiClient approvalApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final MySubscriptionApiClient subscriptionApiClient;

    public NotificationFeed(ApprovalApiClient approvalApiClient,
                            InventoryApiClient inventoryApiClient,
                            MySubscriptionApiClient subscriptionApiClient) {
        this.approvalApiClient = approvalApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.subscriptionApiClient = subscriptionApiClient;
    }

    public List<NotificationItem> load(Long companyId) {
        List<NotificationItem> items = new ArrayList<>();
        addApprovals(items);
        addLowStock(items, companyId);
        addExpiries(items, companyId);
        addSubscription(items);
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

    public record NotificationItem(
            String type,
            String title,
            String detail,
            String when,
            String moduleCard,
            int priority
    ) {}
}
