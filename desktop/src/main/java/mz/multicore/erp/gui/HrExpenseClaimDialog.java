package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;

import javax.swing.*;
import java.math.BigDecimal;

/**
 * Diálogo modal ergonómico para submissão de notas de despesa de colaboradores.
 */
public final class HrExpenseClaimDialog {

    private HrExpenseClaimDialog() {}

    public static void show(HRPanel owner, Runnable onSubmitted) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe colaboradores primeiro.");
            return;
        }

        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " - " + e.department());

        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
                "MEALS (Alimentação)", "TRAVEL (Deslocações)", "LODGING (Alojamento)", "OTHER (Outros)"
        });
        UIHelper.styleComboBox(categoryCombo);
        MoneyField amountField = new MoneyField();
        JTextField descField = new JTextField();
        UIHelper.styleTextField(descField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Categoria:", categoryCombo,
                "Valor (MT):", amountField,
                "Descrição:", descField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Submeter Despesa", "fas-receipt",
                "Nota de despesa do colaborador", form)
                .setConfirmButton("Submeter", "fas-paper-plane").showDialog();
        if (!confirmed) return;

        try {
            BigDecimal amount = amountField.value();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                owner.showNotice(FeedbackType.ERROR, "Descrição obrigatória", "Indique uma descrição.");
                return;
            }
            EmployeeDTO emp = owner.employeesList.get(empCombo.getSelectedIndex());
            String cat = categoryCombo.getSelectedItem().toString().split(" ")[0];
            CreateExpenseClaimRequest request = new CreateExpenseClaimRequest(emp.id(), amount, cat, desc);
            UIHelper.runWithProgress(owner, "A submeter despesa…", () -> owner.hrApiClient.submitExpense(request), ignored -> {
                owner.showSuccess("Despesa submetida.");
                onSubmitted.run();
            }, owner::showActionError);
        } catch (IllegalArgumentException ex) {
            owner.showNotice(FeedbackType.ERROR, "Dados da despesa inválidos", ex.getMessage());
        } catch (Exception ex) {
            owner.showNotice(FeedbackType.ERROR, "Não foi possível submeter a despesa", ex.getMessage());
        }
    }
}
