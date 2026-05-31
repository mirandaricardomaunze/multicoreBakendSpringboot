package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.crm.service.CRMService;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.purchases.model.Purchase;
import com.phcpro.modules.purchases.service.PurchaseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final ComercialService comercialService;
    private final FinanceService financeService;
    private final ApprovalService approvalService;
    private final CRMService crmService;
    private final PurchaseService purchaseService;
    private final InventoryService inventoryService;

    private JLabel welcomeLabel;
    private JLabel balanceValLabel;
    private JLabel salesValLabel;
    private JLabel approvalsValLabel;
    private JLabel ticketsValLabel;
    private JLabel taxSummaryLabel;
    private JLabel taxDetailLabel;
    private JLabel stockAlertsLabel;
    private SimpleBarChartPanel financialChart;
    private SimpleBarChartPanel operationsChart;

    public DashboardPanel(
            ComercialService comercialService,
            FinanceService financeService,
            ApprovalService approvalService,
            CRMService crmService,
            PurchaseService purchaseService,
            InventoryService inventoryService
    ) {
        this.comercialService = comercialService;
        this.financeService = financeService;
        this.approvalService = approvalService;
        this.crmService = crmService;
        this.purchaseService = purchaseService;
        this.inventoryService = inventoryService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        welcomeLabel = new JLabel("Olá, SYSTEM! Bem-vindo ao MULTICORE.");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeLabel.setForeground(UIHelper.TEXT_LIGHT);
        headerPanel.add(welcomeLabel, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Visão geral das operações da sua empresa.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        JPanel dashboardContent = new JPanel(new BorderLayout(0, 16));
        dashboardContent.setOpaque(false);
        dashboardContent.setBorder(new EmptyBorder(18, 0, 0, 0));

        // Compact KPI cards leave vertical room for charts.
        JPanel gridPanel = new JPanel(new GridLayout(2, 3, 12, 12));
        gridPanel.setOpaque(false);
        gridPanel.setPreferredSize(new Dimension(0, 220));

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
        taxDetailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        taxDetailLabel.setForeground(new Color(204, 251, 241));
        gridPanel.add(buildKpiCard(
                "RESUMO FISCAL DO IVA", "fas-percentage", new Color(204, 251, 241),
                taxSummaryLabel, taxDetailLabel,
                new Color(13, 148, 136), new Color(20, 184, 166)));

        stockAlertsLabel = newValueLabel("0 Artigos", 20);
        JLabel stockAlertsSub = new JLabel("Quantidade inferior a 5 unidades no armazém");
        stockAlertsSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        stockAlertsSub.setForeground(new Color(254, 226, 226));
        gridPanel.add(buildKpiCard(
                "ALERTAS DE STOCK BAIXO", "fas-exclamation-triangle", new Color(254, 226, 226),
                stockAlertsLabel, stockAlertsSub,
                new Color(220, 38, 38), new Color(185, 28, 28)));

        dashboardContent.add(gridPanel, BorderLayout.NORTH);

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsPanel.setOpaque(false);
        chartsPanel.setPreferredSize(new Dimension(0, 280));
        financialChart = new SimpleBarChartPanel("Vendas, Compras e IVA");
        operationsChart = new SimpleBarChartPanel("Operacoes");
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

        refreshData();
    }

    private JLabel newValueLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, size));
        l.setForeground(Color.WHITE);
        return l;
    }

    private ModernPanel buildKpiCard(String title, String iconCode, Color titleColor,
                                      JLabel valueLabel, JLabel subLabel,
                                      Color gradientStart, Color gradientEnd) {
        ModernPanel card = new ModernPanel(12, gradientStart, gradientEnd);
        card.setLayout(new BorderLayout(6, 6));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(titleColor);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 18, titleColor));
        titleRow.add(iconLabel, BorderLayout.EAST);

        card.add(titleRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        if (subLabel != null) {
            card.add(subLabel, BorderLayout.SOUTH);
        }
        return card;
    }

    private ModernPanel createChartCard(SimpleBarChartPanel chart) {
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
        if (financeService == null || comercialService == null || approvalService == null || crmService == null || purchaseService == null || inventoryService == null) {
            return;
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();

        // 1. Treasury balance sum
        BigDecimal totalBal = financeService.getAllAccounts().stream()
                .map(TreasuryAccountDTO::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        balanceValLabel.setText(String.format("%,.2f MT", totalBal));

        // 2. Sales sum
        List<InvoiceDTO> companyInvoices = comercialService.getInvoicesByCompany(companyId);
        BigDecimal totalSales = companyInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .map(InvoiceDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        salesValLabel.setText(String.format("%,.2f MT", totalSales));

        // 3. Pending approvals count
        int appCount = approvalService.getPendingRequests().size();
        approvalsValLabel.setText(appCount + " Pedidos");

        // 4. CRM unresolved tickets count
        long ticketCount = crmService.getAllTickets().stream()
                .filter(t -> "OPEN".equals(t.status()))
                .count();
        ticketsValLabel.setText(ticketCount + " Tickets Abertos");

        // 5. IVA Summary
        BigDecimal ivaLiquidado = companyInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .map(InvoiceDTO::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Purchase> companyPurchases = purchaseService.getPurchasesByCompany(companyId);
        BigDecimal ivaDeduzido = companyPurchases.stream()
                .filter(p -> !"CANCELLED".equals(p.getStatus()))
                .map(Purchase::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchases = companyPurchases.stream()
                .filter(p -> !"CANCELLED".equals(p.getStatus()))
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ivaLiquido = ivaLiquidado.subtract(ivaDeduzido);
        String labelPrefix = ivaLiquido.compareTo(BigDecimal.ZERO) >= 0 ? "IVA a Pagar: " : "IVA a Recuperar: ";
        taxSummaryLabel.setText(labelPrefix + String.format("%,.2f MT", ivaLiquido.abs()));
        taxDetailLabel.setText(String.format("Liquidado: %,.2f MT | Deduzido: %,.2f MT", ivaLiquidado, ivaDeduzido));

        // 6. Stock Alerts (stocks where quantity < 5)
        List<Stock> companyStocks = inventoryService.getStocksByCompany(companyId);
        long lowStocksCount = companyStocks.stream()
                .filter(s -> s.getQuantity().compareTo(BigDecimal.valueOf(5)) < 0)
                .count();
        stockAlertsLabel.setText(lowStocksCount + " Artigo" + (lowStocksCount == 1 ? "" : "s"));

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

    private static class SimpleBarChartPanel extends JPanel {
        private final String title;
        private String[] labels = new String[0];
        private BigDecimal[] values = new BigDecimal[0];
        private Color[] colors = new Color[0];

        private SimpleBarChartPanel(String title) {
            this.title = title;
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
            setPreferredSize(new Dimension(260, 220));
        }

        private void setData(String[] labels, BigDecimal[] values, Color[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int left = 36;
            int right = 18;
            int top = 48;
            int bottom = 42;
            int chartWidth = Math.max(1, width - left - right);
            int chartHeight = Math.max(1, height - top - bottom);

            g.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g.setColor(UIHelper.TEXT_LIGHT);
            g.drawString(title, 16, 26);

            g.setColor(new Color(55, 65, 81));
            g.drawLine(left, top + chartHeight, left + chartWidth, top + chartHeight);

            if (values.length == 0) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(UIHelper.TEXT_MUTED);
                g.drawString("Sem dados", left, top + 30);
                g.dispose();
                return;
            }

            BigDecimal max = BigDecimal.ONE;
            for (BigDecimal value : values) {
                if (value != null && value.abs().compareTo(max) > 0) {
                    max = value.abs();
                }
            }

            int count = Math.max(1, values.length);
            int slot = Math.max(34, chartWidth / count);
            int barWidth = Math.max(22, Math.min(54, slot - 18));

            for (int i = 0; i < values.length; i++) {
                BigDecimal rawValue = values[i] == null ? BigDecimal.ZERO : values[i].abs();
                double ratio = rawValue.divide(max, 6, RoundingMode.HALF_UP).doubleValue();
                int barHeight = Math.max(4, (int) Math.round(chartHeight * ratio));
                int x = left + i * slot + Math.max(0, (slot - barWidth) / 2);
                int y = top + chartHeight - barHeight;

                Color barColor = i < colors.length && colors[i] != null ? colors[i] : UIHelper.ACCENT_BLUE;
                g.setColor(barColor);
                g.fillRoundRect(x, y, barWidth, barHeight, 10, 10);

                g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g.setColor(UIHelper.TEXT_LIGHT);
                String valueText = formatCompact(rawValue);
                int valueWidth = g.getFontMetrics().stringWidth(valueText);
                g.drawString(valueText, x + (barWidth - valueWidth) / 2, Math.max(42, y - 7));

                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g.setColor(UIHelper.TEXT_MUTED);
                String label = i < labels.length ? labels[i] : "";
                int labelWidth = g.getFontMetrics().stringWidth(label);
                g.drawString(label, x + (barWidth - labelWidth) / 2, top + chartHeight + 22);
            }

            g.dispose();
        }

        private String formatCompact(BigDecimal value) {
            double number = value.doubleValue();
            if (Math.abs(number) >= 1_000_000) {
                return String.format("%.1fM", number / 1_000_000);
            }
            if (Math.abs(number) >= 1_000) {
                return String.format("%.1fk", number / 1_000);
            }
            return String.format("%.0f", number);
        }
    }
}
