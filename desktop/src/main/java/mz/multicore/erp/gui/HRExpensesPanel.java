package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/** Submissão e listagem de despesas de colaboradores. */
final class HRExpensesPanel {
    private final HRPanel owner;
    HRExpensesPanel(HRPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> owner.exportTable("despesas", "Notas de Despesas", owner.expensesTable));
        ModernButton submitBtn = UIHelper.createPrimaryButton("Submeter Despesa");
        submitBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        submitBtn.addActionListener(e -> submitExpense());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Colaborador", "Valor", "Categoria", "Estado", "Motivo Rejeição"};
        owner.expensesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(owner.expensesModel);
        owner.expensesTable = table;
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            HrExpensesViewCoordinator.populateQuickPeek(peek, row, owner);
        });

        JTextField expSearch = TableFilter.searchField("Colaborador, categoria ou motivo…");
        JComboBox<String> expEstado = TableFilter.combo("Todos os estados",
                "Pendente de aprovação", "Aprovado", "Rejeitado");
        TableFilter.install(owner.expensesTable, expSearch,
                new TableFilter.ColumnFilter(expEstado, 3));
        JPanel expBar = TableFilter.bar(expSearch, TableFilter.label("Estado:"), expEstado);
        expBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        card.add(UIHelper.tableCardTop("Notas de Despesas", expBar,
                refreshBtn, exportBtn, submitBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getAllExpenses,
                loaded -> HrExpensesViewCoordinator.applyExpenses(loaded, owner),
                error -> owner.showLoadError("notas de despesas", error));
    }

    private void submitExpense() {
        HrExpenseClaimDialog.show(owner, this::refresh);
    }
}
