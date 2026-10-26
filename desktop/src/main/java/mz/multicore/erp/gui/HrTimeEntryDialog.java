package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalTime;

/** Diálogo modal para registo manual de marcação de ponto de colaborador. */
final class HrTimeEntryDialog {
    private HrTimeEntryDialog() {}

    static void show(HRPanel owner, Runnable onSuccess) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe colaboradores primeiro.");
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());

        DateField dateField = new DateField(LocalDate.now());
        JTextField inField = new JTextField("08:00");
        JTextField outField = new JTextField("17:00");
        JSpinner breakSpinner = new JSpinner(new SpinnerNumberModel(60, 0, 480, 15));
        JTextField notesField = new JTextField();
        for (JTextField f : new JTextField[]{inField, outField, notesField}) UIHelper.styleTextField(f);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data (yyyy-MM-dd):", dateField,
                "Entrada (HH:mm):", inField,
                "Saída (HH:mm):", outField,
                "Pausa (minutos):", breakSpinner,
                "Observação:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Registar Marcação",
                "fas-clock", "Marcação de ponto", form).showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO employee = owner.employeesList.get(empCombo.getSelectedIndex());
            CreateTimeEntryRequest request = new CreateTimeEntryRequest(
                    employee.id(), dateField.value(),
                    LocalTime.parse(inField.getText().trim()),
                    LocalTime.parse(outField.getText().trim()),
                    (Integer) breakSpinner.getValue(), "MANUAL",
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
            UIHelper.runWithProgress(owner, "A registar marcação…",
                    () -> owner.hrApiClient.recordTimeEntry(request),
                    ignored -> { if (onSuccess != null) onSuccess.run(); }, owner::showActionError);
        } catch (RuntimeException ex) {
            owner.showNotice(FeedbackType.ERROR, "Hora inválida", "Use o formato HH:mm.");
        }
    }
}
