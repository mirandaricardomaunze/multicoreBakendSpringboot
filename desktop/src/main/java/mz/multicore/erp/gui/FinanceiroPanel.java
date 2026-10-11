package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.TablePager;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FinanceiroPanel extends JPanel {

    private final FinanceApiClient financeApiClient;
    private final ComercialApiClient comercialApiClient;
    private final mz.multicore.erp.desktop.client.BankReconciliationApiClient bankReconciliationApiClient;
    private final BankReconciliationPanel reconciliationPanel;
    private final CashFlowForecastPanel forecastPanel;
    private final JTabbedPane tabbedPane;

    // Accounts List Elements
    private DefaultTableModel accountsModel;
    private JTable accountsTable;

    // Transaction movements list
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

        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JComponent heading = UIHelper.createHeading("Tesouraria & Conciliação Bancária");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT); feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(heading); north.add(feedback); add(north, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Contas", UIHelper.icon("fas-wallet", 16, UIHelper.APPROVED_GREEN), createAccountsTab());
        tabbedPane.addTab("Fluxo de Caixa", UIHelper.icon("fas-exchange-alt", 16, UIHelper.ACCENT_BLUE),
                createMovementsTab());
        reconciliationPanel = new BankReconciliationPanel(bankReconciliationApiClient, financeApiClient);
        tabbedPane.addTab("Reconciliação Bancária", UIHelper.icon("fas-university", 16, UIHelper.ACCENT),
                reconciliationPanel);
        if (forecastApiClient != null) {
            this.forecastPanel = new CashFlowForecastPanel(forecastApiClient);
            tabbedPane.addTab("Projeção Previsional", UIHelper.icon("fas-chart-line", 16, UIHelper.ACCENT_CYAN), forecastPanel);
        } else {
            this.forecastPanel = null;
        }
        add(tabbedPane, BorderLayout.CENTER);

        // Carregamento preguiçoso: dados por HTTP em onPanelSelected() (via navigate), não no
        // construtor — evita chamada à API no arranque para quem não tem empresa activa.
    }

    /** Aba das contas de tesouraria (tabela ocupa toda a aba). */
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
        TableQuickPeekController.install(accountsTable, accountsCard, (peek, row) -> {
            String nome = String.valueOf(accountsModel.getValueAt(row, 0));
            String tipo = String.valueOf(accountsModel.getValueAt(row, 1));
            String iban = String.valueOf(accountsModel.getValueAt(row, 2));
            String saldo = String.valueOf(accountsModel.getValueAt(row, 3));
            peek.setHeaderIcon("fas-wallet", UIHelper.APPROVED_GREEN);
            peek.setTitle(nome);
            peek.setSubtitle("Tipo: " + tipo);
            peek.setStatus(tipo, UIHelper.ACCENT_BLUE);
            peek.setItems(java.util.List.of(
                    new QuickPeekPanel.PeekItem("IBAN / Conta", iban, false),
                    new QuickPeekPanel.PeekItem("Saldo Actual", saldo, true)
            ));
        });
        panel.add(accountsCard, BorderLayout.CENTER);
        return panel;
    }

    /** Aba do histórico de fluxo de caixa (cabeçalho com acção + tabela). */
    private JPanel createMovementsTab() {
        JPanel movementsPanel = new JPanel(new BorderLayout(0, 10));
        movementsPanel.setOpaque(false);
        movementsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernButton payBtn = UIHelper.createSuccessButton("Registar Recebimento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> registerReceipt());

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
                java.util.List.of(new TableFilter.ColumnFilter(mTipo, 3)),
                java.util.List.of(new TableFilter.PeriodFilter(mPeriodo, 0)));
        JPanel mBar = TableFilter.bar(mSearch, TableFilter.label("Tipo:"), mTipo,
                TableFilter.label("Data:", "fas-calendar-alt"), mPeriodo);
        mBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        movementsCard.add(UIHelper.tableCardTop("Histórico de Fluxo de Caixa", mBar, payBtn), BorderLayout.NORTH);
        movementsCard.add(movScroll, BorderLayout.CENTER);
        movementsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        movementsPager = new TablePager(this::loadTransactionsPage);
        movementsCard.add(movementsPager, BorderLayout.SOUTH);
        TableQuickPeekController.install(movementsTable, movementsCard, (peek, row) -> {
            String data = String.valueOf(movementsModel.getValueAt(row, 0));
            String conta = String.valueOf(movementsModel.getValueAt(row, 1));
            String desc = String.valueOf(movementsModel.getValueAt(row, 2));
            String tipo = String.valueOf(movementsModel.getValueAt(row, 3));
            String val = String.valueOf(movementsModel.getValueAt(row, 4));
            peek.setHeaderIcon("fas-exchange-alt", UIHelper.ACCENT_BLUE);
            peek.setTitle(desc);
            peek.setSubtitle("Conta: " + conta);
            peek.setStatus(tipo, "CREDIT".equalsIgnoreCase(tipo) ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
            peek.setItems(java.util.List.of(
                    new QuickPeekPanel.PeekItem("Data Operação", data, false),
                    new QuickPeekPanel.PeekItem("Tipo Movimento", tipo, false),
                    new QuickPeekPanel.PeekItem("Valor", val, true)
            ));
        });
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
        movementsModel.setRowCount(0);
        if (response == null || response.items() == null) return;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (TreasuryTransactionDTO tx : response.items()) {
            movementsModel.addRow(new Object[]{
                    tx.transactionDate().format(dtf),
                    tx.accountName(),
                    tx.description(),
                    tx.transactionType(),
                    tx.transactionType().equalsIgnoreCase("DEBIT") ? tx.amount() : tx.amount().negate()
            });
        }
    }

    private void applyData(FinanceData data) {
        accountsModel.setRowCount(0);
        accountsList = data.accounts();
        for (TreasuryAccountDTO acc : accountsList) {
            accountsModel.addRow(new Object[]{
                    acc.name(),
                    acc.accountType() == mz.multicore.erp.modules.financeira.model.TreasuryAccountType.CASH
                            ? "CAIXA" : "BANCO",
                    acc.accountNumber(),
                    acc.balance()
            });
        }
        approvedInvoicesList.clear();
        for (InvoiceDTO invoice : data.invoices()) {
            if (invoice.status().isCollectable()) {
                approvedInvoicesList.add(invoice);
            }
        }
    }

    private record FinanceData(List<TreasuryAccountDTO> accounts,
                               List<InvoiceDTO> invoices) {}

    /** Registo de recebimento (liquidação de fatura aprovada) em modal profissional. */
    private void registerReceipt() {
        if (approvedInvoicesList.isEmpty()) {
            feedback.show(FeedbackType.INFO, "Sem recebimentos pendentes",
                    "Não existem faturas aprovadas pendentes de recebimento.", null, null);
            return;
        }
        if (accountsList.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Sem contas de tesouraria",
                    "Configure uma conta de tesouraria antes de registar o recebimento.", null, null);
            return;
        }

        JComboBox<String> invoiceCombo = new JComboBox<>();
        UIHelper.styleComboBox(invoiceCombo);
        for (InvoiceDTO inv : approvedInvoicesList) {
            invoiceCombo.addItem(inv.invoiceNumber() + " - " + inv.clientName() + " (" + String.format("%,.2f MT", inv.totalAmount()) + ")");
        }
        JComboBox<String> accountCombo = new JComboBox<>();
        UIHelper.styleComboBox(accountCombo);
        for (TreasuryAccountDTO acc : accountsList) {
            accountCombo.addItem(acc.name());
        }

        JPanel form = UIHelper.createDialogForm(
                "Fatura Aprovada:", invoiceCombo,
                "Conta de Recebimento:", accountCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Recebimento",
                "fas-money-bill-wave", "Liquidação de fatura aprovada", form)
                .setConfirmButton("Receber", "fas-check");
        dlg.setOnSaveAsync(() -> {
            InvoiceDTO invoice = approvedInvoicesList.get(Math.max(0, invoiceCombo.getSelectedIndex()));
            TreasuryAccountDTO account = accountsList.get(Math.max(0, accountCombo.getSelectedIndex()));
            return () -> {
                financeApiClient.payInvoice(invoice.id(), account.id());
                return null;
            };
        });

        if (dlg.showDialog()) {
            ToastManager.success(this, "Recebimento registado; saldo da conta actualizado.");
            refreshData();
        }
    }

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
