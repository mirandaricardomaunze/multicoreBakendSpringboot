package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.CreateBankExpenseAndMatchRequest;
import mz.multicore.erp.modules.financeira.dto.ManualReconciliationRequest;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogos modais ergonómicos para reconciliação manual e lançamento de encargos bancários.
 */
final class BankReconciliationMatchDialogs {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BankReconciliationMatchDialogs() {}

    static void openManualMatchDialog(BankReconciliationPanel owner) {
        int row = owner.itemsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "Seleccione um item do extracto pendente para associar a uma transacção de tesouraria.");
            return;
        }
        int modelRow = owner.itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = owner.currentItems.get(modelRow);

        TreasuryAccountDTO acc = (TreasuryAccountDTO) owner.accountCombo.getSelectedItem();
        if (acc == null) return;

        BigDecimal targetAmt = item.amount().abs();
        List<TreasuryTransactionDTO> candidates = owner.treasuryTransactions.stream()
                .filter(t -> t.accountId().equals(acc.id()))
                .filter(t -> t.amount().compareTo(targetAmt) == 0)
                .toList();

        JComboBox<TreasuryTransactionDTO> txCombo = new JComboBox<>();
        UIHelper.styleComboBox(txCombo);
        txCombo.setPreferredSize(new Dimension(420, UIHelper.FORM_CONTROL_HEIGHT));
        txCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TreasuryTransactionDTO t) {
                    setText("#TX-" + t.id() + " | " + t.transactionDate().format(DATE_FMT) + " | " + t.description() + " (" + String.format("%,.2f MT", t.amount()) + ")");
                } else {
                    setText("Nenhuma transacção compatível encontrada");
                }
                return this;
            }
        });

        for (TreasuryTransactionDTO t : candidates) {
            txCombo.addItem(t);
        }

        JPanel form = UIHelper.createDialogForm(
                "Item do Extracto:", new JLabel(item.description() + " (" + String.format("%,.2f MT", item.amount()) + ")"),
                "Transacção de Tesouraria:", txCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Conciliar Manualmente",
                "fas-link", "Associe este movimento bancário a uma transacção da tesouraria", form)
                .setConfirmButton("Vincular", "fas-check");

        dlg.setOnSaveAsync(() -> {
            TreasuryTransactionDTO selectedTx = (TreasuryTransactionDTO) txCombo.getSelectedItem();
            if (selectedTx == null) {
                throw new IllegalArgumentException("Seleccione uma transacção para vincular.");
            }
            return () -> owner.apiClient.manualMatch(new ManualReconciliationRequest(item.id(), selectedTx.id()));
        });

        if (dlg.showDialog()) {
            ToastManager.success(owner, "Movimento conciliado manualmente!");
            owner.onStatementChanged();
        }
    }

    static void openExpenseDialog(BankReconciliationPanel owner) {
        int row = owner.itemsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "Seleccione a linha do extracto para lançar a despesa ou receita directa de tesouraria.");
            return;
        }
        int modelRow = owner.itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = owner.currentItems.get(modelRow);

        JTextField descField = new JTextField(item.description());
        UIHelper.styleTextField(descField);
        descField.setPreferredSize(new Dimension(380, UIHelper.FORM_CONTROL_HEIGHT));

        BigDecimal feeAmt = item.amount().abs();
        JTextField amtField = new JTextField(feeAmt.toString());
        UIHelper.styleTextField(amtField);
        amtField.setPreferredSize(new Dimension(380, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel form = UIHelper.createDialogForm(
                "Descrição do Encargo:", descField,
                "Valor do Encargo (MT):", amtField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Lançar Encargo Bancário",
                "fas-receipt", "Cria saída na tesouraria e concilia imediatamente o movimento", form)
                .setConfirmButton("Lançar e Conciliar", "fas-check");

        dlg.setOnSaveAsync(() -> {
            String desc = descField.getText().trim();
            BigDecimal amount = new BigDecimal(amtField.getText().trim());
            return () -> owner.apiClient.createExpenseAndMatch(new CreateBankExpenseAndMatchRequest(item.id(), desc, amount));
        });

        if (dlg.showDialog()) {
            ToastManager.success(owner, "Encargo bancário debitado e conciliado!");
            owner.onStatementChanged();
        }
    }
}
