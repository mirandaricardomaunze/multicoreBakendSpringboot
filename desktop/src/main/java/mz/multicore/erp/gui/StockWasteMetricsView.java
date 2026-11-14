package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.WasteSummaryDTO;
import mz.multicore.erp.modules.inventory.model.WasteReason;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Vista da aba "Métricas & Relatório Executivo" de quebras de stock.
 */
public class StockWasteMetricsView extends JPanel {

    private final StockWasteApiClient wasteApiClient;
    private final Consumer<WasteSummaryDTO> onSummaryLoaded;
    private final JTextField dateStartField;
    private final JTextField dateEndField;
    private final DefaultTableModel reasonMetricsModel;
    private final DefaultTableModel categoryMetricsModel;

    public StockWasteMetricsView(StockWasteApiClient wasteApiClient, Consumer<WasteSummaryDTO> onSummaryLoaded) {
        super(new BorderLayout(0, 10));
        this.wasteApiClient = wasteApiClient;
        this.onSummaryLoaded = onSummaryLoaded;
        setOpaque(false);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        LocalDate now = LocalDate.now();
        dateStartField = new JTextField(now.minusDays(30).format(UIHelper.DATE_FMT));
        UIHelper.styleTextField(dateStartField);
        dateStartField.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        dateEndField = new JTextField(now.format(UIHelper.DATE_FMT));
        UIHelper.styleTextField(dateEndField);
        dateEndField.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton filterBtn = UIHelper.createPrimaryButton("Filtrar Período");
        filterBtn.setIcon(UIHelper.icon("fas-filter", 14));
        filterBtn.addActionListener(e -> reloadMetrics());

        ModernButton printPdfBtn = UIHelper.createSecondaryButton("Imprimir Relatório Oficial PDF");
        printPdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        printPdfBtn.addActionListener(e -> exportPdfReport());

        ModernPanel filterCard = new ModernPanel(16);
        filterCard.setLayout(new BorderLayout());
        filterCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel filterToolbar = UIHelper.tableCardTop("Métricas & Relatório Executivo",
                TableFilter.bar(TableFilter.label("Data Início:", "fas-calendar-alt"), dateStartField,
                        TableFilter.label("Data Fim:", "fas-calendar-alt"), dateEndField),
                filterBtn, printPdfBtn);
        filterCard.add(filterToolbar, BorderLayout.CENTER);
        add(filterCard, BorderLayout.NORTH);

        JPanel tablesGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        tablesGrid.setOpaque(false);

        ModernPanel reasonCard = new ModernPanel(16);
        reasonCard.setLayout(new BorderLayout(0, 8));
        reasonCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        reasonCard.add(UIHelper.createSubheading("Perdas por Motivo"), BorderLayout.NORTH);
        reasonMetricsModel = new DefaultTableModel(new String[]{"Motivo de Quebra", "Custo Total (MT)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable reasonTable = new JTable(reasonMetricsModel);
        UIHelper.styleTable(reasonTable);
        JScrollPane reasonScroll = new JScrollPane(reasonTable);
        UIHelper.styleScrollPane(reasonScroll);
        reasonCard.add(reasonScroll, BorderLayout.CENTER);
        tablesGrid.add(reasonCard);

        ModernPanel catCard = new ModernPanel(16);
        catCard.setLayout(new BorderLayout(0, 8));
        catCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        catCard.add(UIHelper.createSubheading("Perdas por Categoria de Produto"), BorderLayout.NORTH);
        categoryMetricsModel = new DefaultTableModel(new String[]{"Categoria", "Custo Total (MT)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable catTable = new JTable(categoryMetricsModel);
        UIHelper.styleTable(catTable);
        JScrollPane catScroll = new JScrollPane(catTable);
        UIHelper.styleScrollPane(catScroll);
        catCard.add(catScroll, BorderLayout.CENTER);
        tablesGrid.add(catCard);

        add(tablesGrid, BorderLayout.CENTER);
    }

    public void reloadMetrics() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        LocalDate start = parseDate(dateStartField.getText().trim(), LocalDate.now().minusDays(30));
        LocalDate end = parseDate(dateEndField.getText().trim(), LocalDate.now());

        new SwingWorker<WasteSummaryDTO, Void>() {
            @Override
            protected WasteSummaryDTO doInBackground() {
                return wasteApiClient.getSummary(companyId, start, end);
            }

            @Override
            protected void done() {
                try {
                    WasteSummaryDTO s = get();
                    applySummary(s);
                    if (onSummaryLoaded != null) onSummaryLoaded.accept(s);
                } catch (Exception ex) {
                    ToastManager.show(StockWasteMetricsView.this, FeedbackType.ERROR, "Erro ao carregar métricas: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public void applySummary(WasteSummaryDTO s) {
        if (s == null) return;
        reasonMetricsModel.setRowCount(0);
        if (s.costByReason() != null) {
            for (Map.Entry<WasteReason, BigDecimal> entry : s.costByReason().entrySet()) {
                reasonMetricsModel.addRow(new Object[]{entry.getKey().getDescription(), UIHelper.formatMzn(entry.getValue())});
            }
        }
        categoryMetricsModel.setRowCount(0);
        if (s.costByCategory() != null) {
            for (Map.Entry<String, BigDecimal> entry : s.costByCategory().entrySet()) {
                categoryMetricsModel.addRow(new Object[]{entry.getKey(), UIHelper.formatMzn(entry.getValue())});
            }
        }
    }

    public void exportPdfReport() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        LocalDate start = parseDate(dateStartField.getText().trim(), LocalDate.now().minusDays(30));
        LocalDate end = parseDate(dateEndField.getText().trim(), LocalDate.now());

        UIHelper.runWithProgress(this, "A gerar relatório oficial de quebras em PDF...", () -> {
            return wasteApiClient.renderReportPdf(companyId, start, end);
        }, pdf -> PrintPreviewDialog.show(this, pdf, "relatorio-quebras-perdas"),
        err -> ToastManager.show(this, FeedbackType.ERROR, "Erro ao gerar PDF: " + err.getMessage()));
    }

    private LocalDate parseDate(String text, LocalDate fallback) {
        try {
            return LocalDate.parse(text, UIHelper.DATE_FMT);
        } catch (Exception ex) {
            return fallback;
        }
    }

    public DefaultTableModel getReasonMetricsModel() { return reasonMetricsModel; }
    public DefaultTableModel getCategoryMetricsModel() { return categoryMetricsModel; }
}
