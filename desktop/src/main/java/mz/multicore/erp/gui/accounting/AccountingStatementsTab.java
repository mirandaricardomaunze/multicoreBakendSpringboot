package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.BalanceSheetDTO;
import mz.multicore.erp.modules.accounting.dto.FinancialStatementLineDTO;
import mz.multicore.erp.modules.accounting.dto.IncomeStatementDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Aba das Demonstrações Financeiras (Demonstração de Resultados e Balanço Patrimonial).
 */
public final class AccountingStatementsTab extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel incomeModel = AccountingUiHelper.createTableModel("Secção", "Conta", "Nome", "Valor");
    private final DefaultTableModel statementBalanceModel = AccountingUiHelper.createTableModel("Secção", "Conta", "Nome", "Valor");
    private final DateField statementsFrom = new DateField();
    private final DateField statementsTo = new DateField();
    private final JLabel statementsSummary = AccountingUiHelper.createMutedLabel();

    public AccountingStatementsTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        LocalDate today = LocalDate.now();
        statementsFrom.setText(today.withDayOfYear(1).toString());
        statementsTo.setText(today.toString());

        JTable income = AccountingUiHelper.styledTable(incomeModel, false);
        JTable balance = AccountingUiHelper.styledTable(statementBalanceModel, false);
        AccountingUiHelper.applyMoneyRenderers(income, 3);
        AccountingUiHelper.applyMoneyRenderers(balance, 3);

        JPanel incomeCard = AccountingUiHelper.createTableCard(income);
        incomeCard.add(UIHelper.createSubheading("Demonstração de resultados"), BorderLayout.NORTH);
        incomeCard.setPreferredSize(new Dimension(0, 300));

        JPanel balanceCard = AccountingUiHelper.createTableCard(balance);
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

        add(AccountingUiHelper.wrapTabCard(content), BorderLayout.CENTER);
    }

    public void loadStatements() {
        LocalDate from = AccountingUiHelper.parseDate(statementsFrom.getText());
        LocalDate to = AccountingUiHelper.parseDate(statementsTo.getText());
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
                }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível calcular as demonstrações financeiras", error.getMessage(), null, null));
    }

    private static void addStatementLine(DefaultTableModel model, String section,
                                         FinancialStatementLineDTO line) {
        model.addRow(new Object[]{section, line.accountCode(), line.accountName(), line.amount()});
    }

    private record StatementsData(IncomeStatementDTO income, BalanceSheetDTO balance) {}
}
