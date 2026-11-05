package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Diálogo modal corporativo para liquidação de faturas aprovadas em contas de tesouraria.
 */
public final class TreasuryReceiptDialog {

    private TreasuryReceiptDialog() {
    }

    public static void show(
            Component parent,
            FinanceApiClient financeApiClient,
            List<InvoiceDTO> approvedInvoices,
            List<TreasuryAccountDTO> accounts,
            InlineFeedbackPanel feedback,
            Runnable onSuccess
    ) {
        if (approvedInvoices == null || approvedInvoices.isEmpty()) {
            if (feedback != null) {
                feedback.show(FeedbackType.INFO, "Sem recebimentos pendentes",
                        "Não existem faturas aprovadas pendentes de recebimento.", null, null);
            }
            return;
        }
        if (accounts == null || accounts.isEmpty()) {
            if (feedback != null) {
                feedback.show(FeedbackType.WARNING, "Sem contas de tesouraria",
                        "Configure uma conta de tesouraria antes de registar o recebimento.", null, null);
            }
            return;
        }

        JComboBox<String> invoiceCombo = new JComboBox<>();
        UIHelper.styleComboBox(invoiceCombo);
        for (InvoiceDTO inv : approvedInvoices) {
            invoiceCombo.addItem(inv.invoiceNumber() + " - " + inv.buyerName() + " (" + String.format("%,.2f MT", inv.totalAmount()) + ")");
        }

        JComboBox<String> accountCombo = new JComboBox<>();
        UIHelper.styleComboBox(accountCombo);
        for (TreasuryAccountDTO acc : accounts) {
            accountCombo.addItem(acc.name());
        }

        JPanel form = UIHelper.createDialogForm(
                "Fatura Aprovada:", invoiceCombo,
                "Conta de Recebimento:", accountCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Recebimento",
                "fas-money-bill-wave", "Liquidação de fatura aprovada", form)
                .setConfirmButton("Receber", "fas-check");

        dlg.setOnSaveAsync(() -> {
            InvoiceDTO invoice = approvedInvoices.get(Math.max(0, invoiceCombo.getSelectedIndex()));
            TreasuryAccountDTO account = accounts.get(Math.max(0, accountCombo.getSelectedIndex()));
            return () -> {
                financeApiClient.payInvoice(invoice.id(), account.id());
                return null;
            };
        });

        if (dlg.showDialog()) {
            ToastManager.success(parent, "Recebimento registado; saldo da conta actualizado.");
            if (onSuccess != null) onSuccess.run();
        }
    }
}
