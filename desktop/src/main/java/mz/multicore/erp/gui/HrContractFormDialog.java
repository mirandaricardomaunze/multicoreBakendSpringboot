package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Diálogos modais ergonómicos para criação, renovação e cessação de contratos de trabalho.
 */
public final class HrContractFormDialog {

    private static final String[] TYPES = {"SEM_TERMO", "TERMO_CERTO", "TERMO_INCERTO", "TEMPORARIO", "ESTAGIO"};

    private HrContractFormDialog() {}

    public static void showCreateDialog(HRPanel owner, Runnable onSuccess) {
        List<EmployeeDTO> employees = owner.hrApiClient.getAllEmployees();
        if (employees.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe primeiro um colaborador.");
            return;
        }
        JComboBox<EmployeeDTO> employeeCombo = new JComboBox<>(employees.toArray(new EmployeeDTO[0]));
        employeeCombo.setRenderer(employeeRenderer());
        UIHelper.styleComboBox(employeeCombo);
        JComboBox<String> typeCombo = new JComboBox<>(TYPES);
        UIHelper.styleComboBox(typeCombo);

        JTextField jobField = new JTextField();
        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField();
        DateField probationField = new DateField();
        MoneyField salaryField = new MoneyField("0");
        JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 80, 1));
        JTextField locationField = new JTextField();
        JTextField reasonField = new JTextField();
        for (JTextField f : new JTextField[]{jobField, locationField, reasonField}) UIHelper.styleTextField(f);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", employeeCombo,
                "Tipo de contrato:", typeCombo,
                "Função:", jobField,
                "Início (yyyy-MM-dd):", startField,
                "Fim (opcional, yyyy-MM-dd):", endField,
                "Fim da experiência (opcional):", probationField,
                "Salário acordado (MT):", salaryField,
                "Horas semanais:", hoursSpinner,
                "Local de trabalho:", locationField,
                "Motivo do termo (obrigatório a termo):", reasonField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Contrato",
                "fas-file-signature", "Condições acordadas", form).showDialog();
        if (!confirmed) return;

        try {
            CreateContractRequest request = new CreateContractRequest(
                    ((EmployeeDTO) employeeCombo.getSelectedItem()).id(),
                    String.valueOf(typeCombo.getSelectedItem()),
                    startField.value(), parseDate(endField), parseDate(probationField),
                    salaryField.value(), (Integer) hoursSpinner.getValue(),
                    jobField.getText().trim(), locationField.getText().trim(), reasonField.getText().trim());
            UIHelper.runWithProgress(owner, "A criar contrato…",
                    () -> owner.hrApiClient.createContract(request),
                    ignored -> {
                        if (onSuccess != null) onSuccess.run();
                        owner.showSuccess("Contrato criado em rascunho; use Activar para o pôr a vigorar.");
                    }, owner::showActionError);
        } catch (RuntimeException ex) {
            owner.showActionError(ex);
        }
    }

    public static void showRenewDialog(HRPanel owner, EmploymentContractDTO contract, Runnable onSuccess) {
        DateField startField = new DateField(contract.endDate() == null ? LocalDate.now() : contract.endDate().plusDays(1));
        DateField endField = new DateField();
        MoneyField salaryField = new MoneyField(contract.agreedSalary().toPlainString());

        JPanel form = UIHelper.createDialogForm(
                "Novo início (yyyy-MM-dd):", startField,
                "Novo fim (opcional):", endField,
                "Salário acordado (MT):", salaryField);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Renovar " + contract.contractNumber(), "fas-redo",
                "O contrato anterior fecha na véspera do novo", form).showDialog();
        if (!confirmed) return;

        RenewContractRequest request = new RenewContractRequest(
                startField.value(), parseDate(endField), salaryField.value(), null);
        UIHelper.runWithProgress(owner, "A renovar contrato…",
                () -> owner.hrApiClient.renewContract(contract.id(), request),
                ignored -> { if (onSuccess != null) onSuccess.run(); }, owner::showActionError);
    }

    public static void showTerminateDialog(HRPanel owner, EmploymentContractDTO contract, Runnable onSuccess) {
        DateField dateField = new DateField(LocalDate.now());
        JTextField reasonField = new JTextField();
        UIHelper.styleTextField(reasonField);
        JPanel form = UIHelper.createDialogForm("Data da cessação:", dateField, "Motivo (obrigatório):", reasonField);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Cessar " + contract.contractNumber(), "fas-ban", "A cessação fica auditada", form).showDialog();
        if (!confirmed) return;

        UIHelper.runWithProgress(owner, "A cessar contrato…",
                () -> owner.hrApiClient.terminateContract(contract.id(), dateField.value(), reasonField.getText().trim()),
                ignored -> { if (onSuccess != null) onSuccess.run(); }, owner::showActionError);
    }

    private static LocalDate parseDate(JTextField field) {
        String text = field.getText().trim();
        return text.isBlank() ? null : LocalDate.parse(text);
    }

    private static ListCellRenderer<Object> employeeRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof EmployeeDTO employee) {
                    setText(employee.name() + " (" + employee.employeeNumber() + ")");
                }
                return this;
            }
        };
    }
}
