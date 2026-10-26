package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.LedgerMovementDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Aba do Livro Razão e Extrato de Conta Corrente.
 */
public final class AccountingLedgerTab extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel ledgerModel = AccountingUiHelper.createTableModel(
            "Data", "Lançamento", "Descrição", "Documento", "Débito", "Crédito", "Saldo"
    );
    private final JLabel ledgerSummary = AccountingUiHelper.createMutedLabel();
    private final JTextField ledgerAccount = new JTextField("2101", 8);
    private final DateField ledgerFrom = new DateField();
    private final DateField ledgerTo = new DateField();

    public AccountingLedgerTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        LocalDate today = LocalDate.now();
        ledgerFrom.setText(today.withDayOfMonth(1).toString());
        ledgerTo.setText(today.toString());

        JTable table = AccountingUiHelper.styledTable(ledgerModel, false);
        AccountingUiHelper.applyMoneyRenderers(table, 4, 5, 6);
        ModernPanel card = AccountingUiHelper.createTableCard(table);

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

        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
    }

    public void loadLedger() {
        String code = ledgerAccount.getText().trim();
        LocalDate from = AccountingUiHelper.parseDate(ledgerFrom.getText());
        LocalDate to = AccountingUiHelper.parseDate(ledgerTo.getText());
        UIHelper.loadAsync(this, () -> accountingApiClient.getLedger(code, from, to), ledger -> {
            ledgerModel.setRowCount(0);
            for (LedgerMovementDTO movement : ledger.movements()) {
                ledgerModel.addRow(new Object[]{
                        movement.date() == null ? "-" : movement.date().format(AccountingUiHelper.DATE_FMT),
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
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o extracto da conta", error.getMessage(), null, null));
    }
}
