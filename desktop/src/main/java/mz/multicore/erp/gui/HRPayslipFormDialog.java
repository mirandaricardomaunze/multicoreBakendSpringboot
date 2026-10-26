package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Diálogo modal para emissão individual e processamento mensal de recibos de salário.
 */
final class HRPayslipFormDialog {

    private HRPayslipFormDialog() {}

    static void openCreateDialog(HRPanel owner) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe colaboradores primeiro.");
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());

        LocalDate today = LocalDate.now();
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));

        MoneyField allowancesField = new MoneyField("0");
        MoneyField overtimeField = new MoneyField("0");
        MoneyField otherField = new MoneyField("0");
        JTextField notesField = new JTextField();
        JTextField overtimeReasonField = new JTextField();
        UIHelper.styleTextField(notesField);
        UIHelper.styleTextField(overtimeReasonField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Ano:", yearSpinner,
                "Mês:", monthSpinner,
                "Subsídios / Abonos (MT):", allowancesField,
                "Horas Extras (MT):", overtimeField,
                "Justificação (só se divergir do ponto):", overtimeReasonField,
                "IRPS / INSS:", new JLabel("Cálculo automático pela configuração fiscal vigente"),
                "Outros Descontos (MT):", otherField,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Gerar Recibo de Salário",
                "fas-file-invoice-dollar", "Processamento de salário do colaborador", form)
                .setConfirmButton("Gerar", "fas-check")
                .showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = owner.employeesList.get(empCombo.getSelectedIndex());
            CreatePayslipRequest req = new CreatePayslipRequest(
                    emp.id(),
                    (Integer) yearSpinner.getValue(),
                    (Integer) monthSpinner.getValue(),
                    allowancesField.value(),
                    overtimeField.value(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    otherField.value(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim(),
                    overtimeReasonField.getText().trim().isEmpty() ? null : overtimeReasonField.getText().trim()
            );
            UIHelper.runWithProgress(owner, "A gerar recibo de salário…", () -> owner.hrApiClient.createPayslip(req),
                    created -> {
                        owner.loadPayslips();
                        owner.showSuccess("Recibo " + created.payslipNumber() + " gerado com sucesso.");
                    }, owner::showActionError);
        } catch (IllegalArgumentException ex) {
            owner.showNotice(FeedbackType.ERROR, "Não foi possível gerar o recibo", ex.getMessage());
        } catch (Exception ex) {
            owner.showNotice(FeedbackType.ERROR, "Não foi possível gerar o recibo", ex.getMessage());
        }
    }

    static void processMonthlyPayroll(HRPanel owner) {
        LocalDate today = LocalDate.now();
        JSpinner year = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        JSpinner month = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        JPanel form = UIHelper.createDialogForm("Ano:", year, "Mês:", month);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Processar Folha Salarial",
                "fas-calculator", "Gera os recibos de todos os colaboradores ativos do período", form)
                .setConfirmButton("Processar", "fas-cogs")
                .showDialog();
        if (!confirmed) return;

        int selectedYear = (Integer) year.getValue();
        int selectedMonth = (Integer) month.getValue();
        UIHelper.runWithProgress(owner, "A processar folha salarial…",
                () -> owner.hrApiClient.processMonthlyPayroll(selectedYear, selectedMonth), created -> {
                    owner.loadPayslips();
                    owner.showSuccess(created.summaryMessage());
                }, owner::showActionError);
    }
}
