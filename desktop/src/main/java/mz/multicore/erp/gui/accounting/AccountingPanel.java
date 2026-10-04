package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contabilidade: plano de contas, diário, razão e balancete.
 *
 * <p>Cliente-fino: tudo por HTTP, nenhuma regra aqui. As quatro abas são leituras do que o
 * backend calcula — a partida dobrada, a natureza dos saldos e os totais vivem lá.
 */
public final class AccountingPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingApiClient accountingApiClient;

    private final DefaultTableModel accountsModel = table("Código", "Nome", "Classe", "Natureza", "Movimentável");
    private final DefaultTableModel journalModel = table("Nº", "Data", "Descrição", "Origem", "Documento", "Débito", "Crédito");
    private final DefaultTableModel balanceModel = table("Conta", "Nome", "Classe", "Débito", "Crédito", "Saldo");
    private final DefaultTableModel ledgerModel = table("Data", "Lançamento", "Descrição", "Documento", "Débito", "Crédito", "Saldo");
    private final DefaultTableModel incomeModel = table("Secção", "Conta", "Nome", "Valor");
    private final DefaultTableModel statementBalanceModel = table("Secção", "Conta", "Nome", "Valor");

    private final JLabel balanceSummary = mutedLabel();
    private final JLabel ledgerSummary = mutedLabel();
    private final JTextField ledgerAccount = new JTextField("2101", 8);
    private final DateField balanceFrom = new DateField();
    private final DateField balanceTo = new DateField();
    private final DateField ledgerFrom = new DateField();
    private final DateField ledgerTo = new DateField();
    private final DateField statementsFrom = new DateField();
    private final DateField statementsTo = new DateField();
    private final JLabel statementsSummary = mutedLabel();
    private TablePager journalPager;
    private final JLabel journalTotals = mutedLabel();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public AccountingPanel(AccountingApiClient accountingApiClient) {
        this.accountingApiClient = accountingApiClient;
        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UIHelper.createHeading("Contabilidade"), BorderLayout.WEST);
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.LEFT_ALIGNMENT); feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(top); north.add(feedback); add(north, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Plano de Contas", UIHelper.icon("fas-sitemap", 16, UIHelper.MODULE_ACCOUNTING), buildChartTab());
        tabs.addTab("Diário", UIHelper.icon("fas-book", 16, UIHelper.ACCENT_BLUE), buildJournalTab());
        tabs.addTab("Balancete", UIHelper.icon("fas-balance-scale", 16, UIHelper.APPROVED_GREEN), buildTrialBalanceTab());
        tabs.addTab("Razão", UIHelper.icon("fas-list-alt", 16, UIHelper.PENDING_YELLOW), buildLedgerTab());
        tabs.addTab("Demonstrações", UIHelper.icon("fas-chart-line", 16, UIHelper.ACCENT_CYAN), buildStatementsTab());
        add(tabs, BorderLayout.CENTER);

        LocalDate today = LocalDate.now();
        // ISO, porque é o que o DateField lê — ver o javadoc de DateField.value(). Escrever aqui
        // dd/MM/yyyy fazia "Calcular" e "Carregar" falharem À PRIMEIRA, com o ecrã acabado de abrir
        // e o utilizador sem ter tocado em nada. Só se viu depois de o campo ter largura para
        // mostrar a data: um defeito escondia o outro.
        balanceFrom.setText(today.withDayOfMonth(1).toString());
        balanceTo.setText(today.toString());
        ledgerFrom.setText(today.withDayOfMonth(1).toString());
        ledgerTo.setText(today.toString());
        statementsFrom.setText(today.withDayOfYear(1).toString());
        statementsTo.setText(today.toString());
    }

    /** Visível só enquanto não houver plano — ver {@link #loadAccounts()}. */
    private final JLabel emptyChartWarning = UIHelper.createWarningBanner(
            "Esta empresa não tem plano de contas. Enquanto assim for, as facturas, os recibos e os "
                    + "salários pagos NÃO são escriturados — e não fica registo de que foram "
                    + "saltados. Use \"Semear PGC-NIRF\" para criar o plano.");

    /** Carregamento preguiçoso: só busca quando o painel é aberto (sem HTTP no construtor). */
    public void onPanelSelected() {
        loadAccounts();
        if (journalPager != null) journalPager.reload();
    }

    // ─────────────────────────── Plano de contas ───────────────────────────

    private JPanel buildChartTab() {
        JTable table = styledTable(accountsModel);
        JPanel card = card(table);

        ModernButton seed = UIHelper.createPrimaryButton("Semear PGC-NIRF");
        seed.setIcon(UIHelper.icon("fas-seedling", 14));
        seed.addActionListener(e -> seedChart());
        ModernButton create = UIHelper.createSuccessButton("Nova Conta");
        create.setIcon(UIHelper.icon("fas-plus", 14));
        create.addActionListener(e -> openAccountDialog());
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> loadAccounts());

        JPanel chartToolbar = UIHelper.filterBar(null, new JComponent[]{refresh, create, seed});
        JPanel chartNorth = new JPanel(new BorderLayout(0, 8));
        chartNorth.setOpaque(false);
        chartNorth.add(emptyChartWarning, BorderLayout.NORTH);
        chartNorth.add(chartToolbar, BorderLayout.CENTER);
        card.add(chartNorth, BorderLayout.NORTH);
        return wrap(card);
    }

    private void loadAccounts() {
        UIHelper.loadAsync(this, accountingApiClient::getAccounts, accounts -> {
            accountsModel.setRowCount(0);
            for (AccountDTO account : accounts) {
                accountsModel.addRow(new Object[]{
                        account.code(), account.name(), account.classLabel(),
                        account.nature() == null ? "-" : account.nature().name(),
                        account.postable() ? "Sim" : "Não (conta-mãe)"});
            }
            // Sem plano de contas, o AutomaticPostingService devolve em silêncio: facturas, recibos
            // e salários pagos NÃO chegam ao razão e não fica rasto nenhum de que foram saltados —
            // ao contrário do plano incompleto, que já deixa PAYROLL_POSTING_SKIPPED na auditoria.
            // O aviso vive aqui, e não num registo por documento, porque uma empresa que não usa
            // contabilidade não deve ser inundada; mas quem abre este ecrã tem de saber o que se
            // está a perder enquanto a tabela estiver vazia.
            emptyChartWarning.setVisible(accounts.isEmpty());
        }, error -> showError("carregar o plano de contas", error));
    }

    private void seedChart() {
        UIHelper.runWithProgress(this, "A semear o plano de contas…",
                accountingApiClient::seedChart,
                created -> {
                    ToastManager.success(this, created == 0
                            ? "Esta empresa já tem plano de contas; nada foi alterado."
                            : created + " contas criadas no plano PGC-NIRF.");
                    loadAccounts();
                },
                error -> showError("semear o plano de contas", error));
    }

    private void openAccountDialog() {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        UIHelper.styleTextField(code);
        UIHelper.styleTextField(name);
        JComboBox<String> nature = new JComboBox<>(new String[]{"DEVEDORA", "CREDORA"});
        UIHelper.styleComboBox(nature);
        JCheckBox postable = new JCheckBox("Aceita lançamentos (conta folha)", true);
        postable.setOpaque(false);
        postable.setForeground(UIHelper.TEXT_LIGHT);

        FormField codeForm = new FormField("Código", code, true, "A classe vem do 1.º dígito (PGC-NIRF).");
        FormField nameForm = new FormField("Nome", name, true, null);
        JPanel form = UIHelper.createDialogForm("", codeForm, "", nameForm,
                "Natureza:", nature, "", postable);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Conta",
                "fas-plus", "Conta do plano da empresa activa", form);
        dialog.setOnSaveAsync(() -> {
            if (!(codeForm.validateRequired() & nameForm.validateRequired())) {
                throw new IllegalArgumentException("Corrija os campos assinalados.");
            }
            SaveAccountRequest request = new SaveAccountRequest(
                    code.getText().trim(), name.getText().trim(),
                    mz.multicore.erp.modules.accounting.model.AccountNature.valueOf(String.valueOf(nature.getSelectedItem())),
                    postable.isSelected());
            return () -> accountingApiClient.createAccount(request);
        });
        if (dialog.showDialog()) loadAccounts();
    }

    // ─────────────────────────── Diário ───────────────────────────

    private JPanel buildJournalTab() {
        JTable table = styledTable(journalModel);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        money(table, 5, 6);
        JPanel card = card(table);

        TableQuickFilterBar filterBar = UIHelper.attachQuickFilter(table);
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> {
            if (journalPager != null) journalPager.reload();
        });
        ModernButton create = UIHelper.createSuccessButton("Novo Lançamento");
        create.setIcon(UIHelper.icon("fas-plus", 14));
        create.addActionListener(e -> openEntryDialog());

        card.add(UIHelper.tableCardTop("Diário Contabilístico", filterBar,
                refresh, create), BorderLayout.NORTH);

        journalPager = new TablePager(this::loadJournalPage);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(journalPager, BorderLayout.NORTH);

        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setOpaque(false);
        footerBar.add(journalTotals, BorderLayout.WEST);
        south.add(footerBar, BorderLayout.SOUTH);

        card.add(south, BorderLayout.SOUTH);
        return wrap(card);
    }

    private void loadJournalPage(int page, int size) {
        UIHelper.loadAsync(this, () -> accountingApiClient.getJournal(page, size), response -> {
            journalPager.apply(response);
            applyJournal(response);
        }, error -> showError("carregar o diário", error));
    }

    private void applyJournal(PageResponse<JournalEntryDTO> response) {
        journalModel.setRowCount(0);
        java.math.BigDecimal totalDebit = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalCredit = java.math.BigDecimal.ZERO;

        for (JournalEntryDTO entry : response.items()) {
            journalModel.addRow(new Object[]{
                    entry.entryNumber(),
                    entry.entryDate() == null ? "-" : entry.entryDate().format(DATE),
                    entry.description(),
                    entry.sourceLabel(),
                    entry.sourceDocumentNumber() == null ? "—" : entry.sourceDocumentNumber(),
                    entry.totalDebit(), entry.totalCredit()});
            if (entry.totalDebit() != null) totalDebit = totalDebit.add(entry.totalDebit());
            if (entry.totalCredit() != null) totalCredit = totalCredit.add(entry.totalCredit());
        }

        if (response.items().isEmpty()) {
            journalTotals.setText("<html><span style='color:#94a3b8'>Nenhum lançamento no período. Certifique-se de semear o PGC-NIRF na aba 'Plano de Contas'.</span></html>");
        } else {
            boolean balanced = totalDebit.compareTo(totalCredit) == 0;
            journalTotals.setText(String.format(
                    "<html><b>Total Débito:</b> %,.2f MT &nbsp;·&nbsp; <b>Total Crédito:</b> %,.2f MT &nbsp;·&nbsp; %s</html>",
                    totalDebit, totalCredit,
                    balanced ? "<span style='color:#10b981'><b>Equilibrado</b></span>"
                             : "<span style='color:#ef4444'><b>Desbalanceado</b></span>"));
        }
    }

    private void openEntryDialog() {
        DateField date = new DateField();
        date.setText(LocalDate.now().format(DATE));
        JTextField description = new JTextField();
        UIHelper.styleTextField(description);

        JTextField debitAccount = new JTextField();
        JTextField creditAccount = new JTextField();
        UIHelper.styleTextField(debitAccount);
        UIHelper.styleTextField(creditAccount);
        MoneyField amount = new MoneyField();

        FormField descForm = new FormField("Descrição", description, true, null);
        FormField debitForm = new FormField("Conta a debitar", debitAccount, true, "Código da conta (ex.: 1101)");
        FormField creditForm = new FormField("Conta a creditar", creditAccount, true, "Código da conta (ex.: 7101)");
        JPanel form = UIHelper.createDialogForm(
                "Data:", date, "", descForm, "", debitForm, "", creditForm, "Valor (MT):", amount);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Novo Lançamento",
                "fas-book", "Lançamento manual — débito e crédito pelo mesmo valor", form);
        dialog.setOnSaveAsync(() -> {
            if (!(descForm.validateRequired() & debitForm.validateRequired() & creditForm.validateRequired())) {
                throw new IllegalArgumentException("Corrija os campos assinalados.");
            }
            java.math.BigDecimal value = amount.value();
            if (value.signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
            CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                    parseDate(date.getText()),
                    description.getText().trim(),
                    List.of(new CreateJournalEntryRequest.Line(debitAccount.getText().trim(), value, null, null),
                            new CreateJournalEntryRequest.Line(creditAccount.getText().trim(), null, value, null)));
            return () -> accountingApiClient.createEntry(request);
        });
        if (dialog.showDialog()) journalPager.reload();
    }

    // ─────────────────────────── Balancete ───────────────────────────

    private JPanel buildTrialBalanceTab() {
        JTable table = styledTable(balanceModel);
        money(table, 3, 4, 5);
        JPanel card = card(table);

        ModernButton load = UIHelper.createPrimaryButton("Calcular");
        load.setIcon(UIHelper.icon("fas-balance-scale", 14));
        load.addActionListener(e -> loadTrialBalance());

        JPanel filters = TableFilter.bar(TableFilter.label("De:", "fas-calendar-alt"), balanceFrom,
                TableFilter.label("A:", "fas-calendar-alt"), balanceTo, load);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);
        card.add(balanceSummary, BorderLayout.SOUTH);
        return wrap(card);
    }

    private void loadTrialBalance() {
        LocalDate from = parseDate(balanceFrom.getText());
        LocalDate to = parseDate(balanceTo.getText());
        UIHelper.loadAsync(this, () -> accountingApiClient.getTrialBalance(from, to), balance -> {
            balanceModel.setRowCount(0);
            for (TrialBalanceLineDTO line : balance.lines()) {
                balanceModel.addRow(new Object[]{line.accountCode(), line.accountName(),
                        line.classLabel(), line.totalDebit(), line.totalCredit(), line.balance()});
            }
            // Um balancete que não fecha tem de o dizer — apresentar só os números daria a
            // impressão de que está tudo bem.
            balanceSummary.setText(String.format(
                    "<html><b>Total débito:</b> %,.2f MT &nbsp;·&nbsp; <b>Total crédito:</b> %,.2f MT &nbsp;·&nbsp; %s</html>",
                    balance.totalDebit(), balance.totalCredit(),
                    balance.balanced() ? "<b>Balancete fecha</b>"
                            : "<b style='color:#c0392b'>NÃO FECHA — há lançamentos corrompidos</b>"));
        }, error -> showError("calcular o balancete", error));
    }

    // ─────────────────────────── Razão ───────────────────────────

    private JPanel buildLedgerTab() {
        JTable table = styledTable(ledgerModel);
        money(table, 4, 5, 6);
        JPanel card = card(table);

        UIHelper.styleTextField(ledgerAccount);
        ModernButton load = UIHelper.createPrimaryButton("Ver Extracto");
        load.setIcon(UIHelper.icon("fas-search", 14));
        load.addActionListener(e -> loadLedger());

        JPanel filters = TableFilter.bar(TableFilter.label("Conta:"), ledgerAccount,
                TableFilter.label("De:", "fas-calendar-alt"), ledgerFrom,
                TableFilter.label("A:", "fas-calendar-alt"), ledgerTo, load);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);
        card.add(ledgerSummary, BorderLayout.SOUTH);
        return wrap(card);
    }

    private void loadLedger() {
        String code = ledgerAccount.getText().trim();
        LocalDate from = parseDate(ledgerFrom.getText());
        LocalDate to = parseDate(ledgerTo.getText());
        UIHelper.loadAsync(this, () -> accountingApiClient.getLedger(code, from, to), ledger -> {
            ledgerModel.setRowCount(0);
            for (LedgerMovementDTO movement : ledger.movements()) {
                ledgerModel.addRow(new Object[]{
                        movement.date() == null ? "-" : movement.date().format(DATE),
                        movement.entryNumber(), movement.description(),
                        movement.sourceDocumentNumber() == null ? "—" : movement.sourceDocumentNumber(),
                        movement.debit(), movement.credit(), movement.runningBalance()});
            }
            ledgerSummary.setText(String.format(
                    "<html><b>%s — %s</b><br>Saldo de abertura: %,.2f MT &nbsp;·&nbsp; "
                            + "Débitos: %,.2f MT &nbsp;·&nbsp; Créditos: %,.2f MT &nbsp;·&nbsp; "
                            + "<b>Saldo final: %,.2f MT</b></html>",
                    ledger.accountCode(), ledger.accountName(), ledger.openingBalance(),
                    ledger.totalDebit(), ledger.totalCredit(), ledger.closingBalance()));
        }, error -> showError("carregar o extracto da conta", error));
    }

    private JPanel buildStatementsTab() {
        JTable income = styledTable(incomeModel);
        JTable balance = styledTable(statementBalanceModel);
        money(income, 3);
        money(balance, 3);

        JPanel incomeCard = card(income);
        incomeCard.add(UIHelper.createSubheading("Demonstração de resultados"), BorderLayout.NORTH);
        incomeCard.setPreferredSize(new Dimension(0, 300));

        JPanel balanceCard = card(balance);
        balanceCard.add(UIHelper.createSubheading("Balanço"), BorderLayout.NORTH);
        balanceCard.setPreferredSize(new Dimension(0, 300));

        JPanel tablesContainer = new JPanel();
        tablesContainer.setOpaque(false);
        tablesContainer.setLayout(new BoxLayout(tablesContainer, BoxLayout.Y_AXIS));
        incomeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        balanceCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablesContainer.add(incomeCard);
        tablesContainer.add(Box.createVerticalStrut(12));
        tablesContainer.add(balanceCard);

        ModernButton load = UIHelper.createPrimaryButton("Calcular");
        load.setIcon(UIHelper.icon("fas-calculator", 14));
        load.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        load.addActionListener(e -> loadStatements());

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(TableFilter.bar(TableFilter.label("De:", "fas-calendar-alt"), statementsFrom,
                TableFilter.label("A:", "fas-calendar-alt"), statementsTo, load), BorderLayout.NORTH);

        ArrowScrollPanel scroll = new ArrowScrollPanel(tablesContainer);
        content.add(scroll, BorderLayout.CENTER);
        content.add(statementsSummary, BorderLayout.SOUTH);
        return wrap(content);
    }

    private void loadStatements() {
        LocalDate from = parseDate(statementsFrom.getText());
        LocalDate to = parseDate(statementsTo.getText());
        UIHelper.loadAsync(this,
                () -> new StatementsData(accountingApiClient.getIncomeStatement(from, to),
                        accountingApiClient.getBalanceSheet(to)),
                data -> {
                    incomeModel.setRowCount(0);
                    data.income().revenues().forEach(line -> addStatementLine(incomeModel, "Proveitos", line));
                    data.income().expenses().forEach(line -> addStatementLine(incomeModel, "Custos", line));
                    statementBalanceModel.setRowCount(0);
                    data.balance().assets().forEach(line -> addStatementLine(statementBalanceModel, "Activo", line));
                    data.balance().liabilities().forEach(line -> addStatementLine(statementBalanceModel, "Passivo", line));
                    data.balance().equity().forEach(line -> addStatementLine(statementBalanceModel, "Capital", line));
                    statementsSummary.setText(String.format(
                            "<html><b>Resultado:</b> %,.2f MT &nbsp;·&nbsp; <b>Activo:</b> %,.2f MT "
                                    + "&nbsp;·&nbsp; <b>Capital + Passivo:</b> %,.2f MT &nbsp;·&nbsp; %s</html>",
                            data.income().netResult(), data.balance().totalAssets(),
                            data.balance().totalEquityAndLiabilities(),
                            data.balance().balanced() ? "<b>Balanço fecha</b>"
                                    : "<b style='color:#c0392b'>NÃO FECHA</b>"));
                }, error -> showError("calcular as demonstrações financeiras", error));
    }

    private static void addStatementLine(DefaultTableModel model, String section,
                                         FinancialStatementLineDTO line) {
        model.addRow(new Object[]{section, line.accountCode(), line.accountName(), line.amount()});
    }

    private record StatementsData(IncomeStatementDTO income, BalanceSheetDTO balance) {}

    // ─────────────────────────── helpers de UI ───────────────────────────

    private static DefaultTableModel table(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private static void money(JTable table, int... columns) {
        for (int column : columns) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
    }

    private JPanel card(JTable table) {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private static JPanel wrap(JPanel card) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 5, 5, 5));
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buttons(ModernButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panel.setOpaque(false);
        for (ModernButton button : buttons) panel.add(button);
        return panel;
    }

    private static JLabel mutedLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(UIHelper.TEXT_LIGHT);
        label.setBorder(new EmptyBorder(10, 2, 0, 2));
        return label;
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Data inválida — use dd/MM/aaaa.");
        }
    }

    private void showError(String action, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage(), null, null);
    }
}
