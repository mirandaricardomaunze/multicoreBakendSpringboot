package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.financeira.dto.PayInvoiceRequest;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.financeira.dto.TreasuryTransactionDTO;
import com.phcpro.modules.financeira.service.FinanceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FinanceiroPanel extends JPanel {

    private final FinanceService financeService;
    private final ComercialService comercialService;

    // Accounts List Elements
    private DefaultTableModel accountsModel;
    private JTable accountsTable;

    // Transaction movements list
    private DefaultTableModel movementsModel;
    private JTable movementsTable;

    private List<InvoiceDTO> approvedInvoicesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();

    public FinanceiroPanel(FinanceService financeService, ComercialService comercialService) {
        this.financeService = financeService;
        this.comercialService = comercialService;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        add(UIHelper.createHeading("Tesouraria"), BorderLayout.NORTH);

        // Cada tabela na sua aba, para ganhar espaço vertical em vez de ficarem apertadas juntas.
        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabbedPane);
        tabbedPane.addTab("Contas", UIHelper.icon("fas-wallet", 16, UIHelper.TEXT_LIGHT), createAccountsTab());
        tabbedPane.addTab("Fluxo de Caixa", UIHelper.icon("fas-exchange-alt", 16, UIHelper.TEXT_LIGHT),
                createMovementsTab());
        add(tabbedPane, BorderLayout.CENTER);

        refreshData();
    }

    /** Aba das contas de tesouraria (tabela ocupa toda a aba). */
    private JPanel createAccountsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernPanel accountsCard = new ModernPanel(16);
        accountsCard.setLayout(new BorderLayout());
        accountsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] accountCols = {"Conta de Tesouraria", "IBAN / Nº Conta", "Saldo Atual"};
        accountsModel = new DefaultTableModel(accountCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        accountsTable = new JTable(accountsModel);
        UIHelper.styleTable(accountsTable);
        JScrollPane accScroll = new JScrollPane(accountsTable);
        UIHelper.styleScrollPane(accScroll);
        JTextField aSearch = TableFilter.searchField("Conta ou IBAN…");
        TableFilter.install(accountsTable, aSearch);
        JPanel aBar = TableFilter.bar(aSearch);
        aBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        accountsCard.add(aBar, BorderLayout.NORTH);
        accountsCard.add(accScroll, BorderLayout.CENTER);
        panel.add(accountsCard, BorderLayout.CENTER);
        return panel;
    }

    /** Aba do histórico de fluxo de caixa (cabeçalho com acção + tabela). */
    private JPanel createMovementsTab() {
        JPanel movementsPanel = new JPanel(new BorderLayout(0, 10));
        movementsPanel.setOpaque(false);
        movementsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        JPanel movHeader = new JPanel(new BorderLayout());
        movHeader.setOpaque(false);
        movHeader.add(UIHelper.createSubheading("Histórico de Fluxo de Caixa"), BorderLayout.WEST);
        ModernButton payBtn = UIHelper.createSuccessButton("Registar Recebimento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> registerReceipt());
        JPanel movActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        movActions.setOpaque(false);
        movActions.add(payBtn);
        movHeader.add(movActions, BorderLayout.EAST);
        movementsPanel.add(movHeader, BorderLayout.NORTH);

        ModernPanel movementsCard = new ModernPanel(16);
        movementsCard.setLayout(new BorderLayout());
        movementsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] movementCols = {"Data", "Conta", "Descrição", "Tipo", "Valor"};
        movementsModel = new DefaultTableModel(movementCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movementsTable = new JTable(movementsModel);
        UIHelper.styleTable(movementsTable);
        JScrollPane movScroll = new JScrollPane(movementsTable);
        UIHelper.styleScrollPane(movScroll);
        JTextField mSearch = TableFilter.searchField("Conta ou descrição…");
        JComboBox<String> mTipo = TableFilter.combo("Todos os tipos", "DEBIT", "CREDIT");
        JComboBox<String> mPeriodo = TableFilter.periodCombo();
        TableFilter.install(movementsTable, mSearch,
                java.util.List.of(new TableFilter.ColumnFilter(mTipo, 3)),
                java.util.List.of(new TableFilter.PeriodFilter(mPeriodo, 0)));
        JPanel mBar = TableFilter.bar(mSearch, TableFilter.label("Tipo:"), mTipo,
                TableFilter.label("Data:", "fas-calendar-alt"), mPeriodo);
        mBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        movementsCard.add(mBar, BorderLayout.NORTH);
        movementsCard.add(movScroll, BorderLayout.CENTER);
        movementsPanel.add(movementsCard, BorderLayout.CENTER);
        return movementsPanel;
    }

    public void refreshData() {
        loadAccountsTable();
        loadMovementsTable();
        loadApprovedInvoices();
    }

    private void loadAccountsTable() {
        accountsModel.setRowCount(0);
        accountsList = financeService.getAllAccounts();
        for (TreasuryAccountDTO acc : accountsList) {
            accountsModel.addRow(new Object[]{
                    acc.name(),
                    acc.accountNumber(),
                    String.format("%,.2f MT", acc.balance())
            });
        }
    }

    private void loadMovementsTable() {
        movementsModel.setRowCount(0);
        List<TreasuryTransactionDTO> txs = financeService.getAllTransactions();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (TreasuryTransactionDTO tx : txs) {
            movementsModel.addRow(new Object[]{
                    tx.transactionDate().format(dtf),
                    tx.accountName(),
                    tx.description(),
                    tx.transactionType(),
                    (tx.transactionType().equalsIgnoreCase("DEBIT") ? "+" : "-") + String.format(" %,.2f MT", tx.amount())
            });
        }
    }

    private void loadApprovedInvoices() {
        approvedInvoicesList.clear();
        for (InvoiceDTO invoice : comercialService.getAllInvoices()) {
            if (invoice.status() == InvoiceStatus.APPROVED) {
                approvedInvoicesList.add(invoice);
            }
        }
    }

    /** Registo de recebimento (liquidação de fatura aprovada) em modal profissional. */
    private void registerReceipt() {
        if (approvedInvoicesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem faturas aprovadas pendentes de recebimento.", "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma conta de tesouraria configurada.", "Erro", JOptionPane.ERROR_MESSAGE);
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
        dlg.setOnSave(() -> {
            InvoiceDTO invoice = approvedInvoicesList.get(Math.max(0, invoiceCombo.getSelectedIndex()));
            TreasuryAccountDTO account = accountsList.get(Math.max(0, accountCombo.getSelectedIndex()));
            financeService.payInvoice(invoice.id(), account.id());
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Recebimento registado com sucesso.\nSaldo da conta atualizado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        }
    }

    public void onPanelSelected() {
        refreshData();
    }
}
