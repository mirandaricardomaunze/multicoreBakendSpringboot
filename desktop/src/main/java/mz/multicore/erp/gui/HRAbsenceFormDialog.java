package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;

import javax.swing.*;
import java.time.LocalDate;

/**
 * Diálogo modal para registo de faltas e remoção de ausências.
 */
final class HRAbsenceFormDialog {

    private HRAbsenceFormDialog() {}

    static void openCreateDialog(HRPanel owner, String[] absenceTypes) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe colaboradores primeiro.");
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JComboBox<String> typeCombo = new JComboBox<>(absenceTypes);
        UIHelper.styleComboBox(typeCombo);

        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now());
        JTextField reasonField = new JTextField();
        JCheckBox docCheck = new JCheckBox("Possui documento de justificação");
        docCheck.setForeground(UIHelper.TEXT_LIGHT);
        docCheck.setOpaque(false);
        UIHelper.styleTextField(reasonField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Tipo de Falta:", typeCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Motivo:", reasonField,
                "Documento:", docCheck
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Registar Falta",
                "fas-user-times", "Ausência do colaborador", form)
                .setConfirmButton("Registar", "fas-check")
                .showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = owner.employeesList.get(empCombo.getSelectedIndex());
            CreateAbsenceRequest req = new CreateAbsenceRequest(
                    emp.id(),
                    (String) typeCombo.getSelectedItem(),
                    startField.value(),
                    endField.value(),
                    reasonField.getText().trim().isEmpty() ? null : reasonField.getText().trim(),
                    docCheck.isSelected()
            );
            UIHelper.runWithProgress(owner, "A registar falta…", () -> owner.hrApiClient.recordAbsence(req), ignored -> {
                owner.showSuccess("Falta registada.");
                owner.loadAbsences();
            }, owner::showActionError);
        } catch (Exception ex) {
            owner.showNotice(FeedbackType.ERROR, "Não foi possível registar a falta", ex.getMessage());
        }
    }

    static void deleteSelectedAbsence(HRPanel owner, AbsenceDTO sel) {
        if (sel == null) return;
        boolean confirmed = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(owner),
                FeedbackType.WARNING,
                "Eliminar Falta",
                "Tem certeza que deseja eliminar a falta selecionada?",
                "Eliminar");
        if (!confirmed) return;
        UIHelper.runWithProgress(owner, "A eliminar falta…", () -> {
            owner.hrApiClient.deleteAbsence(sel.id());
            return null;
        }, ignored -> owner.loadAbsences(), owner::showActionError);
    }
}
