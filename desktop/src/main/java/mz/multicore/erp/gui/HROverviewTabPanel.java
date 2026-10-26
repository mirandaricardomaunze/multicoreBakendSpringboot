package mz.multicore.erp.gui;

import mz.multicore.erp.gui.HROverviewMetricsCoordinator.OverviewLabels;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Aba "Visão Geral" do módulo de Recursos Humanos:
 * Métricas-chave (KPIs), distribuição de massa salarial e colaboradores por departamento.
 */
final class HROverviewTabPanel {

    private final HRPanel owner;
    private OverviewLabels labels;

    HROverviewTabPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setOpaque(false);

        JLabel ovActiveEmployees = KpiCard.valueLabel("0", 20);
        JLabel ovActiveEmployeesSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("COLABORADORES ATIVOS", "fas-users", UIHelper.KPI_INFO_SOFT,
                ovActiveEmployees, ovActiveEmployeesSub, UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END));

        JLabel ovPayrollNet = KpiCard.valueLabel("0,00 MT", 20);
        JLabel ovPayrollGrossSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("MASSA SALARIAL (MÊS)", "fas-money-check-alt", UIHelper.KPI_PURPLE_SOFT,
                ovPayrollNet, ovPayrollGrossSub, UIHelper.KPI_PURPLE_DARK, UIHelper.KPI_PURPLE_END));

        JLabel ovEmployerInss = KpiCard.valueLabel("0,00 MT", 20);
        JLabel ovEmployerInssSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("INSS PATRONAL (MÊS)", "fas-hand-holding-usd", UIHelper.KPI_SUCCESS_SOFT,
                ovEmployerInss, ovEmployerInssSub, UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN));

        JLabel ovPendingVacations = KpiCard.valueLabel("0", 20);
        JLabel ovPendingVacationsSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("FÉRIAS PENDENTES", "fas-umbrella-beach", UIHelper.KPI_WARNING_SOFT,
                ovPendingVacations, ovPendingVacationsSub, UIHelper.KPI_WARNING_DARK, UIHelper.KPI_WARNING_END));

        JLabel ovMonthAbsences = KpiCard.valueLabel("0", 20);
        JLabel ovMonthAbsencesSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("FALTAS (MÊS)", "fas-user-times", UIHelper.KPI_DANGER_SOFT,
                ovMonthAbsences, ovMonthAbsencesSub, UIHelper.KPI_DANGER_DARK, UIHelper.KPI_DANGER_END));

        JLabel ovPendingExpenses = KpiCard.valueLabel("0", 20);
        JLabel ovPendingExpensesSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("DESPESAS PENDENTES", "fas-receipt", UIHelper.KPI_NEUTRAL_SOFT,
                ovPendingExpenses, ovPendingExpensesSub, UIHelper.KPI_NEUTRAL_DARK, UIHelper.KPI_NEUTRAL_END));

        content.add(grid, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        SimpleBarChart ovPayrollChart = new SimpleBarChart("Massa salarial líquida (6 meses)");
        SimpleBarChart ovDeptChart = new SimpleBarChart("Colaboradores por departamento");
        charts.add(HROverviewUi.chartCard(ovPayrollChart));
        charts.add(HROverviewUi.chartCard(ovDeptChart));
        content.add(charts, BorderLayout.CENTER);

        labels = new OverviewLabels(
                ovActiveEmployees, ovActiveEmployeesSub,
                ovPayrollNet, ovPayrollGrossSub,
                ovEmployerInss, ovEmployerInssSub,
                ovPendingVacations, ovPendingVacationsSub,
                ovMonthAbsences, ovMonthAbsencesSub,
                ovPendingExpenses, ovPendingExpensesSub,
                ovPayrollChart, ovDeptChart
        );

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
    }

    void refresh() {
        HROverviewMetricsCoordinator.updateMetrics(owner, labels);
    }
}
