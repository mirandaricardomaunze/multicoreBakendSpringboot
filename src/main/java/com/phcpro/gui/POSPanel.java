package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ClientDTO;
import com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest;
import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.pos.model.TillSession;
import com.phcpro.modules.pos.dto.POSCheckoutLineRequest;
import com.phcpro.modules.pos.dto.POSCheckoutRequest;
import com.phcpro.modules.pos.dto.POSReturnRequest;
import com.phcpro.modules.pos.service.POSService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class POSPanel extends JPanel {

    /** Largura fixa do formulário (esquerda) para deixar o resto do espaço ao carrinho. */
    private static final int FORM_PANEL_WIDTH = 400;

    private final POSService posService;
    private final ComercialService comercialService;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final com.phcpro.modules.printing.ReceiptPrintService receiptPrintService;
    private final com.phcpro.modules.company.service.CompanyService companyService;
    private final com.phcpro.modules.promotions.service.PromotionService promotionService;

    // Active session status
    private TillSession activeSession = null;

    // GUI elements
    private JLabel statusLabel;
    private JComboBox<String> clientCombo;
    private JComboBox<String> warehouseCombo;
    private JComboBox<String> accountCombo;
    private JComboBox<String> productCombo;
    private JTextField clientSearchField;
    private JTextField productSearchField;
    private JTextField barcodeField;

    private JTextField qtyField;
    private JTextField discountField;
    private JTextField batchField;
    private JTextField expirationField;
    private JTextField serialField;

    private DefaultTableModel cartModel;
    private JTable cartTable;
    private JPanel cartCenter;
    private JScrollPane formScroll;
    private JPanel productGrid;
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
    private DefaultTableModel salesHistoryModel;
    private JTable salesHistoryTable;
    private JLabel salesHistorySummary;
    private List<InvoiceDTO> salesHistoryList = new ArrayList<>();

    private List<ProductDTO> productsList = new ArrayList<>();
    private List<ProductDTO> filteredProducts = new ArrayList<>();
    private List<ClientDTO> clientsList = new ArrayList<>();
    private List<ClientDTO> filteredClients = new ArrayList<>();
    private List<Warehouse> warehousesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();

    // Cart items representation
    private static class CartItem {
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
        com.phcpro.architecture.pricing.LineCalculator.LineAmounts amounts() {
            BigDecimal rate = product.taxRate() != null ? product.taxRate() : BigDecimal.ZERO;
            return com.phcpro.architecture.pricing.LineCalculator.compute(
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

    private final List<CartItem> cartItems = new ArrayList<>();

    public POSPanel(
            POSService posService,
            ComercialService comercialService,
            InventoryService inventoryService,
            FinanceService financeService,
            com.phcpro.modules.printing.ReceiptPrintService receiptPrintService,
            com.phcpro.modules.company.service.CompanyService companyService,
            com.phcpro.modules.promotions.service.PromotionService promotionService
    ) {
        this.posService = posService;
        this.comercialService = comercialService;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
        this.receiptPrintService = receiptPrintService;
        this.companyService = companyService;
        this.promotionService = promotionService;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

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
        cashMoveBtn = UIHelper.createSecondaryButton("Sangria / Suprimento");
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
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(UIHelper.PENDING_YELLOW);

        JPanel sessionBar = new JPanel(new BorderLayout(0, 8));
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
        barcodeField.setFont(new Font("Segoe UI", Font.BOLD, 14));
        barcodeField.addActionListener(e -> handleBarcodeScan());
        JPanel barcodeBox = iconInputBox("fas-barcode", 16, UIHelper.ACCENT, barcodeField);

        add(sessionBar, BorderLayout.NORTH);


        // 2. MAIN POS WORKSPACE: FORM (esquerda) & CART (direita) num JSplitPane redimensionável.
        //    O resize favorece o carrinho (resizeWeight alto) e o formulário arranca com largura
        //    confortável mas pode ser arrastado pelo operador.
        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workspace.setOpaque(false);
        workspace.setBorder(null);
        workspace.setDividerSize(10);
        workspace.setContinuousLayout(true);
        workspace.setResizeWeight(0.0); // espaço extra da janela vai para o carrinho (direita)
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
        productSearchField.getDocument().addDocumentListener(simpleDocumentListener(() -> filterProducts(productSearchField.getText())));

        ModernButton newClientBtn = UIHelper.createSuccessButton("+ Novo");
        newClientBtn.setToolTipText("Criar novo cliente");
        newClientBtn.addActionListener(e -> createClientDialog());

        // Barra superior compacta de selects de documento (Cliente | Armazém | Conta) — estilo web.
        JPanel clientRow = new JPanel(new BorderLayout(6, 0));
        clientRow.setOpaque(false);
        clientRow.add(clientCombo, BorderLayout.CENTER);
        clientRow.add(newClientBtn, BorderLayout.EAST);
        JPanel clientPicker = stackedPicker(searchRow(clientSearchField), clientRow);

        topSelectsBar = new JPanel(new GridBagLayout());
        topSelectsBar.setOpaque(false);
        topSelectsBar.setBorder(new EmptyBorder(4, 0, 4, 0));
        GridBagConstraints tg = new GridBagConstraints();
        tg.fill = GridBagConstraints.HORIZONTAL; tg.anchor = GridBagConstraints.NORTH; tg.gridy = 0;
        tg.gridx = 0; tg.weightx = 0.34; tg.insets = new Insets(0, 0, 0, 12);
        topSelectsBar.add(labeledField("Cliente", clientPicker), tg);
        tg.gridx = 1; tg.weightx = 0.16;
        topSelectsBar.add(labeledField("Armazém Expedição", warehouseCombo), tg);
        tg.gridx = 2; tg.weightx = 0.16;
        topSelectsBar.add(labeledField("Conta Tesouraria", accountCombo), tg);
        tg.gridx = 3; tg.weightx = 0.34; tg.insets = new Insets(0, 0, 0, 0);
        topSelectsBar.add(labeledField("Código de barras", barcodeBox), tg);

        // Catálogo (esquerda do workspace): pesquisa + grid de cards clicáveis
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setOpaque(false);
        leftPanel.setMinimumSize(new Dimension(360, 10));
        JPanel catalogHeader = new JPanel(new BorderLayout(0, 8));
        catalogHeader.setOpaque(false);
        catalogHeader.add(UIHelper.createSubheading("Produtos"), BorderLayout.NORTH);
        catalogHeader.add(searchRow(productSearchField), BorderLayout.SOUTH);
        leftPanel.add(catalogHeader, BorderLayout.NORTH);

        productGrid = new JPanel(new GridLayout(0, 2, 12, 12));
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
        catalogCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        catalogCard.add(productGridScroll, BorderLayout.CENTER);
        leftPanel.add(catalogCard, BorderLayout.CENTER);
        workspace.setLeftComponent(leftPanel);

        // RIGHT: CART TABLE & CHECKOUT
        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(UIHelper.createSubheading("Carrinho de Vendas (POS)"), BorderLayout.NORTH);

        ModernPanel cartCard = new ModernPanel(16);
        cartCard.setLayout(new BorderLayout(0, 15));
        cartCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cartCols = {"Artigo", "Preço Unit.", "Qtd", "Desc %", "Lote/Série", "Líquido", "IVA", "Total"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        UIHelper.styleTable(cartTable);
        cartTable.setFillsViewportHeight(true);
        cartTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        // Larguras: "Artigo" leva o espaço; colunas numéricas/metadados ficam compactas.
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(240); // Artigo
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Preço Unit.
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Qtd
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(60);  // Desc %
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Lote/Série
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Líquido
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // IVA
        cartTable.getColumnModel().getColumn(7).setPreferredWidth(110); // Total
        // Altura confortável: viewport para ~12 linhas; o scroll trata do excesso de produtos.
        cartTable.setPreferredScrollableViewportSize(new Dimension(620, cartTable.getRowHeight() * 12));
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        cartScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIHelper.styleScrollPane(cartScroll);

        // Empty state — mostrado quando o carrinho está vazio (orienta o operador)
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setOpaque(false);
        JPanel emptyInner = new JPanel();
        emptyInner.setLayout(new BoxLayout(emptyInner, BoxLayout.Y_AXIS));
        emptyInner.setOpaque(false);
        JLabel emptyIcon = new JLabel(UIHelper.icon("fas-shopping-cart", 48, UIHelper.TEXT_MUTED));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyTitle = new JLabel("Carrinho vazio");
        emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        emptyTitle.setForeground(UIHelper.TEXT_MUTED);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyHint = new JLabel("Leia um código de barras ou adicione um artigo.");
        emptyHint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        cartCenter.add(emptyState, "empty");
        cartCenter.add(cartScroll, "table");
        cartCard.add(cartCenter, BorderLayout.CENTER);

        // Cart Actions (Remove & Total & Checkout)
        JPanel cartBottom = new JPanel();
        cartBottom.setLayout(new BoxLayout(cartBottom, BoxLayout.Y_AXIS));
        cartBottom.setOpaque(false);

        // Row 0: discriminação Subtotal s/ IVA + IVA — bloco sempre visível (legenda + valor por linha)
        subtotalValueLabel = new JLabel("0,00 MT");
        ivaValueLabel = new JLabel("0,00 MT");
        JPanel breakdownRow = new JPanel(new GridBagLayout());
        breakdownRow.setOpaque(false);
        breakdownRow.setBorder(new EmptyBorder(2, 4, 2, 4));
        GridBagConstraints bg = new GridBagConstraints();
        bg.gridx = 0; bg.gridy = 0; bg.weightx = 1.0; bg.fill = GridBagConstraints.HORIZONTAL;
        breakdownRow.add(breakdownCaption("Subtotal s/ IVA"), bg);
        bg.gridx = 1; bg.weightx = 0; bg.fill = GridBagConstraints.NONE; bg.anchor = GridBagConstraints.EAST;
        breakdownRow.add(breakdownValue(subtotalValueLabel), bg);
        bg.gridx = 0; bg.gridy = 1; bg.weightx = 1.0; bg.fill = GridBagConstraints.HORIZONTAL; bg.anchor = GridBagConstraints.WEST;
        bg.insets = new Insets(4, 0, 0, 0);
        breakdownRow.add(breakdownCaption("IVA"), bg);
        bg.gridx = 1; bg.weightx = 0; bg.fill = GridBagConstraints.NONE; bg.anchor = GridBagConstraints.EAST;
        breakdownRow.add(breakdownValue(ivaValueLabel), bg);

        // Row 1: Total em destaque — faixa de acento com legenda + valor grande (info nº1 do POS)
        ModernPanel totalRow = new ModernPanel(12);
        totalRow.setBackground(UIHelper.SELECTION_BG);
        totalRow.setLayout(new BorderLayout());
        totalRow.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel totalCaption = new JLabel("TOTAL A PAGAR");
        totalCaption.setFont(new Font("Segoe UI", Font.BOLD, 12));
        totalCaption.setForeground(UIHelper.TEXT_MUTED);
        totalLabel = new JLabel("0,00 MT");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        totalLabel.setForeground(UIHelper.TEXT_LIGHT);
        totalRow.add(totalCaption, BorderLayout.WEST);
        totalRow.add(totalLabel, BorderLayout.EAST);

        // Row 2: Fiado checkbox (linha própria, evita ser esmagado entre botões)
        creditCheck = new JCheckBox("Fiado (cliente paga depois)");
        creditCheck.setForeground(UIHelper.TEXT_LIGHT);
        creditCheck.setOpaque(false);
        creditCheck.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel creditRow = new JPanel(new BorderLayout());
        creditRow.setOpaque(false);
        creditRow.add(creditCheck, BorderLayout.WEST);

        // Row 3: Action Buttons
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        ModernButton removeBtn = UIHelper.createDangerButton("Remover Selecionado");
        removeBtn.setIcon(UIHelper.icon("fas-trash", 14));
        buttonRow.add(removeBtn, BorderLayout.WEST);

        checkoutBtn = UIHelper.createSuccessButton("Finalizar Venda (F9)");
        checkoutBtn.setIcon(UIHelper.icon("fas-check-circle", 14));
        buttonRow.add(checkoutBtn, BorderLayout.EAST);

        cartBottom.add(breakdownRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 6)));
        cartBottom.add(totalRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 8)));
        cartBottom.add(creditRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 8)));
        cartBottom.add(buttonRow);
        cartCard.add(cartBottom, BorderLayout.SOUTH);

        // Container do carrinho com scroll: acompanha a largura do viewport e só faz scroll vertical
        // quando falta altura — evita que a tabela colapse para só o cabeçalho em janelas baixas.
        VScrollPanel cartHolder = new VScrollPanel(new BorderLayout());
        cartHolder.add(cartCard, BorderLayout.CENTER);
        JScrollPane cartCardScroll = new JScrollPane(cartHolder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cartCardScroll.setBorder(BorderFactory.createEmptyBorder());
        cartCardScroll.setOpaque(false);
        cartCardScroll.getViewport().setOpaque(false);
        cartCardScroll.getVerticalScrollBar().setUnitIncrement(16);

        rightPanel.add(cartCardScroll, BorderLayout.CENTER);
        rightPanel.setMinimumSize(new Dimension(400, 10));
        workspace.setRightComponent(rightPanel);
        workspace.setDividerLocation(FORM_PANEL_WIDTH);

        JPanel salesTop = new JPanel(new BorderLayout(0, 8));
        salesTop.setOpaque(false);
        salesTop.add(topSelectsBar, BorderLayout.NORTH);

        JPanel salesTab = new JPanel(new BorderLayout(0, 12));
        salesTab.setOpaque(false);
        salesTab.setBorder(new EmptyBorder(15, 5, 5, 5));
        salesTab.add(salesTop, BorderLayout.NORTH);
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
        checkoutBtn.addActionListener(e -> runCheckout());

        refreshSessionState();
    }

    /** Comuta entre a vista de venda e o histórico, alternando o estilo dos botões do topo. */
    private void selectView(boolean history) {
        this.historyView = history;
        if (viewCards != null) {
            ((CardLayout) viewCards.getLayout()).show(viewCards, history ? "hist" : "venda");
        }
        Color active = UIHelper.ACCENT_BLUE;
        Color activeHover = UIHelper.ACCENT_BLUE.brighter();
        Color idle = new Color(55, 65, 81);
        Color idleHover = new Color(75, 85, 99);
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

    private void refreshSessionState() {
        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        Optional<TillSession> sessionOpt = posService.getActiveSession(operator, companyId);
        if (sessionOpt.isPresent()) {
            activeSession = sessionOpt.get();
            statusLabel.setText(String.format("Caixa Aberta por %s | Fundo Inicial: %,.2f MT",
                    activeSession.getOperator(), activeSession.getOpeningBalance()));
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

    private void loadMetadata() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        clientsList = comercialService.getAllClients();
        productsList = comercialService.getAllProducts();
        warehousesList = inventoryService.getWarehousesByCompany(companyId);
        accountsList = financeService.getAllAccounts();

        warehouseCombo.removeAllItems();
        accountCombo.removeAllItems();
        for (Warehouse w : warehousesList) {
            warehouseCombo.addItem(w.getName());
        }
        for (TreasuryAccountDTO acc : accountsList) {
            accountCombo.addItem(acc.name() + " (" + String.format("%.2f", acc.balance()) + " MT)");
        }

        filterClients(clientSearchField == null ? "" : clientSearchField.getText());
        filterProducts(productSearchField == null ? "" : productSearchField.getText());
    }

    private void filterClients(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        filteredClients = clientsList.stream()
                .filter(c -> q.isEmpty()
                        || (c.name() != null && c.name().toLowerCase().contains(q))
                        || (c.taxId() != null && c.taxId().toLowerCase().contains(q)))
                .toList();
        clientCombo.removeAllItems();
        for (ClientDTO c : filteredClients) {
            clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }
    }

    private void filterProducts(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        filteredProducts = productsList.stream()
                .filter(p -> q.isEmpty()
                        || (p.sku() != null && p.sku().toLowerCase().contains(q))
                        || (p.reference() != null && p.reference().toLowerCase().contains(q))
                        || (p.barcode() != null && p.barcode().toLowerCase().contains(q))
                        || (p.name() != null && p.name().toLowerCase().contains(q)))
                .toList();
        rebuildProductGrid();
    }

    /** Reconstrói o grid de cards de produto a partir de {@link #filteredProducts}. */
    private void rebuildProductGrid() {
        if (productGrid == null) return;
        productGrid.removeAll();
        if (filteredProducts.isEmpty()) {
            JLabel empty = new JLabel(productsList.isEmpty()
                    ? "Não há produtos cadastrados."
                    : "Nenhum produto corresponde à pesquisa.");
            empty.setForeground(UIHelper.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(20, 8, 20, 8));
            productGrid.add(empty);
        } else {
            for (ProductDTO p : filteredProducts) {
                productGrid.add(productCard(p));
            }
        }
        productGrid.revalidate();
        productGrid.repaint();
    }

    /** Card clicável de um produto: imagem (ou marcador) + nome + preço. Clique adiciona ao carrinho. */
    private JComponent productCard(ProductDTO p) {
        ModernPanel card = new ModernPanel(12);
        card.setLayout(new BorderLayout(0, 6));
        card.setBackground(UIHelper.BG_CARD);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel image = new JLabel("", SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(120, 84));
        javax.swing.Icon img = UIHelper.imageIconFromBytes(p.image(), 120, 84);
        if (img != null) {
            image.setIcon(img);
        } else {
            image.setIcon(UIHelper.icon("fas-box", 40, UIHelper.TEXT_MUTED));
        }
        card.add(image, BorderLayout.NORTH);

        JLabel name = new JLabel("<html><div style='text-align:center'>" + escapeHtml(p.name()) + "</div></html>", SwingConstants.CENTER);
        name.setForeground(UIHelper.TEXT_LIGHT);
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card.add(name, BorderLayout.CENTER);

        JLabel price = new JLabel(String.format("%,.2f MT", p.unitPrice()), SwingConstants.CENTER);
        price.setForeground(UIHelper.ACCENT_BLUE);
        price.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(price, BorderLayout.SOUTH);

        card.setToolTipText(productLabel(p));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { addProductToCart(p); }
        });
        return card;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Legenda (esquerda) do bloco de discriminação Subtotal/IVA. */
    private static JLabel breakdownCaption(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    /** Valor (direita) do bloco de discriminação Subtotal/IVA — bem visível. */
    private static JLabel breakdownValue(JLabel l) {
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(UIHelper.TEXT_LIGHT);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    /** Pequeno bloco "label em cima / componente em baixo" para a barra de selects do topo. */
    private static JPanel labeledField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIHelper.ACCENT);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /**
     * Adiciona um produto ao carrinho (quantidade 1). Se já existir uma linha do mesmo produto (sem
     * série), incrementa a quantidade — comportamento de carrinho web. Promoção automática aplicada.
     */
    private void addProductToCart(ProductDTO product) {
        if (activeSession == null) {
            JOptionPane.showMessageDialog(this,
                    "Não é possível adicionar artigos sem caixa aberta.\nClique em \"Abrir Caixa\" primeiro.",
                    "Caixa Fechada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (CartItem it : cartItems) {
            if (it.serial == null && it.product.id().equals(product.id())) {
                it.qty = it.qty.add(BigDecimal.ONE);
                updateCartTotal();
                return;
            }
        }
        BigDecimal discount = BigDecimal.ZERO;
        String note = "-";
        var promo = promotionService.bestPromotion(
                CurrentUserContext.getCurrentCompanyId(), product.id(), product.categoryId(), BigDecimal.ONE);
        if (promo.isPresent()) {
            discount = promo.get().discountPercent();
            note = "Promo: " + promo.get().name();
        }
        CartItem item = new CartItem(product, BigDecimal.ONE, discount, null, null);
        item.note = note;
        cartItems.add(item);
        updateCartTotal();
    }

    /** Reconstrói todas as linhas da tabela do carrinho a partir de {@link #cartItems}. */
    private void rebuildCartRows() {
        cartModel.setRowCount(0);
        for (CartItem item : cartItems) {
            cartModel.addRow(new Object[]{
                    item.product.name(),
                    String.format("%.2f MT", item.product.unitPrice()),
                    item.qty.stripTrailingZeros().toPlainString(),
                    item.discount.stripTrailingZeros().toPlainString() + "%",
                    item.note != null ? item.note : "-",
                    String.format("%,.2f MT", item.getSubtotal()),
                    ivaCellLabel(item),
                    String.format("%,.2f MT", item.getTotal())
            });
        }
    }

    private String productLabel(ProductDTO p) {
        String code = p.barcode() != null && !p.barcode().isBlank()
                ? p.barcode()
                : p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        return code + " - " + p.name();
    }

    private javax.swing.event.DocumentListener simpleDocumentListener(Runnable onChange) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        };
    }

    private void createClientDialog() {
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "NUIT / NIF:", taxIdField,
                "Email:", emailField,
                "Endereço:", addressField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Cliente", "fas-address-book", "Cadastro rápido de cliente", form).showDialog();
        if (!confirmed) return;

        String name = nameField.getText().trim();
        String taxId = taxIdField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || taxId.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome, NUIT e Email são obrigatórios.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ClientDTO created = comercialService.createClient(name, taxId, email, address);
            clientsList = comercialService.getAllClients();
            clientSearchField.setText(created.name());
            filterClients(clientSearchField.getText());
            int idx = -1;
            for (int i = 0; i < filteredClients.size(); i++) {
                if (filteredClients.get(i).id().equals(created.id())) { idx = i; break; }
            }
            if (idx >= 0) clientCombo.setSelectedIndex(idx);
            JOptionPane.showMessageDialog(this, "Cliente '" + created.name() + "' criado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSession() {
        BigDecimal bal = UIHelper.promptAmount("Abrir Caixa", "fas-lock-open",
                "Saldo inicial em numerário na gaveta", "Saldo de Abertura (MT):", BigDecimal.ZERO);
        if (bal == null) return;
        try {
            String operator = CurrentUserContext.getUsername();
            Long companyId = CurrentUserContext.getCurrentCompanyId();

            posService.openSession(operator, bal, companyId);
            JOptionPane.showMessageDialog(this, "Sessão de caixa aberta com sucesso!", "Informação", JOptionPane.INFORMATION_MESSAGE);
            refreshSessionState();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeSession() {
        if (activeSession == null) return;

        BigDecimal closingReal = UIHelper.promptAmount("Fechar Caixa", "fas-lock",
                "Numerário fisicamente contado na gaveta", "Saldo Físico no Fecho (MT):", BigDecimal.ZERO);
        if (closingReal == null) return;

        // Conta de tesouraria que recebe o depósito do numerário da sessão (opcional).
        Long depositAccountId = chooseDepositAccount();

        try {
            TillSession closed = posService.closeSession(activeSession.getId(), closingReal, depositAccountId);

            String summary = String.format("Sessão Fechada com sucesso!\n" +
                    "Saldo Esperado: %,.2f MT\n" +
                    "Saldo Real: %,.2f MT\n" +
                    "Diferença: %,.2f MT", closed.getClosingBalanceExpected(), closed.getClosingBalanceReal(), closed.getDifference());
            JOptionPane.showMessageDialog(this, summary, "Fecho de Caixa", JOptionPane.INFORMATION_MESSAGE);
            
            refreshSessionState();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Pergunta ao operador para que conta de tesouraria deve ir o depósito do numerário
     * da sessão. Devolve o id da conta, ou null se o operador optar por não depositar
     * agora (ou não houver contas configuradas).
     */
    private Long chooseDepositAccount() {
        if (accountsList == null || accountsList.isEmpty()) return null;

        String[] options = new String[accountsList.size() + 1];
        for (int i = 0; i < accountsList.size(); i++) {
            options[i] = accountsList.get(i).name();
        }
        options[accountsList.size()] = "Não depositar agora";

        int choice = JOptionPane.showOptionDialog(this,
                "Depositar o numerário da sessão em que conta de tesouraria?",
                "Depósito de Fecho de Caixa",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0 || choice == accountsList.size()) return null;
        return accountsList.get(choice).id();
    }

    private void manageCashMovements() {
        if (activeSession == null) return;

        String[] options = {"SUPRIMENTO (Entrada de Dinheiro)", "SANGRIA (Retirada de Dinheiro)"};
        int opt = JOptionPane.showOptionDialog(this, "Selecione o tipo de movimento:", "Movimentar Caixa",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (opt < 0) return;
        String type = (opt == 0) ? "SUPRIMENTO" : "SANGRIA";

        JTextField amountField = new JTextField();
        JTextField descField = new JTextField();
        JPanel dialogPanel = UIHelper.createDialogForm(
                "Valor (MT):", amountField,
                "Descrição / Motivo:", descField
        );

        int confirm = JOptionPane.showConfirmDialog(this, dialogPanel, type, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amt = new BigDecimal(amountField.getText().trim());
                String desc = descField.getText().trim();
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(this, "O valor deve ser maior do que zero.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                posService.addCashMovement(activeSession.getId(), type, amt, desc);
                JOptionPane.showMessageDialog(this, "Movimento de caixa registado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Valor de montante inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // (Adicionar ao carrinho agora é feito por clique no card — ver addProductToCart. O FEFO é
    //  aplicado pelo backend no checkout; deixou de haver pré-visualização no formulário.)

    private void removeFromCart() {
        int selected = cartTable.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha do carrinho para remover.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cartItems.remove(selected);
        cartModel.removeRow(selected);
        updateCartTotal();
    }

    private void updateCartTotal() {
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
    }

    /** Etiqueta da célula IVA da linha: "Isento" quando taxa 0, senão "valor (taxa%)". */
    private static String ivaCellLabel(CartItem item) {
        BigDecimal rate = item.product.taxRate() != null ? item.product.taxRate() : BigDecimal.ZERO;
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return "Isento";
        }
        String pct = rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
        return String.format("%,.2f MT (%s%%)", item.getTax(), pct);
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
            JOptionPane.showMessageDialog(this, "Falta cadastrar armazéns ou contas de tesouraria.", "Erro", JOptionPane.ERROR_MESSAGE);
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
        Warehouse wh = warehousesList.get(whIdx);
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

        java.util.List<com.phcpro.modules.pos.dto.PosPaymentRequest> payments;
        Long treasuryAccountId;
        if (fiado) {
            // Venda a crédito: pagamento CREDIT pelo total, sem mover tesouraria.
            payments = java.util.List.of(new com.phcpro.modules.pos.dto.PosPaymentRequest(
                    "CREDIT", cartTotal, java.math.BigDecimal.ZERO, "Venda a crédito", null));
            treasuryAccountId = null;
        } else {
            // Diálogo de pagamento: método + valor entregue (numerário) com cálculo de troco.
            com.phcpro.modules.pos.dto.PosPaymentRequest payment = askPayment(cartTotal, acc.id());
            if (payment == null) return; // operador cancelou
            payments = java.util.List.of(payment);
            treasuryAccountId = null; // o pagamento vai pela lista de payments
        }

        POSCheckoutRequest request = new POSCheckoutRequest(
                operator,
                companyId,
                client != null ? client.id() : null,
                walkInName,
                wh.getId(),
                treasuryAccountId,
                lines,
                payments
        );

        // Checkout corre fora do EDT com indicador "a finalizar venda…" (não congela a UI).
        UIHelper.runWithProgress(this, "A finalizar venda…",
                () -> posService.checkout(request),
                inv -> {
                    String paymentLabel = fiado ? "EM DÍVIDA (fiado)" : "PAGO";
                    JOptionPane.showMessageDialog(this, "Venda POS efetuada com sucesso!\n" +
                            "Documento emitido: " + inv.getInvoiceNumber() + "\n" +
                            "Valor Total: " + inv.getTotalAmount() + " MT (" + paymentLabel + ")", "Venda Concluída", JOptionPane.INFORMATION_MESSAGE);

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
    private com.phcpro.modules.pos.dto.PosPaymentRequest askPayment(BigDecimal total, Long accountId) {
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Numerário", "Cartão", "Transferência"});
        UIHelper.styleComboBox(methodCombo);

        JTextField totalField = new JTextField(String.format("%,.2f MT", total));
        UIHelper.styleTextField(totalField);
        totalField.setEditable(false);

        JTextField tenderedField = new JTextField(total.toPlainString());
        UIHelper.styleTextField(tenderedField);

        JLabel changeLabel = new JLabel("Troco: 0,00 MT");
        changeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        changeLabel.setForeground(UIHelper.APPROVED_GREEN);

        // Recalcula o troco a cada alteração; só relevante quando o método é numerário.
        Runnable recompute = () -> {
            boolean cash = methodCombo.getSelectedIndex() == 0;
            tenderedField.setEnabled(cash);
            if (!cash) {
                changeLabel.setText("Troco: —");
                return;
            }
            try {
                BigDecimal tendered = new BigDecimal(tenderedField.getText().trim().replace(",", "."));
                BigDecimal change = tendered.subtract(total);
                if (change.compareTo(BigDecimal.ZERO) < 0) {
                    changeLabel.setForeground(UIHelper.PENDING_YELLOW);
                    changeLabel.setText(String.format("Falta: %,.2f MT", change.abs()));
                } else {
                    changeLabel.setForeground(UIHelper.APPROVED_GREEN);
                    changeLabel.setText(String.format("Troco: %,.2f MT", change));
                }
            } catch (NumberFormatException ex) {
                changeLabel.setForeground(UIHelper.PENDING_YELLOW);
                changeLabel.setText("Troco: valor inválido");
            }
        };
        methodCombo.addActionListener(e -> recompute.run());
        tenderedField.getDocument().addDocumentListener(simpleDocumentListener(recompute));
        recompute.run();

        JPanel form = UIHelper.createDialogForm(
                "Método de pagamento:", methodCombo,
                "Total a pagar:", totalField,
                "Valor entregue (MT):", tenderedField
        );
        changeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        changeLabel.setBorder(new EmptyBorder(8, 4, 0, 4));
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(form, BorderLayout.CENTER);
        panel.add(changeLabel, BorderLayout.SOUTH);

        // Modal premium (ModernFormDialog): ícone + subtítulo + botão estilizado. A validação corre no
        // onSave — lançar excepção mantém o modal aberto (sem recursão).
        com.phcpro.modules.pos.dto.PosPaymentRequest[] result = {null};
        boolean ok = new ModernFormDialog(UIHelper.mainWindow, "Pagamento", "fas-money-bill-wave",
                "Receba do cliente e confirme o troco", panel)
                .setConfirmButton("Confirmar Pagamento", "fas-check")
                .setOnSave(() -> {
                    int methodIdx = methodCombo.getSelectedIndex();
                    if (methodIdx == 0) {
                        // Numerário: valida valor entregue ≥ total; entra na gaveta (sem conta).
                        BigDecimal tendered;
                        try {
                            tendered = new BigDecimal(tenderedField.getText().trim().replace(",", "."));
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Valor entregue inválido.");
                        }
                        if (tendered.compareTo(total) < 0) {
                            throw new IllegalArgumentException("O valor entregue é inferior ao total a pagar.");
                        }
                        result[0] = new com.phcpro.modules.pos.dto.PosPaymentRequest("CASH", total, tendered, null, null);
                    } else {
                        String method = (methodIdx == 1) ? "CARD" : "BANK_TRANSFER";
                        result[0] = new com.phcpro.modules.pos.dto.PosPaymentRequest(method, total, total, null, accountId);
                    }
                })
                .showDialog();
        return ok ? result[0] : null;
    }

    private void printReceiptIfConfirmed(Invoice invoice) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Deseja imprimir o recibo da venda " + invoice.getInvoiceNumber() + "?",
                "Imprimir Recibo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(this, "A gerar recibo…",
                () -> { com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(
                            receiptPrintService.render(invoice.getId()), "recibo-" + invoice.getInvoiceNumber());
                        return null; },
                ok -> { },
                ex -> JOptionPane.showMessageDialog(this, "Erro ao imprimir recibo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Scanner USB / leitor de código de barras: ler → procurar → adicionar ao carrinho.
     * Inicia uma sessão de caixa em modo "easy add" — quantidade 1, sem desconto.
     */
    private void handleBarcodeScan() {
        String code = barcodeField.getText() == null ? "" : barcodeField.getText().trim();
        if (code.isEmpty()) return;

        ProductDTO product = comercialService.findProductByBarcode(code);
        if (product == null) {
            JOptionPane.showMessageDialog(this,
                    "Produto com código de barras '" + code + "' não encontrado.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            barcodeField.selectAll();
            barcodeField.requestFocusInWindow();
            return;
        }

        // Adiciona com quantidade 1 ao carrinho (mesmo caminho do clique no card; faz merge de qtd).
        addProductToCart(product);
        barcodeField.setText("");
        barcodeField.requestFocusInWindow();
    }

    // ─── Form-layout helpers ────────────────────────────────────────────────────

    /**
     * Painel para colocar dentro de um {@link JScrollPane} vertical: acompanha a largura do viewport
     * (sem scroll horizontal) e só permite scroll vertical quando o conteúdo é mais alto que o viewport
     * (caso contrário, estica para preencher a altura — a tabela usa o espaço disponível).
     */
    private static final class VScrollPanel extends JPanel implements Scrollable {
        VScrollPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && vp.getHeight() >= getPreferredSize().height;
        }
    }

    private static JPanel stackedPicker(JComponent top, JComponent bottom) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(bottom, BorderLayout.CENTER);
        return p;
    }

    /** Campo de pesquisa com ícone de lupa vectorial **dentro** do input (caixa única, aspecto profissional). */
    private static JPanel searchRow(JTextField field) {
        return iconInputBox("fas-search", 13, UIHelper.TEXT_MUTED, field);
    }

    /**
     * Campo de texto de altura única ({@code FORM_CONTROL_HEIGHT}) com um ícone vectorial **dentro**
     * do input, à esquerda. Base de {@link #searchRow} (lupa) e do campo de código de barras
     * (`fas-barcode`), para todos alinharem com os combos do cabeçalho.
     */
    private static JPanel iconInputBox(String iconCode, int iconSize, Color iconColor, JTextField field) {
        JPanel box = new JPanel(new BorderLayout(8, 0));
        box.setBackground(UIHelper.FIELD_BG);
        box.setBorder(iconBoxBorder(UIHelper.BORDER, 1));

        JLabel icon = new JLabel(UIHelper.icon(iconCode, iconSize, iconColor));
        box.add(icon, BorderLayout.WEST);

        // O campo herda o aspecto da caixa: sem borda/fundo próprios para o ícone parecer dentro do
        // input. O realce de foco vive na CAIXA (o campo opta por não desenhar a sua borda de foco).
        field.putClientProperty("noFocusBorder", Boolean.TRUE);
        field.setBorder(new EmptyBorder(6, 8, 6, 0));
        field.setOpaque(false);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { box.setBorder(iconBoxBorder(UIHelper.ACCENT, 2)); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { box.setBorder(iconBoxBorder(UIHelper.BORDER, 1)); }
        });
        box.add(field, BorderLayout.CENTER);

        int h = UIHelper.FORM_CONTROL_HEIGHT;
        box.setPreferredSize(new Dimension(box.getPreferredSize().width, h));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        return box;
    }

    private static javax.swing.border.Border iconBoxBorder(Color line, int thickness) {
        return BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(line, thickness, true),
                new EmptyBorder(0, 10 - (thickness - 1), 0, 8));
    }

    private static int addSectionHeader(JPanel host, GridBagConstraints gbc, int row, String text) {
        JLabel section = new JLabel(text);
        section.setFont(new Font("Segoe UI", Font.BOLD, 12));
        section.setForeground(UIHelper.ACCENT);
        section.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 70)));

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.weighty = 0.0;
        gbc.insets = new Insets(row == 0 ? 0 : 14, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        host.add(section, gbc);
        return row + 1;
    }

    private static int addFullRowField(JPanel host, GridBagConstraints gbc, int row, String label, JComponent control) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setForeground(UIHelper.TEXT_MUTED);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 6, 2, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        host.add(lbl, gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 6, 10, 6);
        host.add(control, gbc);
        return row + 2;
    }

    private static int addTwoColumnRow(JPanel host, GridBagConstraints gbc, int row,
                                        String leftLabel, JComponent leftControl,
                                        String rightLabel, JComponent rightControl) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(4, 6, 2, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel l = new JLabel(leftLabel + ":");
        JLabel r = new JLabel(rightLabel + ":");
        l.setForeground(UIHelper.TEXT_MUTED);
        r.setForeground(UIHelper.TEXT_MUTED);

        gbc.gridx = 0; gbc.gridy = row;     host.add(l, gbc);
        gbc.gridx = 1;                       host.add(r, gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 6, 10, 6);
        gbc.gridx = 0; host.add(leftControl, gbc);
        gbc.gridx = 1; host.add(rightControl, gbc);
        return row + 2;
    }

    private JPanel buildSalesHistoryTab() {
        String[] cols = {"ID", "Nº Venda", "Data", "Operador", "Cliente", "Total", "Estado"};
        salesHistoryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        salesHistoryTable = new JTable(salesHistoryModel);
        UIHelper.styleTable(salesHistoryTable);
        salesHistoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        salesHistoryTable.setFillsViewportHeight(true);
        // Esconder coluna ID
        salesHistoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        salesHistoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        salesHistoryTable.getColumnModel().getColumn(0).setWidth(0);
        // Larguras proporcionais
        salesHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(140);  // Nº Venda
        salesHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Data
        salesHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Operador
        salesHistoryTable.getColumnModel().getColumn(4).setPreferredWidth(160);  // Cliente
        salesHistoryTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Total
        salesHistoryTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Estado

        JScrollPane scroll = new JScrollPane(salesHistoryTable);
        UIHelper.styleScrollPane(scroll);

        salesHistorySummary = new JLabel(" ");
        salesHistorySummary.setForeground(UIHelper.TEXT_LIGHT);
        salesHistorySummary.setBorder(new EmptyBorder(8, 8, 8, 8));

        ModernButton reprintBtn = UIHelper.createPrimaryButton("Reimprimir Recibo");
        reprintBtn.setIcon(UIHelper.icon("fas-print", 14));
        reprintBtn.addActionListener(e -> {
            int row = salesHistoryTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecione uma venda primeiro.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long invoiceId = (Long) salesHistoryModel.getValueAt(row, 0);
            String invNum = String.valueOf(salesHistoryModel.getValueAt(row, 1));
            UIHelper.runWithProgress(this, "A gerar recibo…",
                    () -> { com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(
                                receiptPrintService.render(invoiceId), "recibo-" + invNum);
                            return null; },
                    ok -> { },
                    ex -> JOptionPane.showMessageDialog(this,
                            "Erro ao gerar recibo: " + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE));
        });

        ModernButton returnBtn = UIHelper.createSecondaryButton("Devolver / Trocar");
        returnBtn.setIcon(UIHelper.icon("fas-undo", 14));
        returnBtn.addActionListener(e -> showReturnDialog());

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refreshSalesHistory());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setOpaque(false);
        buttons.add(refreshBtn);
        buttons.add(returnBtn);
        buttons.add(reprintBtn);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 5, 5, 5));
        content.add(salesHistorySummary, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }

    private void refreshSalesHistory() {
        if (salesHistoryModel == null || salesHistorySummary == null) return;

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        salesHistoryList = comercialService.getPOSSalesByCompany(companyId);
        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        salesHistoryModel.setRowCount(0);
        for (var inv : salesHistoryList) {
            salesHistoryModel.addRow(new Object[]{
                    inv.id(),
                    inv.invoiceNumber(),
                    inv.createdAt() != null ? inv.createdAt().format(dtf) : "—",
                    inv.createdBy() != null ? inv.createdBy() : "—",
                    inv.clientName() != null ? inv.clientName() : "—",
                    String.format("%,.2f MT", inv.totalAmount()),
                    inv.status() != null ? inv.status().name() : "—"
            });
        }
        BigDecimal total = salesHistoryList.stream()
                .map(InvoiceDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        salesHistorySummary.setText(String.format(
                "<html><b>%d</b> vendas POS — total <b>%,.2f MT</b></html>",
                salesHistoryList.size(), total));
    }

    private void showReturnDialog() {
        int selectedRow = salesHistoryTable == null ? -1 : salesHistoryTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= salesHistoryList.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma venda no histórico primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (warehousesList.isEmpty()) {
            loadMetadata();
        }
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não há armazéns configurados para receber a devolução.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        InvoiceDTO invoice = salesHistoryList.get(selectedRow);
        DefaultTableModel linesModel = new DefaultTableModel(
                new String[]{"Linha ID", "Produto", "Vendido", "Qtd a devolver"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        for (var line : invoice.lines()) {
            linesModel.addRow(new Object[]{
                    line.id(),
                    line.productName(),
                    line.quantity().stripTrailingZeros().toPlainString(),
                    "0"
            });
        }
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);
        linesTable.getColumnModel().getColumn(0).setMinWidth(0);
        linesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        linesTable.getColumnModel().getColumn(0).setWidth(0);

        JComboBox<String> warehouseReturnCombo = new JComboBox<>();
        for (Warehouse warehouse : warehousesList) {
            warehouseReturnCombo.addItem(warehouse.getName());
        }
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CASH", "CARD", "BANK_TRANSFER", "CREDIT"});
        JComboBox<String> refundAccountCombo = new JComboBox<>();
        for (TreasuryAccountDTO account : accountsList) {
            refundAccountCombo.addItem(account.name());
        }
        JTextField reasonField = new JTextField("Devolução de cliente");
        UIHelper.styleComboBox(warehouseReturnCombo);
        UIHelper.styleComboBox(methodCombo);
        UIHelper.styleComboBox(refundAccountCombo);
        UIHelper.styleTextField(reasonField);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(new JScrollPane(linesTable), BorderLayout.CENTER);
        panel.add(UIHelper.createDialogForm(
                "Armazém de entrada:", warehouseReturnCombo,
                "Método de reembolso:", methodCombo,
                "Conta para reembolso:", refundAccountCombo,
                "Motivo:", reasonField
        ), BorderLayout.SOUTH);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Devolver / Trocar venda " + invoice.invoiceNumber(),
                "fas-undo", "Devolução por nota de crédito", panel).setConfirmButton("Confirmar", "fas-check").showDialog();
        if (!confirmed) {
            return;
        }

        List<CreateCreditNoteLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                BigDecimal qty = new BigDecimal(String.valueOf(linesModel.getValueAt(i, 3)).trim().replace(",", "."));
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(new CreateCreditNoteLineRequest((Long) linesModel.getValueAt(i, 0), qty));
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida em alguma linha.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (lines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe pelo menos uma quantidade a devolver.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String method = String.valueOf(methodCombo.getSelectedItem());
        Long accountId = null;
        if (!"CASH".equals(method) && !"CREDIT".equals(method)) {
            int accIdx = refundAccountCombo.getSelectedIndex();
            if (accIdx < 0 || accIdx >= accountsList.size()) {
                JOptionPane.showMessageDialog(this, "Selecione a conta de tesouraria para o reembolso.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            accountId = accountsList.get(accIdx).id();
        }

        try {
            CreditNoteDTO note = posService.returnSale(new POSReturnRequest(
                    CurrentUserContext.getUsername(),
                    CurrentUserContext.getCurrentCompanyId(),
                    invoice.id(),
                    warehousesList.get(warehouseReturnCombo.getSelectedIndex()).getId(),
                    reasonField.getText().trim(),
                    method,
                    accountId,
                    lines
            ));
            JOptionPane.showMessageDialog(this,
                    "Devolução registada com sucesso.\nNota de crédito: " + note.noteNumber()
                            + "\nTotal: " + note.totalAmount() + " MT",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            int exchange = JOptionPane.showConfirmDialog(this,
                    "Pretende lançar agora a venda de troca/substituição?",
                    "Troca", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (exchange == JOptionPane.YES_OPTION) {
                selectView(false);
            }
            refreshSalesHistory();
            refreshSessionState();
            loadMetadata();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
