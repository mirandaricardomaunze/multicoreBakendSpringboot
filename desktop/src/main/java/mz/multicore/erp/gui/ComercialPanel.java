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
    boolean invoiceGridEditable;
    boolean syncingInvoiceGrid;

    JComboBox<String> invBusinessUnitCombo;
    DefaultTableModel invoicesTableModel;
    mz.multicore.erp.gui.components.TablePager invoicesPager;
    JTable invoicesTable;

    // TAB 2: RECIBOS ELEMENTS

    // TAB 3: REGISTAR CLIENTE ELEMENTS

    // TAB 4: ENCOMENDAS ELEMENTS
    JComboBox<mz.multicore.erp.modules.comercial.model.OrderKind> orderKindCombo;
    JComboBox<String> orderClientCombo;
    JTextField orderClientWalkInField;
    JComboBox<String> orderWarehouseCombo;
    /** Armazém que recebe, só visível na reposição interna. Ver docs/REPOSICAO_INTERNA_SPEC.md. */
    JComboBox<String> orderDestinationCombo;
    ProductSearchComboBox orderProductCombo;
    DecimalField orderDiscountField;
    JTextField orderSerialField;
    mz.multicore.erp.gui.components.BarcodeScanField orderBarcodeField;
    DefaultTableModel orderLinesTableModel;
    JTable orderLinesTable;
    JLabel orderTotalLabel;
    JLabel orderLoadLabel;

    JComboBox<String> ecBusinessUnitCombo;
    DefaultTableModel ordersTableModel;
    mz.multicore.erp.gui.components.TablePager ordersPager;
    JTable ordersTable;

    /** Colunas da tabela de encomendas lidas pelas acções. Nomeadas para o índice não andar solto. */
    static final int ORDERS_COL_ID = 0;
    static final int ORDERS_COL_STATUS = 3;
    static final int ORDERS_COL_KIND = 6;
    static final int ORDERS_COL_ORIGIN = 7;
    static final int ORDERS_COL_DELIVERY = 8;
    static final int ORDERS_COL_BUSINESS_UNIT = 9;

    // TAB 5: GUIAS DE REMESSA ELEMENTS

    // Seeding lists for selections
    List<ClientDTO> clientsList = new ArrayList<>();
    final List<ProductDTO> productsList = new ArrayList<>();
    List<WarehouseDTO> warehousesList = new ArrayList<>();
    
    // In-memory line items of the invoice currently being drafted
    final List<CreateInvoiceLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftSubtotal = BigDecimal.ZERO;
    private BigDecimal draftTax = BigDecimal.ZERO;
    private BigDecimal draftTotal = BigDecimal.ZERO;

    // In-memory line items of the order currently being drafted
    final List<CreateInvoiceLineRequest> draftOrderLines = new ArrayList<>();
    final List<BigDecimal> draftOrderUnitPrices = new ArrayList<>();
    BigDecimal draftOrderSubtotal = BigDecimal.ZERO;
    BigDecimal draftOrderTax = BigDecimal.ZERO;
    BigDecimal draftOrderTotal = BigDecimal.ZERO;


    private final POSApiClient posApiClient;
    private final MovimentosApiClient movimentosApiClient;

    JPanel invoiceFormContent;              // conteúdo do modal de nova fatura
    private mz.multicore.erp.modules.comercial.dto.InvoiceDTO lastCreatedInvoice;
    JPanel orderFormContent;                // conteúdo do editor de nova encomenda
    DocumentEditorHost orderEditor;
    OrderDTO editingOrder;
    private boolean orderEditorDirty;
    boolean orderGridEditable;
    boolean syncingOrderGrid;
    OrderDTO lastCreatedOrder;
    private List<OrderDTO> cachedOrders = new ArrayList<>();
    CardLayout encomendasCards;             // alterna lista <-> editor na aba Encomendas
    JPanel encomendasHost;
    CardLayout faturacaoCards;              // alterna lista <-> editor na aba Faturação
    JPanel faturacaoHost;
    private final CommercialMovementsPanel movementsPanel;
    private final OutstandingAccountsPanel outstandingAccountsPanel;
    private final CommercialNotesPanel notesPanel;
    private final DeliveryGuidesPanel deliveryGuidesPanel;
    private final QuotationsPanel quotationsPanel;
    private final ReceiptsPanel receiptsPanel;
    private final BillOrderDialog billOrderDialog;
    private final CancelOrderDialog cancelOrderDialog;
    final OrderDetailsDialog orderDetailsDialog;
    private final PromotionsPanel promotionsPanel;
    private final mz.multicore.erp.desktop.client.ProductionApiClient productionApiClient;
    private final mz.multicore.erp.desktop.client.FleetApiClient fleetApiClient;

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
            Runnable openWarehouseTransfers
    ) {
        this(comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient,
                posApiClient, movimentosApiClient, promotionApiClient, printApiClient, openWarehouseTransfers,
                null, null);
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
            Runnable openWarehouseTransfers,
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

        tabbedPane.addTab("Recibos", UIHelper.icon("fas-receipt", 16, UIHelper.APPROVED_GREEN), receiptsPanel);

        JPanel tabEncomendas = createEncomendasTab();
        tabbedPane.addTab("Pedidos & Separação", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_CYAN), tabEncomendas);

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

        add(feedback, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void showCustomerOrders() {
        for (int index = 0; index < commercialTabs.getTabCount(); index++) {
            if (!commercialTabs.getTitleAt(index).startsWith("Pedidos")) continue;
            commercialTabs.setSelectedIndex(index);
            loadOrdersTable();
            return;
        }
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
        if (clientsList.isEmpty()) {
            showCommercialNotice(FeedbackType.WARNING, "Cliente necessário", "Registe um cliente antes de emitir a fatura.");
            return;
        }
        if (warehousesList.isEmpty()) {
            showCommercialNotice(FeedbackType.WARNING, "Armazém necessário",
                    "Registe um armazém para a empresa actual antes de emitir a fatura.");
            return;
        }
        resetInvoiceDraft();
        lastCreatedInvoice = null;
        invoiceGridEditable = true;
        if (invoiceBarcodeField != null) invoiceBarcodeField.setEnabled(true);
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "editor");
        }
    }

    void backToInvoicesList() {
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "list");
        }
        invoiceGridEditable = false;
        if (invoiceBarcodeField != null) invoiceBarcodeField.setEnabled(false);
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    void saveInvoiceFromEditor() {
        stopInvoiceCellEditing();
        try {
            int clientIdx = clientCombo.getSelectedIndex();
            if (clientIdx >= 0 && clientIdx < clientsList.size()) {
                ClientDTO client = clientsList.get(clientIdx);
                CustomerCreditValidator.CreditAssessment assessment =
                        CustomerCreditValidator.validateCreditPreFlight(client, draftTotal);
                if (!assessment.isApproved()) {
                    showCommercialNotice(FeedbackType.WARNING, "Crédito não autorizado", assessment.details());
                    return;
                }
            }
            CreateInvoiceRequest request = buildInvoiceRequest();
            UIHelper.runWithProgress(this, "A emitir fatura…", () -> comercialApiClient.createInvoice(request), created -> {
            lastCreatedInvoice = created;
            resetInvoiceDraft();
            if (created.status() == InvoiceStatus.PENDING_DISCOUNT_APPROVAL) {
                showCommercialNotice(FeedbackType.WARNING, "Fatura aguarda aprovação",
                        "Fatura " + created.invoiceNumber() + " emitida com desconto superior a 10%. Valor: "
                                + created.totalAmount() + " MT.");
            } else {
                ToastManager.success(this, "Fatura " + created.invoiceNumber() + " emitida · " + created.totalAmount() + " MT.");
            }
            loadInvoicesTable();
            backToInvoicesList();
            }, error -> showCommercialError("emitir fatura", error));
        } catch (Exception ex) {
            showCommercialNotice(FeedbackType.ERROR, "Não foi possível emitir a fatura",
                    ex.getMessage() == null ? "Tente novamente." : ex.getMessage());
        }
    }

    public void onPanelSelected() {
        loadClientsAndProducts();
        loadWarehouses();
        loadBusinessUnits();
        loadInvoicesTable();
        receiptsPanel.refresh();
        loadOrdersTable();
        quotationsPanel.refresh();
        deliveryGuidesPanel.refresh();
        notesPanel.refresh();
        outstandingAccountsPanel.refresh();
        movementsPanel.refresh();
        promotionsPanel.reload();
    }

    private void loadClientsAndProducts() {
        UIHelper.loadAsync(this, () -> new CommercialMetadata(comercialApiClient.getAllClients(),
                        comercialApiClient.getAllProducts()), this::applyClientsAndProducts,
                error -> showCommercialLoadError("clientes e produtos", error));
    }

    private void applyClientsAndProducts(CommercialMetadata metadata) {
        clientCombo.removeAllItems();
        orderClientCombo.removeAllItems();
        clientsList = metadata.clients();
        productsList.clear();
        productsList.addAll(metadata.products());

        // Encomendas aceitam venda sem cliente registado — primeiro item do combo.
        orderClientCombo.addItem("Consumidor Final (sem registo)");

        for (ClientDTO c : clientsList) {
            clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
            orderClientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }

        productCombo.setProducts(productsList);
        orderProductCombo.setProducts(productsList);
    }

    private String productLabel(ProductDTO p) {
        String code = p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        if (p.barcode() != null && !p.barcode().isBlank()) {
            return code + " | " + p.barcode() + " - " + p.name();
        }
        return code + " - " + p.name();
    }

    private void loadWarehouses() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getWarehousesByCompany(companyId), this::applyWarehouses,
                error -> showCommercialLoadError("armazéns", error));
    }

    private void applyWarehouses(List<WarehouseDTO> loaded) {
        warehouseCombo.removeAllItems();
        orderWarehouseCombo.removeAllItems();
        if (orderDestinationCombo != null) orderDestinationCombo.removeAllItems();
        warehousesList = loaded;

        for (WarehouseDTO w : warehousesList) {
            warehouseCombo.addItem(w.name());
            orderWarehouseCombo.addItem(w.name());
            if (orderDestinationCombo != null) orderDestinationCombo.addItem(w.name());
        }
    }

    private void loadBusinessUnits() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> comercialApiClient.getActiveBusinessUnits(companyId), this::applyBusinessUnits,
                error -> { /* linhas opcionais, falha suave */ });
    }

    private void applyBusinessUnits(List<BusinessUnitDTO> loaded) {
        businessUnitsList = loaded != null ? loaded : new ArrayList<>();
        if (invoiceBusinessUnitCombo != null) {
            invoiceBusinessUnitCombo.removeAllItems();
            invoiceBusinessUnitCombo.addItem(new BusinessUnitOption(null, "— Misto (Sem restrição) —"));
            for (BusinessUnitDTO bu : businessUnitsList) {
                invoiceBusinessUnitCombo.addItem(new BusinessUnitOption(bu.id(), bu.code() + " - " + bu.name()));
            }
        }
        if (invBusinessUnitCombo != null) {
            Object current = invBusinessUnitCombo.getSelectedItem();
            invBusinessUnitCombo.removeAllItems();
            invBusinessUnitCombo.addItem("Todos os negócios");
            invBusinessUnitCombo.addItem("Misto");
            for (BusinessUnitDTO bu : businessUnitsList) {
                invBusinessUnitCombo.addItem(bu.name());
            }
            if (current != null) invBusinessUnitCombo.setSelectedItem(current);
        }
        if (ecBusinessUnitCombo != null) {
            Object current = ecBusinessUnitCombo.getSelectedItem();
            ecBusinessUnitCombo.removeAllItems();
            ecBusinessUnitCombo.addItem("Todos os negócios");
            ecBusinessUnitCombo.addItem("—");
            for (BusinessUnitDTO bu : businessUnitsList) {
                ecBusinessUnitCombo.addItem(bu.name());
            }
            if (current != null) ecBusinessUnitCombo.setSelectedItem(current);
        }
    }


    void addDraftLine() {
        if (!invoiceGridEditable) return;
        stopInvoiceCellEditing();
        draftLines.add(new CreateInvoiceLineRequest(
                0L, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        refreshInvoiceGrid();
        int modelRow = linesTableModel.getRowCount() - 1;
        int rowCount = linesTable.getRowCount();
        if (modelRow >= 0 && rowCount > 0) {
            int viewRow = Math.min(Math.max(0, modelRow), rowCount - 1);
            try {
                if (modelRow < rowCount) {
                    viewRow = linesTable.convertRowIndexToView(modelRow);
                }
            } catch (Exception ignored) {
                viewRow = rowCount - 1;
            }
            if (viewRow >= 0 && viewRow < rowCount) {
                final int targetRow = viewRow;
                try {
                    linesTable.setRowSelectionInterval(targetRow, targetRow);
                    linesTable.scrollRectToVisible(linesTable.getCellRect(targetRow, 0, true));
                } catch (Exception ignored) {
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (targetRow < linesTable.getRowCount()) {
                            linesTable.editCellAt(targetRow, 0);
                            if (linesTable.getEditorComponent() != null) {
                                linesTable.getEditorComponent().requestFocusInWindow();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    void handleInvoiceBarcodeScanned(String code) {
        if (!invoiceGridEditable) return;
        ProductDTO product = productsList.stream().filter(p ->
                code.equalsIgnoreCase(p.barcode())
                || code.equalsIgnoreCase(p.sku())
                || (p.reference() != null && code.equalsIgnoreCase(p.reference()))
        ).findFirst().orElse(null);

        if (product == null) {
            Toolkit.getDefaultToolkit().beep();
            showCommercialNotice(FeedbackType.WARNING, "Artigo não encontrado",
                    "Nenhum produto cadastrado com o código ou SKU: " + code);
            return;
        }

        int existingIndex = -1;
        for (int i = 0; i < draftLines.size(); i++) {
            if (product.id().equals(draftLines.get(i).productId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            CreateInvoiceLineRequest current = draftLines.get(existingIndex);
            BigDecimal newQty = current.quantity().add(BigDecimal.ONE);
            draftLines.set(existingIndex, new CreateInvoiceLineRequest(
                    product.id(), newQty, current.taxRate(), current.discountPercentage(),
                    current.batchNumber(), current.serialNumber()));
            syncInvoiceLineFromGrid(existingIndex, 1);
            linesTable.setRowSelectionInterval(existingIndex, existingIndex);
            linesTable.scrollRectToVisible(linesTable.getCellRect(existingIndex, 0, true));
        } else {
            stopInvoiceCellEditing();
            draftLines.add(new CreateInvoiceLineRequest(
                    product.id(), BigDecimal.ONE, product.effectiveTaxRate(), BigDecimal.ZERO, null, null));
            refreshInvoiceGrid();
            int newRow = linesTableModel.getRowCount() - 1;
            if (newRow >= 0) {
                linesTable.setRowSelectionInterval(newRow, newRow);
                linesTable.scrollRectToVisible(linesTable.getCellRect(newRow, 0, true));
            }
        }
    }

    void syncInvoiceLineFromGrid(int row, int column) {
        if (syncingInvoiceGrid || row < 0 || row >= draftLines.size()) return;
        CreateInvoiceLineRequest previous = draftLines.get(row);
        Object cellProduct = linesTableModel.getValueAt(row, 0);
        ProductDTO product = null;
        if (cellProduct instanceof ProductDTO selected) {
            product = selected;
        } else if (productCombo != null) {
            product = productCombo.resolve(cellProduct);
        }
        if (product == null && cellProduct != null) {
            String label = String.valueOf(cellProduct).trim();
            if (!label.isBlank() && !"Pesquisar produto...".equalsIgnoreCase(label)) {
                product = productsList.stream().filter(p ->
                        label.equalsIgnoreCase(p.name())
                        || (p.reference() != null && label.equalsIgnoreCase(p.reference()))
                        || (p.sku() != null && label.equalsIgnoreCase(p.sku()))
                        || (p.barcode() != null && label.equalsIgnoreCase(p.barcode()))
                ).findFirst().orElse(null);
            }
        }
        if (product == null && previous.productId() != null && previous.productId() > 0) {
            final Long prevId = previous.productId();
            product = productsList.stream().filter(p -> prevId.equals(p.id())).findFirst().orElse(null);
        }

        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal quantity;
        if (column == 2) quantity = decimalGrid(linesTableModel.getValueAt(row, 2))
                .multiply(BigDecimal.valueOf(unitsPerPackage));
        else if (column == 3) quantity = decimalGrid(linesTableModel.getValueAt(row, 3)).multiply(unitsPerBox);
        else quantity = decimalGrid(linesTableModel.getValueAt(row, 1));
        if (quantity.signum() <= 0) quantity = BigDecimal.ONE;

        BigDecimal discount = decimalGrid(linesTableModel.getValueAt(row, 7));
        String serial = String.valueOf(linesTableModel.getValueAt(row, 8)).trim();
        if (serial.isEmpty() || "—".equals(serial)) serial = null;
        draftLines.set(row, new CreateInvoiceLineRequest(product == null ? 0L : product.id(), quantity,
                product == null ? BigDecimal.ZERO : product.effectiveTaxRate(), discount, null, serial));

        syncingInvoiceGrid = true;
        try {
            BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            BigDecimal subtotal = price.multiply(quantity)
                    .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
            BigDecimal tax = subtotal.multiply(product == null ? BigDecimal.ZERO : product.effectiveTaxRate());
            BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            BigDecimal packages = quantity.divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
            BigDecimal boxes = quantity.divide(unitsPerBox, 2, RoundingMode.HALF_UP);
            BigDecimal percentage = quantity.multiply(BigDecimal.valueOf(100))
                    .divide(unitsPerBox, 1, RoundingMode.HALF_UP);

            linesTableModel.setValueAt(product, row, 0);
            linesTableModel.setValueAt(quantity, row, 1);
            linesTableModel.setValueAt(packages, row, 2);
            linesTableModel.setValueAt(boxes, row, 3);
            linesTableModel.setValueAt(percentage.stripTrailingZeros().toPlainString() + "%", row, 4);
            linesTableModel.setValueAt(price, row, 5);
            linesTableModel.setValueAt(product == null ? "0%" : product.effectiveTaxRate().movePointRight(2) + "%", row, 6);
            linesTableModel.setValueAt(discount, row, 7);
            linesTableModel.setValueAt(serial == null ? "" : serial, row, 8);
            linesTableModel.setValueAt(total, row, 9);
        } finally {
            syncingInvoiceGrid = false;
        }
        recalculateInvoiceTotals();
    }

    void removeSelectedInvoiceDraftLine() {
        int row = linesTable.getSelectedRow();
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma linha", "Escolha um item para remover.");
            return;
        }
        draftLines.remove(linesTable.convertRowIndexToModel(row));
        refreshInvoiceGrid();
    }

    private void recalculateInvoiceTotals() {
        draftSubtotal = BigDecimal.ZERO;
        draftTax = BigDecimal.ZERO;
        draftTotal = BigDecimal.ZERO;
        for (CreateInvoiceLineRequest line : draftLines) {
            ProductDTO product = productsList.stream().filter(item -> item.id().equals(line.productId()))
                    .findFirst().orElse(null);
            BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            BigDecimal subtotal = price.multiply(line.quantity())
                    .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
            BigDecimal tax = subtotal.multiply(product == null ? line.taxRate() : product.effectiveTaxRate());
            BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            draftSubtotal = draftSubtotal.add(subtotal);
            draftTax = draftTax.add(tax);
            draftTotal = draftTotal.add(total);
        }
        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));
    }

    private void refreshInvoiceGrid() {
        syncingInvoiceGrid = true;
        try {
            linesTableModel.setRowCount(0);
            for (CreateInvoiceLineRequest line : draftLines) {
                ProductDTO product = productsList.stream().filter(item -> item.id().equals(line.productId()))
                        .findFirst().orElse(null);
                BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
                BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
                BigDecimal subtotal = price.multiply(line.quantity())
                        .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
                BigDecimal tax = subtotal.multiply(product == null ? line.taxRate() : product.effectiveTaxRate());
                BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
                int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
                BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                        : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
                BigDecimal packages = line.quantity().divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
                BigDecimal boxes = line.quantity().divide(unitsPerBox, 2, RoundingMode.HALF_UP);
                BigDecimal percentage = line.quantity().multiply(BigDecimal.valueOf(100))
                        .divide(unitsPerBox, 1, RoundingMode.HALF_UP);
                linesTableModel.addRow(new Object[]{product, line.quantity(), packages, boxes,
                        percentage.stripTrailingZeros().toPlainString() + "%", price,
                        product == null ? "0%" : product.effectiveTaxRate().movePointRight(2) + "%",
                        discount, line.serialNumber() == null ? "" : line.serialNumber(), total});
            }
        } finally {
            syncingInvoiceGrid = false;
        }
        recalculateInvoiceTotals();
    }

    /** Abre o formulário de nova fatura num modal responsivo (com scroll). */
    /** Validação + emissão. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private CreateInvoiceRequest buildInvoiceRequest() {
        stopInvoiceCellEditing();
        if (draftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à fatura.");
        }
        for (int row = 0; row < draftLines.size(); row++) {
            CreateInvoiceLineRequest line = draftLines.get(row);
            boolean productExists = productsList.stream().anyMatch(product -> product.id().equals(line.productId()));
            if (!productExists) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity().signum() <= 0) throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("O desconto da linha " + (row + 1) + " deve ficar entre 0 e 100.");
            }
        }
        int clientIdx = clientCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        if (clientIdx < 0 || whIdx < 0) {
            throw new RuntimeException("Selecione cliente e armazém.");
        }
        ClientDTO client = clientsList.get(clientIdx);
        WarehouseDTO warehouse = warehousesList.get(whIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Long businessUnitId = null;
        if (invoiceBusinessUnitCombo != null && invoiceBusinessUnitCombo.getSelectedItem() instanceof BusinessUnitOption opt) {
            businessUnitId = opt.id();
        }
        return new CreateInvoiceRequest(client.id(), companyId, warehouse.id(), new ArrayList<>(draftLines), null, businessUnitId);
    }

    private void resetInvoiceDraft() {
        draftLines.clear();
        draftSubtotal = BigDecimal.ZERO;
        draftTax = BigDecimal.ZERO;
        draftTotal = BigDecimal.ZERO;
        if (linesTableModel != null) linesTableModel.setRowCount(0);
        if (totalLabel != null) totalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
    }

    private boolean inStopInvoiceCellEditing = false;

    private void stopInvoiceCellEditing() {
        if (inStopInvoiceCellEditing) return;
        inStopInvoiceCellEditing = true;
        try {
            if (linesTable != null && linesTable.isEditing() && linesTable.getCellEditor() != null) {
                if (!linesTable.getCellEditor().stopCellEditing()) {
                    linesTable.getCellEditor().cancelCellEditing();
                }
            }
        } finally {
            inStopInvoiceCellEditing = false;
        }
    }

    private static BigDecimal decimalGrid(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value).trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    void cancelSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha na tabela a fatura que pretende anular.");
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);

        String reason = UIHelper.promptRequiredText("Anular Fatura", "fas-ban",
                "Fatura " + invoiceNum, "Motivo da anulação:");
        if (reason == null) return;

        UIHelper.runWithProgress(this, "A anular fatura…", () -> {
            comercialApiClient.cancelInvoice(invoiceId, reason);
            return null;
        }, ignored -> {
            ToastManager.success(this, "Fatura " + invoiceNum + " anulada com sucesso.");
            loadInvoicesTable();
            receiptsPanel.refresh();
        }, error -> showCommercialError("anular fatura", error));
    }

    void paySelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha na tabela a fatura que pretende liquidar.");
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);
        String statusStr = (String) invoicesTableModel.getValueAt(row, 5);
        BigDecimal invoiceTotal = (BigDecimal) invoicesTableModel.getValueAt(row, 7);

        // Uma fatura parcialmente paga continua a receber recibos até o saldo chegar a zero.
        if (!"APPROVED".equalsIgnoreCase(statusStr) && !"PARTIALLY_PAID".equalsIgnoreCase(statusStr)) {
            showCommercialNotice(FeedbackType.WARNING, "Fatura não liquidável",
                    "Apenas faturas por cobrar podem receber recibo. Estado actual: " + UIHelper.humanStatus(statusStr) + ".");
            return;
        }

        // O valor sugerido é o que falta receber, não o total — com pagamentos parciais são
        // coisas diferentes, e o backend recusa um recibo acima do saldo.
        UIHelper.loadAsync(this, financeApiClient::getAllAccounts,
                accounts -> receiptsPanel.openPayment(invoiceId, invoiceNum, invoiceTotal, accounts),
                error -> showCommercialLoadError("contas de tesouraria", error));
    }

    /** Recarrega a listagem de faturas na primeira página (a tabela vem paginada do servidor). */
    public void loadInvoicesTable() {
        if (invoicesPager != null) invoicesPager.reload();
    }

    /** Carrega uma página de faturas. Ligado ao {@link TablePager} da aba de Faturação. */
    void loadInvoicesPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getInvoicePage(companyId, page, size),
                response -> {
                    invoicesPager.apply(response);
                    applyInvoices(response.items());
                },
                error -> showCommercialLoadError("faturas", error));
    }

    private static final java.time.format.DateTimeFormatter INVOICE_DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private void applyInvoices(List<InvoiceDTO> invoices) {
        invoicesTableModel.setRowCount(0);
        for (InvoiceDTO invoice : invoices) {
            String dateStr = invoice.createdAt() != null ? invoice.createdAt().format(INVOICE_DATE_FMT) : "—";
            String buLabel = invoice.businessUnitName() != null && !invoice.businessUnitName().isBlank()
                    ? invoice.businessUnitName() : "Misto";
            invoicesTableModel.addRow(new Object[]{
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.clientName(),
                    dateStr,
                    buLabel,
                    invoice.status().name(),
                    invoice.totalAmount(), invoice.outstandingAmount(),
                    printApiClient == null ? "—" : "A verificar…"
            });
        }
        if (printApiClient != null && !invoices.isEmpty()) {
            List<Long> ids = invoices.stream().map(InvoiceDTO::id).toList();
            UIHelper.loadAsync(this,
                    () -> printApiClient.getPrintStatuses(SensitiveDocumentType.INVOICE, ids),
                    this::applyInvoicePrintStatuses,
                    error -> {
                        // A lista de faturas continua utilizável se apenas o indicador falhar.
                        for (int row = 0; row < invoicesTableModel.getRowCount(); row++) {
                            if (ids.contains(invoicesTableModel.getValueAt(row, 0))) {
                                invoicesTableModel.setValueAt("Indisponível", row, 8);
                            }
                        }
                    });
        }
    }

    private void applyInvoicePrintStatuses(List<DocumentPrintStatusDTO> statuses) {
        java.util.Map<Long, DocumentPrintStatusDTO> byId = statuses.stream()
                .collect(java.util.stream.Collectors.toMap(DocumentPrintStatusDTO::documentId, status -> status));
        for (int row = 0; row < invoicesTableModel.getRowCount(); row++) {
            Long id = (Long) invoicesTableModel.getValueAt(row, 0);
            DocumentPrintStatusDTO status = byId.get(id);
            if (status == null || status.printCount() == 0) {
                invoicesTableModel.setValueAt("Não impressa", row, 8);
            } else {
                String label = status.printCount() == 1 ? "1 via" : status.printCount() + " vias";
                invoicesTableModel.setValueAt(label, row, 8);
            }
        }
    }

    private JPanel createEncomendasTab() {
        return CommercialOrdersView.create(this);
    }
    /** Abre o editor de nova encomenda (painel completo, substitui o modal). */
    void openOrderEditor() {
        if (warehousesList.isEmpty()) {
            showCommercialNotice(FeedbackType.WARNING, "Armazém necessário", "Registe um armazém para a empresa actual.");
            return;
        }
        resetOrderDraft();
        editingOrder = null;
        lastCreatedOrder = null;
        orderGridEditable = true;
        if (orderBarcodeField != null) orderBarcodeField.setEnabled(true);
        orderKindCombo.setEnabled(true);
        orderEditor.setEditorTitle("Nova Encomenda");
        orderEditor.setSaveText("Guardar encomenda");
        orderEditor.setSaveEnabled(true);
        orderEditorDirty = false;
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "editor");
        }
    }

    void openSelectedOrderEditor() {
        CommercialOrderEditorActions.openSelected(this);
    }

    void backToOrdersList() {
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "list");
        }
        editingOrder = null;
        orderGridEditable = false;
        if (orderBarcodeField != null) orderBarcodeField.setEnabled(false);
        orderEditorDirty = false;
    }

    void saveOrderFromEditor() {
        CommercialOrderSubmission.save(this, comercialApiClient);
    }

    boolean isOrderEditorDirty() {
        return orderEditorDirty;
    }

    void markOrderEditorDirty() {
        orderEditorDirty = true;
    }

    void clearOrderEditorDirty() {
        orderEditorDirty = false;
    }

    void openInvoiceProductLookup() {
        if (!invoiceGridEditable) return;
        stopInvoiceCellEditing();
        List<ProductDTO> selected;
        if (comercialApiClient != null) {
            selected = ProductLookupDialog.showMultiRemote(
                    linesTable,
                    (query, categoryId, page, size) -> comercialApiClient.getProductsPage(query, categoryId, page, size)
            );
        } else {
            List<ProductDTO> catalog = productsList;
            if (catalog == null || catalog.isEmpty()) {
                showCommercialNotice(FeedbackType.WARNING, "Catálogo Vazio", "Não existem produtos registados no sistema.");
                return;
            }
            selected = ProductLookupDialog.showMulti(linesTable, catalog);
        }
        if (selected != null && !selected.isEmpty()) {
            applyInvoiceProductsFromLookup(selected);
        }
    }

    void applyInvoiceProductsFromLookup(List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty() || !invoiceGridEditable) return;
        stopInvoiceCellEditing();
        int viewRow = linesTable.getSelectedRow();
        int modelRow = viewRow >= 0 ? linesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < draftLines.size()
                && (draftLines.get(modelRow).productId() == null || draftLines.get(modelRow).productId() <= 0));

        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            if (i == 0 && fillCurrent) {
                draftLines.set(modelRow, new CreateInvoiceLineRequest(
                        p.id(), BigDecimal.ONE, p.effectiveTaxRate(), BigDecimal.ZERO, null, null));
                targetModelRow = modelRow;
            } else {
                draftLines.add(new CreateInvoiceLineRequest(
                        p.id(), BigDecimal.ONE, p.effectiveTaxRate(), BigDecimal.ZERO, null, null));
                targetModelRow = draftLines.size() - 1;
            }
        }
        refreshInvoiceGrid();
        if (targetModelRow >= 0 && targetModelRow < linesTable.getRowCount()) {
            final int finalRow = targetModelRow;
            SwingUtilities.invokeLater(() -> {
                try {
                    int vr = linesTable.convertRowIndexToView(finalRow);
                    linesTable.setRowSelectionInterval(vr, vr);
                    linesTable.scrollRectToVisible(linesTable.getCellRect(vr, 1, true));
                    linesTable.editCellAt(vr, 1);
                    Component comp = linesTable.getEditorComponent();
                    if (comp != null) comp.requestFocusInWindow();
                } catch (Exception ignored) {
                }
            });
        }
    }

    void openOrderProductLookup() {
        if (!orderGridEditable) return;
        if (orderLinesTable.isEditing()) orderLinesTable.getCellEditor().stopCellEditing();
        List<ProductDTO> selected;
        Component parent = orderFormContent != null ? orderFormContent : ordersTable;
        if (comercialApiClient != null) {
            selected = ProductLookupDialog.showMultiRemote(
                    parent,
                    (query, categoryId, page, size) -> comercialApiClient.getProductsPage(query, categoryId, page, size)
            );
        } else {
            List<ProductDTO> catalog = productsList;
            if (catalog == null || catalog.isEmpty()) {
                showCommercialNotice(FeedbackType.WARNING, "Catálogo Vazio", "Não existem produtos registados no sistema.");
                return;
            }
            selected = ProductLookupDialog.showMulti(parent, catalog);
        }
        if (selected != null && !selected.isEmpty()) {
            CommercialOrderEditorActions.applyProductsFromLookup(this, selected);
        }
    }

    void addDraftOrderLine() {
        CommercialOrderEditorActions.addBlankLine(this);
    }

    void removeSelectedDraftOrderLine() {
        CommercialOrderEditorActions.removeSelectedLine(this);
    }

    void refreshOrderDraftTable() {
        CommercialOrderEditorActions.refreshTable(this);
    }

    /** Via escolhida no editor — ver {@link CommercialOrderSubmission#selectedKind}. */
    mz.multicore.erp.modules.comercial.model.OrderKind selectedOrderKind() {
        return CommercialOrderSubmission.selectedKind(this);
    }

    /** Limpa o rascunho da encomenda (linhas, totais e selecção) antes de abrir o editor. */
    void resetOrderDraft() {
        draftOrderLines.clear();
        draftOrderUnitPrices.clear();
        draftOrderSubtotal = BigDecimal.ZERO;
        draftOrderTax = BigDecimal.ZERO;
        draftOrderTotal = BigDecimal.ZERO;
        if (orderLinesTableModel != null) orderLinesTableModel.setRowCount(0);
        if (orderTotalLabel != null) orderTotalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
        if (orderLoadLabel != null) orderLoadLabel.setText("Carga: 0.000 kg");
        if (orderClientWalkInField != null) orderClientWalkInField.setText("");
        if (orderClientCombo != null && orderClientCombo.getItemCount() > 0) orderClientCombo.setSelectedIndex(0);
    }

    void billSelectedOrder() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha na tabela a encomenda que pretende faturar.");
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        String orderNum = (String) ordersTableModel.getValueAt(row, 1);
        String statusStr = (String) ordersTableModel.getValueAt(row, 3);

        Object kindObj = ordersTableModel.getValueAt(row, ORDERS_COL_KIND);
        mz.multicore.erp.modules.comercial.model.OrderKind kind = kindObj instanceof mz.multicore.erp.modules.comercial.model.OrderKind k
                ? k : mz.multicore.erp.modules.comercial.model.OrderKind.FORMAL_ORDER;

        if ("BILLED".equalsIgnoreCase(statusStr) || "INVOICED".equalsIgnoreCase(statusStr)
                || "Facturado".equalsIgnoreCase(statusStr) || "Faturado".equalsIgnoreCase(statusStr)) {
            showCommercialNotice(FeedbackType.WARNING, "Encomenda já faturada",
                    "A encomenda " + orderNum + " já foi faturada anteriormente. Não é permitido faturar a mesma encomenda novamente.");
            return;
        }

        if (!"SEPARATED".equalsIgnoreCase(statusStr) && !(!"PENDING".equalsIgnoreCase(statusStr) ? false : !kind.usesSeparationFlow())) {
            if ("PENDING_APPROVAL".equalsIgnoreCase(statusStr)) {
                boolean choice = ModernMessageDialog.confirm(
                        SwingUtilities.getWindowAncestor(this),
                        FeedbackType.INFO,
                        "Aprovação de Encomenda Pendente",
                        "A encomenda " + orderNum + " está com aprovação pendente (retida por alçada ou risco de crédito/desconto).\n\n"
                        + "Deseja analisar e autorizar/aprovar esta encomenda agora?",
                        "Analisar"
                );
                if (choice) {
                    promptApproveOrRejectOrder(orderId, orderNum);
                }
                return;
            }
            showCommercialNotice(FeedbackType.WARNING, "Encomenda ainda não separada",
                    "Apenas pedidos separados podem ser faturados. Estado actual: " + UIHelper.humanStatus(statusStr) + ".");
            return;
        }

        java.util.concurrent.Callable<InvoiceDTO> billAction = kind.usesSeparationFlow()
                ? () -> comercialApiClient.billFulfillmentOrder(orderId, CustomerOrderFulfillmentActions.terminalName())
                : () -> comercialApiClient.billOrder(orderId);

        UIHelper.runWithProgress(this, "A faturar encomenda…", billAction, invoice -> {
            ToastManager.success(this, "Encomenda " + orderNum + " faturada · fatura " + invoice.invoiceNumber() + ".");

            loadOrdersTable();
            loadInvoicesTable();

            boolean choice = ModernMessageDialog.confirm(
                    SwingUtilities.getWindowAncestor(this),
                    FeedbackType.INFO,
                    "Emitir Guia de Transporte",
                    "Encomenda " + orderNum + " faturada com sucesso!\n"
                    + "Fatura: " + invoice.invoiceNumber() + " (stock movimentado pelo faturador).\n\n"
                    + "Deseja emitir e visualizar a Guia de Remessa / Transporte agora para acompanhar a mercadoria?",
                    "Emitir Guia"
            );
            if (choice) {
                SensitivePrintCoordinator.print(this, printApiClient,
                        SensitiveDocumentType.REMITTANCE_GUIDE, invoice.id(),
                        "Guia de remessa da fatura " + invoice.invoiceNumber(),
                        "guia-remessa-" + invoice.invoiceNumber(),
                        () -> comercialApiClient.renderGuide(invoice.id()), null);
            }
        }, error -> showCommercialError("faturar encomenda", error));
    }

    void promptApproveOrRejectOrder(Long orderId, String orderNum) {
        boolean approveChoice = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(this),
                FeedbackType.INFO,
                "Aprovação de Encomenda — " + orderNum,
                "Decisão de Alçada e Risco Corporativo para a Encomenda " + orderNum + ":\n\n"
                + "• Clique em 'Aprovar Encomenda' para libertar a encomenda para separação e faturação (Green Lane).\n"
                + "• Para rejeitar ou cancelar, clique em 'Cancelar'.",
                "Aprovar Encomenda"
        );

        if (approveChoice) {
            String notes = UIHelper.promptRequiredText("Aprovar Encomenda", "fas-check-circle",
                    "Encomenda " + orderNum, "Justificação da aprovação:");
            if (notes == null) return;

            UIHelper.runWithProgress(this, "A aprovar encomenda…",
                    () -> comercialApiClient.approveOrder(orderId, notes),
                    approved -> {
                        ToastManager.success(this, "Encomenda " + orderNum + " aprovada com sucesso! Liberada para o armazém.");
                        loadOrdersTable();
                    },
                    error -> showCommercialError("aprovar encomenda", error));
        } else {
            boolean wantReject = ModernMessageDialog.confirm(
                    SwingUtilities.getWindowAncestor(this),
                    FeedbackType.WARNING,
                    "Rejeitar Encomenda — " + orderNum,
                    "Deseja rejeitar formalmente a Encomenda " + orderNum + "?\nSerá solicitado o motivo da rejeição para arquivo e auditoria.",
                    "Rejeitar Encomenda"
            );
            if (!wantReject) return;

            String reason = UIHelper.promptRequiredText("Rejeitar Encomenda", "fas-times-circle",
                    "Encomenda " + orderNum, "Indique o motivo da rejeição:");
            if (reason == null || reason.trim().isBlank()) return;

            UIHelper.runWithProgress(this, "A rejeitar encomenda…",
                    () -> comercialApiClient.rejectOrder(orderId, reason.trim()),
                    rejected -> {
                        ToastManager.show(this, FeedbackType.WARNING, "Encomenda " + orderNum + " rejeitada com sucesso.");
                        loadOrdersTable();
                    },
                    error -> showCommercialError("rejeitar encomenda", error));
        }
    }

    void convertSelectedOrderToGuide() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha na tabela a encomenda que pretende converter em guia.");
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, ORDERS_COL_ID);
        String status = String.valueOf(ordersTableModel.getValueAt(row, ORDERS_COL_STATUS));

        // A via decide que guia sai — remessa ao cliente ou transferência entre armazéns. Mesmo
        // princípio da impressão: o documento é um facto da encomenda, não do estado em que está.
        Object kindCell = ordersTableModel.getValueAt(row, ORDERS_COL_KIND);
        if (kindCell instanceof mz.multicore.erp.modules.comercial.model.OrderKind kind
                && kind.usesWarehouseTransfer()) {
            UIHelper.loadAsync(this, () -> comercialApiClient.getOrderById(orderId),
                    order -> InternalReplenishmentActions.showConvertDialog(this, comercialApiClient, order),
                    error -> showCommercialLoadError("encomenda", error));
            return;
        }

        if (!"PENDING".equals(status)) {
            showCommercialNotice(FeedbackType.WARNING, "Encomenda indisponível para guia",
                    "A encomenda deve estar pendente e aprovada. Estado actual: " + UIHelper.humanStatus(status) + ".");
            return;
        }

        UIHelper.loadAsync(this, () -> comercialApiClient.getOrderById(orderId), this::showConvertOrderToGuideDialog,
                error -> showCommercialLoadError("encomenda", error));
    }

    private void showConvertOrderToGuideDialog(OrderDTO order) {

        JTextField orderField = new JTextField(order.orderNumber());
        JTextField clientField = new JTextField(order.clientName());
        JTextField totalField = new JTextField(String.format("%,.2f MT", order.totalAmount()));
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField field : List.of(orderField, clientField, totalField, responsibleField, vehicleField)) {
            UIHelper.styleTextField(field);
        }
        orderField.setEditable(false);
        clientField.setEditable(false);
        totalField.setEditable(false);
        responsibleField.putClientProperty("JTextField.placeholderText", "Nome de quem acompanha a expedição");
        vehicleField.putClientProperty("JTextField.placeholderText", "Matrícula ou identificação da viatura");
        UIHelper.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        UIHelper.styleScrollPane(notesScroll);

        JComboBox<String> fleetCombo = UIHelper.createComboBox(
                new String[]{"(Viatura externa / digitação manual)"}, s -> s);
        if (fleetApiClient != null) {
            UIHelper.loadAsync(this, fleetApiClient::listVehicles, vehicles -> {
                if (vehicles != null && !vehicles.isEmpty()) {
                    vehicles.forEach(v -> fleetCombo.addItem(v.licensePlate() + " • " + v.make() + " " + v.model()
                            + (v.assignedDriverName() != null && !v.assignedDriverName().isBlank() ? " (" + v.assignedDriverName() + ")" : "")));
                }
            }, null);
            fleetCombo.addActionListener(e -> {
                int idx = fleetCombo.getSelectedIndex();
                if (idx > 0) {
                    String selected = (String) fleetCombo.getSelectedItem();
                    if (selected != null) {
                        String[] parts = selected.split(" • ");
                        if (parts.length > 0) vehicleField.setText(parts[0].trim());
                        if (selected.contains("(") && selected.contains(")")) {
                            String driver = selected.substring(selected.lastIndexOf('(') + 1, selected.lastIndexOf(')'));
                            responsibleField.setText(driver);
                        }
                    }
                }
            });
        }

        JPanel form = UIHelper.createDialogForm(
                "Encomenda:", orderField,
                "Cliente:", clientField,
                "Total:", totalField,
                "Viatura da Frota:", fleetCombo,
                "Responsável pelo transporte *:", responsibleField,
                "Viatura / Matrícula *:", vehicleField,
                "Observações:", notesScroll
        );

        DeliveryGuideDTO[] created = new DeliveryGuideDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(this),
                "Converter em Guia", "fas-truck",
                "Dados de transporte da Guia de Remessa", form)
                .setConfirmButton("Criar Guia", "fas-truck")
                .setOnSaveAsync(() -> {
                    String responsible = blankToNull(responsibleField.getText());
                    String vehicle = blankToNull(vehicleField.getText());
                    String notes = blankToNull(notesArea.getText());
                    if (responsible == null) {
                        responsibleField.requestFocusInWindow();
                        throw new IllegalArgumentException("O transportador / responsável é obrigatório para emitir a guia de remessa.");
                    }
                    if (vehicle == null) {
                        vehicleField.requestFocusInWindow();
                        throw new IllegalArgumentException("A viatura / matrícula é obrigatória para emitir a guia de remessa.");
                    }
                    return () -> created[0] = comercialApiClient.createDeliveryGuide(
                            order.id(), responsible, vehicle, notes);
                });

        if (dialog.showDialog() && created[0] != null) {
            ToastManager.success(this, "Guia " + created[0].guideNumber() + " criada e submetida para aprovação.");
            loadOrdersTable();
            deliveryGuidesPanel.refresh();
        }
    }

    void generateProductionOrderFromSelectedOrder() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha na tabela a encomenda para gerar a Ordem de Fabrico.");
            return;
        }
        if (productionApiClient == null) {
            showCommercialNotice(FeedbackType.INFO, "Manufatura Indisponível",
                    "O módulo de Produção não está inicializado nesta sessão.");
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, ORDERS_COL_ID);
        UIHelper.loadAsync(this, () -> comercialApiClient.getOrderById(orderId), order -> {
            List<CommercialProductionOrderDialog.DocumentItem> items = new ArrayList<>();
            for (OrderLineDTO line : order.lines()) {
                items.add(new CommercialProductionOrderDialog.DocumentItem(
                        line.productId(), line.productName(), line.quantity()));
            }
            CommercialProductionOrderDialog.open(
                    this,
                    productionApiClient,
                    "Encomenda " + order.orderNumber(),
                    items,
                    warehousesList,
                    this::loadOrdersTable
            );
        }, error -> showCommercialLoadError("encomenda", error));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    public void loadOrdersTable() {
        if (ordersPager != null) {
            ordersPager.reload();
        } else {
            loadOrdersPage(0, 50);
        }
    }

    void loadOrdersPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getOrdersPage(companyId, page, size),
                response -> {
                    if (ordersPager != null) ordersPager.apply(response);
                    applyOrders(response != null ? response.items() : List.of());
                },
                error -> showCommercialLoadError("encomendas", error));
    }

    private void applyOrders(List<OrderDTO> orders) {
        this.cachedOrders = orders != null ? new ArrayList<>(orders) : new ArrayList<>();
        ordersTableModel.setRowCount(0);
        java.time.format.DateTimeFormatter dtfShort =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (OrderDTO order : this.cachedOrders) {
            String clientLabel = order.clientName();
            if (order.walkInName() != null && !order.walkInName().isBlank()) {
                clientLabel += " — " + order.walkInName();
            }
            // Coluna combinada: "—" se nunca; "N × dd/MM/yyyy" se impressa.
            // Operador + hora completos ficam no diálogo "Ver Detalhes".
            String impressoes;
            if (order.printCount() <= 0) {
                impressoes = "—";
            } else if (order.printedAt() != null) {
                impressoes = order.printCount() + " × " + order.printedAt().format(dtfShort);
            } else {
                impressoes = String.valueOf(order.printCount());
            }
            ordersTableModel.addRow(new Object[]{
                    order.id(),
                    order.orderNumber(),
                    clientLabel,
                    order.status(),
                    order.totalAmount(),
                    impressoes,
                    order.kind(),
                    order.quotationNumber() == null ? "—" : order.quotationNumber(),
                    formatExpectedDelivery(order),
                    order.businessUnitName() != null && !order.businessUnitName().isBlank()
                            ? order.businessUnitName() : "—"
            });
        }
    }

    public OrderDTO findCachedOrder(Long orderId) {
        if (orderId == null) return null;
        for (OrderDTO o : cachedOrders) {
            if (orderId.equals(o.id())) return o;
        }
        return null;
    }

    /** Data prometida, com o atraso que o <b>servidor</b> calculou — o painel não compara datas. */
    private static String formatExpectedDelivery(OrderDTO order) {
        if (order.expectedDeliveryDate() == null) return "—";
        String date = order.expectedDeliveryDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return order.deliveryOverdue() ? date + " (em atraso)" : date;
    }

    /** Reposição interna → transferência entre armazéns. Ver docs/REPOSICAO_INTERNA_SPEC.md. */
    void convertSelectedOrderToTransfer() {
        OrderToTransferAction.convertSelected(this, comercialApiClient);
    }

    void openBillFromOrderDialog() {
        billOrderDialog.open();
    }
    void openCancelOrderDialog() {
        cancelOrderDialog.open();
    }
    void printSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));

        String[] options = {"Folha A4 Oficial (4 Vias)", "Talão Térmico (80mm)"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Selecione o formato de impressão da Fatura " + invoiceNum + ":",
                "Imprimir Fatura",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice < 0) return;

        boolean thermal = (choice == 1);
        String taskMsg = thermal ? "A gerar fatura térmica 80mm…" : "A gerar fatura em PDF A4…";
        SensitivePrintCoordinator.print(this, printApiClient, SensitiveDocumentType.INVOICE, invoiceId,
                "Fatura " + invoiceNum,
                "fatura-" + (thermal ? "80mm-" : "") + invoiceNum,
                () -> thermal ? comercialApiClient.renderInvoiceThermal(invoiceId)
                        : comercialApiClient.renderInvoice(invoiceId),
                this::loadInvoicesTable);
    }

    void printSelectedGuide() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        SensitivePrintCoordinator.print(this, printApiClient,
                SensitiveDocumentType.REMITTANCE_GUIDE, invoiceId,
                "Guia de remessa da fatura " + invoiceNum,
                "guia-remessa-" + invoiceNum,
                () -> comercialApiClient.renderGuide(invoiceId), null);
    }

    void exportInvoicesTable() {
        TableExportAction.export(this, printApiClient, invoicesTable, "Faturas Emitidas", "faturas");
    }

    void printSelectedOrder() {
        CustomerOrderFulfillmentActions.printSelected(this, comercialApiClient);
    }

    void completeSelectedOrderSeparation() {
        CustomerOrderFulfillmentActions.completeSeparation(this, comercialApiClient);
    }

    void showSelectedOrderEvents() {
        CustomerOrderFulfillmentActions.showEvents(this, comercialApiClient);
    }

    void openSelectedOrderDetails() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha uma encomenda na tabela para continuar.");
            return;
        }
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "list");
        }
        orderDetailsDialog.open((Long) ordersTableModel.getValueAt(row, 0));
    }

    void exportOrdersTable() {
        TableExportAction.export(this, printApiClient, ordersTable, "Encomendas", "encomendas");
    }

    private void showCommercialLoadError(String area, Throwable error) {
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

    private record CommercialMetadata(List<ClientDTO> clients, List<ProductDTO> products) {}
}
