package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierReturnRequest;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseLineDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Devolução de mercadoria ao fornecedor (nota DF) a partir de uma fatura de compra registada: o operador indica o
 * que sai em cada linha e o motivo; o servidor tira o stock, abate a dívida e estorna o IVA dedutível.
 */
final class SupplierReturnDialog {

    private static final int COL_PRODUCT = 0, COL_BOUGHT = 1, COL_RETURNED = 2, COL_AVAILABLE = 3, COL_TO_RETURN = 4;

    private SupplierReturnDialog() {
    }

    static mz.multicore.erp.gui.components.ModernButton button(ComprasPanel owner, JTable purchasesTable) {
        var button = UIHelper.createSecondaryButton("Devolver ao Fornecedor");
        button.setIcon(UIHelper.icon("fas-undo", 14));
        button.addActionListener(e -> withSelectedPurchase(owner, purchasesTable, purchase -> show(owner, purchase)));
        return button;
    }

    /** Loads the purchase selected in the history (with its lines) and hands it to the action. */
    static void withSelectedPurchase(ComprasPanel owner, JTable purchasesTable, java.util.function.Consumer<PurchaseDTO> action) {
        int row = TableFilter.selectedModelRow(purchasesTable);
        if (row < 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "Seleccione uma fatura de compra.");
            return;
        }
        String number = String.valueOf(purchasesTable.getModel().getValueAt(row, 0));
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.getPurchasesByCompany(companyId).stream()
                        .filter(p -> number.equals(p.purchaseNumber())).findFirst().orElse(null),
                purchase -> {
                    if (purchase == null) ToastManager.show(owner, FeedbackType.ERROR, "Compra " + number + " não encontrada.");
                    else action.accept(purchase);
                },
                error -> owner.showPurchaseLoadError("compra seleccionada", error));
    }

    private static void show(ComprasPanel owner, PurchaseDTO purchase) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Artigo", "Comprado", "Já devolvido", "Pode devolver", "A devolver"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == COL_TO_RETURN;
            }
        };
        List<PurchaseLineDTO> lines = purchase.lines();
        for (PurchaseLineDTO l : lines) {
            BigDecimal returned = l.returnedQuantity() == null ? BigDecimal.ZERO : l.returnedQuantity();
            model.addRow(new Object[]{l.productName(), l.quantity(), returned, l.quantity().subtract(returned), BigDecimal.ZERO});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(640, 200));
        JTextField reason = new JTextField();
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);
        content.add(UIHelper.createDialogForm("Motivo da devolução:", reason), BorderLayout.SOUTH);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Devolver ao Fornecedor", "fas-undo",
                "Compra " + purchase.purchaseNumber() + " — " + purchase.supplierName()
                        + ". A mercadoria sai do armazém e a dívida ao fornecedor baixa.", content);
        dialog.setConfirmButton("Registar Devolução", "fas-check-circle");
        dialog.setOnSaveAsync(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            List<CreateSupplierReturnRequest.Line> toReturn = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                BigDecimal qty = decimal(model.getValueAt(i, COL_TO_RETURN));
                if (qty.signum() < 0) throw new IllegalArgumentException("A quantidade a devolver não pode ser negativa.");
                if (qty.compareTo(decimal(model.getValueAt(i, COL_AVAILABLE))) > 0) {
                    throw new IllegalArgumentException(lines.get(i).productName() + ": só pode devolver "
                            + model.getValueAt(i, COL_AVAILABLE) + ".");
                }
                if (qty.signum() > 0) toReturn.add(new CreateSupplierReturnRequest.Line(lines.get(i).id(), qty));
            }
            if (toReturn.isEmpty()) throw new IllegalArgumentException("Indique a quantidade a devolver em pelo menos uma linha.");
            if (reason.getText().isBlank()) throw new IllegalArgumentException("Indique o motivo da devolução.");
            CreateSupplierReturnRequest request = new CreateSupplierReturnRequest(reason.getText().trim(), toReturn);
            return () -> owner.purchaseApiClient.registerSupplierReturn(purchase.id(), request);
        });
        if (dialog.showDialog()) {
            ToastManager.success(owner, "Devolução registada. A dívida ao fornecedor e o stock foram actualizados.");
            owner.loadPurchasesHistory();
            owner.loadPayables();
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal b) return b;
        try {
            return value == null || value.toString().isBlank() ? BigDecimal.ZERO
                    : new BigDecimal(value.toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantidade inválida: " + value);
        }
    }
}
