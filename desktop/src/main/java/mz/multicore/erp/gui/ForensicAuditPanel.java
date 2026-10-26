package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel Executivo da Central de Auditoria Forense & Controlo de Fraude Interna.
 * Monitorização analítica de anomalias fiscais, cancelamentos, quebras de armazém e descontos manuais.
 */
public class ForensicAuditPanel extends JPanel {

    private final ForensicAuditApiClient apiClient;

    final JLabel kpiTotalRisk = new JLabel("0,00 MT");
    final JLabel kpiCritical = new JLabel("0");
    final JLabel kpiSuspicious = new JLabel("0");
    final JLabel kpiComplianceScore = new JLabel("100% — Regular");

    DateField startDateField;
    DateField endDateField;
    JComboBox<String> severityCombo;
    JComboBox<String> categoryCombo;
    JTextField operatorField;
    private JProgressBar busyBar;

    DefaultTableModel tableModel;
    JTable table;
    List<ForensicAnomalyDTO> currentAnomalies = new ArrayList<>();

    public ForensicAuditPanel(ForensicAuditApiClient apiClient) {
        this.apiClient = apiClient;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(buildKpiBar(), BorderLayout.NORTH);
        centerPanel.add(buildMainCard(), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel title = UIHelper.createHeading("Central de Auditoria Forense & Controlo de Fraude");
        JLabel subtitle = new JLabel("Deteção analítica de faturas canceladas, quebras anormais, desvios e concessão de descontos");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);

        busyBar = UIHelper.createBusyBar();
        busyBar.setVisible(false);
        busyBar.setPreferredSize(new Dimension(160, 6));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        right.setOpaque(false);
        right.add(busyBar);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel buildKpiBar() {
        JPanel kpiRow = KpiCard.createGrid(4);
        kpiRow.add(KpiCard.createMetricCard("RISCO FINANCEIRO TOTAL", kpiTotalRisk, "Exposição apurada em auditoria", "fas-shield-alt", UIHelper.REJECTED_RED));
        kpiRow.add(KpiCard.createMetricCard("OCORRÊNCIAS CRÍTICAS", kpiCritical, "Cancelamentos e desvios graves", "fas-exclamation-circle", UIHelper.REJECTED_RED));
        kpiRow.add(KpiCard.createMetricCard("OCORRÊNCIAS SUSPEITAS", kpiSuspicious, "Operações com padrão atípico", "fas-exclamation-triangle", UIHelper.PENDING_YELLOW));
        kpiRow.add(KpiCard.createMetricCard("ÍNDICE DE CONFORMIDADE", kpiComplianceScore, "Score global de controlo interno", "fas-clipboard-check", UIHelper.APPROVED_GREEN));
        return kpiRow;
    }

    private JPanel buildMainCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        card.add(buildFilterBar(), BorderLayout.NORTH);

        String[] columns = {
                "Data/Hora", "Severidade", "Categoria", "Referência", "Operador",
                "Impacto (MT)", "Detalhes / Ocorrência", "Recomendação Preventiva"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.putClientProperty("noRowInspector", Boolean.TRUE);

        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(125);
            table.getColumnModel().getColumn(1).setPreferredWidth(95);
            table.getColumnModel().getColumn(2).setPreferredWidth(160);
            table.getColumnModel().getColumn(3).setPreferredWidth(110);
            table.getColumnModel().getColumn(4).setPreferredWidth(110);
            table.getColumnModel().getColumn(5).setPreferredWidth(110);
            table.getColumnModel().getColumn(6).setPreferredWidth(230);
            table.getColumnModel().getColumn(7).setPreferredWidth(240);
        }

        table.setDefaultRenderer(Object.class, new ForensicCellRenderer());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showAnomalyDetails(table.getSelectedRow());
                }
            }
        });

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            ForensicAuditOperationsCoordinator.populatePeek(peek, tableModel, row, this::showAnomalyDetails);
        });

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFilterBar() {
        JLabel lblStart = new JLabel("Início:");
        lblStart.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblStart.setForeground(UIHelper.TEXT_MUTED);

        startDateField = new DateField(LocalDate.now().minusDays(30));
        startDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));

        JLabel lblEnd = new JLabel("Fim:");
        lblEnd.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblEnd.setForeground(UIHelper.TEXT_MUTED);

        endDateField = new DateField(LocalDate.now());
        endDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));

        severityCombo = new JComboBox<>(new String[]{"Todas Severidades", "CRITICAL", "SUSPICIOUS", "INFO"});
        severityCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(severityCombo);

        categoryCombo = new JComboBox<>(new String[]{
                "Todas Categorias",
                "Cancelamento de Documentos",
                "Desconto Excessivo",
                "Quebra / Desvio de Stock",
                "Risco / Excepção de Crédito",
                "Anulação de Pagamento",
                "Segurança e Auditoria"
        });
        categoryCombo.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(categoryCombo);

        operatorField = TableFilter.searchField("Operador...");
        operatorField.setPreferredSize(new Dimension(105, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadData);

        ModernButton pdfBtn = UIHelper.createPrimaryButton("Dossiê PDF");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        pdfBtn.setToolTipText("Emitir Dossiê de Auditoria Forense em PDF A4");
        pdfBtn.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        pdfBtn.addActionListener(e -> emitForensicPdf());

        return TableFilter.toolbar(
                new JComponent[]{lblStart, startDateField, lblEnd, endDateField, severityCombo, categoryCombo, operatorField},
                new JComponent[]{refreshBtn, pdfBtn}
        );
    }

    public void loadData() {
        busyBar.setVisible(true);
        busyBar.setIndeterminate(true);

        LocalDate start = ForensicAuditOperationsCoordinator.parseDate(startDateField.getText().trim());
        LocalDate end = ForensicAuditOperationsCoordinator.parseDate(endDateField.getText().trim());
        ForensicSeverity sev = ForensicAuditOperationsCoordinator.parseSeverity(severityCombo.getSelectedIndex());
        ForensicCategory cat = ForensicAuditOperationsCoordinator.parseCategory(categoryCombo.getSelectedIndex());
        String operator = operatorField.getText().trim();

        new SwingWorker<ForensicAuditSummaryDTO, Void>() {
            @Override
            protected ForensicAuditSummaryDTO doInBackground() {
                return apiClient.getSummary(start, end, sev, cat, operator);
            }

            @Override
            protected void done() {
                busyBar.setIndeterminate(false);
                busyBar.setVisible(false);
                try {
                    ForensicAuditSummaryDTO summary = get();
                    renderSummary(summary);
                } catch (Exception ex) {
                    ToastManager.show(ForensicAuditPanel.this, FeedbackType.ERROR, "Falha ao carregar auditoria forense: " + ex.getMessage());
                }
            }
        }.execute();
    }

    void renderSummary(ForensicAuditSummaryDTO summary) {
        if (summary == null) return;

        kpiTotalRisk.setText(UIHelper.formatMzn(summary.totalFinancialRisk() != null ? summary.totalFinancialRisk() : BigDecimal.ZERO));
        kpiCritical.setText(String.valueOf(summary.criticalCount()));
        kpiSuspicious.setText(String.valueOf(summary.suspiciousCount()));
        kpiComplianceScore.setText(summary.complianceScore() != null ? summary.complianceScore() : "100%");

        currentAnomalies = summary.anomalies() != null ? summary.anomalies() : new ArrayList<>();
        tableModel.setRowCount(0);

        for (ForensicAnomalyDTO a : currentAnomalies) {
            String timeStr = a.timestamp() != null ? a.timestamp().format(UIHelper.DATETIME_FMT) : "—";
            String impactStr = UIHelper.formatMzn(a.financialImpact() != null ? a.financialImpact() : BigDecimal.ZERO);

            tableModel.addRow(new Object[]{
                    timeStr,
                    a.severity() != null ? a.severity().name() : "INFO",
                    a.categoryLabel() != null ? a.categoryLabel() : "—",
                    a.documentOrReference() != null ? a.documentOrReference() : "—",
                    a.operator() != null ? a.operator() : "—",
                    impactStr,
                    a.details() != null ? a.details() : "—",
                    a.recommendation() != null ? a.recommendation() : "—"
            });
        }
    }

    private void emitForensicPdf() {
        LocalDate start = ForensicAuditOperationsCoordinator.parseDate(startDateField.getText().trim());
        LocalDate end = ForensicAuditOperationsCoordinator.parseDate(endDateField.getText().trim());
        ForensicSeverity sev = ForensicAuditOperationsCoordinator.parseSeverity(severityCombo.getSelectedIndex());
        ForensicCategory cat = ForensicAuditOperationsCoordinator.parseCategory(categoryCombo.getSelectedIndex());
        String operator = operatorField.getText().trim();

        ForensicAuditOperationsCoordinator.emitForensicPdf(this, apiClient, busyBar, start, end, sev, cat, operator);
    }

    private void showAnomalyDetails(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= currentAnomalies.size()) return;
        ForensicAnomalyDetailsDialog.show(this, currentAnomalies.get(rowIndex));
    }
}
