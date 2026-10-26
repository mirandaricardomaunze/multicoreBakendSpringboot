package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.CashFlowBucketDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Painel Executivo de Projeção de Fluxo de Caixa Previsional (Cash Flow Forecast).
 * Monitorização de horizontes de liquidez, baldes temporais, alertas e maiores recebimentos/pagamentos.
 */
public class CashFlowForecastPanel extends JPanel {

    private final CashFlowForecastApiClient forecastApiClient;

    private final JLabel lblLiquidityVal = new JLabel("0,00 MT");
    private final JLabel lblReceivablesVal = new JLabel("0,00 MT");
    private final JLabel lblPayablesVal = new JLabel("0,00 MT");
    private final JLabel lblNetVal = new JLabel("0,00 MT");

    private final ModernPanel alertBanner = new ModernPanel(16);
    private final JLabel lblAlertTitle = new JLabel("A carregar posição de tesouraria...");
    private final JLabel lblAlertRecommendation = new JLabel("");

    private DefaultTableModel bucketsTableModel;
    private JTable bucketsTable;
    private CashFlowForecastDetailsPanel detailsPanel;

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

        mainContent.add(buildToolbar());
        mainContent.add(Box.createVerticalStrut(10));
        mainContent.add(buildSummaryCards());
        mainContent.add(Box.createVerticalStrut(10));
        mainContent.add(buildAlertBannerPanel());
        mainContent.add(Box.createVerticalStrut(8));

        busyBar.setIndeterminate(true);
        busyBar.setVisible(false);
        mainContent.add(busyBar);
        mainContent.add(Box.createVerticalStrut(6));

        mainContent.add(buildBucketsCard());
        mainContent.add(Box.createVerticalStrut(12));

        detailsPanel = new CashFlowForecastDetailsPanel();
        mainContent.add(detailsPanel);

        ArrowScrollPanel scroll = new ArrowScrollPanel(mainContent);
        add(scroll, BorderLayout.CENTER);

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
        JPanel grid = KpiCard.createGrid(4);
        grid.add(KpiCard.createCard("Disponível Imediato", lblLiquidityVal, "fas-wallet", UIHelper.ACCENT_BLUE));
        grid.add(KpiCard.createCard("Contas a Receber", lblReceivablesVal, "fas-arrow-circle-down", UIHelper.APPROVED_GREEN));
        grid.add(KpiCard.createCard("Contas a Pagar", lblPayablesVal, "fas-arrow-circle-up", UIHelper.REJECTED_RED));
        grid.add(KpiCard.createCard("Saldo Projetado Líquido", lblNetVal, "fas-chart-line", UIHelper.TEXT_LIGHT));
        return grid;
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
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Matriz de Evolução Temporal da Liquidez (Horizontes Previsionais)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(UIHelper.TEXT_LIGHT);
        card.add(title, BorderLayout.NORTH);

        String[] cols = {"Período Temporal", "Entradas Previstas (+)", "Saídas Previstas (-)", "Movimento Líquido", "Saldo Projetado Cumulativo"};
        bucketsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        bucketsTable = new JTable(bucketsTableModel);
        bucketsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(bucketsTable);
        bucketsTable.setPreferredScrollableViewportSize(new Dimension(100, 5 * 35));

        bucketsTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        bucketsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        bucketsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        bucketsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        bucketsTable.getColumnModel().getColumn(4).setPreferredWidth(140);

        for (int c : new int[]{1, 2, 3, 4}) {
            bucketsTable.getColumnModel().getColumn(c).setCellRenderer(new CashFlowMoneyCellRenderer(c == 4));
        }
        UIHelper.ensureHeadersFit(bucketsTable);

        JScrollPane scroll = new JScrollPane(bucketsTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(0, 222));
        card.add(scroll, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 275));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 275));

        return card;
    }

    public void loadForecast() {
        CashFlowForecastOperationsCoordinator.loadForecast(this, forecastApiClient, busyBar, this::displayForecast);
    }

    public void displayForecast(CashFlowForecastDTO data) {
        this.currentForecast = data;

        lblLiquidityVal.setText(CashFlowForecastOperationsCoordinator.formatMoney(data.totalAvailableLiquidity()));
        lblReceivablesVal.setText(CashFlowForecastOperationsCoordinator.formatMoney(data.totalReceivables()));
        lblPayablesVal.setText(CashFlowForecastOperationsCoordinator.formatMoney(data.totalPayables()));
        lblNetVal.setText(CashFlowForecastOperationsCoordinator.formatMoney(data.netProjectedPosition()));

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

        bucketsTableModel.setRowCount(0);
        for (CashFlowBucketDTO b : data.buckets()) {
            bucketsTableModel.addRow(new Object[]{
                    b.bucketLabel(), b.inflows(), b.outflows(), b.netMovement(), b.projectedCumulativeBalance()
            });
        }

        if (detailsPanel != null) {
            detailsPanel.populate(data.topReceivables(), data.topPayables());
        }
    }

    private void printForecastPdf() {
        CashFlowForecastOperationsCoordinator.printForecastPdf(this, forecastApiClient, busyBar);
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
}
