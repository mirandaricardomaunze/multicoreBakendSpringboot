package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.CreateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.UpdateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.WarehouseType;

import javax.swing.*;
import java.math.BigDecimal;

/** Diálogo modal para criação e edição de armazéns e centros de distribuição. */
final class WarehouseFormDialog {

    private WarehouseFormDialog() {}

    static void show(StockPanel owner, WarehouseDTO existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.name() : "");
        JTextField numberField = new JTextField(editing && existing.warehouseNumber() != null ? existing.warehouseNumber() : "");
        JTextField capacityField = new JTextField(editing && existing.capacity() != null ? existing.capacity().toPlainString() : "0");
        JTextField locField = new JTextField(editing && existing.location() != null ? existing.location() : "");
        JComboBox<String> typeCombo = new JComboBox<>();
        for (var t : WarehouseType.values()) typeCombo.addItem(t.label());
        UIHelper.styleComboBox(typeCombo);
        if (editing && existing.type() != null) typeCombo.setSelectedIndex(existing.type().ordinal());
        JTextField managerField = new JTextField(editing && existing.manager() != null ? existing.manager() : "");
        JTextField phoneField = new JTextField(editing && existing.phone() != null ? existing.phone() : "");
        UIHelper.styleTextField(managerField);
        UIHelper.styleTextField(phoneField);
        JCheckBox allowsSalesCheck = new JCheckBox("Permite vendas ao balcão (POS)", editing ? existing.allowsSales() : true);
        allowsSalesCheck.setOpaque(false);
        allowsSalesCheck.setForeground(UIHelper.TEXT_LIGHT);

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Nome do Armazem:", nameField,
                "Numero do Armazem:", numberField,
                "Tipo:", typeCombo,
                "Capacidade:", capacityField,
                "Localizacao / Endereco:", locField,
                "Responsável:", managerField,
                "Telefone:", phoneField,
                "Vendas:", allowsSalesCheck
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                editing ? "Editar Armazém" : "Criar Novo Armazém", "fas-warehouse",
                editing ? "Actualize os dados do local" : "Registe um novo local de stock", dialogPanel).showDialog();
        if (!confirmed) return;

        String name = nameField.getText().trim();
        String warehouseNumber = numberField.getText().trim();
        String capacityStr = capacityField.getText().trim();
        String location = locField.getText().trim();

        if (name.isEmpty() || warehouseNumber.isEmpty()) {
            owner.showStockNotice(FeedbackType.ERROR, "Campos obrigatórios", "Nome e número do armazém são obrigatórios.");
            return;
        }

        try {
            BigDecimal capacity = capacityStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(capacityStr);
            if (capacity.compareTo(BigDecimal.ZERO) < 0) {
                owner.showStockNotice(FeedbackType.ERROR, "Capacidade inválida", "A capacidade deve ser zero ou superior.");
                return;
            }
            WarehouseType type = WarehouseType.values()[Math.max(0, typeCombo.getSelectedIndex())];
            boolean allowsSales = allowsSalesCheck.isSelected();
            String manager = managerField.getText().trim();
            String phone = phoneField.getText().trim();
            Long companyId = CurrentUserContext.getCurrentCompanyId();
            UIHelper.runWithProgress(owner, "A guardar armazém…", () -> {
                if (editing) {
                    return owner.inventoryApiClient.updateWarehouse(existing.id(), new UpdateWarehouseRequest(
                            name, warehouseNumber, capacity, location, type, allowsSales, manager, phone));
                }
                return owner.inventoryApiClient.createWarehouse(new CreateWarehouseRequest(
                        name, warehouseNumber, capacity, location, companyId, type, allowsSales, manager, phone));
            }, ignored -> {
                owner.showStockSuccess(editing ? "Armazém '" + name + "' actualizado."
                        : "Armazém '" + name + "' criado com sucesso.");
                owner.onPanelSelected();
            }, owner::showStockError);
        } catch (NumberFormatException ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Capacidade inválida", "A capacidade deve ser um valor numérico.");
        } catch (Exception ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Não foi possível gravar o armazém", ex.getMessage());
        }
    }
}
