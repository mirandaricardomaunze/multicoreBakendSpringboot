package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogos modais corporativos de histórico e registo de documentos do colaborador (§B8.8).
 */
final class HREmployeeDocumentsDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] DOCUMENT_TYPES = {
            "BI", "DIRE", "PASSAPORTE", "NUIT", "CERTIFICADO", "OUTRO"};

    private HREmployeeDocumentsDialog() {}

    static void openDocuments(HRPanel owner, EmployeeDTO employee) {
        if (employee == null) return;
        UIHelper.runWithProgress(owner, "A carregar documentos…",
                () -> owner.hrApiClient.getEmployeeDocuments(employee.id()),
                documents -> showDocuments(owner, employee, documents), owner::showActionError);
    }

    private static void showDocuments(HRPanel owner, EmployeeDTO employee, List<EmployeeDocumentDTO> documents) {
        String[] cols = {"Tipo", "Número", "Emissão", "Validade", "Situação"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (EmployeeDocumentDTO d : documents) {
            model.addRow(new Object[]{
                    d.documentType(),
                    d.documentNumber() == null ? "-" : d.documentNumber(),
                    d.issueDate() == null ? "-" : d.issueDate().format(DATE_FMT),
                    d.expiryDate() == null ? "Não caduca" : d.expiryDate().format(DATE_FMT),
                    situationOf(d)});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(640, 260));

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                "Documentos — " + employee.name(), "fas-id-card",
                "Documentos oficiais com monitorização de validade e alertas de caducidade.", scroll);
        dialog.setConfirmButton("Novo Documento", "fas-plus");
        dialog.setSize(720, 420);
        if (dialog.showDialog()) {
            openDocumentDialog(owner, employee);
        }
    }

    static String situationOf(EmployeeDocumentDTO d) {
        if (d.expiryDate() == null) return "Sem validade";
        if (d.expired()) return "Caducado há " + Math.abs(d.daysUntilExpiry()) + " dia(s)";
        return "Válido — faltam " + d.daysUntilExpiry() + " dia(s)";
    }

    static void openDocumentDialog(HRPanel owner, EmployeeDTO employee) {
        JComboBox<String> typeCombo = new JComboBox<>(DOCUMENT_TYPES);
        UIHelper.styleComboBox(typeCombo);
        JTextField numberField = new JTextField();
        UIHelper.styleTextField(numberField);
        DateField issueField = new DateField(null);
        DateField expiryField = new DateField(null);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Tipo:", typeCombo,
                "Número:", numberField,
                "Emissão:", issueField,
                "Validade (vazio = não caduca):", expiryField,
                "Observações:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Novo Documento — " + employee.name(), "fas-id-card",
                "Deixar a validade vazia significa que o documento não caduca — não é o mesmo que "
                        + "ainda não a saber.", form).showDialog();
        if (!confirmed) return;

        SaveEmployeeDocumentRequest request = new SaveEmployeeDocumentRequest(
                employee.id(), (String) typeCombo.getSelectedItem(),
                numberField.getText().trim().isEmpty() ? null : numberField.getText().trim(),
                issueField.value(), expiryField.value(),
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
        UIHelper.runWithProgress(owner, "A gravar documento…",
                () -> owner.hrApiClient.saveEmployeeDocument(request),
                ignored -> owner.showSuccess("Documento registado."),
                owner::showActionError);
    }
}
