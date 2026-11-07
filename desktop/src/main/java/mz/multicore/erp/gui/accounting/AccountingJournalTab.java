package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.CreateJournalEntryRequest;
import mz.multicore.erp.modules.accounting.dto.JournalEntryDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aba de Gestão do Diário Contabilístico.
 */
public final class AccountingJournalTab extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel journalModel = AccountingUiHelper.createTableModel(
            "Nº", "Data", "Descrição", "Origem", "Documento", "Débito", "Crédito"
    );
    private final JLabel journalTotals = AccountingUiHelper.createMutedLabel();
    private TablePager journalPager;

    public AccountingJournalTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        JTable table = AccountingUiHelper.styledTable(journalModel, true);
        AccountingUiHelper.applyMoneyRenderers(table, 5, 6);
        ModernPanel card = AccountingUiHelper.createTableCard(table);

        TableQuickFilterBar filterBar = UIHelper.attachQuickFilter(table);
        ModernButton refresh = UIHelper.createRefreshButton(() -> {
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
        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        if (journalPager != null) journalPager.reload();
    }

    private void loadJournalPage(int page, int size) {
        UIHelper.loadAsync(this, () -> accountingApiClient.getJournal(page, size), response -> {
            journalPager.apply(response);
            applyJournal(response);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o diário", error.getMessage(), null, null));
    }

    private void applyJournal(PageResponse<JournalEntryDTO> response) {
        journalModel.setRowCount(0);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntryDTO entry : response.items()) {
            journalModel.addRow(new Object[]{
                    entry.entryNumber(),
                    entry.entryDate() == null ? "-" : entry.entryDate().format(AccountingUiHelper.DATE_FMT),
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
        date.setText(LocalDate.now().toString());
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
            BigDecimal value = amount.value();
            if (value.signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
            CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                    AccountingUiHelper.parseDate(date.getText()),
                    description.getText().trim(),
                    List.of(new CreateJournalEntryRequest.Line(debitAccount.getText().trim(), value, null, null),
                            new CreateJournalEntryRequest.Line(creditAccount.getText().trim(), null, value, null)));
            return () -> accountingApiClient.createEntry(request);
        });
        if (dialog.showDialog()) journalPager.reload();
    }
}
