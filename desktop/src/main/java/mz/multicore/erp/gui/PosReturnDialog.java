package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Fluxo de devolução/troca iniciado a partir do histórico do POS. */
final class PosReturnDialog {
    private final POSPanel owner;
    PosReturnDialog(POSPanel owner) { this.owner = owner; }

    public void show() {
        int selectedRow = owner.salesHistoryTable == null ? -1 : TableFilter.selectedModelRow(owner.salesHistoryTable);
        if (selectedRow < 0 || selectedRow >= owner.salesHistoryList.size()) {
            JOptionPane.showMessageDialog(owner, "Selecione uma venda no histórico primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (owner.warehousesList.isEmpty()) {
            owner.loadMetadata();
        }
        if (owner.warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Não há armazéns configurados para receber a devolução.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        InvoiceDTO invoice = owner.salesHistoryList.get(selectedRow);
        DefaultTableModel linesModel = new DefaultTableModel(
                new String[]{"Linha ID", "Produto", "Vendido", "Qtd a devolver"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        for (var line : invoice.lines()) {
            linesModel.addRow(new Object[]{
                    line.id(),
                    line.productName(),
                    line.quantity().stripTrailingZeros().toPlainString(),
                    "0"
            });
        }
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);
        linesTable.getColumnModel().getColumn(0).setMinWidth(0);
        linesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        linesTable.getColumnModel().getColumn(0).setWidth(0);

        JComboBox<String> warehouseReturnCombo = new JComboBox<>();
        for (WarehouseDTO warehouse : owner.warehousesList) {
            warehouseReturnCombo.addItem(warehouse.name());
        }
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CASH", "CARD", "BANK_TRANSFER", "MPESA", "EMOLA", "CREDIT"});
        JComboBox<String> refundAccountCombo = new JComboBox<>();
        for (TreasuryAccountDTO account : owner.accountsList) {
            refundAccountCombo.addItem(account.name());
        }
        JTextField reasonField = new JTextField("Devolução de cliente");
        UIHelper.styleComboBox(warehouseReturnCombo);
        UIHelper.styleComboBox(methodCombo);
        UIHelper.styleComboBox(refundAccountCombo);
        UIHelper.styleTextField(reasonField);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(new JScrollPane(linesTable), BorderLayout.CENTER);
        panel.add(UIHelper.createDialogForm(
                "Armazém de entrada:", warehouseReturnCombo,
                "Método de reembolso:", methodCombo,
                "Conta para reembolso:", refundAccountCombo,
                "Motivo:", reasonField
        ), BorderLayout.SOUTH);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Devolver / Trocar venda " + invoice.invoiceNumber(),
                "fas-undo", "Devolução por nota de crédito", panel).setConfirmButton("Confirmar", "fas-check").showDialog();
        if (!confirmed) {
            return;
        }

        List<CreateCreditNoteLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                BigDecimal qty = new BigDecimal(String.valueOf(linesModel.getValueAt(i, 3)).trim().replace(",", "."));
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(new CreateCreditNoteLineRequest((Long) linesModel.getValueAt(i, 0), qty));
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Quantidade inválida em alguma linha.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (lines.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Informe pelo menos uma quantidade a devolver.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String method = String.valueOf(methodCombo.getSelectedItem());
        Long accountId = null;
        if (!"CASH".equals(method) && !"CREDIT".equals(method)) {
            int accIdx = refundAccountCombo.getSelectedIndex();
            if (accIdx < 0 || accIdx >= owner.accountsList.size()) {
                JOptionPane.showMessageDialog(owner, "Selecione a conta de tesouraria para o reembolso.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            accountId = owner.accountsList.get(accIdx).id();
        }

        POSReturnRequest request = new POSReturnRequest(
                CurrentUserContext.getUsername(), CurrentUserContext.getCurrentCompanyId(), invoice.id(),
                owner.warehousesList.get(warehouseReturnCombo.getSelectedIndex()).id(),
                reasonField.getText().trim(), method, accountId, lines);
        UIHelper.runWithProgress(owner, "A registar devolução…", () -> owner.posApiClient.returnSale(request), note -> {
            JOptionPane.showMessageDialog(owner,
                    "Devolução registada com sucesso.\nNota de crédito: " + note.noteNumber()
                            + "\nTotal: " + note.totalAmount() + " MT",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            int exchange = JOptionPane.showConfirmDialog(owner,
                    "Pretende lançar agora a venda de troca/substituição?",
                    "Troca", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (exchange == JOptionPane.YES_OPTION) {
                owner.selectView(false);
            }
            owner.refreshSalesHistory();
            owner.refreshSessionState();
            owner.loadMetadata();
        }, error -> JOptionPane.showMessageDialog(owner,
                "Não foi possível registar a devolução: " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE));
    }
}
