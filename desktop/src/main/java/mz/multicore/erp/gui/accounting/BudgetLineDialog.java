package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.model.AccountClass;

import javax.swing.*;
import java.util.List;

/**
 * "Adicionar linha" of the budget grid (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.4, OR-UI-01): asks for a postable,
 * active account of class 6/7 and an active cost centre, and adds the empty account × centre row to the grid.
 */
final class BudgetLineDialog {

    private BudgetLineDialog() {}

    /** Accounts that can carry a budget: postable, active, class 6 (costs) or 7 (revenues). */
    static List<AccountDTO> budgetableAccounts(List<AccountDTO> accounts) {
        return (accounts == null ? List.<AccountDTO>of() : accounts).stream()
                .filter(account -> account.postable() && account.active())
                .filter(account -> account.accountClass() == AccountClass.CUSTOS_E_PERDAS
                        || account.accountClass() == AccountClass.PROVEITOS_E_GANHOS)
                .toList();
    }

    /** Opens the dialog; returns {@code true} when a row was added to {@code grid}. */
    static boolean open(AccountingApiClient accountingApiClient, BudgetGridModel grid) {
        JComboBox<AccountDTO> account = UIHelper.createComboBox(new AccountDTO[0],
                a -> a == null ? "" : a.code() + " — " + a.name());
        UIHelper.loadAsync(account, () -> accountingApiClient.getAccounts(), accounts -> {
            account.removeAllItems();
            budgetableAccounts(accounts).forEach(account::addItem);
        });
        CostCenterCombo costCenter = new CostCenterCombo().load(accountingApiClient);

        JPanel form = UIHelper.createDialogForm("Conta (classe 6 ou 7):", account, "Centro de custo:", costCenter);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Adicionar linha", "fas-plus",
                "Conta e centro de custo a orçamentar", form);
        dialog.setOnSave(() -> {
            if (!(account.getSelectedItem() instanceof AccountDTO chosen)) {
                throw new IllegalArgumentException("Escolha a conta a orçamentar.");
            }
            Long centerId = costCenter.selectedCostCenterId();
            if (centerId == null) throw new IllegalArgumentException("Escolha um centro de custo activo.");
            grid.addLine(chosen.code(), chosen.name(), centerId, String.valueOf(costCenter.getSelectedItem()));
        });
        return dialog.showDialog();
    }
}
