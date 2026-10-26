package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogos modais corporativos de histórico e registo de alteração salarial do colaborador (§B4).
 */
final class HREmployeeSalaryDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] SALARY_REASONS = {
            "AUMENTO", "PROMOCAO", "REVISAO_ANUAL", "ACORDO", "CORRECCAO"};
    private static final String[] SALARY_REASON_LABELS = {
            "Aumento", "Promoção", "Revisão anual", "Acordo", "Correcção"};

    private HREmployeeSalaryDialog() {}

    static void openSalaryHistory(HRPanel owner, EmployeeDTO employee) {
        if (employee == null) return;
        UIHelper.runWithProgress(owner, "A carregar evolução salarial…",
                () -> owner.hrApiClient.getSalaryHistory(employee.id()),
                history -> showSalaryHistory(owner, employee, history), owner::showActionError);
    }

    private static void showSalaryHistory(HRPanel owner, EmployeeDTO employee, List<SalaryChangeDTO> history) {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(720, 460));

        SimpleBarChart chart = new SimpleBarChart("Evolução do salário base");
        int size = history.size();
        String[] labels = new String[size];
        java.math.BigDecimal[] values = new java.math.BigDecimal[size];
        Color[] colors = new Color[size];
        for (int i = 0; i < size; i++) {
            SalaryChangeDTO change = history.get(size - 1 - i);
            labels[i] = change.effectiveDate().format(DATE_FMT);
            values[i] = change.newSalary() == null ? java.math.BigDecimal.ZERO : change.newSalary();
            colors[i] = UIHelper.KPI_PURPLE_DARK;
        }
        chart.setData(labels, values, colors);
        content.add(chart, BorderLayout.CENTER);

        String[] cols = {"Data de efeito", "Anterior (MT)", "Novo (MT)", "Diferença (MT)",
                "Motivo", "Função", "Aprovado por"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SalaryChangeDTO change : history) {
            model.addRow(new Object[]{
                    change.effectiveDate().format(DATE_FMT),
                    change.previousSalary(), change.newSalary(), change.difference(),
                    change.reasonLabel(),
                    change.jobTitle() == null ? "-" : change.jobTitle(),
                    change.approvedBy()});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(880, 180));
        content.add(scroll, BorderLayout.SOUTH);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                "Evolução Salarial — " + employee.name(), "fas-chart-line",
                "Série cronológica de alterações e revisões de salário base.", content);
        dialog.setConfirmButton("Registar Alteração", "fas-plus");
        dialog.setSize(880, 560);
        if (dialog.showDialog()) {
            openSalaryChangeDialog(owner, employee);
        }
    }

    static void openSalaryChangeDialog(HRPanel owner, EmployeeDTO employee) {
        MoneyField salaryField = new MoneyField(
                employee.baseSalary() == null ? "0.00" : employee.baseSalary().toPlainString());
        DateField effectiveField = new DateField(LocalDate.now());
        JComboBox<String> reasonCombo = new JComboBox<>(SALARY_REASON_LABELS);
        UIHelper.styleComboBox(reasonCombo);
        JTextField jobField = new JTextField();
        UIHelper.styleTextField(jobField);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Novo salário:", salaryField,
                "Data de efeito:", effectiveField,
                "Motivo:", reasonCombo,
                "Nova função (vazio = mantém):", jobField,
                "Observações:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Alteração Salarial — " + employee.name(), "fas-chart-line",
                "Uma data de efeito futura é um compromisso: não mexe na ficha até lá chegar, mas "
                        + "já manda no recibo desse mês.", form).showDialog();
        if (!confirmed) return;

        CreateSalaryChangeRequest request = new CreateSalaryChangeRequest(
                employee.id(), salaryField.value(), effectiveField.value(),
                SALARY_REASONS[reasonCombo.getSelectedIndex()],
                jobField.getText().trim().isEmpty() ? null : jobField.getText().trim(), null,
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
        UIHelper.runWithProgress(owner, "A registar alteração salarial…",
                () -> owner.hrApiClient.registerSalaryChange(request),
                ignored -> {
                    owner.showSuccess("Alteração salarial registada.");
                    owner.refreshData();
                }, owner::showActionError);
    }
}
