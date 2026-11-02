package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Diálogo modal ergonómico para registo de recebimento tardio de faturas a prazo. */
public final class OutstandingPaymentDialog {

    private OutstandingPaymentDialog() {}

    public static void show(JComponent parent,
                            InvoiceDTO invoice,
                            List<TreasuryAccountDTO> accounts,
                            POSApiClient posApiClient,
                            Runnable onSuccess,
                            BiConsumer<String, String> onErrorNotice,
                            Consumer<Throwable> onErrorAction) {
        BigDecimal paid = invoice.amountPaid() == null ? BigDecimal.ZERO : invoice.amountPaid();
        BigDecimal outstanding = invoice.outstandingAmount();
        JComboBox<String> method = new JComboBox<>(new String[]{"CASH", "CARD", "BANK_TRANSFER"});
        UIHelper.styleComboBox(method);
        JComboBox<String> account = new JComboBox<>();
        for (TreasuryAccountDTO item : accounts) account.addItem(item.name());
        UIHelper.styleComboBox(account);
        MoneyField amount = new MoneyField(outstanding.toPlainString());
        JTextField reference = new JTextField();
        UIHelper.styleTextField(reference);
        JLabel info = new JLabel(String.format(
                "<html><b>Fatura:</b> %s · <b>Cliente:</b> %s<br><b>Total:</b> %,.2f MT &nbsp; " +
                        "<b>Pago:</b> %,.2f MT &nbsp; <b>Em dívida:</b> %,.2f MT</html>",
                invoice.invoiceNumber(), invoice.clientName(), invoice.totalAmount(), paid, outstanding));
        info.setForeground(UIHelper.TEXT_LIGHT);
        JPanel form = UIHelper.createDialogForm("Resumo:", info, "Método:", method,
                "Conta de Tesouraria:", account, "Valor a Receber (MT):", amount,
                "Referência (Nº recibo/transação):", reference);
        if (!new ModernFormDialog(UIHelper.mainWindow, "Receber Pagamento — " + invoice.invoiceNumber(),
                "fas-money-bill-wave", "Liquidação de fatura em dívida", form)
                .setConfirmButton("Receber", "fas-money-bill-wave").showDialog()) return;
        try {
            BigDecimal value = amount.value();
            if (value.signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
            Long accountId = accounts.isEmpty() ? null : accounts.get(account.getSelectedIndex()).id();
            PosPaymentRequest request = new PosPaymentRequest(String.valueOf(method.getSelectedItem()), value, value,
                    reference.getText().trim().isEmpty() ? null : reference.getText().trim(), accountId,
                    "PAY-" + UUID.randomUUID());
            UIHelper.runWithProgress(parent, "A registar pagamento…", () -> {
                posApiClient.registerLatePayment(invoice.id(), request);
                return null;
            }, ignored -> {
                ToastManager.success(parent, "Pagamento registado com sucesso.");
                onSuccess.run();
            }, onErrorAction::accept);
        } catch (IllegalArgumentException error) {
            onErrorNotice.accept("Dados de pagamento inválidos", error.getMessage());
        }
    }
}
