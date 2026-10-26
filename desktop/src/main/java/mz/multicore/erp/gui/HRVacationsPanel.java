package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Separador de pedidos de férias.
 */
final class HRVacationsPanel {

    private final HRPanel owner;

    HRVacationsPanel(HRPanel owner) { this.owner = owner; }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createPrimaryButton("Novo Pedido");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openCreateVacationDialog());

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> owner.exportTable("ferias", "Mapa de Férias", owner.vacationsTable));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::load);

        ActionMenuButton decisionBtn = UIHelper.createActionMenuButton("Decisão & Subsídio")
                .addAction("Aprovar", UIHelper.icon("fas-check", 14), () -> decideVacation(true))
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14), () -> decideVacation(false))
                .addAction("Subsídio de Férias", UIHelper.icon("fas-money-check-alt", 14), this::openVacationAllowance);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Início", "Fim", "Dias", "Ano Ref.", "Estado", "Decidido Por"};
        owner.vacationsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(owner.vacationsModel);
        owner.vacationsTable = table;
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            HrVacationsViewCoordinator.populateQuickPeek(peek, row, owner);
        });

        JTextField vacSearch = TableFilter.searchField("Colaborador ou decisor…");
        JComboBox<String> vacEstado = TableFilter.combo("Todos os estados",
                "Pendente", "Aprovado", "Rejeitado", "Cancelado");
        JComboBox<String> vacPeriodo = TableFilter.periodCombo();
        TableFilter.install(owner.vacationsTable, vacSearch,
                List.of(new TableFilter.ColumnFilter(vacEstado, 6)),
                List.of(new TableFilter.PeriodFilter(vacPeriodo, 2)));
        JPanel vacBar = TableFilter.bar(vacSearch,
                TableFilter.label("Estado:"), vacEstado,
                TableFilter.label("Início:", "fas-calendar-alt"), vacPeriodo, refreshBtn);
        vacBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Pedidos de Férias", vacBar,
                exportBtn, decisionBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getAllVacations,
                loaded -> HrVacationsViewCoordinator.applyVacations(loaded, owner),
                error -> owner.showLoadError("férias", error));
    }

    private void openCreateVacationDialog() {
        HrVacationFormDialog.show(owner, this::load);
    }

    private void decideVacation(boolean approve) {
        HrVacationsViewCoordinator.decideVacation(approve, owner, this::load);
    }

    void openVacationAllowance() {
        HrVacationsViewCoordinator.openVacationAllowance(owner);
    }
}
