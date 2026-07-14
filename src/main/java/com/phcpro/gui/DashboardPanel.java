package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.KpiCard;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.SimpleBarChart;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.desktop.client.ApprovalApiClient;
import com.phcpro.desktop.client.CRMApiClient;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.FinanceApiClient;
import com.phcpro.desktop.client.InventoryApiClient;
import com.phcpro.desktop.client.PurchaseApiClient;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.inventory.dto.StockDTO;
import com.phcpro.modules.purchases.dto.PurchaseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final ComercialApiClient comercialApiClient;
    private final FinanceApiClient financeApiClient;
    private final ApprovalApiClient approvalApiClient;
    private final CRMApiClient crmApiClient;
    private final PurchaseApiClient purchaseApiClient;
    private final InventoryApiClient inventoryApiClient;

    private JLabel welcomeLabel;
    private JLabel balanceValLabel;
    private JLabel salesValLabel;
    private JLabel approvalsValLabel;
    private JLabel ticketsValLabel;
    private JLabel taxSummaryLabel;
    private JLabel taxDetailLabel;
    private JLabel stockAlertsLabel;
    private JLabel expiryAlertsLabel;
    private JLabel expiryAlertsSub;

    /** Horizonte (dias) do alerta de validade no dashboard. */
    private static final int EXPIRY_ALERT_DAYS = 30;
    private SimpleBarChart financialChart;
    private SimpleBarChart operationsChart;

    public DashboardPanel(
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient,
            CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient
    ) {
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.approvalApiClient = approvalApiClient;
        this.crmApiClient = crmApiClient;
        this.purchaseApiClient = purchaseApiClient;
        this.inventoryApiClient = inventoryApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        welcomeLabel = new JLabel("Olá, SYSTEM! Bem-vindo ao MULTICORE.");
        welcomeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 24));
        welcomeLabel.setForeground(UIHelper.TEXT_LIGHT);
        headerPanel.add(welcomeLabel, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Visão geral das operações da sua empresa.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 14));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        JPanel dashboardContent = new JPanel(new BorderLayout(0, 16));
        dashboardContent.setOpaque(false);
        dashboardContent.setBorder(new EmptyBorder(18, 0, 0, 0));

        // Compact KPI cards leave vertical room for charts. Grelha de 3 colunas, linhas automáticas
        // (7 cartões → 3 linhas), dentro do scroll.
        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 12, 12));
        gridPanel.setOpaque(false);
        gridPanel.setPreferredSize(new Dimension(0, 330));

        balanceValLabel = newValueLabel("0.00 MT", 20);
        gridPanel.add(buildKpiCard(
                "SALDO DE TESOURARIA", "fas-piggy-bank", new Color(224, 242, 254),
                balanceValLabel, null,
                new Color(9, 79, 172), new Color(13, 148, 136)));

        salesValLabel = newValueLabel("0.00 MT", 20);
        gridPanel.add(buildKpiCard(
                "TOTAL FATURADO (VENDAS)", "fas-file-invoice-dollar", new Color(243, 232, 255),
                salesValLabel, null,
                new Color(109, 40, 217), new Color(147, 51, 234)));

        approvalsValLabel = newValueLabel("0 Pedidos", 20);
        gridPanel.add(buildKpiCard(
                "APROVAÇÕES PENDENTES", "fas-clipboard-check", new Color(254, 243, 199),
                approvalsValLabel, null,
                new Color(180, 83, 9), new Color(217, 119, 6)));

        ticketsValLabel = newValueLabel("0 Tickets", 20);
        gridPanel.add(buildKpiCard(
                "SUPORTE CRM / ASSISTÊNCIAS", "fas-headset", new Color(209, 213, 219),
                ticketsValLabel, null,
                new Color(15, 23, 42), new Color(30, 41, 59)));

        taxSummaryLabel = newValueLabel("IVA Líquido: 0.00 MT", 18);
        taxDetailLabel = new JLabel("Liquidado: 0.00 MT | Deduzido: 0.00 MT");
        taxDetailLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        taxDetailLabel.setForeground(new Color(204, 251, 241));
        gridPanel.add(buildKpiCard(
                "RESUMO FISCAL DO IVA", "fas-percentage", new Color(204, 251, 241),
                taxSummaryLabel, taxDetailLabel,
                new Color(13, 148, 136), new Color(20, 184, 166)));

        stockAlertsLabel = newValueLabel("0 Artigos", 20);
        JLabel stockAlertsSub = new JLabel("Quantidade inferior a 5 unidades no armazém");
        stockAlertsSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        stockAlertsSub.setForeground(new Color(254, 226, 226));
        gridPanel.add(buildKpiCard(
                "ALERTAS DE STOCK BAIXO", "fas-exclamation-triangle", new Color(254, 226, 226),
                stockAlertsLabel, stockAlertsSub,
                new Color(220, 38, 38), new Color(185, 28, 28)));

        expiryAlertsLabel = newValueLabel("0 Lotes", 20);
        expiryAlertsSub = new JLabel("Vencidos ou a vencer em ≤ " + EXPIRY_ALERT_DAYS + " dias");
        expiryAlertsSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        expiryAlertsSub.setForeground(new Color(255, 237, 213));
        gridPanel.add(buildKpiCard(
                "ALERTAS DE VALIDADE", "fas-calendar-times", new Color(255, 237, 213),
                expiryAlertsLabel, expiryAlertsSub,
                new Color(194, 65, 12), new Color(234, 88, 12)));

        dashboardContent.add(gridPanel, BorderLayout.NORTH);

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsPanel.setOpaque(false);
        chartsPanel.setPreferredSize(new Dimension(0, 280));
        financialChart = new SimpleBarChart("Vendas, Compras e IVA");
        operationsChart = new SimpleBarChart("Operacoes");
        chartsPanel.add(createChartCard(financialChart));
        chartsPanel.add(createChartCard(operationsChart));
        dashboardContent.add(chartsPanel, BorderLayout.CENTER);

        // Scroll wrapper so cards + charts stay accessible on smaller windows
        JScrollPane scroll = new JScrollPane(dashboardContent);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Popula ao arrancar; se o backend falhar, não bloquear o login — o painel é repovoado ao
        // navegar (MainFrame.navigate("dashboard") chama refreshData()).
        try {
            refreshData();
        } catch (Exception ignored) {
            // arranque resiliente
        }
    }

    // Aspecto dos KPIs/gráficos partilhado com os outros painéis de visão geral (DRY): ver
    // com.phcpro.gui.components.KpiCard / SimpleBarChart.
    private JLabel newValueLabel(String text, int size) {
        return KpiCard.valueLabel(text, size);
    }

    private ModernPanel buildKpiCard(String title, String iconCode, Color titleColor,
                                      JLabel valueLabel, JLabel subLabel,
                                      Color gradientStart, Color gradientEnd) {
        return KpiCard.create(title, iconCode, titleColor, valueLabel, subLabel, gradientStart, gradientEnd);
    }

    private ModernPanel createChartCard(SimpleBarChart chart) {
        ModernPanel card = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    public void updateWelcomeMessage(String username, String role) {
        welcomeLabel.setText("Olá, " + username);
    }

    public void refreshData() {
        if (financeApiClient == null || comercialApiClient == null || approvalApiClient == null || crmApiClient == null || purchaseApiClient == null || inventoryApiClient == null) {
            return;
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();

        // 1. Treasury balance sum
        BigDecimal totalBal = financeApiClient.getAllAccounts().stream()
                .map(TreasuryAccountDTO::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        balanceValLabel.setText(String.format("%,.2f MT", totalBal));

        // 2. Sales sum
        List<InvoiceDTO> companyInvoices = comercialApiClient.getAllInvoices();
        BigDecimal totalSales = companyInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .map(InvoiceDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        salesValLabel.setText(String.format("%,.2f MT", totalSales));

        // 3. Pending approvals count
        int appCount = approvalApiClient.getPendingRequests().size();
        approvalsValLabel.setText(appCount + " Pedidos");

        // 4. CRM unresolved tickets count
        long ticketCount = crmApiClient.getAllTickets().stream()
                .filter(t -> "OPEN".equals(t.status()))
                .count();
        ticketsValLabel.setText(ticketCount + " Tickets Abertos");

        // 5. IVA Summary
        BigDecimal ivaLiquidado = companyInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .map(InvoiceDTO::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PurchaseDTO> companyPurchases = purchaseApiClient.getPurchasesByCompany(companyId);
        BigDecimal ivaDeduzido = companyPurchases.stream()
                .filter(p -> !"CANCELLED".equals(p.status()))
                .map(PurchaseDTO::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchases = companyPurchases.stream()
                .filter(p -> !"CANCELLED".equals(p.status()))
                .map(PurchaseDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ivaLiquido = ivaLiquidado.subtract(ivaDeduzido);
        String labelPrefix = ivaLiquido.compareTo(BigDecimal.ZERO) >= 0 ? "IVA a Pagar: " : "IVA a Recuperar: ";
        taxSummaryLabel.setText(labelPrefix + String.format("%,.2f MT", ivaLiquido.abs()));
        taxDetailLabel.setText(String.format("Liquidado: %,.2f MT | Deduzido: %,.2f MT", ivaLiquidado, ivaDeduzido));

        // 6. Stock Alerts (stocks where quantity < 5)
        List<StockDTO> companyStocks = inventoryApiClient.getStocksByCompany(companyId);
        long lowStocksCount = companyStocks.stream()
                .filter(s -> s.quantity().compareTo(BigDecimal.valueOf(5)) < 0)
                .count();
        stockAlertsLabel.setText(lowStocksCount + " Artigo" + (lowStocksCount == 1 ? "" : "s"));

        // 7. Alertas de validade — lotes vencidos ou a vencer no horizonte definido
        java.time.LocalDate today = java.time.LocalDate.now();
        List<com.phcpro.modules.inventory.dto.ProductBatchDTO> expiring =
                inventoryApiClient.findExpiringBatches(companyId, EXPIRY_ALERT_DAYS);
        long expiredCount = expiring.stream()
                .filter(b -> b.expirationDate() != null && b.expirationDate().isBefore(today))
                .count();
        long soonCount = expiring.size() - expiredCount;
        expiryAlertsLabel.setText(expiring.size() + " Lote" + (expiring.size() == 1 ? "" : "s"));
        expiryAlertsSub.setText(String.format("%d vencido%s · %d a vencer (≤ %d dias)",
                expiredCount, expiredCount == 1 ? "" : "s", soonCount, EXPIRY_ALERT_DAYS));

        financialChart.setData(
                new String[]{"Vendas", "Compras", "IVA"},
                new BigDecimal[]{totalSales, totalPurchases, ivaLiquido.abs()},
                new Color[]{UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN, UIHelper.PENDING_YELLOW}
        );
        operationsChart.setData(
                new String[]{"Aprov.", "Tickets", "Stock"},
                new BigDecimal[]{
                        BigDecimal.valueOf(appCount),
                        BigDecimal.valueOf(ticketCount),
                        BigDecimal.valueOf(lowStocksCount)
                },
                new Color[]{UIHelper.PENDING_YELLOW, UIHelper.ACCENT, UIHelper.REJECTED_RED}
        );
    }

}
