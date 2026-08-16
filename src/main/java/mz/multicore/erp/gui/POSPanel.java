package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class POSPanel extends JPanel {

    /** Proporção inicial responsiva: catálogo compacto e maior área útil para o carrinho. */
    private static final double CATALOG_WIDTH_RATIO = 0.36;
    private static final int CATALOG_MIN_WIDTH = 380;
    private static final int CART_MIN_WIDTH = 650;

    final POSApiClient posApiClient;
    private final PosSalesHistoryPanel salesHistoryPanel;
    private final PosReturnDialog returnDialog;
    private final PosCashSessionActions cashSessionActions;
    private final PosBarcodeActions barcodeActions;
    private final PosCatalogController catalogController;
    final ComercialApiClient comercialApiClient;
    private final InventoryApiClient inventoryApiClient;
    final FinanceApiClient financeApiClient;
    final PromotionApiClient promotionApiClient;
    final mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser;

    // Active session status
    TillSessionDTO activeSession = null;

    // GUI elements
    private JLabel statusLabel;
    JComboBox<String> clientCombo;
    private JComboBox<String> warehouseCombo;
    private JComboBox<String> accountCombo;
    JTextField clientSearchField;
    JTextField productSearchField;
    JTextField barcodeField;

    DefaultTableModel cartModel;
    JTable cartTable;
    private JLabel cartItemCountLabel;
    private JPanel cartCenter;
    private JScrollPane formScroll;
    JPanel productGrid;
    private JScrollPane productGridScroll;
    private JPanel topSelectsBar;
    private JLabel totalLabel;
    private JLabel subtotalValueLabel;
    private JLabel ivaValueLabel;

    private ModernButton openSessionBtn;
    private ModernButton closeSessionBtn;
    private ModernButton cashMoveBtn;
    private ModernButton checkoutBtn;
    private ModernButton addToCartBtn;
    private JCheckBox creditCheck;
    private JPanel viewCards;
    private ModernButton tabVendaBtn;
    private ModernButton tabHistBtn;
    private boolean historyView = false;
    DefaultTableModel salesHistoryModel;
    JTable salesHistoryTable;
    private JLabel salesHistorySummary;
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
    static class CartItem {
        ProductDTO product;
        BigDecimal qty;
        BigDecimal discount;
        String batch;
        String serial;
        String note;

        CartItem(ProductDTO product, BigDecimal qty, BigDecimal discount, String batch, String serial) {
            this.product = product;
            this.qty = qty;
            this.discount = discount;
            this.batch = batch;
            this.serial = serial;
        }

        /** Líquido, IVA e total da linha — IVA dinâmico via taxa do produto. */
        mz.multicore.erp.architecture.pricing.LineCalculator.LineAmounts amounts() {
            BigDecimal rate = effectiveTaxRate(product.taxRate());
            return mz.multicore.erp.architecture.pricing.LineCalculator.compute(
                    product.unitPrice(), qty, discount, rate);
        }

        /** Valor líquido da linha (sem IVA). */
        BigDecimal getSubtotal() {
            return amounts().net();
        }

        BigDecimal getTax() {
            return amounts().tax();
        }

        BigDecimal getTotal() {
            return amounts().total();
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
        this.posApiClient = posApiClient;
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.financeApiClient = financeApiClient;
        this.promotionApiClient = promotionApiClient;
        this.scaleBarcodeParser = scaleBarcodeParser;
        this.salesHistoryPanel = new PosSalesHistoryPanel(this);
        this.returnDialog = new PosReturnDialog(this);
        this.cashSessionActions = new PosCashSessionActions(this);
        this.barcodeActions = new PosBarcodeActions(this);
        this.catalogController = new PosCatalogController(this);

        setLayout(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(PosLayout.ROOT_VERTICAL_MARGIN, 18,
                PosLayout.ROOT_VERTICAL_MARGIN, 18));

        // 1. TOP BAR — selector de vista (Venda POS | Histórico) à esquerda, na MESMA linha que as
        //    acções de caixa (Abrir/Sangria/Fechar) à direita, para poupar espaço vertical.
        tabVendaBtn = new ModernButton("Venda POS");
        tabVendaBtn.setIcon(UIHelper.icon("fas-cash-register", 14));
        tabVendaBtn.setPreferredSize(new Dimension(150, 38));
        tabVendaBtn.addActionListener(e -> selectView(false));
        tabHistBtn = new ModernButton("Histórico de Vendas");
        tabHistBtn.setIcon(UIHelper.icon("fas-history", 14));
        tabHistBtn.setPreferredSize(new Dimension(190, 38));
        tabHistBtn.addActionListener(e -> selectView(true));

        JPanel segmented = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        segmented.setOpaque(false);
        segmented.add(tabVendaBtn);
        segmented.add(tabHistBtn);

        openSessionBtn = UIHelper.createSuccessButton("Abrir Caixa");
        openSessionBtn.setIcon(UIHelper.icon("fas-lock-open", 14));
        closeSessionBtn = UIHelper.createDangerButton("Fechar Caixa");
        closeSessionBtn.setIcon(UIHelper.icon("fas-lock", 14));
        cashMoveBtn = UIHelper.createWarningButton("Sangria / Suprimento");
        cashMoveBtn.setIcon(UIHelper.icon("fas-exchange-alt", 14));

        JPanel sessionActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sessionActions.setOpaque(false);
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

        JPanel sessionBar = new JPanel(new BorderLayout(0, 3));
        sessionBar.setOpaque(false);
        sessionBar.add(topBar, BorderLayout.NORTH);
        sessionBar.add(statusLabel, BorderLayout.SOUTH);

        // 1b. CÓDIGO DE BARRAS — Enter procura por código e adiciona ao carrinho. Campo de altura
        //     única com o ícone DENTRO do input, para subir e alinhar com os combos do cabeçalho
        //     (ver POS_CABECALHO_COMPACTO_SPEC). A barra própria foi removida para ganhar altura no
        //     catálogo de produtos.
        barcodeField = new JTextField();
        UIHelper.styleTextField(barcodeField);
        barcodeField.putClientProperty("JTextField.placeholderText", "Ler código de barras… (Enter)");
        barcodeField.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        barcodeField.addActionListener(e -> handleBarcodeScan());
        JPanel barcodeBox = PosLayout.iconInputBox("fas-barcode", 16, UIHelper.ACCENT, barcodeField);

        add(sessionBar, BorderLayout.NORTH);


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
        warehouseCombo = new JComboBox<>();
        accountCombo = new JComboBox<>();
        clientCombo = new JComboBox<>();
        UIHelper.styleComboBox(warehouseCombo);
        UIHelper.styleComboBox(accountCombo);
        UIHelper.styleComboBox(clientCombo);

        clientSearchField = new JTextField();
        productSearchField = new JTextField();
        UIHelper.styleTextField(clientSearchField);
        UIHelper.styleTextField(productSearchField);
        clientSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar cliente por nome ou NUIT…");
        productSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar produto por SKU ou nome…");
        clientSearchField.getDocument().addDocumentListener(simpleDocumentListener(() -> filterClients(clientSearchField.getText())));
        productSearchField.getDocument().addDocumentListener(simpleDocumentListener(catalogController::scheduleCatalogReload));

        ModernButton newClientBtn = UIHelper.createPrimaryButton("Novo");
        newClientBtn.setIcon(UIHelper.icon("fas-user-plus", 12));
        newClientBtn.setToolTipText("Criar novo cliente");
        newClientBtn.addActionListener(e -> createClientDialog());

        // Cabeçalho operacional numa única linha: nenhum campo fica escondido ou rouba altura ao carrinho.
        JPanel clientRow = new JPanel(new BorderLayout(6, 0));
        clientRow.setOpaque(false);
        clientRow.add(clientCombo, BorderLayout.CENTER);
        clientRow.add(newClientBtn, BorderLayout.EAST);

        topSelectsBar = new JPanel(new GridBagLayout());
        topSelectsBar.setOpaque(false);
        topSelectsBar.setBorder(new EmptyBorder(4, 0, 4, 0));
        GridBagConstraints tg = new GridBagConstraints();
        tg.fill = GridBagConstraints.HORIZONTAL; tg.anchor = GridBagConstraints.NORTH; tg.gridy = 0;
        tg.gridx = 0; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[0]; tg.insets = new Insets(0, 0, 0, 6);
        topSelectsBar.add(labeledField("Pesquisar cliente", PosLayout.searchRow(clientSearchField)), tg);
        tg.gridx = 1; tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[1];
        topSelectsBar.add(labeledField("Cliente", clientRow), tg);
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
        catalogHeader.add(UIHelper.createSubheading("Produtos"), BorderLayout.NORTH);
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
        rightPanel.add(UIHelper.createSubheading("Carrinho de Vendas (POS)"), BorderLayout.NORTH);

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

        // Empty state — mostrado quando o carrinho está vazio (orienta o operador)
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setOpaque(false);
        JPanel emptyInner = new JPanel();
        emptyInner.setLayout(new BoxLayout(emptyInner, BoxLayout.Y_AXIS));
        emptyInner.setOpaque(false);
        JLabel emptyIcon = new JLabel(UIHelper.icon("fas-shopping-cart", 48, UIHelper.TEXT_MUTED));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyTitle = new JLabel("Carrinho vazio");
        emptyTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        emptyTitle.setForeground(UIHelper.TEXT_MUTED);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyHint = new JLabel("Leia um código de barras ou adicione um artigo.");
        emptyHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        emptyHint.setForeground(UIHelper.TEXT_MUTED);
        emptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptyIcon);
        emptyInner.add(Box.createRigidArea(new Dimension(0, 12)));
        emptyInner.add(emptyTitle);
        emptyInner.add(Box.createRigidArea(new Dimension(0, 4)));
        emptyInner.add(emptyHint);
        emptyState.add(emptyInner);

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
        decreaseBtn.setIcon(UIHelper.icon("fas-minus", 12));
        decreaseBtn.setToolTipText("Diminuir a quantidade seleccionada");
        ModernButton editQtyBtn = UIHelper.createPrimaryButton("Quantidade (F6)");
        editQtyBtn.setIcon(UIHelper.icon("fas-sort-numeric-up", 12));
        ModernButton increaseBtn = UIHelper.createPrimaryButton("+");
        increaseBtn.setIcon(UIHelper.icon("fas-plus", 12));
        increaseBtn.setToolTipText("Aumentar a quantidade seleccionada");
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
        ModernPanel totalRow = new ModernPanel(12);
        totalRow.setBackground(UIHelper.SELECTION_BG);
        totalRow.setLayout(new BorderLayout());
        totalRow.setBorder(new EmptyBorder(7, 12, 7, 12));
        JPanel taxSummary = new JPanel(new GridLayout(2, 2, 12, 2));
        taxSummary.setOpaque(false);
        taxSummary.add(breakdownCaption("Subtotal s/ IVA"));
        taxSummary.add(breakdownValue(subtotalValueLabel));
        taxSummary.add(breakdownCaption("IVA"));
        taxSummary.add(breakdownValue(ivaValueLabel));
        JLabel totalCaption = new JLabel("TOTAL A PAGAR");
        totalCaption.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        totalCaption.setForeground(UIHelper.TEXT_MUTED);
        totalLabel = new JLabel("0,00 MT");
        totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        totalLabel.setForeground(UIHelper.TEXT_LIGHT);
        JPanel payable = new JPanel(new BorderLayout(12, 0));
        payable.setOpaque(false);
        payable.add(totalCaption, BorderLayout.WEST);
        payable.add(totalLabel, BorderLayout.EAST);
        totalRow.add(taxSummary, BorderLayout.WEST);
        totalRow.add(payable, BorderLayout.EAST);

        creditCheck = new JCheckBox("Fiado (cliente paga depois)");
        creditCheck.setForeground(UIHelper.TEXT_LIGHT);
        creditCheck.setOpaque(false);
        creditCheck.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        ModernButton removeBtn = UIHelper.createDangerButton("Remover Selecionado");
        removeBtn.setIcon(UIHelper.icon("fas-trash", 14));
        buttonRow.add(removeBtn, BorderLayout.WEST);
        JPanel creditCell = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        creditCell.setOpaque(false);
        creditCell.add(creditCheck);
        buttonRow.add(creditCell, BorderLayout.CENTER);

        checkoutBtn = UIHelper.createSuccessButton("Finalizar Venda (F9)");
        checkoutBtn.setIcon(UIHelper.icon("fas-check-circle", 14));
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

        refreshSessionState();
    }

    /** Atalhos de caixa previsíveis; Delete fica limitado à tabela para não apagar texto digitado. */
    private void installKeyboardShortcuts() {
        InputMap input = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actions = getActionMap();
        bindShortcut(input, actions, "posProductSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
                () -> focusAndSelect(productSearchField));
        bindShortcut(input, actions, "posClientSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                () -> focusAndSelect(clientSearchField));
        bindShortcut(input, actions, "posEditQuantity", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
                this::editSelectedCartQuantity);
        bindShortcut(input, actions, "posCheckout", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
                this::runCheckout);

        InputMap tableInput = cartTable.getInputMap(JComponent.WHEN_FOCUSED);
        bindShortcut(tableInput, cartTable.getActionMap(), "posRemoveLine",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::removeFromCart);
    }

    static void bindShortcut(InputMap input, ActionMap actions, String name, KeyStroke key, Runnable command) {
        input.put(key, name);
        actions.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { command.run(); }
        });
    }

    private static void focusAndSelect(JTextField field) {
        field.requestFocusInWindow();
        field.selectAll();
    }

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
        if (history) {
            refreshSalesHistory();
        }
    }

    public void onPanelSelected() {
        refreshSessionState();
        loadMetadata();
        if (historyView) {
            refreshSalesHistory();
        }
        // Repõe o formulário no topo para a secção DOCUMENTO (Cliente/Armazém) ficar sempre visível.
        if (formScroll != null) {
            SwingUtilities.invokeLater(() -> formScroll.getVerticalScrollBar().setValue(0));
        }
    }

    void refreshSessionState() {
        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

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

        UIHelper.loadAsync(this, () -> new PosMetadata(comercialApiClient.getAllClients(),
                        inventoryApiClient.getSalesWarehousesByCompany(companyId), financeApiClient.getAllAccounts()),
                this::applyMetadata, error -> showPosLoadError("dados do ponto de venda", error));
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

    private static JLabel breakdownCaption(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    /** Valor (direita) do bloco de discriminação Subtotal/IVA — bem visível. */
    private static JLabel breakdownValue(JLabel l) {
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        l.setForeground(UIHelper.TEXT_LIGHT);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

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

    private void createClientDialog() { catalogController.createClient(); }

    private void openSession() { cashSessionActions.openSession(); }

    private void closeSession() { cashSessionActions.closeSession(); }

    private void manageCashMovements() { cashSessionActions.manageCashMovements(); }

    private void removeFromCart() {
        int selectedView = cartTable.getSelectedRow();
        if (selectedView < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha do carrinho para remover.", "Aviso", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this,
                    "Selecione uma linha do carrinho para alterar a quantidade.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "É obrigatório abrir sessão de caixa.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O carrinho de vendas está vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (warehousesList.isEmpty() || accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Falta registar armazéns ou contas de tesouraria.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int clientIdx = clientCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        int accIdx = accountCombo.getSelectedIndex();

        if (whIdx < 0 || accIdx < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione armazém e conta de tesouraria.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
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

        List<POSCheckoutLineRequest> lines = new ArrayList<>();
        for (CartItem item : cartItems) {
            lines.add(new POSCheckoutLineRequest(
                    item.product.id(),
                    item.qty,
                    item.discount,
                    item.batch,
                    item.serial
            ));
        }

        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        boolean fiado = creditCheck != null && creditCheck.isSelected();
        java.math.BigDecimal cartTotal = java.math.BigDecimal.ZERO;
        for (CartItem item : cartItems) cartTotal = cartTotal.add(item.getTotal());
        cartTotal = cartTotal.setScale(2, RoundingMode.HALF_UP);

        java.util.List<mz.multicore.erp.modules.pos.dto.PosPaymentRequest> payments;
        Long treasuryAccountId;
        if (fiado) {
            // Venda a crédito: pagamento CREDIT pelo total, sem mover tesouraria.
            payments = java.util.List.of(new mz.multicore.erp.modules.pos.dto.PosPaymentRequest(
                    "CREDIT", cartTotal, java.math.BigDecimal.ZERO, "Venda a crédito", null));
            treasuryAccountId = null;
        } else {
            // Diálogo de pagamento: método + valor entregue (numerário) com cálculo de troco.
            mz.multicore.erp.modules.pos.dto.PosPaymentRequest payment = askPayment(cartTotal, acc.id());
            if (payment == null) return; // operador cancelou
            payments = java.util.List.of(payment);
            treasuryAccountId = null; // o pagamento vai pela lista de payments
        }

        POSCheckoutRequest request = new POSCheckoutRequest(
                operator,
                companyId,
                client != null ? client.id() : null,
                walkInName,
                wh.id(),
                treasuryAccountId,
                lines,
                payments
        );

        // Checkout corre fora do EDT com indicador "a finalizar venda…" (não congela a UI).
        UIHelper.runWithProgress(this, "A finalizar venda…",
                () -> posApiClient.checkout(request),
                inv -> {
                    String paymentLabel = fiado ? "EM DÍVIDA (fiado)" : "PAGO";
                    JOptionPane.showMessageDialog(this, "Venda POS efetuada com sucesso!\n" +
                            "Documento emitido: " + inv.invoiceNumber() + "\n" +
                            "Valor Total: " + inv.totalAmount() + " MT (" + paymentLabel + ")", "Venda Concluída", JOptionPane.INFORMATION_MESSAGE);

                    if (fiado && creditCheck != null) creditCheck.setSelected(false);

                    printReceiptIfConfirmed(inv);

                    // Reset cart
                    cartItems.clear();
                    cartModel.setRowCount(0);
                    updateCartTotal();
                    refreshSessionState();
                    loadMetadata(); // refresh account balance display
                },
                ex -> JOptionPane.showMessageDialog(this, "Erro ao processar checkout: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
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
        int choice = JOptionPane.showConfirmDialog(this,
                "Deseja imprimir o recibo da venda " + invoice.invoiceNumber() + "?",
                "Imprimir Recibo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(this, "A gerar recibo…",
                () -> { mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(
                            posApiClient.renderReceipt(invoice.id()), "recibo-" + invoice.invoiceNumber());
                        return null; },
                ok -> { },
                ex -> JOptionPane.showMessageDialog(this, "Erro ao imprimir recibo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Scanner USB / leitor de código de barras: ler → procurar → adicionar ao carrinho.
     * Inicia uma sessão de caixa em modo "easy add" — quantidade 1, sem desconto.
     */
    private void handleBarcodeScan() { barcodeActions.handleBarcodeScan(); }

    private JPanel buildSalesHistoryTab() { return salesHistoryPanel.buildPanel(); }

    void refreshSalesHistory() { salesHistoryPanel.refresh(); }

    void showPosLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    boolean isProductSellable(ProductDTO product) { return product != null && sellableProductIds.contains(product.id()); }

    void registerSellableProduct(ProductDTO product) {
        java.util.Set<Long> updated = new java.util.HashSet<>(sellableProductIds);
        updated.add(product.id());
        sellableProductIds = updated;
    }

    private record PosMetadata(java.util.List<ClientDTO> clients, java.util.List<WarehouseDTO> warehouses,
                               java.util.List<TreasuryAccountDTO> accounts) {}

    void showReturnDialog() { returnDialog.show(); }
}
