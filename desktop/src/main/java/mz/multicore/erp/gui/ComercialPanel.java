package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.DocumentEditorHost;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.ProductLookupDialog;
import mz.multicore.erp.gui.components.CustomerCreditValidator;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.commercial.CommercialMovementsPanel;
import mz.multicore.erp.gui.commercial.OutstandingAccountsPanel;
import mz.multicore.erp.gui.commercial.CommercialNotesPanel;
import mz.multicore.erp.gui.commercial.DeliveryGuidesPanel;
import mz.multicore.erp.gui.commercial.QuotationsPanel;
import mz.multicore.erp.gui.commercial.ReceiptsPanel;
import mz.multicore.erp.gui.commercial.BillOrderDialog;
import mz.multicore.erp.gui.commercial.CancelOrderDialog;
import mz.multicore.erp.gui.commercial.CommercialProductionOrderDialog;
import mz.multicore.erp.gui.commercial.OrderDetailsDialog;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MovimentosApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

public class ComercialPanel extends JPanel {

    public record BusinessUnitOption(Long id, String name) {
        @Override
        public String toString() { return name; }
    }

    final ComercialApiClient comercialApiClient;
    final PrintApiClient printApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final FinanceApiClient financeApiClient;
    private final JTabbedPane commercialTabs;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    // TAB 1: FATURAÇÃO ELEMENTS
    JComboBox<String> clientCombo;
    JTextField invoiceCustomerNameField;
    JComboBox<String> warehouseCombo;
    JComboBox<BusinessUnitOption> invoiceBusinessUnitCombo;
    List<BusinessUnitDTO> businessUnitsList = new ArrayList<>();
    ProductSearchComboBox productCombo;
    DecimalField discountField;
    JTextField serialField;
    mz.multicore.erp.gui.components.BarcodeScanField invoiceBarcodeField;
    DefaultTableModel linesTableModel;
    JTable linesTable;
    JLabel totalLabel;
    boolean invoiceGridEditable, syncingInvoiceGrid;
    JComboBox<String> invBusinessUnitCombo;
    DefaultTableModel invoicesTableModel;
    mz.multicore.erp.gui.components.TablePager invoicesPager;
    JTable invoicesTable;

    // TAB 4: ENCOMENDAS ELEMENTS
    JComboBox<mz.multicore.erp.modules.comercial.model.OrderKind> orderKindCombo;
    JComboBox<String> orderClientCombo;
    JTextField orderClientWalkInField;
    JComboBox<String> orderPaymentTermsCombo;
    JLabel orderClientCreditWarningLabel;
    JComboBox<String> orderWarehouseCombo, orderDestinationCombo;
    JTextField orderNotesField;
    ProductSearchComboBox orderProductCombo;
    DecimalField orderDiscountField;
    JTextField orderSerialField;
    mz.multicore.erp.gui.components.BarcodeScanField orderBarcodeField;
    DefaultTableModel orderLinesTableModel;
    JTable orderLinesTable;
    JLabel orderTotalLabel, orderLoadLabel;
    JComboBox<String> ecBusinessUnitCombo;
    DefaultTableModel ordersTableModel;
    mz.multicore.erp.gui.components.TablePager ordersPager;
    JTable ordersTable;

    static final int ORDERS_COL_ID = 0, ORDERS_COL_STATUS = 3, ORDERS_COL_KIND = 6;
    static final int ORDERS_COL_ORIGIN = 7, ORDERS_COL_DELIVERY = 8, ORDERS_COL_BUSINESS_UNIT = 9;

    List<ClientDTO> clientsList = new ArrayList<>();
    final List<ProductDTO> productsList = new ArrayList<>();
    List<WarehouseDTO> warehousesList = new ArrayList<>();

    final List<CreateInvoiceLineRequest> draftLines = new ArrayList<>();
    BigDecimal draftSubtotal = BigDecimal.ZERO, draftTax = BigDecimal.ZERO, draftTotal = BigDecimal.ZERO;
    BigDecimal getDraftTotal() { return draftTotal; }
    void setDraftTotal(BigDecimal draftTotal) { this.draftTotal = draftTotal; }

    final List<CreateInvoiceLineRequest> draftOrderLines = new ArrayList<>();
    final List<BigDecimal> draftOrderUnitPrices = new ArrayList<>();
    BigDecimal draftOrderSubtotal = BigDecimal.ZERO, draftOrderTax = BigDecimal.ZERO, draftOrderTotal = BigDecimal.ZERO;

    private final POSApiClient posApiClient;
    private final MovimentosApiClient movimentosApiClient;

    JPanel invoiceFormContent;
    private mz.multicore.erp.modules.comercial.dto.InvoiceDTO lastCreatedInvoice;
    mz.multicore.erp.modules.comercial.dto.InvoiceDTO getLastCreatedInvoice() { return lastCreatedInvoice; }
    void setLastCreatedInvoice(mz.multicore.erp.modules.comercial.dto.InvoiceDTO inv) { this.lastCreatedInvoice = inv; }

    JPanel orderFormContent;
    DocumentEditorHost orderEditor;
    OrderDTO editingOrder;
    private boolean orderEditorDirty;
    boolean orderGridEditable, syncingOrderGrid;
    boolean inStopInvoiceCellEditing = false;
    OrderDTO lastCreatedOrder;
    private List<OrderDTO> cachedOrders = new ArrayList<>();
    CardLayout encomendasCards, faturacaoCards;
    JPanel encomendasHost, faturacaoHost;

    private final CommercialMovementsPanel movementsPanel;
    private final OutstandingAccountsPanel outstandingAccountsPanel;
    private final CommercialNotesPanel notesPanel;
    final DeliveryGuidesPanel deliveryGuidesPanel;
    private final QuotationsPanel quotationsPanel;
    private final ReceiptsPanel receiptsPanel;
    private final BillOrderDialog billOrderDialog;
    private final CancelOrderDialog cancelOrderDialog;
    final OrderDetailsDialog orderDetailsDialog;
    private final PromotionsPanel promotionsPanel;
    final mz.multicore.erp.desktop.client.ProductionApiClient productionApiClient;
    final mz.multicore.erp.desktop.client.FleetApiClient fleetApiClient;

    DeliveryGuidesPanel getDeliveryGuidesPanel() { return deliveryGuidesPanel; }
    mz.multicore.erp.desktop.client.ProductionApiClient getProductionApiClient() { return productionApiClient; }
    mz.multicore.erp.desktop.client.FleetApiClient getFleetApiClient() { return fleetApiClient; }
    InventoryApiClient getInventoryApiClient() { return inventoryApiClient; }
    FinanceApiClient getFinanceApiClient() { return financeApiClient; }
    ReceiptsPanel getReceiptsPanel() { return receiptsPanel; }
    List<OrderDTO> getCachedOrders() { return cachedOrders; }
    void setCachedOrders(List<OrderDTO> cachedOrders) { this.cachedOrders = cachedOrders; }
    BillOrderDialog getBillOrderDialog() { return billOrderDialog; }
    CancelOrderDialog getCancelOrderDialog() { return cancelOrderDialog; }

    public ComercialPanel(
            ComercialApiClient comercialApiClient, InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient, CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient, POSApiClient posApiClient,
            MovimentosApiClient movimentosApiClient, PromotionApiClient promotionApiClient,
            PrintApiClient printApiClient, Runnable openWarehouseTransfers
    ) {
        this(comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient,
                posApiClient, movimentosApiClient, promotionApiClient, printApiClient, openWarehouseTransfers,
                null, null);
    }

    public ComercialPanel(
            ComercialApiClient comercialApiClient, InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient, CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient, POSApiClient posApiClient,
            MovimentosApiClient movimentosApiClient, PromotionApiClient promotionApiClient,
            PrintApiClient printApiClient, Runnable openWarehouseTransfers,
            mz.multicore.erp.desktop.client.ProductionApiClient productionApiClient,
            mz.multicore.erp.desktop.client.FleetApiClient fleetApiClient
    ) {
        this(comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient,
                posApiClient, movimentosApiClient, promotionApiClient, printApiClient,
                productionApiClient, fleetApiClient, openWarehouseTransfers);
    }

    public ComercialPanel(
            ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient,
            CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient,
            POSApiClient posApiClient,
            MovimentosApiClient movimentosApiClient,
            PromotionApiClient promotionApiClient,
            PrintApiClient printApiClient,
            mz.multicore.erp.desktop.client.ProductionApiClient productionApiClient,
            mz.multicore.erp.desktop.client.FleetApiClient fleetApiClient,
            Runnable openWarehouseTransfers
    ) {
        this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.financeApiClient = financeApiClient;
        this.posApiClient = posApiClient;
        this.movimentosApiClient = movimentosApiClient;
        this.productionApiClient = productionApiClient;
        this.fleetApiClient = fleetApiClient;
        this.movementsPanel = new CommercialMovementsPanel(movimentosApiClient, printApiClient, inventoryApiClient);
        this.outstandingAccountsPanel = new OutstandingAccountsPanel(comercialApiClient, financeApiClient, posApiClient);
        this.notesPanel = new CommercialNotesPanel(comercialApiClient, creditNoteApiClient, debitNoteApiClient,
                () -> List.copyOf(warehousesList), printApiClient);
        this.deliveryGuidesPanel = new DeliveryGuidesPanel(comercialApiClient, this::loadOrdersTable,
                openWarehouseTransfers, () -> List.copyOf(clientsList), () -> List.copyOf(warehousesList),
                printApiClient);
        this.receiptsPanel = new ReceiptsPanel(comercialApiClient, this::loadInvoicesTable);
        this.quotationsPanel = new QuotationsPanel(comercialApiClient, () -> List.copyOf(clientsList),
                () -> List.copyOf(productsList), () -> List.copyOf(warehousesList), this::loadOrdersTable,
                productionApiClient);
        this.billOrderDialog = new BillOrderDialog(this, comercialApiClient, printApiClient,
                this::loadInvoicesTable, this::loadOrdersTable);
        this.cancelOrderDialog = new CancelOrderDialog(this, comercialApiClient, this::loadOrdersTable);
        this.orderDetailsDialog = new OrderDetailsDialog(this, comercialApiClient, printApiClient,
                this::loadOrdersTable);
        this.promotionsPanel = new PromotionsPanel(promotionApiClient, comercialApiClient);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        commercialTabs = new JTabbedPane();
        JTabbedPane tabbedPane = commercialTabs;
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        // Rótulos sem o código da série: com dez separadores em SCROLL_TAB_LAYOUT, o que não cabe
        // desaparece atrás das setas. Guarda em CommercialTabStripFitsTest. Ordem = ciclo comercial.
        tabbedPane.addTab("Cotações", UIHelper.icon("fas-file-signature", 16, UIHelper.ACCENT_BLUE),
                quotationsPanel);

        JPanel tabFaturacao = createFaturacaoTab();
        tabbedPane.addTab("Faturação", UIHelper.icon("fas-file-invoice", 16, UIHelper.MODULE_COMERCIAL), tabFaturacao);

        JPanel tabEncomendas = createEncomendasTab();
        tabbedPane.addTab("Pedidos & Separação", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_CYAN), tabEncomendas);

        boolean sellerView = "SELLER".equalsIgnoreCase(CurrentUserContext.getRole());
        if (!sellerView) {
        tabbedPane.addTab("Recibos", UIHelper.icon("fas-receipt", 16, UIHelper.APPROVED_GREEN), receiptsPanel);

        // TAB 5: GUIAS DE REMESSA (GR)
        tabbedPane.addTab("Guias", UIHelper.icon("fas-truck", 16, UIHelper.ACCENT_SKY),
                deliveryGuidesPanel);

        // TAB 6: NOTAS DE CRÉDITO (NC)
        tabbedPane.addTab("Notas de Crédito", UIHelper.icon("fas-undo-alt", 16, UIHelper.PENDING_YELLOW), notesPanel.creditTab());

        // TAB 7: NOTAS DE DÉBITO (ND)
        tabbedPane.addTab("Notas de Débito", UIHelper.icon("fas-plus-circle", 16, UIHelper.ACCENT_ORANGE), notesPanel.debitTab());

        // TAB 8: CONTAS CORRENTES (FIADOS)
        tabbedPane.addTab("Contas Correntes", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.REJECTED_RED), outstandingAccountsPanel);

        // TAB 9: PROMOÇÕES
        tabbedPane.addTab("Promoções", UIHelper.icon("fas-tags", 16, UIHelper.ACCENT_PINK), promotionsPanel);

        // TAB 10: MOVIMENTOS (vista unificada de todos os documentos comerciais)
        tabbedPane.addTab("Movimentos", UIHelper.icon("fas-list-alt", 16, UIHelper.ACCENT), movementsPanel);
        }

        add(feedback, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    public String activeTabTitle() {
        int index = commercialTabs.getSelectedIndex();
        return index < 0 ? "" : commercialTabs.getTitleAt(index);
    }

    public void onTabChanged(Runnable listener) {
        commercialTabs.addChangeListener(e -> listener.run());
    }

    public void showCustomerOrders() {
        for (int index = 0; index < commercialTabs.getTabCount(); index++) {
            if (!commercialTabs.getTitleAt(index).startsWith("Pedidos")) continue;
            commercialTabs.setSelectedIndex(index);
            loadOrdersTable();
            return;
        }
    }

    public void showQuotations() {
        for (int index = 0; index < commercialTabs.getTabCount(); index++) {
            if (!commercialTabs.getTitleAt(index).startsWith("Cotações")) continue;
            commercialTabs.setSelectedIndex(index);
            quotationsPanel.refresh();
            return;
        }
    }

    public void startNewOrder() {
        showCustomerOrders();
        openOrderEditor();
    }

    public void startNewQuotation() {
        showQuotations();
        quotationsPanel.openCreate();
    }

    public void showMovements() {
        for (int index = 0; index < commercialTabs.getTabCount(); index++) {
            if (commercialTabs.getTitleAt(index).startsWith("Movimentos")) {
                commercialTabs.setSelectedIndex(index);
                if (movementsPanel != null) {
                    movementsPanel.refresh();
                }
                return;
            }
        }
    }


    private JPanel createFaturacaoTab() {
        return CommercialInvoicesView.create(this);
    }
    void openInvoiceEditor() {
        CommercialInvoiceOperations.openInvoiceEditor(this);
    }

    void backToInvoicesList() {
        CommercialInvoiceOperations.backToInvoicesList(this);
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    void saveInvoiceFromEditor() {
        CommercialInvoiceDraftActions.saveInvoiceFromEditor(this);
    }

    public void onPanelSelected() {
        CommercialMetadataCoordinator.loadClientsAndProducts(this);
        CommercialMetadataCoordinator.loadWarehouses(this);
        loadInvoicesTable();
        loadOrdersTable();
        quotationsPanel.refresh();
        if ("SELLER".equalsIgnoreCase(CurrentUserContext.getRole())) return;
        CommercialMetadataCoordinator.loadBusinessUnits(this);
        receiptsPanel.refresh();
        deliveryGuidesPanel.refresh();
        notesPanel.refresh();
        outstandingAccountsPanel.refresh();
        movementsPanel.refresh();
        promotionsPanel.reload();
    }


    void addDraftLine() { CommercialInvoiceDraftActions.addDraftLine(this); }
    void handleInvoiceBarcodeScanned(String code) { CommercialInvoiceDraftActions.handleInvoiceBarcodeScanned(this, code); }
    void syncInvoiceLineFromGrid(int row, int column) { CommercialInvoiceDraftActions.syncInvoiceLineFromGrid(this, row, column); }
    void removeSelectedInvoiceDraftLine() { CommercialInvoiceDraftActions.removeSelectedInvoiceDraftLine(this); }
    void recalculateInvoiceTotals() { CommercialInvoiceDraftActions.recalculateInvoiceTotals(this); }
    void refreshInvoiceGrid() { CommercialInvoiceDraftActions.refreshInvoiceGrid(this); }
    CreateInvoiceRequest buildInvoiceRequest() { return CommercialInvoiceDraftActions.buildInvoiceRequest(this); }
    void resetInvoiceDraft() { CommercialInvoiceDraftActions.resetInvoiceDraft(this); }
    void stopInvoiceCellEditing() { CommercialInvoiceDraftActions.stopInvoiceCellEditing(this); }

    void cancelSelectedInvoice() { CommercialInvoiceOperations.cancelSelectedInvoice(this); }
    void paySelectedInvoice() { CommercialInvoiceOperations.paySelectedInvoice(this); }
    public void loadInvoicesTable() { CommercialInvoiceOperations.loadInvoicesTable(this); }
    void loadInvoicesPage(int page, int size) { CommercialInvoiceOperations.loadInvoicesPage(this, page, size); }

    private JPanel createEncomendasTab() {
        return CommercialOrdersView.create(this);
    }

    void openOrderEditor() { CommercialOrderOperations.openOrderEditor(this); }
    void openSelectedOrderEditor() { CommercialOrderEditorActions.openSelected(this); }
    void backToOrdersList() { CommercialOrderOperations.backToOrdersList(this); }
    void saveOrderFromEditor() { CommercialOrderOperations.saveOrderFromEditor(this); }

    boolean isOrderEditorDirty() { return orderEditorDirty; }
    void markOrderEditorDirty() { orderEditorDirty = true; }
    void clearOrderEditorDirty() { orderEditorDirty = false; }

    void openInvoiceProductLookup() { CommercialInvoiceDraftActions.openInvoiceProductLookup(this); }
    void applyInvoiceProductsFromLookup(List<ProductDTO> selected) { CommercialInvoiceDraftActions.applyInvoiceProductsFromLookup(this, selected); }
    void openOrderProductLookup() { CommercialOrderOperations.openOrderProductLookup(this); }

    void addDraftOrderLine() { CommercialOrderOperations.addDraftOrderLine(this); }
    void removeSelectedDraftOrderLine() { CommercialOrderOperations.removeSelectedDraftOrderLine(this); }
    void refreshOrderDraftTable() { CommercialOrderOperations.refreshOrderDraftTable(this); }
    mz.multicore.erp.modules.comercial.model.OrderKind selectedOrderKind() { return CommercialOrderOperations.selectedOrderKind(this); }
    void resetOrderDraft() { CommercialOrderOperations.resetOrderDraft(this); }

    void billSelectedOrder() { CommercialOrderOperations.billSelectedOrder(this); }
    void promptApproveOrRejectOrder(Long orderId, String orderNum) { CommercialOrderOperations.promptApproveOrRejectOrder(this, orderId, orderNum); }
    void convertSelectedOrderToGuide() { CommercialOrderOperations.convertSelectedOrderToGuide(this); }
    void generateProductionOrderFromSelectedOrder() { CommercialOrderOperations.generateProductionOrderFromSelectedOrder(this); }

    public void loadOrdersTable() { CommercialOrderOperations.loadOrdersTable(this); }
    void loadOrdersPage(int page, int size) { CommercialOrderOperations.loadOrdersPage(this, page, size); }
    public OrderDTO findCachedOrder(Long orderId) { return CommercialOrderOperations.findCachedOrder(this, orderId); }
    void convertSelectedOrderToTransfer() { CommercialOrderOperations.convertSelectedOrderToTransfer(this); }

    void openBillFromOrderDialog() { CommercialOrderOperations.openBillFromOrderDialog(this); }
    void openCancelOrderDialog() { CommercialOrderOperations.openCancelOrderDialog(this); }

    void printSelectedInvoice() {
        // Impressão segura auditada via SensitivePrintCoordinator.print(...)
        CommercialInvoiceOperations.printSelectedInvoice(this);
    }

    void printSelectedGuide() {
        // Guia de remessa impressa via SensitivePrintCoordinator.print(...)
        CommercialInvoiceOperations.printSelectedGuide(this);
    }

    void exportInvoicesTable() { CommercialInvoiceOperations.exportInvoicesTable(this); }
    void printSelectedOrder() { CommercialOrderOperations.printSelectedOrder(this); }
    void completeSelectedOrderSeparation() { CommercialOrderOperations.completeSelectedOrderSeparation(this); }
    void showSelectedOrderEvents() { CommercialOrderOperations.showSelectedOrderEvents(this); }
    void openSelectedOrderDetails() { CommercialOrderOperations.openSelectedOrderDetails(this); }
    void exportOrdersTable() { CommercialOrderOperations.exportOrdersTable(this); }

    void showCommercialLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area,
                error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }

    void showCommercialError(String action, Throwable error) {
        showCommercialNotice(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }

    public void showCommercialNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    public void showCommercialSuccess(String message) { ToastManager.success(this, message); }
}
