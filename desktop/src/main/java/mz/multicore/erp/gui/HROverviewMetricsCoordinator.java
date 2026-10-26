package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.model.ExpenseStatus;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Agregador analítico e povoamento dos KPIs e gráficos da visão geral de RH.
 */
public final class HROverviewMetricsCoordinator {

    private HROverviewMetricsCoordinator() {}

    public record OverviewLabels(
            JLabel activeEmployees, JLabel activeEmployeesSub,
            JLabel payrollNet, JLabel payrollGrossSub,
            JLabel employerInss, JLabel employerInssSub,
            JLabel pendingVacations, JLabel pendingVacationsSub,
            JLabel monthAbsences, JLabel monthAbsencesSub,
            JLabel pendingExpenses, JLabel pendingExpensesSub,
            SimpleBarChart payrollChart, SimpleBarChart deptChart
    ) {}

    public static void updateMetrics(HRPanel owner, OverviewLabels lbl) {
        if (lbl == null || lbl.activeEmployees() == null) return;
        LocalDate today = LocalDate.now();
        int year = today.getYear(), month = today.getMonthValue();

        long active = owner.employeesList.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.status())).count();
        lbl.activeEmployees().setText(String.valueOf(active));
        lbl.activeEmployeesSub().setText("de " + owner.employeesList.size() + " no quadro");

        List<PayslipDTO> monthSlips = owner.payslipsList.stream()
                .filter(p -> p.year() == year && p.month() == month && !"CANCELLED".equalsIgnoreCase(p.status())).toList();
        BigDecimal net = monthSlips.stream().map(PayslipDTO::netPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = monthSlips.stream().map(PayslipDTO::grossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inss = monthSlips.stream().map(PayslipDTO::employerInss).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retained = monthSlips.stream().map(p -> p.irpsDeduction().add(p.inssDeduction())).reduce(BigDecimal.ZERO, BigDecimal::add);
        lbl.payrollNet().setText(String.format("%,.2f MT", net));
        lbl.payrollGrossSub().setText(String.format("Bruto: %,.2f MT · %d recibo(s)", gross, monthSlips.size()));
        lbl.employerInss().setText(String.format("%,.2f MT", inss));
        lbl.employerInssSub().setText(String.format("IRPS+INSS retido: %,.2f MT", retained));

        List<VacationDTO> pendingVac = owner.vacationsList.stream().filter(v -> "PENDING".equalsIgnoreCase(v.status())).toList();
        lbl.pendingVacations().setText(String.valueOf(pendingVac.size()));
        lbl.pendingVacationsSub().setText(pendingVac.stream().mapToInt(VacationDTO::totalDays).sum() + " dia(s) por decidir");

        LocalDate first = today.withDayOfMonth(1), last = today.withDayOfMonth(today.lengthOfMonth());
        List<AbsenceDTO> monthAbs = owner.absencesList.stream()
                .filter(a -> a.startDate() != null && a.endDate() != null && !a.startDate().isAfter(last) && !a.endDate().isBefore(first)).toList();
        int unjustified = monthAbs.stream().filter(a -> "UNJUSTIFIED".equalsIgnoreCase(a.absenceType())).mapToInt(AbsenceDTO::totalDays).sum();
        lbl.monthAbsences().setText(String.valueOf(monthAbs.size()));
        lbl.monthAbsencesSub().setText(unjustified + " dia(s) não justificados");

        List<ExpenseClaimDTO> pendingExp = owner.expensesList.stream().filter(c -> c.status() == ExpenseStatus.PENDING_APPROVAL).toList();
        BigDecimal pendingSum = pendingExp.stream().map(ExpenseClaimDTO::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        lbl.pendingExpenses().setText(String.valueOf(pendingExp.size()));
        lbl.pendingExpensesSub().setText(String.format("%,.2f MT por aprovar", pendingSum));

        updatePayrollChart(owner, lbl.payrollChart());
        updateDeptChart(owner, lbl.deptChart());
    }

    private static void updatePayrollChart(HRPanel owner, SimpleBarChart chart) {
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (PayslipDTO p : owner.payslipsList) {
            if (!"CANCELLED".equalsIgnoreCase(p.status())) byMonth.merge(YearMonth.of(p.year(), p.month()), p.netPay(), BigDecimal::add);
        }
        List<YearMonth> months = new ArrayList<>(byMonth.keySet());
        List<YearMonth> last6 = months.subList(Math.max(0, months.size() - 6), months.size());
        String[] payLabels = new String[last6.size()];
        BigDecimal[] payValues = new BigDecimal[last6.size()];
        Color[] payColors = new Color[last6.size()];
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("MM/yy");
        for (int i = 0; i < last6.size(); i++) {
            payLabels[i] = last6.get(i).format(ymFmt);
            payValues[i] = byMonth.get(last6.get(i));
            payColors[i] = UIHelper.ACCENT;
        }
        chart.setData(payLabels, payValues, payColors);
    }

    private static void updateDeptChart(HRPanel owner, SimpleBarChart chart) {
        Map<String, Integer> byDept = new HashMap<>();
        for (EmployeeDTO e : owner.employeesList) {
            if ("ACTIVE".equalsIgnoreCase(e.status())) byDept.merge((e.department() == null || e.department().isBlank()) ? "—" : e.department(), 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> top = byDept.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(5).toList();
        String[] deptLabels = new String[top.size()];
        BigDecimal[] deptValues = new BigDecimal[top.size()];
        Color[] deptColors = new Color[top.size()];
        Color[] palette = {UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN, UIHelper.PENDING_YELLOW, UIHelper.ACCENT, UIHelper.REJECTED_RED};
        for (int i = 0; i < top.size(); i++) {
            String d = top.get(i).getKey();
            deptLabels[i] = d.length() > 9 ? d.substring(0, 9) : d;
            deptValues[i] = BigDecimal.valueOf(top.get(i).getValue());
            deptColors[i] = palette[i % palette.length];
        }
        chart.setData(deptLabels, deptValues, deptColors);
    }
}
