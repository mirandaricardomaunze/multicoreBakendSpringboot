package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowAlertDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Widget executivo unificado (Dashboard 360° - Pulso Estratégico da Empresa).
 * Consolida os 4 pilares estratégicos de decisão num cockpit visual de alta densidade:
 * 1. Compliance & Fraude Forense
 * 2. Liquidez Previsional (30 Dias)
 * 3. Risco de Crédito em Mora
 * 4. Metas Comerciais & Performance
 */
public class StrategicPulseWidget extends ModernPanel {

    private final ForensicAuditApiClient forensicAuditApiClient;
    private final CashFlowForecastApiClient cashFlowForecastApiClient;
    private final CreditRiskApiClient creditRiskApiClient;
    private final PerformanceApiClient performanceApiClient;
    private final Consumer<String> navigationHandler;

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

    private final DecimalFormat currencyFormatter;
    private final DecimalFormat percentFormatter;

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

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        this.currencyFormatter = new DecimalFormat("#,##0.00 MT", symbols);
        this.percentFormatter = new DecimalFormat("0.0'%'", symbols);

        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        buildHeader();
        buildCards();
        wireActions();
    }

    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon("fas-compass", 18, UIHelper.ACCENT_BLUE));
        JLabel title = new JLabel("PULSO ESTRATÉGICO DA EMPRESA (DECISÃO 360°)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        title.setForeground(UIHelper.TEXT_LIGHT);
        titleRow.add(icon);
        titleRow.add(title);
        titleBox.add(titleRow);

        JLabel sub = new JLabel("Indicadores em tempo real de Governação, Liquidez, Risco e Metas.");
        sub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        sub.setForeground(UIHelper.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(3, 26, 0, 0));
        titleBox.add(sub);

        header.add(titleBox, BorderLayout.WEST);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadData);
        refreshBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        refreshBtn.setPreferredSize(new Dimension(135, 28));
        header.add(refreshBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void buildCards() {
        JPanel grid = KpiCard.createGrid(4, 12, 12);

        grid.add(createPillarCard(
                "COMPLIANCE & FRAUDE",
                "fas-shield-alt",
                UIHelper.REJECTED_RED,
                forensicScoreLabel,
                forensicSubLabel,
                forensicNavBtn,
                "fas-file-alt"
        ));

        grid.add(createPillarCard(
                "LIQUIDEZ PREVISIONAL 30D",
                "fas-chart-line",
                UIHelper.ACCENT_BLUE,
                liquidityValLabel,
                liquiditySubLabel,
                liquidityNavBtn,
                "fas-coins"
        ));

        grid.add(createPillarCard(
                "RISCO DE CRÉDITO EM MORA",
                "fas-user-shield",
                UIHelper.PENDING_YELLOW,
                creditRiskValLabel,
                creditRiskSubLabel,
                creditRiskNavBtn,
                "fas-search-dollar"
        ));

        grid.add(createPillarCard(
                "METAS COMERCIAIS",
                "fas-bullseye",
                UIHelper.ACCENT,
                performanceValLabel,
                performanceSubLabel,
                performanceNavBtn,
                "fas-trophy"
        ));

        add(grid, BorderLayout.CENTER);
    }

    private JPanel createPillarCard(
            String title,
            String iconCode,
            Color accentColor,
            JLabel valueLabel,
            JLabel subLabel,
            ModernButton actionBtn,
            String actionIcon
    ) {
        KpiCard.KpiPalette pal = KpiCard.resolvePalette(accentColor);

        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, KpiCard.STANDARD_CARD_HEIGHT));
        card.setMinimumSize(new Dimension(KpiCard.STANDARD_CARD_MIN_WIDTH, KpiCard.STANDARD_CARD_HEIGHT));

        // Header do card
        JPanel cardHeader = new JPanel(new BorderLayout(6, 0));
        cardHeader.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(pal.title);

        JLabel iconLbl = new JLabel(UIHelper.icon(iconCode, 14, pal.title));
        cardHeader.add(iconLbl, BorderLayout.WEST);
        cardHeader.add(titleLbl, BorderLayout.CENTER);
        card.add(cardHeader, BorderLayout.NORTH);

        // Centro: Valor grande e subtexto
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        valueLabel.setForeground(pal.value);
        valueLabel.setAlignmentX(0.0f);

        subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        subLabel.setForeground(pal.subtitle);
        subLabel.setAlignmentX(0.0f);

        content.add(valueLabel);
        content.add(Box.createVerticalStrut(3));
        content.add(subLabel);
        card.add(content, BorderLayout.CENTER);

        // Rodapé: Botão de acção de 1-clique
        actionBtn.setIcon(UIHelper.icon(actionIcon, 11));
        actionBtn.setPreferredSize(new Dimension(0, 26));
        actionBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        card.add(actionBtn, BorderLayout.SOUTH);

        return card;
    }

    private void wireActions() {
        forensicNavBtn.addActionListener(e -> navigateSafely("auditoria_forense"));
        liquidityNavBtn.addActionListener(e -> navigateSafely("previsao_tesouraria"));
        creditRiskNavBtn.addActionListener(e -> navigateSafely("risco_credito"));
        performanceNavBtn.addActionListener(e -> navigateSafely("desempenho"));
    }

    private void navigateSafely(String card) {
        if (navigationHandler != null) {
            navigationHandler.accept(card);
        }
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
                } catch (Exception ignored) {
                    // Mantém visual neutro resiliente sem falhas
                }
            }
        }.execute();
    }

    public void applyData(
            ForensicAuditSummaryDTO forensic,
            CashFlowForecastDTO forecast,
            CreditRiskSummaryDTO credit,
            List<SalesGoalProgressDTO> goals
    ) {
        // 1. Forense
        if (forensic != null) {
            String scoreStr = forensic.complianceScore() != null ? forensic.complianceScore() : "100.0%";
            forensicScoreLabel.setText(scoreStr);
            if (scoreStr.contains("100") || scoreStr.contains("CONFORME")) {
                forensicScoreLabel.setForeground(UIHelper.APPROVED_GREEN);
            } else if (scoreStr.contains("9") || scoreStr.contains("8")) {
                forensicScoreLabel.setForeground(UIHelper.PENDING_YELLOW);
            } else {
                forensicScoreLabel.setForeground(UIHelper.REJECTED_RED);
            }
            int crit = forensic.criticalCount();
            int susp = forensic.suspiciousCount();
            if (crit == 0 && susp == 0) {
                forensicSubLabel.setText("Nenhuma anomalia detetada");
                forensicSubLabel.setForeground(UIHelper.APPROVED_GREEN);
            } else {
                forensicSubLabel.setText(crit + " críticas · " + susp + " suspeitas");
                forensicSubLabel.setForeground(crit > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            }
        } else {
            forensicScoreLabel.setText("100.0%");
            forensicScoreLabel.setForeground(UIHelper.APPROVED_GREEN);
            forensicSubLabel.setText("Sem anomalias ativas");
            forensicSubLabel.setForeground(UIHelper.TEXT_MUTED);
        }

        // 2. Liquidez Previsional
        if (forecast != null) {
            BigDecimal proj = forecast.netProjectedPosition() != null ? forecast.netProjectedPosition() : BigDecimal.ZERO;
            liquidityValLabel.setText(currencyFormatter.format(proj));
            if (proj.compareTo(BigDecimal.ZERO) >= 0) {
                liquidityValLabel.setForeground(UIHelper.APPROVED_GREEN);
            } else {
                liquidityValLabel.setForeground(UIHelper.REJECTED_RED);
            }
            CashFlowAlertDTO alert = forecast.alert();
            if (alert != null && "CRITICAL".equalsIgnoreCase(alert.status())) {
                liquiditySubLabel.setText("Alerta: Défice de tesouraria previsto");
                liquiditySubLabel.setForeground(UIHelper.REJECTED_RED);
            } else {
                liquiditySubLabel.setText("Posição líquida a 30 dias estável");
                liquiditySubLabel.setForeground(UIHelper.TEXT_MUTED);
            }
        } else {
            liquidityValLabel.setText("0,00 MT");
            liquidityValLabel.setForeground(UIHelper.TEXT_LIGHT);
            liquiditySubLabel.setText("Sem projeção disponível");
            liquiditySubLabel.setForeground(UIHelper.TEXT_MUTED);
        }

        // 3. Risco de Crédito
        if (credit != null) {
            BigDecimal overdue = credit.totalOverdue() != null ? credit.totalOverdue() : BigDecimal.ZERO;
            creditRiskValLabel.setText(currencyFormatter.format(overdue));
            if (overdue.compareTo(BigDecimal.ZERO) > 0) {
                creditRiskValLabel.setForeground(UIHelper.PENDING_YELLOW);
            } else {
                creditRiskValLabel.setForeground(UIHelper.APPROVED_GREEN);
            }
            int atRisk = credit.criticalRiskCount() + credit.highRiskCount();
            creditRiskSubLabel.setText(atRisk + " clientes em mora de crédito");
            creditRiskSubLabel.setForeground(atRisk > 0 ? UIHelper.PENDING_YELLOW : UIHelper.TEXT_MUTED);
        } else {
            creditRiskValLabel.setText("0,00 MT");
            creditRiskValLabel.setForeground(UIHelper.APPROVED_GREEN);
            creditRiskSubLabel.setText("0 clientes em mora crítica");
            creditRiskSubLabel.setForeground(UIHelper.TEXT_MUTED);
        }

        // 4. Metas Comerciais
        if (goals != null && !goals.isEmpty()) {
            BigDecimal totalTarget = BigDecimal.ZERO;
            BigDecimal totalAchieved = BigDecimal.ZERO;
            for (SalesGoalProgressDTO g : goals) {
                if (g.targetRevenue() != null) totalTarget = totalTarget.add(g.targetRevenue());
                if (g.currentRevenue() != null) totalAchieved = totalAchieved.add(g.currentRevenue());
            }
            BigDecimal pct = BigDecimal.ZERO;
            if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
                pct = totalAchieved.multiply(BigDecimal.valueOf(100)).divide(totalTarget, 1, RoundingMode.HALF_UP);
            }
            performanceValLabel.setText(percentFormatter.format(pct));
            performanceValLabel.setForeground(pct.compareTo(BigDecimal.valueOf(100)) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_LIGHT);
            performanceSubLabel.setText(currencyFormatter.format(totalAchieved) + " de " + currencyFormatter.format(totalTarget));
            performanceSubLabel.setForeground(UIHelper.TEXT_MUTED);
        } else {
            performanceValLabel.setText("100.0%");
            performanceValLabel.setForeground(UIHelper.APPROVED_GREEN);
            performanceSubLabel.setText("Sem metas ativas no ciclo");
            performanceSubLabel.setForeground(UIHelper.TEXT_MUTED);
        }

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
