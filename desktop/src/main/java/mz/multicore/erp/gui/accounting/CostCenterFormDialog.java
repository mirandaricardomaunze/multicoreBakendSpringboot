package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.dto.SaveCostCenterRequest;

import javax.swing.*;

/**
 * New or edited manual cost centre (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1): code, name and, when editing,
 * the state. The rules (unique code, the default centre cannot be deactivated, warehouse centres are not edited)
 * are the backend's; its message is shown in the dialog when refused.
 */
final class CostCenterFormDialog {

    private static final String ACTIVE = "Activo";
    private static final String INACTIVE = "Inactivo";

    private final AccountingApiClient accountingApiClient;
    private final CostCenterDTO existing;

    CostCenterFormDialog(AccountingApiClient accountingApiClient, CostCenterDTO existing) {
        this.accountingApiClient = accountingApiClient;
        this.existing = existing;
    }

    /** Opens the dialog; true when the centre was saved. */
    boolean show() {
        JTextField code = new JTextField(existing == null ? "" : existing.code());
        JTextField name = new JTextField(existing == null ? "" : existing.name());
        UIHelper.styleTextField(code);
        UIHelper.styleTextField(name);
        JComboBox<String> state = UIHelper.createComboBox(new String[]{ACTIVE, INACTIVE}, label -> label);
        state.setSelectedItem(existing == null || existing.active() ? ACTIVE : INACTIVE);
        state.setEnabled(existing != null);

        JPanel form = UIHelper.createDialogForm(
                "", new FormField("Código", code, true, "Único na empresa, até 20 caracteres."),
                "", new FormField("Nome", name, true, "Ex.: Administração, Frota, Produção."),
                "Estado:", state);
        ModernFormDialog dialog = existing == null
                ? new ModernFormDialog(UIHelper.mainWindow, "Novo centro de custo", "fas-plus",
                        "Centro manual da empresa activa", form)
                : new ModernFormDialog(UIHelper.mainWindow, "Editar centro de custo", "fas-edit",
                        existing.code() + " — " + existing.name(), form);
        dialog.setOnSaveAsync(() -> {
            SaveCostCenterRequest request = new SaveCostCenterRequest(code.getText().trim(), name.getText().trim(),
                    existing == null ? null : ACTIVE.equals(state.getSelectedItem()));
            if (existing == null) return () -> accountingApiClient.createCostCenter(request);
            Long id = existing.id();
            return () -> accountingApiClient.updateCostCenter(id, request);
        });
        return dialog.showDialog();
    }
}
