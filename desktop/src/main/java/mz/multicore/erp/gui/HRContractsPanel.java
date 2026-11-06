package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador de contratos de trabalho. Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
final class HRContractsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HRPanel owner;
    private DefaultTableModel model;
    private JTable table;
    private List<EmploymentContractDTO> loaded = List.of();

    HRContractsPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        printBtn.addActionListener(e -> withSelection(this::printContract));

        ActionMenuButton manageBtn = UIHelper.createActionMenuButton("Gestão do Contrato")
                .addAction("Activar", UIHelper.icon("fas-check", 14), () -> withSelection(this::activateContract))
                .addAction("Renovar", UIHelper.icon("fas-redo", 14), () -> withSelection(this::renewContract))
                .addAction("Cessar", UIHelper.icon("fas-ban", 14), () -> withSelection(this::terminateContract));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::load);

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Contrato");
        newBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newBtn.addActionListener(e -> HrContractFormDialog.showCreateDialog(owner, this::load));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº", "Colaborador", "Tipo", "Função", "Início", "Fim", "Salário", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            HRContractsOperationsCoordinator.populatePeek(peek, model, row);
        });

        JTextField search = TableFilter.searchField("Nº, colaborador ou função…");
        JComboBox<String> statusFilter = TableFilter.combo("Todos os estados",
                "Rascunho", "Vigente", "Cessado", "Prazo terminado");
        TableFilter.install(table, search, new TableFilter.ColumnFilter(statusFilter, 7));

        JPanel bar = TableFilter.bar(search, TableFilter.label("Estado:", "fas-filter"), statusFilter);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(UIHelper.tableCardTop("Contratos de Trabalho", bar,
                refreshBtn, manageBtn, printBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);

        load();
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getAllContracts, this::apply,
                error -> owner.showLoadError("contratos", error));
    }

    private void apply(List<EmploymentContractDTO> contracts) {
        loaded = contracts;
        model.setRowCount(0);
        for (EmploymentContractDTO c : contracts) {
            model.addRow(new Object[]{
                    c.contractNumber(),
                    c.employeeName(),
                    c.contractTypeLabel(),
                    c.jobTitle(),
                    c.startDate() == null ? "" : c.startDate().format(DATE_FMT),
                    c.endDate() == null ? "Sem termo" : c.endDate().format(DATE_FMT),
                    c.agreedSalary(),
                    c.expired() ? "Prazo terminado" : c.statusLabel()
            });
        }
        UIHelper.ensureHeadersFit(table);
    }

    private void withSelection(java.util.function.Consumer<EmploymentContractDTO> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione um contrato", "Escolha um contrato na tabela para continuar.");
            return;
        }
        action.accept(loaded.get(table.convertRowIndexToModel(row)));
    }

    private void printContract(EmploymentContractDTO contract) {
        HRContractsOperationsCoordinator.printContract(owner, contract, this::load);
    }

    private void activateContract(EmploymentContractDTO contract) {
        HRContractsOperationsCoordinator.activateContract(owner, contract, this::load);
    }

    private void renewContract(EmploymentContractDTO contract) {
        HrContractFormDialog.showRenewDialog(owner, contract, this::load);
    }

    private void terminateContract(EmploymentContractDTO contract) {
        HrContractFormDialog.showTerminateDialog(owner, contract, this::load);
    }
}
