package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;

import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de operações visuais, mapeamento de tabelas e Quick Peek de tesouraria.
 */
public final class TreasuryOperationsCoordinator {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TreasuryOperationsCoordinator() {
    }

    public static void populateAccountsPeek(QuickPeekPanel peek, DefaultTableModel model, int row) {
        if (row < 0 || row >= model.getRowCount()) return;
        String nome = String.valueOf(model.getValueAt(row, 0));
        String tipo = String.valueOf(model.getValueAt(row, 1));
        String iban = String.valueOf(model.getValueAt(row, 2));
        String saldo = String.valueOf(model.getValueAt(row, 3));

        peek.setHeaderIcon("fas-wallet", UIHelper.APPROVED_GREEN);
        peek.setTitle(nome);
        peek.setSubtitle("Tipo: " + tipo);
        peek.setStatus(tipo, UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("IBAN / Conta", iban, false),
                new QuickPeekPanel.PeekItem("Saldo Actual", saldo, true)
        ));
    }

    public static void populateMovementsPeek(QuickPeekPanel peek, DefaultTableModel model, int row) {
        if (row < 0 || row >= model.getRowCount()) return;
        String data = String.valueOf(model.getValueAt(row, 0));
        String conta = String.valueOf(model.getValueAt(row, 1));
        String desc = String.valueOf(model.getValueAt(row, 2));
        String tipo = String.valueOf(model.getValueAt(row, 3));
        String val = String.valueOf(model.getValueAt(row, 4));

        peek.setHeaderIcon("fas-exchange-alt", UIHelper.ACCENT_BLUE);
        peek.setTitle(desc);
        peek.setSubtitle("Conta: " + conta);
        peek.setStatus(tipo, "CREDIT".equalsIgnoreCase(tipo) ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Data Operação", data, false),
                new QuickPeekPanel.PeekItem("Tipo Movimento", tipo, false),
                new QuickPeekPanel.PeekItem("Valor", val, true)
        ));
    }

    public static void populateAccountsModel(DefaultTableModel model, List<TreasuryAccountDTO> accounts) {
        model.setRowCount(0);
        if (accounts == null) return;
        for (TreasuryAccountDTO acc : accounts) {
            model.addRow(new Object[]{
                    acc.name(),
                    acc.accountType() == TreasuryAccountType.CASH ? "CAIXA" : "BANCO",
                    acc.accountNumber(),
                    acc.balance()
            });
        }
    }

    public static void populateTransactionsModel(DefaultTableModel model, List<TreasuryTransactionDTO> transactions) {
        model.setRowCount(0);
        if (transactions == null) return;
        for (TreasuryTransactionDTO tx : transactions) {
            model.addRow(new Object[]{
                    tx.transactionDate().format(DATE_TIME),
                    tx.accountName(),
                    tx.description(),
                    tx.transactionType(),
                    tx.transactionType().equalsIgnoreCase("DEBIT") ? tx.amount() : tx.amount().negate()
            });
        }
    }

    public static List<InvoiceDTO> extractCollectableInvoices(List<InvoiceDTO> invoices) {
        List<InvoiceDTO> result = new ArrayList<>();
        if (invoices == null) return result;
        for (InvoiceDTO inv : invoices) {
            if (inv.status().isCollectable()) {
                result.add(inv);
            }
        }
        return result;
    }
}
