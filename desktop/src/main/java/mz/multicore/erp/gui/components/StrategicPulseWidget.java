package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

/**
 * Widget executivo unificado (Dashboard 360° - Pulso Estratégico da Empresa).
 */
public class StrategicPulseWidget extends ModernPanel {

    private final ForensicAuditApiClient forensicAuditApiClient;
    private final CashFlowForecastApiClient cashFlowForecastApiClient;
    private final CreditRiskApiClient creditRiskApiClient;
    private final PerformanceApiClient performanceApiClient;
    private final Consumer<String> navigationHandler;
    private final StrategicPulseDataPresenter presenter = new StrategicPulseDataPresenter();

    private final JLabel forensicScoreLabel = new JLabel("—");
    private final JLabel forensicSubLabel = new JLabel("A carregar auditoria...");
    private final ModernButton forensicNavBtn = UIHelper.createSecondaryButton("Dossiê Forense");

    private final JLabel liquidityValLabel = new JLabel("—");
    private final JLabel liquiditySubLabel = new JLabel("A carregar liquidez...");
    private final ModernButton liquidityNavBtn = UIHelper.createSecondaryButton("Fluxo Previsional");

    private final JLabel creditRiskValLabel = new JLabel("—");
    private final JLabel creditRiskSubLabel = new JLabel("A carregar risco...");
    private final ModernButton creditRiskNavBtn = UIHelper.createSecondaryButton("Matriz de Risco");

    private final JLabel performanceValLabel = new JLabel("—");
    private final JLabel performanceSubLabel = new JLabel("A carregar metas...");
    private final ModernButton performanceNavBtn = UIHelper.createSecondaryButton("Desempenho");

    public StrategicPulseWidget(
            ForensicAuditApiClient forensicAuditApiClient,
            CashFlowForecastApiClient cashFlowForecastApiClient,
            CreditRiskApiClient creditRiskApiClient,
            PerformanceApiClient performanceApiClient,
            Consumer<String> navigationHandler
    ) {
        super(UIHelper.RADIUS_LG, UIHelper.BG_CARD);
        this.forensicAuditApiClient = forensicAuditApiClient;
        this.cashFlowForecastApiClient = cashFlowForecastApiClient;
        this.creditRiskApiClient = creditRiskApiClient;
        this.performanceApiClient = performanceApiClient;
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        add(StrategicPulseCardFactory.buildHeader(this::loadData), BorderLayout.NORTH);
        buildCards();
        wireActions();
    }

    private void buildCards() {
        JPanel grid = KpiCard.createGrid(4, 12, 12);
        grid.add(StrategicPulseCardFactory.createPillarCard(
                "COMPLIANCE & FRAUDE", "fas-shield-alt", UIHelper.REJECTED_RED,
                forensicScoreLabel, forensicSubLabel, forensicNavBtn, "fas-file-alt"));
        grid.add(StrategicPulseCardFactory.createPillarCard(
                "LIQUIDEZ PREVISIONAL 30D", "fas-chart-line", UIHelper.ACCENT_BLUE,
                liquidityValLabel, liquiditySubLabel, liquidityNavBtn, "fas-coins"));
        grid.add(StrategicPulseCardFactory.createPillarCard(
                "RISCO DE CRÉDITO EM MORA", "fas-user-shield", UIHelper.PENDING_YELLOW,
                creditRiskValLabel, creditRiskSubLabel, creditRiskNavBtn, "fas-search-dollar"));
        grid.add(StrategicPulseCardFactory.createPillarCard(
                "METAS COMERCIAIS", "fas-bullseye", UIHelper.ACCENT,
                performanceValLabel, performanceSubLabel, performanceNavBtn, "fas-trophy"));
        add(grid, BorderLayout.CENTER);
    }

    private void wireActions() {
        forensicNavBtn.addActionListener(e -> navigateSafely("auditoria_forense"));
        liquidityNavBtn.addActionListener(e -> navigateSafely("previsao_tesouraria"));
        creditRiskNavBtn.addActionListener(e -> navigateSafely("risco_credito"));
        performanceNavBtn.addActionListener(e -> navigateSafely("desempenho"));
    }

    private void navigateSafely(String card) {
        if (navigationHandler != null) navigationHandler.accept(card);
    }

    public void loadData() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        new SwingWorker<PulseData, Void>() {
            @Override
            protected PulseData doInBackground() {
                ForensicAuditSummaryDTO forensic = null;
                CashFlowForecastDTO forecast = null;
                CreditRiskSummaryDTO credit = null;
                List<SalesGoalProgressDTO> goals = null;
                if (forensicAuditApiClient != null) {
                    try { forensic = forensicAuditApiClient.getSummary(null, null, null, null, null); } catch (Exception ignored) {}
                }
                if (cashFlowForecastApiClient != null) {
                    try { forecast = cashFlowForecastApiClient.getForecast(); } catch (Exception ignored) {}
                }
                if (creditRiskApiClient != null) {
                    try { credit = creditRiskApiClient.getSummary(null); } catch (Exception ignored) {}
                }
                if (performanceApiClient != null) {
                    try { goals = performanceApiClient.getActiveGoalsSummary(companyId); } catch (Exception ignored) {}
                }
                return new PulseData(forensic, forecast, credit, goals);
            }

            @Override
            protected void done() {
                try {
                    PulseData data = get();
                    applyData(data.forensic, data.forecast, data.credit, data.goals);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void applyData(
            ForensicAuditSummaryDTO forensic,
            CashFlowForecastDTO forecast,
            CreditRiskSummaryDTO credit,
            List<SalesGoalProgressDTO> goals
    ) {
        presenter.applyForensic(forensic, forensicScoreLabel, forensicSubLabel);
        presenter.applyLiquidity(forecast, liquidityValLabel, liquiditySubLabel);
        presenter.applyCreditRisk(credit, creditRiskValLabel, creditRiskSubLabel);
        presenter.applyGoals(goals, performanceValLabel, performanceSubLabel);
        revalidate();
        repaint();
    }

    public JLabel getForensicScoreLabel() { return forensicScoreLabel; }
    public JLabel getLiquidityValLabel() { return liquidityValLabel; }
    public JLabel getCreditRiskValLabel() { return creditRiskValLabel; }
    public JLabel getPerformanceValLabel() { return performanceValLabel; }
    public ModernButton getForensicNavBtn() { return forensicNavBtn; }
    public ModernButton getLiquidityNavBtn() { return liquidityNavBtn; }
    public ModernButton getCreditRiskNavBtn() { return creditRiskNavBtn; }
    public ModernButton getPerformanceNavBtn() { return performanceNavBtn; }

    private record PulseData(
            ForensicAuditSummaryDTO forensic,
            CashFlowForecastDTO forecast,
            CreditRiskSummaryDTO credit,
            List<SalesGoalProgressDTO> goals
    ) {}
}
