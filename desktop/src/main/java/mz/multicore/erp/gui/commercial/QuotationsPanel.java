package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/** Cotações ao cliente: listagem, decisão e conversão em encomenda. */
public final class QuotationsPanel extends JPanel {

    private static final int COL_ID = 0, COL_NUMBER = 1, COL_STATUS = 6, COL_VALIDITY = 7;

    private final ComercialApiClient apiClient;
    private final Supplier<List<ClientDTO>> clients;
    private final Supplier<List<ProductDTO>> products;
    private final Supplier<List<WarehouseDTO>> warehouses;
    private final Runnable ordersRefresh;
    private final DefaultTableModel model;
    private final JTable table;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final CardLayout pages = new CardLayout();
    private final QuotationEditorForm editorForm;
    private final DocumentEditorHost editorHost;
    private final ProductionApiClient productionApiClient;
    private final JComboBox<String> businessUnitCombo;

    public QuotationsPanel(ComercialApiClient apiClient, Supplier<List<ClientDTO>> clients,
                           Supplier<List<ProductDTO>> products, Supplier<List<WarehouseDTO>> warehouses,
                           Runnable ordersRefresh) {
        this(apiClient, clients, products, warehouses, ordersRefresh, null);
    }

    public QuotationsPanel(ComercialApiClient apiClient, Supplier<List<ClientDTO>> clients,
                           Supplier<List<ProductDTO>> products, Supplier<List<WarehouseDTO>> warehouses,
                           Runnable ordersRefresh, ProductionApiClient productionApiClient) {
        this.apiClient = apiClient;
        this.clients = clients;
        this.products = products;
        this.warehouses = warehouses;
        this.ordersRefresh = ordersRefresh;
        this.productionApiClient = productionApiClient;

        setLayout(pages);
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel listPage = new JPanel(new BorderLayout(0, 8));
        listPage.setOpaque(false);
        listPage.add(feedback, BorderLayout.NORTH);

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Cotação");
        newBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newBtn.addActionListener(e -> openEditor());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        model = new DefaultTableModel(new String[]{"ID", "Nº Cotação", "Data", "Cliente", "NUIT", "Total", "Estado", "Validade", "Encomenda", "Negócio"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(TableCellRenderers.status());
        hideColumn(COL_ID);

        JTextField search = TableFilter.searchField("Nº cotação, cliente ou NUIT…");
        businessUnitCombo = TableFilter.combo("Todos os negócios");
        JComboBox<String> status = TableFilter.combo("Todos os estados", "Rascunho", "Enviada", "Aceite", "Recusada", "Convertida", "Cancelada");
        JComboBox<String> periodo = TableFilter.periodCombo();
        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(businessUnitCombo, 9), new TableFilter.ColumnFilter(status, COL_STATUS)),
                List.of(new TableFilter.PeriodFilter(periodo, 2)));

        ActionMenuButton more = UIHelper.createActionMenuButton("Gerir Cotação")
                .addAction("Imprimir", UIHelper.icon("fas-print", 14), this::print)
                .addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), this::openSelectedEditor)
                .addAction("Marcar como enviada", UIHelper.icon("fas-paper-plane", 14), this::send)
                .addAction("Estender validade", UIHelper.icon("fas-calendar-plus", 14), this::extendValidity)
                .addAction("Gerar Ordem de Fabrico", UIHelper.icon("fas-industry", 14, UIHelper.ACCENT_CYAN), this::generateProductionOrderFromSelectedQuotation);

        ActionMenuButton decision = UIHelper.createActionMenuButton("Decisão do Cliente")
                .addAction("Recusada pelo cliente", UIHelper.icon("fas-times", 14), this::reject)
                .addAction("Aceite pelo cliente", UIHelper.icon("fas-check", 14), this::accept)
                .addAction("Cancelar cotação", UIHelper.icon("fas-ban", 14), this::cancel)
                .addAction("Converter em Encomenda", UIHelper.icon("fas-exchange-alt", 14, UIHelper.ACCENT_BLUE), this::convert)
                .addAction("Converter em Fatura", UIHelper.icon("fas-file-invoice-dollar", 14, UIHelper.APPROVED_GREEN), this::convertToInvoice);

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da cotação (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar cotação");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(search, TableFilter.label("Negócio:"), businessUnitCombo,
                TableFilter.label("Estado:"), status, TableFilter.label("Data:"), periodo, peekToggleBtn);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Cotações", filters, UIHelper.createRefreshButton(this::refresh), more, decision, newBtn), BorderLayout.NORTH);

        UIHelper.ensureHeadersFit(table);
        TableQuickPeekController peek = TableQuickPeekController.install(table, card,
                (p, mr) -> QuotationViewCoordinator.populateQuickPeek(p, model, mr, this::openSelectedEditor));
        peekToggleBtn.addActionListener(e -> peek.toggle());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) openSelectedEditor();
            }
        });
        card.add(scroll, BorderLayout.CENTER);
        listPage.add(card, BorderLayout.CENTER);

        editorForm = new QuotationEditorForm(this);
        editorHost = new DocumentEditorHost("Nova Cotação", editorForm.component(), this::saveEditor, this::showList, editorForm::isDirty);
        add(listPage, "LIST");
        add(editorHost, "EDITOR");
        pages.show(this, "LIST");
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.getQuotationsByCompany(companyId),
                quotations -> QuotationViewCoordinator.applyQuotations(model, businessUnitCombo, quotations),
                error -> showError("carregar cotações", error));
    }

    public void openCreate() { openEditor(); }

    private void openEditor() {
        if (warehouses.get().isEmpty() || products.get().isEmpty()) {
            showNotice(FeedbackType.WARNING, "Dados necessários", "Registe um armazém e pelo menos um produto antes de criar a cotação.");
            return;
        }
        editorForm.setOptions(clients.get(), products.get(), warehouses.get());
        editorForm.prepareCreate();
        editorHost.setEditorTitle("Nova Cotação");
        editorHost.setSaveText("Emitir Cotação");
        editorHost.setSaveEnabled(true);
        pages.show(this, "EDITOR");
    }

    private void openSelectedEditor() {
        int row = selected("editar ou consultar");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        UIHelper.loadAsync(this, () -> apiClient.getQuotationById(id), quotation -> {
            editorForm.setOptions(clients.get(), products.get(), warehouses.get());
            editorForm.load(quotation);
            boolean editable = "DRAFT".equals(quotation.status());
            editorHost.setEditorTitle((editable ? "Editar Cotação " : "Consultar Cotação ") + quotation.quotationNumber());
            editorHost.setSaveText("Guardar alterações");
            editorHost.setSaveEnabled(editable);
            pages.show(this, "EDITOR");
            if (!editable) showNotice(FeedbackType.INFO, "Cotação protegida", "Depois do envio, a cotação fica apenas para consulta. Use as operações próprias do estado.");
        }, error -> showError("carregar a cotação", error));
    }

    void saveEditor() {
        try {
            QuotationDTO loaded = editorForm.loaded();
            if (loaded == null) {
                var req = editorForm.createRequest();
                UIHelper.runWithProgress(this, "A emitir cotação…", () -> apiClient.createQuotation(req),
                        saved -> finishSave(saved, "emitida"), error -> showError("emitir a cotação", error));
            } else {
                var request = editorForm.updateRequest();
                UIHelper.runWithProgress(this, "A guardar alterações…", () -> apiClient.updateQuotation(loaded.id(), request),
                        saved -> finishSave(saved, "actualizada"), error -> showError("actualizar a cotação", error));
            }
        } catch (RuntimeException error) {
            showNotice(FeedbackType.ERROR, "Dados incompletos", error.getMessage());
        }
    }

    private void finishSave(QuotationDTO saved, String action) {
        editorForm.markClean();
        ToastManager.success(this, "Cotação " + saved.quotationNumber() + " " + action + "; total " + saved.totalAmount() + " MT.");
        showList();
        refresh();
    }

    private void showList() { pages.show(this, "LIST"); }

    private void send() {
        int r = selected("marcar como enviada");
        if (r >= 0) QuotationOperationsCoordinator.send(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)));
    }

    private void accept() {
        int r = selected("registar a aceitação");
        if (r >= 0) QuotationOperationsCoordinator.accept(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)));
    }

    private void reject() {
        int r = selected("registar a recusa");
        if (r >= 0) QuotationOperationsCoordinator.reject(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)));
    }

    private void cancel() {
        int r = selected("cancelar");
        if (r >= 0) QuotationOperationsCoordinator.cancel(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)));
    }

    private void extendValidity() {
        int r = selected("estender a validade");
        if (r >= 0) QuotationOperationsCoordinator.extendValidity(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)), String.valueOf(model.getValueAt(r, COL_VALIDITY)));
    }

    private void generateProductionOrderFromSelectedQuotation() {
        int r = selected("gerar ordem de fabrico");
        if (r >= 0) QuotationOperationsCoordinator.generateProductionOrder(this, productionApiClient, apiClient, (Long) model.getValueAt(r, COL_ID), warehouses);
    }

    private void convert() {
        int r = selected("converter");
        if (r >= 0) QuotationOperationsCoordinator.convert(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)), ordersRefresh);
    }

    void convertToInvoice() {
        int r = selected("converter em fatura");
        if (r >= 0) QuotationOperationsCoordinator.convertToInvoice(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)), ordersRefresh);
    }

    private void print() {
        int r = selected("imprimir");
        if (r >= 0) {
            // Delega a impressão profissional via PrintPreviewDialog.show(...)
            QuotationOperationsCoordinator.print(this, apiClient, (Long) model.getValueAt(r, COL_ID), String.valueOf(model.getValueAt(r, COL_NUMBER)));
        }
    }

    private int selected(String action) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) showNotice(FeedbackType.WARNING, "Seleccione uma cotação", "Escolha uma cotação na tabela para " + action + ".");
        return row;
    }

    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }

    void showSuccess(String message) {
        showNotice(FeedbackType.SUCCESS, "Operação Concluída", message);
    }

    void showError(String action, Throwable error) {
        showNotice(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }

    void showNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }
}
