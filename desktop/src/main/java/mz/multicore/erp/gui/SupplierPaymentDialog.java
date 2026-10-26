package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;

import javax.swing.*;
import java.math.BigDecimal;

/**
 * Diálogo modal ergonómico para liquidação de faturas de compras a crédito.
 */
public final class SupplierPaymentDialog {

    private SupplierPaymentDialog() {}

    public static void show(ComprasPanel owner, Runnable onPaymentCompleted) {
        int row = TableFilter.selectedModelRow(owner.payablesTable);
        if (row < 0 || owner.payablesList == null || row >= owner.payablesList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione uma conta a pagar",
                    "Escolha uma conta na tabela para continuar.");
            return;
        }
        PayableDTO pa = owner.payablesList.get(row);
        if (owner.accountsList.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Sem contas de tesouraria",
                    "Registe uma conta de tesouraria antes do pagamento.");
            return;
        }

        JComboBox<String> accCombo = new JComboBox<>();
        for (TreasuryAccountDTO a : owner.accountsList) accCombo.addItem(a.name());
        UIHelper.styleComboBox(accCombo);
        MoneyField amountField = new MoneyField(pa.outstanding().toPlainString());
        JTextField refField = new JTextField();
        UIHelper.styleTextField(refField);

        JLabel info = new JLabel(String.format(
                "<html><b>Compra:</b> %s · <b>Fornecedor:</b> %s<br><b>Em dívida:</b> %,.2f MT</html>",
                pa.purchaseNumber(), pa.supplierName(), pa.outstanding()));
        info.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Resumo:", info,
                "Conta de Tesouraria:", accCombo,
                "Valor a Pagar (MT):", amountField,
                "Referência:", refField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Pagar a Fornecedor - " + pa.purchaseNumber(), "fas-money-bill-wave",
                "Liquidação de compra a crédito", form)
                .setConfirmButton("Pagar", "fas-money-bill-wave").showDialog();
        if (!confirmed) return;

        try {
            BigDecimal amount = amountField.value();
            Long accountId = owner.accountsList.get(accCombo.getSelectedIndex()).id();
            String ref = refField.getText().trim();
            String reference = ref.isEmpty() ? null : ref;
            UIHelper.runWithProgress(owner, "A registar pagamento ao fornecedor…", () -> {
                owner.purchaseApiClient.registerSupplierPayment(pa.purchaseId(), amount, accountId, reference);
                return null;
            }, ignored -> {
                owner.showPurchaseSuccess("Pagamento registado.");
                onPaymentCompleted.run();
                owner.loadPurchasesHistory();
                owner.loadAccounts();
            }, owner::showPurchaseError);
        } catch (IllegalArgumentException ex) {
            owner.showPurchaseNotice(FeedbackType.ERROR, "Dados de pagamento inválidos", ex.getMessage());
        } catch (Exception ex) {
            owner.showPurchaseNotice(FeedbackType.ERROR, "Não foi possível registar o pagamento", ex.getMessage());
        }
    }
}
