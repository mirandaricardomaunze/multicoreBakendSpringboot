package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador das cessações e acertos finais. Ver docs/RH_COMPLETO_SPEC.md §B3.
 */
final class HRTerminationsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HRPanel owner;
    private DefaultTableModel model;
    private JTable table;
    private List<TerminationDTO> terminations = List.of();

    HRTerminationsPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createPrimaryButton("Cessar Vínculo");
        newBtn.setIcon(UIHelper.icon("fas-user-slash", 14));
        newBtn.addActionListener(e -> HrTerminationFormDialog.openTerminationDialog(owner, this::load));

        ModernButton payBtn = UIHelper.createSuccessButton("Pagar Acerto");
        payBtn.setIcon(UIHelper.icon("fas-money-check-alt", 14));
        payBtn.addActionListener(e -> HRTerminationsOperationsCoordinator.paySettlement(owner, selected(), this::load));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::load);

        ActionMenuButton docsBtn = UIHelper.createActionMenuButton("Documentos")
                .addAction("Acerto PDF", UIHelper.icon("fas-file-pdf", 14), () -> HRTerminationsOperationsCoordinator.printSettlement(owner, selected(), this::load))
                .addAction("Certificado de Trabalho", UIHelper.icon("fas-file-alt", 14), () -> HRTerminationsOperationsCoordinator.printCertificate(owner, selected(), this::load));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Acerto", "Colaborador", "Saída", "Motivo", "Aviso Prévio",
                "Ganhos (MT)", "Descontos (MT)", "Líquido (MT)", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        for (int col : new int[]{6, 7, 8}) table.getColumnModel().getColumn(col).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            HRTerminationsOperationsCoordinator.populatePeek(peek, model, row);
        });

        JTextField search = TableFilter.searchField("Colaborador ou acerto…");
        JComboBox<String> estado = TableFilter.combo("Todos os estados", "Por pagar", "Pago");
        JComboBox<String> periodo = TableFilter.periodCombo();
        TableFilter.install(table, search, List.of(new TableFilter.ColumnFilter(estado, 9)),
                List.of(new TableFilter.PeriodFilter(periodo, 3)));
        JPanel bar = TableFilter.bar(search, TableFilter.label("Estado:"), estado,
                TableFilter.label("Saída:", "fas-calendar-alt"), periodo);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(UIHelper.tableCardTop("Cessações e Acertos Finais", bar,
                refreshBtn, docsBtn, payBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getTerminations, this::apply,
                error -> owner.showLoadError("cessações", error));
    }

    private void apply(List<TerminationDTO> loaded) {
        terminations = loaded;
        model.setRowCount(0);
        for (TerminationDTO t : terminations) {
            model.addRow(new Object[]{
                    t.id(), t.settlementNumber(), t.employeeName(),
                    t.terminationDate() == null ? "-" : t.terminationDate().format(DATE_FMT),
                    t.reasonLabel(), t.noticeServed() ? "Cumprido" : "Não cumprido",
                    t.totalEarnings(), t.totalDeductions(), t.netAmount(), t.statusLabel()
            });
        }
    }

    private TerminationDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione uma cessação", "Escolha uma cessação na tabela para continuar.");
            return null;
        }
        return terminations.get(row);
    }
}
