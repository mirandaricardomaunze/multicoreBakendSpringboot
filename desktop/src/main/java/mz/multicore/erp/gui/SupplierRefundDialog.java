package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Regista o dinheiro que o fornecedor devolve depois de uma devolução de mercadoria já paga: entra na conta
 * escolhida e abate ao que o fornecedor deve.
 */
final class SupplierRefundDialog {

    private SupplierRefundDialog() {
    }

    static ModernButton button(ComprasPanel owner, JTable purchasesTable) {
        ModernButton button = UIHelper.createSecondaryButton("Reembolso do Fornecedor");
        button.setIcon(UIHelper.icon("fas-hand-holding-usd", 14));
        button.addActionListener(e -> SupplierReturnDialog.withSelectedPurchase(owner, purchasesTable, purchase -> {
            if (purchase.supplierOwes().signum() <= 0) {
                ToastManager.show(owner, FeedbackType.WARNING, "O fornecedor não deve nada nesta compra. "
                        + "Só há reembolso depois de devolver mercadoria já paga.");
                return;
            }
            UIHelper.loadAsync(owner, owner.financeApiClient::getAllAccounts, accounts -> show(owner, purchase, accounts),
                    error -> owner.showPurchaseLoadError("contas de tesouraria", error));
        }));
        return button;
    }

    private static void show(ComprasPanel owner, PurchaseDTO purchase, List<TreasuryAccountDTO> accounts) {
        if (accounts.isEmpty()) {
            ToastManager.show(owner, FeedbackType.WARNING, "Registe uma conta de tesouraria primeiro.");
            return;
        }
        JComboBox<String> account = new JComboBox<>();
        UIHelper.styleComboBox(account);
        accounts.forEach(a -> account.addItem(a.name()));
        MoneyField amount = new MoneyField(purchase.supplierOwes().toPlainString());
        JTextField reference = new JTextField();
        JPanel form = UIHelper.createDialogForm("Conta onde entrou:", account, "Valor recebido (MT):", amount,
                "Referência (opcional):", reference);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Reembolso do Fornecedor", "fas-hand-holding-usd",
                "Compra " + purchase.purchaseNumber() + " — " + purchase.supplierName() + ": o fornecedor deve "
                        + purchase.supplierOwes().toPlainString() + " MT.", form);
        dialog.setConfirmButton("Registar Reembolso", "fas-check-circle");
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = amount.value();
            Long accountId = accounts.get(Math.max(0, account.getSelectedIndex())).id();
            String ref = reference.getText();
            return () -> owner.purchaseApiClient.registerSupplierRefund(purchase.id(), value, accountId, ref);
        });
        if (dialog.showDialog()) {
            ToastManager.success(owner, "Reembolso registado.");
            owner.loadPurchasesHistory();
        }
    }
}
