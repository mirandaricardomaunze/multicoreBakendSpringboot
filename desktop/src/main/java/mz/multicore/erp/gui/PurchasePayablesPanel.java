package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Contas a pagar e liquidação de fornecedores. */
final class PurchasePayablesPanel {
    private final ComprasPanel owner;
    private JLabel payablesFooter;
    PurchasePayablesPanel(ComprasPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Contas a Pagar a Fornecedores"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton payBtn = UIHelper.createSuccessButton("Registar Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> openSupplierPaymentDialog());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refresh());
        actions.add(refreshBtn); actions.add(payBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Nº Compra", "Fornecedor", "Total", "Pago", "Em Dívida", "Data"};
        owner.payablesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.payablesTable = new JTable(owner.payablesModel);
        UIHelper.styleTable(owner.payablesTable);
        for (int column : new int[]{2, 3, 4}) {
            owner.payablesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        JScrollPane scroll = new JScrollPane(owner.payablesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField paySearch = TableFilter.searchField("Nº compra ou fornecedor…");
        JComboBox<String> payPeriodo = TableFilter.periodCombo();
        TableFilter.install(owner.payablesTable, paySearch,
                java.util.List.of(),
                java.util.List.of(new TableFilter.PeriodFilter(payPeriodo, 5)));
        JPanel payBar = TableFilter.bar(paySearch,
                TableFilter.label("Data:", "fas-calendar-alt"), payPeriodo);
        payBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(payBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        payablesFooter = new JLabel(" ");
        payablesFooter.setForeground(UIHelper.TEXT_MUTED);
        payablesFooter.setBorder(new EmptyBorder(8, 4, 0, 4));
        card.add(payablesFooter, BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.payablesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.findPayablesByCompany(companyId), this::applyPayables,
                error -> owner.showPurchaseLoadError("contas a pagar", error));
    }

    private void applyPayables(java.util.List<mz.multicore.erp.modules.purchases.dto.PayableDTO> loaded) {
        owner.payablesList = loaded;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        BigDecimal totalDivida = BigDecimal.ZERO;
        owner.payablesModel.setRowCount(0);
        for (var pa : owner.payablesList) {
            totalDivida = totalDivida.add(pa.outstanding());
            owner.payablesModel.addRow(new Object[]{
                    pa.purchaseNumber(), pa.supplierName(),
                    pa.totalAmount(), pa.amountPaid(), pa.outstanding(),
                    pa.purchaseDate() == null ? "-" : pa.purchaseDate().format(dtf)});
        }
        payablesFooter.setText(String.format("%d fatura(s) em dívida · Total a pagar: %,.2f MT",
                owner.payablesList.size(), totalDivida));
    }

    private void openSupplierPaymentDialog() {
        int row = TableFilter.selectedModelRow(owner.payablesTable);
        if (row < 0 || row >= owner.payablesList.size()) {
            JOptionPane.showMessageDialog(owner, "Selecione uma conta a pagar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var pa = owner.payablesList.get(row);
        if (owner.accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Falta registar contas de tesouraria.", "Erro", JOptionPane.ERROR_MESSAGE);
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
                "Pagar a Fornecedor — " + pa.purchaseNumber(), "fas-money-bill-wave", "Liquidação de compra a crédito", form)
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
                JOptionPane.showMessageDialog(owner, "Pagamento registado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                refresh();
                owner.loadPurchasesHistory();
                owner.loadAccounts();
            }, owner::showPurchaseError);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

}
