package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ExceptionsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;
import mz.multicore.erp.modules.exceptions.dto.ExceptionReportDTO;
import mz.multicore.erp.modules.exceptions.model.ExceptionSeverity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excepções (SA-C21, {@code docs/ANTIFRAUDE_SECTORES_SPEC.md}): what the manager should check, most serious first —
 * breakdowns that lost too much, leftovers, guides never invoiced, typed weights, cancellations and discounts by
 * operator, cash differences. Selecting a line shows the full explanation; the period prints to PDF.
 */
public class ExceptionsPanel extends JPanel {

    private static final String[] COLUMNS = {"Gravidade", "Sector", "Controlo", "Operador", "Data", "Referência"};
    private static final String[][] PERIODS = {{"Últimos 7 dias", "7"}, {"Últimos 30 dias", "30"}, {"Últimos 90 dias", "90"}};
    static final String HIGH = "ALTA";
    static final String MEDIUM = "MÉDIA";
    static final String LOW = "BAIXA";

    private final ExceptionsApiClient api;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> period = new JComboBox<>(java.util.Arrays.stream(PERIODS).map(p -> p[0]).toArray(String[]::new));
    private final JTextArea detail = ResponsiveSupport.wrappingText("Escolha uma excepção para ver o que aconteceu.",
            new Font(UIHelper.FONT, Font.PLAIN, 13), UIHelper.TEXT_LIGHT);
    private final List<ExceptionFindingDTO> findings = new ArrayList<>();
    private Consumer<ExceptionReportDTO> onLoaded = report -> { };

    public ExceptionsPanel(ExceptionsApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
        add(buildDetail(), BorderLayout.SOUTH);
    }

    public void onLoaded(Consumer<ExceptionReportDTO> listener) {
        this.onLoaded = listener == null ? report -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setCellRenderer(new SeverityRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        UIHelper.styleComboBox(period);
        period.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        period.addActionListener(e -> reload());
        JTextField search = TableFilter.searchField("Operador, controlo ou referência…");
        search.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> severity = TableFilter.combo("Todas", HIGH, MEDIUM, LOW);
        UIHelper.styleComboBox(severity);
        severity.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> sector = TableFilter.combo("Todos os sectores", "Geral", "Talho", "Padaria", "Estaleiro");
        UIHelper.styleComboBox(sector);
        sector.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search, new TableFilter.ColumnFilter(severity, 0), new TableFilter.ColumnFilter(sector, 1));
        JPanel filters = TableFilter.bar(TableFilter.label("Período:"), period, search, TableFilter.label("Gravidade:"), severity, sector);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton pdfBtn = UIHelper.createSecondaryButton("Exportar PDF");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        pdfBtn.addActionListener(e -> exportPdf());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Relatório de Excepções", filters, UIHelper.createRefreshButton(this::reload), pdfBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDetail() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.add(new JLabel(UIHelper.icon("fas-search", 18, UIHelper.ACCENT_BLUE)), BorderLayout.WEST);
        card.add(detail, BorderLayout.CENTER);
        return card;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Integer.parseInt(PERIODS[Math.max(0, period.getSelectedIndex())][1]));
        UIHelper.loadAsync(this, () -> api.report(companyId, from, to), this::apply,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o relatório", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(ExceptionReportDTO report) {
        feedback.clear();
        findings.clear();
        findings.addAll(report.findings());
        model.setRowCount(0);
        for (ExceptionFindingDTO f : findings) {
            model.addRow(new Object[]{label(f.severity()), f.sector(), f.title(), f.operator() == null ? "—" : f.operator(),
                    f.date() == null ? "—" : UIHelper.formatDate(f.date()), f.reference() == null ? "—" : f.reference()});
        }
        if (findings.isEmpty()) detail.setText("Nenhuma excepção no período. Os controlos estão em docs/ANTIFRAUDE_SECTORES_SPEC.md.");
        else showDetail();
        onLoaded.accept(report);
    }

    private void showDetail() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= findings.size()) {
            if (!findings.isEmpty()) detail.setText("Escolha uma excepção para ver o que aconteceu.");
            return;
        }
        ExceptionFindingDTO f = findings.get(row);
        detail.setText(f.title() + ": " + f.detail() + (f.reference() == null ? "" : "  Documento: " + f.reference() + ".")
                + "  Uma excepção não prova fraude: indica o que deve ser verificado.");
    }

    private void exportPdf() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Integer.parseInt(PERIODS[Math.max(0, period.getSelectedIndex())][1]));
        UIHelper.runWithProgress(this, "A gerar o relatório de excepções…", () -> api.pdf(companyId, from, to),
                pdf -> PrintPreviewDialog.show(this, pdf, "excepcoes-" + from),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o PDF", error.getMessage(), null, null));
    }

    static String label(ExceptionSeverity severity) {
        return switch (severity) {
            case HIGH -> HIGH;
            case MEDIUM -> MEDIUM;
            case LOW -> LOW;
        };
    }

    /** Red for high, amber for medium, muted for low. */
    private static final class SeverityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            String text = value == null ? "" : value.toString();
            Color color = HIGH.equals(text) ? UIHelper.REJECTED_RED : MEDIUM.equals(text) ? UIHelper.PENDING_YELLOW : UIHelper.TEXT_MUTED;
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            if (!selected) label.setForeground(color);
            label.setIcon(UIHelper.icon("fas-circle", 8, color));
            label.setIconTextGap(6);
            return label;
        }
    }
}
