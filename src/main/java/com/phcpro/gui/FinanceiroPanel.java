package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
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

    // Settle elements
    private JComboBox<String> approvedInvoicesCombo;
    private JComboBox<String> targetAccountCombo;

    private List<InvoiceDTO> approvedInvoicesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();

    public FinanceiroPanel(FinanceService financeService, ComercialService comercialService) {
        this.financeService = financeService;
        this.comercialService = comercialService;

        setLayout(new BorderLayout(0, 20));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // TOP SECTION: TREASURY ACCOUNTS & BALANCES
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);
        topPanel.add(UIHelper.createHeading("Tesouraria & Contas"), BorderLayout.NORTH);

        ModernPanel accountsCard = new ModernPanel(16);
        accountsCard.setLayout(new BorderLayout());
        accountsCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        accountsCard.setPreferredSize(new Dimension(800, 150));

        String[] accountCols = {"Conta de Tesouraria", "IBAN / Nº Conta", "Saldo Atual"};
        accountsModel = new DefaultTableModel(accountCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        accountsTable = new JTable(accountsModel);
        UIHelper.styleTable(accountsTable);
        JScrollPane accScroll = new JScrollPane(accountsTable);
        UIHelper.styleScrollPane(accScroll);
        accountsCard.add(accScroll, BorderLayout.CENTER);
        topPanel.add(accountsCard, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // CENTER SECTION: TRANSACTION LOG (LEFT) & RECEIVE PAYMENT FORM (RIGHT)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setOpaque(false);

        // Left Center: Transaction movements
        JPanel movementsPanel = new JPanel(new BorderLayout(0, 10));
        movementsPanel.setOpaque(false);
        movementsPanel.add(UIHelper.createSubheading("Histórico de Fluxo de Caixa"), BorderLayout.NORTH);

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
        movementsCard.add(movScroll, BorderLayout.CENTER);
        movementsPanel.add(movementsCard, BorderLayout.CENTER);
        centerPanel.add(movementsPanel);

        // Right Center: Settlement / Registar Recebimento
        JPanel settlementPanel = new JPanel(new BorderLayout(0, 10));
        settlementPanel.setOpaque(false);
        settlementPanel.add(UIHelper.createSubheading("Liquidacão de Documentos (Receber Venda)"), BorderLayout.NORTH);

        ModernPanel settlementCard = new ModernPanel(16);
        settlementCard.setLayout(new GridBagLayout());
        settlementCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Form Row 1: Invoice selector
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 2, 10);
        JLabel invLbl = new JLabel("Selecionar Fatura Aprovada:");
        invLbl.setForeground(UIHelper.TEXT_MUTED);
        settlementCard.add(invLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 10, 12, 10);
        approvedInvoicesCombo = new JComboBox<>();
        UIHelper.styleComboBox(approvedInvoicesCombo);
        settlementCard.add(approvedInvoicesCombo, gbc);

        // Form Row 2: Target account selector
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 10, 2, 10);
        JLabel accLbl = new JLabel("Conta de Recebimento:");
        accLbl.setForeground(UIHelper.TEXT_MUTED);
        settlementCard.add(accLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 10, 12, 10);
        targetAccountCombo = new JComboBox<>();
        UIHelper.styleComboBox(targetAccountCombo);
        settlementCard.add(targetAccountCombo, gbc);

        // Form Row 3: Submit Button
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 10, 10, 10);
        ModernButton payBtn = new ModernButton("Registar Recebimento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.setGradient(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.darker());
        settlementCard.add(payBtn, gbc);

        settlementPanel.add(settlementCard, BorderLayout.CENTER);
        centerPanel.add(settlementPanel);

        add(centerPanel, BorderLayout.CENTER);

        // Action Listener
        payBtn.addActionListener(e -> registerReceipt());

        refreshData();
    }

    public void refreshData() {
        loadAccountsTable();
        loadMovementsTable();
        loadSettlementCombos();
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

    private void loadSettlementCombos() {
        approvedInvoicesCombo.removeAllItems();
        targetAccountCombo.removeAllItems();

        // 1. Load approved invoices
        approvedInvoicesList.clear();
        List<InvoiceDTO> allInvoices = comercialService.getAllInvoices();
        for (InvoiceDTO invoice : allInvoices) {
            if (invoice.status() == InvoiceStatus.APPROVED) {
                approvedInvoicesList.add(invoice);
                approvedInvoicesCombo.addItem(invoice.invoiceNumber() + " - " + invoice.clientName() + " (" + invoice.totalAmount() + " MT)");
            }
        }

        // 2. Load accounts into targets
        for (TreasuryAccountDTO acc : accountsList) {
            targetAccountCombo.addItem(acc.name());
        }
    }

    private void registerReceipt() {
        if (approvedInvoicesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem faturas aprovadas pendentes de recebimento.", "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma conta de tesouraria configurada.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int invoiceIdx = approvedInvoicesCombo.getSelectedIndex();
        int accountIdx = targetAccountCombo.getSelectedIndex();

        if (invoiceIdx < 0 || accountIdx < 0) return;

        InvoiceDTO invoice = approvedInvoicesList.get(invoiceIdx);
        TreasuryAccountDTO account = accountsList.get(accountIdx);

        try {
            financeService.payInvoice(invoice.id(), account.id());
            JOptionPane.showMessageDialog(this, "Recebimento da fatura " + invoice.invoiceNumber() + " registado com sucesso.\n" +
                    "Saldo da conta " + account.name() + " atualizado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao registar recebimento: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onPanelSelected() {
        refreshData();
    }
}
