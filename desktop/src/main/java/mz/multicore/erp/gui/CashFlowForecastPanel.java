package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.CashFlowBucketDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowItemDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CashFlowForecastPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CashFlowForecastApiClient forecastApiClient;

    private final JLabel lblLiquidityVal = new JLabel("0,00 MT");
    private final JLabel lblReceivablesVal = new JLabel("0,00 MT");
    private final JLabel lblPayablesVal = new JLabel("0,00 MT");
    private final JLabel lblNetVal = new JLabel("0,00 MT");

    private final ModernPanel alertBanner = new ModernPanel(10);
    private final JLabel lblAlertTitle = new JLabel("A carregar posição de tesouraria...");
    private final JLabel lblAlertRecommendation = new JLabel("");

    private DefaultTableModel bucketsTableModel;
    private JTable bucketsTable;

    private DefaultTableModel topReceivablesModel;
    private JTable topReceivablesTable;

    private DefaultTableModel topPayablesModel;
    private JTable topPayablesTable;

    private final JProgressBar busyBar = new JProgressBar();
    private CashFlowForecastDTO currentForecast;

    public CashFlowForecastPanel(CashFlowForecastApiClient forecastApiClient) {
        this.forecastApiClient = forecastApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);

        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(8, 8, 16, 8));

        // 1. Toolbar superior com título e botões
        mainContent.add(buildToolbar());
        mainContent.add(Box.createVerticalStrut(10));

        // 2. Cartões de Resumo Métrico Superior
        mainContent.add(buildSummaryCards());
        mainContent.add(Box.createVerticalStrut(10));

        // 3. Banner de Alerta e Recomendações
        mainContent.add(buildAlertBannerPanel());
        mainContent.add(Box.createVerticalStrut(8));

        busyBar.setIndeterminate(true);
        busyBar.setVisible(false);
        mainContent.add(busyBar);
        mainContent.add(Box.createVerticalStrut(6));

        // 4. Matriz de Baldes Temporais (tamanho natural para 5 linhas completas)
        mainContent.add(buildBucketsCard());
        mainContent.add(Box.createVerticalStrut(12));

        // 5. Abas com Detalhes dos Maiores Movimentos
        mainContent.add(buildDetailsCard());

        // Painel de Scroll com rastreio de largura integral e setas suaves
        ArrowScrollPanel scroll = new ArrowScrollPanel(mainContent);
        add(scroll, BorderLayout.CENTER);

        // Carrega os dados assincronamente ao iniciar
        loadForecast();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel("Projeção de Fluxo de Caixa Previsional (Cash Flow Forecast)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(UIHelper.TEXT_LIGHT);
        bar.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        ModernButton btnRefresh = UIHelper.createRefreshButton(this::loadForecast);
        btnRefresh.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));
        actions.add(btnRefresh);

        ModernButton btnPrint = new ModernButton("Imprimir / PDF");
        btnPrint.setIcon(UIHelper.icon("fas-file-pdf", 13, Color.WHITE));
        btnPrint.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        btnPrint.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        btnPrint.addActionListener(e -> printForecastPdf());
        actions.add(btnPrint);

        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 0));
        grid.setOpaque(false);

        grid.add(createKpiCard("Disponível Imediato", lblLiquidityVal, "fas-wallet", UIHelper.ACCENT_BLUE));
        grid.add(createKpiCard("Contas a Receber", lblReceivablesVal, "fas-arrow-circle-down", UIHelper.APPROVED_GREEN));
        grid.add(createKpiCard("Contas a Pagar", lblPayablesVal, "fas-arrow-circle-up", UIHelper.REJECTED_RED));
        grid.add(createKpiCard("Saldo Projetado Líquido", lblNetVal, "fas-chart-line", UIHelper.TEXT_LIGHT));

        return grid;
    }

    private ModernPanel createKpiCard(String title, JLabel valLabel, String icon, Color accent) {
        ModernPanel card = new ModernPanel(10);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.PLAIN, 11f));
        titleLbl.setForeground(UIHelper.TEXT_MUTED);
        top.add(titleLbl, BorderLayout.WEST);

        JLabel iconLbl = new JLabel(UIHelper.icon(icon, 14, accent));
        top.add(iconLbl, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        valLabel.setFont(valLabel.getFont().deriveFont(Font.BOLD, 15f));
        valLabel.setForeground(accent);
        card.add(valLabel, BorderLayout.CENTER);

        return card;
    }

    private ModernPanel buildAlertBannerPanel() {
        alertBanner.setLayout(new BorderLayout(0, 2));
        alertBanner.setBorder(new EmptyBorder(8, 12, 8, 12));
        alertBanner.setBackground(UIHelper.KPI_INFO_SOFT);

        lblAlertTitle.setFont(lblAlertTitle.getFont().deriveFont(Font.BOLD, 12f));
        lblAlertTitle.setForeground(UIHelper.TEXT_LIGHT);
        alertBanner.add(lblAlertTitle, BorderLayout.NORTH);

        lblAlertRecommendation.setFont(lblAlertRecommendation.getFont().deriveFont(Font.PLAIN, 11f));
        lblAlertRecommendation.setForeground(UIHelper.TEXT_MUTED);
        alertBanner.add(lblAlertRecommendation, BorderLayout.CENTER);

        return alertBanner;
    }

    private ModernPanel buildBucketsCard() {
        ModernPanel card = new ModernPanel(10);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Matriz de Evolução Temporal da Liquidez (Horizontes Previsionais)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(UIHelper.TEXT_LIGHT);
        card.add(title, BorderLayout.NORTH);

        String[] cols = {"Período Temporal", "Entradas Previstas (+)", "Saídas Previstas (-)", "Movimento Líquido", "Saldo Projetado Cumulativo"};
        bucketsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        bucketsTable = new JTable(bucketsTableModel);
        UIHelper.styleTable(bucketsTable);

        // Exibe todas as 5 linhas da matriz temporal de liquidez com altura confortável
        bucketsTable.setPreferredScrollableViewportSize(new Dimension(100, 5 * 35));

        bucketsTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        bucketsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        bucketsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        bucketsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        bucketsTable.getColumnModel().getColumn(4).setPreferredWidth(140);

        for (int c : new int[]{1, 2, 3, 4}) {
            bucketsTable.getColumnModel().getColumn(c).setCellRenderer(new MoneyCellRenderer(c == 4));
        }

        JScrollPane scroll = new JScrollPane(bucketsTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(0, 222));
        card.add(scroll, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 275));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 275));

        return card;
    }

    private ModernPanel buildDetailsCard() {
        ModernPanel card = new ModernPanel(10);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        // Aba 1: Maiores Contas a Receber
        String[] rCols = {"Documento", "Cliente", "Vencimento", "Montante Pendente", "Período"};
        topReceivablesModel = new DefaultTableModel(rCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        topReceivablesTable = new JTable(topReceivablesModel);
        UIHelper.styleTable(topReceivablesTable);
        topReceivablesTable.setPreferredScrollableViewportSize(new Dimension(100, 260));
        topReceivablesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        JScrollPane rScroll = new JScrollPane(topReceivablesTable);
        UIHelper.styleScrollPane(rScroll);
        tabs.addTab("Maiores Recebimentos Previstos", UIHelper.icon("fas-hand-holding-usd", 13, UIHelper.TEXT_LIGHT), rScroll);

        // Aba 2: Maiores Contas a Pagar
        String[] pCols = {"Documento", "Fornecedor", "Vencimento", "Montante Pendente", "Período"};
        topPayablesModel = new DefaultTableModel(pCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        topPayablesTable = new JTable(topPayablesModel);
        UIHelper.styleTable(topPayablesTable);
        topPayablesTable.setPreferredScrollableViewportSize(new Dimension(100, 260));
        topPayablesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        JScrollPane pScroll = new JScrollPane(topPayablesTable);
        UIHelper.styleScrollPane(pScroll);
        tabs.addTab("Maiores Pagamentos a Fornecedores", UIHelper.icon("fas-file-invoice-dollar", 13, UIHelper.TEXT_LIGHT), pScroll);

        card.add(tabs, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 360));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        return card;
    }

    public void loadForecast() {
        busyBar.setVisible(true);

        new SwingWorker<CashFlowForecastDTO, Void>() {
            @Override
            protected CashFlowForecastDTO doInBackground() {
                try {
                    return forecastApiClient.getForecast();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    CashFlowForecastDTO data = get();
                    if (data != null) {
                        displayForecast(data);
                    } else {
                        ToastManager.show(CashFlowForecastPanel.this, FeedbackType.WARNING, "Não foi possível carregar a projeção de tesouraria.");
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void displayForecast(CashFlowForecastDTO data) {
        this.currentForecast = data;

        lblLiquidityVal.setText(formatMoney(data.totalAvailableLiquidity()));
        lblReceivablesVal.setText(formatMoney(data.totalReceivables()));
        lblPayablesVal.setText(formatMoney(data.totalPayables()));
        lblNetVal.setText(formatMoney(data.netProjectedPosition()));

        // Actualizar Banner de Alerta
        if (data.alert() != null) {
            String status = data.alert().status();
            if ("CRITICAL".equals(status)) {
                alertBanner.setBackground(UIHelper.KPI_DANGER_SOFT);
                lblAlertTitle.setForeground(UIHelper.KPI_DANGER_DARK);
                lblAlertTitle.setText("ALERTA CRÍTICO DE TESOURARIA: " + data.alert().alertMessage());
            } else if ("WARNING".equals(status)) {
                alertBanner.setBackground(UIHelper.KPI_WARNING_SOFT);
                lblAlertTitle.setForeground(UIHelper.KPI_WARNING_DARK);
                lblAlertTitle.setText("AVISO DE CAIXA: " + data.alert().alertMessage());
            } else {
                alertBanner.setBackground(UIHelper.KPI_SUCCESS_SOFT);
                lblAlertTitle.setForeground(UIHelper.APPROVED_GREEN);
                lblAlertTitle.setText("SITUAÇÃO DE TESOURARIA ESTÁVEL: " + data.alert().alertMessage());
            }
            lblAlertRecommendation.setText("Recomendação: " + (data.alert().recommendation() != null ? data.alert().recommendation() : "Manter acompanhamento de rotina."));
        }

        // Preencher Matriz de Baldes
        bucketsTableModel.setRowCount(0);
        for (CashFlowBucketDTO b : data.buckets()) {
            bucketsTableModel.addRow(new Object[]{
                    b.bucketLabel(),
                    b.inflows(),
                    b.outflows(),
                    b.netMovement(),
                    b.projectedCumulativeBalance()
            });
        }

        // Preencher Top Receivables
        topReceivablesModel.setRowCount(0);
        if (data.topReceivables() != null) {
            for (CashFlowItemDTO r : data.topReceivables()) {
                topReceivablesModel.addRow(new Object[]{
                        r.documentNumber(),
                        r.entityName(),
                        r.dueDate() != null ? r.dueDate().format(DATE_FMT) : "-",
                        r.amount(),
                        r.bucketCode()
                });
            }
        }

        // Preencher Top Payables
        topPayablesModel.setRowCount(0);
        if (data.topPayables() != null) {
            for (CashFlowItemDTO p : data.topPayables()) {
                topPayablesModel.addRow(new Object[]{
                        p.documentNumber(),
                        p.entityName(),
                        p.dueDate() != null ? p.dueDate().format(DATE_FMT) : "-",
                        p.amount(),
                        p.bucketCode()
                });
            }
        }
    }

    private void printForecastPdf() {
        busyBar.setVisible(true);

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                try {
                    return forecastApiClient.getForecastPdf();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(CashFlowForecastPanel.this, pdfBytes, "fluxo-caixa-previsional.pdf");
                    } else {
                        ToastManager.show(CashFlowForecastPanel.this, FeedbackType.ERROR, "Não foi possível gerar o PDF de fluxo de caixa.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(CashFlowForecastPanel.this, FeedbackType.ERROR, "Erro ao gerar PDF: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public JTable getBucketsTable() {
        return bucketsTable;
    }

    public ModernPanel getAlertBanner() {
        return alertBanner;
    }

    public CashFlowForecastDTO getCurrentForecast() {
        return currentForecast;
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) return "0,00 MT";
        return String.format(java.util.Locale.forLanguageTag("pt-MZ"), "%,.2f MT", val);
    }

    private static class MoneyCellRenderer extends DefaultTableCellRenderer {
        private final boolean isCumulative;

        public MoneyCellRenderer(boolean isCumulative) {
            this.isCumulative = isCumulative;
            setHorizontalAlignment(RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (value instanceof BigDecimal val) {
                setText(String.format(java.util.Locale.forLanguageTag("pt-MZ"), "%,.2f MT", val));
                if (isCumulative) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    if (val.compareTo(BigDecimal.ZERO) < 0) {
                        setForeground(UIHelper.REJECTED_RED);
                    } else {
                        setForeground(UIHelper.APPROVED_GREEN);
                    }
                } else if (col == 3) { // Movimento Líquido
                    if (val.compareTo(BigDecimal.ZERO) < 0) {
                        setForeground(UIHelper.REJECTED_RED);
                    } else if (val.compareTo(BigDecimal.ZERO) > 0) {
                        setForeground(UIHelper.APPROVED_GREEN);
                    }
                }
            }
            return c;
        }
    }
}
