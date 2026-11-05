package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Caso de uso autónomo de contas correntes e recebimentos tardios. */
public final class OutstandingAccountsPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ComercialApiClient comercialApiClient;
    private final FinanceApiClient financeApiClient;
    private final POSApiClient posApiClient;
    private final DefaultTableModel model;
    private final JTable table;
    private final OutstandingAccountsAgingSummaryView agingSummary;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JComboBox<String> businessUnit;
    private List<InvoiceDTO> invoices = new ArrayList<>();

    public OutstandingAccountsPanel(ComercialApiClient comercialApiClient,
                                    FinanceApiClient financeApiClient,
                                    POSApiClient posApiClient) {
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.posApiClient = posApiClient;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton pay = UIHelper.createSuccessButton("Receber Pagamento");
        pay.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        pay.addActionListener(e -> receivePayment());
        ActionMenuButton collectionActions = UIHelper.createActionMenuButton("Cobranças")
                .addAction("Radar Preventivo", UIHelper.icon("fas-satellite-dish", 14, UIHelper.ACCENT_BLUE),
                        () -> OutstandingAccountsOperationsCoordinator.openRadar(this, comercialApiClient))
                .addAction("Prestações", UIHelper.icon("fas-calendar-alt", 14, UIHelper.PENDING_YELLOW),
                        () -> OutstandingAccountsOperationsCoordinator.openInstallments(this, comercialApiClient, selected(), this::refresh))
                .addAction("Devolver Fatura", UIHelper.icon("fas-undo-alt", 14, UIHelper.REJECTED_RED),
                        () -> OutstandingAccountsOperationsCoordinator.openSalesReturn(this, comercialApiClient, selected(), this::refresh));
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        model = new DefaultTableModel(
                new String[]{"Nº Fatura", "Data", "Negócio", "Cliente", "NUIT", "Total", "Pago", "Em Dívida",
                        "Vencimento", "Dias em Atraso", "Antiguidade", "Estado"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        UIHelper.ensureColumnFits(table, 10, "Corrente (por vencer)");
        UIHelper.ensureColumnFits(table, 11, "Parcialmente paga");
        for (int column : new int[]{5, 6, 7}) table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(11).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Nº fatura, cliente ou NUIT…");
        search.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        businessUnit = TableFilter.combo("Todos os negócios");
        UIHelper.styleComboBox(businessUnit);
        businessUnit.setPreferredSize(new Dimension(165, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> status = TableFilter.combo("Todos os estados", "Aprovado", "Parcialmente paga");
        UIHelper.styleComboBox(status);
        status.setPreferredSize(new Dimension(165, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> aging = TableFilter.combo(agingLabels());
        UIHelper.styleComboBox(aging);
        aging.setPreferredSize(new Dimension(175, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> period = TableFilter.periodCombo();
        UIHelper.styleComboBox(period);
        period.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(businessUnit, 2),
                        new TableFilter.ColumnFilter(status, 11),
                        new TableFilter.ColumnFilter(aging, 10)),
                List.of(new TableFilter.PeriodFilter(period, 1)));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da conta (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar conta");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(
                search,
                TableFilter.label("Negócio:"), businessUnit,
                TableFilter.label("Estado:"), status,
                TableFilter.label("Idade:"), aging,
                TableFilter.label("Data:"), period,
                peekToggleBtn
        );
        filters.setBorder(new EmptyBorder(0, 0, 8, 0));
        agingSummary = new OutstandingAccountsAgingSummaryView();
        JPanel filterHeader = new JPanel(new BorderLayout(0, 4));
        filterHeader.setOpaque(false);
        filterHeader.add(filters, BorderLayout.NORTH);
        filterHeader.add(agingSummary, BorderLayout.SOUTH);

        TableQuickPeekController peekController = TableQuickPeekController.install(table, card,
                (peek, mr) -> OutstandingAccountsOperationsCoordinator.populatePeek(peek, model, mr, pay::doClick));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        card.add(UIHelper.tableCardTop("Contas Correntes — Faturas com Saldo em Dívida", filterHeader,
                refresh, collectionActions, pay), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    private static String[] agingLabels() {
        AgingBucket[] values = AgingBucket.values();
        String[] labels = new String[values.length + 1];
        labels[0] = "Toda a antiguidade";
        for (int i = 0; i < values.length; i++) labels[i + 1] = values[i].label();
        return labels;
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getOutstandingInvoicesByCompany(companyId), this::apply,
                error -> showError("carregar contas correntes", error));
        UIHelper.loadAsync(this, comercialApiClient::getReceivablesAging,
                agingSummary::showSummary, error -> agingSummary.clear());
    }

    private void apply(List<InvoiceDTO> loaded) {
        invoices = loaded;
        model.setRowCount(0);
        Set<String> bus = new TreeSet<>();
        for (InvoiceDTO invoice : invoices) {
            BigDecimal paid = invoice.amountPaid() == null ? BigDecimal.ZERO : invoice.amountPaid();
            String bu = invoice.businessUnitName() == null || invoice.businessUnitName().isBlank() ? "-" : invoice.businessUnitName();
            if (!"-".equals(bu)) bus.add(bu);
            model.addRow(new Object[]{invoice.invoiceNumber(),
                    invoice.createdAt() == null ? "-" : invoice.createdAt().format(DATE),
                    bu, invoice.clientName(), invoice.clientTaxId(), invoice.totalAmount(), paid,
                    invoice.outstandingAmount(),
                    invoice.dueDate() == null ? "-" : invoice.dueDate().format(DATE),
                    invoice.daysOverdue() == 0 ? "—" : invoice.daysOverdue(),
                    invoice.agingBucket() == null ? "-" : invoice.agingBucket().label(),
                    invoice.status().name()});
        }
        if (businessUnit != null) {
            String selectedBu = (String) businessUnit.getSelectedItem();
            businessUnit.removeAllItems();
            businessUnit.addItem("Todos os negócios");
            for (String b : bus) businessUnit.addItem(b);
            if (selectedBu != null) businessUnit.setSelectedItem(selectedBu);
        }
    }

    private InvoiceDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= invoices.size()) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.", null, null);
            return null;
        }
        return invoices.get(row);
    }

    private void receivePayment() {
        InvoiceDTO invoice = selected();
        if (invoice == null) return;
        OutstandingAccountsOperationsCoordinator.receivePayment(this, invoice, financeApiClient, posApiClient,
                this::refresh, error -> showError("carregar contas de tesouraria", error),
                (title, msg) -> feedback.show(FeedbackType.ERROR, title, msg, null, null));
    }

    private void showError(String action, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage(), null, null);
    }
}
