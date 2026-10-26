package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;

import javax.swing.*;
import java.awt.Window;

/**
 * Diálogo modal ergonómico para registo e edição de fornecedores.
 */
public final class SupplierFormDialog {

    private SupplierFormDialog() {}

    public static void show(ComprasPanel owner, SupplierDTO existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.name() : "");
        JTextField taxIdField = new JTextField(editing ? existing.taxId() : "");
        JTextField phoneField = new JTextField(editing && existing.phone() != null ? existing.phone() : "");
        JTextField contactField = new JTextField(editing && existing.contactPerson() != null ? existing.contactPerson() : "");
        JTextField emailField = new JTextField(editing && existing.email() != null ? existing.email() : "");
        JTextField addressField = new JTextField(editing && existing.address() != null ? existing.address() : "");

        JPanel form = UIHelper.createDialogForm(
                "Nome / Empresa:", nameField,
                "NUIT / NIF (9 dígitos):", taxIdField,
                "Telefone:", phoneField,
                "Pessoa de Contacto:", contactField,
                "Correio Eletrónico:", emailField,
                "Endereço:", addressField
        );

        Window parent = SwingUtilities.getWindowAncestor(owner);
        ModernFormDialog dlg = new ModernFormDialog(parent, editing ? "Editar Fornecedor" : "Novo Fornecedor", form);
        dlg.setSize(520, 480);
        dlg.setOnSaveAsync(() -> {
            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            if (name.isEmpty() || taxId.isEmpty()) {
                throw new RuntimeException("Nome e NUIT/NIF são campos obrigatórios.");
            }
            CreateSupplierRequest req = new CreateSupplierRequest(
                    name, taxId,
                    emailField.getText().trim(),
                    addressField.getText().trim(),
                    phoneField.getText().trim(),
                    contactField.getText().trim(),
                    CurrentUserContext.getCurrentCompanyId());
            return () -> editing ? owner.purchaseApiClient.updateSupplier(existing.id(), req)
                    : owner.purchaseApiClient.createSupplier(req);
        });

        if (dlg.showDialog()) {
            owner.showPurchaseSuccess("Fornecedor '" + nameField.getText().trim()
                    + (editing ? "' actualizado." : "' registado."));
            owner.loadSuppliers();
        }
    }
}
