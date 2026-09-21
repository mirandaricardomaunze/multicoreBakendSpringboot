package mz.multicore.erp.gui.performance;

import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.AlertLevel;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ProgressTab extends JPanel {

    private final PerformanceApiClient apiClient;
    private final DesktopSession session;

    private JPanel cardsContainer;
    private InlineFeedbackPanel feedbackPanel;
    private static final DecimalFormat CURRENCY_FMT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        sym.setGroupingSeparator(' ');
        sym.setDecimalSeparator(',');
        CURRENCY_FMT = new DecimalFormat("#,##0.00", sym);
    }

    public ProgressTab(PerformanceApiClient apiClient, DesktopSession session) {
        this.apiClient = apiClient;
        this.session = session;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        buildUI();
    }

    private void buildUI() {
        feedbackPanel = new InlineFeedbackPanel();
        add(feedbackPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(UIHelper.BG_DARK);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBackground(UIHelper.BG_DARK);

        ModernButton btnRefresh = UIHelper.createSecondaryButton("Recarregar Progresso");
        btnRefresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnRefresh.addActionListener(e -> reload());
        toolbar.add(btnRefresh);

        mainPanel.add(toolbar, BorderLayout.NORTH);

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(UIHelper.BG_DARK);

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        UIHelper.styleScrollPane(scrollPane);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    public void reload() {
        feedbackPanel.clear();
        Long companyId = session.activeCompanyId();
        if (companyId == null) return;

        UIHelper.loadAsync(
                this,
                () -> apiClient.getActiveGoalsSummary(companyId),
                progressList -> renderCards(progressList),
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao calcular progresso: " + ex.getMessage())
        );
    }

    private void renderCards(List<SalesGoalProgressDTO> progressList) {
        cardsContainer.removeAll();

        if (progressList == null || progressList.isEmpty()) {
            JPanel emptyPanel = new ModernPanel();
            emptyPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 40));
            JLabel lblEmpty = new JLabel("Nenhuma meta comercial ativa no momento.");
            lblEmpty.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
            lblEmpty.setForeground(UIHelper.TEXT_MUTED);
            emptyPanel.add(lblEmpty);
            cardsContainer.add(emptyPanel);
        } else {
            for (SalesGoalProgressDTO p : progressList) {
                cardsContainer.add(createGoalCard(p));
                cardsContainer.add(Box.createRigidArea(new Dimension(0, 12)));
            }
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel createGoalCard(SalesGoalProgressDTO p) {
        ModernPanel card = new ModernPanel();
        card.setLayout(new BorderLayout(15, 12));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        // Top line: Name, Scope, and Status / Alert badges
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel(p.goalName() + " (" + (p.scopeLabel() != null ? p.scopeLabel() : p.scope().name()) + ")");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        title.setForeground(UIHelper.TEXT_LIGHT);
        headerRow.add(title, BorderLayout.WEST);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgeRow.setOpaque(false);

        // On Track badge
        JLabel lblTrack = new JLabel(p.isOnTrack() ? "No Ritmo" : "Atrasado");
        lblTrack.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        lblTrack.setForeground(Color.WHITE);
        lblTrack.setOpaque(true);
        lblTrack.setBackground(p.isOnTrack() ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
        lblTrack.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        badgeRow.add(lblTrack);

        // Alert Level badge
        JLabel lblAlert = createAlertBadge(p.alertLevel());
        badgeRow.add(lblAlert);

        headerRow.add(badgeRow, BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        // Center: Progress Bar and stats
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        centerPanel.setOpaque(false);

        // Progress bar with percentage label
        int pctInt = Math.min(100, p.progressPct() != null ? p.progressPct().intValue() : 0);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pctInt);
        bar.setStringPainted(true);
        bar.setString((p.progressPct() != null ? p.progressPct() : BigDecimal.ZERO) + "% Concluído");
        bar.setForeground(getColorForAlert(p.alertLevel()));
        bar.setPreferredSize(new Dimension(400, 24));
        centerPanel.add(bar);

        // Stats grid
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 10, 0));
        statsRow.setOpaque(false);

        statsRow.add(createMiniStat("Receita Atual", formatMoney(p.currentRevenue()) + " / " + formatMoney(p.targetRevenue())));
        statsRow.add(createMiniStat("Margem Bruta", formatMoney(p.currentMargin()) + " / " + formatMoney(p.targetMargin())));
        statsRow.add(createMiniStat("Projeção Fim do Período", formatMoney(p.projectedRevenue()) + " MZN"));
        statsRow.add(createMiniStat("Prémio Estimado", formatMoney(p.bonusEstimate()) + " MZN"));

        centerPanel.add(statsRow);
        card.add(centerPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMiniStat(String label, String value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        lbl.setForeground(UIHelper.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        val.setForeground(UIHelper.TEXT_LIGHT);

        p.add(lbl);
        p.add(val);
        return p;
    }

    private JLabel createAlertBadge(AlertLevel level) {
        String text = switch (level) {
            case NONE -> "Meta no Prazo";
            case CAUTION -> "Atenção (Ritmo Lento)";
            case LATE -> "Meta Atrasada";
            case CRITICAL -> "Atraso Crítico";
        };
        Color bg = getColorForAlert(level);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private Color getColorForAlert(AlertLevel level) {
        if (level == null) return UIHelper.APPROVED_GREEN;
        return switch (level) {
            case NONE -> UIHelper.APPROVED_GREEN;
            case CAUTION -> UIHelper.PENDING_YELLOW;
            case LATE -> UIHelper.KPI_ORANGE_DARK;
            case CRITICAL -> UIHelper.REJECTED_RED;
        };
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0,00";
        return CURRENCY_FMT.format(amount);
    }
}
