package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FinanceiroPanel extends JPanel {

    private final FinanceApiClient financeApiClient;
    private final ComercialApiClient comercialApiClient;
    private final mz.multicore.erp.desktop.client.BankReconciliationApiClient bankReconciliationApiClient;
    private final BankReconciliationPanel reconciliationPanel;
    private final CashFlowForecastPanel forecastPanel;
    private final JTabbedPane tabbedPane;

    private DefaultTableModel accountsModel;
    private JTable accountsTable;

    private DefaultTableModel movementsModel;
    private JTable movementsTable;
    private TablePager movementsPager;

    private List<InvoiceDTO> approvedInvoicesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public FinanceiroPanel(FinanceApiClient financeApiClient,
                           ComercialApiClient comercialApiClient,
                           mz.multicore.erp.desktop.client.BankReconciliationApiClient bankReconciliationApiClient) {
        this(financeApiClient, comercialApiClient, bankReconciliationApiClient, null);
    }

    public FinanceiroPanel(FinanceApiClient financeApiClient,
                           ComercialApiClient comercialApiClient,
                           mz.multicore.erp.desktop.client.BankReconciliationApiClient bankReconciliationApiClient,
                           mz.multicore.erp.desktop.client.CashFlowForecastApiClient forecastApiClient) {
        this.financeApiClient = financeApiClient;
        this.comercialApiClient = comercialApiClient;
        this.bankReconciliationApiClient = bankReconciliationApiClient;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JComponent heading = UIHelper.createHeading("Tesouraria & Conciliação Bancária");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(heading);
        north.add(feedback);
        add(north, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Contas", UIHelper.icon("fas-wallet", 16, UIHelper.APPROVED_GREEN), createAccountsTab());
        tabbedPane.addTab("Fluxo de Caixa", UIHelper.icon("fas-exchange-alt", 16, UIHelper.ACCENT_BLUE), createMovementsTab());
        reconciliationPanel = new BankReconciliationPanel(bankReconciliationApiClient, financeApiClient);
        tabbedPane.addTab("Reconciliação Bancária", UIHelper.icon("fas-university", 16, UIHelper.ACCENT), reconciliationPanel);
        if (forecastApiClient != null) {
            this.forecastPanel = new CashFlowForecastPanel(forecastApiClient);
            tabbedPane.addTab("Projeção Previsional", UIHelper.icon("fas-chart-line", 16, UIHelper.ACCENT_CYAN), forecastPanel);
        } else {
            this.forecastPanel = null;
        }
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createAccountsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernPanel accountsCard = new ModernPanel(16);
        accountsCard.setLayout(new BorderLayout());
        accountsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] accountCols = {"Conta de Tesouraria", "Tipo", "IBAN / Nº Conta", "Saldo Atual"};
        accountsModel = new DefaultTableModel(accountCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        accountsTable = new JTable(accountsModel);
        UIHelper.styleTable(accountsTable);
        UIHelper.ensureHeadersFit(accountsTable);
        accountsTable.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.status());
        accountsTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        JScrollPane accScroll = new JScrollPane(accountsTable);
        UIHelper.styleScrollPane(accScroll);
        JTextField aSearch = TableFilter.searchField("Conta ou IBAN…");
        TableFilter.install(accountsTable, aSearch);
        JPanel aBar = TableFilter.bar(aSearch);
        aBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        accountsCard.add(UIHelper.tableCardTop("Contas de Tesouraria & Saldos", aBar), BorderLayout.NORTH);
        accountsCard.add(accScroll, BorderLayout.CENTER);
        TableQuickPeekController.install(accountsTable, accountsCard, (peek, row) ->
                TreasuryOperationsCoordinator.populateAccountsPeek(peek, accountsModel, row));
        panel.add(accountsCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMovementsTab() {
        JPanel movementsPanel = new JPanel(new BorderLayout(0, 10));
        movementsPanel.setOpaque(false);
        movementsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernButton payBtn = UIHelper.createSuccessButton("Registar Recebimento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> TreasuryReceiptDialog.show(
                this, financeApiClient, approvedInvoicesList, accountsList, feedback, this::refreshData));

        ModernPanel movementsCard = new ModernPanel(16);
        movementsCard.setLayout(new BorderLayout());
        movementsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] movementCols = {"Data", "Conta", "Descrição", "Tipo", "Valor"};
        movementsModel = new DefaultTableModel(movementCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movementsTable = new JTable(movementsModel);
        movementsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(movementsTable);
        UIHelper.ensureHeadersFit(movementsTable);
        movementsTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        movementsTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        JScrollPane movScroll = new JScrollPane(movementsTable);
        UIHelper.styleScrollPane(movScroll);

        JTextField mSearch = TableFilter.searchField("Conta ou descrição…");
        JComboBox<String> mTipo = TableFilter.combo("Todos os tipos", "Débito", "Crédito");
        JComboBox<String> mPeriodo = TableFilter.periodCombo();
        TableFilter.install(movementsTable, mSearch,
                List.of(new TableFilter.ColumnFilter(mTipo, 3)),
                List.of(new TableFilter.PeriodFilter(mPeriodo, 0)));
        JPanel mBar = TableFilter.bar(mSearch, TableFilter.label("Tipo:"), mTipo,
                TableFilter.label("Data:", "fas-calendar-alt"), mPeriodo);
        mBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        movementsCard.add(UIHelper.tableCardTop("Histórico de Fluxo de Caixa", mBar, payBtn), BorderLayout.NORTH);
        movementsCard.add(movScroll, BorderLayout.CENTER);
        movementsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        movementsPager = new TablePager(this::loadTransactionsPage);
        movementsCard.add(movementsPager, BorderLayout.SOUTH);
        TableQuickPeekController.install(movementsTable, movementsCard, (peek, row) ->
                TreasuryOperationsCoordinator.populateMovementsPeek(peek, movementsModel, row));
        movementsPanel.add(movementsCard, BorderLayout.CENTER);
        return movementsPanel;
    }

    public void refreshData() {
        UIHelper.loadAsync(this,
                () -> new FinanceData(financeApiClient.getAllAccounts(), comercialApiClient.getAllInvoices()),
                this::applyData,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar a tesouraria",
                        error.getMessage(), "Tentar novamente", this::refreshData));
        if (movementsPager != null) {
            movementsPager.reload();
        } else {
            loadTransactionsPage(0, 50);
        }
    }

    private void loadTransactionsPage(int page, int size) {
        UIHelper.loadAsync(this,
                () -> financeApiClient.getTransactionsPage(page, size),
                this::applyTransactionsPage,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar transações",
                        error.getMessage(), "Tentar novamente", () -> {
                            if (movementsPager != null) movementsPager.reload();
                        }));
    }

    private void applyTransactionsPage(PageResponse<TreasuryTransactionDTO> response) {
        if (movementsPager != null) {
            movementsPager.apply(response);
        }
        TreasuryOperationsCoordinator.populateTransactionsModel(
                movementsModel, response != null ? response.items() : null);
    }

    private void applyData(FinanceData data) {
        accountsList = data.accounts();
        TreasuryOperationsCoordinator.populateAccountsModel(accountsModel, accountsList);
        approvedInvoicesList = TreasuryOperationsCoordinator.extractCollectableInvoices(data.invoices());
    }

    private record FinanceData(List<TreasuryAccountDTO> accounts, List<InvoiceDTO> invoices) {}

    public void showReconciliationTab() {
        if (tabbedPane != null && tabbedPane.getTabCount() >= 3) {
            tabbedPane.setSelectedIndex(2);
        }
    }

    public void showForecastTab() {
        if (tabbedPane != null && tabbedPane.getTabCount() >= 4) {
            tabbedPane.setSelectedIndex(3);
        }
    }

    public CashFlowForecastPanel getForecastPanel() {
        return forecastPanel;
    }

    public void onPanelSelected() {
        refreshData();
        if (reconciliationPanel != null) {
            reconciliationPanel.refreshData();
        }
        if (forecastPanel != null) {
            forecastPanel.loadForecast();
        }
    }
}
