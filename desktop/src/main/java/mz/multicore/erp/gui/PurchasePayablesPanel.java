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
    private JComboBox<String> businessUnitCombo;
    PurchasePayablesPanel(ComprasPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        ModernButton payBtn = UIHelper.createSuccessButton("Registar Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> openSupplierPaymentDialog());

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refresh());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Nº Compra", "Negócio", "Fornecedor", "Total", "Pago", "Em Dívida", "Data"};
        owner.payablesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.payablesTable = new JTable(owner.payablesModel);
        UIHelper.styleTable(owner.payablesTable);
        for (int column : new int[]{3, 4, 5}) {
            owner.payablesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        UIHelper.ensureHeadersFit(owner.payablesTable);
        JScrollPane scroll = new JScrollPane(owner.payablesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField paySearch = TableFilter.searchField("Nº compra ou fornecedor…");
        businessUnitCombo = TableFilter.combo("Todos os negócios");
        businessUnitCombo.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(businessUnitCombo);
        JComboBox<String> payPeriodo = TableFilter.periodCombo();

        TableFilter.install(owner.payablesTable, paySearch,
                java.util.List.of(new TableFilter.ColumnFilter(businessUnitCombo, 1)),
                java.util.List.of(new TableFilter.PeriodFilter(payPeriodo, 6)));

        JPanel payFilters = TableFilter.bar(
                paySearch,
                TableFilter.label("Negócio:"), businessUnitCombo,
                TableFilter.label("Período:"), payPeriodo);

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da conta a pagar (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        filtersRow.add(payFilters, BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        card.add(UIHelper.tableCardTop("Contas a Pagar a Fornecedores", filtersRow,
                refreshBtn, payBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        payablesFooter = new JLabel(" ");
        payablesFooter.setForeground(UIHelper.TEXT_MUTED);
        payablesFooter.setBorder(new EmptyBorder(8, 4, 0, 4));
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(ClientTablePagination.install(owner.payablesTable), BorderLayout.NORTH);
        southPanel.add(payablesFooter, BorderLayout.SOUTH);
        card.add(southPanel, BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(owner.payablesTable, card, this::populatePeek);
        peekToggleBtn.addActionListener(e -> peek.toggle());

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void populatePeek(QuickPeekPanel peek, int modelRow) {
        if (owner.payablesList == null || modelRow < 0 || modelRow >= owner.payablesList.size()) return;
        var pa = owner.payablesList.get(modelRow);
        boolean inDebt = pa.outstanding() != null && pa.outstanding().signum() > 0;
        peek.setHeaderIcon("fas-file-invoice-dollar", UIHelper.REJECTED_RED);
        peek.setTitle("Compra " + (pa.purchaseNumber() != null ? pa.purchaseNumber() : ""));
        peek.setSubtitle(pa.supplierName() != null ? pa.supplierName() : "—");
        peek.setStatus(inDebt ? "EM DÍVIDA" : "LIQUIDADO", inDebt ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.util.List<QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", pa.businessUnitName() != null ? pa.businessUnitName() : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Fornecedor", pa.supplierName() != null ? pa.supplierName() : "-", false));
        BigDecimal total = pa.totalAmount() != null ? pa.totalAmount() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Total da Compra", String.format("%,.2f MT", total), false));
        BigDecimal paid = pa.amountPaid() != null ? pa.amountPaid() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Total Já Pago", String.format("%,.2f MT", paid), false));
        items.add(new QuickPeekPanel.PeekItem("Data da Compra", pa.purchaseDate() != null ? pa.purchaseDate().format(dtf) : "-", false));
        BigDecimal debt = pa.outstanding() != null ? pa.outstanding() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Valor em Dívida", String.format("%,.2f MT", debt), true));
        peek.setItems(items);

        if (inDebt) {
            peek.getOpenFullButton().setText("Registar Pagamento");
            peek.setOnOpenFullAction(ignored -> openSupplierPaymentDialog());
        } else {
            peek.setOnOpenFullAction(null);
        }
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
        java.util.Set<String> bus = new java.util.TreeSet<>();
        for (var pa : owner.payablesList) {
            String bu = (pa.businessUnitName() != null && !pa.businessUnitName().isBlank()) ? pa.businessUnitName() : "Geral";
            bus.add(bu);
            totalDivida = totalDivida.add(pa.outstanding());
            owner.payablesModel.addRow(new Object[]{
                    pa.purchaseNumber(),
                    bu,
                    pa.supplierName(),
                    pa.totalAmount(), pa.amountPaid(), pa.outstanding(),
                    pa.purchaseDate() == null ? "-" : pa.purchaseDate().format(dtf)});
        }
        Object sel = businessUnitCombo.getSelectedItem();
        businessUnitCombo.removeAllItems();
        businessUnitCombo.addItem("Todos os negócios");
        for (String b : bus) businessUnitCombo.addItem(b);
        if (sel != null) businessUnitCombo.setSelectedItem(sel);
        payablesFooter.setText(String.format("%d fatura(s) em dívida · Total a pagar: %,.2f MT",
                owner.payablesList.size(), totalDivida));
    }

    private void openSupplierPaymentDialog() {
        int row = TableFilter.selectedModelRow(owner.payablesTable);
        if (row < 0 || row >= owner.payablesList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione uma conta a pagar", "Escolha uma conta na tabela para continuar.");
            return;
        }
        var pa = owner.payablesList.get(row);
        if (owner.accountsList.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Sem contas de tesouraria", "Registe uma conta de tesouraria antes do pagamento.");
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
                owner.showPurchaseSuccess("Pagamento registado.");
                refresh();
                owner.loadPurchasesHistory();
                owner.loadAccounts();
            }, owner::showPurchaseError);
        } catch (IllegalArgumentException ex) {
            owner.showPurchaseNotice(FeedbackType.ERROR, "Dados de pagamento inválidos", ex.getMessage());
        } catch (Exception ex) {
            owner.showPurchaseNotice(FeedbackType.ERROR, "Não foi possível registar o pagamento", ex.getMessage());
        }
    }

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
