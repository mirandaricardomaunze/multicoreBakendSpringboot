package mz.multicore.erp.gui.performance;

import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.performance.dto.EmployeeRankingDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class RankingTab extends JPanel {

    private final PerformanceApiClient apiClient;
    private final DesktopSession session;

    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> periodFilter;
    private InlineFeedbackPanel feedbackPanel;


    public RankingTab(PerformanceApiClient apiClient, DesktopSession session) {
        this.apiClient = apiClient;
        this.session = session;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        buildUI();
    }

    private void buildUI() {
        feedbackPanel = new InlineFeedbackPanel();
        add(feedbackPanel, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Toolbar inside card
        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);

        JPanel filtersLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtersLeft.setOpaque(false);

        periodFilter = PerformanceControls.createSelect(
                new String[]{"Este Mês", "Últimos 7 Dias", "Últimos 30 Dias", "Este Ano"},
                PerformanceControls.FILTER_SELECT_WIDTH
        );
        periodFilter.addActionListener(e -> reload());
        filtersLeft.add(PerformanceControls.createFilterGroup("Período:", periodFilter));

        ModernButton btnRefresh = UIHelper.createSecondaryButton("Recarregar");
        btnRefresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnRefresh.addActionListener(e -> reload());

        ModernButton btnPrint = UIHelper.createPrimaryButton("Imprimir Relatório PDF");
        btnPrint.setIcon(UIHelper.icon("fas-print", 14));
        btnPrint.addActionListener(e -> printReport());

        JPanel actionsRight = UIHelper.actionsBar(btnRefresh, btnPrint);

        filterBar.add(filtersLeft, BorderLayout.WEST);
        filterBar.add(actionsRight, BorderLayout.EAST);

        card.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Posição", "Colaborador", "Receita Gerada (MZN)", "Margem Bruta (MZN)", "Ticket Médio (MZN)", "Nº Faturas", "% Meta"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        TableContextMenu.install(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void reload() {
        feedbackPanel.clear();
        Long companyId = session.activeCompanyId();
        if (companyId == null) return;

        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to = dates[1];

        UIHelper.loadAsync(
                this,
                () -> apiClient.getRanking(companyId, from, to),
                rankingList -> {
                    tableModel.setRowCount(0);
                    for (EmployeeRankingDTO r : rankingList) {
                        tableModel.addRow(new Object[]{
                                r.rank() + "º",
                                r.employeeName(),
                                UIHelper.formatMzn(r.revenue() != null ? r.revenue() : BigDecimal.ZERO),
                                UIHelper.formatMzn(r.grossMargin() != null ? r.grossMargin() : BigDecimal.ZERO),
                                UIHelper.formatMzn(r.avgTicket() != null ? r.avgTicket() : BigDecimal.ZERO),
                                r.invoiceCount(),
                                (r.goalProgressPct() != null ? r.goalProgressPct() : BigDecimal.ZERO) + "%"
                        });
                    }
                },
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao carregar ranking: " + ex.getMessage())
        );
    }

    private void printReport() {
        Long companyId = session.activeCompanyId();
        if (companyId == null) return;

        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to = dates[1];

        UIHelper.loadAsync(
                this,
                () -> apiClient.printReport(companyId, from, to),
                pdf -> PrintPreviewDialog.show(this, pdf, "desempenho-comercial-" + from + "-" + to),
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao gerar PDF: " + ex.getMessage())
        );
    }

    private LocalDate[] resolveDates() {
        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to = today;

        switch (periodFilter.getSelectedIndex()) {
            case 1 -> from = today.minusDays(7);
            case 2 -> from = today.minusDays(30);
            case 3 -> from = today.withDayOfYear(1);
            default -> from = today.withDayOfMonth(1);
        }
        return new LocalDate[]{from, to};
    }
}
