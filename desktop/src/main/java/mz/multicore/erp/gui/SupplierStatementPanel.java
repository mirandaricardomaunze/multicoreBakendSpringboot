package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** Painel executivo de consulta de Conta Corrente de Fornecedores com Saldo Progressivo e Reconciliação. */
public class SupplierStatementPanel extends JPanel {

    private final AccountStatementApiClient statementApiClient;
    private final PurchaseApiClient purchaseApiClient;

    private JComboBox<SupplierComboItem> supplierCombo;
    private JComboBox<String> businessUnitCombo;
    private DateField startDateField, endDateField;
    private JProgressBar busyBar;
    private JLabel openingBalanceLabel, totalDebitsLabel, totalCreditsLabel, closingBalanceLabel;
    private DefaultTableModel tableModel;
    private JTable table;
    private SupplierStatementDTO currentStatement;

    public SupplierStatementPanel(AccountStatementApiClient statementApiClient, PurchaseApiClient purchaseApiClient) {
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
        openingBalanceLabel = new JLabel("0,00 MT"); totalDebitsLabel = new JLabel("0,00 MT");
        totalCreditsLabel = new JLabel("0,00 MT"); closingBalanceLabel = new JLabel("0,00 MT");
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

        busyBar = new JProgressBar(); busyBar.setIndeterminate(true); busyBar.setVisible(false);
        busyBar.setPreferredSize(new Dimension(100, 14));

        supplierCombo = new JComboBox<>(); supplierCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(supplierCombo); supplierCombo.addActionListener(e -> refreshData());

        businessUnitCombo = TableFilter.combo("Todos os negócios"); businessUnitCombo.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(businessUnitCombo); businessUnitCombo.addActionListener(e -> renderLines());

        startDateField = new DateField(LocalDate.now().withDayOfYear(1)); startDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        startDateField.addActionListener(e -> refreshData());

        endDateField = new DateField(LocalDate.now()); endDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        endDateField.addActionListener(e -> refreshData());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshData);
        ModernButton pdfBtn = UIHelper.createPrimaryButton("Imprimir Extrato (PDF)");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        pdfBtn.setPreferredSize(new Dimension(185, UIHelper.FORM_CONTROL_HEIGHT));
        pdfBtn.addActionListener(e -> emitStatementPdf());

        JPanel filters = TableFilter.bar(
                TableFilter.label("Fornecedor:"), supplierCombo, TableFilter.label("Negócio:"), businessUnitCombo,
                TableFilter.label("Início:"), startDateField, TableFilter.label("Fim:"), endDateField, busyBar
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
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        int[] widths = {85, 50, 110, 115, 180, 100, 105, 105, 115};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        for (int c : new int[]{6, 7, 8}) table.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.money());
        UIHelper.ensureHeadersFit(table);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(UIHelper.tableCardTop("Conta Corrente & Reconciliação de Fornecedores", filtersRow, refreshBtn, pdfBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(table, card,
                (p, mr) -> SupplierStatementOperationsCoordinator.populatePeek(p, tableModel, mr));
        peekToggleBtn.addActionListener(e -> peek.toggle());
        return card;
    }

    public void loadSuppliers() {
        SupplierStatementViewCoordinator.loadSuppliersAsync(purchaseApiClient, supplierCombo, busyBar, this::refreshData);
    }

    public void refreshData() {
        if (supplierCombo.getItemCount() == 0) { loadSuppliers(); return; }
        SupplierComboItem selected = (SupplierComboItem) supplierCombo.getSelectedItem();
        if (selected == null) return;

        LocalDate start = SupplierStatementOperationsCoordinator.parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = SupplierStatementOperationsCoordinator.parseDate(endDateField.getText(), LocalDate.now());
        SupplierStatementViewCoordinator.loadStatementAsync(statementApiClient, this, selected.id(), start, end, busyBar, stmt -> {
            this.currentStatement = stmt;
            SupplierStatementViewCoordinator.populateKpisAndBusinessUnits(openingBalanceLabel, totalDebitsLabel, totalCreditsLabel, closingBalanceLabel, businessUnitCombo, stmt);
            renderLines();
        });
    }

    private void renderLines() {
        SupplierStatementViewCoordinator.renderLines(tableModel, currentStatement, businessUnitCombo != null ? (String) businessUnitCombo.getSelectedItem() : null);
    }

    private void emitStatementPdf() {
        SupplierComboItem selected = (SupplierComboItem) supplierCombo.getSelectedItem();
        if (selected == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione um fornecedor para gerar o extrato.");
            return;
        }
        LocalDate start = SupplierStatementOperationsCoordinator.parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
        LocalDate end = SupplierStatementOperationsCoordinator.parseDate(endDateField.getText(), LocalDate.now());
        SupplierStatementOperationsCoordinator.emitStatementPdf(this, statementApiClient, selected.id(), selected.taxId(), start, end, busyBar);
    }

    public record SupplierComboItem(Long id, String name, String taxId) {
        @Override public String toString() {
            return name + (taxId != null && !taxId.isBlank() ? " (NUIT/NIF: " + taxId + ")" : "");
        }
    }
}
