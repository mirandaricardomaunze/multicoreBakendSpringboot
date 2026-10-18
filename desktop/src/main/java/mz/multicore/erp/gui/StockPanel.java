package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.InventoryCountApiClient;
import mz.multicore.erp.desktop.client.ProductCategoryApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.CreateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.RegisterMovementRequest;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateWarehouseRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.dto.StockAlertDTO;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;
import java.time.LocalDateTime;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mz.multicore.erp.desktop.client.PrintApiClient;

public class StockPanel extends JPanel {

    final InventoryApiClient inventoryApiClient;
    final PrintApiClient printApiClient;
    final ComercialApiClient comercialApiClient;
    final StockTransferApiClient stockTransferApiClient;
    private final StockTransferActions transferActions;
    private final StockProductActions productActions;
    final InventoryCountApiClient inventoryCountApiClient;
    private final StockInventoryCountActions inventoryCountActions;
    private final StockWarehousesPanel warehousesPanel;
    final ProductCategoryApiClient productCategoryApiClient;
    private final StockCategoriesPanel categoriesPanel;
    private final StockAlertsPanel alertsPanel;
    private final StockBatchesPanel batchesPanel;
    final StockWasteApiClient stockWasteApiClient;
    private final StockWastePanel wastePanel;
    final mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient physicalCountingApiClient;
    private final mz.multicore.erp.gui.inventory.PhysicalInventoryPanel physicalInventoryPanel;

    private JTabbedPane stockTabs;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    // Transfer history
    private DefaultTableModel transferModel;
    JTable transferTable;
    List<StockTransferDTO> transfersList = new ArrayList<>();
    CardLayout transferPagesLayout;
    JPanel transferPages;
    DocumentEditorHost transferEditorHost;
    StockTransferEditorForm transferEditorForm;

    // KPI labels para a aba de transferências
    private final JLabel kpiTransfersSeparation = new JLabel("0");
    private final JLabel kpiTransfersPending = new JLabel("0");
    private final JLabel kpiTransfersInTransit = new JLabel("0");
    private final JLabel kpiTransfersCompleted = new JLabel("0");
    private final JLabel kpiTransfersTotalValue = new JLabel("0.00 MT");

    // Warehouses list
    private JComboBox<String> warehouseFilterCombo;
    List<WarehouseDTO> warehousesList = new ArrayList<>();

    // Stock levels
    private DefaultTableModel stockModel;
    private JTable stockTable;
    List<StockDTO> stocksList = new ArrayList<>();

    // Bloqueio de stock (contagem cega): quantidades ocultas a não-administradores.
    private static final String MASK = "•••";
    private JLabel stockLockBanner;
    private ActionMenuButton moreBtn;
    private boolean stockLockKnown;
    private boolean stockLocked;

    // Gestão de armazéns
    DefaultTableModel warehousesModel;
    JTable warehousesTable;
    List<WarehouseDTO> warehousesFullList = new ArrayList<>();
    // ID do produto por linha visível da tabela (paralelo às linhas, respeita filtros).
    private final java.util.List<Long> stockRowProductIds = new java.util.ArrayList<>();

    // Movements log
    private DefaultTableModel movementsModel;
    private JTable movementsTable;
    private TablePager movementsPager;
    private JComboBox<String> movementProductFilter;
    private JComboBox<String> movementBusinessUnitFilter;
    private JComboBox<String> transferBusinessUnitCombo;

    // Categorias
    DefaultTableModel categoriesModel;
    JTable categoriesTable;
    JTextField categorySearchField;
    java.util.List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categoriesList = new ArrayList<>();
    java.util.List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categoriesFiltered = new ArrayList<>();
    final java.util.Map<Long, Integer> categoryProductCounts = new java.util.HashMap<>();
    List<ProductDTO> catalogProducts = new ArrayList<>();

    public StockPanel(InventoryApiClient inventoryApiClient,
                       ComercialApiClient comercialApiClient,
                       StockTransferApiClient stockTransferApiClient,
                       InventoryCountApiClient inventoryCountApiClient,
                       ProductCategoryApiClient productCategoryApiClient,
                       PrintApiClient printApiClient,
                       StockWasteApiClient stockWasteApiClient) {
        this(inventoryApiClient, comercialApiClient, stockTransferApiClient, inventoryCountApiClient,
             productCategoryApiClient, printApiClient, stockWasteApiClient, null);
    }

    public StockPanel(InventoryApiClient inventoryApiClient,
                       ComercialApiClient comercialApiClient,
                       StockTransferApiClient stockTransferApiClient,
                       InventoryCountApiClient inventoryCountApiClient,
                       ProductCategoryApiClient productCategoryApiClient,
                       PrintApiClient printApiClient,
                       StockWasteApiClient stockWasteApiClient,
                       mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient physicalCountingApiClient) {
        this.inventoryApiClient = inventoryApiClient;
        this.printApiClient = printApiClient;
        this.comercialApiClient = comercialApiClient;
        this.stockTransferApiClient = stockTransferApiClient;
        this.inventoryCountApiClient = inventoryCountApiClient;
        this.productCategoryApiClient = productCategoryApiClient;
        this.stockWasteApiClient = stockWasteApiClient;
        this.physicalCountingApiClient = physicalCountingApiClient;
        this.physicalInventoryPanel = physicalCountingApiClient != null ? new mz.multicore.erp.gui.inventory.PhysicalInventoryPanel(physicalCountingApiClient) : null;
        this.transferActions = new StockTransferActions(this);
        this.productActions = new StockProductActions(this);
        this.inventoryCountActions = new StockInventoryCountActions(this);
        this.warehousesPanel = new StockWarehousesPanel(this);
        this.categoriesPanel = new StockCategoriesPanel(this);
        this.alertsPanel = new StockAlertsPanel(this);
        this.batchesPanel = new StockBatchesPanel(this);
        this.wastePanel = new StockWastePanel(stockWasteApiClient, inventoryApiClient, comercialApiClient);

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        // TOP BAR: heading + globally-relevant catalogue buttons
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        topBar.add(UIHelper.createHeading("Controle de Stock & Armazéns"), BorderLayout.WEST);

        ModernButton newProductBtn = UIHelper.createSuccessButton("Registar Produto");
        newProductBtn.setIcon(UIHelper.icon("fas-plus", 14));
        this.moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Inventário Físico", UIHelper.icon("fas-clipboard-check", 14, UIHelper.APPROVED_GREEN), this::openPhysicalInventoryDialog)
                .addAction("Trancar Stock", UIHelper.icon("fas-lock", 14, UIHelper.PENDING_YELLOW), this::toggleStockLock)
                .addAction("Ficha do Artigo", UIHelper.icon("fas-id-card", 14, UIHelper.ACCENT_BLUE),
                        () -> showProductDetailDialog(selectedStockProductId()))
                .addAction("Editar Produto", UIHelper.icon("fas-edit", 14, UIHelper.PENDING_YELLOW),
                        () -> editProductDialog(selectedStockProductId()))
                .addAction("Etiquetas", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT), this::openLabelDialog);
        moreBtn.actionAt(1).setVisible(isAdmin()); // só ADMIN tranca/destranca

        ModernButton refreshAllBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshAllBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshAllBtn.setToolTipText("Recarregar dados de stock e armazéns");
        refreshAllBtn.addActionListener(e -> onPanelSelected());
        topBar.add(UIHelper.actionsBar(refreshAllBtn, moreBtn, newProductBtn), BorderLayout.EAST);

        stockLockBanner = new JLabel(" ");
        stockLockBanner.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        stockLockBanner.setBorder(new EmptyBorder(8, 2, 0, 0));
        stockLockBanner.setVisible(false);

        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setOpaque(false);
        northWrap.add(topBar, BorderLayout.NORTH);
        northWrap.add(stockLockBanner, BorderLayout.SOUTH);
        JPanel northStack = new JPanel();
        northStack.setOpaque(false);
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        northStack.add(northWrap);
        northStack.add(feedback);
        add(northStack, BorderLayout.NORTH);

        // TABS: Níveis | Movimentos | Transferências
        JTabbedPane tabs = new JTabbedPane();
        this.stockTabs = tabs;
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Níveis de Stock",               UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK),         buildLevelsTab());
        tabs.addTab("Alertas",                       UIHelper.icon("fas-exclamation-triangle", 16, UIHelper.PENDING_YELLOW), buildAlertsTab());
        tabs.addTab("Lotes & Validades",             UIHelper.icon("fas-calendar-times", 16, UIHelper.ACCENT_ORANGE),buildBatchesTab());
        tabs.addTab("Movimentos & Rastreabilidade",  UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE),buildMovementsTab());
        tabs.addTab("Transferências entre Armazéns", UIHelper.icon("fas-truck", 16, UIHelper.ACCENT),         buildTransfersTab());
        tabs.addTab("Gestão de Armazéns",            UIHelper.icon("fas-warehouse", 16, UIHelper.ACCENT_CYAN),     buildWarehousesTab());
        tabs.addTab("Categorias",                    UIHelper.icon("fas-tags", 16, UIHelper.ACCENT_PINK),         buildCategoriesTab());
        tabs.addTab("Quebras & Desperdício",         UIHelper.icon("fas-trash-alt", 16, UIHelper.REJECTED_RED),     wastePanel);
        if (physicalInventoryPanel != null) {
            tabs.addTab("Inventário Físico",         UIHelper.icon("fas-clipboard-check", 16, UIHelper.APPROVED_GREEN), physicalInventoryPanel);
        }

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == wastePanel) {
                wastePanel.reload();
            }
        });

        add(tabs, BorderLayout.CENTER);

        // GLOBAL ACTION LISTENERS
        newProductBtn.addActionListener(e -> createProductDialog());
    }

    public void showWasteManagement() {
        if (stockTabs != null && wastePanel != null) {
            stockTabs.setSelectedComponent(wastePanel);
            wastePanel.reload();
        }
    }

    public void selectWasteTab() {
        showWasteManagement();
    }

    private JTextField stockSearchField;
    private JComboBox<String> stockStatusCombo;
    private JComboBox<String> stockCategoryCombo;
    private JComboBox<String> stockBusinessUnitCombo;

    // Batches tab
    DefaultTableModel alertsOutModel;
    JTable alertsOutTable;
    DefaultTableModel alertsExpModel;
    JTable alertsExpTable;
    JLabel alertsSummary;

    DefaultTableModel batchesModel;
    JTable batchesTable;
    JTextField batchSearchField;
    JComboBox<String> batchExpirationCombo;
    JComboBox<String> batchWarehouseCombo;
    JComboBox<String> batchBusinessUnitCombo;
    JLabel batchesSummary;
    List<mz.multicore.erp.modules.inventory.dto.ProductBatchDTO> batchesList = new ArrayList<>();

    /** Horizonte (dias) considerado "a vencer" no resumo de validades. */
    static final int EXPIRY_SOON_DAYS = 30;

    /** Aba de alertas: produtos esgotados e lotes expirados / a expirar. Cada lista na sua sub-aba. */
    private JPanel buildAlertsTab() { return alertsPanel.buildPanel(); }

    private void loadAlerts() { alertsPanel.refresh(); }

    private JPanel buildBatchesTab() { return batchesPanel.buildPanel(); }

    private void loadBatches() { batchesPanel.refresh(); }

    private JPanel buildLevelsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ActionMenuButton printMenu = UIHelper.createActionMenuButton("Imprimir")
                .addAction("Inventário de Stock", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), this::printInventoryReport)
                .addAction("Livro Valorizado (PGC-NIRF)", UIHelper.icon("fas-book", 14, UIHelper.APPROVED_GREEN), this::printValuedInventoryBook);
        printMenu.setToolTipText("Relatórios e mapas de existências");

        ActionMenuButton barcodeMenu = UIHelper.createActionMenuButton("Leitor & Código")
                .addAction("Modo Coletor (Auditoria)", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_BLUE), this::openInventoryBarcodeCollector)
                .addAction("Imprimir Etiquetas", UIHelper.icon("fas-tags", 14, UIHelper.APPROVED_GREEN), this::openLabelDialog);
        barcodeMenu.setToolTipText("Leitura contínua por scanner e emissão de etiquetas");

        ModernButton refreshLevelsBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshLevelsBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshLevelsBtn.setToolTipText("Recarregar saldos de stock e armazéns");
        refreshLevelsBtn.addActionListener(e -> {
            loadWarehouses();
            loadStocks();
            refreshStockLock();
        });
        warehouseFilterCombo = new JComboBox<>();
        UIHelper.styleComboBox(warehouseFilterCombo);
        warehouseFilterCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        warehouseFilterCombo.addActionListener(e -> filterStocks());

        stockStatusCombo = new JComboBox<>(new String[]{
                "Todos os artigos",
                "Em stock",
                "Stock baixo (< 5)",
                "Sem stock"
        });
        UIHelper.styleComboBox(stockStatusCombo);
        stockStatusCombo.setPreferredSize(new Dimension(125, UIHelper.FORM_CONTROL_HEIGHT));
        stockStatusCombo.addActionListener(e -> filterStocks());

        stockBusinessUnitCombo = new JComboBox<>();
        stockBusinessUnitCombo.addItem("Todos os negócios");
        UIHelper.styleComboBox(stockBusinessUnitCombo);
        stockBusinessUnitCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        stockBusinessUnitCombo.addActionListener(e -> filterStocks());

        stockCategoryCombo = new JComboBox<>();
        stockCategoryCombo.addItem("Todas as categorias");
        UIHelper.styleComboBox(stockCategoryCombo);
        stockCategoryCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        stockCategoryCombo.addActionListener(e -> filterStocks());

        stockSearchField = TableFilter.searchField("Pesquisar por SKU ou nome…");
        stockSearchField.setPreferredSize(new Dimension(175, UIHelper.FORM_CONTROL_HEIGHT));
        stockSearchField.getDocument().addDocumentListener(simpleDocumentListener(this::filterStocks));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar artigo seleccionado (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar artigo");
        peekToggleBtn.setPreferredSize(new Dimension(34, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersLeft = TableFilter.bar(
                stockSearchField,
                TableFilter.label("Negócio:"), stockBusinessUnitCombo,
                TableFilter.label("Armazém:"), warehouseFilterCombo,
                TableFilter.label("Estado:"), stockStatusCombo,
                TableFilter.label("Categoria:"), stockCategoryCombo
        );
        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        filtersRow.add(filtersLeft, BorderLayout.CENTER);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Níveis de Stock", filtersRow,
                refreshLevelsBtn, barcodeMenu, printMenu), BorderLayout.NORTH);

        String[] stockCols = {"Código de Barras", "Referência", "Nome do Produto", "Negócio", "Qtd Unidades", "Qtd Caixas", "Preço", "Estado"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        UIHelper.styleTable(stockTable);
        stockTable.setAutoCreateRowSorter(true);
        stockTable.putClientProperty("noRowInspector", Boolean.TRUE);
        stockTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        stockTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        // Duplo-clique ou Enter numa linha → abre a Ficha Executiva do Artigo com fotografia e métricas.
        stockTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Long id = selectedStockProductId();
                    if (id != null) showProductDetailDialog(id);
                }
            }
        });
        stockTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "viewProduct");
        stockTable.getActionMap().put("viewProduct", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                Long id = selectedStockProductId();
                if (id != null) showProductDetailDialog(id);
            }
        });
        UIHelper.ensureHeadersFit(stockTable);

        TableQuickPeekController peekController = TableQuickPeekController.install(stockTable, card, (peek, mr) -> {
            if (mr < 0 || mr >= stockModel.getRowCount()) return;
            String barcode = String.valueOf(stockModel.getValueAt(mr, 0));
            String ref = String.valueOf(stockModel.getValueAt(mr, 1));
            String name = String.valueOf(stockModel.getValueAt(mr, 2));
            String bu = String.valueOf(stockModel.getValueAt(mr, 3));
            String qtdUnits = String.valueOf(stockModel.getValueAt(mr, 4));
            String qtdBoxes = String.valueOf(stockModel.getValueAt(mr, 5));
            String price = String.valueOf(stockModel.getValueAt(mr, 6));
            String status = String.valueOf(stockModel.getValueAt(mr, 7));

            peek.setTitle(name);
            peek.setSubtitle("SKU/Ref: " + ref);
            peek.setItems(java.util.List.of(
                    new QuickPeekPanel.PeekItem("Código de Barras", barcode, false),
                    new QuickPeekPanel.PeekItem("Referência", ref, false),
                    new QuickPeekPanel.PeekItem("Linha de Negócio", bu, true),
                    new QuickPeekPanel.PeekItem("Qtd Unidades", qtdUnits, true),
                    new QuickPeekPanel.PeekItem("Qtd Embalagens", qtdBoxes, false),
                    new QuickPeekPanel.PeekItem("Preço de Venda", price, false),
                    new QuickPeekPanel.PeekItem("Estado", status, false)
            ));
            peek.getOpenFullButton().setText("Ficha Executiva");
            peek.setOnOpenFullAction(v -> {
                Long id = selectedStockProductId();
                if (id != null) showProductDetailDialog(id);
            });
        });
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane stockScroll = new JScrollPane(stockTable);
        UIHelper.styleScrollPane(stockScroll);
        card.add(stockScroll, BorderLayout.CENTER);
        card.add(mz.multicore.erp.gui.components.ClientTablePagination.install(stockTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        return l;
    }

    private javax.swing.event.DocumentListener simpleDocumentListener(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        };
    }

    private JPanel buildMovementsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton adjustmentBtn = UIHelper.createPrimaryButton("Ajuste / Inventário");
        adjustmentBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        adjustmentBtn.addActionListener(e -> createAdjustmentDialog());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] movCols = {"Data", "Artigo", "Armazém", "Qtd Movimentada", "Tipo Mov.", "Nº Lote", "Nº Série", "Descrição", "Negócio"};
        movementsModel = new DefaultTableModel(movCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movementsTable = new JTable(movementsModel);
        movementsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(movementsTable);
        movementsTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        JScrollPane movScroll = new JScrollPane(movementsTable);
        UIHelper.styleScrollPane(movScroll);
        JTextField mvSearch = TableFilter.searchField("Lote, série ou descrição…");
        mvSearch.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> mvTipo = TableFilter.combo("Todos os tipos",
                "Compra", "Entrada", "Venda", "Transferência", "Ajuste", "Devolução", "Estorno");
        UIHelper.styleComboBox(mvTipo);
        mvTipo.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        movementProductFilter = TableFilter.combo("Todos os artigos");
        movementProductFilter.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));

        movementBusinessUnitFilter = TableFilter.combo("Todos os negócios");
        movementBusinessUnitFilter.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));

        DateField movementFrom = new DateField(null);
        DateField movementTo = new DateField(null);
        movementFrom.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));
        movementTo.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));
        movementFrom.putClientProperty("JTextField.placeholderText", "aaaa-mm-dd");
        movementTo.putClientProperty("JTextField.placeholderText", "aaaa-mm-dd");

        UIHelper.ensureHeadersFit(movementsTable);
        TableFilter.install(movementsTable, mvSearch,
                java.util.List.of(
                        new TableFilter.ColumnFilter(movementProductFilter, 1),
                        new TableFilter.ColumnFilter(mvTipo, 4),
                        new TableFilter.ColumnFilter(movementBusinessUnitFilter, 8)),
                java.util.List.of(),
                java.util.List.of(new TableFilter.DateRangeFilter(movementFrom, movementTo, 0)));

        JPanel mvFilters = TableFilter.bar(
                mvSearch,
                TableFilter.label("Negócio:"), movementBusinessUnitFilter,
                TableFilter.label("Artigo:"), movementProductFilter,
                TableFilter.label("Tipo:"), mvTipo,
                TableFilter.label("De:"), movementFrom,
                TableFilter.label("Até:"), movementTo
        );
        mvFilters.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton refreshMovBtn = UIHelper.createRefreshButton(this::loadMovements);
        ModernButton printMovBtn = UIHelper.createSecondaryButton("Imprimir movimentos");
        printMovBtn.setIcon(UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE));
        card.add(UIHelper.tableCardTop("Rastreabilidade e Movimentos (Lotes & Séries)", mvFilters,
                refreshMovBtn, printMovBtn, adjustmentBtn), BorderLayout.NORTH);
        card.add(movScroll, BorderLayout.CENTER);
        movementsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        movementsPager = new TablePager(this::loadMovementsPage);
        card.add(movementsPager, BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildTransfersTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ModernButton transferBtn = UIHelper.createPrimaryButton("Nova Transferência");
        transferBtn.setIcon(UIHelper.icon("fas-truck", 14));
        transferBtn.addActionListener(e -> transferActions.openCreateEditor());

        ActionMenuButton transferMenu = UIHelper.createActionMenuButton("Fluxo da Transferência")
                .addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), () -> transferActions.openSelectedEditor())
                .addAction("Submeter", UIHelper.icon("fas-paper-plane", 14, UIHelper.PENDING_YELLOW), () -> transferActions.submitSelectedTransfer())
                .addAction("Aprovar", UIHelper.icon("fas-check", 14, UIHelper.APPROVED_GREEN), this::approveSelectedTransfer)
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14, UIHelper.REJECTED_RED), this::rejectSelectedTransfer)
                .addAction("Cancelar", UIHelper.icon("fas-ban", 14, UIHelper.REJECTED_RED), () -> transferActions.cancelSelectedTransfer());

        ActionMenuButton transferDocuments = UIHelper.createActionMenuButton("Documentos")
                .addAction("Guia de Separação", UIHelper.icon("fas-clipboard-list", 14, UIHelper.ACCENT_BLUE), () -> transferActions.printPickingSheetForSelected())
                .addAction("Concluir Separação", UIHelper.icon("fas-tasks", 14, UIHelper.APPROVED_GREEN), () -> transferActions.completeSeparationForSelected())
                .addAction("Imprimir Guia (3 Vias)", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), this::printSelectedTransfer)
                .addAction("Confirmar Recepção", UIHelper.icon("fas-box-open", 14, UIHelper.APPROVED_GREEN), () -> transferActions.receiveSelectedTransfer())
                .addAction("Registar Encomenda", UIHelper.icon("fas-clipboard-check", 14, UIHelper.ACCENT_BLUE), () -> transferActions.recordOrderForSelectedTransfer());

        ModernButton refreshTransfersBtn = UIHelper.createRefreshButton(this::loadTransfers);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        String[] transferCols = {"Nº Guia", "Data", "Origem", "Destino", "Motorista", "Matrícula", "Linhas", "Estado", "Negócio"};
        transferModel = new DefaultTableModel(transferCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        transferTable = new JTable(transferModel);
        UIHelper.styleTable(transferTable);
        transferTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        JScrollPane transferScroll = new JScrollPane(transferTable);
        UIHelper.styleScrollPane(transferScroll);
        JTextField trSearch = TableFilter.searchField("Nº guia, armazém, motorista ou matrícula…");
        JComboBox<String> trEstado = TableFilter.combo("Todos os estados",
                "Rascunho", "Aguarda separação", "Em separação", "Separado", "Pendente de aprovação", "Aprovado", "Em trânsito", "Recebido", "Recebido c/ divergência", "Rejeitado", "Cancelado");
        UIHelper.styleComboBox(trEstado);
        trEstado.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> trPeriodo = TableFilter.periodCombo();
        UIHelper.styleComboBox(trPeriodo);
        trPeriodo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        transferBusinessUnitCombo = TableFilter.combo("Todos os negócios");

        UIHelper.ensureHeadersFit(transferTable);
        TableFilter.install(transferTable, trSearch,
                java.util.List.of(
                        new TableFilter.ColumnFilter(trEstado, 7),
                        new TableFilter.ColumnFilter(transferBusinessUnitCombo, 8)),
                java.util.List.of(new TableFilter.PeriodFilter(trPeriodo, 1)));

        JPanel trFilters = TableFilter.bar(
                trSearch,
                TableFilter.label("Negócio:"), transferBusinessUnitCombo,
                TableFilter.label("Estado:"), trEstado,
                TableFilter.label("Período:"), trPeriodo
        );
        trFilters.setBorder(new EmptyBorder(0, 0, 4, 0));
        card.add(UIHelper.tableCardTop("Transferências entre Armazéns", trFilters,
                refreshTransfersBtn, transferDocuments, transferMenu, transferBtn), BorderLayout.NORTH);
        card.add(transferScroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(transferTable), BorderLayout.SOUTH);

        JPanel kpiGrid = KpiCard.createGrid(5);
        kpiGrid.add(KpiCard.createCard("Aguarda Separação", kpiTransfersSeparation,
                "Picking pendente no armazém", "fas-dolly", UIHelper.PENDING_YELLOW));
        kpiGrid.add(KpiCard.createCard("Aguarda Decisão", kpiTransfersPending,
                "Guias prontas para aprovação", "fas-clock", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Em Trânsito", kpiTransfersInTransit,
                "Mercadoria em viagem entre armazéns", "fas-truck", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Concluídas (Mês)", kpiTransfersCompleted,
                "Transferências finalizadas no período", "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Valor Movimentado", kpiTransfersTotalValue,
                "Total valorizado em mercadoria", "fas-coins", UIHelper.MODULE_STOCK));

        tab.add(kpiGrid, BorderLayout.NORTH);
        tab.add(card, BorderLayout.CENTER);

        transferEditorForm = new StockTransferEditorForm(this);
        transferEditorHost = new DocumentEditorHost("Nova Transferência", transferEditorForm.component(),
                transferActions::saveEditor, transferActions::showList, transferEditorForm::isDirty);
        transferPagesLayout = new CardLayout();
        transferPages = new JPanel(transferPagesLayout);
        transferPages.setOpaque(false);
        transferPages.add(tab, "list");
        transferPages.add(transferEditorHost, "editor");
        return transferPages;
    }


    /**
     * Abre o separador das transferências entre armazéns.
     *
     * <p>A guia de transferência vive no Stock — é lá que ela pertence, porque o que se move é
     * mercadoria entre armazéns da mesma empresa. Mas quem a procura tende a procurá-la ao pé das
     * Guias de Remessa, no Comercial; daí o atalho que chama este método.
     */
    public void showWarehouseTransfers() {
        if (stockTabs == null) return;
        for (int index = 0; index < stockTabs.getTabCount(); index++) {
            if (!stockTabs.getTitleAt(index).startsWith("Transferências")) continue;
            stockTabs.setSelectedIndex(index);
            loadTransfers();
            return;
        }
    }

    public void onPanelSelected() {
        refreshStockLock();
        loadWarehouses();
        loadStocks();
        loadMovements();
        loadTransfers();
        loadBatches();
        loadAlerts();
        loadCategories();
        loadWarehousesManagement();
    }

    // ─── Bloqueio de stock (contagem cega) ───────────────────────────────────

    private static boolean isAdmin() {
        return SignedInUser.isAdmin();
    }

    /** As quantidades devem ser ocultadas ao utilizador actual? (trancado E não-admin) */
    boolean stockHidden() {
        return !isAdmin() && (!stockLockKnown || stockLocked);
    }

    /** Actualiza o texto/estado do botão de bloqueio e o banner conforme o estado actual. */
    private void refreshStockLock() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.isStockCountLocked(companyId), this::applyStockLock,
                error -> applyStockLock(false));
    }

    private void applyStockLock(boolean locked) {
        stockLockKnown = true;
        stockLocked = locked;
        if (moreBtn != null && moreBtn.actionCount() > 1) {
            moreBtn.actionAt(1).setVisible(isAdmin());
            moreBtn.actionAt(1).setText(locked ? "Destrancar Stock" : "Trancar Stock");
            moreBtn.actionAt(1).setIcon(UIHelper.icon(locked ? "fas-lock-open" : "fas-lock", 14));
        }
        if (stockLockBanner != null) {
            if (!locked) {
                stockLockBanner.setVisible(false);
            } else {
                stockLockBanner.setVisible(true);
                stockLockBanner.setIcon(UIHelper.icon("fas-lock", 13, UIHelper.PENDING_YELLOW));
                stockLockBanner.setForeground(UIHelper.PENDING_YELLOW);
                stockLockBanner.setText(isAdmin()
                        ? "Stock trancado — os funcionários não veem quantidades. Como administrador, vê tudo."
                        : "Stock trancado — quantidades visíveis apenas para administradores.");
            }
        }
    }

    private void toggleStockLock() {
        if (!isAdmin()) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        boolean target = !stockLocked;
        UIHelper.runWithProgress(this, "A actualizar bloqueio do stock…", () -> {
            inventoryApiClient.setStockCountLocked(companyId, target);
            return null;
        }, ignored -> onPanelSelected(), this::showStockError);
    }

    // ===== Categorias de produto =====

    private JPanel buildCategoriesTab() { return categoriesPanel.buildPanel(); }

    private void loadCategories() { categoriesPanel.refresh(); }

    private void loadTransfers() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> stockTransferApiClient.findByCompany(companyId), this::applyTransfers,
                error -> showStockLoadError("transferências", error));
    }

    private void applyTransfers(List<StockTransferDTO> loaded) {
        transferModel.setRowCount(0);
        transfersList = loaded != null ? loaded : List.of();

        int separationCount = 0;
        int pendingCount = 0;
        int inTransitCount = 0;
        int completedCount = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (StockTransferDTO t : transfersList) {
            String status = t.status() != null ? t.status() : "";
            if ("AWAITING_SEPARATION".equals(status) || "IN_SEPARATION".equals(status)) {
                separationCount++;
            } else if ("SEPARATED".equals(status) || "PENDING_APPROVAL".equals(status) || "DRAFT".equals(status)) {
                pendingCount++;
            } else if ("IN_TRANSIT".equals(status)) {
                inTransitCount++;
            } else if ("APPROVED".equals(status) || "RECEIVED".equals(status)
                    || "RECEIVED_WITH_DISCREPANCY".equals(status)) {
                if (t.transferDate() == null || t.transferDate().isAfter(thirtyDaysAgo)) {
                    completedCount++;
                }
            }

            if (t.lines() != null) {
                for (StockTransferLineDTO line : t.lines()) {
                    BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ZERO;
                    BigDecimal price = line.safeUnitPrice();
                    totalValue = totalValue.add(qty.multiply(price));
                }
            }

            String driver = t.driverName() != null ? t.driverName()
                    : (t.responsible() != null ? t.responsible() : "-");
            String plate = t.vehiclePlate() != null ? t.vehiclePlate()
                    : (t.vehicle() != null ? t.vehicle() : "-");
            String trBu = t.businessUnitName() != null && !t.businessUnitName().isBlank() ? t.businessUnitName() : "—";
            transferModel.addRow(new Object[]{
                    t.transferNumber(),
                    t.transferDate() != null ? t.transferDate().format(dtf) : "-",
                    t.originWarehouseName(),
                    t.destinationWarehouseName(),
                    driver,
                    plate,
                    t.lines() == null ? 0 : t.lines().size(),
                    t.status(),
                    trBu
            });
        }

        kpiTransfersSeparation.setText(String.valueOf(separationCount));
        kpiTransfersPending.setText(String.valueOf(pendingCount));
        kpiTransfersInTransit.setText(String.valueOf(inTransitCount));
        kpiTransfersCompleted.setText(String.valueOf(completedCount));
        kpiTransfersTotalValue.setText(MultiCurrencyEngine.formatCurrency(totalValue, MultiCurrencyEngine.Currency.MZN));
        if (transferBusinessUnitCombo != null) {
            Object prev = transferBusinessUnitCombo.getSelectedItem();
            java.util.Set<String> bus = new java.util.TreeSet<>();
            for (StockTransferDTO t : transfersList) {
                if (t.businessUnitName() != null && !t.businessUnitName().isBlank()) bus.add(t.businessUnitName());
            }
            transferBusinessUnitCombo.removeAllItems();
            transferBusinessUnitCombo.addItem("Todos os negócios");
            for (String b : bus) transferBusinessUnitCombo.addItem(b);
            if (prev != null) transferBusinessUnitCombo.setSelectedItem(prev);
        }
    }

    private void loadWarehouses() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getWarehousesByCompany(companyId), this::applyWarehouses,
                error -> showStockLoadError("armazéns", error));
    }

    private void applyWarehouses(List<WarehouseDTO> loaded) {
        warehousesList = loaded;

        // Disable listeners temporarily
        Object selectedItem = warehouseFilterCombo.getSelectedItem();
        warehouseFilterCombo.removeAllItems();
        warehouseFilterCombo.addItem("Todos os Armazéns");

        for (WarehouseDTO w : warehousesList) {
            warehouseFilterCombo.addItem(w.name());
        }

        if (selectedItem != null) {
            warehouseFilterCombo.setSelectedItem(selectedItem);
        }
    }

    private void loadStocks() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getStocksByCompany(companyId), this::applyStocks,
                error -> showStockLoadError("saldos de stock", error));
    }

    private void applyStocks(List<StockDTO> loaded) {
        stocksList = loaded;
        syncCategoryCombo();
        syncBusinessUnitCombo();
        filterStocks();
        if (warehousesPanel != null) {
            warehousesPanel.updateKpis();
        }
    }

    /** Popula o combo de categorias com as categorias distintas presentes no stock (preserva a escolha). */
    private void syncCategoryCombo() {
        if (stockCategoryCombo == null) return;
        Object selected = stockCategoryCombo.getSelectedItem();
        java.util.TreeSet<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StockDTO s : stocksList) {
            if (s.categoryName() != null) {
                names.add(s.categoryName());
            }
        }
        stockCategoryCombo.removeAllItems();
        stockCategoryCombo.addItem("Todas as categorias");
        for (String n : names) stockCategoryCombo.addItem(n);
        if (selected != null) stockCategoryCombo.setSelectedItem(selected);
    }

    /** Popula o combo de unidades de negócio com as linhas comerciais distintas presentes no stock. */
    private void syncBusinessUnitCombo() {
        if (stockBusinessUnitCombo == null) return;
        Object selected = stockBusinessUnitCombo.getSelectedItem();
        java.util.TreeSet<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StockDTO s : stocksList) {
            if (s.businessUnitName() != null && !s.businessUnitName().isBlank()) {
                names.add(s.businessUnitName());
            }
        }
        stockBusinessUnitCombo.removeAllItems();
        stockBusinessUnitCombo.addItem("Todos os negócios");
        for (String n : names) stockBusinessUnitCombo.addItem(n);
        stockBusinessUnitCombo.addItem("Sem Linha (Livre)");
        if (selected != null) stockBusinessUnitCombo.setSelectedItem(selected);
    }

    private void filterStocks() {
        stockModel.setRowCount(0);
        stockRowProductIds.clear();
        boolean hide = stockHidden();

        int filterIdx = warehouseFilterCombo.getSelectedIndex();
        Long filterWarehouseId = null;
        if (filterIdx > 0 && (filterIdx - 1) < warehousesList.size()) {
            filterWarehouseId = warehousesList.get(filterIdx - 1).id();
        }

        String query = stockSearchField == null ? "" : stockSearchField.getText().trim().toLowerCase();
        int statusIdx = stockStatusCombo == null ? 0 : stockStatusCombo.getSelectedIndex();
        java.math.BigDecimal lowThreshold = java.math.BigDecimal.valueOf(5);

        String wantCategory = stockCategoryCombo != null && stockCategoryCombo.getSelectedIndex() > 0
                ? String.valueOf(stockCategoryCombo.getSelectedItem()) : null;

        String wantBusinessUnit = stockBusinessUnitCombo != null && stockBusinessUnitCombo.getSelectedIndex() > 0
                ? String.valueOf(stockBusinessUnitCombo.getSelectedItem()) : null;

        for (StockDTO s : stocksList) {
            if (filterWarehouseId != null && !filterWarehouseId.equals(s.warehouseId())) continue;

            if (wantCategory != null) {
                String cat = s.categoryName() == null ? "" : s.categoryName();
                if (!wantCategory.equalsIgnoreCase(cat)) continue;
            }

            if (wantBusinessUnit != null) {
                if ("Sem Linha (Livre)".equalsIgnoreCase(wantBusinessUnit)) {
                    if (s.businessUnitName() != null && !s.businessUnitName().isBlank()) continue;
                } else {
                    String bu = s.businessUnitName() == null ? "" : s.businessUnitName();
                    if (!wantBusinessUnit.equalsIgnoreCase(bu)) continue;
                }
            }

            if (!query.isEmpty()) {
                String sku = s.sku() == null ? "" : s.sku().toLowerCase();
                String reference = s.reference() == null ? "" : s.reference().toLowerCase();
                String barcode = s.barcode() == null ? "" : s.barcode().toLowerCase();
                String name = s.productName() == null ? "" : s.productName().toLowerCase();
                if (!sku.contains(query) && !reference.contains(query) && !barcode.contains(query) && !name.contains(query)) continue;
            }

            java.math.BigDecimal qty = s.quantity() == null ? java.math.BigDecimal.ZERO : s.quantity();
            int statusOk = switch (statusIdx) {
                case 1 -> qty.compareTo(java.math.BigDecimal.ZERO) > 0 ? 1 : 0;        // Em stock
                case 2 -> qty.compareTo(java.math.BigDecimal.ZERO) > 0
                        && qty.compareTo(lowThreshold) < 0 ? 1 : 0;                    // Stock baixo
                case 3 -> qty.compareTo(java.math.BigDecimal.ZERO) <= 0 ? 1 : 0;       // Sem stock
                default -> 1;
            };
            if (statusOk == 0) continue;

            int unitsPerBox = s.unitsPerBox() <= 0 ? 1 : s.unitsPerBox();
            java.math.BigDecimal qtyBoxes = qty.divide(
                    java.math.BigDecimal.valueOf(unitsPerBox), 2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal price = s.unitPrice() == null
                    ? java.math.BigDecimal.ZERO : s.unitPrice();

            java.math.BigDecimal minStk = s.minStock();
            String estado;
            if (qty.compareTo(java.math.BigDecimal.ZERO) <= 0) estado = "ESGOTADO";
            else if (minStk != null && minStk.signum() > 0 && qty.compareTo(minStk) < 0) estado = "BAIXO";
            else estado = "EM STOCK";

            String buName = s.businessUnitName() != null && !s.businessUnitName().isBlank() ? s.businessUnitName() : "—";

            stockModel.addRow(new Object[]{
                    s.barcode() == null ? "—" : s.barcode(),
                    s.reference() == null ? s.sku() : s.reference(),
                    s.productName(),
                    buName,
                    hide ? MASK : String.format("%,.2f", qty),
                    hide ? MASK : String.format("%,.2f", qtyBoxes),
                    price,
                    hide ? MASK : estado
            });
            stockRowProductIds.add(s.productId());
        }
    }

    /** ID do produto da linha seleccionada em Níveis de Stock, ou {@code null} se nada seleccionado. */
    private Long selectedStockProductId() {
        if (stockTable == null) return null;
        int viewRow = stockTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = stockTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= stockRowProductIds.size()) return null;
        return stockRowProductIds.get(modelRow);
    }

    private void loadMovements() {
        if (movementsPager != null) {
            movementsPager.reload();
        } else {
            loadMovementsPage(0, 50);
        }
    }

    private void loadMovementsPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getMovementsPageByCompany(companyId, page, size), this::applyMovementsPage,
                error -> showStockLoadError("movimentos", error));
    }

    private void applyMovementsPage(PageResponse<StockMovementDTO> response) {
        if (movementsPager != null) movementsPager.apply(response);
        applyMovements(response != null ? response.items() : List.of());
    }

    private void applyMovements(List<StockMovementDTO> movements) {
        movementsModel.setRowCount(0);

        Object selectedProduct = movementProductFilter == null ? null : movementProductFilter.getSelectedItem();
        java.util.TreeSet<String> productNames = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (movements != null) {
            for (StockMovementDTO movement : movements) {
                if (movement.productName() != null && !movement.productName().isBlank()) {
                    productNames.add(movement.productName());
                }
            }
        }
        if (catalogProducts != null) {
            for (ProductDTO prod : catalogProducts) {
                if (prod.name() != null && !prod.name().isBlank()) {
                    productNames.add(prod.name());
                }
            }
        }
        if (movementProductFilter != null) {
            movementProductFilter.removeAllItems();
            movementProductFilter.addItem("Todos os artigos");
            productNames.forEach(movementProductFilter::addItem);
            if (selectedProduct != null) movementProductFilter.setSelectedItem(selectedProduct);
        }
        if (movementBusinessUnitFilter != null) {
            Object prevBu = movementBusinessUnitFilter.getSelectedItem();
            java.util.Set<String> bus = new java.util.TreeSet<>();
            if (movements != null) {
                for (StockMovementDTO movement : movements) {
                    if (movement.businessUnitName() != null && !movement.businessUnitName().isBlank()) {
                        bus.add(movement.businessUnitName());
                    }
                }
            }
            movementBusinessUnitFilter.removeAllItems();
            movementBusinessUnitFilter.addItem("Todos os negócios");
            for (String b : bus) movementBusinessUnitFilter.addItem(b);
            if (prevBu != null) movementBusinessUnitFilter.setSelectedItem(prevBu);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (StockMovementDTO m : movements == null ? List.<StockMovementDTO>of() : movements) {
            BigDecimal qty = m.quantity();
            String formattedQty = (qty.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + String.format("%,.2f", qty);
            String buName = m.businessUnitName() != null && !m.businessUnitName().isBlank() ? m.businessUnitName() : "—";
            movementsModel.addRow(new Object[]{
                    m.movementDate().format(dtf),
                    m.productName(),
                    m.warehouseName(),
                    formattedQty,
                    UIHelper.humanStatus(m.movementType()),
                    m.batchNumber() != null ? m.batchNumber() : "-",
                    m.serialNumber() != null ? m.serialNumber() : "-",
                    m.description(),
                    buName
            });
        }
    }


    private void printFilteredMovements(JComboBox<String> productFilter,
                                        DateField fromField,
                                        DateField toField) {
        String product = productFilter != null && productFilter.getSelectedIndex() > 0
                ? String.valueOf(productFilter.getSelectedItem()) : null;
        LocalDate fromDate = fromField != null ? fromField.getDate() : null;
        LocalDate toDate = toField != null ? toField.getDate() : null;

        StringBuilder titleBuilder = new StringBuilder("Movimentos de Stock");
        StringBuilder baseNameBuilder = new StringBuilder("movimentos-stock");

        if (product != null && !product.isBlank()) {
            titleBuilder.append(" — Artigo: ").append(product);
            String prodSlug = product.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            if (prodSlug.length() > 24) prodSlug = prodSlug.substring(0, 24);
            if (!prodSlug.isBlank()) {
                baseNameBuilder.append("-").append(prodSlug);
            }
        }

        if (fromDate != null || toDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter fileFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateLabel;
            if (fromDate != null && toDate != null) {
                dateLabel = fromDate.format(fmt) + " a " + toDate.format(fmt);
                baseNameBuilder.append("-").append(fromDate.format(fileFmt)).append("-").append(toDate.format(fileFmt));
            } else if (fromDate != null) {
                dateLabel = "desde " + fromDate.format(fmt);
                baseNameBuilder.append("-desde-").append(fromDate.format(fileFmt));
            } else {
                dateLabel = "até " + toDate.format(fmt);
                baseNameBuilder.append("-ate-").append(toDate.format(fileFmt));
            }
            titleBuilder.append(" (").append(dateLabel).append(")");
        }

        TableExportAction.export(this, printApiClient, movementsTable,
                titleBuilder.toString(), baseNameBuilder.toString());
    }

    private JPanel buildWarehousesTab() { return warehousesPanel.buildPanel(); }

    private void loadWarehousesManagement() { warehousesPanel.refresh(); }

    private void createWarehouseDialogV2() { warehousesPanel.createWarehouseDialogV2(); }

    private void openPhysicalInventoryDialog() {
        if (stockTabs != null && physicalInventoryPanel != null) {
            stockTabs.setSelectedComponent(physicalInventoryPanel);
        } else {
            inventoryCountActions.openPhysicalInventoryDialog();
        }
    }

    void openLabelDialog() {
        Long selectedId = selectedStockProductId();
        java.util.List<ProductDTO> products = new ArrayList<>();
        if (selectedId != null) {
            catalogProducts.stream().filter(p -> p.id().equals(selectedId)).findFirst().ifPresent(products::add);
        }
        if (products.isEmpty()) {
            products.addAll(catalogProducts);
        }
        if (products.isEmpty()) {
            showStockNotice(FeedbackType.WARNING, "Produto necessário", "Registe um produto antes de criar etiquetas.");
            return;
        }
        mz.multicore.erp.gui.components.ShelfLabelsDialog.show(SwingUtilities.getWindowAncestor(this), products);
    }

    private void createAdjustmentDialog() {
        List<ProductDTO> products = new ArrayList<>(catalogProducts);
        if (products.isEmpty() || warehousesList.isEmpty()) {
            showStockNotice(FeedbackType.WARNING, "Configuração incompleta", "Registe produtos e armazéns antes de ajustar o stock.");
            return;
        }

        ProductSearchComboBox prodCombo = new ProductSearchComboBox();
        JComboBox<String> whCombo = new JComboBox<>();
        JTextField countedField = new JTextField();
        JTextField reasonField = new JTextField("Contagem física de inventário");

        UIHelper.styleComboBox(whCombo);
        UIHelper.styleTextField(countedField);
        UIHelper.styleTextField(reasonField);

        prodCombo.setProducts(products);
        for (WarehouseDTO w : warehousesList) {
            whCombo.addItem(w.name());
        }

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Selecionar Artigo:", prodCombo,
                "Selecionar Armazém:", whCombo,
                "Quantidade contada:", countedField,
                "Motivo / Descrição:", reasonField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Contagem / Ajuste de Stock",
                "fas-clipboard-list", "Acerte a quantidade física em stock", dialogPanel);
        dialog.setConfirmButton("Registar ajuste", "fas-check");
        dialog.setOnSaveAsync(() -> {
            int whIdx = whCombo.getSelectedIndex();
            ProductDTO product = prodCombo.selectedProduct();
            if (product == null || whIdx < 0) {
                throw new IllegalArgumentException("Seleccione o artigo e o armazém.");
            }
            BigDecimal counted;
            try {
                counted = new BigDecimal(countedField.getText().trim().replace(",", "."));
                UIHelper.clearFieldInvalid(countedField);
            } catch (NumberFormatException error) {
                UIHelper.markFieldInvalid(countedField, "Introduza uma quantidade válida.");
                throw new IllegalArgumentException("Introduza uma quantidade válida.");
            }
            String reason = reasonField.getText().trim();
            if (reason.isBlank()) {
                UIHelper.markFieldInvalid(reasonField, "Indique o motivo do ajuste.");
                throw new IllegalArgumentException("Indique o motivo do ajuste.");
            }
            UIHelper.clearFieldInvalid(reasonField);
            WarehouseDTO warehouse = warehousesList.get(whIdx);
            CreateStockAdjustmentRequest request = new CreateStockAdjustmentRequest(
                    CurrentUserContext.getCurrentCompanyId(), product.id(), warehouse.id(), counted, reason);
            return () -> inventoryApiClient.adjustStock(request);
        });
        if (dialog.showDialog()) {
            ToastManager.success(this, "Contagem de stock registada com sucesso.");
            onPanelSelected();
        }
    }

    /**
     * Diálogo profissional de entrada de lote com validade. Se {@code preselected} for indicado,
     * pré-selecciona o produto (usado depois de registar um produto novo).
     */
    /** Inteiro ≥ 0 a partir de texto livre; vazio/inválido → 0 (para os campos de caixas). */
    void createBatchEntryDialog(ProductDTO product) { productActions.createBatchEntryDialog(product); }

    private void createProductDialog() { productActions.createProductDialog(); }

    void editProductDialog(Long productId) { productActions.editProductDialog(productId); }

    void showProductDetailDialog(Long productId) {
        if (productId == null) {
            showStockNotice(FeedbackType.INFO, "Selecione um artigo", "Selecione um produto na tabela para ver a ficha detalhada.");
            return;
        }
        ProductDTO product = null;
        for (ProductDTO p : catalogProducts) {
            if (p.id().equals(productId)) { product = p; break; }
        }
        if (product == null) {
            try {
                catalogProducts = comercialApiClient.getAllProducts();
                for (ProductDTO p : catalogProducts) {
                    if (p.id().equals(productId)) { product = p; break; }
                }
            } catch (Exception ignored) {}
        }
        if (product != null) new StockProductDetailDialog(this, product).showDialog();
    }

    void saveTransferEditor() { transferActions.saveEditor(); }
    private void approveSelectedTransfer() { transferActions.approveSelectedTransfer(); }
    private void rejectSelectedTransfer() { transferActions.rejectSelectedTransfer(); }
    private void printSelectedTransfer() { transferActions.printSelectedTransfer(); }

    private void printInventoryReport() {
        if (stockTable != null && stockTable.getRowCount() == 0) {
            showStockNotice(FeedbackType.INFO, "Sem dados para imprimir", "A listagem de stock está vazia.");
            return;
        }
        Long warehouseId = null;
        String fileSuffix = "todos";
        if (warehouseFilterCombo != null) {
            int idx = warehouseFilterCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < warehousesList.size()) {
                WarehouseDTO selectedWarehouse = warehousesList.get(idx - 1);
                warehouseId = selectedWarehouse.id();
                fileSuffix = selectedWarehouse.name().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
                if (fileSuffix.isBlank()) fileSuffix = "armazem-" + selectedWarehouse.id();
            }
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Long selectedWarehouseId = warehouseId;
        String suffix = fileSuffix;
        UIHelper.runWithProgress(this, "A gerar inventário em PDF…",
                () -> inventoryApiClient.renderInventoryReport(companyId, selectedWarehouseId),
                pdf -> PrintPreviewDialog.show(this, pdf, "inventario-stock-" + suffix),
                this::showStockError);
    }

    private void printValuedInventoryBook() {
        if (printApiClient == null) {
            showStockNotice(FeedbackType.INFO, "Impressão indisponível", "O cliente de impressão não está inicializado.");
            return;
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        int year = java.time.LocalDate.now().getYear();
        UIHelper.runWithProgress(this, "A gerar Livro de Inventário Valorizado (PGC-NIRF)…",
                () -> printApiClient.renderValuedInventoryBook(companyId, year),
                pdf -> PrintPreviewDialog.show(this, pdf, "livro-inventario-valorizado-" + year),
                this::showStockError);
    }

    private void openInventoryBarcodeCollector() {
        var collectorDialog = new BarcodeScannerCollectorDialog(
                SwingUtilities.getWindowAncestor(this),
                BarcodeScannerCollectorDialog.CollectorMode.INVENTORY,
                comercialApiClient,
                null,
                inventoryApiClient,
                this::loadStocks
        );
        collectorDialog.showDialog();
    }

    void showStockLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area, error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }
    void showStockError(Throwable error) { showStockNotice(FeedbackType.ERROR, "Não foi possível concluir a operação", error.getMessage()); }
    void showStockNotice(FeedbackType type, String title, String message) { feedback.show(type, title, message, null, null); }
    void showStockSuccess(String message) { ToastManager.success(this, message); }

    private record StockAlerts(List<StockAlertDTO> outOfStock, List<ProductBatchDTO> expiring) {}
    private record CategoryData(List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categories, List<ProductDTO> products) {}
}
