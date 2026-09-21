package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
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

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(buildSummaryCards(), BorderLayout.NORTH);
        centerPanel.add(buildTableCard(), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        loadSuppliers();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(UIHelper.createHeading("Conta Corrente & Reconciliação de Fornecedores"), BorderLayout.WEST);

        busyBar = new JProgressBar();
        busyBar.setIndeterminate(true);
        busyBar.setVisible(false);
        busyBar.setPreferredSize(new Dimension(140, 14));
        topRow.add(busyBar, BorderLayout.EAST);
        header.add(topRow, BorderLayout.NORTH);

        ModernPanel filterBar = new ModernPanel(12);
        filterBar.setLayout(new BorderLayout(12, 0));
        filterBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        JLabel lblFornecedor = new JLabel("Fornecedor:");
        lblFornecedor.setForeground(UIHelper.TEXT_LIGHT);
        lblFornecedor.setFont(lblFornecedor.getFont().deriveFont(12f));
        leftFilters.add(lblFornecedor);

        supplierCombo = new JComboBox<>();
        supplierCombo.setPreferredSize(new Dimension(280, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(supplierCombo);
        supplierCombo.addActionListener(e -> refreshData());
        leftFilters.add(supplierCombo);

        JLabel lblInicio = new JLabel("Início:");
        lblInicio.setForeground(UIHelper.TEXT_LIGHT);
        lblInicio.setFont(lblInicio.getFont().deriveFont(12f));
        leftFilters.add(lblInicio);

        LocalDate defaultStart = LocalDate.now().withDayOfYear(1);
        startDateField = new DateField(defaultStart);
        startDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        startDateField.addActionListener(e -> refreshData());
        leftFilters.add(startDateField);

        JLabel lblFim = new JLabel("Fim:");
        lblFim.setForeground(UIHelper.TEXT_LIGHT);
        lblFim.setFont(lblFim.getFont().deriveFont(12f));
        leftFilters.add(lblFim);

        LocalDate defaultEnd = LocalDate.now();
        endDateField = new DateField(defaultEnd);
        endDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        endDateField.addActionListener(e -> refreshData());
        leftFilters.add(endDateField);

        filterBar.add(leftFilters, BorderLayout.WEST);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshData);
        rightActions.add(refreshBtn);

        ModernButton pdfBtn = UIHelper.createPrimaryButton("Imprimir Extrato (PDF)");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        pdfBtn.setPreferredSize(new Dimension(185, UIHelper.FORM_CONTROL_HEIGHT));
        pdfBtn.addActionListener(e -> emitStatementPdf());
        rightActions.add(pdfBtn);

        filterBar.add(rightActions, BorderLayout.EAST);
        header.add(filterBar, BorderLayout.SOUTH);

        return header;
    }

    private JPanel buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 12, 0));
        grid.setOpaque(false);

        openingBalanceLabel = new JLabel("0,00 MT");
        totalDebitsLabel = new JLabel("0,00 MT");
        totalCreditsLabel = new JLabel("0,00 MT");
        closingBalanceLabel = new JLabel("0,00 MT");

        grid.add(KpiCard.createMetricCard("Saldo Anterior a Pagar", openingBalanceLabel, "Posição inicial", "fas-history", UIHelper.TEXT_LIGHT));
        grid.add(KpiCard.createMetricCard("Total Compras (+)", totalCreditsLabel, "Compras do período", "fas-shopping-bag", UIHelper.PENDING_YELLOW));
        grid.add(KpiCard.createMetricCard("Total Pagamentos (-)", totalDebitsLabel, "Pagamentos efectuados", "fas-money-bill-wave", UIHelper.APPROVED_GREEN));
        grid.add(KpiCard.createMetricCard("Saldo em Dívida a Pagar", closingBalanceLabel, "Posição actual", "fas-balance-scale", UIHelper.TEXT_LIGHT));

        return grid;
    }

    private ModernPanel buildTableCard() {
        ModernPanel card = new ModernPanel(12);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"Data", "Tipo", "Nº Documento", "Descrição", "Referência", "Pagamentos (-)", "Compras (+)", "Saldo a Pagar"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(55);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(210);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);
        table.getColumnModel().getColumn(7).setPreferredWidth(120);

        for (int c : new int[]{5, 6, 7}) {
            table.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.money());
        }

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        return card;
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

        tableModel.setRowCount(0);

        // Linha de Saldo Anterior a Pagar
        tableModel.addRow(new Object[]{
                stmt.startDate().format(DATE_FMT),
                "TRANS",
                "SALDO-ANT",
                "Saldo Anterior a Pagar",
                "-",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                stmt.openingBalance()
        });

        for (SupplierStatementLineDTO line : stmt.lines()) {
            tableModel.addRow(new Object[]{
                    line.date().format(DATE_FMT),
                    line.documentType(),
                    line.documentNumber(),
                    line.description(),
                    line.reference(),
                    line.debit(),
                    line.credit(),
                    line.runningBalance()
            });
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
