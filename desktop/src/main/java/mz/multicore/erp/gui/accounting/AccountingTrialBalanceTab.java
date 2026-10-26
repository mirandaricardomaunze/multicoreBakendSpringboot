package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.TrialBalanceLineDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Aba de Apuramento do Balancete de Verificação.
 */
public final class AccountingTrialBalanceTab extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel balanceModel = AccountingUiHelper.createTableModel(
            "Conta", "Nome", "Classe", "Débito", "Crédito", "Saldo"
    );
    private final JLabel balanceSummary = AccountingUiHelper.createMutedLabel();
    private final DateField balanceFrom = new DateField();
    private final DateField balanceTo = new DateField();

    public AccountingTrialBalanceTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        LocalDate today = LocalDate.now();
        balanceFrom.setText(today.withDayOfMonth(1).toString());
        balanceTo.setText(today.toString());

        JTable table = AccountingUiHelper.styledTable(balanceModel, false);
        AccountingUiHelper.applyMoneyRenderers(table, 3, 4, 5);
        ModernPanel card = AccountingUiHelper.createTableCard(table);

        ModernButton load = UIHelper.createPrimaryButton("Calcular");
        load.setIcon(UIHelper.icon("fas-balance-scale", 14));
        load.addActionListener(e -> loadTrialBalance());

        JPanel filters = TableFilter.bar(TableFilter.label("De:", "fas-calendar-alt"), balanceFrom,
                TableFilter.label("A:", "fas-calendar-alt"), balanceTo, load);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);
        card.add(balanceSummary, BorderLayout.SOUTH);

        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
    }

    public void loadTrialBalance() {
        LocalDate from = AccountingUiHelper.parseDate(balanceFrom.getText());
        LocalDate to = AccountingUiHelper.parseDate(balanceTo.getText());
        UIHelper.loadAsync(this, () -> accountingApiClient.getTrialBalance(from, to), balance -> {
            balanceModel.setRowCount(0);
            for (TrialBalanceLineDTO line : balance.lines()) {
                balanceModel.addRow(new Object[]{line.accountCode(), line.accountName(),
                        line.classLabel(), line.totalDebit(), line.totalCredit(), line.balance()});
            }
            balanceSummary.setText(String.format(
                    "<html><b>Total débito:</b> %,.2f MT &nbsp;·&nbsp; <b>Total crédito:</b> %,.2f MT &nbsp;·&nbsp; %s</html>",
                    balance.totalDebit(), balance.totalCredit(),
                    balance.balanced() ? "<b>Balancete fecha</b>"
                            : "<b style='color:#c0392b'>NÃO FECHA — há lançamentos corrompidos</b>"));
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível calcular o balancete", error.getMessage(), null, null));
    }
}
