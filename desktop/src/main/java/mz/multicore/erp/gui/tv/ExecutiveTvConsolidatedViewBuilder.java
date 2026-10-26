package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Construtor da visão consolidada de todas as unidades de negócio do painel de TV Executiva.
 */
public final class ExecutiveTvConsolidatedViewBuilder {

    private ExecutiveTvConsolidatedViewBuilder() {}

    public static JPanel build(List<BusinessUnitDTO> businessUnits, List<InvoiceDTO> filteredInvoices,
                               ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        BigDecimal globalGoal = BusinessUnitGoalManager.getGlobalGoal();
        BigDecimal globalBilled = ExecutiveTvCalculationHelper.calculateTotalBilled(filteredInvoices, null);
        BigDecimal difference = globalBilled.subtract(globalGoal);
        double globalPct = globalGoal.compareTo(BigDecimal.ZERO) > 0
                ? globalBilled.multiply(BigDecimal.valueOf(100)).divide(globalGoal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        panel.add(buildMasterCard(globalGoal, globalBilled, difference, globalPct, currentPeriod, lightMode), BorderLayout.NORTH);

        JPanel centerSplit = new JPanel(new BorderLayout(14, 0));
        centerSplit.setOpaque(false);
        centerSplit.add(buildUnitsGrid(businessUnits, filteredInvoices, lightMode), BorderLayout.CENTER);
        centerSplit.add(buildRightWidgets(businessUnits, filteredInvoices, lightMode), BorderLayout.EAST);
        panel.add(centerSplit, BorderLayout.CENTER);

        return panel;
    }

    private static JPanel buildMasterCard(BigDecimal globalGoal, BigDecimal globalBilled, BigDecimal difference,
                                          double globalPct, ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        JPanel masterCard = new ModernPanel(14, ExecutiveTvTheme.getCardBgColor(lightMode), ExecutiveTvTheme.getCardBgColor(lightMode));
        masterCard.setLayout(new BorderLayout(0, 10));
        masterCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(14, 18, 14, 18)));

        JPanel masterTitleRow = new JPanel(new BorderLayout());
        masterTitleRow.setOpaque(false);

        JLabel masterTitle = new JLabel("META GLOBAL DA EMPRESA • " + currentPeriod.getLabel().toUpperCase());
        masterTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        masterTitle.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        masterTitleRow.add(masterTitle, BorderLayout.WEST);
        masterTitleRow.add(ExecutiveTvTheme.buildStatusBadge(globalPct), BorderLayout.EAST);
        masterCard.add(masterTitleRow, BorderLayout.NORTH);

        GoalProgressBar masterBar = new GoalProgressBar(32);
        masterBar.setLightMode(lightMode);
        masterBar.setValues(globalBilled, globalGoal);
        masterCard.add(masterBar, BorderLayout.CENTER);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.setBorder(new EmptyBorder(8, 0, 0, 0));

        kpiRow.add(ExecutiveTvTheme.buildTvKpi("META ESTIPULADA", String.format("%,.2f MT", globalGoal), "Valor orçamentado", UIHelper.ACCENT_CYAN, lightMode));
        kpiRow.add(ExecutiveTvTheme.buildTvKpi("TOTAL FATURADO", String.format("%,.2f MT", globalBilled), String.format("%.1f%% da meta atingida", globalPct),
                globalPct >= 100.0 ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE, lightMode));

        String diffLabel = difference.compareTo(BigDecimal.ZERO) >= 0 ? "SUPERÁVIT" : "FALTAM PARA A META";
        Color diffColor = difference.compareTo(BigDecimal.ZERO) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW;
        kpiRow.add(ExecutiveTvTheme.buildTvKpi(diffLabel, String.format("%,.2f MT", difference.abs()),
                difference.compareTo(BigDecimal.ZERO) >= 0 ? "Meta superada com êxito" : "Necessário acelerar vendas", diffColor, lightMode));

        int daysInMonth = YearMonth.now().lengthOfMonth();
        int dayOfMonth = Math.max(1, LocalDate.now().getDayOfMonth());
        BigDecimal currentDailyRate = globalBilled.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
        int daysRemaining = Math.max(1, daysInMonth - dayOfMonth);
        BigDecimal requiredDailyRate = difference.compareTo(BigDecimal.ZERO) < 0
                ? difference.abs().divide(BigDecimal.valueOf(daysRemaining), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        String runRateText = difference.compareTo(BigDecimal.ZERO) >= 0
                ? String.format("Média: %,.2f MT/dia", currentDailyRate) : String.format("Meta dia: %,.2f MT", requiredDailyRate);

        kpiRow.add(ExecutiveTvTheme.buildTvKpi("RITMO DIÁRIO", String.format("%,.2f MT/dia", currentDailyRate), runRateText, UIHelper.KPI_PURPLE_SOFT, lightMode));
        masterCard.add(kpiRow, BorderLayout.SOUTH);

        return masterCard;
    }

    private static JPanel buildUnitsGrid(List<BusinessUnitDTO> businessUnits, List<InvoiceDTO> filteredInvoices, boolean lightMode) {
        JPanel unitsGridContainer = new JPanel(new BorderLayout());
        unitsGridContainer.setOpaque(false);

        JLabel unitsTitle = new JLabel("PROGRESSO POR LINHA DE DISTRIBUIÇÃO / UNIDADE DE NEGÓCIO");
        unitsTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        unitsTitle.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        unitsTitle.setBorder(new EmptyBorder(0, 4, 8, 0));
        unitsGridContainer.add(unitsTitle, BorderLayout.NORTH);

        int cols = businessUnits.size() > 4 ? 3 : 2;
        JPanel unitsCards = new JPanel(new GridLayout(0, cols, 10, 10));
        unitsCards.setOpaque(false);
        for (BusinessUnitDTO bu : businessUnits) unitsCards.add(buildBusinessUnitCard(bu, filteredInvoices, lightMode));

        JScrollPane scrollUnits = new JScrollPane(unitsCards);
        scrollUnits.setOpaque(false);
        scrollUnits.getViewport().setOpaque(false);
        scrollUnits.setBorder(BorderFactory.createEmptyBorder());
        unitsGridContainer.add(scrollUnits, BorderLayout.CENTER);
        return unitsGridContainer;
    }

    private static JPanel buildBusinessUnitCard(BusinessUnitDTO bu, List<InvoiceDTO> filteredInvoices, boolean lightMode) {
        ModernPanel card = new ModernPanel(12, ExecutiveTvTheme.getCardBgColor(lightMode), ExecutiveTvTheme.getCardBgColor(lightMode));
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(12, 14, 12, 14)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel nameLbl = new JLabel(bu.name());
        nameLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        nameLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        top.add(nameLbl, BorderLayout.WEST);

        boolean isIncluded = BusinessUnitGoalManager.isUnitIncluded(bu.id());
        BigDecimal billed = ExecutiveTvCalculationHelper.calculateTotalBilled(filteredInvoices, bu.id());
        BigDecimal goal = BusinessUnitGoalManager.getGoalForUnit(bu.id());
        double pct = goal.compareTo(BigDecimal.ZERO) > 0 ? billed.multiply(BigDecimal.valueOf(100)).divide(goal, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;

        JLabel badge;
        if (!isIncluded) {
            badge = new JLabel("  FORA DA META  ");
            badge.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
            badge.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
            badge.setOpaque(true);
            badge.setBackground(lightMode ? new Color(241, 245, 249) : new Color(51, 65, 85, 40));
            badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(2, 6, 2, 6)));
        } else {
            badge = ExecutiveTvTheme.buildStatusBadge(pct);
        }
        top.add(badge, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        GoalProgressBar bar = new GoalProgressBar(22);
        bar.setLightMode(lightMode);
        bar.setValues(billed, isIncluded ? goal : BigDecimal.ZERO);
        card.add(bar, BorderLayout.CENTER);

        JPanel btm = new JPanel(new BorderLayout());
        btm.setOpaque(false);
        String supplier = bu.supplierName() != null && !bu.supplierName().isBlank() ? bu.supplierName() : "Multi-fornecedor";
        JLabel supLbl = new JLabel(supplier);
        supLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        supLbl.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
        btm.add(supLbl, BorderLayout.WEST);

        JLabel targetLbl = new JLabel(isIncluded ? String.format("Meta: %,.2f MT", goal) : "Não orçamentado");
        targetLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        targetLbl.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
        btm.add(targetLbl, BorderLayout.EAST);

        card.add(btm, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel buildRightWidgets(List<BusinessUnitDTO> businessUnits, List<InvoiceDTO> filteredInvoices, boolean lightMode) {
        JPanel rightWidgets = new JPanel();
        rightWidgets.setLayout(new BoxLayout(rightWidgets, BoxLayout.Y_AXIS));
        rightWidgets.setOpaque(false);
        rightWidgets.setPreferredSize(new Dimension(380, 0));

        SimplePieChart shareChart = buildShareChart(businessUnits, filteredInvoices);
        ModernPanel shareCard = new ModernPanel(14, ExecutiveTvTheme.getCardBgColor(lightMode), ExecutiveTvTheme.getCardBgColor(lightMode));
        shareCard.setLayout(new BorderLayout());
        shareCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(8, 8, 8, 8)));
        shareCard.add(shareChart, BorderLayout.CENTER);
        rightWidgets.add(shareCard);
        rightWidgets.add(Box.createVerticalStrut(10));
        rightWidgets.add(ExecutiveTvUnitViewBuilder.buildTopProductsTvWidget(filteredInvoices, null, lightMode));
        return rightWidgets;
    }

    private static SimplePieChart buildShareChart(List<BusinessUnitDTO> businessUnits, List<InvoiceDTO> filteredInvoices) {
        SimplePieChart chart = new SimplePieChart("Participação nas Vendas (%)", true);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (BusinessUnitDTO bu : businessUnits) {
            BigDecimal b = ExecutiveTvCalculationHelper.calculateTotalBilled(filteredInvoices, bu.id());
            if (b.compareTo(BigDecimal.ZERO) > 0) {
                labels.add(bu.name());
                values.add(b);
            }
        }
        if (values.isEmpty()) {
            labels.add("Sem Vendas");
            values.add(BigDecimal.ONE);
        }

        Color[] palette = new Color[]{
                new Color(59, 130, 246), new Color(16, 185, 129), new Color(245, 158, 11),
                new Color(139, 92, 246), new Color(236, 72, 153), new Color(20, 184, 166), new Color(249, 115, 22)
        };
        Color[] sliceColors = new Color[labels.size()];
        for (int i = 0; i < sliceColors.length; i++) sliceColors[i] = palette[i % palette.length];
        chart.setData(labels.toArray(new String[0]), values.toArray(new BigDecimal[0]), sliceColors);
        return chart;
    }
}
