package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.dto.EmailDispatchResultDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel executivo de consulta de Conta Corrente de Clientes com Saldo Progressivo e Reconciliação em PDF.
 */
public class CustomerStatementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountStatementApiClient statementApiClient;
    private final ComercialApiClient comercialApiClient;

    private JComboBox<ClientComboItem> clientCombo;
    private JComboBox<String> businessUnitCombo;
    private DateField startDateField;
    private DateField endDateField;
    private JProgressBar busyBar;

    private JLabel openingBalanceLabel;
    private JLabel totalDebitsLabel;
    private JLabel totalCreditsLabel;
    private JLabel closingBalanceLabel;
    private JLabel overdueAmountLabel;

    private DefaultTableModel tableModel;
    private JTable table;

    private CustomerStatementDTO currentStatement;
    private List<ClientDTO> loadedClients = new ArrayList<>();

    public CustomerStatementPanel(
            AccountStatementApiClient statementApiClient,
            ComercialApiClient comercialApiClient
    ) {
        this.statementApiClient = statementApiClient;
        this.comercialApiClient = comercialApiClient;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        add(buildSummaryCards(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        loadClients();
    }

    private JPanel buildSummaryCards() {
        JPanel grid = KpiCard.createGrid(5);

        openingBalanceLabel = new JLabel("0,00 MT");
        totalDebitsLabel = new JLabel("0,00 MT");
        totalCreditsLabel = new JLabel("0,00 MT");
        closingBalanceLabel = new JLabel("0,00 MT");
        overdueAmountLabel = new JLabel("0,00 MT");

        grid.add(KpiCard.createMetricCard("Saldo Anterior", openingBalanceLabel, "Posição inicial", "fas-history", UIHelper.ACCENT));
        grid.add(KpiCard.createMetricCard("Total Facturado (+)", totalDebitsLabel, "Débitos do período", "fas-file-invoice", UIHelper.ACCENT_BLUE));
        grid.add(KpiCard.createMetricCard("Total Liquidado (-)", totalCreditsLabel, "Créditos/Recibos", "fas-money-bill-wave", UIHelper.APPROVED_GREEN));
        grid.add(KpiCard.createMetricCard("Saldo em Aberto", closingBalanceLabel, "Posição actual", "fas-balance-scale", UIHelper.PENDING_YELLOW));
        grid.add(KpiCard.createMetricCard("Total Vencido em Mora", overdueAmountLabel, "Valores em atraso", "fas-exclamation-triangle", UIHelper.REJECTED_RED));

        return grid;
    }

    private ModernPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        busyBar = new JProgressBar();
        busyBar.setIndeterminate(true);
        busyBar.setVisible(false);
        busyBar.setPreferredSize(new Dimension(100, 14));

        clientCombo = new JComboBox<>();
        clientCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(clientCombo);
        clientCombo.addActionListener(e -> refreshData());

        businessUnitCombo = TableFilter.combo("Todos os negócios");
        businessUnitCombo.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(businessUnitCombo);
        businessUnitCombo.addActionListener(e -> renderLines());

        LocalDate defaultStart = LocalDate.now().withDayOfYear(1);
        startDateField = new DateField(defaultStart);
        startDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        startDateField.addActionListener(e -> refreshData());

        LocalDate defaultEnd = LocalDate.now();
        endDateField = new DateField(defaultEnd);
        endDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        endDateField.addActionListener(e -> refreshData());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshData);

        ModernButton pdfBtn = UIHelper.createPrimaryButton("Imprimir Extrato (PDF)");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        pdfBtn.setPreferredSize(new Dimension(185, UIHelper.FORM_CONTROL_HEIGHT));
        pdfBtn.addActionListener(e -> emitStatementPdf());

        ModernButton emailBtn = UIHelper.createSecondaryButton("Enviar por Email");
        emailBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        emailBtn.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        emailBtn.addActionListener(e -> emitStatementEmail());

        JPanel filters = TableFilter.bar(
                TableFilter.label("Cliente:"), clientCombo,
                TableFilter.label("Negócio:"), businessUnitCombo,
                TableFilter.label("Início:"), startDateField,
                TableFilter.label("Fim:"), endDateField,
                busyBar
        );

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da linha (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        filtersRow.add(filters, BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        String[] cols = {"Data", "Tipo", "Negócio", "Nº Documento", "Descrição", "Referência", "Débito (+)", "Crédito (-)", "Saldo Acumulado"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        int[] widths = {85, 50, 110, 115, 180, 100, 105, 105, 115};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        for (int c : new int[]{6, 7, 8}) {
            table.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.money());
        }
        UIHelper.ensureHeadersFit(table);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        card.add(UIHelper.tableCardTop("Conta Corrente & Reconciliação de Clientes", filtersRow,
                refreshBtn, pdfBtn, emailBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(table, card, this::populatePeek);
        peekToggleBtn.addActionListener(e -> peek.toggle());

        return card;
    }

    private void populatePeek(QuickPeekPanel peek, int modelRow) {
        if (tableModel == null || modelRow < 0 || modelRow >= tableModel.getRowCount()) return;
        String date = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String type = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String bu = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String docNo = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String desc = String.valueOf(tableModel.getValueAt(modelRow, 4));
        String ref = String.valueOf(tableModel.getValueAt(modelRow, 5));
        Object debitVal = tableModel.getValueAt(modelRow, 6);
        Object creditVal = tableModel.getValueAt(modelRow, 7);
        Object balanceVal = tableModel.getValueAt(modelRow, 8);

        peek.setHeaderIcon("fas-file-invoice", UIHelper.ACCENT_BLUE);
        peek.setTitle(docNo != null && !"-".equals(docNo) ? docNo : desc);
        peek.setSubtitle(desc);
        peek.setStatus(type, "FT".equals(type) || "ND".equals(type) ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        java.util.List<QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Data", date, false));
        items.add(new QuickPeekPanel.PeekItem("Tipo de Documento", type, false));
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", bu != null && !"-".equals(bu) ? bu : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Referência", ref != null ? ref : "-", false));
        if (debitVal instanceof BigDecimal d && d.signum() > 0) {
            items.add(new QuickPeekPanel.PeekItem("Débito (+)", formatMoney(d), false));
        }
        if (creditVal instanceof BigDecimal c && c.signum() > 0) {
            items.add(new QuickPeekPanel.PeekItem("Crédito (-)", formatMoney(c), false));
        }
        if (balanceVal instanceof BigDecimal b) {
            items.add(new QuickPeekPanel.PeekItem("Saldo Acumulado", formatMoney(b), true));
        }
        peek.setItems(items);
        peek.setOnOpenFullAction(null);
    }

    public void loadClients() {
        busyBar.setVisible(true);
        new SwingWorker<List<ClientDTO>, Void>() {
            @Override
            protected List<ClientDTO> doInBackground() {
                try {
                    return comercialApiClient.getClients();
                } catch (Exception e) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    loadedClients = get();
                    clientCombo.removeAllItems();
                    for (ClientDTO c : loadedClients) {
                        clientCombo.addItem(new ClientComboItem(c.id(), c.name(), c.taxId()));
                    }
                    if (!loadedClients.isEmpty()) {
                        refreshData();
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void refreshData() {
        ClientComboItem selected = (ClientComboItem) clientCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        LocalDate start = parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = parseDate(endDateField.getText(), LocalDate.now());

        busyBar.setVisible(true);

        new SwingWorker<CustomerStatementDTO, Void>() {
            @Override
            protected CustomerStatementDTO doInBackground() {
                return statementApiClient.getCustomerStatement(selected.id(), start, end);
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    currentStatement = get();
                    populateData(currentStatement);
                } catch (Exception ex) {
                    ToastManager.show(CustomerStatementPanel.this, FeedbackType.ERROR,
                            "Erro ao carregar extrato de conta corrente: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateData(CustomerStatementDTO stmt) {
        if (stmt == null) return;

        openingBalanceLabel.setText(formatMoney(stmt.openingBalance()));
        totalDebitsLabel.setText(formatMoney(stmt.totalDebits()));
        totalCreditsLabel.setText(formatMoney(stmt.totalCredits()));
        closingBalanceLabel.setText(formatMoney(stmt.closingBalance()));
        overdueAmountLabel.setText(formatMoney(stmt.overdueAmount()));

        if (stmt.closingBalance().signum() > 0) {
            closingBalanceLabel.setForeground(UIHelper.REJECTED_RED);
        } else {
            closingBalanceLabel.setForeground(UIHelper.APPROVED_GREEN);
        }

        // Atualizar seletor de negócios
        java.util.Set<String> bus = new java.util.TreeSet<>();
        if (stmt.lines() != null) {
            for (CustomerStatementLineDTO line : stmt.lines()) {
                if (line.businessUnitName() != null && !line.businessUnitName().isBlank()) {
                    bus.add(line.businessUnitName());
                }
            }
        }
        if (businessUnitCombo != null) {
            String selectedBu = (String) businessUnitCombo.getSelectedItem();
            businessUnitCombo.removeAllItems();
            businessUnitCombo.addItem("Todos os negócios");
            for (String b : bus) businessUnitCombo.addItem(b);
            if (selectedBu != null) businessUnitCombo.setSelectedItem(selectedBu);
        }

        renderLines();
    }

    private void renderLines() {
        if (currentStatement == null) return;
        tableModel.setRowCount(0);

        String selectedBu = businessUnitCombo != null ? (String) businessUnitCombo.getSelectedItem() : null;
        boolean filterBu = selectedBu != null && !selectedBu.startsWith("Todos");

        // Linha de Saldo Anterior Transmitido
        tableModel.addRow(new Object[]{
                currentStatement.startDate().format(DATE_FMT),
                "TRANS",
                "-",
                "SALDO-ANT",
                "Saldo Anterior Transmitido",
                "-",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                currentStatement.openingBalance()
        });

        if (currentStatement.lines() != null) {
            for (CustomerStatementLineDTO line : currentStatement.lines()) {
                String bu = line.businessUnitName() != null && !line.businessUnitName().isBlank() ? line.businessUnitName() : "-";
                if (filterBu && !bu.equalsIgnoreCase(selectedBu)) {
                    continue;
                }
                tableModel.addRow(new Object[]{
                        line.date().format(DATE_FMT),
                        line.documentType(),
                        bu,
                        line.documentNumber(),
                        line.description(),
                        line.reference(),
                        line.debit(),
                        line.credit(),
                        line.runningBalance()
                });
            }
        }
    }

    private void emitStatementPdf() {
        ClientComboItem selected = (ClientComboItem) clientCombo.getSelectedItem();
        if (selected == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione um cliente para gerar o extrato.");
            return;
        }

        LocalDate start = parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = parseDate(endDateField.getText(), LocalDate.now());

        busyBar.setVisible(true);

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return statementApiClient.getCustomerStatementPdf(selected.id(), start, end);
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        String filename = "extrato-cliente-" + selected.taxId() + ".pdf";
                        PrintPreviewDialog.show(CustomerStatementPanel.this, pdfBytes, filename);
                    } else {
                        ToastManager.show(CustomerStatementPanel.this, FeedbackType.ERROR, "Documento de extrato vazio.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(CustomerStatementPanel.this, FeedbackType.ERROR,
                            "Erro ao gerar extrato em PDF: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void emitStatementEmail() {
        ClientComboItem selected = (ClientComboItem) clientCombo.getSelectedItem();
        if (selected == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Selecione um cliente para enviar o extrato por correio eletrónico.");
            return;
        }

        LocalDate start = parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = parseDate(endDateField.getText(), LocalDate.now());

        SendStatementEmailDialog dlg = new SendStatementEmailDialog(
                UIHelper.mainWindow,
                statementApiClient,
                selected.id(),
                selected.name(),
                null,
                start,
                end
        );

        if (dlg.showDialog()) {
            EmailDispatchResultDTO res = dlg.getLastResult();
            if (res != null && res.success()) {
                ToastManager.success(this, res.message());
            }
        }
    }

    private LocalDate parseDate(String text, LocalDate fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            try {
                return LocalDate.parse(text.trim(), DATE_FMT);
            } catch (Exception e2) {
                return fallback;
            }
        }
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) return "0,00 MT";
        return String.format(java.util.Locale.forLanguageTag("pt-MZ"), "%,.2f MT", val);
    }

    public record ClientComboItem(Long id, String name, String taxId) {
        @Override
        public String toString() {
            return name + (taxId != null && !taxId.isBlank() ? " (NUIT: " + taxId + ")" : "");
        }
    }
}
