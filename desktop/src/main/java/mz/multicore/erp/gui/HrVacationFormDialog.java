package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;

import javax.swing.*;
import java.time.LocalDate;

/**
 * Diálogo modal ergonómico para registo e submissão de pedidos de férias.
 */
public final class HrVacationFormDialog {

    private HrVacationFormDialog() {}

    public static void show(HRPanel owner, Runnable onSubmitted) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe colaboradores primeiro.");
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " - " + e.department());

        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now().plusDays(15));
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Ano de Referência:", yearSpinner,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Pedido de Férias",
                "fas-umbrella-beach", "Marcação de férias do colaborador", form).showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = owner.employeesList.get(empCombo.getSelectedIndex());
            CreateVacationRequest req = new CreateVacationRequest(
                    emp.id(),
                    startField.value(),
                    endField.value(),
                    (Integer) yearSpinner.getValue(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            UIHelper.runWithProgress(owner, "A submeter pedido de férias…",
                    () -> owner.hrApiClient.submitVacation(req),
                    ignored -> {
                        owner.showSuccess("Pedido de férias submetido.");
                        onSubmitted.run();
                    }, owner::showActionError);
        } catch (Exception ex) {
            owner.showNotice(FeedbackType.ERROR, "Não foi possível submeter o pedido de férias", ex.getMessage());
        }
    }
}
