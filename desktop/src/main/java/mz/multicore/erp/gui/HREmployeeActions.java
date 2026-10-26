package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Orquestrador canónico de acções sobre colaboradores de Recursos Humanos.
 * Delega histórico salarial para {@link HREmployeeSalaryDialog}, documentos para
 * {@link HREmployeeDocumentsDialog} e saúde ocupacional para {@link HREmployeeOccupationalHealthDialog}.
 */
final class HREmployeeActions {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] ABSENCE_TYPES = {
            "JUSTIFIED", "SICK", "MATERNITY", "UNJUSTIFIED", "UNPAID_LEAVE"};
    private static final String[] ABSENCE_TYPE_LABELS = {
            "Justificada (remunerada)", "Baixa médica (remunerada)", "Maternidade (remunerada)",
            "Injustificada (desconta)", "Licença sem vencimento (desconta)"};

    private final HRPanel owner;
    private final Supplier<EmployeeDTO> employeeSelection;
    private final Supplier<AbsenceDTO> absenceSelection;
    private final Runnable afterAbsenceChange;

    HREmployeeActions(HRPanel owner,
                      Supplier<EmployeeDTO> employeeSelection,
                      Supplier<AbsenceDTO> absenceSelection,
                      Runnable afterAbsenceChange) {
        this.owner = owner;
        this.employeeSelection = employeeSelection;
        this.absenceSelection = absenceSelection;
        this.afterAbsenceChange = afterAbsenceChange;
    }

    void openSalaryHistory() { HREmployeeSalaryDialog.openSalaryHistory(owner, employeeSelection.get()); }
    void openSalaryChangeDialog(EmployeeDTO employee) { HREmployeeSalaryDialog.openSalaryChangeDialog(owner, employee); }
    void openDocuments() { HREmployeeDocumentsDialog.openDocuments(owner, employeeSelection.get()); }
    void openDocumentDialog(EmployeeDTO employee) { HREmployeeDocumentsDialog.openDocumentDialog(owner, employee); }
    void openOccupationalHealth() { HREmployeeOccupationalHealthDialog.openOccupationalHealth(owner, employeeSelection.get()); }
    void openOccupationalHealthAttachment(List<OccupationalHealthExamDTO> history, int row) { HREmployeeOccupationalHealthDialog.openOccupationalHealthAttachment(owner, history, row); }
    void payOccupationalHealthExam(EmployeeDTO employee, List<OccupationalHealthExamDTO> history, int row) { HREmployeeOccupationalHealthDialog.payOccupationalHealthExam(owner, employee, history, row); }
    void openOccupationalHealthCompliance() { HREmployeeOccupationalHealthDialog.openOccupationalHealthCompliance(owner); }
    void openOccupationalHealthForm(EmployeeDTO employee) { HREmployeeOccupationalHealthDialog.openOccupationalHealthForm(owner, employee); }

    // ─── §B2 (RHC-25): Justificar Falta ──────────────────────────────────────

    void justifyAbsence() {
        AbsenceDTO absence = absenceSelection.get();
        if (absence == null) return;
        JComboBox<String> typeCombo = new JComboBox<>(ABSENCE_TYPE_LABELS);
        UIHelper.styleComboBox(typeCombo);
        JTextField reasonField = new JTextField();
        UIHelper.styleTextField(reasonField);
        JCheckBox documentBox = new JCheckBox("Com documento comprovativo");
        documentBox.setOpaque(false);
        documentBox.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Passa a ser:", typeCombo,
                "Motivo:", reasonField,
                "Documento:", documentBox);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Justificar Falta",
                "fas-user-check",
                String.format("%s — %s (%d dia(s)), actualmente %s", absence.employeeName(),
                        absence.startDate().format(DATE_FMT), absence.totalDays(),
                        UIHelper.humanStatus(absence.absenceType())), form).showDialog();
        if (!confirmed) return;

        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Motivo obrigatório", "Justificar uma falta exige um motivo.");
            return;
        }
        String type = ABSENCE_TYPES[typeCombo.getSelectedIndex()];
        boolean hasDocument = documentBox.isSelected();
        UIHelper.runWithProgress(owner, "A justificar falta…",
                () -> owner.hrApiClient.justifyAbsence(absence.id(), type, reason, hasDocument),
                ignored -> afterAbsenceChange.run(), owner::showActionError);
    }
}
