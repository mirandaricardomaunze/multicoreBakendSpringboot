package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;

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
 * Painel executivo de consulta de Conta Corrente de Fornecedores com Saldo Progressivo e Reconciliação em PDF.
 */
public class SupplierStatementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountStatementApiClient statementApiClient;
    private final PurchaseApiClient purchaseApiClient;

    private JComboBox<SupplierComboItem> supplierCombo;
    private JComboBox<String> businessUnitCombo;
    private DateField startDateField;
    private DateField endDateField;
    private JProgressBar busyBar;

    private JLabel openingBalanceLabel;
    private JLabel totalDebitsLabel;
    private JLabel totalCreditsLabel;
    private JLabel closingBalanceLabel;

    private DefaultTableModel tableModel;
    private JTable table;

    private SupplierStatementDTO currentStatement;
    private List<SupplierDTO> loadedSuppliers = new ArrayList<>();

    public SupplierStatementPanel(
            AccountStatementApiClient statementApiClient,
            PurchaseApiClient purchaseApiClient
    ) {
        this.statementApiClient = statementApiClient;
        this.purchaseApiClient = purchaseApiClient;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        add(buildSummaryCards(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        loadSuppliers();
    }

    private JPanel buildSummaryCards() {
        JPanel grid = KpiCard.createGrid(4);

        openingBalanceLabel = new JLabel("0,00 MT");
        totalDebitsLabel = new JLabel("0,00 MT");
        totalCreditsLabel = new JLabel("0,00 MT");
        closingBalanceLabel = new JLabel("0,00 MT");

        grid.add(KpiCard.createMetricCard("Saldo Anterior a Pagar", openingBalanceLabel, "Posição inicial", "fas-history", UIHelper.ACCENT_BLUE));
        grid.add(KpiCard.createMetricCard("Total Compras (+)", totalCreditsLabel, "Compras do período", "fas-shopping-bag", UIHelper.PENDING_YELLOW));
        grid.add(KpiCard.createMetricCard("Total Pagamentos (-)", totalDebitsLabel, "Pagamentos efectuados", "fas-money-bill-wave", UIHelper.APPROVED_GREEN));
        grid.add(KpiCard.createMetricCard("Saldo em Dívida a Pagar", closingBalanceLabel, "Posição actual", "fas-balance-scale", UIHelper.REJECTED_RED));

        return grid;
    }

    private ModernPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        busyBar = new JProgressBar();
        busyBar.setIndeterminate(true);
        busyBar.setVisible(false);
        busyBar.setPreferredSize(new Dimension(100, 14));

        supplierCombo = new JComboBox<>();
        supplierCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(supplierCombo);
        supplierCombo.addActionListener(e -> refreshData());

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

        JPanel filters = TableFilter.bar(
                TableFilter.label("Fornecedor:"), supplierCombo,
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

        String[] cols = {"Data", "Tipo", "Negócio", "Nº Documento", "Descrição", "Referência", "Pagamentos (-)", "Compras (+)", "Saldo a Pagar"};
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

        card.add(UIHelper.tableCardTop("Conta Corrente & Reconciliação de Fornecedores", filtersRow,
                refreshBtn, pdfBtn), BorderLayout.NORTH);
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
        Object paymentsVal = tableModel.getValueAt(modelRow, 6);
        Object purchasesVal = tableModel.getValueAt(modelRow, 7);
        Object balanceVal = tableModel.getValueAt(modelRow, 8);

        peek.setHeaderIcon("fas-truck-loading", UIHelper.ACCENT_BLUE);
        peek.setTitle(docNo != null && !"-".equals(docNo) ? docNo : desc);
        peek.setSubtitle(desc);
        peek.setStatus(type, "FT".equals(type) || "ND".equals(type) ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        java.util.List<QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Data", date, false));
        items.add(new QuickPeekPanel.PeekItem("Tipo de Documento", type, false));
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", bu != null && !"-".equals(bu) ? bu : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Referência", ref != null ? ref : "-", false));
        if (paymentsVal instanceof BigDecimal p && p.signum() > 0) {
            items.add(new QuickPeekPanel.PeekItem("Pagamentos (-)", formatMoney(p), false));
        }
        if (purchasesVal instanceof BigDecimal c && c.signum() > 0) {
            items.add(new QuickPeekPanel.PeekItem("Compras (+)", formatMoney(c), false));
        }
        if (balanceVal instanceof BigDecimal b) {
            items.add(new QuickPeekPanel.PeekItem("Saldo a Pagar", formatMoney(b), true));
        }
        peek.setItems(items);
        peek.setOnOpenFullAction(null);
    }

    public void loadSuppliers() {
        busyBar.setVisible(true);
        new SwingWorker<List<SupplierDTO>, Void>() {
            @Override
            protected List<SupplierDTO> doInBackground() {
                try {
                    return purchaseApiClient.getSuppliers();
                } catch (Exception e) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    loadedSuppliers = get();
                    supplierCombo.removeAllItems();
                    for (SupplierDTO s : loadedSuppliers) {
                        supplierCombo.addItem(new SupplierComboItem(s.id(), s.name(), s.taxId()));
                    }
                    if (!loadedSuppliers.isEmpty()) {
                        refreshData();
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void refreshData() {
        SupplierComboItem selected = (SupplierComboItem) supplierCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        LocalDate start = parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = parseDate(endDateField.getText(), LocalDate.now());

        busyBar.setVisible(true);

        new SwingWorker<SupplierStatementDTO, Void>() {
            @Override
            protected SupplierStatementDTO doInBackground() {
                return statementApiClient.getSupplierStatement(selected.id(), start, end);
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    currentStatement = get();
                    populateData(currentStatement);
                } catch (Exception ex) {
                    ToastManager.show(SupplierStatementPanel.this, FeedbackType.ERROR,
                            "Erro ao carregar extrato de fornecedor: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateData(SupplierStatementDTO stmt) {
        if (stmt == null) return;

        openingBalanceLabel.setText(formatMoney(stmt.openingBalance()));
        totalDebitsLabel.setText(formatMoney(stmt.totalDebits()));
        totalCreditsLabel.setText(formatMoney(stmt.totalCredits()));
        closingBalanceLabel.setText(formatMoney(stmt.closingBalance()));

        if (stmt.closingBalance().signum() > 0) {
            closingBalanceLabel.setForeground(UIHelper.REJECTED_RED);
        } else {
            closingBalanceLabel.setForeground(UIHelper.APPROVED_GREEN);
        }

        // Atualizar seletor de negócios
        java.util.Set<String> bus = new java.util.TreeSet<>();
        if (stmt.lines() != null) {
            for (SupplierStatementLineDTO line : stmt.lines()) {
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

        // Linha de Saldo Anterior a Pagar
        tableModel.addRow(new Object[]{
                currentStatement.startDate().format(DATE_FMT),
                "TRANS",
                "-",
                "SALDO-ANT",
                "Saldo Anterior a Pagar",
                "-",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                currentStatement.openingBalance()
        });

        if (currentStatement.lines() != null) {
            for (SupplierStatementLineDTO line : currentStatement.lines()) {
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
        SupplierComboItem selected = (SupplierComboItem) supplierCombo.getSelectedItem();
        if (selected == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione um fornecedor para gerar o extrato.");
            return;
        }

        LocalDate start = parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = parseDate(endDateField.getText(), LocalDate.now());

        busyBar.setVisible(true);

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return statementApiClient.getSupplierStatementPdf(selected.id(), start, end);
            }

            @Override
            protected void done() {
                busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        String filename = "extrato-fornecedor-" + selected.taxId() + ".pdf";
                        PrintPreviewDialog.show(SupplierStatementPanel.this, pdfBytes, filename);
                    } else {
                        ToastManager.show(SupplierStatementPanel.this, FeedbackType.ERROR, "Documento de extrato vazio.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(SupplierStatementPanel.this, FeedbackType.ERROR,
                            "Erro ao gerar extrato em PDF: " + ex.getMessage());
                }
            }
        }.execute();
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

    public record SupplierComboItem(Long id, String name, String taxId) {
        @Override
        public String toString() {
            return name + (taxId != null && !taxId.isBlank() ? " (NUIT/NIF: " + taxId + ")" : "");
        }
    }
}
