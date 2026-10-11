package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.RecurringContractApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final JLabel kpiMonthlyTotal = new JLabel("0.00 MT");
    private final JLabel kpiDueContracts = new JLabel("0 A Facturar");
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
        titleRow.add(UIHelper.createHeading("Contratos & Facturação Recorrente"), BorderLayout.WEST);

        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(feedback);

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Avenças Activas", kpiActiveContracts, "Contratos em vigor",
                "fas-file-contract", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Receita Recorrente Fixa", kpiMonthlyTotal, "Valor periódico acumulado",
                "fas-coins", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("A Facturar Este Mês", kpiDueContracts, "Contratos prontos para emissão",
                "fas-clock", UIHelper.MODULE_COMERCIAL));
        kpiGrid.add(KpiCard.createCard("Suspensos / Expirados", kpiSuspendedContracts, "Necessitam de renovação",
                "fas-exclamation-circle", UIHelper.PENDING_YELLOW));

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
                "Dia Emissão", "Valor Fixo", "Início", "Próx. Facturação", "Última Emissão", "Renovação", "Estado"
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

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadContracts());

        ModernButton batchBillingBtn = UIHelper.createPrimaryButton("Facturar Avenças");
        batchBillingBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        batchBillingBtn.setToolTipText("Executar ciclo de facturação periódica em lote");
        batchBillingBtn.addActionListener(e -> openBatchBillingDialog());

        ModernButton newContractBtn = UIHelper.createSuccessButton("Novo Contrato");
        newContractBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newContractBtn.setToolTipText("Registar novo contrato recorrente");
        newContractBtn.addActionListener(e -> openNewContractDialog());

        card.add(UIHelper.tableCardTop("Gestão de Avenças e Facturação Periódica", filterBar,
                refreshBtn, batchBillingBtn, newContractBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        loadContracts();
    }

    private void loadContracts() {
        feedback.clear();
        SwingWorker<List<RecurringContractDTO>, Void> worker = new SwingWorker<>() {
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
        };
        worker.execute();
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
        kpiDueContracts.setText(due + " A Facturar");
        kpiSuspendedContracts.setText(inactive + " Inactivos");
    }

    private void openNewContractDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);

        JTextField numberField = new JTextField();
        JTextField customerIdField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<BillingFrequency> freqCombo = UIHelper.createComboBox(BillingFrequency.values(), BillingFrequency::label);

        Integer[] days = new Integer[31];
        for (int i = 1; i <= 31; i++) days[i - 1] = i;
        JComboBox<Integer> dayCombo = UIHelper.createComboBox(days, d -> "Dia " + d);

        JTextField amountField = new JTextField();
        JTextField startDateField = new JTextField(LocalDate.now().format(DATE_FMT));
        JTextField endDateField = new JTextField();
        JCheckBox autoRenewCheck = new JCheckBox("Renovação Automática do Contrato", true);
        autoRenewCheck.setOpaque(false);
        autoRenewCheck.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Nº do Contrato *:", numberField,
                "ID do Cliente *:", customerIdField,
                "Descrição do Serviço *:", descField,
                "Periodicidade *:", freqCombo,
                "Dia de Cobrança (1-31) *:", dayCombo,
                "Valor Fixo (MT) *:", amountField,
                "Data de Início (DD/MM/AAAA) *:", startDateField,
                "Data de Término (Opcional):", endDateField,
                "", autoRenewCheck
        );

        ModernFormDialog dialog = new ModernFormDialog(
                parent,
                "Registar Novo Contrato Recorrente",
                "fas-file-contract",
                "Adicionar contrato de avença com facturação periódica",
                form
        );

        dialog.setOnSave(() -> {
            String number = numberField.getText().trim();
            if (number.isEmpty()) {
                numberField.requestFocus();
                throw new IllegalArgumentException("O número do contrato é obrigatório.");
            }

            Long customerId;
            try {
                customerId = Long.parseLong(customerIdField.getText().trim());
            } catch (NumberFormatException e) {
                customerIdField.requestFocus();
                throw new IllegalArgumentException("O ID do cliente deve ser um número válido.");
            }

            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                descField.requestFocus();
                throw new IllegalArgumentException("A descrição do serviço/avença é obrigatória.");
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountField.getText().trim().replace(",", "."));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    amountField.requestFocus();
                    throw new IllegalArgumentException("O valor fixo deve ser positivo.");
                }
            } catch (Exception e) {
                amountField.requestFocus();
                throw new IllegalArgumentException("Valor fixo inválido.");
            }

            LocalDate startDate;
            try {
                startDate = LocalDate.parse(startDateField.getText().trim(), DATE_FMT);
            } catch (Exception e) {
                startDateField.requestFocus();
                throw new IllegalArgumentException("Data de início inválida. Use DD/MM/AAAA.");
            }

            LocalDate endDate = null;
            String endStr = endDateField.getText().trim();
            if (!endStr.isEmpty()) {
                try {
                    endDate = LocalDate.parse(endStr, DATE_FMT);
                    if (endDate.isBefore(startDate)) {
                        endDateField.requestFocus();
                        throw new IllegalArgumentException("A data de término não pode ser anterior à data de início.");
                    }
                } catch (Exception e) {
                    endDateField.requestFocus();
                    throw new IllegalArgumentException("Data de término inválida. Use DD/MM/AAAA.");
                }
            }

            BillingFrequency freq = (BillingFrequency) freqCombo.getSelectedItem();
            int day = (Integer) dayCombo.getSelectedItem();

            CreateRecurringContractRequest req = new CreateRecurringContractRequest(
                    number,
                    customerId,
                    desc,
                    freq,
                    day,
                    amount,
                    startDate,
                    endDate,
                    autoRenewCheck.isSelected()
            );

            contractApiClient.createContract(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Contrato registado com sucesso.");
            loadContracts();
        }
    }

    private void openBatchBillingDialog() {
        boolean confirm = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(this),
                FeedbackType.INFO,
                "Facturação Recorrente em Lote",
                "Deseja processar o ciclo de facturação de todas as avenças activas vencidas até hoje?\n\n" +
                "Serão emitidas faturas comerciais e os contratos serão actualizados automaticamente.",
                "Processar Ciclo"
        );

        if (!confirm) return;

        feedback.show(FeedbackType.INFO, "A processar ciclo de facturação periódica...");
        SwingWorker<BatchBillingResultDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected BatchBillingResultDTO doInBackground() {
                return contractApiClient.runBatchBilling(LocalDate.now());
            }

            @Override
            protected void done() {
                try {
                    BatchBillingResultDTO result = get();
                    String msg = String.format("Facturação em lote concluída com sucesso: %d contratos processados, %d faturas geradas (%s MT).",
                            result.contractsProcessed(),
                            result.invoicesGenerated(),
                            CURRENCY_FMT.format(result.totalBilledAmount()));
                    feedback.show(FeedbackType.SUCCESS, msg);
                    loadContracts();

                    if (!result.errors().isEmpty()) {
                        feedback.show(FeedbackType.WARNING, "Avisos de facturação: " + String.join("; ", result.errors()));
                    }
                } catch (Exception ex) {
                    feedback.show(FeedbackType.ERROR, "Falha na facturação em lote: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showContractActionsDialog() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentContracts.size()) return;

        RecurringContractDTO selected = currentContracts.get(row);
        String[] options = {"Activar Contrato", "Suspender Contrato", "Cancelar Contrato", "Fechar"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Selecione a acção desejada para o contrato " + selected.contractNumber() + ":",
                "Gestão do Contrato",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[3]
        );

        ContractStatus newStatus = null;
        if (choice == 0) newStatus = ContractStatus.ACTIVE;
        else if (choice == 1) newStatus = ContractStatus.SUSPENDED;
        else if (choice == 2) newStatus = ContractStatus.CANCELLED;

        if (newStatus != null && newStatus != selected.status()) {
            final ContractStatus statusToApply = newStatus;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    contractApiClient.updateStatus(selected.id(), statusToApply);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        feedback.show(FeedbackType.SUCCESS, "Estado do contrato alterado para " + statusToApply.name());
                        loadContracts();
                    } catch (Exception ex) {
                        feedback.show(FeedbackType.ERROR, "Erro ao alterar estado: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }
}
