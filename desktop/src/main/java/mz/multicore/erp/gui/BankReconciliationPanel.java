package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.BankReconciliationApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.gui.components.ArrowScrollPanel;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
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
    private final JComboBox<TreasuryAccountDTO> accountCombo = new JComboBox<>();
    private final JComboBox<BankStatementDTO> statementCombo = new JComboBox<>();

    // KPI Labels
    private final JLabel kpiBankBalance = new JLabel("0,00 MT");
    private final JLabel kpiSystemBalance = new JLabel("0,00 MT");
    private final JLabel kpiDifference = new JLabel("0,00 MT");
    private final JLabel kpiPendingCount = new JLabel("0");

    // Tabela e Modelo
    private DefaultTableModel itemsModel;
    private JTable itemsTable;
    private final List<BankStatementItemDTO> currentItems = new ArrayList<>();
    private final List<TreasuryTransactionDTO> treasuryTransactions = new ArrayList<>();

    // Acções de linha
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

        // 1. KPI Cards
        JPanel kpiGrid = buildKpiCards();
        kpiGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(kpiGrid);
        mainContent.add(Box.createVerticalStrut(10));

        // 2. Barra de Controlos e Filtros
        JPanel controlBar = buildControlBar();
        controlBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(controlBar);
        mainContent.add(Box.createVerticalStrut(12));

        // 3. Tabela de Movimentos com altura ampla
        JPanel tableCard = buildTableCard();
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(tableCard);
        mainContent.add(Box.createVerticalStrut(10));

        // 4. Barra de Ações Inferior
        JPanel bottomBar = buildBottomActionBar();
        bottomBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(bottomBar);

        // Painel de Scroll com rastreio de largura integral e setas suaves (SPEC-ASO-001)
        ArrowScrollPanel scroll = new ArrowScrollPanel(mainContent);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildKpiCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 12, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        grid.setBorder(new EmptyBorder(0, 0, 4, 0));

        grid.add(createKpiCard("Saldo no Extracto", kpiBankBalance, "Posição bancária oficial", UIHelper.ACCENT_BLUE, "fas-university"));
        grid.add(createKpiCard("Saldo no Sistema", kpiSystemBalance, "Tesouraria Multicore ERP", UIHelper.TEXT_LIGHT, "fas-book"));
        grid.add(createKpiCard("Diferença", kpiDifference, "Zero indica conciliação perfeita", UIHelper.APPROVED_GREEN, "fas-balance-scale"));
        grid.add(createKpiCard("Movimentos Pendentes", kpiPendingCount, "Itens por reconciliar", UIHelper.PENDING_YELLOW, "fas-hourglass-half"));

        return grid;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor, String iconName) {
        ModernPanel card = new ModernPanel();
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        titleLbl.setForeground(UIHelper.TEXT_MUTED);
        top.add(titleLbl, BorderLayout.WEST);

        JLabel icon = new JLabel(UIHelper.icon(iconName, 14, accentColor));
        top.add(icon, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        valueLabel.setForeground(accentColor);
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        sub.setForeground(UIHelper.TEXT_MUTED);
        card.add(sub, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildControlBar() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        container.setBorder(new EmptyBorder(0, 0, 8, 0));

        // Linha 1: Selecção de Conta e Extracto
        JPanel selectionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        selectionRow.setOpaque(false);

        JLabel accLbl = new JLabel("Conta Bancária:");
        accLbl.setForeground(UIHelper.TEXT_LIGHT);
        accLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        selectionRow.add(accLbl);

        UIHelper.styleComboBox(accountCombo);
        accountCombo.setPreferredSize(new Dimension(280, UIHelper.FORM_CONTROL_HEIGHT));
        accountCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TreasuryAccountDTO acc) {
                    setText(acc.name() + " (" + acc.accountNumber() + ")");
                } else {
                    setText("— Seleccionar Conta —");
                }
                return this;
            }
        });
        accountCombo.addActionListener(e -> onAccountChanged());
        selectionRow.add(accountCombo);

        JLabel stLbl = new JLabel("Extracto:");
        stLbl.setForeground(UIHelper.TEXT_LIGHT);
        stLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        selectionRow.add(stLbl);

        UIHelper.styleComboBox(statementCombo);
        statementCombo.setPreferredSize(new Dimension(280, UIHelper.FORM_CONTROL_HEIGHT));
        statementCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BankStatementDTO st) {
                    setText(st.statementReference() + " (" + st.status() + ")");
                } else {
                    setText("— Sem Extractos —");
                }
                return this;
            }
        });
        statementCombo.addActionListener(e -> onStatementChanged());
        selectionRow.add(statementCombo);

        // Linha 2: Barra de Acções da Reconciliação com largura plena e sem truncamento
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsRow.setOpaque(false);

        ModernButton importBtn = UIHelper.createPrimaryButton("Importar Extracto");
        importBtn.setIcon(UIHelper.icon("fas-file-import", 14));
        importBtn.setPreferredSize(new Dimension(165, UIHelper.FORM_CONTROL_HEIGHT));
        importBtn.addActionListener(e -> openImportDialog());
        actionsRow.add(importBtn);

        autoMatchBtn = UIHelper.createSuccessButton("Auto-Conciliar");
        autoMatchBtn.setIcon(UIHelper.icon("fas-magic", 14));
        autoMatchBtn.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        autoMatchBtn.addActionListener(e -> performAutoMatch());
        actionsRow.add(autoMatchBtn);

        reportBtn = UIHelper.createSecondaryButton("Emitir Relatório (PDF)");
        reportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        reportBtn.setPreferredSize(new Dimension(175, UIHelper.FORM_CONTROL_HEIGHT));
        reportBtn.addActionListener(e -> emitPdfReport());
        actionsRow.add(reportBtn);

        closeBtn = UIHelper.createDangerButton("Fechar Reconciliação");
        closeBtn.setIcon(UIHelper.icon("fas-lock", 14));
        closeBtn.setPreferredSize(new Dimension(185, UIHelper.FORM_CONTROL_HEIGHT));
        closeBtn.addActionListener(e -> closeStatement());
        actionsRow.add(closeBtn);

        container.add(selectionRow);
        container.add(Box.createVerticalStrut(8));
        container.add(actionsRow);

        return container;
    }

    private JPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 580));

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
        scroll.setPreferredSize(new Dimension(0, 440));

        JTextField searchField = TableFilter.searchField("Filtrar descrição ou referência…");
        JComboBox<String> statusFilter = TableFilter.combo("Todos os estados", "APROVADO", "PENDENTE", "INACTIVO");
        JComboBox<String> periodFilter = TableFilter.periodCombo();
        TableFilter.install(itemsTable, searchField,
                List.of(new TableFilter.ColumnFilter(statusFilter, 4)),
                List.of(new TableFilter.PeriodFilter(periodFilter, 0)));
        JPanel searchBar = TableFilter.bar(
                searchField,
                TableFilter.label("Estado:"), statusFilter,
                TableFilter.label("Data:", "fas-calendar-alt"), periodFilter
        );
        searchBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        card.add(searchBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        bar.setBorder(new EmptyBorder(6, 0, 0, 0));

        manualMatchBtn = UIHelper.createPrimaryButton("Conciliar Manualmente");
        manualMatchBtn.setIcon(UIHelper.icon("fas-link", 14));
        manualMatchBtn.setEnabled(false);
        manualMatchBtn.addActionListener(e -> openManualMatchDialog());
        bar.add(manualMatchBtn);

        expenseBtn = UIHelper.createSecondaryButton("Lançar Encargo Bancário");
        expenseBtn.setIcon(UIHelper.icon("fas-receipt", 14));
        expenseBtn.setEnabled(false);
        expenseBtn.addActionListener(e -> openExpenseDialog());
        bar.add(expenseBtn);

        unmatchBtn = UIHelper.createDangerButton("Desfazer Conciliação");
        unmatchBtn.setIcon(UIHelper.icon("fas-undo", 14));
        unmatchBtn.setEnabled(false);
        unmatchBtn.addActionListener(e -> unmatchSelectedItem());
        bar.add(unmatchBtn);

        return bar;
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
                    setText("— Nenhuma transacção compatível encontrada —");
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
