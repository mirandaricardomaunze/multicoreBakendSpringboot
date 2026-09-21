package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.PosTodaySummaryView;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.pos.contingency.PosContingencyDialog;
import mz.multicore.erp.gui.pos.contingency.PosContingencyManager;
import mz.multicore.erp.gui.pos.contingency.PosContingencySyncService;

public class POSPanel extends JPanel {

    /** Proporção inicial responsiva: catálogo compacto e maior área útil para o carrinho. */
    private static final double CATALOG_WIDTH_RATIO = 0.36;
    private static final int CATALOG_MIN_WIDTH = 380;
    private static final int CART_MIN_WIDTH = 650;

    final POSApiClient posApiClient;
    final ComercialApiClient comercialApiClient;
    final InventoryApiClient inventoryApiClient;
    final FinanceApiClient financeApiClient;
    final PromotionApiClient promotionApiClient;
    final mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser;
    final PosContingencyManager contingencyManager;
    final PosContingencySyncService contingencySyncService;

    final mz.multicore.erp.modules.pos.scale.SerialScaleReader scaleReader;
    final mz.multicore.erp.gui.pos.scale.PosScaleLiveWidget scaleWidget;
    final mz.multicore.erp.gui.pos.loyalty.PosLoyaltyController loyaltyController;

    // Componentes de apoio decompostos para manter o painel < 1000 linhas
    private final PosSalesHistoryPanel salesHistoryPanel;
    private final PosReturnDialog returnDialog;
    private final PosCashSessionActions cashSessionActions;
    private final PosBarcodeActions barcodeActions;
    private final PosCatalogController catalogController;
    private ModernButton loyaltyBtn;

    // Active session status
    TillSessionDTO activeSession = null;

    // GUI elements
    JLabel statusLabel;
    ModernPanel sessionBanner;
    JComboBox<String> clientCombo;
    private JComboBox<String> warehouseCombo;
    private JComboBox<String> accountCombo;
    JTextField clientSearchField;
    JTextField productSearchField;
    JTextField barcodeField;

    DefaultTableModel cartModel;
    JTable cartTable;
    private JLabel cartItemCountLabel;
    JPanel cartCenter;
    private JScrollPane formScroll;
    JPanel productGrid;
    private JScrollPane productGridScroll;
    JPanel topSelectsBar;
    JLabel totalLabel;
    JLabel subtotalValueLabel;
    JLabel ivaValueLabel;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private ModernButton openSessionBtn;
    private ModernButton closeSessionBtn;
    private ModernButton cashMoveBtn;
    private ModernButton checkoutBtn;
    private ModernButton addToCartBtn;
    JCheckBox creditCheck;
    JPanel viewCards;
    private ModernButton tabVendaBtn;
    private ModernButton tabHistBtn;
    private ModernButton contingencyBtn;
    private boolean historyView = false;
    DefaultTableModel salesHistoryModel;
    JTable salesHistoryTable;
    private JLabel salesHistorySummary;
    private final PosTodaySummaryView todaySummaryView;
    List<InvoiceDTO> salesHistoryList = new ArrayList<>();

    List<ProductDTO> productsList = new ArrayList<>();
    List<ProductDTO> filteredProducts = new ArrayList<>();
    Set<Long> sellableProductIds = Set.of();
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
    }

    final List<CartItem> cartItems = new ArrayList<>();

    public POSPanel(
            POSApiClient posApiClient,
            ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient,
            PromotionApiClient promotionApiClient,
            mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser
    ) {
        this(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient, promotionApiClient,
                scaleBarcodeParser, new PosContingencyManager(), null);
    }

    public POSPanel(
            POSApiClient posApiClient,
            ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient,
            PromotionApiClient promotionApiClient,
            mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser,
            PosContingencyManager contingencyManager,
            PosContingencySyncService contingencySyncService
    ) {
        this.posApiClient = posApiClient;
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.financeApiClient = financeApiClient;
        this.promotionApiClient = promotionApiClient;
        this.scaleBarcodeParser = scaleBarcodeParser;
        this.contingencyManager = contingencyManager != null ? contingencyManager : new PosContingencyManager();
        this.contingencySyncService = contingencySyncService;
        this.salesHistoryPanel = new PosSalesHistoryPanel(this);
        this.returnDialog = new PosReturnDialog(this);
        this.cashSessionActions = new PosCashSessionActions(this);
        this.barcodeActions = new PosBarcodeActions(this);
        this.catalogController = new PosCatalogController(this);
        this.scaleReader = new mz.multicore.erp.modules.pos.scale.SerialScaleReader(true);
        this.scaleWidget = new mz.multicore.erp.gui.pos.scale.PosScaleLiveWidget(this.scaleReader);
        this.loyaltyController = new mz.multicore.erp.gui.pos.loyalty.PosLoyaltyController(comercialApiClient);
        this.todaySummaryView = new PosTodaySummaryView(comercialApiClient,
                error -> showPosLoadError("resumo diário do POS", error));

        setLayout(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(PosLayout.ROOT_VERTICAL_MARGIN, 18,
                PosLayout.ROOT_VERTICAL_MARGIN, 18));

        // 1. TOP BAR — selector de vista (Venda POS | Histórico | Contingência)
        tabVendaBtn = new ModernButton("Venda POS");
        tabVendaBtn.setIcon(UIHelper.icon("fas-cash-register", 14));
        tabVendaBtn.setPreferredSize(new Dimension(125, UIHelper.FORM_CONTROL_HEIGHT));
        tabVendaBtn.addActionListener(e -> selectView(false));
        tabHistBtn = new ModernButton("Histórico de Vendas");
        tabHistBtn.setIcon(UIHelper.icon("fas-history", 14));
        tabHistBtn.setPreferredSize(new Dimension(165, UIHelper.FORM_CONTROL_HEIGHT));
        tabHistBtn.addActionListener(e -> selectView(true));

        loyaltyBtn = UIHelper.createButton("Fidelidade (F7)", UIHelper.icon("fas-star", 14, Color.WHITE), UIHelper.BUTTON_NEUTRAL, e -> openLoyaltyDialog());
        loyaltyBtn.setForeground(Color.WHITE);
        loyaltyBtn.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));

        contingencyBtn = UIHelper.createWarningButton("Contingência");
        contingencyBtn.setIcon(UIHelper.icon("fas-exclamation-triangle", 14));
        contingencyBtn.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        contingencyBtn.setVisible(false);
        contingencyBtn.addActionListener(e -> new PosContingencyDialog(
                SwingUtilities.getWindowAncestor(this), this.contingencyManager, this.contingencySyncService).setVisible(true));

        this.contingencyManager.addListener(count -> SwingUtilities.invokeLater(() -> {
            contingencyBtn.setVisible(count > 0);
            contingencyBtn.setText("Contingência (" + count + ")");
        }));
        if (this.contingencyManager.getPendingCount() > 0) {
            contingencyBtn.setVisible(true);
            contingencyBtn.setText("Contingência (" + this.contingencyManager.getPendingCount() + ")");
        }

        JPanel segmented = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        segmented.setOpaque(false);
        segmented.add(tabVendaBtn);
        segmented.add(tabHistBtn);
        segmented.add(loyaltyBtn);
        segmented.add(contingencyBtn);
        segmented.add(scaleWidget);

        openSessionBtn = UIHelper.createSuccessButton("Abrir Caixa");
        openSessionBtn.setIcon(UIHelper.icon("fas-lock-open", 14));
        openSessionBtn.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT)); openSessionBtn.addActionListener(e -> cashSessionActions.openSession());
        closeSessionBtn = UIHelper.createDangerButton("Fechar Caixa (Z)");
        closeSessionBtn.setIcon(UIHelper.icon("fas-lock", 14));
        closeSessionBtn.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        closeSessionBtn.addActionListener(e -> cashSessionActions.closeSession()); closeSessionBtn.setVisible(false);
        cashMoveBtn = UIHelper.createWarningButton("Sangria / Suprimento");
        cashMoveBtn.setIcon(UIHelper.icon("fas-exchange-alt", 14));
        cashMoveBtn.setPreferredSize(new Dimension(165, UIHelper.FORM_CONTROL_HEIGHT));
        cashMoveBtn.addActionListener(e -> cashSessionActions.manageCashMovements()); cashMoveBtn.setVisible(false);
        ModernButton historyZBtn = UIHelper.createPrimaryButton("Fechos (Z)");
        historyZBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        historyZBtn.setToolTipText("Histórico de Fechos de Caixa (Z)");
        historyZBtn.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));
        historyZBtn.addActionListener(e -> cashSessionActions.showSessionHistory());

        JPanel sessionActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sessionActions.setOpaque(false);
        sessionActions.add(UIHelper.createRefreshButton(this::refreshOperationalData));
        sessionActions.add(historyZBtn);
        sessionActions.add(openSessionBtn);
        sessionActions.add(cashMoveBtn);
        sessionActions.add(closeSessionBtn);

        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);
        topBar.add(segmented, BorderLayout.WEST);
        topBar.add(sessionActions, BorderLayout.EAST);

        statusLabel = new JLabel("Caixa Fechada. Abra uma sessão para vender.");
        statusLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        statusLabel.setForeground(UIHelper.PENDING_YELLOW);
        sessionBanner = PosLayout.createSessionBanner(statusLabel);
        sessionBanner.setBackground(UIHelper.ROW_ALT);

        JPanel sessionBar = new JPanel(new BorderLayout(0, 4));
        sessionBar.setOpaque(false);
        sessionBar.add(topBar, BorderLayout.NORTH);
        sessionBar.add(sessionBanner, BorderLayout.SOUTH);

        // 1b. CÓDIGO DE BARRAS — Enter procura por código e adiciona ao carrinho. Campo de altura
        //     única com o ícone DENTRO do input, para subir e alinhar com os combos do cabeçalho
        //     (ver POS_CABECALHO_COMPACTO_SPEC). A barra própria foi removida para ganhar altura no
        //     catálogo de produtos.
        barcodeField = new JTextField();
        UIHelper.styleTextField(barcodeField);
        barcodeField.putClientProperty("JTextField.placeholderText", "Código de barras… (F3/Enter)");
        barcodeField.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        barcodeField.addActionListener(e -> handleBarcodeScan());
        JPanel barcodeBox = PosLayout.iconInputBox("fas-barcode", 16, UIHelper.ACCENT, barcodeField);

        JPanel northStack = new JPanel();
        northStack.setOpaque(false);
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        sessionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        northStack.add(sessionBar);
        northStack.add(Box.createVerticalStrut(6));
        northStack.add(feedback);
        add(northStack, BorderLayout.NORTH);


        // 2. MAIN POS WORKSPACE: FORM (esquerda) & CART (direita) num JSplitPane redimensionável.
        //    O resize favorece o carrinho (resizeWeight alto) e o formulário arranca com largura
        //    confortável mas pode ser arrastado pelo operador.
        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workspace.setOpaque(false);
        workspace.setBorder(null);
        workspace.setDividerSize(10);
        workspace.setContinuousLayout(true);
        workspace.setResizeWeight(CATALOG_WIDTH_RATIO);
        workspace.setBackground(UIHelper.BG_DARK);

        // LEFT: CATÁLOGO DE PRODUTOS EM CARDS — clicar adiciona ao carrinho
        warehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(warehouseCombo);
        accountCombo = new JComboBox<>(); UIHelper.styleComboBox(accountCombo);
        clientCombo = new JComboBox<>(); UIHelper.styleComboBox(clientCombo);

        clientSearchField = new JTextField(); UIHelper.styleTextField(clientSearchField);
        productSearchField = new JTextField(); UIHelper.styleTextField(productSearchField);
        clientSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar cliente por nome ou NUIT…");
        productSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar produto por SKU ou nome…");
        clientSearchField.getDocument().addDocumentListener(simpleDocumentListener(() -> filterClients(clientSearchField.getText())));
        productSearchField.getDocument().addDocumentListener(simpleDocumentListener(catalogController::scheduleCatalogReload));

        // Cabeçalho operacional numa única linha: nenhum campo fica escondido ou rouba altura ao carrinho.
        // O cadastro de clientes vive no painel Clientes — no balcão a venda sem cliente vai para "Consumidor Final".
        topSelectsBar = new JPanel(new GridBagLayout());
        topSelectsBar.setOpaque(false);
        topSelectsBar.setBorder(new EmptyBorder(4, 0, 4, 0));
        GridBagConstraints tg = new GridBagConstraints();
        tg.fill = GridBagConstraints.HORIZONTAL; tg.anchor = GridBagConstraints.NORTH; tg.gridy = 0;
        tg.gridx = 0; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[0]; tg.insets = new Insets(0, 0, 0, 6);
        topSelectsBar.add(labeledField("Pesquisar cliente", PosLayout.searchRow(clientSearchField)), tg);
        tg.gridx = 1; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[1];
        topSelectsBar.add(labeledField("Cliente", clientCombo), tg);
        tg.gridx = 2; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[2];
        topSelectsBar.add(labeledField("Armazém", warehouseCombo), tg);
        tg.gridx = 3; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[3];
        topSelectsBar.add(labeledField("Conta", accountCombo), tg);
        tg.gridx = 4; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[4]; tg.insets = new Insets(0, 0, 0, 0);
        topSelectsBar.add(labeledField("Código de barras", barcodeBox), tg);

        // Catálogo (esquerda do workspace): pesquisa + grid de cards clicáveis
        JPanel leftPanel = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        leftPanel.setOpaque(false);
        leftPanel.setMinimumSize(new Dimension(CATALOG_MIN_WIDTH, 10));
        JPanel catalogHeader = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        catalogHeader.setOpaque(false);
        JLabel prodTitle = new JLabel("Catálogo de Produtos", UIHelper.icon("fas-boxes", 15, UIHelper.ACCENT_BLUE), SwingConstants.LEFT);
        prodTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        prodTitle.setForeground(UIHelper.TEXT_LIGHT);
        catalogHeader.add(prodTitle, BorderLayout.NORTH);
        JComboBox<String> availabilityFilter = new JComboBox<>(new String[]{"Todos", "Disponíveis"});
        UIHelper.styleComboBox(availabilityFilter);
        availabilityFilter.setToolTipText("Mostrar todos os produtos ou apenas os disponíveis para venda");
        availabilityFilter.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        availabilityFilter.addActionListener(e -> {
            showAllProducts = availabilityFilter.getSelectedIndex() == 0;
            catalogController.loadCatalogPage(0);
        });
        JPanel catalogFilters = new JPanel(new BorderLayout(8, 0));
        catalogFilters.setOpaque(false);
        catalogFilters.add(PosLayout.searchRow(productSearchField), BorderLayout.CENTER);
        catalogFilters.add(availabilityFilter, BorderLayout.EAST);
        catalogHeader.add(catalogFilters, BorderLayout.SOUTH);
        leftPanel.add(catalogHeader, BorderLayout.NORTH);

        productGrid = new JPanel(new GridLayout(0, 2, 8, 8));
        productGrid.setOpaque(false);
        productGridScroll = new JScrollPane(productGrid);
        productGridScroll.setBorder(BorderFactory.createEmptyBorder());
        productGridScroll.getViewport().setOpaque(false);
        productGridScroll.setOpaque(false);
        productGridScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        productGridScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        productGridScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIHelper.styleScrollPane(productGridScroll);
        formScroll = productGridScroll; // reaproveita o reset de scroll em onPanelSelected

        ModernPanel catalogCard = new ModernPanel(16);
        catalogCard.setLayout(new BorderLayout());
        catalogCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        catalogCard.add(productGridScroll, BorderLayout.CENTER);
        catalogCard.add(catalogController.buildPaginationBar(), BorderLayout.SOUTH);
        leftPanel.add(catalogCard, BorderLayout.CENTER);
        workspace.setLeftComponent(leftPanel);

        // RIGHT: CART TABLE & CHECKOUT
        JPanel rightPanel = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        rightPanel.setOpaque(false);
        JLabel cartTitle = new JLabel("Carrinho de Vendas (POS)", UIHelper.icon("fas-shopping-cart", 15, UIHelper.ACCENT_BLUE), SwingConstants.LEFT);
        cartTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        cartTitle.setForeground(UIHelper.TEXT_LIGHT);
        rightPanel.add(cartTitle, BorderLayout.NORTH);

        ModernPanel cartCard = new ModernPanel(16);
        cartCard.setLayout(new BorderLayout(0, PosLayout.CARD_VERTICAL_GAP));
        cartCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cartCols = {"Artigo", "Qtd", "Preço", "Desc.", "IVA", "Total"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel) {
            @Override public String getToolTipText(MouseEvent event) {
                int viewRow = rowAtPoint(event.getPoint());
                if (viewRow < 0) return null;
                int modelRow = convertRowIndexToModel(viewRow);
                if (modelRow < 0 || modelRow >= cartItems.size()) return null;
                CartItem item = cartItems.get(modelRow);
                String detail = item.note == null || item.note.isBlank() ? "Sem promoção" : item.note;
                if (item.batch != null && !item.batch.isBlank()) detail += " | Lote: " + item.batch;
                if (item.serial != null && !item.serial.isBlank()) detail += " | Série: " + item.serial;
                return detail;
            }
        };
        UIHelper.styleTable(cartTable);
        cartTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        cartTable.putClientProperty("noRowInspector", Boolean.TRUE);
        cartTable.setRowHeight(42);
        cartTable.setFillsViewportHeight(true);
        cartTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        PosLayout.configureOperationalCartColumns(cartTable);
        // Altura confortável: viewport para ~12 linhas; o scroll trata do excesso de produtos.
        cartTable.setPreferredScrollableViewportSize(new Dimension(620, cartTable.getRowHeight() * 12));
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setMinimumSize(new Dimension(0, 126));
        cartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        cartScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIHelper.styleScrollPane(cartScroll);
        cartTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelectedCartQuantity();
                }
            }
        });

        JPanel emptyState = buildEmptyStatePanel();

        cartCenter = new JPanel(new CardLayout());
        cartCenter.setOpaque(false);
        cartCenter.setMinimumSize(new Dimension(0, 126));
        cartCenter.add(emptyState, "empty");
        cartCenter.add(cartScroll, "table");
        JPanel cartToolbar = new JPanel(new BorderLayout(8, 0));
        cartToolbar.setOpaque(false);
        cartItemCountLabel = new JLabel("0 artigos");
        cartItemCountLabel.setForeground(UIHelper.TEXT_MUTED);
        cartItemCountLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        cartToolbar.add(cartItemCountLabel, BorderLayout.WEST);

        ModernButton decreaseBtn = UIHelper.createPrimaryButton("−");
        decreaseBtn.setIcon(null); decreaseBtn.setForeground(Color.WHITE);
        decreaseBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        decreaseBtn.setPreferredSize(new Dimension(36, 32)); decreaseBtn.setToolTipText("Diminuir quantidade");
        ModernButton editQtyBtn = UIHelper.createPrimaryButton("Quantidade (F6)");
        editQtyBtn.setIcon(UIHelper.icon("fas-sort-numeric-up", 12, Color.WHITE)); editQtyBtn.setForeground(Color.WHITE);
        ModernButton increaseBtn = UIHelper.createPrimaryButton("+");
        increaseBtn.setIcon(null); increaseBtn.setForeground(Color.WHITE);
        increaseBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        increaseBtn.setPreferredSize(new Dimension(36, 32)); increaseBtn.setToolTipText("Aumentar quantidade");
        JPanel quantityActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quantityActions.setOpaque(false);
        quantityActions.add(decreaseBtn);
        quantityActions.add(editQtyBtn);
        quantityActions.add(increaseBtn);
        cartToolbar.add(quantityActions, BorderLayout.EAST);
        cartCard.add(cartToolbar, BorderLayout.NORTH);
        cartCard.add(cartCenter, BorderLayout.CENTER);

        // Resumo e acções ficam compactos para preservar a altura operacional da tabela.
        JPanel cartBottom = new JPanel();
        cartBottom.setLayout(new BoxLayout(cartBottom, BoxLayout.Y_AXIS));
        cartBottom.setOpaque(false);

        subtotalValueLabel = new JLabel("0,00 MT");
        ivaValueLabel = new JLabel("0,00 MT");
        totalLabel = new JLabel("0,00 MT");
        ModernPanel totalRow = PosLayout.createTotalsRow(subtotalValueLabel, ivaValueLabel, totalLabel);

        creditCheck = new JCheckBox("Venda a Crédito (Conta Corrente)");
        creditCheck.setToolTipText("Registar a venda a crédito na conta corrente do cliente");
        creditCheck.setForeground(UIHelper.TEXT_LIGHT);
        creditCheck.setOpaque(false);
        creditCheck.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        ModernButton removeBtn = UIHelper.createDangerButton("Remover Selecionado");
        removeBtn.setIcon(UIHelper.icon("fas-trash", 14, Color.WHITE)); removeBtn.setForeground(Color.WHITE);
        buttonRow.add(removeBtn, BorderLayout.WEST);
        JPanel creditCell = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        creditCell.setOpaque(false); creditCell.add(creditCheck);
        buttonRow.add(creditCell, BorderLayout.CENTER);

        checkoutBtn = UIHelper.createSuccessButton("Finalizar Venda (F9)");
        checkoutBtn.setIcon(UIHelper.icon("fas-check-circle", 14, Color.WHITE)); checkoutBtn.setForeground(Color.WHITE);
        buttonRow.add(checkoutBtn, BorderLayout.EAST);

        cartBottom.add(totalRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 8)));
        cartBottom.add(buttonRow);
        cartCard.add(cartBottom, BorderLayout.SOUTH);

        // Totais e checkout ficam fora do viewport da tabela para permanecerem sempre acessíveis.
        rightPanel.add(cartCard, BorderLayout.CENTER);
        rightPanel.setMinimumSize(new Dimension(CART_MIN_WIDTH, 10));
        workspace.setRightComponent(rightPanel);
        SwingUtilities.invokeLater(() -> workspace.setDividerLocation(CATALOG_WIDTH_RATIO));

        JPanel salesTab = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        salesTab.setOpaque(false);
        salesTab.setBorder(new EmptyBorder(4, 5, 4, 5));
        salesTab.add(topSelectsBar, BorderLayout.NORTH);
        salesTab.add(workspace, BorderLayout.CENTER);

        // Vistas comutadas pelos botões do topo (em vez de um JTabbedPane), para o selector ficar
        // na mesma linha que as acções de caixa.
        viewCards = new JPanel(new CardLayout());
        viewCards.setOpaque(false);
        viewCards.add(salesTab, "venda");
        viewCards.add(buildSalesHistoryTab(), "hist");
        add(viewCards, BorderLayout.CENTER);
        selectView(false);

        // LISTENERS
        openSessionBtn.addActionListener(e -> openSession());
        closeSessionBtn.addActionListener(e -> closeSession());
        cashMoveBtn.addActionListener(e -> manageCashMovements());
        removeBtn.addActionListener(e -> removeFromCart());
        decreaseBtn.addActionListener(e -> changeSelectedQuantity(BigDecimal.ONE.negate()));
        editQtyBtn.addActionListener(e -> editSelectedCartQuantity());
        increaseBtn.addActionListener(e -> changeSelectedQuantity(BigDecimal.ONE));
        checkoutBtn.addActionListener(e -> runCheckout());

        installKeyboardShortcuts();
    }

    /** Atalhos de caixa previsíveis; Delete fica limitado à tabela para não apagar texto digitado. */
    private void installKeyboardShortcuts() {
        InputMap input = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actions = getActionMap();
        bindShortcut(input, actions, "posProductSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
                () -> focusAndSelect(productSearchField));
        bindShortcut(input, actions, "posBarcodeSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
                () -> focusAndSelect(barcodeField));
        bindShortcut(input, actions, "posClientSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                () -> focusAndSelect(clientSearchField));
        bindShortcut(input, actions, "posEditQuantity", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
                this::editSelectedCartQuantity);
        bindShortcut(input, actions, "posLoyaltySearch", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0),
                this::openLoyaltyDialog);
        bindShortcut(input, actions, "posCheckout", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
                this::runCheckout);

        InputMap tableInput = cartTable.getInputMap(JComponent.WHEN_FOCUSED);
        bindShortcut(tableInput, cartTable.getActionMap(), "posRemoveLine",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::removeFromCart);
    }

    void openLoyaltyDialog() {
        loyaltyController.openLoyaltyCardDialog(this, () -> loyaltyController.getSelectedLoyaltyClient()
                .ifPresent(client -> { clientSearchField.setText(client.name()); catalogController.filterClients(client.name()); }));
    }

    static void bindShortcut(InputMap input, ActionMap actions, String name, KeyStroke key, Runnable command) {
        input.put(key, name);
        actions.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { command.run(); }
        });
    }

    private static void focusAndSelect(JTextField field) { field.requestFocusInWindow(); field.selectAll(); }

    /** Comuta entre a vista de venda e o histórico, alternando o estilo dos botões do topo. */
    void selectView(boolean history) {
        this.historyView = history;
        if (viewCards != null) {
            ((CardLayout) viewCards.getLayout()).show(viewCards, history ? "hist" : "venda");
        }
        Color active = UIHelper.ACCENT_BLUE;
        Color activeHover = UIHelper.ACCENT_BLUE.brighter();
        Color idle = UIHelper.BUTTON_NEUTRAL;
        Color idleHover = UIHelper.BUTTON_NEUTRAL_HOVER;
        tabVendaBtn.setColors(history ? idle : active, history ? idleHover : activeHover);
        tabHistBtn.setColors(history ? active : idle, history ? activeHover : idleHover);
        tabVendaBtn.setForeground(Color.WHITE);
        tabHistBtn.setForeground(Color.WHITE);
        if (history) {
            refreshSalesHistory();
        }
    }

    public void onPanelSelected() {
        refreshOperationalData();
        // Repõe o formulário no topo para a secção DOCUMENTO (Cliente/Armazém) ficar sempre visível.
        if (formScroll != null) {
            SwingUtilities.invokeLater(() -> formScroll.getVerticalScrollBar().setValue(0));
        }
    }

    /** Recarrega caixa, catálogo, clientes, contas e histórico visível a partir do backend central. */
    void refreshOperationalData() {
        refreshSessionState();
        loadTodaySalesSummary();
        loadMetadata();
        if (historyView) refreshSalesHistory();
    }

    void refreshSessionState() {
        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null || operator == null) {
            return;
        }

        UIHelper.loadAsync(this, () -> posApiClient.getActiveSession(operator, companyId),
                this::applySessionState, error -> showPosLoadError("estado do caixa", error));
    }

    private void applySessionState(Optional<TillSessionDTO> sessionOpt) {
        if (sessionOpt.isPresent()) {
            activeSession = sessionOpt.get();
            statusLabel.setText(String.format("Caixa Aberta por %s | Fundo Inicial: %,.2f MT",
                    activeSession.operator(), activeSession.openingBalance()));
            statusLabel.setForeground(UIHelper.APPROVED_GREEN);
            statusLabel.setIcon(UIHelper.icon("fas-lock-open", 14, UIHelper.APPROVED_GREEN));
            statusLabel.setIconTextGap(8);
            if (sessionBanner != null) {
                sessionBanner.setBackground(UIHelper.SELECTION_BG);
                sessionBanner.repaint();
            }
            openSessionBtn.setVisible(false);
            closeSessionBtn.setVisible(true);
            cashMoveBtn.setVisible(true);
            checkoutBtn.setEnabled(true);
            checkoutBtn.setToolTipText(null);
            if (addToCartBtn != null) {
                addToCartBtn.setEnabled(true);
                addToCartBtn.setToolTipText(null);
            }
        } else {
            activeSession = null;
            statusLabel.setText("Caixa Fechada. É necessário abrir sessão antes de vender.");
            statusLabel.setForeground(UIHelper.PENDING_YELLOW);
            statusLabel.setIcon(UIHelper.icon("fas-lock", 14, UIHelper.PENDING_YELLOW));
            statusLabel.setIconTextGap(8);
            if (sessionBanner != null) {
                sessionBanner.setBackground(UIHelper.ROW_ALT);
                sessionBanner.repaint();
            }
            openSessionBtn.setVisible(true);
            closeSessionBtn.setVisible(false);
            cashMoveBtn.setVisible(false);
            checkoutBtn.setEnabled(false);
            checkoutBtn.setToolTipText("Abra a caixa antes de finalizar uma venda.");
            if (addToCartBtn != null) {
                addToCartBtn.setEnabled(false);
                addToCartBtn.setToolTipText("Abra a caixa antes de adicionar artigos.");
            }
        }
    }

    void loadMetadata() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            return;
        }

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

        warehouseCombo.removeAllItems();
        accountCombo.removeAllItems();
        for (WarehouseDTO w : warehousesList) {
            warehouseCombo.addItem(w.name());
        }
        for (TreasuryAccountDTO acc : accountsList) {
            accountCombo.addItem(acc.name() + " (" + String.format("%.2f", acc.balance()) + " MT)");
        }

        filterClients(clientSearchField == null ? "" : clientSearchField.getText());
        catalogController.loadCatalogPage(0);
    }

    private void filterClients(String query) { catalogController.filterClients(query); }

    private void rebuildProductGrid() { catalogController.rebuildProductGrid(); }


    /** Pequeno bloco "label em cima / componente em baixo" para a barra de selects do topo. */
    private static JPanel labeledField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        l.setForeground(UIHelper.ACCENT);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /**
     * Adiciona um produto ao carrinho (quantidade 1). Se já existir uma linha do mesmo produto (sem
     * série), incrementa a quantidade — comportamento de carrinho web. Promoção automática aplicada.
     */
    void addProductToCart(ProductDTO product) { catalogController.addProductToCart(product); }

    void rebuildCartRows() { catalogController.rebuildCartRows(); }

    private javax.swing.event.DocumentListener simpleDocumentListener(Runnable onChange) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        };
    }

    private void openSession() { cashSessionActions.openSession(); }

    private void closeSession() { cashSessionActions.closeSession(); }

    private void manageCashMovements() { cashSessionActions.manageCashMovements(); }

    private void removeFromCart() {
        int selectedView = cartTable.getSelectedRow();
        if (selectedView < 0) {
            showPosNotice(FeedbackType.WARNING, "Seleccione um artigo", "Escolha uma linha do carrinho para remover.");
            return;
        }
        int selected = cartTable.convertRowIndexToModel(selectedView);
        cartItems.remove(selected);
        updateCartTotal(Math.min(selected, cartItems.size() - 1));
    }

    private void changeSelectedQuantity(BigDecimal delta) { catalogController.changeSelectedQuantity(delta); }

    /** Alteração rápida e segura da quantidade; mantém produto, promoção e cálculos oficiais. */
    private void editSelectedCartQuantity() {
        int selectedView = cartTable.getSelectedRow();
        if (selectedView < 0) {
            showPosNotice(FeedbackType.WARNING, "Seleccione um artigo",
                    "Escolha uma linha do carrinho para alterar a quantidade.");
            return;
        }
        int selected = cartTable.convertRowIndexToModel(selectedView);
        if (selected >= cartItems.size()) return;

        CartItem item = cartItems.get(selected);
        JTextField quantityField = new JTextField(item.qty.stripTrailingZeros().toPlainString());
        UIHelper.styleTextField(quantityField);
        JPanel form = UIHelper.createDialogForm(
                "Artigo:", readOnlyField(item.product.name()),
                "Quantidade:", quantityField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Alterar Quantidade",
                "fas-sort-numeric-up", "Actualize a quantidade da linha seleccionada", form)
                .setConfirmButton("Actualizar", "fas-check")
                .setOnSave(() -> {
                    BigDecimal quantity;
                    try {
                        quantity = new BigDecimal(quantityField.getText().trim().replace(',', '.'));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Introduza uma quantidade válida.");
                    }
                    if (quantity.signum() <= 0) {
                        throw new IllegalArgumentException("A quantidade deve ser maior do que zero.");
                    }
                    item.qty = quantity;
                })
                .showDialog();
        if (confirmed) {
            updateCartTotal(selected);
        }
    }

    private static JTextField readOnlyField(String value) {
        JTextField field = new JTextField(value == null ? "" : value);
        UIHelper.styleTextField(field);
        field.setEditable(false);
        return field;
    }

    void updateCartTotal() {
        int selected = cartTable == null || cartTable.getSelectedRow() < 0
                ? -1 : cartTable.convertRowIndexToModel(cartTable.getSelectedRow());
        updateCartTotal(selected);
    }

    void updateCartTotal(int preferredModelRow) {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            net = net.add(item.getSubtotal());
            tax = tax.add(item.getTax());
            total = total.add(item.getTotal());
        }
        totalLabel.setText(String.format("%,.2f MT", total));
        if (subtotalValueLabel != null) {
            subtotalValueLabel.setText(String.format("%,.2f MT", net));
        }
        if (ivaValueLabel != null) {
            ivaValueLabel.setText(String.format("%,.2f MT", tax));
        }
        rebuildCartRows();
        refreshCartView();
        if (cartItemCountLabel != null) {
            BigDecimal units = cartItems.stream()
                    .map(item -> item.qty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String formatted = units.stripTrailingZeros().toPlainString();
            cartItemCountLabel.setText(formatted + (BigDecimal.ONE.compareTo(units) == 0
                    ? " artigo" : " artigos"));
        }
        catalogController.selectAndRevealCartRow(preferredModelRow);
    }

    /** Etiqueta da célula IVA da linha: "Isento" quando taxa 0, senão "valor (taxa%)". */
    static String ivaCellLabel(CartItem item) {
        BigDecimal rate = effectiveTaxRate(item.product.taxRate());
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return "Isento";
        }
        String pct = rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
        return String.format("%,.2f MT (%s%%)", item.getTax(), pct);
    }

    /** Mantém a apresentação do desktop alinhada com o fallback fiscal aplicado no checkout. */
    static BigDecimal effectiveTaxRate(BigDecimal productRate) {
        return productRate != null ? productRate : TaxRates.STANDARD_VAT;
    }

    /** Alterna entre o empty state e a tabela conforme o carrinho tem ou não linhas. */
    private void refreshCartView() {
        if (cartCenter == null) return;
        CardLayout layout = (CardLayout) cartCenter.getLayout();
        layout.show(cartCenter, cartItems.isEmpty() ? "empty" : "table");
    }

    private void runCheckout() {
        if (activeSession == null) {
            showPosNotice(FeedbackType.WARNING, "Sessão de caixa necessária", "Abra uma sessão de caixa antes de finalizar a venda.");
            return;
        }
        if (cartItems.isEmpty()) {
            showPosNotice(FeedbackType.WARNING, "Carrinho vazio", "Adicione pelo menos um artigo antes de finalizar a venda.");
            return;
        }
        if (warehousesList.isEmpty() || accountsList.isEmpty()) {
            showPosNotice(FeedbackType.WARNING, "Configuração incompleta", "Registe um armazém e uma conta de tesouraria.");
            return;
        }

        int clientIdx = clientCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        int accIdx = accountCombo.getSelectedIndex();

        if (whIdx < 0 || accIdx < 0) {
            showPosNotice(FeedbackType.WARNING, "Selecção incompleta",
                    "Seleccione o armazém e a conta de tesouraria.");
            return;
        }

        // Cliente é opcional. Se nada seleccionado, usa-se "Consumidor Final".
        // Se o operador escreveu algo no campo de pesquisa sem seleccionar combo, esse texto
        // vai como walkInName (rótulo para o recibo, sem criar registo de cliente).
        ClientDTO client = (clientIdx >= 0 && clientIdx < filteredClients.size())
                ? filteredClients.get(clientIdx)
                : null;
        String walkInName = null;
        if (client == null) {
            String typed = clientSearchField == null ? "" : clientSearchField.getText().trim();
            if (!typed.isEmpty()) walkInName = typed;
        }
        WarehouseDTO wh = warehousesList.get(whIdx);
        TreasuryAccountDTO acc = accountsList.get(accIdx);

        List<POSCheckoutLineRequest> lines = cartItems.stream()
                .map(i -> new POSCheckoutLineRequest(i.product.id(), i.qty, i.discount, i.batch, i.serial))
                .toList();

        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        boolean fiado = creditCheck != null && creditCheck.isSelected();
        BigDecimal cartTotal = cartItems.stream().map(CartItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        java.util.List<mz.multicore.erp.modules.pos.dto.PosPaymentRequest> payments;
        Long treasuryAccountId;
        if (fiado) {
            payments = java.util.List.of(new mz.multicore.erp.modules.pos.dto.PosPaymentRequest(
                    "CREDIT", cartTotal, BigDecimal.ZERO, "Venda a crédito", null));
            treasuryAccountId = null;
        } else {
            mz.multicore.erp.modules.pos.dto.PosPaymentRequest payment = askPayment(cartTotal, acc.id());
            if (payment == null) return;
            payments = java.util.List.of(payment);
            treasuryAccountId = null;
        }

        POSCheckoutRequest request = new POSCheckoutRequest(operator, companyId, client != null ? client.id() : null,
                walkInName, wh.id(), treasuryAccountId, lines, payments);

        // Checkout corre fora do EDT com indicador "a finalizar venda…" (não congela a UI).
        UIHelper.runWithProgress(this, "A finalizar venda…",
                () -> posApiClient.checkout(request),
                inv -> {
                    String paymentLabel = fiado ? "EM DÍVIDA (fiado)" : "PAGO";
                    ToastManager.success(this, "Venda concluída · " + inv.invoiceNumber() + " · "
                            + inv.totalAmount() + " MT · " + paymentLabel + ".");

                    if (fiado && creditCheck != null) creditCheck.setSelected(false);

                    printReceiptIfConfirmed(inv);

                    // Reset cart
                    cartItems.clear();
                    cartModel.setRowCount(0);
                    updateCartTotal();
                    refreshSessionState();
                    loadTodaySalesSummary();
                    loadMetadata(); // refresh account balance display
                },
                ex -> {
                    if (contingencyManager.handleContingencyCheckout(this, request, cartTotal, ex, () -> {
                        cartItems.clear();
                        cartModel.setRowCount(0);
                        updateCartTotal();
                    })) return;
                    showPosNotice(FeedbackType.ERROR, "Não foi possível concluir a venda", ex.getMessage());
                });
    }

    /**
     * Diálogo de pagamento do checkout (vendas não-fiado). Recolhe o método e, para numerário,
     * o valor entregue pelo cliente com cálculo de troco em tempo real. Devolve o pedido de
     * pagamento pronto a enviar, ou {@code null} se o operador cancelar.
     */
    private mz.multicore.erp.modules.pos.dto.PosPaymentRequest askPayment(BigDecimal total, Long accountId) {
        return PosPaymentDialog.show(total, accountId);
    }

    private void printReceiptIfConfirmed(InvoiceDTO invoice) {
        if (mz.multicore.erp.gui.components.PosDirectPrintEngine.isDirectPrintEnabled()) {
            UIHelper.runWithProgress(this, "A imprimir talão…",
                    () -> posApiClient.renderReceipt(invoice.id()),
                    pdf -> mz.multicore.erp.gui.components.PosDirectPrintEngine.printReceiptSilent(pdf, "recibo-" + invoice.invoiceNumber(), this),
                    ex -> showPosNotice(FeedbackType.ERROR, "Falha na impressão direta", ex.getMessage()));
            return;
        }
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this), FeedbackType.INFO,
                "Imprimir recibo", "Deseja imprimir o recibo da venda " + invoice.invoiceNumber() + "?",
                "Imprimir")) return;
        UIHelper.runWithProgress(this, "A gerar recibo…",
                () -> posApiClient.renderReceipt(invoice.id()),
                pdf -> PrintPreviewDialog.show(this, pdf, "recibo-" + invoice.invoiceNumber()),
                ex -> showPosNotice(FeedbackType.ERROR, "Não foi possível imprimir o recibo", ex.getMessage()));
    }

    /**
     * Scanner USB / leitor de código de barras: ler → procurar → adicionar ao carrinho.
     * Inicia uma sessão de caixa em modo "easy add" — quantidade 1, sem desconto.
     */
    private void handleBarcodeScan() { barcodeActions.handleBarcodeScan(); }

    private JPanel buildSalesHistoryTab() { return salesHistoryPanel.buildPanel(); }

    void refreshSalesHistory() { salesHistoryPanel.refresh(); }

    void showPosLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area,
                error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }

    void showPosNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    void showPosSuccess(String message) { ToastManager.success(this, message); }
    boolean isProductSellable(ProductDTO product) { return product != null && sellableProductIds.contains(product.id()); }
    void registerSellableProduct(ProductDTO product) { java.util.Set<Long> u = new java.util.HashSet<>(sellableProductIds); u.add(product.id()); sellableProductIds = u; }
    void showReturnDialog() { returnDialog.show(); }

    private record PosMetadata(java.util.List<ClientDTO> clients, java.util.List<WarehouseDTO> warehouses, java.util.List<TreasuryAccountDTO> accounts) {}

    private static JPanel buildEmptyStatePanel() {
        JPanel s = new JPanel(new GridBagLayout()); s.setOpaque(false);
        JPanel i = new JPanel(); i.setLayout(new BoxLayout(i, BoxLayout.Y_AXIS)); i.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon("fas-shopping-cart", 48, UIHelper.TEXT_MUTED)); icon.setAlignmentX(0.5f);
        JLabel title = new JLabel("Carrinho vazio"); title.setFont(new Font(UIHelper.FONT, Font.BOLD, 16)); title.setForeground(UIHelper.TEXT_MUTED); title.setAlignmentX(0.5f);
        JLabel hint = new JLabel("Leia um código de barras ou adicione um artigo."); hint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13)); hint.setForeground(UIHelper.TEXT_MUTED); hint.setAlignmentX(0.5f);
        i.add(icon); i.add(Box.createRigidArea(new Dimension(0, 12))); i.add(title); i.add(Box.createRigidArea(new Dimension(0, 4))); i.add(hint); s.add(i); return s;
    }
}
