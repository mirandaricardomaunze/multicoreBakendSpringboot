package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Visão consolidada do painel de TV: resumo da empresa, cartões por unidade de negócio,
 * participação nas vendas e ranking de artigos.
 */
public final class ExecutiveTvConsolidatedViewBuilder {

    private static final int SIDE_COLUMN_WIDTH = 400;

    private ExecutiveTvConsolidatedViewBuilder() {}

    public static JPanel build(List<BusinessUnitDTO> businessUnits, List<InvoiceDTO> filteredInvoices,
                               ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        return build(businessUnits, filteredInvoices, currentPeriod, lightMode, null);
    }

    public static JPanel build(List<BusinessUnitDTO> businessUnits, List<InvoiceDTO> filteredInvoices,
                               ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode, Runnable onEditGoals) {
        int gap = ExecutiveTvComponents.GAP;
        JPanel panel = ExecutiveTvComponents.transparent(new BorderLayout(0, gap));
        ExecutiveTvMetrics company = ExecutiveTvMetrics.of(filteredInvoices, null,
                BusinessUnitGoalManager.getGlobalGoal(), currentPeriod, LocalDate.now());
        panel.add(ExecutiveTvSummaryBuilder.build("Faturado da empresa · " + currentPeriod.getLabel(),
                company, lightMode, onEditGoals), BorderLayout.NORTH);

        JPanel lower = ExecutiveTvComponents.transparent(new BorderLayout(gap, 0));
        lower.add(buildUnitsSection(businessUnits, filteredInvoices, currentPeriod, lightMode), BorderLayout.CENTER);

        JPanel side = ExecutiveTvComponents.transparent(new GridLayout(2, 1, 0, gap));
        side.setPreferredSize(new Dimension(SIDE_COLUMN_WIDTH, 0));
        side.add(ExecutiveTvShareChart.card(businessUnits, filteredInvoices, lightMode));
        side.add(ExecutiveTvListCards.topProducts(filteredInvoices, null, lightMode));
        lower.add(side, BorderLayout.EAST);
        panel.add(lower, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buildUnitsSection(List<BusinessUnitDTO> units, List<InvoiceDTO> invoices,
                                            ExecutiveTvDashboardDialog.Period period, boolean lightMode) {
        JPanel section = ExecutiveTvComponents.card(lightMode, new BorderLayout(), 16);
        long inGoal = units.stream().filter(u -> BusinessUnitGoalManager.isUnitIncluded(u.id())).count();
        String caption = units.isEmpty() ? null
                : units.size() + (units.size() == 1 ? " unidade" : " unidades") + " · " + inGoal + " na meta";
        section.add(ExecutiveTvComponents.sectionHeader("Unidades de negócio", caption, lightMode), BorderLayout.NORTH);

        if (units.isEmpty()) {
            section.add(ExecutiveTvComponents.emptyState("fas-sitemap", "Sem unidades de negócio activas",
                    "Crie unidades em Compras para acompanhar metas por linha de distribuição.", lightMode),
                    BorderLayout.CENTER);
            return section;
        }
        JPanel grid = ExecutiveTvComponents.transparent(new GridLayout(0, units.size() > 4 ? 3 : 2,
                ExecutiveTvComponents.GAP - 4, ExecutiveTvComponents.GAP - 4));
        for (BusinessUnitDTO unit : units) grid.add(buildUnitCard(unit, invoices, period, lightMode));
        section.add(ExecutiveTvComponents.verticalScroll(grid), BorderLayout.CENTER);
        return section;
    }

    private static JPanel buildUnitCard(BusinessUnitDTO unit, List<InvoiceDTO> invoices,
                                        ExecutiveTvDashboardDialog.Period period, boolean lightMode) {
        boolean included = BusinessUnitGoalManager.isUnitIncluded(unit.id());
        BigDecimal goal = included ? BusinessUnitGoalManager.getGoalForUnit(unit.id()) : BigDecimal.ZERO;
        ExecutiveTvMetrics m = ExecutiveTvMetrics.of(invoices, unit.id(), goal, period, LocalDate.now());
        Color text = ExecutiveTvTheme.getTextLightColor(lightMode);
        Color muted = ExecutiveTvTheme.getTextMutedColor(lightMode);

        JPanel card = new ExecutiveTvComponents.RoundedSurface(new BorderLayout(0, 3),
                ExecutiveTvTheme.getSurfaceAltColor(lightMode), ExecutiveTvTheme.getCardBorderColor(lightMode), 12);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel top = ExecutiveTvComponents.transparent(new BorderLayout(8, 0));
        JLabel name = ExecutiveTvComponents.label(unit.name(), Font.BOLD, 14, text);
        name.setToolTipText(unit.supplierName() != null ? "Fornecedor: " + unit.supplierName() : unit.name());
        top.add(name, BorderLayout.CENTER);
        top.add(included ? ExecutiveTvTheme.buildStatusBadge(m.status(), lightMode)
                : ExecutiveTvComponents.pill("FORA DA META", muted, lightMode), BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel figures = ExecutiveTvComponents.transparent(new BorderLayout(8, 2));
        figures.add(ExecutiveTvComponents.label(ExecutiveTvSummaryBuilder.money(m.billed()), Font.BOLD, 17, text),
                BorderLayout.WEST);
        JLabel pct = ExecutiveTvComponents.label(m.hasGoal() ? String.format("%.0f%%", m.percent()) : "",
                Font.BOLD, 16, m.status().color(lightMode));
        pct.setHorizontalAlignment(SwingConstants.RIGHT);
        figures.add(pct, BorderLayout.EAST);
        GoalProgressBar bar = new GoalProgressBar(8);
        bar.setLightMode(lightMode);
        bar.setValues(m.billed(), m.goal());
        figures.add(bar, BorderLayout.SOUTH);
        card.add(figures, BorderLayout.CENTER);

        String goalText = !included ? "Não conta para a meta global"
                : m.hasGoal() ? "Meta " + ExecutiveTvSummaryBuilder.money(m.goal()) : "Sem meta definida";
        card.add(ExecutiveTvComponents.label(goalText, Font.PLAIN, 12, muted), BorderLayout.SOUTH);
        return card;
    }
}
