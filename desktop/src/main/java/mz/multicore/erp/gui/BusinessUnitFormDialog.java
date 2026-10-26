package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.dto.UpdateBusinessUnitRequest;

import javax.swing.*;
import java.util.List;

/** Diálogo modal ergonómico para registo e edição de linhas de negócio/distribuição. */
final class BusinessUnitFormDialog {

    private BusinessUnitFormDialog() {}

    static void show(JComponent parent, ComprasPanel owner, BusinessUnitDTO edit, Runnable onSaved) {
        JTextField codeField = new JTextField(edit != null ? edit.code() : "");
        JTextField nameField = new JTextField(edit != null ? edit.name() : "");
        JTextField descField = new JTextField(edit != null && edit.description() != null ? edit.description() : "");

        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(descField);

        JComboBox<SupplierOption> supplierCombo = new JComboBox<>();
        UIHelper.styleComboBox(supplierCombo);
        supplierCombo.addItem(new SupplierOption(null, "— Nenhum / Fornecedor livre —"));

        List<SupplierDTO> suppliers = owner.suppliersList;
        int selectedSupplierIdx = 0;
        if (suppliers != null) {
            for (int i = 0; i < suppliers.size(); i++) {
                SupplierDTO s = suppliers.get(i);
                supplierCombo.addItem(new SupplierOption(s.id(), s.name()));
                if (edit != null && edit.supplierId() != null && edit.supplierId().equals(s.id())) {
                    selectedSupplierIdx = i + 1;
                }
            }
        }
        supplierCombo.setSelectedIndex(selectedSupplierIdx);

        JComponent form = UIHelper.createDialogForm(
                "Código (ex.: UNL, BAT, FOOD):", codeField,
                "Nome da Linha de Negócio:", nameField,
                "Fornecedor Fabricante Associado:", supplierCombo,
                "Descrição Comercial:", descField
        );

        String title = edit == null ? "Nova Linha de Negócio" : "Editar Linha de Negócio " + edit.code();
        ModernFormDialog dialog = new ModernFormDialog(
                SwingUtilities.getWindowAncestor(parent),
                title,
                "fas-briefcase",
                "Associa produtos e compras à divisão comercial (ex.: Unilever, BAT, Alimentares).",
                form
        );

        UIHelper.applyStandardModalBounds(dialog.getDialog(), 520, 480);

        dialog.setOnSaveAsync(() -> {
            String code = codeField.getText().trim().toUpperCase();
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            SupplierOption selSup = (SupplierOption) supplierCombo.getSelectedItem();
            Long supplierId = selSup != null ? selSup.id() : null;

            if (code.isBlank()) {
                SwingUtilities.invokeLater(codeField::requestFocusInWindow);
                throw new IllegalArgumentException("Indique o código do negócio (ex.: UNL, BAT, FOOD).");
            }
            if (name.isBlank()) {
                SwingUtilities.invokeLater(nameField::requestFocusInWindow);
                throw new IllegalArgumentException("Indique o nome da linha de negócio.");
            }

            return () -> {
                if (edit == null) {
                    CreateBusinessUnitRequest req = new CreateBusinessUnitRequest(code, name, supplierId, desc.isBlank() ? null : desc);
                    return owner.purchaseApiClient.createBusinessUnit(req);
                } else {
                    UpdateBusinessUnitRequest req = new UpdateBusinessUnitRequest(code, name, supplierId, desc.isBlank() ? null : desc, edit.active());
                    return owner.purchaseApiClient.updateBusinessUnit(edit.id(), req);
                }
            };
        });

        SwingUtilities.invokeLater(codeField::requestFocusInWindow);

        if (dialog.showDialog()) {
            ToastManager.success(parent, "Linha de negócio gravada com sucesso.");
            onSaved.run();
        }
    }

    private record SupplierOption(Long id, String name) {
        @Override
        public String toString() { return name; }
    }
}
