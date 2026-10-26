package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/** Lista e fluxo de aprovação das guias de remessa. */
public final class DeliveryGuidesPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ComercialApiClient apiClient;
    private final Runnable ordersRefresh;
    private final Supplier<List<ClientDTO>> clients;
    private final Supplier<List<WarehouseDTO>> warehouses;
    private final PrintApiClient printApiClient;
    private final DefaultTableModel model;
    private JComboBox<String> guideBusinessUnitCombo;
    private final JTable table;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh) {
        this(apiClient, ordersRefresh, null, List::of, List::of, null);
    }

    public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh,
                               Runnable openWarehouseTransfers) {
        this(apiClient, ordersRefresh, openWarehouseTransfers, List::of, List::of, null);
    }

    public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh,
                               Runnable openWarehouseTransfers,
                               Supplier<List<ClientDTO>> clients,
                               Supplier<List<WarehouseDTO>> warehouses) {
        this(apiClient, ordersRefresh, openWarehouseTransfers, clients, warehouses, null);
    }

    public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh,
                               Runnable openWarehouseTransfers,
                               Supplier<List<ClientDTO>> clients,
                               Supplier<List<WarehouseDTO>> warehouses,
                               PrintApiClient printApiClient) {
        this.apiClient = apiClient;
        this.ordersRefresh = ordersRefresh;
        this.clients = clients;
        this.warehouses = warehouses;
        this.printApiClient = printApiClient;
        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(4, 0, 0, 0));
        ModernButton transfersBtn = null;
        if (openWarehouseTransfers != null) {
            transfersBtn = UIHelper.createSecondaryButton("Transferências entre Armazéns");
            transfersBtn.setIcon(UIHelper.icon("fas-truck", 14));
            transfersBtn.setToolTipText("Estas guias vão para o cliente. Para mover mercadoria entre "
                    + "armazéns da empresa, use a Guia de Transferência (em Stock).");
            transfersBtn.addActionListener(e -> openWarehouseTransfers.run());
        }
        add(feedback, BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        model = new DefaultTableModel(new String[]{"ID", "Nº Guia", "Data", "Encomenda", "Cliente", "Armazém",
                "Responsável", "Viatura", "Total", "Estado", "Negócio"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        hideColumn(0);
        table.putClientProperty("customDetailAction", (Runnable) this::showPackages);
        JTextField search = TableFilter.searchField("Nº guia, encomenda, cliente ou viatura…");
        guideBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        JComboBox<String> status = TableFilter.combo("Todos os estados",
                "Pendente de aprovação", "Aprovado", "Rejeitado", "Cancelado");
        JComboBox<String> periodo = TableFilter.periodCombo();
        TableFilter.install(table, search,
                List.of(
                        new TableFilter.ColumnFilter(status, 9),
                        new TableFilter.ColumnFilter(guideBusinessUnitCombo, 10)),
                List.of(new TableFilter.PeriodFilter(periodo, 2)));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Nova guia avulsa", UIHelper.icon("fas-plus", 14, UIHelper.APPROVED_GREEN), this::createStandalone)
                .addAction("Ver caixas e unidades", UIHelper.icon("fas-boxes", 14), this::showPackages)
                .addAction("Imprimir", UIHelper.icon("fas-print", 14), this::print)
                .addAction("Cancelar", UIHelper.icon("fas-ban", 14), this::cancel)
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14), this::reject);
        ModernButton approve = button("Aprovar", "fas-check", UIHelper.createSuccessButton("Aprovar"), this::approve);
        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da guia (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar guia");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(
                search,
                TableFilter.label("Negócio:"), guideBusinessUnitCombo,
                TableFilter.label("Estado:"), status,
                TableFilter.label("Data:"), periodo,
                peekToggleBtn
        );
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        if (transfersBtn == null) {
            card.add(UIHelper.tableCardTop("Guias de Remessa", filters, refreshBtn, more, approve), BorderLayout.NORTH);
        } else {
            card.add(UIHelper.tableCardTop("Guias de Remessa", filters, refreshBtn, transfersBtn, more, approve), BorderLayout.NORTH);
        }

        UIHelper.ensureHeadersFit(table);
        TableQuickPeekController peekController = TableQuickPeekController.install(table, card, (peek, mr) ->
                DeliveryGuidesOperationsCoordinator.populatePeek(peek, model, mr, this::showPackages));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    private void createStandalone() {
        List<ClientDTO> availableClients = clients.get();
        List<WarehouseDTO> availableWarehouses = warehouses.get();
        if (availableClients == null || availableClients.isEmpty()) {
            showError("criar guia", new IllegalArgumentException("Registe pelo menos um cliente antes de criar a guia."));
            return;
        }
        if (availableWarehouses == null || availableWarehouses.isEmpty()) {
            showError("criar guia", new IllegalArgumentException("Registe pelo menos um armazém antes de criar a guia."));
            return;
        }
        DeliveryGuideDTO created = StandaloneDeliveryGuideDialog.show(
                this, apiClient, availableClients, availableWarehouses);
        if (created != null) {
            ToastManager.success(this, "Guia " + created.guideNumber() + " criada e submetida para aprovação.");
            refresh();
        }
    }

    private ModernButton button(String text, String icon, ModernButton button, Runnable action) {
        button.setText(text);
        button.setIcon(UIHelper.icon(icon, 14));
        button.addActionListener(e -> action.run());
        return button;
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.getDeliveryGuidesByCompany(companyId), guides -> {
            model.setRowCount(0);
            Set<String> bus = new TreeSet<>();
            for (DeliveryGuideDTO guide : guides) {
                String buName = guide.businessUnitName() != null && !guide.businessUnitName().isBlank() ? guide.businessUnitName() : "—";
                if (!"—".equals(buName)) bus.add(buName);
                model.addRow(new Object[]{guide.id(), guide.guideNumber(),
                    guide.guideDate() == null ? "—" : guide.guideDate().format(DATE_TIME),
                    guide.orderNumber() == null ? "Guia avulsa" : guide.orderNumber(),
                    guide.clientName(), guide.warehouseName(), blank(guide.responsible()), blank(guide.vehicle()),
                    guide.totalAmount(), guide.status(), buName});
            }
            if (guideBusinessUnitCombo != null) {
                Object prev = guideBusinessUnitCombo.getSelectedItem();
                guideBusinessUnitCombo.removeAllItems();
                guideBusinessUnitCombo.addItem("Todos os negócios");
                for (String b : bus) guideBusinessUnitCombo.addItem(b);
                if (prev != null) guideBusinessUnitCombo.setSelectedItem(prev);
            }
        }, error -> showError("carregar guias", error));
    }

    private void approve() {
        int row = selected("aprovar");
        if (row < 0 || !pending(row)) return;
        DeliveryGuidesOperationsCoordinator.approve(
                this, (Long) model.getValueAt(row, 0), String.valueOf(model.getValueAt(row, 1)),
                apiClient, feedback, this::refreshBoth);
    }

    private void reject() {
        int row = selected("rejeitar");
        if (row < 0 || !pending(row)) return;
        DeliveryGuidesOperationsCoordinator.reject(
                this, (Long) model.getValueAt(row, 0), String.valueOf(model.getValueAt(row, 1)),
                apiClient, feedback, this::refreshBoth);
    }

    private void cancel() {
        int row = selected("cancelar");
        if (row < 0 || !pending(row)) return;
        DeliveryGuidesOperationsCoordinator.cancel(
                this, (Long) model.getValueAt(row, 0), String.valueOf(model.getValueAt(row, 1)),
                apiClient, feedback, this::refreshBoth);
    }

    private void print() {
        int row = selected("imprimir");
        if (row < 0) return;
        DeliveryGuidesOperationsCoordinator.print(
                this, (Long) model.getValueAt(row, 0), String.valueOf(model.getValueAt(row, 1)),
                apiClient, printApiClient, this::refresh, feedback);
    }

    private void showPackages() {
        int row = selected("consultar");
        if (row < 0) return;
        DeliveryGuidesOperationsCoordinator.showPackagesModal(
                this, (Long) model.getValueAt(row, 0), apiClient, feedback);
    }

    private int selected(String action) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) feedback.show(FeedbackType.WARNING, "Seleccione uma guia",
                "Escolha uma guia na tabela para " + action + ".", null, null);
        return row;
    }

    private boolean pending(int row) {
        if ("PENDING_APPROVAL".equals(String.valueOf(model.getValueAt(row, 9)))) return true;
        feedback.show(FeedbackType.WARNING, "Acção indisponível",
                "Esta acção só é permitida para guias pendentes de aprovação.", null, null);
        return false;
    }

    private void refreshBoth() { refresh(); if (ordersRefresh != null) ordersRefresh.run(); }
    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }
    private static String blank(String value) { return value == null || value.isBlank() ? "—" : value; }
    private void showError(String action, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage(), null, null);
    }
}
