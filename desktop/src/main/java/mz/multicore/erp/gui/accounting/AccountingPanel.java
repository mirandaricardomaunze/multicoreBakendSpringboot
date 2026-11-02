package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Contabilidade: plano de contas, diário, razão, balancete e demonstrações financeiras.
 *
 * <p>Cliente-fino: tudo por HTTP, nenhuma regra aqui. As cinco abas são leituras do que o
 * backend calcula — a partida dobrada, a natureza dos saldos e os totais vivem lá.
 *
 * <p>Estrutura canónica de tabelas e topos de card migrada via {@code UIHelper.tableCardTop(} e {@code UIHelper.ensureHeadersFit(table)}.
 */
public final class AccountingPanel extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private final AccountingChartTab chartTab;
    private final AccountingJournalTab journalTab;
    private final AccountingTrialBalanceTab trialBalanceTab;
    private final AccountingLedgerTab ledgerTab;
    private final AccountingStatementsTab statementsTab;

    public AccountingPanel(AccountingApiClient accountingApiClient) {
        this.accountingApiClient = accountingApiClient;
        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UIHelper.createHeading("Contabilidade"), BorderLayout.WEST);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(top);
        north.add(feedback);
        add(north, BorderLayout.NORTH);

        this.chartTab = new AccountingChartTab(accountingApiClient, feedback);
        this.journalTab = new AccountingJournalTab(accountingApiClient, feedback);
        this.trialBalanceTab = new AccountingTrialBalanceTab(accountingApiClient, feedback);
        this.ledgerTab = new AccountingLedgerTab(accountingApiClient, feedback);
        this.statementsTab = new AccountingStatementsTab(accountingApiClient, feedback);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Plano de Contas", UIHelper.icon("fas-sitemap", 16, UIHelper.MODULE_ACCOUNTING), chartTab);
        tabs.addTab("Diário", UIHelper.icon("fas-book", 16, UIHelper.ACCENT_BLUE), journalTab);
        tabs.addTab("Balancete", UIHelper.icon("fas-balance-scale", 16, UIHelper.APPROVED_GREEN), trialBalanceTab);
        tabs.addTab("Razão", UIHelper.icon("fas-list-alt", 16, UIHelper.PENDING_YELLOW), ledgerTab);
        tabs.addTab("Demonstrações", UIHelper.icon("fas-chart-line", 16, UIHelper.ACCENT_CYAN), statementsTab);
        add(tabs, BorderLayout.CENTER);
    }

    /** Carregamento preguiçoso: só busca quando o painel é aberto (sem HTTP no construtor). */
    public void onPanelSelected() {
        chartTab.onPanelSelected();
        journalTab.onPanelSelected();
        trialBalanceTab.loadTrialBalance();
        ledgerTab.loadLedger();
    }

    /**
     * Helper de estilização para tabelas de contabilidade, garantindo cabeçalhos ajustados.
     */
    public static void styleTable(JTable table) {
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
    }
}
