package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.config.DesktopLocalSettings;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.PosTodaySummaryView;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import mz.multicore.erp.gui.components.CustomerCreditValidator;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mz.multicore.erp.gui.pos.contingency.PosContingencyManager;
import mz.multicore.erp.gui.pos.contingency.PosContingencySyncService;

/**
 * Ponto de Venda (POS) - Painel principal coordenador.
 * Refatorado segundo a arquitetura de decomposição (SPEC-POS-PANEL-DECOMPOSITION-001).
 * Layout modular, carrinho e checkout delegados a controladores especializados.
 */
public class POSPanel extends JPanel {

    static final double CATALOG_WIDTH_RATIO = 0.36;
    static final int CATALOG_MIN_WIDTH = 380;
    static final int CART_MIN_WIDTH = 650;

    final POSApiClient posApiClient;
    final ComercialApiClient comercialApiClient;
    final InventoryApiClient inventoryApiClient;
    final FinanceApiClient financeApiClient;
    final PromotionApiClient promotionApiClient;
    final PrintApiClient printApiClient;
    final mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser;
    final PosContingencyManager contingencyManager;
    final PosContingencySyncService contingencySyncService;

    final mz.multicore.erp.modules.pos.scale.SerialScaleReader scaleReader;
    final mz.multicore.erp.gui.pos.scale.PosScaleLiveWidget scaleWidget;
    final mz.multicore.erp.gui.pos.loyalty.PosLoyaltyController loyaltyController;

    // Controladores especializados decompostos
    private final PosSalesHistoryPanel salesHistoryPanel;
    private final PosReturnDialog returnDialog;
    final PosCashSessionActions cashSessionActions;
    private final PosBarcodeActions barcodeActions;
    final PosCatalogController catalogController;
    final PosCartController cartController;
    final PosCheckoutCoordinator checkoutCoordinator;
    final PosSuspendedSaleActions suspendedSaleActions;
    ModernButton loyaltyBtn;

    // Active session status
    TillSessionDTO activeSession = null;

    // GUI elements
    JLabel statusLabel, totalLabel, subtotalValueLabel, ivaValueLabel;
    ModernPanel sessionBanner;
    JComboBox<String> clientCombo, warehouseCombo;
    JComboBox<String> accountCombo;
    JTextField clientSearchField, productSearchField, barcodeField;
    DefaultTableModel cartModel;
    JTable cartTable;
    JLabel cartItemCountLabel;
    JPanel cartCenter, productGrid, topSelectsBar;
    JScrollPane formScroll, productGridScroll;
    final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    ModernButton openSessionBtn, closeSessionBtn, cashMoveBtn, shiftHandoverBtn, checkoutBtn, addToCartBtn;
    List<ModernButton> cartLineButtons = List.of();
    private String operatorDisplayName;
    String discountAuthorization;
    JCheckBox creditCheck;
    JPanel viewCards;
    ModernButton tabVendaBtn, tabHistBtn, contingencyBtn, quotationBtn, suspendedSalesBtn;
    ActionMenuButton operationsMenu;
    final PosQuotationActions quotationActions;
    Long currentQuotationId;
    String currentQuotationNumber;
    JPanel quotationBannerPanel;
    JLabel quotationBannerLabel;
    private boolean historyView = false;
    DefaultTableModel salesHistoryModel;
    JTable salesHistoryTable;
    final PosTodaySummaryView todaySummaryView;
    List<InvoiceDTO> salesHistoryList = new ArrayList<>();

    List<ProductDTO> productsList = new ArrayList<>();
    List<ProductDTO> filteredProducts = new ArrayList<>();
    Set<Long> sellableProductIds = Set.of();
    final java.util.Map<Long, BigDecimal> productStockQuantities = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean suppressWarehouseEvents = false;
    boolean showAllProducts = true;
    List<ClientDTO> clientsList = new ArrayList<>();
    List<ClientDTO> filteredClients = new ArrayList<>();
    List<WarehouseDTO> warehousesList = new ArrayList<>();
    List<TreasuryAccountDTO> accountsList = new ArrayList<>();

    // Cart items representation
    static class CartItem extends mz.multicore.erp.gui.pos.PosCartItem {
        CartItem(ProductDTO product, BigDecimal qty, BigDecimal discount, String batch, String serial) {
            super(product, qty, discount, batch, serial);
        }

        CartItem(ProductDTO product, BigDecimal qty, BigDecimal discount, String batch, String serial, BigDecimal customUnitPrice) {
            super(product, qty, discount, batch, serial, customUnitPrice);
        }
    }

    final List<CartItem> cartItems = new ArrayList<>();

    public POSPanel(POSApiClient posApiClient, ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient, FinanceApiClient financeApiClient,
            PromotionApiClient promotionApiClient, mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser) {
        this(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient, promotionApiClient, scaleBarcodeParser, new PosContingencyManager(), null, null);
    }

    public POSPanel(POSApiClient posApiClient, ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient, FinanceApiClient financeApiClient,
            PromotionApiClient promotionApiClient, mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser,
            PrintApiClient printApiClient) {
        this(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient, promotionApiClient, scaleBarcodeParser, new PosContingencyManager(), null, printApiClient);
    }

    public POSPanel(POSApiClient posApiClient, ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient, FinanceApiClient financeApiClient,
            PromotionApiClient promotionApiClient, mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser,
            PosContingencyManager contingencyManager, PosContingencySyncService contingencySyncService) {
        this(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient, promotionApiClient, scaleBarcodeParser, contingencyManager, contingencySyncService, null);
    }

    public POSPanel(POSApiClient posApiClient, ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient, FinanceApiClient financeApiClient,
            PromotionApiClient promotionApiClient, mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser,
            PosContingencyManager contingencyManager, PosContingencySyncService contingencySyncService,
            PrintApiClient printApiClient) {
        this.posApiClient = posApiClient;
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.financeApiClient = financeApiClient;
        this.promotionApiClient = promotionApiClient;
        this.printApiClient = printApiClient;
        this.scaleBarcodeParser = scaleBarcodeParser;
        this.contingencyManager = contingencyManager != null ? contingencyManager : new PosContingencyManager();
        this.contingencySyncService = contingencySyncService;
        this.quotationActions = new PosQuotationActions(this, comercialApiClient);
        this.salesHistoryPanel = new PosSalesHistoryPanel(this);
        this.returnDialog = new PosReturnDialog(this);
        this.cashSessionActions = new PosCashSessionActions(this);
        this.barcodeActions = new PosBarcodeActions(this);
        this.catalogController = new PosCatalogController(this);
        this.cartController = new PosCartController(this);
        this.checkoutCoordinator = new PosCheckoutCoordinator(this);
        this.suspendedSaleActions = new PosSuspendedSaleActions(this);
        this.scaleReader = mz.multicore.erp.modules.pos.scale.ScaleConnection.connectedReader();
        this.scaleWidget = new mz.multicore.erp.gui.pos.scale.PosScaleLiveWidget(this.scaleReader);
        this.scaleWidget.setOnWeightCapturedCallback(this.catalogController::handleScaleWeightCaptured);
        this.loyaltyController = new mz.multicore.erp.gui.pos.loyalty.PosLoyaltyController(comercialApiClient);
        this.todaySummaryView = new PosTodaySummaryView(comercialApiClient,
                error -> showPosLoadError("resumo diário do POS", error));

        setLayout(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(PosLayout.ROOT_VERTICAL_MARGIN, 18,
                PosLayout.ROOT_VERTICAL_MARGIN, 18));

        barcodeField = new JTextField();
        UIHelper.styleTextField(barcodeField);
        barcodeField.putClientProperty("JTextField.placeholderText", "Leia ou escreva o código (F3)");
        barcodeField.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        barcodeField.addActionListener(e -> handleBarcodeScan());
        JPanel barcodeBox = PosLayout.iconInputBox("fas-barcode", 16, UIHelper.ACCENT, barcodeField);

        warehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(warehouseCombo);
        warehouseCombo.addActionListener(e -> onWarehouseChanged());
        accountCombo = new JComboBox<>(); UIHelper.styleComboBox(accountCombo);
        clientCombo = new JComboBox<>(); UIHelper.styleComboBox(clientCombo);

        clientSearchField = new JTextField();
        UIHelper.styleTextField(clientSearchField);
        clientSearchField.putClientProperty("JTextField.placeholderText", "Nome ou NUIT (F4)");
        clientSearchField.setToolTipText("Pesquise por nome ou NUIT. Sem cliente seleccionado, o nome escrito é obrigatório.");

        productSearchField = new JTextField();
        UIHelper.styleTextField(productSearchField);
        productSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar produto por SKU ou nome");

        clientSearchField.getDocument().addDocumentListener(simpleDocumentListener(() -> filterClients(clientSearchField.getText())));
        productSearchField.getDocument().addDocumentListener(simpleDocumentListener(catalogController::scheduleCatalogReload));

        add(PosWorkspaceBuilder.buildNorthStack(this), BorderLayout.NORTH);

        JPanel salesTab = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        salesTab.setOpaque(false);
        salesTab.setBorder(new EmptyBorder(4, 5, 4, 5));
        salesTab.add(PosWorkspaceBuilder.buildTopSelectsBar(this, barcodeBox), BorderLayout.NORTH);
        salesTab.add(PosWorkspaceBuilder.buildWorkspace(this), BorderLayout.CENTER);

        viewCards = new JPanel(new CardLayout());
        viewCards.setOpaque(false);
        viewCards.add(salesTab, "venda");
        viewCards.add(buildSalesHistoryTab(), "hist");
        add(viewCards, BorderLayout.CENTER);
        selectView(false);

        PosKeyboardShortcutsHandler.install(this);
        applyScaleAccess();
        cartController.refreshCartActions();
        suspendedSaleActions.refreshButton();
    }

    private void applyScaleAccess() {
        scaleWidget.applyAccess(DesktopLocalSettings.posScaleEnabled(), CurrentUserContext.getRole());
    }

    public void setOperatorDisplayName(String displayName) { operatorDisplayName = displayName; }
    BigDecimal authorizedDiscount = BigDecimal.ZERO;

    public void openLoyaltyDialog() {
        loyaltyController.openLoyaltyCardDialog(this, () -> loyaltyController.getSelectedLoyaltyClient()
                .ifPresent(client -> { clientSearchField.setText(client.name()); catalogController.filterClients(client.name()); }));
    }

    void selectView(boolean history) {
        this.historyView = history;
        if (viewCards != null) {
            ((CardLayout) viewCards.getLayout()).show(viewCards, history ? "hist" : "venda");
        }
        Color active = UIHelper.ACCENT_BLUE, activeHover = UIHelper.ACCENT_BLUE_HOVER;
        Color activeHist = UIHelper.ACCENT_BLUE, activeHistHover = UIHelper.ACCENT_BLUE_HOVER;
        Color idle = UIHelper.ROW_ALT, idleHover = UIHelper.GRID;
        tabVendaBtn.setColors(history ? idle : active, history ? idleHover : activeHover);
        tabHistBtn.setColors(history ? activeHist : idle, history ? activeHistHover : idleHover);
        if (history) refreshSalesHistory();
    }

    public void onPanelSelected() {
        applyScaleAccess();
        suspendedSaleActions.refreshButton();
        refreshOperationalData();
        SwingUtilities.invokeLater(barcodeField::requestFocusInWindow);
        if (formScroll != null) {
            SwingUtilities.invokeLater(() -> formScroll.getVerticalScrollBar().setValue(0));
        }
    }

    // createRefreshButton(this::refreshOperationalData) delegado no PosWorkspaceBuilder
    void refreshOperationalData() {
        refreshSessionState();
        loadTodaySalesSummary();
        loadMetadata();
        if (historyView) refreshSalesHistory();
    }

    void refreshSessionState() {
        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null || operator == null) return;
        UIHelper.loadAsync(this, () -> posApiClient.getActiveSession(operator, companyId),
                this::applySessionState, error -> showPosLoadError("estado do caixa", error));
    }

    void applySessionState(Optional<TillSessionDTO> sessionOpt) {
        if (sessionOpt.isPresent()) {
            activeSession = sessionOpt.get();
            String effectiveOp = activeSession.currentOperator() != null && !activeSession.currentOperator().isBlank()
                    ? activeSession.currentOperator() : activeSession.operator();
            statusLabel.setText(String.format("Caixa Aberta por %s | Fundo Inicial: %,.2f MT",
                    PosLayout.operatorLabel(effectiveOp, CurrentUserContext.getUsername(), operatorDisplayName),
                    activeSession.openingBalance()));
            statusLabel.setForeground(UIHelper.APPROVED_GREEN);
            statusLabel.setIcon(UIHelper.icon("fas-lock-open", 14, UIHelper.APPROVED_GREEN));
            statusLabel.setIconTextGap(8);
            if (sessionBanner != null) { sessionBanner.setBackground(UIHelper.SELECTION_BG); sessionBanner.repaint(); }
            openSessionBtn.setVisible(false);
            closeSessionBtn.setVisible(true);
            cashMoveBtn.setVisible(true);
            shiftHandoverBtn.setVisible(true);
            if (addToCartBtn != null) { addToCartBtn.setEnabled(true); addToCartBtn.setToolTipText(null); }
        } else {
            activeSession = null;
            statusLabel.setText("Caixa Fechada. É necessário abrir sessão antes de vender.");
            statusLabel.setForeground(UIHelper.PENDING_YELLOW);
            statusLabel.setIcon(UIHelper.icon("fas-lock", 14, UIHelper.PENDING_YELLOW));
            statusLabel.setIconTextGap(8);
            if (sessionBanner != null) { sessionBanner.setBackground(UIHelper.ROW_ALT); sessionBanner.repaint(); }
            openSessionBtn.setVisible(true);
            closeSessionBtn.setVisible(false);
            cashMoveBtn.setVisible(false);
            shiftHandoverBtn.setVisible(false);
            if (addToCartBtn != null) { addToCartBtn.setEnabled(false); addToCartBtn.setToolTipText("Abra a caixa antes de adicionar artigos."); }
        }
        cartController.refreshCartActions();
    }

    void loadMetadata() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> new PosMetadata(comercialApiClient.getAllClients(),
                        inventoryApiClient.getSalesWarehousesByCompany(companyId), financeApiClient.getAllAccounts()),
                this::applyMetadata, error -> showPosLoadError("dados do ponto de venda", error));
    }

    private void loadTodaySalesSummary() {
        todaySummaryView.loadTodaySalesSummary(this);
    }

    private void applyMetadata(PosMetadata metadata) {
        clientsList = metadata.clients();
        warehousesList = metadata.warehouses();
        accountsList = metadata.accounts();
        suppressWarehouseEvents = true;
        try {
            warehouseCombo.removeAllItems();
            accountCombo.removeAllItems();
            for (WarehouseDTO w : warehousesList) warehouseCombo.addItem(w.name());
            for (TreasuryAccountDTO acc : accountsList) accountCombo.addItem(acc.name());
        } finally {
            suppressWarehouseEvents = false;
        }
        filterClients(clientSearchField == null ? "" : clientSearchField.getText());
        catalogController.loadCatalogPage(0);
    }

    private void filterClients(String query) { catalogController.filterClients(query); }
    private void rebuildProductGrid() { catalogController.rebuildProductGrid(); }
    void addProductToCart(ProductDTO product) { catalogController.addProductToCart(product); }
    void rebuildCartRows() { catalogController.rebuildCartRows(); }

    private javax.swing.event.DocumentListener simpleDocumentListener(Runnable onChange) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        };
    }

    void openSession() { cashSessionActions.openSession(); }
    public void closeSession() { cashSessionActions.closeSession(); }
    public void manageCashMovements() { cashSessionActions.manageCashMovements(); }

    public void removeFromCart() {
        cartController.removeFromCart();
    }

    public void changeSelectedQuantity(BigDecimal delta) { catalogController.changeSelectedQuantity(delta); }

    public void editSelectedCartQuantity() {
        cartController.editSelectedCartQuantity();
    }

    void updateCartTotal() {
        cartController.updateCartTotal();
    }

    void updateCartTotal(int preferredModelRow) {
        cartController.updateCartTotal(preferredModelRow);
    }

    static String ivaCellLabel(CartItem item) {
        return PosCartController.ivaCellLabel(item);
    }

    static BigDecimal effectiveTaxRate(BigDecimal productRate) {
        return PosCartController.effectiveTaxRate(productRate);
    }

    private void refreshCartView() {
        cartController.refreshCartView();
    }

    /**
     * Validação mandatória e finalização de venda no POS.
     * Requisito de cliente: promptRequiredText ("É obrigatório indicar o nome do cliente").
     */
    public void runCheckout() {
        // Validação pre-flight de crédito no checkout: CustomerCreditValidator.validateCreditPreFlight
        checkoutCoordinator.runCheckout();
    }

    CustomerCreditValidator.CreditAssessment validateCreditPreFlight(ClientDTO client, java.math.BigDecimal amount) {
        return CustomerCreditValidator.validateCreditPreFlight(client, amount);
    }

    void updateQuotationBanner() {
        if (quotationBannerPanel == null || quotationBannerLabel == null) return;
        boolean hasQuot = currentQuotationId != null && currentQuotationNumber != null;
        if (hasQuot) quotationBannerLabel.setText("Cotação vinculada: " + currentQuotationNumber);
        quotationBannerPanel.setVisible(hasQuot);
    }

    private void handleBarcodeScan() { barcodeActions.handleBarcodeScan(); }
    private JPanel buildSalesHistoryTab() { return salesHistoryPanel.buildPanel(); }
    void refreshSalesHistory() { salesHistoryPanel.refresh(); }
    void showPosLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area, error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }
    void showPosNotice(FeedbackType type, String title, String message) { feedback.show(type, title, message, null, null); }
    void showPosSuccess(String message) { ToastManager.success(this, message); }
    boolean isProductSellable(ProductDTO product) { return product != null && sellableProductIds.contains(product.id()); }
    void registerSellableProduct(ProductDTO product) { java.util.Set<Long> u = new java.util.HashSet<>(sellableProductIds); u.add(product.id()); sellableProductIds = u; }
    public void showReturnDialog() { returnDialog.show(); }
    public boolean isHistoryView() { return historyView; }
    public void clearCart() {
        if (quotationActions != null) quotationActions.clearQuotationLink();
        cartItems.clear();
        updateCartTotal(-1);
        cartController.resetCustomerForNextSale();
        if (barcodeField != null) barcodeField.requestFocusInWindow();
    }
    public BigDecimal getProductStock(Long productId) { return productId != null ? productStockQuantities.get(productId) : null; }
    public Long getSelectedWarehouseId() {
        if (warehouseCombo == null) return null;
        int idx = warehouseCombo.getSelectedIndex();
        return (idx >= 0 && idx < warehousesList.size()) ? warehousesList.get(idx).id() : null;
    }
    public String getSelectedWarehouseName() {
        if (warehouseCombo == null) return null;
        int idx = warehouseCombo.getSelectedIndex();
        return (idx >= 0 && idx < warehousesList.size()) ? warehousesList.get(idx).name() : null;
    }
    void onWarehouseChanged() {
        if (suppressWarehouseEvents) return;
        if (catalogController != null) catalogController.loadCatalogPage(0);
    }

    private record PosMetadata(java.util.List<ClientDTO> clients, java.util.List<WarehouseDTO> warehouses, java.util.List<TreasuryAccountDTO> accounts) {}
}
