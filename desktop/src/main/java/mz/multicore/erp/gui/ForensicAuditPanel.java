package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Painel Executivo da Central de Auditoria Forense & Controlo de Fraude Interna.
 * Monitorização de anomalias fiscais, cancelamentos, quebras de armazém e descontos manuais.
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

        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
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
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

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
        table.putClientProperty("noRowInspector", Boolean.TRUE);

        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(125); // Data/Hora
            table.getColumnModel().getColumn(1).setPreferredWidth(95);  // Severidade
            table.getColumnModel().getColumn(2).setPreferredWidth(160); // Categoria
            table.getColumnModel().getColumn(3).setPreferredWidth(110); // Referência
            table.getColumnModel().getColumn(4).setPreferredWidth(110); // Operador
            table.getColumnModel().getColumn(5).setPreferredWidth(110); // Impacto
            table.getColumnModel().getColumn(6).setPreferredWidth(230); // Detalhes
            table.getColumnModel().getColumn(7).setPreferredWidth(240); // Recomendação
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

        LocalDate start = parseDate(startDateField.getText().trim());
        LocalDate end = parseDate(endDateField.getText().trim());
        ForensicSeverity sev = parseSeverity();
        ForensicCategory cat = parseCategory();
        String operator = operatorField.getText().trim();

        SwingWorker<ForensicAuditSummaryDTO, Void> worker = new SwingWorker<>() {
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
        };
        worker.execute();
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
        busyBar.setVisible(true);
        busyBar.setIndeterminate(true);

        LocalDate start = parseDate(startDateField.getText().trim());
        LocalDate end = parseDate(endDateField.getText().trim());
        ForensicSeverity sev = parseSeverity();
        ForensicCategory cat = parseCategory();
        String operator = operatorField.getText().trim();

        SwingWorker<byte[], Void> worker = new SwingWorker<>() {
            @Override
            protected byte[] doInBackground() {
                return apiClient.getPdf(start, end, sev, cat, operator);
            }

            @Override
            protected void done() {
                busyBar.setIndeterminate(false);
                busyBar.setVisible(false);
                try {
                    byte[] pdf = get();
                    if (pdf != null && pdf.length > 0) {
                        PrintPreviewDialog.show(ForensicAuditPanel.this, pdf, "dossie-auditoria-forense");
                    } else {
                        ToastManager.show(ForensicAuditPanel.this, FeedbackType.WARNING, "Dossiê PDF vazio devolvido pelo servidor.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(ForensicAuditPanel.this, FeedbackType.ERROR, "Erro ao gerar PDF forense: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showAnomalyDetails(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= currentAnomalies.size()) return;
        ForensicAnomalyDTO a = currentAnomalies.get(rowIndex);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Dossiê de Anomalia Forense", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(580, 420);
        dialog.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(UIHelper.BG_CARD);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = UIHelper.createHeading("Ocorrência: " + a.documentOrReference());
        p.add(title, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        area.setBackground(UIHelper.BG_DARK);
        area.setForeground(UIHelper.TEXT_LIGHT);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));

        String content = "Data/Hora: " + a.timestamp().format(UIHelper.DATETIME_FMT) + "\n"
                + "Severidade: " + a.severity() + "\n"
                + "Categoria: " + a.categoryLabel() + "\n"
                + "Operador / Utilizador: " + a.operator() + "\n"
                + "Impacto Financeiro: " + UIHelper.formatMzn(a.financialImpact()) + "\n"
                + "Motivo Original Declarado: " + a.justification() + "\n\n"
                + "DETALHES ANALÍTICOS:\n" + a.details() + "\n\n"
                + "RECOMENDAÇÃO PREVENTIVA DA AUDITORIA:\n" + a.recommendation();

        area.setText(content);
        p.add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.addActionListener(e -> dialog.dispose());
        btnRow.add(closeBtn);
        p.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(p);
        dialog.setVisible(true);
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ForensicSeverity parseSeverity() {
        int idx = severityCombo.getSelectedIndex();
        if (idx == 1) return ForensicSeverity.CRITICAL;
        if (idx == 2) return ForensicSeverity.SUSPICIOUS;
        if (idx == 3) return ForensicSeverity.INFO;
        return null;
    }

    private ForensicCategory parseCategory() {
        int idx = categoryCombo.getSelectedIndex();
        return switch (idx) {
            case 1 -> ForensicCategory.DOC_CANCELLATION;
            case 2 -> ForensicCategory.EXCESSIVE_DISCOUNT;
            case 3 -> ForensicCategory.STOCK_SHRINKAGE;
            case 4 -> ForensicCategory.CREDIT_OVERRIDE;
            case 5 -> ForensicCategory.PAYMENT_VOID;
            case 6 -> ForensicCategory.AUDIT_SECURITY;
            default -> null;
        };
    }

    private static class ForensicCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);

            if (!isSel) {
                c.setBackground(row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.BG_DARK);
            }

            if (col == 1) { // Severidade
                String sev = String.valueOf(val);
                if ("CRITICAL".equals(sev)) {
                    c.setForeground(UIHelper.REJECTED_RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("SUSPICIOUS".equals(sev)) {
                    c.setForeground(UIHelper.PENDING_YELLOW);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(UIHelper.TEXT_MUTED);
                }
                setHorizontalAlignment(CENTER);
            } else if (col == 5) { // Impacto
                setHorizontalAlignment(RIGHT);
                c.setForeground(UIHelper.TEXT_LIGHT);
            } else {
                c.setForeground(UIHelper.TEXT_LIGHT);
                setHorizontalAlignment(LEFT);
            }

            return c;
        }
    }
}
