package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.BankReconciliationApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ArrowScrollPanel;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableQuickFilterBar;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.BankReconciliationSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemImportDTO;
import mz.multicore.erp.modules.financeira.dto.CreateBankExpenseAndMatchRequest;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.dto.ManualReconciliationRequest;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Painel moderno para o Centro de Reconciliação Bancária.
 * Permite importar extractos oficiais dos bancos moçambicanos (BIM, BCI, Standard Bank, Moza),
 * realizar conciliação automática e manual, criar lançamentos de encargos em lote e emitir relatórios PDF.
 */
public class BankReconciliationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BankReconciliationApiClient apiClient;
    private final FinanceApiClient financeApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    // Filtros e selecção
    final JComboBox<TreasuryAccountDTO> accountCombo = new JComboBox<>();
    final JComboBox<BankStatementDTO> statementCombo = new JComboBox<>();

    // KPI Labels
    final JLabel kpiBankBalance = new JLabel("0,00 MT");
    final JLabel kpiSystemBalance = new JLabel("0,00 MT");
    final JLabel kpiDifference = new JLabel("0,00 MT");
    final JLabel kpiPendingCount = new JLabel("0");

    // Tabela e Modelo
    private DefaultTableModel itemsModel;
    JTable itemsTable;
    private final List<BankStatementItemDTO> currentItems = new ArrayList<>();
    private final List<TreasuryTransactionDTO> treasuryTransactions = new ArrayList<>();

    // Acções de linha
    ActionMenuButton operationsMenu;
    JComboBox<String> statusFilter;
    private ModernButton manualMatchBtn;
    private ModernButton expenseBtn;
    private ModernButton unmatchBtn;
    private ModernButton autoMatchBtn;
    private ModernButton reportBtn;
    private ModernButton closeBtn;

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

        // 1. KPI Cards com Drilldown Interactivo
        JPanel kpiGrid = buildKpiCards();
        kpiGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(kpiGrid);
        mainContent.add(Box.createVerticalStrut(10));

        // 2. Tabela de Movimentos e Ações no Card Unificado (elimina barras soltas exteriores)
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

        grid.add(cardBank);
        grid.add(cardSys);
        grid.add(cardDiff);
        grid.add(cardPending);

        return grid;
    }

    private JPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {"Data", "Descrição do Movimento", "Referência", "Valor", "Estado", "Transacção Vinculada (ERP)"};
        itemsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
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
            if (!e.getValueIsAdjusting()) {
                updateRowActionButtons();
            }
        });

        JScrollPane scroll = new JScrollPane(itemsTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(0, 480));

        // 1. Linha Superior: Seletores de Conta/Extracto à esquerda e Ações Primárias à direita
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
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TreasuryAccountDTO acc) {
                    setText(acc.name() + " (" + acc.accountNumber() + ")");
                } else {
                    setText("Seleccionar Conta");
                }
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
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BankStatementDTO st) {
                    setText(st.statementReference() + " (" + st.status() + ")");
                } else {
                    setText("Sem Extractos");
                }
                return this;
            }
        });
        statementCombo.addActionListener(e -> onStatementChanged());
        selectors.add(statementCombo);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshData);
        selectors.add(refreshBtn);

        cardHeader.add(selectors, BorderLayout.WEST);

        ModernButton importBtn = UIHelper.createPrimaryButton("Importar Extracto");
        importBtn.setIcon(UIHelper.icon("fas-file-import", 14));
        importBtn.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        importBtn.addActionListener(e -> openImportDialog());

        operationsMenu = UIHelper.createActionMenuButton("Operações")
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

        // 2. Filtros, Pesquisa e Ações de Fecho
        JTextField searchField = TableFilter.searchField("Filtrar descrição ou referência…");
        statusFilter = TableFilter.combo("Todos os estados", "APROVADO", "PENDENTE", "INACTIVO");
        JComboBox<String> periodFilter = TableFilter.periodCombo();
        TableFilter.install(itemsTable, searchField,
                List.of(new TableFilter.ColumnFilter(statusFilter, 4)),
                List.of(new TableFilter.PeriodFilter(periodFilter, 0)));
        JPanel searchBar = TableFilter.bar(
                searchField,
                TableFilter.label("Estado:"), statusFilter,
                TableFilter.label("Data:", "fas-calendar-alt"), periodFilter
        );
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

        // 3. Quick Peek Silencioso com Tecla Espaço
        TableQuickPeekController reconPeek = TableQuickPeekController.install(itemsTable, card, (peek, modelRow) -> {
            if (modelRow >= 0 && modelRow < currentItems.size()) {
                BankStatementItemDTO item = currentItems.get(modelRow);
                boolean isMatched = item.matchedTransactionId() != null;
                peek.setHeaderIcon("fas-university", UIHelper.ACCENT_BLUE);
                peek.setTitle(item.description());
                peek.setSubtitle("Movimento Bancário");
                peek.setStatus(isMatched ? "CONCILIADO" : "PENDENTE", isMatched ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW);

                List<QuickPeekPanel.PeekItem> peekItems = new ArrayList<>();
                peekItems.add(new QuickPeekPanel.PeekItem("Data", item.transactionDate() != null ? item.transactionDate().format(DATE_FMT) : "—", false));
                peekItems.add(new QuickPeekPanel.PeekItem("Referência", item.reference() != null ? item.reference() : "—", false));
                peekItems.add(new QuickPeekPanel.PeekItem("Valor", String.format("%,.2f MT", item.amount()), true));
                if (isMatched) {
                    String linkedTx = treasuryTransactions.stream()
                            .filter(t -> t.id().equals(item.matchedTransactionId()))
                            .findFirst()
                            .map(t -> "#TX-" + t.id() + " (" + t.description() + ")")
                            .orElse("#TX-" + item.matchedTransactionId());
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
                    List<TreasuryAccountDTO> allAcc = financeApiClient.getAllAccounts();
                    List<TreasuryAccountDTO> bankAcc = allAcc.stream()
                            .filter(a -> a.accountType() != TreasuryAccountType.CASH)
                            .toList();
                    List<TreasuryTransactionDTO> txs = financeApiClient.getAllTransactions();
                    return new AccountsPayload(bankAcc, txs);
                },
                payload -> {
                    treasuryTransactions.clear();
                    treasuryTransactions.addAll(payload.transactions());

                    TreasuryAccountDTO prevSel = (TreasuryAccountDTO) accountCombo.getSelectedItem();
                    accountCombo.removeAllItems();
                    for (TreasuryAccountDTO acc : payload.accounts()) {
                        accountCombo.addItem(acc);
                        if (prevSel != null && prevSel.id().equals(acc.id())) {
                            accountCombo.setSelectedItem(acc);
                        }
                    }
                    if (accountCombo.getSelectedIndex() < 0 && accountCombo.getItemCount() > 0) {
                        accountCombo.setSelectedIndex(0);
                    }
                    onAccountChanged();
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao carregar contas bancárias",
                        error.getMessage(), "Tentar novamente", this::refreshData));
    }

    private void onAccountChanged() {
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
                    for (BankStatementDTO st : statements) {
                        statementCombo.addItem(st);
                    }
                    if (statementCombo.getItemCount() > 0) {
                        statementCombo.setSelectedIndex(0);
                    } else {
                        clearItemsTable();
                    }
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao carregar extractos",
                        error.getMessage(), null, null));
    }

    private void onStatementChanged() {
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
                error -> feedback.show(FeedbackType.ERROR, "Erro ao carregar dados do extracto",
                        error.getMessage(), null, null));
    }

    private void renderItemsTable(List<BankStatementItemDTO> items) {
        itemsModel.setRowCount(0);
        for (BankStatementItemDTO it : items) {
            String linkedTx = it.matchedTransactionId() != null
                    ? "#TX-" + it.matchedTransactionId()
                    : "—";

            String estadoStr = switch (it.status()) {
                case MATCHED -> "APROVADO"; // rendered green in TableCellRenderers
                case UNMATCHED -> "PENDENTE"; // rendered yellow in TableCellRenderers
                case IGNORED -> "INACTIVO"; // rendered muted
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

        if (s.difference().compareTo(BigDecimal.ZERO) == 0) {
            kpiDifference.setForeground(UIHelper.APPROVED_GREEN);
        } else {
            kpiDifference.setForeground(UIHelper.REJECTED_RED);
        }

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

    private void updateRowActionButtons() {
        int row = itemsTable.getSelectedRow();
        if (row < 0 || row >= currentItems.size()) {
            manualMatchBtn.setEnabled(false);
            expenseBtn.setEnabled(false);
            unmatchBtn.setEnabled(false);
            return;
        }

        int modelRow = itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = currentItems.get(modelRow);
        BankStatementDTO st = (BankStatementDTO) statementCombo.getSelectedItem();
        boolean isClosed = st != null && st.status() == BankReconciliationStatus.CLOSED;

        if (isClosed) {
            manualMatchBtn.setEnabled(false);
            expenseBtn.setEnabled(false);
            unmatchBtn.setEnabled(false);
            return;
        }

        if (item.status() == BankStatementItemStatus.UNMATCHED) {
            manualMatchBtn.setEnabled(true);
            expenseBtn.setEnabled(item.amount().compareTo(BigDecimal.ZERO) < 0);
            unmatchBtn.setEnabled(false);
        } else if (item.status() == BankStatementItemStatus.MATCHED) {
            manualMatchBtn.setEnabled(false);
            expenseBtn.setEnabled(false);
            unmatchBtn.setEnabled(true);
        } else {
            manualMatchBtn.setEnabled(false);
            expenseBtn.setEnabled(false);
            unmatchBtn.setEnabled(false);
        }

        if (operationsMenu != null) {
            operationsMenu.setActionEnabled(0, !isClosed);
            operationsMenu.setActionEnabled(1, manualMatchBtn.isEnabled());
            operationsMenu.setActionEnabled(2, expenseBtn.isEnabled());
            operationsMenu.setActionEnabled(3, unmatchBtn.isEnabled());
        }
    }

    private void performAutoMatch() {
        BankStatementDTO st = (BankStatementDTO) statementCombo.getSelectedItem();
        if (st == null) {
            feedback.show(FeedbackType.WARNING, "Selecção necessária", "Seleccione um extracto para auto-conciliar.", null, null);
            return;
        }

        UIHelper.loadAsync(this,
                () -> apiClient.autoMatch(st.id()),
                res -> {
                    Object count = res != null ? res.get("matchedCount") : 0;
                    ToastManager.success(this, "Auto-conciliação concluída: " + count + " movimentos associados.");
                    onStatementChanged();
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao auto-conciliar", error.getMessage(), null, null));
    }

    private void emitPdfReport() {
        BankStatementDTO st = (BankStatementDTO) statementCombo.getSelectedItem();
        if (st == null) {
            feedback.show(FeedbackType.WARNING, "Selecção necessária", "Seleccione um extracto para emitir relatório.", null, null);
            return;
        }

        UIHelper.loadAsync(this,
                () -> apiClient.downloadReportPdf(st.id()),
                pdfBytes -> {
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(this, pdfBytes, "conciliacao-bancaria-" + st.statementReference(), "Relatório de Reconciliação Bancária");
                    } else {
                        ToastManager.show(this, FeedbackType.WARNING, "O servidor não gerou o ficheiro PDF.");
                    }
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao gerar relatório", error.getMessage(), null, null));
    }

    private void closeStatement() {
        BankStatementDTO st = (BankStatementDTO) statementCombo.getSelectedItem();
        if (st == null) return;

        boolean confirmed = mz.multicore.erp.gui.components.ModernMessageDialog.confirm(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                FeedbackType.WARNING,
                "Fechar Reconciliação",
                "Tem a certeza que deseja fechar este extracto (" + st.statementReference() + ")?\n"
                        + "Uma vez fechado, as vinculações não poderão ser alteradas.",
                "Fechar Extracto");
        if (!confirmed) return;

        UIHelper.loadAsync(this,
                () -> apiClient.closeStatement(st.id()),
                closed -> {
                    ToastManager.success(this, "Extracto bancário fechado e reconciliado com sucesso.");
                    onAccountChanged();
                },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível fechar extracto", error.getMessage(), null, null));
    }

    private void unmatchSelectedItem() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = currentItems.get(modelRow);

        UIHelper.loadAsync(this,
                () -> apiClient.unmatch(item.id()),
                unmatched -> {
                    ToastManager.success(this, "Conciliação desfeita com sucesso.");
                    onStatementChanged();
                },
                error -> feedback.show(FeedbackType.ERROR, "Erro ao desfazer conciliação", error.getMessage(), null, null));
    }

    private void openManualMatchDialog() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = currentItems.get(modelRow);

        TreasuryAccountDTO acc = (TreasuryAccountDTO) accountCombo.getSelectedItem();
        if (acc == null) return;

        // Procura transacções da mesma conta com valor igual em módulo
        BigDecimal targetAmt = item.amount().abs();
        List<TreasuryTransactionDTO> candidates = treasuryTransactions.stream()
                .filter(t -> t.accountId().equals(acc.id()))
                .filter(t -> t.amount().compareTo(targetAmt) == 0)
                .toList();

        JComboBox<TreasuryTransactionDTO> txCombo = new JComboBox<>();
        UIHelper.styleComboBox(txCombo);
        txCombo.setPreferredSize(new Dimension(420, UIHelper.FORM_CONTROL_HEIGHT));
        txCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TreasuryTransactionDTO t) {
                    setText("#TX-" + t.id() + " | " + t.transactionDate().format(DATE_FMT) + " | " + t.description() + " (" + String.format("%,.2f MT", t.amount()) + ")");
                } else {
                    setText("Nenhuma transacção compatível encontrada");
                }
                return this;
            }
        });

        for (TreasuryTransactionDTO t : candidates) {
            txCombo.addItem(t);
        }

        JPanel form = UIHelper.createDialogForm(
                "Item do Extracto:", new JLabel(item.description() + " (" + String.format("%,.2f MT", item.amount()) + ")"),
                "Transacção de Tesouraria:", txCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Conciliar Manualmente",
                "fas-link", "Associe este movimento bancário a uma transacção da tesouraria", form)
                .setConfirmButton("Vincular", "fas-check");

        dlg.setOnSaveAsync(() -> {
            TreasuryTransactionDTO selectedTx = (TreasuryTransactionDTO) txCombo.getSelectedItem();
            if (selectedTx == null) {
                throw new IllegalArgumentException("Seleccione uma transacção para vincular.");
            }
            return () -> apiClient.manualMatch(new ManualReconciliationRequest(item.id(), selectedTx.id()));
        });

        if (dlg.showDialog()) {
            ToastManager.success(this, "Movimento conciliado manualmente!");
            onStatementChanged();
        }
    }

    private void openExpenseDialog() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = itemsTable.convertRowIndexToModel(row);
        BankStatementItemDTO item = currentItems.get(modelRow);

        JTextField descField = new JTextField(item.description());
        UIHelper.styleTextField(descField);
        descField.setPreferredSize(new Dimension(380, UIHelper.FORM_CONTROL_HEIGHT));

        BigDecimal feeAmt = item.amount().abs();
        JTextField amtField = new JTextField(feeAmt.toString());
        UIHelper.styleTextField(amtField);
        amtField.setPreferredSize(new Dimension(380, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel form = UIHelper.createDialogForm(
                "Descrição do Encargo:", descField,
                "Valor do Encargo (MT):", amtField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Lançar Encargo Bancário",
                "fas-receipt", "Cria saída na tesouraria e concilia imediatamente o movimento", form)
                .setConfirmButton("Lançar e Conciliar", "fas-check");

        dlg.setOnSaveAsync(() -> {
            String desc = descField.getText().trim();
            BigDecimal amount = new BigDecimal(amtField.getText().trim());
            return () -> apiClient.createExpenseAndMatch(new CreateBankExpenseAndMatchRequest(item.id(), desc, amount));
        });

        if (dlg.showDialog()) {
            ToastManager.success(this, "Encargo bancário debitado e conciliado!");
            onStatementChanged();
        }
    }

    private void openImportDialog() {
        TreasuryAccountDTO acc = (TreasuryAccountDTO) accountCombo.getSelectedItem();
        if (acc == null) {
            feedback.show(FeedbackType.WARNING, "Selecção necessária", "Seleccione a conta bancária antes de importar extracto.", null, null);
            return;
        }

        JTextField refField = new JTextField("EXT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        UIHelper.styleTextField(refField);
        refField.setPreferredSize(new Dimension(360, UIHelper.FORM_CONTROL_HEIGHT));

        JTextField startField = new JTextField(LocalDate.now().withDayOfMonth(1).toString());
        UIHelper.styleTextField(startField);
        startField.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));

        JTextField endField = new JTextField(LocalDate.now().toString());
        UIHelper.styleTextField(endField);
        endField.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel datesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        datesPanel.setOpaque(false);
        datesPanel.add(startField);
        datesPanel.add(new JLabel("até"));
        datesPanel.add(endField);

        JTextField openBalField = new JTextField("0.00");
        UIHelper.styleTextField(openBalField);
        openBalField.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));

        JTextField closeBalField = new JTextField("0.00");
        UIHelper.styleTextField(closeBalField);
        closeBalField.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel balPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        balPanel.setOpaque(false);
        balPanel.add(openBalField);
        balPanel.add(new JLabel("Final:"));
        balPanel.add(closeBalField);

        JTextArea csvArea = new JTextArea(8, 35);
        csvArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        csvArea.setBackground(UIHelper.FIELD_BG);
        csvArea.setForeground(UIHelper.TEXT_LIGHT);
        csvArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        csvArea.setText("# Formato: Data;Descrição;Referência;Valor\n"
                + LocalDate.now() + ";Depósito Cliente ABC;DEP-9988;25000.00\n"
                + LocalDate.now() + ";Comissão de Manutenção;;-150.00");

        JScrollPane csvScroll = new JScrollPane(csvArea);
        UIHelper.styleScrollPane(csvScroll);

        ModernButton loadCsvFileBtn = UIHelper.createSecondaryButton("Carregar Ficheiro CSV");
        loadCsvFileBtn.setIcon(UIHelper.icon("fas-folder-open", 13));
        loadCsvFileBtn.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        loadCsvFileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros de Extracto (*.csv, *.txt)", "csv", "txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File f = chooser.getSelectedFile();
                    String content = Files.readString(f.toPath());
                    csvArea.setText(content);
                } catch (Exception ex) {
                    ToastManager.show(this, FeedbackType.ERROR, "Erro ao ler ficheiro: " + ex.getMessage());
                }
            }
        });

        JPanel csvBox = new JPanel(new BorderLayout(0, 5));
        csvBox.setOpaque(false);
        csvBox.add(loadCsvFileBtn, BorderLayout.NORTH);
        csvBox.add(csvScroll, BorderLayout.CENTER);

        JPanel form = UIHelper.createDialogForm(
                "Referência do Extracto:", refField,
                "Período (Início / Fim):", datesPanel,
                "Saldos (Inicial / Final):", balPanel,
                "Linhas do Extracto:", csvBox
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Importar Extracto Bancário",
                "fas-file-import", "Carregue o ficheiro oficial ou cole os movimentos do extracto", form)
                .setConfirmButton("Importar e Validar", "fas-check");

        dlg.setOnSaveAsync(() -> {
            String ref = refField.getText().trim();
            LocalDate sDate = LocalDate.parse(startField.getText().trim());
            LocalDate eDate = LocalDate.parse(endField.getText().trim());
            BigDecimal oBal = new BigDecimal(openBalField.getText().trim());
            BigDecimal cBal = new BigDecimal(closeBalField.getText().trim());

            List<BankStatementItemImportDTO> items = parseCsvLines(csvArea.getText());
            if (items.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma linha de movimento válida foi detectada no extracto.");
            }

            ImportBankStatementRequest req = new ImportBankStatementRequest(acc.id(), ref, sDate, eDate, oBal, cBal, items);
            return () -> apiClient.importStatement(req);
        });

        if (dlg.showDialog()) {
            ToastManager.success(this, "Extracto bancário importado com sucesso!");
            onAccountChanged();
        }
    }

    private List<BankStatementItemImportDTO> parseCsvLines(String raw) {
        List<BankStatementItemImportDTO> result = new ArrayList<>();
        String[] lines = raw.split("\r?\n");
        for (String line : lines) {
            String clean = line.trim();
            if (clean.isEmpty() || clean.startsWith("#") || clean.toLowerCase().startsWith("data")) {
                continue;
            }
            String[] tokens = clean.contains(";") ? clean.split(";") : clean.contains(",") ? clean.split(",") : clean.split("\t");
            if (tokens.length >= 2) {
                LocalDate date = LocalDate.parse(tokens[0].trim());
                String desc = tokens[1].trim();
                String ref = tokens.length >= 3 && !tokens[2].trim().isEmpty() ? tokens[2].trim() : null;
                BigDecimal amt = tokens.length >= 4 ? new BigDecimal(tokens[3].trim()) : new BigDecimal(tokens[2].trim());
                result.add(new BankStatementItemImportDTO(date, date, desc, ref, amt, null));
            }
        }
        return result;
    }

    private record AccountsPayload(List<TreasuryAccountDTO> accounts, List<TreasuryTransactionDTO> transactions) {}
    private record StatementPayload(List<BankStatementItemDTO> items, BankReconciliationSummaryDTO summary) {}
}
