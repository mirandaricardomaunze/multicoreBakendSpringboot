package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.RecurringContractApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel de Gestão de Contratos & Facturação Recorrente (Avenças).
 * Controlo de contratos periódicos, emissão de faturas em lote e renovações automáticas.
 */
public final class RecurringContractsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    private final RecurringContractApiClient contractApiClient;

    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JLabel kpiActiveContracts = new JLabel("0 Contratos");
    private final JLabel kpiMonthlyTotal = new JLabel("0,00 MT");
    private final JLabel kpiDueContracts = new JLabel("0 A Faturar");
    private final JLabel kpiSuspendedContracts = new JLabel("0 Inactivos");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<RecurringContractDTO> currentContracts = new ArrayList<>();

    private final JTextField searchField = TableFilter.searchField("Pesquisar por contrato, cliente, NUIT ou serviço...");
    private final JComboBox<String> frequencyFilterCombo = TableFilter.combo("Todas as Frequências", "Mensal", "Trimestral", "Semestral", "Anual");
    private final JComboBox<String> statusFilterCombo = TableFilter.combo("Todos os Estados", "Activo", "Suspenso", "Expirado", "Cancelado");

    public RecurringContractsPanel(RecurringContractApiClient contractApiClient) {
        this.contractApiClient = contractApiClient;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(feedback);

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Avenças Activas", kpiActiveContracts, "Contratos em vigor", "fas-file-contract", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Receita Recorrente Fixa", kpiMonthlyTotal, "Valor periódico acumulado", "fas-coins", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("A Faturar Este Mês", kpiDueContracts, "Contratos prontos para emissão", "fas-clock", UIHelper.MODULE_COMERCIAL));
        kpiGrid.add(KpiCard.createCard("Suspensos / Expirados", kpiSuspendedContracts, "Necessitam de renovação", "fas-exclamation-circle", UIHelper.PENDING_YELLOW));

        JPanel northStack = new JPanel(new BorderLayout(0, 12));
        northStack.setOpaque(false);
        northStack.add(headerPanel, BorderLayout.NORTH);
        northStack.add(kpiGrid, BorderLayout.CENTER);
        add(northStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Nº Contrato", "Cliente", "NUIT", "Serviço / Descrição", "Frequência",
                "Dia Emissão", "Valor Fixo", "Início", "Próx. Faturação", "Última Emissão", "Renovação", "Estado"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        int[] widths = {110, 170, 95, 180, 95, 85, 110, 95, 105, 105, 85, 95};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(table);

        table.getColumnModel().getColumn(11).setCellRenderer(TableCellRenderers.status());
        table.putClientProperty("customDetailAction", (Runnable) this::showContractActionsDialog);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableFilter.install(table, searchField,
                new TableFilter.ColumnFilter(frequencyFilterCombo, 4),
                new TableFilter.ColumnFilter(statusFilterCombo, 11));

        JPanel filterBar = TableFilter.bar(
                searchField,
                TableFilter.label("Frequência:"), frequencyFilterCombo,
                TableFilter.label("Estado:"), statusFilterCombo
        );

        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> loadContracts());

        ModernButton batchBillingBtn = UIHelper.createPrimaryButton("Faturar Avenças");
        batchBillingBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        batchBillingBtn.setToolTipText("Executar ciclo de faturação periódica em lote");
        batchBillingBtn.addActionListener(e -> openBatchBillingDialog());

        ModernButton newContractBtn = UIHelper.createSuccessButton("Novo Contrato");
        newContractBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newContractBtn.setToolTipText("Registar novo contrato recorrente");
        newContractBtn.addActionListener(e -> openNewContractDialog());

        card.add(UIHelper.tableCardTop("Gestão de Avenças e Faturação Periódica", filterBar,
                refreshBtn, batchBillingBtn, newContractBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        loadContracts();
    }

    private void loadContracts() {
        feedback.clear();
        new SwingWorker<List<RecurringContractDTO>, Void>() {
            @Override
            protected List<RecurringContractDTO> doInBackground() {
                return contractApiClient.listContracts();
            }

            @Override
            protected void done() {
                try {
                    currentContracts = get();
                    updateTableAndKpis(currentContracts);
                } catch (Exception ex) {
                    feedback.show(FeedbackType.ERROR, "Falha ao carregar contratos: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateTableAndKpis(List<RecurringContractDTO> contracts) {
        tableModel.setRowCount(0);
        int active = 0;
        int due = 0;
        int inactive = 0;
        BigDecimal totalFixed = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (RecurringContractDTO c : contracts) {
            if (c.status() == ContractStatus.ACTIVE) {
                active++;
                if (c.fixedAmount() != null) {
                    totalFixed = totalFixed.add(c.fixedAmount());
                }
                if (c.nextBillingDate() != null && !c.nextBillingDate().isAfter(today)) {
                    due++;
                }
            } else {
                inactive++;
            }

            tableModel.addRow(new Object[]{
                    c.contractNumber(),
                    c.customerName() != null ? c.customerName() : ("Cliente #" + c.customerId()),
                    c.customerNuit() != null ? c.customerNuit() : "—",
                    c.description(),
                    c.frequency() != null ? c.frequency().label() : "—",
                    "Dia " + c.billingDayOfMonth(),
                    c.fixedAmount() != null ? CURRENCY_FMT.format(c.fixedAmount()) : "—",
                    c.startDate() != null ? c.startDate().format(DATE_FMT) : "—",
                    c.nextBillingDate() != null ? c.nextBillingDate().format(DATE_FMT) : "—",
                    c.lastBilledDate() != null ? c.lastBilledDate().format(DATE_FMT) : "Nunca",
                    c.autoRenew() ? "Sim" : "Não",
                    c.status() != null ? c.status().name() : "ACTIVE"
            });
        }

        kpiActiveContracts.setText(active + " Contratos");
        kpiMonthlyTotal.setText(CURRENCY_FMT.format(totalFixed));
        kpiDueContracts.setText(due + " A Faturar");
        kpiSuspendedContracts.setText(inactive + " Inactivos");
    }

    private void openNewContractDialog() {
        if (RecurringContractFormDialog.show(this, contractApiClient)) {
            feedback.show(FeedbackType.SUCCESS, "Contrato registado com sucesso.");
            loadContracts();
        }
    }

    private void openBatchBillingDialog() {
        RecurringContractsOperationsCoordinator.runBatchBilling(this, contractApiClient, feedback, CURRENCY_FMT, this::loadContracts);
    }

    private void showContractActionsDialog() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= currentContracts.size()) return;
        RecurringContractDTO selected = currentContracts.get(row);
        RecurringContractsOperationsCoordinator.manageContractStatus(this, contractApiClient, selected, feedback, this::loadContracts);
    }
}
