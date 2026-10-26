package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
                List.of(new TableFilter.ColumnFilter(businessUnitCombo, 1)),
                List.of(new TableFilter.PeriodFilter(payPeriodo, 6)));

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

        TableQuickPeekController peek = TableQuickPeekController.install(owner.payablesTable, card,
                (p, mr) -> PurchasePayablesViewCoordinator.populatePeek(p, mr, owner, this::openSupplierPaymentDialog));
        peekToggleBtn.addActionListener(e -> peek.toggle());

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.payablesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.findPayablesByCompany(companyId),
                loaded -> PurchasePayablesViewCoordinator.applyPayables(loaded, owner, businessUnitCombo, payablesFooter),
                error -> owner.showPurchaseLoadError("contas a pagar", error));
    }

    private void openSupplierPaymentDialog() {
        SupplierPaymentDialog.show(owner, this::refresh);
    }
}
