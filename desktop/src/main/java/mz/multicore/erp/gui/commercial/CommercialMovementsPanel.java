package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MovimentosApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Aba autónoma da vista unificada de movimentos e documentos comerciais/operacionais. */
public final class CommercialMovementsPanel extends JPanel {

    private final MovimentosApiClient apiClient;
    private final PrintApiClient printApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search;
    private final JComboBox<String> productFilter;
    private final JComboBox<String> warehouseFilter;
    private final JComboBox<String> typeFilter;
    private final DateField fromField;
    private final DateField toField;
    private final JLabel footer;
    private List<MovimentoDTO> data = List.of();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public CommercialMovementsPanel(MovimentosApiClient apiClient) {
        this(apiClient, null, null);
    }

    public CommercialMovementsPanel(MovimentosApiClient apiClient, PrintApiClient printApiClient) {
        this(apiClient, printApiClient, null);
    }

    public CommercialMovementsPanel(MovimentosApiClient apiClient, PrintApiClient printApiClient, InventoryApiClient inventoryApiClient) {
        this.apiClient = apiClient;
        this.printApiClient = printApiClient;
        this.inventoryApiClient = inventoryApiClient;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        model = new DefaultTableModel(new String[]{"Tipo", "Nº", "Cliente / Destino", "Artigos", "Qtd", "Armazém", "Data", "Estado", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        search = TableFilter.searchField("Nº, cliente, artigo ou armazém…");
        productFilter = TableFilter.combo("Todos os artigos");
        warehouseFilter = TableFilter.combo("Todos os armazéns");
        typeFilter = TableFilter.combo("Todos os tipos",
                "Fatura", "Venda POS", "Cotação", "Encomenda", "Guia de Remessa",
                "Nota de Crédito", "Nota de Débito", "Recibo", "Transferência");
        fromField = new DateField(null);
        toField = new DateField(null);

        UIHelper.styleComboBox(productFilter);
        UIHelper.styleComboBox(warehouseFilter);
        UIHelper.styleComboBox(typeFilter);

        search.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        productFilter.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        warehouseFilter.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        typeFilter.setPreferredSize(new Dimension(115, UIHelper.FORM_CONTROL_HEIGHT));
        fromField.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));
        toField.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(typeFilter, 0)),
                List.of(),
                List.of(new TableFilter.DateRangeFilter(fromField, toField, 6)),
                List.of(
                        new TableFilter.ContainsColumnFilter(productFilter, 3),
                        new TableFilter.ContainsColumnFilter(warehouseFilter, 5)
                ));

        UIHelper.onTextChange(search, this::updateFooter);
        productFilter.addActionListener(e -> updateFooter());
        warehouseFilter.addActionListener(e -> updateFooter());
        typeFilter.addActionListener(e -> updateFooter());
        UIHelper.onTextChange(fromField, this::updateFooter);
        UIHelper.onTextChange(toField, this::updateFooter);

        UIHelper.ensureHeadersFit(table);
        JPanel filterBar = TableFilter.bar(
                search,
                TableFilter.label("Artigo:"), productFilter,
                TableFilter.label("Armazém:"), warehouseFilter,
                TableFilter.label("Tipo:"), typeFilter,
                TableFilter.label("De:"), fromField,
                TableFilter.label("Até:"), toField
        );
        filterBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        TableQuickPeekController.install(table, card, (peek, mr) ->
                CommercialMovementsOperationsCoordinator.populatePeek(peek, table, model, mr));

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir movimentos");
        printBtn.setIcon(UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE));
        printBtn.addActionListener(e -> CommercialMovementsOperationsCoordinator.exportMovements(
                this, printApiClient, table, warehouseFilter, productFilter, typeFilter, fromField, toField));

        card.add(UIHelper.tableCardTop("Movimentos — Todos os Documentos Comerciais & Operacionais", filterBar,
                refresh, printBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        footer = new JLabel(" ");
        footer.setForeground(UIHelper.TEXT_MUTED);
        footer.setBorder(new EmptyBorder(8, 4, 0, 4));
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(ClientTablePagination.install(table), BorderLayout.NORTH);
        southPanel.add(footer, BorderLayout.SOUTH);
        card.add(southPanel, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        CommercialMovementsDataCoordinator.syncWarehouses(this, inventoryApiClient, companyId, data, warehouseFilter);
        UIHelper.loadAsync(this, () -> apiClient.listar(companyId, "", null, null), this::apply,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar movimentos comerciais",
                        error.getMessage(), "Tentar novamente", this::refresh));
    }

    private void apply(List<MovimentoDTO> loaded) {
        data = loaded != null ? loaded : List.of();
        CommercialMovementsDataCoordinator.populateModelAndFilters(
                data, model, productFilter, warehouseFilter, inventoryApiClient != null);
        updateFooter();
    }

    private void updateFooter() {
        String query = search.getText();
        String selectedProduct = productFilter.getSelectedIndex() > 0 ? String.valueOf(productFilter.getSelectedItem()) : null;
        String selectedWarehouse = warehouseFilter.getSelectedIndex() > 0 ? String.valueOf(warehouseFilter.getSelectedItem()) : null;
        String selectedType = typeFilter.getSelectedIndex() > 0 ? String.valueOf(typeFilter.getSelectedItem()) : null;

        CommercialMovementsDataCoordinator.MovementsSummary summary = CommercialMovementsDataCoordinator.calculateSummary(
                data, query, selectedProduct, selectedWarehouse, selectedType, fromField.getDate(), toField.getDate());
        footer.setText(CommercialMovementsDataCoordinator.formatSummary(summary));
    }
}
