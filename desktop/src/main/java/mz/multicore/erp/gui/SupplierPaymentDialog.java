package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.currency.PaymentExchangeRateField;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo modal ergonómico para liquidação de faturas de compras a crédito. Compra em moeda estrangeira pede o
 * câmbio do pagamento e mostra a diferença cambial antes de confirmar (SPEC FX §3.4, §4.5).
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

        PaymentExchangeRateField rateField = new PaymentExchangeRateField(pa, owner.exchangeRateApiClient);
        String currency = rateField.isForeign() ? pa.currency() : "MT";
        JLabel info = new JLabel("<html><b>Compra:</b> " + pa.purchaseNumber() + " · <b>Fornecedor:</b> "
                + pa.supplierName() + "<br><b>Em dívida:</b> " + DisplayNumbers.money(pa.outstanding()) + " " + currency
                + (rateField.isForeign() ? " · câmbio do documento " + DisplayNumbers.rate(pa.exchangeRate()) : "")
                + "</html>");
        info.setForeground(UIHelper.TEXT_LIGHT);
        rateField.follow(amountField);

        List<Object> fields = new ArrayList<>(List.of(
                "Resumo:", info,
                "Conta de Tesouraria:", accCombo,
                "Valor a Pagar (" + currency + "):", amountField));
        if (rateField.isForeign()) fields.addAll(List.of("Câmbio do Pagamento:", rateField));
        fields.addAll(List.of("Referência:", refField));
        JPanel form = UIHelper.createDialogForm(fields.toArray());

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
            BigDecimal paymentRate = rateField.exchangeRate();
            UIHelper.runWithProgress(owner, "A registar pagamento ao fornecedor…", () -> {
                owner.purchaseApiClient.registerSupplierPayment(pa.purchaseId(), amount, accountId, reference, paymentRate);
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
