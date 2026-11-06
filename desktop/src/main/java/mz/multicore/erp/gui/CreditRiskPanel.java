package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Centro Executivo de Risco de Crédito, Matriz de Aging & Cobrança Formal. */
public class CreditRiskPanel extends JPanel {

    private final CreditRiskApiClient creditRiskApiClient;

    final JLabel kpiTotalReceivable = new JLabel("0,00 MT"), kpiTotalOverdue = new JLabel("0,00 MT"),
            kpiBlockedCount = new JLabel("0"), kpiCriticalCount = new JLabel("0");

    JTextField dateField, searchField;
    JComboBox<String> riskFilterCombo;
    private final JProgressBar busyBar;

    DefaultTableModel tableModel;
    JTable table;
    List<ClientCreditRiskDTO> allClientsRisk = new ArrayList<>(), visibleClientsRisk = new ArrayList<>();

    private ModernButton refreshBtn, pdfBtn, exceptionBtn;

    public CreditRiskPanel(CreditRiskApiClient creditRiskApiClient) {
        this.creditRiskApiClient = creditRiskApiClient;
        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        busyBar = UIHelper.createBusyBar();
        busyBar.setVisible(false);
        busyBar.setPreferredSize(new Dimension(140, 6));
        add(buildKpiBar(), BorderLayout.NORTH);
        add(buildMainCard(), BorderLayout.CENTER);
    }

    private JPanel buildKpiBar() {
        JPanel kpiRow = KpiCard.createGrid(4);
        CreditRiskViewCoordinator.populateKpis(kpiRow, kpiTotalReceivable, kpiTotalOverdue, kpiBlockedCount, kpiCriticalCount);
        return kpiRow;
    }

    private JPanel buildMainCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        dateField = new JTextField(LocalDate.now().toString());
        dateField.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleTextField(dateField);
        dateField.addActionListener(e -> refreshData());

        riskFilterCombo = new JComboBox<>(new String[]{"Todos os Riscos", "Apenas Crítico / Alto", "Apenas Bloqueados", "CRITICAL - Crítico", "HIGH - Alto", "MEDIUM - Médio", "LOW - Baixo"});
        riskFilterCombo.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(riskFilterCombo);
        riskFilterCombo.addActionListener(e -> applyFilters());

        searchField = TableFilter.searchField("Pesquisar cliente, NUIT, email...");
        searchField.setPreferredSize(new Dimension(280, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(searchField, this::applyFilters);

        refreshBtn = UIHelper.createRefreshButton(this::refreshData);
        pdfBtn = UIHelper.createPrimaryButton("Carta Cobrança (PDF)");
        pdfBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        pdfBtn.setPreferredSize(new Dimension(175, UIHelper.FORM_CONTROL_HEIGHT));
        pdfBtn.setEnabled(false);
        pdfBtn.addActionListener(e -> emitDebtCollectionNoticePdf());
        exceptionBtn = UIHelper.createSuccessButton("Pedir Excepção");
        exceptionBtn.setIcon(UIHelper.icon("fas-unlock-alt", 14));
        exceptionBtn.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        exceptionBtn.setEnabled(false);
        exceptionBtn.addActionListener(e -> openCreditExceptionDialog());
        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar cliente seleccionado (Espaço)");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filterInputs = TableFilter.bar(TableFilter.label("Data Ref:"), dateField, TableFilter.label("Risco:"), riskFilterCombo, searchField);
        JPanel filterBar = new JPanel(new BorderLayout(8, 0));
        filterBar.setOpaque(false);
        filterBar.setBorder(new EmptyBorder(0, 0, 4, 0));
        filterBar.add(filterInputs, BorderLayout.WEST);
        filterBar.add(peekToggleBtn, BorderLayout.EAST);

        card.add(UIHelper.tableCardTop("Matriz de Aging & Gestão de Risco", filterBar,
                busyBar, refreshBtn, pdfBtn, exceptionBtn), BorderLayout.NORTH);

        String[] cols = {"ID", "Cliente", "NUIT", "Limite (MT)", "Dívida Total", "Disponível", "Corrente", "1-30d", "31-60d", "61-90d", ">90d", "Total em Mora", "Atraso", "Nível Risco", "Situação"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.putClientProperty("noRowInspector", Boolean.TRUE);
        CreditRiskViewCoordinator.configureTableColumns(table);

        table.setDefaultRenderer(Object.class, new CreditRiskCellRenderer());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) emitDebtCollectionNoticePdf(); }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSel = table.getSelectedRow() >= 0;
                pdfBtn.setEnabled(hasSel); exceptionBtn.setEnabled(hasSel);
            }
        });

        TableQuickPeekController peekController = TableQuickPeekController.install(table, card,
                (peek, mr) -> CreditRiskViewCoordinator.populateQuickPeek(peek, mr, this, this::emitDebtCollectionNoticePdf));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    void updateActionButtons() {
        if (pdfBtn != null) pdfBtn.setEnabled(false);
        if (exceptionBtn != null) exceptionBtn.setEnabled(false);
    }

    public void refreshData() {
        CreditRiskOperationsCoordinator.loadRiskData(this, creditRiskApiClient, parseReferenceDate(), busyBar, res -> {
            updateKpis(res.summary());
            allClientsRisk = res.clients() != null ? res.clients() : new ArrayList<>();
            applyFilters();
        });
    }

    private LocalDate parseReferenceDate() {
        try {
            if (dateField != null && !dateField.getText().trim().isEmpty()) return LocalDate.parse(dateField.getText().trim());
        } catch (Exception ignored) {}
        return LocalDate.now();
    }

    void updateKpis(CreditRiskSummaryDTO summary) { CreditRiskViewCoordinator.updateKpis(this, summary); }

    void applyFilters() { CreditRiskViewCoordinator.applyFilters(this); }

    private ClientCreditRiskDTO getSelectedClientRisk() {
        int row = TableFilter.selectedModelRow(table);
        return (row >= 0 && row < visibleClientsRisk.size()) ? visibleClientsRisk.get(row) : null;
    }

    private void emitDebtCollectionNoticePdf() {
        CreditRiskOperationsCoordinator.emitDebtCollectionNoticePdf(this, creditRiskApiClient, getSelectedClientRisk(), parseReferenceDate(), busyBar);
    }

    private void openCreditExceptionDialog() {
        CreditExceptionDialog.show(this, creditRiskApiClient, getSelectedClientRisk(), this::refreshData);
    }
}
