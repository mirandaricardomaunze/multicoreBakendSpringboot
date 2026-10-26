package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Diálogo modal para registo de recebimento de fatura e emissão de recibo comercial.
 */
public final class ReceiptPaymentDialog {

    private ReceiptPaymentDialog() {}

    public static void show(Component parent, ComercialApiClient apiClient, InlineFeedbackPanel feedback,
                            Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal,
                            List<TreasuryAccountDTO> accounts, Runnable onRefresh, Runnable invoicesRefresh) {
        if (accounts.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Sem contas de tesouraria", "Registe uma conta antes de emitir o recibo.", null, null);
            return;
        }
        JComboBox<String> accountCombo = new JComboBox<>();
        UIHelper.styleComboBox(accountCombo);
        accounts.forEach(account -> accountCombo.addItem(account.name() + " (" + account.balance() + " MT)"));

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"DINHEIRO", "TRANSFERÊNCIA", "M-PESA", "CARTÃO"});
        UIHelper.styleComboBox(methodCombo);
        MoneyField amountField = new MoneyField(invoiceTotal.toString());

        JLabel invoiceLabel = new JLabel(invoiceNumber);
        invoiceLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        invoiceLabel.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm("Fatura:", invoiceLabel,
                "Conta de Tesouraria:", accountCombo, "Método de Pagamento:", methodCombo,
                "Montante a Receber (MT):", amountField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Registar Recebimento (Emitir Recibo)", "fas-receipt",
                "Recebimento de cliente e emissão de recibo", form)
                .setConfirmButton("Receber", "fas-money-bill-wave").showDialog();

        if (!confirmed || accountCombo.getSelectedIndex() < 0) return;

        try {
            BigDecimal amount = amountField.value();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                feedback.show(FeedbackType.ERROR, "Valor inválido", "O valor pago deve ser maior que zero.", null, null);
                return;
            }
            Long accountId = accounts.get(accountCombo.getSelectedIndex()).id();
            String method = String.valueOf(methodCombo.getSelectedItem());

            UIHelper.runWithProgress(parent, "A emitir recibo…",
                    () -> apiClient.createReceipt(invoiceId, accountId, method, amount), ignored -> {
                        BigDecimal remaining = invoiceTotal.subtract(amount);
                        String message = remaining.compareTo(BigDecimal.ZERO) > 0
                                ? "Recibo emitido. Continuam por receber " + remaining + " MT desta fatura."
                                : "Fatura liquidada com sucesso! Recibo emitido.";
                        ToastManager.success(parent, message);
                        if (invoicesRefresh != null) invoicesRefresh.run();
                        if (onRefresh != null) onRefresh.run();
                    }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível emitir o recibo", error.getMessage(), null, null));
        } catch (IllegalArgumentException ex) {
            feedback.show(FeedbackType.ERROR, "Dados de pagamento inválidos", ex.getMessage(), null, null);
        }
    }
}
