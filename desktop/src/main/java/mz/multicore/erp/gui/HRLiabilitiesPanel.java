package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Separador das retenções por entregar ao Estado de Moçambique (IRPS e INSS)
 * e acompanhamento do custo total do trabalhador.
 */
final class HRLiabilitiesPanel {

    private final HRPanel owner;

    private DefaultTableModel model;
    private JTable table;
    private List<PayrollLiabilityDTO> liabilities = List.of();
    private JLabel costLabel;
    private JSpinner yearSpinner;
    private JSpinner monthSpinner;

    HRLiabilitiesPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> load());

        ActionMenuButton actionsBtn = UIHelper.createActionMenuButton("Ações")
                .addAction("Apurar Período", UIHelper.icon("fas-calculator", 14), this::accrue)
                .addAction("Valores Legais", UIHelper.icon("fas-balance-scale", 14), () -> HrPolicyConfigDialog.show(owner, this::load));

        ModernButton deliverBtn = UIHelper.createSuccessButton("Marcar Entregue");
        deliverBtn.setIcon(UIHelper.icon("fas-check-double", 14));
        deliverBtn.addActionListener(e -> deliver());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Período", "Retenção", "Valor (MT)", "Prazo", "Estado", "Entregue Por", "Referência"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            HRLiabilitiesOperationsCoordinator.populatePeek(peek, model, row);
        });

        JTextField search = TableFilter.searchField("Retenção ou período…");
        JComboBox<String> estado = TableFilter.combo("Todos os estados", "Por entregar", "Entregue");
        TableFilter.install(table, search, List.of(new TableFilter.ColumnFilter(estado, 5)), List.of());
        JPanel bar = TableFilter.bar(search, TableFilter.label("Estado:"), estado);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(UIHelper.tableCardTop("Retenções por Entregar", bar,
                refreshBtn, actionsBtn, deliverBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(buildCostBar(), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildCostBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 0, 0, 0));

        LocalDate today = LocalDate.now();
        yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
        monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));

        ModernButton costBtn = UIHelper.createSecondaryButton("Custo do Mês");
        costBtn.setIcon(UIHelper.icon("fas-hand-holding-usd", 14));
        costBtn.addActionListener(e -> loadCost());

        costLabel = new JLabel("Custo total do trabalhador: —");
        costLabel.setForeground(UIHelper.TEXT_LIGHT);

        bar.add(TableFilter.label("Ano:"));
        bar.add(yearSpinner);
        bar.add(TableFilter.label("Mês:"));
        bar.add(monthSpinner);
        bar.add(costBtn);
        bar.add(costLabel);
        return bar;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getPayrollLiabilities, this::apply,
                error -> owner.showLoadError("retenções da folha", error));
    }

    private void apply(List<PayrollLiabilityDTO> loaded) {
        liabilities = loaded;
        model.setRowCount(0);
        for (PayrollLiabilityDTO l : liabilities) {
            model.addRow(new Object[]{
                    l.id(),
                    String.format("%02d/%d", l.month(), l.year()),
                    l.liabilityTypeLabel(),
                    l.amount(),
                    HRLiabilitiesOperationsCoordinator.dueLabel(l),
                    l.statusLabel(),
                    l.deliveredBy() == null ? "-" : l.deliveredBy(),
                    l.paymentReference() == null ? "-" : l.paymentReference()
            });
        }
    }

    private void loadCost() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        UIHelper.runWithProgress(owner, "A apurar o custo do mês…",
                () -> owner.hrApiClient.getPayrollCost(year, month),
                cost -> costLabel.setText(HRLiabilitiesOperationsCoordinator.costText(cost)),
                owner::showActionError);
    }

    private void accrue() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        UIHelper.runWithProgress(owner, "A apurar retenções…",
                () -> owner.hrApiClient.accruePayrollLiabilities(year, month),
                ignored -> load(), owner::showActionError);
    }

    private void deliver() {
        HRLiabilitiesOperationsCoordinator.deliver(owner, table, liabilities, this::load);
    }
}
