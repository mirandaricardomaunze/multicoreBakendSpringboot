package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.CircularAvatar;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Diálogo modal para criação e edição de ficha de colaborador no RH.
 * Dados pessoais, upload/avatar de foto, vínculo laboral e dados de pagamento.
 */
final class HREmployeeFormDialog {

    private HREmployeeFormDialog() {}

    static void show(HRPanel owner, EmployeeDTO existing) {
        JTextField numberField = new JTextField(existing == null ? "" : existing.employeeNumber());
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField emailField = new JTextField(existing == null ? "" : existing.email());
        JTextField phoneField = new JTextField(existing == null || existing.phone() == null ? "" : existing.phone());
        final byte[][] photoHolder = {existing == null ? null : existing.photo()};
        CircularAvatar photoPreview = new CircularAvatar(photoHolder[0],
                existing == null ? "+" : CircularAvatar.initials(existing.name()), 104);
        photoPreview.setCameraOverlay(true);
        ModernButton choosePhotoButton = UIHelper.createSecondaryButton("Escolher Foto");
        choosePhotoButton.setIcon(UIHelper.icon("fas-camera", 14));
        ModernButton removePhotoButton = UIHelper.createSecondaryButton("Remover");
        removePhotoButton.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removePhotoButton.setEnabled(photoHolder[0] != null && photoHolder[0].length > 0);
        choosePhotoButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Escolher fotografia do trabalhador");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Imagens (JPG, PNG)", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
            try {
                byte[] photo = UIHelper.readScaledImage(chooser.getSelectedFile(), 512);
                photoHolder[0] = photo;
                photoPreview.setPhoto(photo);
                removePhotoButton.setEnabled(true);
            } catch (Exception ex) {
                owner.showNotice(FeedbackType.ERROR, "Fotografia inválida",
                        "Não foi possível ler a fotografia seleccionada.");
            }
        });
        removePhotoButton.addActionListener(event -> {
            photoHolder[0] = null;
            photoPreview.setPhoto(null);
            removePhotoButton.setEnabled(false);
        });
        JPanel photoActions = new JPanel();
        photoActions.setOpaque(false);
        photoActions.setLayout(new BoxLayout(photoActions, BoxLayout.Y_AXIS));
        choosePhotoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removePhotoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoActions.add(choosePhotoButton);
        photoActions.add(Box.createVerticalStrut(7));
        photoActions.add(removePhotoButton);
        JLabel photoHint = new JLabel("JPG ou PNG · máximo 2 MB");
        photoHint.setForeground(UIHelper.TEXT_MUTED);
        photoHint.setFont(photoHint.getFont().deriveFont(11f));
        photoHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoActions.add(Box.createVerticalStrut(8));
        photoActions.add(photoHint);
        JPanel photoPanel = new JPanel(new BorderLayout(14, 0));
        photoPanel.setOpaque(false);
        photoPanel.add(photoPreview, BorderLayout.WEST);
        photoPanel.add(photoActions, BorderLayout.CENTER);
        JTextField taxIdField = new JTextField(existing == null || existing.taxId() == null ? "" : existing.taxId());
        JTextField inssField = new JTextField(existing == null || existing.inssNumber() == null ? "" : existing.inssNumber());
        JSpinner dependentsSpinner = new JSpinner(new SpinnerNumberModel(existing == null ? 0 : existing.dependentsCount(), 0, 20, 1));
        JTextField departmentField = new JTextField(existing == null ? "" : existing.department());
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"EMPLOYEE", "SELLER", "MANAGER", "ADMIN"});
        roleCombo.setSelectedItem(existing == null ? "EMPLOYEE" : existing.role());
        MoneyField salaryField = new MoneyField(existing == null ? "0" : existing.baseSalary().toPlainString());
        DateField hireDateField = new DateField(existing == null || existing.hireDate() == null
                ? LocalDate.now() : existing.hireDate());
        DateField contractEndField = new DateField(existing == null ? null : existing.contractEndDate());
        JTextField usernameField = new JTextField(existing == null || existing.username() == null
                ? "" : existing.username());
        JTextField bankNameField = new JTextField(existing == null || existing.bankName() == null
                ? "" : existing.bankName());
        JTextField bankAccountField = new JTextField(existing == null || existing.bankAccount() == null
                ? "" : existing.bankAccount());

        for (JTextField field : new JTextField[]{numberField, nameField, emailField, phoneField, taxIdField,
                inssField, departmentField, usernameField, bankNameField, bankAccountField}) {
            UIHelper.styleTextField(field);
        }
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel personalForm = UIHelper.createDialogForm(
                "Fotografia:", photoPanel,
                "Número Interno:", numberField,
                "Nome Completo:", nameField,
                "Email:", emailField,
                "Telefone:", phoneField,
                "NUIT:", taxIdField,
                "Nº INSS:", inssField,
                "Dependentes IRPS:", dependentsSpinner
        );
        JPanel employmentForm = UIHelper.createDialogForm(
                "Departamento:", departmentField,
                "Cargo / Perfil:", roleCombo,
                "Salário Base (MT):", salaryField,
                "Data de Admissão (yyyy-MM-dd):", hireDateField,
                "Fim do Contrato (opcional):", contractEndField
        );
        JPanel accessForm = UIHelper.createDialogForm(
                "Conta de Utilizador (opcional):", usernameField,
                "Banco (opcional):", bankNameField,
                "Conta bancária (opcional):", bankAccountField
        );
        JTabbedPane form = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(form);
        form.addTab("Dados pessoais", UIHelper.icon("fas-user", 14, UIHelper.ACCENT_BLUE), personalForm);
        form.addTab("Vínculo", UIHelper.icon("fas-briefcase", 14, UIHelper.ACCENT_ORANGE), employmentForm);
        form.addTab("Acesso e pagamento", UIHelper.icon("fas-credit-card", 14, UIHelper.APPROVED_GREEN), accessForm);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Novo Colaborador" : "Editar Colaborador", "fas-users",
                "Ficha organizada por dados pessoais, vínculo e pagamento", form)
                .setSize(790, 650)
                .showDialog();
        if (!confirmed) return;

        try {
            UpsertEmployeeRequest request = new UpsertEmployeeRequest(
                    numberField.getText().trim(),
                    nameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    photoHolder[0],
                    taxIdField.getText().trim(),
                    inssField.getText().trim(),
                    (Integer) dependentsSpinner.getValue(),
                    departmentField.getText().trim(),
                    String.valueOf(roleCombo.getSelectedItem()),
                    salaryField.value(),
                    hireDateField.value(),
                    contractEndField.getDate(),
                    usernameField.getText().trim(),
                    bankNameField.getText().trim(),
                    bankAccountField.getText().trim()
            );
            UIHelper.runWithProgress(owner, "A guardar colaborador…",
                    () -> existing == null ? owner.hrApiClient.createEmployee(request)
                            : owner.hrApiClient.updateEmployee(existing.id(), request), ignored -> {
                        owner.loadEmployees();
                        owner.loadExpenses();
                        owner.showSuccess("Dados do colaborador guardados.");
                    }, owner::showActionError);
        } catch (Exception ex) {
            owner.showNotice(FeedbackType.ERROR, "Não foi possível guardar o colaborador", ex.getMessage());
        }
    }

    static void changeStatus(HRPanel owner, EmployeeDTO employee) {
        if (employee == null) return;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "SUSPENDED", "TERMINATED"});
        statusCombo.setSelectedItem(employee.status());
        UIHelper.styleComboBox(statusCombo);
        JPanel form = UIHelper.createDialogForm("Novo Estado:", statusCombo);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Alterar Estado Laboral",
                "fas-user-shield", "Actualizar a situação contratual do colaborador", form)
                .setConfirmButton("Actualizar", "fas-check")
                .showDialog();
        if (!confirmed) return;
        String status = String.valueOf(statusCombo.getSelectedItem());
        UIHelper.runWithProgress(owner, "A actualizar estado laboral…",
                () -> owner.hrApiClient.changeEmployeeStatus(employee.id(), status),
                ignored -> owner.loadEmployees(),
                owner::showActionError);
    }
}
