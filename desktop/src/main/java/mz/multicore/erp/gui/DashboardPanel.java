package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.tv.ExecutiveTvDashboardDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Painel principal do Dashboard Executivo e Operacional.
 * Orquestrador desacoplado das abas de Visão Geral, Cockpit Operacional 360° e Rentabilidade/DRE.
 */
public class DashboardPanel extends JPanel {

    public enum PeriodFilter {
        HOJE("Hoje"),
        ESTA_SEMANA("Esta Semana"),
        ESTE_MES("Este Mês"),
        ESTE_ANO("Este Ano"),
        TODOS("Todo o Período");

        private final String label;
        PeriodFilter(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final ComercialApiClient comercialApiClient;
    private final DashboardDataCoordinator dataCoordinator;
    private final DashboardOverviewTab overviewTab;
    private final DashboardOperationsTab operationsTab;
    private final StrategicPulseWidget strategicPulseWidget;
    private final OperationalPulseWidget operationalPulseWidget;
    private final ProfitAnalyticsWidget profitAnalyticsWidget;

    private PeriodFilter currentPeriod = PeriodFilter.HOJE;
    private final Map<PeriodFilter, ModernButton> periodButtons = new HashMap<>();
    private JLabel welcomeLabel;

    public DashboardPanel(
            ComercialApiClient comercialApiClient, FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient, CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient, InventoryApiClient inventoryApiClient) {
        this(comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient,
                null, null, null, null, null, null);
    }

    public DashboardPanel(
            ComercialApiClient comercialApiClient, FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient, CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient, InventoryApiClient inventoryApiClient,
            ForensicAuditApiClient forensicAuditApiClient, CashFlowForecastApiClient cashFlowForecastApiClient,
            CreditRiskApiClient creditRiskApiClient, PerformanceApiClient performanceApiClient,
            Consumer<String> navigationHandler) {
        this(comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient,
                forensicAuditApiClient, cashFlowForecastApiClient, creditRiskApiClient, performanceApiClient, null, navigationHandler);
    }

    public DashboardPanel(
            ComercialApiClient comercialApiClient, FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient, CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient, InventoryApiClient inventoryApiClient,
            ForensicAuditApiClient forensicAuditApiClient, CashFlowForecastApiClient cashFlowForecastApiClient,
            CreditRiskApiClient creditRiskApiClient, PerformanceApiClient performanceApiClient,
            DashboardApiClient dashboardApiClient, Consumer<String> navigationHandler) {
        this.comercialApiClient = comercialApiClient;
        this.strategicPulseWidget = new StrategicPulseWidget(
                forensicAuditApiClient, cashFlowForecastApiClient, creditRiskApiClient, performanceApiClient, navigationHandler);
        this.operationalPulseWidget = new OperationalPulseWidget(dashboardApiClient, navigationHandler);
        this.profitAnalyticsWidget = new ProfitAnalyticsWidget();

        this.dataCoordinator = new DashboardDataCoordinator(
                comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient);
        this.overviewTab = new DashboardOverviewTab(navigationHandler);
        this.operationsTab = new DashboardOperationsTab(navigationHandler, strategicPulseWidget, operationalPulseWidget);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        add(buildHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane dashboardTabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(dashboardTabs);
        dashboardTabs.addTab("Visão Geral & Gráficos", UIHelper.icon("fas-chart-pie", 13, UIHelper.ACCENT_BLUE), overviewTab.buildTab());
        dashboardTabs.addTab("Cockpit Operacional 360°", UIHelper.icon("fas-cogs", 13, UIHelper.APPROVED_GREEN), buildOperationsTab());
        dashboardTabs.addTab("Rentabilidade & DRE", UIHelper.icon("fas-coins", 13, UIHelper.PENDING_YELLOW), buildProfitTab());
        add(dashboardTabs, BorderLayout.CENTER);
        registerKeyboardAction(e -> openExecutiveTvDashboard(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        refreshData();
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 6));
        header.setOpaque(false);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        welcomeLabel = new JLabel("Olá, Gestor! Bem-vindo ao MULTICORE.");
        welcomeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        welcomeLabel.setForeground(UIHelper.TEXT_LIGHT);
        stack.add(welcomeLabel);

        JLabel subtitle = new JLabel("Visão geral das operações, vendas e fluxo financeiro.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        stack.add(subtitle);

        header.add(stack, BorderLayout.WEST);
        header.add(buildPeriodFilterBar(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildPeriodFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        bar.setOpaque(false);

        ModernButton tvBtn = UIHelper.createSecondaryButton("Painel TV");
        tvBtn.setIcon(UIHelper.icon("fas-tv", 12, UIHelper.ACCENT_CYAN));
        tvBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        tvBtn.setToolTipText("Abrir Painel Executivo em Tela Cheia para Sala de Reuniões (F11)");
        tvBtn.setPreferredSize(new Dimension(tvBtn.getPreferredSize().width + 12, 28));
        tvBtn.addActionListener(e -> openExecutiveTvDashboard());
        bar.add(tvBtn);

        for (PeriodFilter p : PeriodFilter.values()) {
            ModernButton btn = UIHelper.createSecondaryButton(p.getLabel());
            btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 12, 28));
            btn.addActionListener(e -> selectPeriod(p));
            periodButtons.put(p, btn);
            bar.add(btn);
        }
        updatePeriodButtonStyles();
        return bar;
    }

    private JPanel buildOperationsTab() {
        JPanel panel = operationsTab.buildTab();
        JPanel tab2Center = new JPanel();
        tab2Center.add(operationalPulseWidget);
        return panel;
    }

    private JPanel buildProfitTab() {
        JPanel tab3 = new JPanel(new BorderLayout(0, 12));
        tab3.setOpaque(false);
        tab3.setBorder(new EmptyBorder(12, 4, 4, 4));
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(profitAnalyticsWidget);
        tab3.add(new ArrowScrollPanel(center), BorderLayout.CENTER);
        return tab3;
    }

    public void openExecutiveTvDashboard() {
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        ExecutiveTvDashboardDialog dialog = new ExecutiveTvDashboardDialog(ancestor, comercialApiClient, 1L);
        dialog.setVisible(true);
    }

    public void selectPeriod(PeriodFilter period) {
        if (period == null) return;
        this.currentPeriod = period;
        updatePeriodButtonStyles();
        refreshData();
    }

    public PeriodFilter getCurrentPeriod() { return currentPeriod; }

    private void updatePeriodButtonStyles() {
        for (Map.Entry<PeriodFilter, ModernButton> entry : periodButtons.entrySet()) {
            boolean active = entry.getKey() == currentPeriod;
            ModernButton btn = entry.getValue();
            if (active) {
                btn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
                btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            } else {
                Color idleBg = UIHelper.isLight() ? UIHelper.ROW_ALT : UIHelper.BG_CARD;
                Color idleHover = UIHelper.isLight() ? UIHelper.GRID : UIHelper.ROW_ALT;
                btn.setColors(idleBg, idleHover);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIHelper.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(3, 9, 3, 9)
                ));
            }
        }
    }

    public void updateWelcomeMessage(String username, String role) {
        welcomeLabel.setText("Olá, " + username);
    }

    public void refreshData() {
        if (strategicPulseWidget != null) strategicPulseWidget.loadData();
        if (operationalPulseWidget != null) operationalPulseWidget.loadData();

        UIHelper.loadAsync(this, () -> dataCoordinator.fetchDashboardData(currentPeriod), this::applyDashboardData, error -> {
            putClientProperty("loadError", error.getMessage());
            setToolTipText("Não foi possível actualizar o dashboard: " + error.getMessage());
        });
    }

    private void applyDashboardData(DashboardDataCoordinator.DashboardData data) {
        overviewTab.applyData(data, currentPeriod);
        operationsTab.applyData(data);
        profitAnalyticsWidget.updateMetrics(data.profitMetrics());
    }

    public StrategicPulseWidget getStrategicPulseWidget() { return strategicPulseWidget; }
    public OperationalPulseWidget getOperationalPulseWidget() { return operationalPulseWidget; }
}
