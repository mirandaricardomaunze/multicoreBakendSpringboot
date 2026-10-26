package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;

import javax.swing.*;
import java.time.LocalDate;

/**
 * Diálogo modal ergonómico para registo de adiantamentos, empréstimos e descontos recorrentes.
 */
public final class HrDeductionFormDialog {

    private HrDeductionFormDialog() {}

    public static void open(HRPanel owner, String kind, Runnable onSuccess) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Colaboradores Necessários", "Cadastre colaboradores primeiro.");
            return;
        }
        boolean recurring = "RECORRENTE".equals(kind);
        boolean loan = "EMPRESTIMO".equals(kind);

        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) {
            empCombo.addItem(e.name() + " — " + e.department());
        }
        JTextField descField = new JTextField(defaultDescription(kind));
        UIHelper.styleTextField(descField);
        MoneyField principalField = new MoneyField("0.00");
        MoneyField installmentField = new MoneyField("0.00");
        JSpinner installmentsSpinner = new JSpinner(new SpinnerNumberModel(loan ? 6 : 1, 1, 120, 1));
        DateField startField = new DateField(LocalDate.now().withDayOfMonth(1));
        DateField endField = new DateField(null);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = recurring
                ? UIHelper.createDialogForm(
                        "Colaborador:", empCombo,
                        "Descrição:", descField,
                        "Valor por recibo:", installmentField,
                        "Início:", startField,
                        "Fim (vazio = sem fim):", endField,
                        "Observações:", notesField)
                : UIHelper.createDialogForm(
                        "Colaborador:", empCombo,
                        "Descrição:", descField,
                        "Valor entregue:", principalField,
                        loan ? "Nº de prestações:" : "Prestações (1):", installmentsSpinner,
                        "Início do desconto:", startField,
                        "Observações:", notesField);

        String subtitle = recurring
                ? "Desconta enquanto estiver activo e dentro da vigência. Não sai dinheiro da caixa."
                : "O valor sai da tesouraria agora e volta pelos recibos seguintes.";
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, dialogTitle(kind),
                iconFor(kind), subtitle, form).showDialog();
        if (!confirmed) {
            return;
        }

        try {
            EmployeeDTO employee = owner.employeesList.get(empCombo.getSelectedIndex());
            CreatePayrollDeductionRequest request = new CreatePayrollDeductionRequest(
                    employee.id(), kind, descField.getText().trim(),
                    recurring ? null : principalField.value(),
                    recurring ? installmentField.value() : null,
                    recurring ? null : (Integer) installmentsSpinner.getValue(),
                    startField.value(),
                    recurring ? endField.value() : null,
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
            UIHelper.runWithProgress(owner, "A registar…",
                    () -> owner.hrApiClient.createDeduction(request),
                    ignored -> {
                        owner.showNotice(FeedbackType.SUCCESS, "Desconto Registado", confirmationOf(kind));
                        onSuccess.run();
                    }, owner::showActionError);
        } catch (RuntimeException ex) {
            owner.showNotice(FeedbackType.ERROR, "Erro ao registar", ex.getMessage());
        }
    }

    private static String dialogTitle(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "Novo Adiantamento";
            case "EMPRESTIMO" -> "Novo Empréstimo";
            default -> "Novo Desconto Recorrente";
        };
    }

    private static String iconFor(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "fas-hand-holding-usd";
            case "EMPRESTIMO" -> "fas-file-invoice-dollar";
            default -> "fas-redo";
        };
    }

    private static String defaultDescription(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "Adiantamento de " + LocalDate.now().getMonthValue()
                    + "/" + LocalDate.now().getYear();
            case "EMPRESTIMO" -> "Empréstimo";
            default -> "";
        };
    }

    private static String confirmationOf(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "Adiantamento registado. O valor saiu da tesouraria e será "
                    + "descontado no recibo do período.";
            case "EMPRESTIMO" -> "Empréstimo registado. O capital saiu da tesouraria e as prestações "
                    + "são descontadas nos recibos seguintes.";
            default -> "Desconto recorrente registado.";
        };
    }
}
