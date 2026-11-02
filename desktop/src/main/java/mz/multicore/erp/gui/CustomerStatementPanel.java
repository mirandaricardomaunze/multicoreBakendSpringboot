package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Painel executivo de consulta de Conta Corrente de Clientes com Saldo Progressivo e Reconciliação em PDF.
 */
public class CustomerStatementPanel extends JPanel {

    private final AccountStatementApiClient statementApiClient;
    private final ComercialApiClient comercialApiClient;

    private JComboBox<ClientComboItem> clientCombo;
    private JComboBox<String> businessUnitCombo;
    private DateField startDateField;
    private DateField endDateField;
    private JProgressBar busyBar;

    private JLabel openingBalanceLabel = new JLabel("0,00 MT");
    private JLabel totalDebitsLabel = new JLabel("0,00 MT");
    private JLabel totalCreditsLabel = new JLabel("0,00 MT");
    private JLabel closingBalanceLabel = new JLabel("0,00 MT");
    private JLabel overdueAmountLabel = new JLabel("0,00 MT");

    private DefaultTableModel tableModel;
    private JTable table;
    private CustomerStatementDTO currentStatement;

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
        grid.add(KpiCard.createMetricCard("Saldo Anterior", openingBalanceLabel, "Posição inicial", "fas-history", UIHelper.ACCENT));
        grid.add(KpiCard.createMetricCard("Total Faturado (+)", totalDebitsLabel, "Débitos do período", "fas-file-invoice", UIHelper.ACCENT_BLUE));
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

        startDateField = new DateField(LocalDate.now().withDayOfYear(1));
        startDateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        startDateField.addActionListener(e -> refreshData());
        endDateField = new DateField(LocalDate.now());
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

        card.add(UIHelper.tableCardTop("Conta Corrente & Reconciliação de Clientes", filtersRow,
                refreshBtn, pdfBtn, emailBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(table, card,
                (p, mr) -> CustomerStatementOperationsCoordinator.populatePeek(p, tableModel, mr));
        peekToggleBtn.addActionListener(e -> peek.toggle());
        return card;
    }

    public void loadClients() {
        CustomerStatementOperationsCoordinator.loadClients(comercialApiClient, busyBar, clientCombo, this::refreshData);
    }

    public void refreshData() {
        if (clientCombo.getItemCount() == 0) { loadClients(); return; }
        ClientComboItem selected = (ClientComboItem) clientCombo.getSelectedItem();
        if (selected == null) return;
        busyBar.setVisible(true);
        new SwingWorker<CustomerStatementDTO, Void>() {
            @Override protected CustomerStatementDTO doInBackground() {
                return statementApiClient.getCustomerStatement(selected.id(), getStartDate(), getEndDate());
            }
            @Override protected void done() {
                busyBar.setVisible(false);
                try {
                    currentStatement = get();
                    CustomerStatementOperationsCoordinator.updateSummary(currentStatement, openingBalanceLabel,
                            totalDebitsLabel, totalCreditsLabel, closingBalanceLabel, overdueAmountLabel, businessUnitCombo);
                    renderLines();
                } catch (Exception ex) {
                    ToastManager.show(CustomerStatementPanel.this, FeedbackType.ERROR, "Erro ao carregar extrato: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void renderLines() {
        String selectedBu = businessUnitCombo != null ? (String) businessUnitCombo.getSelectedItem() : null;
        CustomerStatementOperationsCoordinator.renderLines(tableModel, currentStatement, selectedBu);
    }

    private void emitStatementPdf() {
        ClientComboItem sel = getSelectedClient("Seleccione um cliente para gerar o extrato.");
        if (sel != null) CustomerStatementOperationsCoordinator.emitStatementPdf(this, statementApiClient, sel.id(), sel.taxId(), getStartDate(), getEndDate(), busyBar);
    }

    private void emitStatementEmail() {
        ClientComboItem sel = getSelectedClient("Selecione um cliente para enviar o extrato por correio eletrónico.");
        if (sel != null) CustomerStatementOperationsCoordinator.emitStatementEmail(this, statementApiClient, sel.id(), sel.name(), getStartDate(), getEndDate());
    }

    private ClientComboItem getSelectedClient(String warnMsg) {
        ClientComboItem selected = (ClientComboItem) clientCombo.getSelectedItem();
        if (selected == null) ToastManager.show(this, FeedbackType.WARNING, warnMsg);
        return selected;
    }

    private LocalDate getStartDate() {
        return CustomerStatementOperationsCoordinator.parseDate(startDateField.getText(), LocalDate.now().withDayOfYear(1));
    }

    private LocalDate getEndDate() {
        return CustomerStatementOperationsCoordinator.parseDate(endDateField.getText(), LocalDate.now());
    }

    public record ClientComboItem(Long id, String name, String taxId) {
        @Override
        public String toString() {
            return name + (taxId != null && !taxId.isBlank() ? " (NUIT: " + taxId + ")" : "");
        }
    }
}
