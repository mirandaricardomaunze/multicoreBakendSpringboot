package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Centro Executivo de Risco de Crédito, Matriz de Aging & Cobrança Formal.
 * Controlos uniformizados a 38px, monitorização proactiva de dívida e gestão de excepções.
 */
public class CreditRiskPanel extends JPanel {


    private final CreditRiskApiClient creditRiskApiClient;

    final JLabel kpiTotalReceivable = new JLabel("0,00 MT");
    final JLabel kpiTotalOverdue = new JLabel("0,00 MT");
    final JLabel kpiBlockedCount = new JLabel("0");
    final JLabel kpiCriticalCount = new JLabel("0");

    JTextField dateField;
    JComboBox<String> riskFilterCombo;
    JTextField searchField;
    private JProgressBar busyBar;

    DefaultTableModel tableModel;
    JTable table;
    List<ClientCreditRiskDTO> allClientsRisk = new ArrayList<>();
    List<ClientCreditRiskDTO> visibleClientsRisk = new ArrayList<>();

    public CreditRiskPanel(CreditRiskApiClient creditRiskApiClient) {
        this.creditRiskApiClient = creditRiskApiClient;

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

        JLabel title = UIHelper.createHeading("Centro de Risco de Crédito & Cobrança");
        JLabel subtitle = new JLabel("Matriz de Aging, monitorização de limites, cartas de cobrança e excepções de crédito");
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

        kpiRow.add(KpiCard.createMetricCard("TOTAL A RECEBER", kpiTotalReceivable, "Carteira global em aberto", "fas-wallet", UIHelper.ACCENT_BLUE));
        kpiRow.add(KpiCard.createMetricCard("SALDO EM MORA", kpiTotalOverdue, "Valores vencidos em atraso", "fas-exclamation-triangle", UIHelper.PENDING_YELLOW));
        kpiRow.add(KpiCard.createMetricCard("CLIENTES BLOQUEADOS", kpiBlockedCount, "Vendas a crédito suspensas", "fas-ban", UIHelper.REJECTED_RED));
        kpiRow.add(KpiCard.createMetricCard("RISCO ELEVADO / CRÍTICO", kpiCriticalCount, "Atrasos > 30d ou limite excedido", "fas-user-slash", UIHelper.REJECTED_RED));

        return kpiRow;
    }

    private JPanel buildMainCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        card.add(buildFilterBar(), BorderLayout.NORTH);

        String[] columns = {
                "ID", "Cliente", "NUIT", "Limite (MT)", "Dívida Total", "Disponível",
                "Corrente", "1-30d", "31-60d", "61-90d", ">90d",
                "Total em Mora", "Atraso", "Nível Risco", "Situação"
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
            table.getColumnModel().getColumn(0).setMaxWidth(50); // ID
            table.getColumnModel().getColumn(1).setPreferredWidth(170); // Cliente
            table.getColumnModel().getColumn(2).setPreferredWidth(90); // NUIT
            table.getColumnModel().getColumn(3).setPreferredWidth(95); // Limite
            table.getColumnModel().getColumn(4).setPreferredWidth(100); // Dívida
            table.getColumnModel().getColumn(5).setPreferredWidth(95); // Disponível
            table.getColumnModel().getColumn(11).setPreferredWidth(100); // Em Mora
            table.getColumnModel().getColumn(12).setPreferredWidth(65); // Atraso
            table.getColumnModel().getColumn(13).setPreferredWidth(95); // Risco
            table.getColumnModel().getColumn(14).setPreferredWidth(110); // Situação
        }

        table.setDefaultRenderer(Object.class, new CreditRiskCellRenderer());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    emitDebtCollectionNoticePdf();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFilterBar() {
        JLabel dateLbl = new JLabel("Data Ref:");
        dateLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        dateLbl.setForeground(UIHelper.TEXT_MUTED);

        dateField = new JTextField(LocalDate.now().toString());
        dateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleTextField(dateField);

        riskFilterCombo = new JComboBox<>(new String[]{
                "Todos os Riscos", "Apenas Crítico / Alto", "Apenas Bloqueados",
                "CRITICAL - Crítico", "HIGH - Alto", "MEDIUM - Médio", "LOW - Baixo"
        });
        riskFilterCombo.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(riskFilterCombo);

        searchField = TableFilter.searchField("Pesquisar cliente, NUIT, email...");
        searchField.setPreferredSize(new Dimension(230, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshData);

        ModernButton pdfBtn = UIHelper.createPrimaryButton("Carta Cobrança (PDF)");
        pdfBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        pdfBtn.setPreferredSize(new Dimension(175, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton exceptionBtn = UIHelper.createSuccessButton("Pedir Excepção");
        exceptionBtn.setIcon(UIHelper.icon("fas-unlock-alt", 14));
        exceptionBtn.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel bar = TableFilter.toolbar(
                new JComponent[]{dateLbl, dateField, riskFilterCombo, searchField},
                new JComponent[]{refreshBtn, pdfBtn, exceptionBtn}
        );

        // Eventos
        pdfBtn.addActionListener(e -> emitDebtCollectionNoticePdf());
        exceptionBtn.addActionListener(e -> openCreditExceptionDialog());

        riskFilterCombo.addActionListener(e -> applyFilters());
        dateField.addActionListener(e -> refreshData());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        return bar;
    }

    public void refreshData() {
        busyBar.setVisible(true);
        LocalDate refDate = parseReferenceDate();

        new SwingWorker<RiskFetchResult, Void>() {
            @Override
            protected RiskFetchResult doInBackground() {
                CreditRiskSummaryDTO summary = creditRiskApiClient.getSummary(refDate);
                List<ClientCreditRiskDTO> clients = creditRiskApiClient.getClientsRisk(refDate);
                return new RiskFetchResult(summary, clients);
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    RiskFetchResult res = get();
                    updateKpis(res.summary);
                    allClientsRisk = res.clients != null ? res.clients : new ArrayList<>();
                    applyFilters();
                } catch (Exception ex) {
                    ToastManager.show(CreditRiskPanel.this, FeedbackType.ERROR,
                            "Erro ao carregar risco de crédito: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private LocalDate parseReferenceDate() {
        try {
            String text = dateField != null ? dateField.getText().trim() : "";
            if (!text.isEmpty()) {
                return LocalDate.parse(text);
            }
        } catch (Exception ignored) {}
        return LocalDate.now();
    }

    void updateKpis(CreditRiskSummaryDTO summary) {
        if (summary == null) return;
        kpiTotalReceivable.setText(UIHelper.formatMzn(summary.totalReceivable()));
        kpiTotalOverdue.setText(UIHelper.formatMzn(summary.totalOverdue()));
        kpiBlockedCount.setText(String.valueOf(summary.blockedClientsCount()));
        kpiCriticalCount.setText(String.valueOf(summary.criticalRiskCount() + summary.highRiskCount()));
    }

    void applyFilters() {
        String filterType = riskFilterCombo != null ? (String) riskFilterCombo.getSelectedItem() : "Todos os Riscos";
        String term = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        visibleClientsRisk = new ArrayList<>();
        tableModel.setRowCount(0);

        for (ClientCreditRiskDTO c : allClientsRisk) {
            // Filtro por Nível/Estado
            if ("Apenas Bloqueados".equals(filterType) && !c.isBlocked()) continue;
            if ("Apenas Crítico / Alto".equals(filterType) && c.riskLevel() != CreditRiskLevel.CRITICAL && c.riskLevel() != CreditRiskLevel.HIGH) continue;
            if ("CRITICAL - Crítico".equals(filterType) && c.riskLevel() != CreditRiskLevel.CRITICAL) continue;
            if ("HIGH - Alto".equals(filterType) && c.riskLevel() != CreditRiskLevel.HIGH) continue;
            if ("MEDIUM - Médio".equals(filterType) && c.riskLevel() != CreditRiskLevel.MEDIUM) continue;
            if ("LOW - Baixo".equals(filterType) && c.riskLevel() != CreditRiskLevel.LOW) continue;

            // Filtro por Texto
            if (!term.isEmpty()) {
                boolean matchName = c.clientName() != null && c.clientName().toLowerCase().contains(term);
                boolean matchTaxId = c.clientTaxId() != null && c.clientTaxId().toLowerCase().contains(term);
                boolean matchEmail = c.email() != null && c.email().toLowerCase().contains(term);
                if (!matchName && !matchTaxId && !matchEmail) continue;
            }

            visibleClientsRisk.add(c);

            String statusLabel = c.isBlocked() ? "BLOQUEADO" : (c.blockReason() != null ? "EXCEPÇÃO" : "NORMAL");

            tableModel.addRow(new Object[]{
                    c.clientId(),
                    c.clientName(),
                    c.clientTaxId() != null ? c.clientTaxId() : "-",
                    c.creditLimit() != null ? formatMoney(c.creditLimit()) : "Livre",
                    formatMoney(c.totalDebt()),
                    c.availableCredit() != null ? formatMoney(c.availableCredit()) : "Livre",
                    formatMoney(c.corrente()),
                    formatMoney(c.ate30()),
                    formatMoney(c.de31a60()),
                    formatMoney(c.de61a90()),
                    formatMoney(c.maisDe90()),
                    formatMoney(c.totalOverdue()),
                    c.maxDaysOverdue() > 0 ? c.maxDaysOverdue() + " d" : "-",
                    c.riskLevel().label(),
                    statusLabel
            });
        }
    }

    private ClientCreditRiskDTO getSelectedClientRisk() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= visibleClientsRisk.size()) {
            return null;
        }
        return visibleClientsRisk.get(row);
    }

    private void emitDebtCollectionNoticePdf() {
        ClientCreditRiskDTO selected = getSelectedClientRisk();
        if (selected == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione um cliente na tabela para emitir a carta de cobrança.");
            return;
        }

        if (selected.totalOverdue() == null || selected.totalOverdue().signum() <= 0) {
            ToastManager.show(this, FeedbackType.INFO,
                    String.format("O cliente %s não possui facturas vencidas em mora.", selected.clientName()));
            return;
        }

        busyBar.setVisible(true);
        LocalDate refDate = parseReferenceDate();

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return creditRiskApiClient.getNoticePdf(selected.clientId(), refDate);
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(CreditRiskPanel.this, pdfBytes,
                                "notificacao-cobranca-" + selected.clientTaxId() + ".pdf");
                    } else {
                        ToastManager.show(CreditRiskPanel.this, FeedbackType.ERROR, "Não foi possível gerar a carta de cobrança.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(CreditRiskPanel.this, FeedbackType.ERROR,
                            "Erro ao gerar carta de cobrança: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void openCreditExceptionDialog() {
        ClientCreditRiskDTO selected = getSelectedClientRisk();

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Pedido de Excepção de Crédito", true);
        dialog.setLayout(new BorderLayout(0, 16));
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 24, 20, 24));
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Cliente
        content.add(createFormLabel("Cliente:"));
        JTextField clientField = new JTextField(selected != null ? selected.clientName() + " (NUIT: " + selected.clientTaxId() + ")" : "Seleccione um cliente na tabela primeiro");
        clientField.setEditable(false);
        clientField.setPreferredSize(new Dimension(420, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleTextField(clientField);
        content.add(clientField);
        content.add(Box.createVerticalStrut(12));

        // Valor Solicitado
        content.add(createFormLabel("Valor de Crédito Adicional Requerido (MT):"));
        MoneyField amountField = new MoneyField();
        amountField.setPreferredSize(new Dimension(420, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleTextField(amountField);
        content.add(amountField);
        content.add(Box.createVerticalStrut(12));

        // Motivo / Justificação
        content.add(createFormLabel("Justificação Comercial / Motivo da Excepção:"));
        JTextArea reasonArea = new JTextArea(4, 30);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBackground(UIHelper.BG_CARD);
        reasonArea.setForeground(UIHelper.TEXT_LIGHT);
        reasonArea.setCaretColor(UIHelper.TEXT_LIGHT);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        UIHelper.styleScrollPane(reasonScroll);
        content.add(reasonScroll);

        dialog.add(content, BorderLayout.CENTER);

        // Botões do diálogo
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        ModernButton cancelBtn = UIHelper.createSecondaryButton("Cancelar");
        cancelBtn.setPreferredSize(new Dimension(100, UIHelper.FORM_CONTROL_HEIGHT));
        cancelBtn.addActionListener(e -> dialog.dispose());

        ModernButton submitBtn = UIHelper.createSuccessButton("Submeter à Gerência");
        submitBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        submitBtn.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));

        submitBtn.addActionListener(e -> {
            if (selected == null) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Por favor seleccione primeiro um cliente na tabela.");
                return;
            }
            BigDecimal amount;
            try {
                amount = amountField.value();
            } catch (Exception ex) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Introduza um valor monetário válido.");
                return;
            }
            if (amount == null || amount.signum() <= 0) {
                ToastManager.show(dialog, FeedbackType.WARNING, "Introduza um valor válido para o crédito.");
                return;
            }
            String reason = reasonArea.getText().trim();
            if (reason.length() < 10) {
                ToastManager.show(dialog, FeedbackType.WARNING, "A justificação deve conter pelo menos 10 caracteres explicativos.");
                return;
            }

            submitBtn.setEnabled(false);
            CreditExceptionApprovalRequest req = new CreditExceptionApprovalRequest(
                    selected.clientId(), amount, reason, CurrentUserContext.getUsername()
            );

            new SwingWorker<ApprovalRequestDTO, Void>() {
                @Override
                protected ApprovalRequestDTO doInBackground() {
                    return creditRiskApiClient.requestCreditException(req);
                }

                @Override
                protected void done() {
                    dialog.dispose();
                    try {
                        ApprovalRequestDTO approval = get();
                        ToastManager.success(CreditRiskPanel.this,
                                "Pedido de excepção submetido com sucesso. ID: #" + approval.id() + " (Perfil: " + approval.requiredRole() + ")");
                        refreshData();
                    } catch (Exception ex) {
                        ToastManager.show(CreditRiskPanel.this, FeedbackType.ERROR,
                                "Erro ao submeter excepção: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        buttons.add(cancelBtn);
        buttons.add(submitBtn);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        return label;
    }

    private static String formatMoney(BigDecimal val) {
        return UIHelper.formatMzn(val);
    }

    private record RiskFetchResult(CreditRiskSummaryDTO summary, List<ClientCreditRiskDTO> clients) {}

    /**
     * Renderizador visual de células para a matriz de aging, crachás de risco e bloqueio.
     */
    private static class CreditRiskCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            setHorizontalAlignment(SwingConstants.LEFT);

            // Alinhamento à direita para valores monetários e atrasos
            if (col >= 3 && col <= 12) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }

            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT);
                c.setForeground(UIHelper.TEXT_LIGHT);

                // Destaque de risco na coluna 13 (Nível Risco)
                if (col == 13 && value != null) {
                    String str = value.toString();
                    if ("Crítico".equalsIgnoreCase(str)) {
                        c.setForeground(UIHelper.REJECTED_RED);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if ("Alto".equalsIgnoreCase(str)) {
                        c.setForeground(UIHelper.KPI_ORANGE_DARK); // Laranja
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if ("Médio".equalsIgnoreCase(str)) {
                        c.setForeground(UIHelper.PENDING_YELLOW);
                    } else {
                        c.setForeground(UIHelper.APPROVED_GREEN);
                    }
                }

                // Destaque na coluna 14 (Situação)
                if (col == 14 && value != null) {
                    String str = value.toString();
                    if ("BLOQUEADO".equalsIgnoreCase(str)) {
                        c.setForeground(UIHelper.REJECTED_RED);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if ("EXCEPÇÃO".equalsIgnoreCase(str)) {
                        c.setForeground(UIHelper.ACCENT_BLUE);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(UIHelper.APPROVED_GREEN);
                    }
                }
            }

            return c;
        }
    }
}
