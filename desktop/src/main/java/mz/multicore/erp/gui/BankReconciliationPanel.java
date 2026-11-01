package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.BankReconciliationApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.*;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel moderno para o Centro de Reconciliação Bancária.
 * Orquestrador desacoplado alinhado com o Design System Multicore e rastreabilidade total.
 */
public class BankReconciliationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    final BankReconciliationApiClient apiClient;
    final FinanceApiClient financeApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    final JComboBox<TreasuryAccountDTO> accountCombo = new JComboBox<>();
    final JComboBox<BankStatementDTO> statementCombo = new JComboBox<>();

    final JLabel kpiBankBalance = new JLabel("0,00 MT");
    final JLabel kpiSystemBalance = new JLabel("0,00 MT");
    final JLabel kpiDifference = new JLabel("0,00 MT");
    final JLabel kpiPendingCount = new JLabel("0");

    private DefaultTableModel itemsModel;
    JTable itemsTable;
    final List<BankStatementItemDTO> currentItems = new ArrayList<>();
    final List<TreasuryTransactionDTO> treasuryTransactions = new ArrayList<>();

    ActionMenuButton operationsMenu;
    JComboBox<String> statusFilter;
    ModernButton manualMatchBtn, expenseBtn, unmatchBtn, autoMatchBtn, reportBtn, closeBtn;

    public BankReconciliationPanel(BankReconciliationApiClient apiClient, FinanceApiClient financeApiClient) {
        this.apiClient = apiClient;
        this.financeApiClient = financeApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);

        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(10, 0, 15, 0));

        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(feedback);

        JPanel kpiGrid = buildKpiCards();
        kpiGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(kpiGrid);
        mainContent.add(Box.createVerticalStrut(10));

        JPanel tableCard = buildTableCard();
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(tableCard);

        ArrowScrollPanel scroll = new ArrowScrollPanel(mainContent);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildKpiCards() {
        JPanel grid = KpiCard.createGrid(4);
        grid.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel cardBank = KpiCard.createInteractiveCard("Saldo no Extracto", kpiBankBalance, "Posição bancária oficial", "fas-university", UIHelper.ACCENT_BLUE,
                "Mostrar todos os movimentos", () -> { if (statusFilter != null) statusFilter.setSelectedIndex(0); });
        JPanel cardSys = KpiCard.createCard("Saldo no Sistema", kpiSystemBalance, "Tesouraria Multicore ERP", "fas-book", UIHelper.TEXT_LIGHT);
        JPanel cardDiff = KpiCard.createInteractiveCard("Diferença", kpiDifference, "Zero indica conciliação perfeita", "fas-balance-scale", UIHelper.APPROVED_GREEN,
                "Filtrar movimentos pendentes ou divergentes", () -> { if (statusFilter != null) statusFilter.setSelectedItem("PENDENTE"); });
        JPanel cardPending = KpiCard.createInteractiveCard("Movimentos Pendentes", kpiPendingCount, "Itens por reconciliar", "fas-hourglass-half", UIHelper.PENDING_YELLOW,
                "Filtrar movimentos por reconciliar", () -> { if (statusFilter != null) statusFilter.setSelectedItem("PENDENTE"); });

        grid.add(cardBank); grid.add(cardSys); grid.add(cardDiff); grid.add(cardPending);
        return grid;
    }

    private JPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {"Data", "Descrição do Movimento", "Referência", "Valor", "Estado", "Transacção Vinculada (ERP)"};
        itemsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        itemsTable = new JTable(itemsModel);
        UIHelper.styleTable(itemsTable);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(240);
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(115);
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(115);
        itemsTable.getColumnModel().getColumn(5).setPreferredWidth(230);
        UIHelper.ensureHeadersFit(itemsTable);

        itemsTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        itemsTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        itemsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateRowActionButtons();
        });

        JScrollPane scroll = new JScrollPane(itemsTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(0, 480));

        JPanel cardHeader = new JPanel(new BorderLayout(8, 0));
        cardHeader.setOpaque(false);

        JPanel selectors = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectors.setOpaque(false);
        JLabel accLbl = new JLabel("Conta:");
        accLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        accLbl.setForeground(UIHelper.TEXT_LIGHT);
        selectors.add(accLbl);

        UIHelper.styleComboBox(accountCombo);
        accountCombo.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        accountCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, val, idx, sel, focus);
                setText(val instanceof TreasuryAccountDTO acc ? acc.name() + " (" + acc.accountNumber() + ")" : "Seleccionar Conta");
                return this;
            }
        });
        accountCombo.addActionListener(e -> onAccountChanged());
        selectors.add(accountCombo);

        JLabel stLbl = new JLabel("Extracto:");
        stLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        stLbl.setForeground(UIHelper.TEXT_LIGHT);
        selectors.add(stLbl);

        UIHelper.styleComboBox(statementCombo);
        statementCombo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        statementCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, val, idx, sel, focus);
                setText(val instanceof BankStatementDTO st ? st.statementReference() + " (" + st.status() + ")" : "Sem Extractos");
                return this;
            }
        });
        statementCombo.addActionListener(e -> onStatementChanged());
        selectors.add(statementCombo);
        selectors.add(UIHelper.createRefreshButton(this::refreshData));
        cardHeader.add(selectors, BorderLayout.WEST);

        ModernButton importBtn = UIHelper.createPrimaryButton("Importar Extracto");
        importBtn.setIcon(UIHelper.icon("fas-file-import", 14));
        importBtn.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        importBtn.addActionListener(e -> openImportDialog());

        operationsMenu = UIHelper.createActionMenuButton("Conciliação")
                .addAction("Auto-Conciliar", UIHelper.icon("fas-magic", 14, UIHelper.APPROVED_GREEN), this::performAutoMatch)
                .addAction("Conciliar Manualmente", UIHelper.icon("fas-link", 14, UIHelper.ACCENT_BLUE), this::openManualMatchDialog)
                .addAction("Lançar Encargo Bancário", UIHelper.icon("fas-receipt", 14, UIHelper.PENDING_YELLOW), this::openExpenseDialog)
                .addAction("Desfazer Conciliação", UIHelper.icon("fas-undo", 14, UIHelper.REJECTED_RED), this::unmatchSelectedItem)
                .addAction("Emitir Relatório (PDF)", UIHelper.icon("fas-file-pdf", 14, UIHelper.ACCENT), this::emitPdfReport);

        closeBtn = UIHelper.createDangerButton("Fechar Extracto");
        closeBtn.setIcon(UIHelper.icon("fas-lock", 14));
        closeBtn.setToolTipText("Fechar e trancar este extracto bancário reconciliado");
        closeBtn.addActionListener(e -> closeStatement());

        manualMatchBtn = UIHelper.createPrimaryButton("Conciliar Manualmente");
        manualMatchBtn.addActionListener(e -> openManualMatchDialog());
        expenseBtn = UIHelper.createSecondaryButton("Lançar Encargo");
        expenseBtn.addActionListener(e -> openExpenseDialog());
        unmatchBtn = UIHelper.createDangerButton("Desfazer Vínculo");
        unmatchBtn.addActionListener(e -> unmatchSelectedItem());
        autoMatchBtn = UIHelper.createSuccessButton("Auto-Conciliar");
        autoMatchBtn.addActionListener(e -> performAutoMatch());
        reportBtn = UIHelper.createSecondaryButton("Relatório");
        reportBtn.addActionListener(e -> emitPdfReport());

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerActions.setOpaque(false);
        headerActions.add(importBtn);
        headerActions.add(operationsMenu);
        cardHeader.add(headerActions, BorderLayout.EAST);

        JTextField searchField = TableFilter.searchField("Filtrar descrição ou referência…");
        statusFilter = TableFilter.combo("Todos os estados", "APROVADO", "PENDENTE", "INACTIVO");
        JComboBox<String> periodFilter = TableFilter.periodCombo();
        TableFilter.install(itemsTable, searchField,
                List.of(new TableFilter.ColumnFilter(statusFilter, 4)),
                List.of(new TableFilter.PeriodFilter(periodFilter, 0)));
        JPanel searchBar = TableFilter.bar(searchField, TableFilter.label("Estado:"), statusFilter, TableFilter.label("Data:", "fas-calendar-alt"), periodFilter);
        searchBar.setBorder(new EmptyBorder(6, 0, 8, 0));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da linha seleccionada (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes da linha");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filterRightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterRightActions.setOpaque(false);
        filterRightActions.add(closeBtn);
        filterRightActions.add(peekToggleBtn);

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchBar, BorderLayout.WEST);
        searchRow.add(filterRightActions, BorderLayout.EAST);

        JPanel cardTop = new JPanel(new BorderLayout(0, 6));
        cardTop.setOpaque(false);
        cardTop.add(cardHeader, BorderLayout.NORTH);
        cardTop.add(searchRow, BorderLayout.SOUTH);

        card.add(cardTop, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        TableQuickPeekController reconPeek = TableQuickPeekController.install(itemsTable, card, (peek, modelRow) -> {
            if (modelRow >= 0 && modelRow < itemsModel.getRowCount()) {
                BankStatementItemDTO item = modelRow < currentItems.size() ? currentItems.get(modelRow) : null;
                boolean isMatched = item != null ? item.matchedTransactionId() != null : "APROVADO".equals(itemsModel.getValueAt(modelRow, 4));
                String desc = item != null ? item.description() : String.valueOf(itemsModel.getValueAt(modelRow, 1));
                String date = item != null ? (item.transactionDate() != null ? item.transactionDate().format(DATE_FMT) : "—") : String.valueOf(itemsModel.getValueAt(modelRow, 0));
                String ref = item != null ? (item.reference() != null ? item.reference() : "—") : String.valueOf(itemsModel.getValueAt(modelRow, 2));
                Object amountVal = item != null ? item.amount() : itemsModel.getValueAt(modelRow, 3);
                String amountStr = amountVal instanceof BigDecimal bd ? String.format("%,.2f MT", bd) : String.valueOf(amountVal) + " MT";

                peek.setHeaderIcon("fas-university", UIHelper.ACCENT_BLUE);
                peek.setTitle(desc);
                peek.setSubtitle("Movimento Bancário");
                peek.setStatus(isMatched ? "CONCILIADO" : "PENDENTE", isMatched ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW);

                List<QuickPeekPanel.PeekItem> peekItems = new ArrayList<>();
                peekItems.add(new QuickPeekPanel.PeekItem("Data", date, false));
                peekItems.add(new QuickPeekPanel.PeekItem("Referência", ref, false));
                peekItems.add(new QuickPeekPanel.PeekItem("Valor", amountStr, true));
                if (isMatched) {
                    String linkedTx = "—";
                    if (item != null && item.matchedTransactionId() != null) {
                        linkedTx = treasuryTransactions.stream()
                                .filter(t -> t.id().equals(item.matchedTransactionId()))
                                .findFirst()
                                .map(t -> "#TX-" + t.id() + " (" + t.description() + ")")
                                .orElse("#TX-" + item.matchedTransactionId());
                    } else if (itemsModel.getColumnCount() > 5 && itemsModel.getValueAt(modelRow, 5) != null) {
                        linkedTx = String.valueOf(itemsModel.getValueAt(modelRow, 5));
                    }
                    peekItems.add(new QuickPeekPanel.PeekItem("Transação Vinculada", linkedTx, false));
                    peek.setOnOpenFullAction(ignored -> unmatchSelectedItem());
                } else {
                    peekItems.add(new QuickPeekPanel.PeekItem("Transação Vinculada", "Nenhuma (Pendente)", false));
                    peek.setOnOpenFullAction(ignored -> openManualMatchDialog());
                }
                peek.setItems(peekItems);
            }
        });
        peekToggleBtn.addActionListener(e -> reconPeek.toggle());
        return card;
    }

    public void refreshData() {
        UIHelper.loadAsync(this,
                () -> {
                    List<TreasuryAccountDTO> bankAcc = financeApiClient.getAllAccounts().stream()
                            .filter(a -> a.accountType() != TreasuryAccountType.CASH).toList();
                    return new AccountsPayload(bankAcc, financeApiClient.getAllTransactions());
                },
                payload -> {
                    treasuryTransactions.clear();
                    treasuryTransactions.addAll(payload.transactions());
                    TreasuryAccountDTO prevSel = (TreasuryAccountDTO) accountCombo.getSelectedItem();
                    accountCombo.removeAllItems();
                    for (TreasuryAccountDTO acc : payload.accounts()) {
                        accountCombo.addItem(acc);
                        if (prevSel != null && prevSel.id().equals(acc.id())) accountCombo.setSelectedItem(acc);
                    }
                    if (accountCombo.getSelectedIndex() < 0 && accountCombo.getItemCount() > 0) accountCombo.setSelectedIndex(0);
                    onAccountChanged();
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao carregar contas bancárias", error.getMessage(), "Tentar novamente", this::refreshData));
    }

    void onAccountChanged() {
        TreasuryAccountDTO selectedAcc = (TreasuryAccountDTO) accountCombo.getSelectedItem();
        if (selectedAcc == null) {
            statementCombo.removeAllItems();
            clearItemsTable();
            return;
        }

        UIHelper.loadAsync(this,
                () -> apiClient.getStatements(selectedAcc.id()),
                statements -> {
                    statementCombo.removeAllItems();
                    for (BankStatementDTO st : statements) statementCombo.addItem(st);
                    if (statementCombo.getItemCount() > 0) statementCombo.setSelectedIndex(0);
                    else clearItemsTable();
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao carregar extractos", error.getMessage(), null, null));
    }

    void onStatementChanged() {
        BankStatementDTO st = (BankStatementDTO) statementCombo.getSelectedItem();
        if (st == null) {
            clearItemsTable();
            return;
        }

        UIHelper.loadAsync(this,
                () -> new StatementPayload(apiClient.getItems(st.id()), apiClient.getSummary(st.id())),
                payload -> {
                    currentItems.clear();
                    currentItems.addAll(payload.items());
                    renderItemsTable(payload.items());
                    updateKpis(payload.summary());
                    updateRowActionButtons();
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao carregar dados do extracto", error.getMessage(), null, null));
    }

    private void renderItemsTable(List<BankStatementItemDTO> items) {
        itemsModel.setRowCount(0);
        for (BankStatementItemDTO it : items) {
            String linkedTx = it.matchedTransactionId() != null ? "#TX-" + it.matchedTransactionId() : "—";
            String estadoStr = switch (it.status()) {
                case MATCHED -> "APROVADO";
                case UNMATCHED -> "PENDENTE";
                case IGNORED -> "INACTIVO";
            };
            itemsModel.addRow(new Object[]{
                    it.transactionDate() != null ? it.transactionDate().format(DATE_FMT) : "—",
                    it.description(),
                    it.reference() != null ? it.reference() : "—",
                    it.amount(),
                    estadoStr,
                    linkedTx
            });
        }
    }

    private void updateKpis(BankReconciliationSummaryDTO s) {
        if (s == null) return;
        kpiBankBalance.setText(String.format("%,.2f MT", s.bankBalance()));
        kpiSystemBalance.setText(String.format("%,.2f MT", s.systemBalance()));
        kpiDifference.setText(String.format("%,.2f MT", s.difference()));
        kpiDifference.setForeground(s.difference().compareTo(BigDecimal.ZERO) == 0 ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
        kpiPendingCount.setText(String.valueOf(s.totalPendingItems()));
        kpiPendingCount.setForeground(s.totalPendingItems() > 0 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);

        BankStatementDTO currentSt = (BankStatementDTO) statementCombo.getSelectedItem();
        boolean isClosed = currentSt != null && currentSt.status() == BankReconciliationStatus.CLOSED;
        closeBtn.setEnabled(!isClosed && s.difference().compareTo(BigDecimal.ZERO) == 0);
        autoMatchBtn.setEnabled(!isClosed);
    }

    private void clearItemsTable() {
        itemsModel.setRowCount(0);
        currentItems.clear();
        kpiBankBalance.setText("0,00 MT");
        kpiSystemBalance.setText("0,00 MT");
        kpiDifference.setText("0,00 MT");
        kpiDifference.setForeground(UIHelper.APPROVED_GREEN);
        kpiPendingCount.setText("0");
        updateRowActionButtons();
    }

    void updateRowActionButtons() { BankReconciliationOperationsCoordinator.updateRowActionButtons(this); }
    void setRowButtonsEnabled(boolean match, boolean expense, boolean unmatch) {
        manualMatchBtn.setEnabled(match);
        expenseBtn.setEnabled(expense);
        unmatchBtn.setEnabled(unmatch);
    }

    private void performAutoMatch() { BankReconciliationOperationsCoordinator.performAutoMatch(this); }
    private void emitPdfReport() { BankReconciliationOperationsCoordinator.emitPdfReport(this); }
    private void closeStatement() { BankReconciliationOperationsCoordinator.closeStatement(this); }
    void unmatchSelectedItem() { BankReconciliationOperationsCoordinator.unmatchSelectedItem(this); }
    void openManualMatchDialog() { BankReconciliationMatchDialogs.openManualMatchDialog(this); }
    void openExpenseDialog() { BankReconciliationMatchDialogs.openExpenseDialog(this); }
    private void openImportDialog() { BankStatementImportDialog.show(this); }
    void showNotice(FeedbackType type, String title, String msg) { feedback.show(type, title, msg, null, null); }

    private record AccountsPayload(List<TreasuryAccountDTO> accounts, List<TreasuryTransactionDTO> transactions) {}
    private record StatementPayload(List<BankStatementItemDTO> items, BankReconciliationSummaryDTO summary) {}
}
