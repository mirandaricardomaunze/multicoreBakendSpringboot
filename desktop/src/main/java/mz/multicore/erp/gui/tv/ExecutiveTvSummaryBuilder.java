package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;

/**
 * Bloco de resumo partilhado pela visão consolidada e pela visão de unidade:
 * o cartão principal (faturado, % da meta, barra com ritmo esperado) e a fila de KPIs.
 */
public final class ExecutiveTvSummaryBuilder {

    private static final int KPI_GRID_WIDTH = 560;

    private ExecutiveTvSummaryBuilder() {}

    public static String money(BigDecimal value) {
        return String.format("%,.2f MT", value != null ? value : BigDecimal.ZERO);
    }

    /** Cartão principal à esquerda e KPIs 2×2 à direita: deixa altura livre para as unidades. */
    public static JPanel build(String title, ExecutiveTvMetrics m, boolean lightMode, Runnable onEditGoals) {
        JPanel summary = ExecutiveTvComponents.transparent(new BorderLayout(ExecutiveTvComponents.GAP, 0));
        summary.add(buildHero(title, m, lightMode, onEditGoals), BorderLayout.CENTER);
        JPanel kpis = buildKpiGrid(m, lightMode);
        kpis.setPreferredSize(new Dimension(KPI_GRID_WIDTH, kpis.getPreferredSize().height));
        summary.add(kpis, BorderLayout.EAST);
        return summary;
    }

    private static JPanel buildHero(String title, ExecutiveTvMetrics m, boolean lightMode, Runnable onEditGoals) {
        Color text = ExecutiveTvTheme.getTextLightColor(lightMode);
        Color muted = ExecutiveTvTheme.getTextMutedColor(lightMode);
        GoalStatus status = m.status();

        JPanel hero = ExecutiveTvComponents.card(lightMode, new BorderLayout(0, 10), 18);
        JPanel top = ExecutiveTvComponents.transparent(new BorderLayout());
        top.add(ExecutiveTvComponents.overline(title, lightMode), BorderLayout.WEST);
        top.add(ExecutiveTvTheme.buildStatusBadge(status, lightMode), BorderLayout.EAST);
        hero.add(top, BorderLayout.NORTH);

        JPanel figures = ExecutiveTvComponents.transparent(new BorderLayout(24, 0));
        figures.add(figure(money(m.billed()), text, m.hasGoal() ? "de " + money(m.goal()) + " de meta"
                : "Sem meta definida para este período", muted, SwingConstants.LEFT), BorderLayout.CENTER);
        if (m.hasGoal()) {
            figures.add(figure(String.format("%.1f%%", m.percent()), status.color(lightMode), "da meta atingida",
                    muted, SwingConstants.RIGHT), BorderLayout.EAST);
        }
        hero.add(figures, BorderLayout.CENTER);

        JPanel bottom = ExecutiveTvComponents.transparent(new BorderLayout(0, 4));
        GoalProgressBar bar = new GoalProgressBar(14);
        bar.setLightMode(lightMode);
        bar.setValues(m.billed(), m.goal());
        bar.setPaceMarker(m.totalDays() > 1 ? m.expectedPercent() : -1);
        bottom.add(bar, BorderLayout.NORTH);
        bottom.add(buildHeroLegend(m, lightMode, onEditGoals), BorderLayout.SOUTH);
        hero.add(bottom, BorderLayout.SOUTH);
        return hero;
    }

    /** Número grande com legenda imediatamente abaixo. */
    private static JPanel figure(String value, Color valueColor, String caption, Color captionColor, int align) {
        JPanel column = ExecutiveTvComponents.transparent(new BorderLayout(0, 0));
        JLabel big = ExecutiveTvComponents.label(value, Font.BOLD, 36, valueColor);
        JLabel small = ExecutiveTvComponents.label(caption, Font.PLAIN, 14, captionColor);
        big.setHorizontalAlignment(align);
        small.setHorizontalAlignment(align);
        column.add(big, BorderLayout.CENTER);
        column.add(small, BorderLayout.SOUTH);
        return column;
    }

    private static JPanel buildHeroLegend(ExecutiveTvMetrics m, boolean lightMode, Runnable onEditGoals) {
        Color muted = ExecutiveTvTheme.getTextMutedColor(lightMode);
        JPanel legend = ExecutiveTvComponents.transparent(new BorderLayout());
        if (!m.hasGoal()) {
            legend.add(ExecutiveTvComponents.label("Defina uma meta para acompanhar o progresso.", Font.PLAIN, 13, muted),
                    BorderLayout.WEST);
            if (onEditGoals != null) {
                JPanel action = ExecutiveTvComponents.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                action.add(ExecutiveTvComponents.ghostButton("Definir metas", "fas-bullseye", UIHelper.PENDING_YELLOW,
                        onEditGoals, lightMode));
                legend.add(action, BorderLayout.EAST);
            }
            return legend;
        }
        JLabel pace = ExecutiveTvComponents.label(m.totalDays() > 1
                ? String.format("Ritmo esperado para hoje: %.0f%%", m.expectedPercent()) : "Período de um dia",
                Font.PLAIN, 13, muted);
        if (m.totalDays() > 1) pace.setIcon(UIHelper.icon("fas-caret-down", 13, ExecutiveTvTheme.getTextLightColor(lightMode)));
        legend.add(pace, BorderLayout.WEST);
        BigDecimal diff = m.difference();
        String gap = diff.signum() >= 0 ? "Acima da meta em " + money(diff) : "Faltam " + money(diff.abs());
        legend.add(ExecutiveTvComponents.label(gap, Font.BOLD, 13,
                diff.signum() >= 0 ? UIHelper.APPROVED_GREEN : ExecutiveTvTheme.getTextLightColor(lightMode)), BorderLayout.EAST);
        legend.setBorder(new EmptyBorder(2, 0, 0, 0));
        return legend;
    }

    private static JPanel buildKpiGrid(ExecutiveTvMetrics m, boolean lightMode) {
        JPanel row = ExecutiveTvComponents.transparent(new GridLayout(2, 2, ExecutiveTvComponents.GAP, ExecutiveTvComponents.GAP));
        row.add(ExecutiveTvComponents.kpiTile("fas-file-invoice", "Faturas", String.valueOf(m.invoiceCount()),
                "Ticket médio " + money(m.averageTicket()), UIHelper.ACCENT_BLUE, lightMode));
        row.add(ExecutiveTvComponents.kpiTile("fas-tachometer-alt", "Ritmo diário", money(m.dailyRate()),
                m.elapsedDays() == 1 ? "Hoje" : "Média de " + m.elapsedDays() + " dias", UIHelper.ACCENT_CYAN, lightMode));
        row.add(ExecutiveTvComponents.kpiTile("fas-chart-line", "Previsão de fecho", money(m.projection()),
                m.hasGoal() ? String.format("%.0f%% da meta ao ritmo actual", m.projectionPercent()) : "Ao ritmo actual",
                UIHelper.ACCENT, lightMode));
        row.add(buildGoalKpi(m, lightMode));
        return row;
    }

    private static JPanel buildGoalKpi(ExecutiveTvMetrics m, boolean lightMode) {
        if (!m.hasGoal()) {
            return ExecutiveTvComponents.kpiTile("fas-bullseye", "Meta", "Não definida", "Configure em Metas",
                    ExecutiveTvTheme.getTextMutedColor(lightMode), lightMode);
        }
        if (m.difference().signum() >= 0) {
            return ExecutiveTvComponents.kpiTile("fas-trophy", "Acima da meta", money(m.difference()),
                    "Meta superada", UIHelper.APPROVED_GREEN, lightMode);
        }
        int remaining = Math.max(1, m.totalDays() - m.elapsedDays());
        return ExecutiveTvComponents.kpiTile("fas-flag-checkered", "Necessário por dia", money(m.requiredDailyRate()),
                remaining == 1 ? "No último dia" : "Nos " + remaining + " dias restantes", UIHelper.PENDING_YELLOW, lightMode);
    }
}
