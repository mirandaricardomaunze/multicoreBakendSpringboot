package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Separador dos descontos, adiantamentos e empréstimos da folha de pagamentos de RH.
 */
final class HRDeductionsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HRPanel owner;

    private DefaultTableModel model;
    private JTable table;
    private List<PayrollDeductionDTO> deductions = List.of();

    HRDeductionsPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton stopBtn = UIHelper.createDangerButton("Desactivar");
        stopBtn.setIcon(UIHelper.icon("fas-ban", 14));
        stopBtn.addActionListener(e -> deactivate());

        ActionMenuButton newDeductionBtn = UIHelper.createActionMenuButton("Novo Desconto")
                .addAction("Adiantamento Salarial", UIHelper.icon("fas-hand-holding-usd", 14), () -> HrDeductionFormDialog.open(owner, "ADIANTAMENTO", this::load))
                .addAction("Empréstimo", UIHelper.icon("fas-file-invoice-dollar", 14), () -> HrDeductionFormDialog.open(owner, "EMPRESTIMO", this::load))
                .addAction("Desconto Recorrente", UIHelper.icon("fas-redo", 14), () -> HrDeductionFormDialog.open(owner, "RECORRENTE", this::load));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Tipo", "Descrição", "Capital (MT)", "Prestação (MT)",
                "Descontado (MT)", "Em Dívida (MT)", "Início", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        for (int column : new int[]{4, 5, 6, 7}) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-hand-holding-usd", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(model.getValueAt(row, 1)));
            peek.setSubtitle(String.valueOf(model.getValueAt(row, 2)) + " • " + model.getValueAt(row, 3));
            peek.setStatus(String.valueOf(model.getValueAt(row, 9)), UIHelper.ACCENT_BLUE);
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new QuickPeekPanel.PeekItem("Capital Inicial", String.valueOf(model.getValueAt(row, 4)), false));
            items.add(new QuickPeekPanel.PeekItem("Prestação Mensal", String.valueOf(model.getValueAt(row, 5)), false));
            items.add(new QuickPeekPanel.PeekItem("Total Descontado", String.valueOf(model.getValueAt(row, 6)), false));
            items.add(new QuickPeekPanel.PeekItem("Dívida Restante", String.valueOf(model.getValueAt(row, 7)), true));
            items.add(new QuickPeekPanel.PeekItem("Data Início", String.valueOf(model.getValueAt(row, 8)), false));
            peek.setItems(items);
        });

        JTextField search = TableFilter.searchField("Colaborador ou descrição…");
        JComboBox<String> tipo = TableFilter.combo("Todos os tipos", "Adiantamento", "Empréstimo", "Desconto recorrente");
        JComboBox<String> estado = TableFilter.combo("Todos os estados", "Activo", "Liquidado", "Desactivado");
        TableFilter.install(table, search, List.of(new TableFilter.ColumnFilter(tipo, 2), new TableFilter.ColumnFilter(estado, 9)), List.of());
        JPanel bar = TableFilter.bar(search, TableFilter.label("Tipo:"), tipo, TableFilter.label("Estado:"), estado);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::load);
        card.add(UIHelper.tableCardTop("Descontos, Adiantamentos e Empréstimos", bar,
                refreshBtn, stopBtn, newDeductionBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getDeductions, this::apply,
                error -> owner.showLoadError("descontos", error));
    }

    private void apply(List<PayrollDeductionDTO> loaded) {
        deductions = loaded;
        model.setRowCount(0);
        for (PayrollDeductionDTO d : deductions) {
            model.addRow(new Object[]{
                    d.id(), d.employeeName(), d.kindLabel(), d.description(),
                    d.principalAmount(), d.installmentAmount(),
                    d.appliedAmount(), d.outstandingAmount(),
                    d.startDate() == null ? "-" : d.startDate().format(DATE_FMT),
                    statusLabel(d)
            });
        }
    }

    private String statusLabel(PayrollDeductionDTO d) {
        if (!d.active()) {
            return "Desactivado";
        }
        return d.settled() ? "Liquidado" : "Activo";
    }

    private void deactivate() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione um desconto", "Selecione um desconto na tabela.");
            return;
        }
        PayrollDeductionDTO sel = deductions.get(row);
        BigDecimal outstanding = sel.outstandingAmount() == null ? BigDecimal.ZERO : sel.outstandingAmount();
        String msg = String.format(
                "Desactivar \"%s\" de %s?%s",
                sel.description(), sel.employeeName(),
                outstanding.signum() > 0
                        ? String.format("%n%nFicam %,.2f MT por liquidar, que deixam de ser descontados.", outstanding)
                        : "");
        if (!ModernMessageDialog.confirm(UIHelper.mainWindow, FeedbackType.WARNING, "Desactivar Desconto", msg, "Desactivar")) {
            return;
        }
        UIHelper.runWithProgress(owner, "A desactivar…", () -> {
            owner.hrApiClient.deactivateDeduction(sel.id());
            return null;
        }, ignored -> load(), owner::showActionError);
    }
}
