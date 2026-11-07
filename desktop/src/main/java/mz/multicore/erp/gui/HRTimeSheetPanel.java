package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Separador de ponto e assiduidade. Ver docs/RH_COMPLETO_SPEC.md §B2.
 */
final class HRTimeSheetPanel {

    private final HRPanel owner;
    private DefaultTableModel model;
    private JTable table;
    private JSpinner yearSpinner, monthSpinner;
    private JLabel statusLabel;
    private ModernButton closeBtn;
    private List<TimeSheetDTO.TimeSheetLineDTO> lines = List.of();

    HRTimeSheetPanel(HRPanel owner) { this.owner = owner; }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        LocalDate today = LocalDate.now();
        yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        yearSpinner.addChangeListener(e -> load());
        monthSpinner.addChangeListener(e -> load());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIHelper.TEXT_LIGHT);

        JPanel periodFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodFilters.setOpaque(false);
        periodFilters.add(new JLabel("Ano:"));
        periodFilters.add(yearSpinner);
        periodFilters.add(new JLabel("Mês:"));
        periodFilters.add(monthSpinner);
        periodFilters.add(statusLabel);

        ModernButton entryBtn = UIHelper.createSuccessButton("Registar Marcação");
        entryBtn.setIcon(UIHelper.icon("fas-clock", 14));
        entryBtn.addActionListener(e -> HrTimeEntryDialog.show(owner, this::load));

        closeBtn = UIHelper.createSecondaryButton("Fechar Mês");
        closeBtn.setIcon(UIHelper.icon("fas-lock", 14));
        closeBtn.addActionListener(e -> toggleClose());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::load);

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Folha de Ponto")
                .addAction("Configurar Acréscimos", UIHelper.icon("fas-percentage", 14), this::openOvertimeRatesDialog)
                .addAction("Exportar PDF", UIHelper.icon("fas-file-pdf", 14),
                        () -> owner.exportTable("folha-ponto", "Folha de Ponto", table));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Colaborador", "Dias prev.", "Dias trab.", "Sem marcação",
                "H. normais", "Extra dia", "Extra noite", "Descanso/feriado", "Atrasos"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            peek.setHeaderIcon("fas-clock", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(model.getValueAt(row, 0)));
            peek.setSubtitle("Assiduidade do Mês");
            peek.setStatus(String.valueOf(model.getValueAt(row, 2)) + " dias", UIHelper.APPROVED_GREEN);
            peek.setItems(List.of(
                    new QuickPeekPanel.PeekItem("Dias Previstos", peek.cell(model.getValueAt(row, 1), 1), false),
                    new QuickPeekPanel.PeekItem("Dias Trabalhados", peek.cell(model.getValueAt(row, 2), 2), false),
                    new QuickPeekPanel.PeekItem("Sem Marcação", peek.cell(model.getValueAt(row, 3), 3), false),
                    new QuickPeekPanel.PeekItem("Horas Normais", peek.cell(model.getValueAt(row, 4), 4), false),
                    new QuickPeekPanel.PeekItem("Horas Extras Dia", peek.cell(model.getValueAt(row, 5), 5), false),
                    new QuickPeekPanel.PeekItem("Horas Extras Noite", peek.cell(model.getValueAt(row, 6), 6), false),
                    new QuickPeekPanel.PeekItem("Descanso / Feriado", peek.cell(model.getValueAt(row, 7), 7), false),
                    new QuickPeekPanel.PeekItem("Atrasos", peek.cell(model.getValueAt(row, 8), 8), true)));
        });

        JTextField search = TableFilter.searchField("Colaborador…");
        TableFilter.install(table, search);
        JPanel bar = TableFilter.bar(search, refreshBtn);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));
        JPanel filters = new JPanel(new BorderLayout(0, 8));
        filters.setOpaque(false);
        filters.add(periodFilters, BorderLayout.NORTH);
        filters.add(bar, BorderLayout.CENTER);
        card.add(UIHelper.tableCardTop("Folha de Ponto", filters,
                moreBtn, closeBtn, entryBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);

        load();
        return tab;
    }

    void load() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        UIHelper.loadAsync(owner, () -> owner.hrApiClient.getMonthlySheet(year, month), this::apply,
                error -> owner.showLoadError("folha de ponto", error));
    }

    private void apply(TimeSheetDTO sheet) {
        lines = sheet.lines();
        model.setRowCount(0);
        for (TimeSheetDTO.TimeSheetLineDTO line : lines) {
            model.addRow(new Object[]{
                    line.employeeName(), line.expectedDays(), line.workedDays(), line.missingDays(),
                    line.normalHours(), line.overtimeDayHours(), line.overtimeNightHours(),
                    line.restDayHours(), line.lateArrivals()});
        }
        statusLabel.setText("  •  " + sheet.statusLabel()
                + (sheet.closedBy() == null ? "" : " por " + sheet.closedBy()));
        closeBtn.setText(sheet.closed() ? "Reabrir Mês" : "Fechar Mês");
        closeBtn.setIcon(UIHelper.icon(sheet.closed() ? "fas-lock-open" : "fas-lock", 14));
    }

    private void toggleClose() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        boolean closed = closeBtn.getText().startsWith("Reabrir");

        if (closed) {
            String reason = UIHelper.promptRequiredText("Reabrir Folha de Ponto", "fas-lock-open",
                    "Mês " + month + "/" + year, "Motivo da reabertura:");
            if (reason == null) return;
            UIHelper.runWithProgress(owner, "A reabrir folha de ponto…",
                    () -> owner.hrApiClient.reopenTimeSheet(year, month, reason),
                    this::apply, owner::showActionError);
            return;
        }
        boolean confirmed = UIHelper.confirm(owner,
                "Fechar a folha de ponto de " + month + "/" + year + "?\n"
                        + "Depois de fechada só se altera com reabertura justificada.",
                "Fechar Mês");
        if (!confirmed) return;
        UIHelper.runWithProgress(owner, "A fechar folha de ponto…",
                () -> owner.hrApiClient.closeTimeSheet(year, month), this::apply, owner::showActionError);
    }

    void openOvertimeRatesDialog() {
        HrOvertimeRatesDialog.show(owner);
    }
}
