package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.SearchField;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.InventoryApiClient;
import com.phcpro.desktop.client.InventoryCountApiClient;
import com.phcpro.desktop.client.ProductCategoryApiClient;
import com.phcpro.desktop.client.StockTransferApiClient;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.inventory.dto.CreateStockAdjustmentRequest;
import com.phcpro.modules.inventory.dto.CreateStockTransferLineRequest;
import com.phcpro.modules.inventory.dto.CreateStockTransferRequest;
import com.phcpro.modules.inventory.dto.CreateWarehouseRequest;
import com.phcpro.modules.inventory.dto.RegisterMovementRequest;
import com.phcpro.modules.inventory.dto.StockDTO;
import com.phcpro.modules.inventory.dto.StockMovementDTO;
import com.phcpro.modules.inventory.dto.StockTransferDTO;
import com.phcpro.modules.inventory.dto.UpdateWarehouseRequest;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import com.phcpro.modules.inventory.dto.StockAlertDTO;
import com.phcpro.modules.inventory.dto.ProductBatchDTO;
import com.phcpro.modules.printing.PdfFileSaver;

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

public class StockPanel extends JPanel {

    final InventoryApiClient inventoryApiClient;
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

    // Transfer history
    private DefaultTableModel transferModel;
    JTable transferTable;
    List<StockTransferDTO> transfersList = new ArrayList<>();

    // Warehouses list
    private JComboBox<String> warehouseFilterCombo;
    List<WarehouseDTO> warehousesList = new ArrayList<>();

    // Stock levels
    private DefaultTableModel stockModel;
    private JTable stockTable;
    private List<StockDTO> stocksList = new ArrayList<>();

    // Bloqueio de stock (contagem cega): quantidades ocultas a não-administradores.
    private static final String MASK = "•••";
    private JLabel stockLockBanner;
    private ModernButton stockLockBtn;
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

    // Categorias
    DefaultTableModel categoriesModel;
    JTable categoriesTable;
    JTextField categorySearchField;
    java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categoriesList = new ArrayList<>();
    java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categoriesFiltered = new ArrayList<>();
    final java.util.Map<Long, Integer> categoryProductCounts = new java.util.HashMap<>();
    List<ProductDTO> catalogProducts = new ArrayList<>();

    public StockPanel(InventoryApiClient inventoryApiClient,
                       ComercialApiClient comercialApiClient,
                       StockTransferApiClient stockTransferApiClient,
                       InventoryCountApiClient inventoryCountApiClient,
                       ProductCategoryApiClient productCategoryApiClient) {
        this.inventoryApiClient = inventoryApiClient;
        this.comercialApiClient = comercialApiClient;
        this.stockTransferApiClient = stockTransferApiClient;
        this.inventoryCountApiClient = inventoryCountApiClient;
        this.productCategoryApiClient = productCategoryApiClient;
        this.transferActions = new StockTransferActions(this);
        this.productActions = new StockProductActions(this);
        this.inventoryCountActions = new StockInventoryCountActions(this);
        this.warehousesPanel = new StockWarehousesPanel(this);
        this.categoriesPanel = new StockCategoriesPanel(this);
        this.alertsPanel = new StockAlertsPanel(this);
        this.batchesPanel = new StockBatchesPanel(this);

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // TOP BAR: heading + globally-relevant catalogue buttons
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        topBar.add(UIHelper.createHeading("Controle de Stock & Armazéns"), BorderLayout.WEST);

        ModernButton newProductBtn = UIHelper.createSuccessButton("Cadastrar Produto");
        newProductBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton editProductBtn = UIHelper.createSecondaryButton("Editar Produto");
        editProductBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton newWarehouseBtn = UIHelper.createPrimaryButton("Criar Armazém");
        newWarehouseBtn.setIcon(UIHelper.icon("fas-warehouse", 14));
        stockLockBtn = UIHelper.createSecondaryButton("Trancar Stock");
        stockLockBtn.setIcon(UIHelper.icon("fas-lock", 14));
        stockLockBtn.setVisible(isAdmin()); // só ADMIN tranca/destranca
        ModernButton physicalInventoryBtn = UIHelper.createPrimaryButton("Inventário Físico");
        physicalInventoryBtn.setIcon(UIHelper.icon("fas-clipboard-check", 14));
        physicalInventoryBtn.addActionListener(e -> openPhysicalInventoryDialog());
        ModernButton labelsBtn = UIHelper.createSecondaryButton("Etiquetas");
        labelsBtn.setIcon(UIHelper.icon("fas-barcode", 14));
        labelsBtn.addActionListener(e -> openLabelDialog());
        JPanel catalogueGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        catalogueGroup.setOpaque(false);
        catalogueGroup.add(stockLockBtn);
        catalogueGroup.add(labelsBtn);
        catalogueGroup.add(physicalInventoryBtn);
        catalogueGroup.add(newProductBtn);
        catalogueGroup.add(editProductBtn);
        catalogueGroup.add(newWarehouseBtn);
        topBar.add(catalogueGroup, BorderLayout.EAST);

        stockLockBanner = new JLabel(" ");
        stockLockBanner.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        stockLockBanner.setBorder(new EmptyBorder(8, 2, 0, 0));
        stockLockBanner.setVisible(false);

        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setOpaque(false);
        northWrap.add(topBar, BorderLayout.NORTH);
        northWrap.add(stockLockBanner, BorderLayout.SOUTH);
        add(northWrap, BorderLayout.NORTH);

        // TABS: Níveis | Movimentos | Transferências
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabs);

        tabs.addTab("Níveis de Stock",               UIHelper.icon("fas-boxes", 16, UIHelper.TEXT_LIGHT),         buildLevelsTab());
        tabs.addTab("Alertas",                       UIHelper.icon("fas-exclamation-triangle", 16, UIHelper.TEXT_LIGHT), buildAlertsTab());
        tabs.addTab("Lotes & Validades",             UIHelper.icon("fas-calendar-times", 16, UIHelper.TEXT_LIGHT),buildBatchesTab());
        tabs.addTab("Movimentos & Rastreabilidade",  UIHelper.icon("fas-clipboard-list", 16, UIHelper.TEXT_LIGHT),buildMovementsTab());
        tabs.addTab("Transferências entre Armazéns", UIHelper.icon("fas-truck", 16, UIHelper.TEXT_LIGHT),         buildTransfersTab());
        tabs.addTab("Gestão de Armazéns",            UIHelper.icon("fas-warehouse", 16, UIHelper.TEXT_LIGHT),     buildWarehousesTab());
        tabs.addTab("Categorias",                    UIHelper.icon("fas-tags", 16, UIHelper.TEXT_LIGHT),         buildCategoriesTab());

        add(tabs, BorderLayout.CENTER);

        // GLOBAL ACTION LISTENERS
        newProductBtn.addActionListener(e -> createProductDialog());
        editProductBtn.addActionListener(e -> editProductDialog(selectedStockProductId()));
        newWarehouseBtn.addActionListener(e -> createWarehouseDialogV2());
        stockLockBtn.addActionListener(e -> toggleStockLock());

        onPanelSelected();
    }

    private JTextField stockSearchField;
    private JComboBox<String> stockStatusCombo;
    private JComboBox<String> stockCategoryCombo;

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
    JLabel batchesSummary;
    List<com.phcpro.modules.inventory.dto.ProductBatchDTO> batchesList = new ArrayList<>();

    /** Horizonte (dias) considerado "a vencer" no resumo de validades. */
    static final int EXPIRY_SOON_DAYS = 30;

    /** Aba de alertas: produtos esgotados e lotes expirados / a expirar. Cada lista na sua sub-aba. */
    private JPanel buildAlertsTab() { return alertsPanel.buildPanel(); }

    private void loadAlerts() { alertsPanel.refresh(); }

    private JPanel buildBatchesTab() { return batchesPanel.buildPanel(); }

    private void loadBatches() { batchesPanel.refresh(); }

    private JPanel buildLevelsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Níveis de Stock"), BorderLayout.WEST);
        ModernButton printInventoryBtn = UIHelper.createSecondaryButton("Imprimir Inventário");
        printInventoryBtn.setIcon(UIHelper.icon("fas-print", 14));
        printInventoryBtn.addActionListener(e -> printInventoryReport());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(printInventoryBtn);
        header.add(actions, BorderLayout.EAST);

        // Multi-row filter bar
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        // Warehouse
        warehouseFilterCombo = new JComboBox<>();
        UIHelper.styleComboBox(warehouseFilterCombo);
        warehouseFilterCombo.setPreferredSize(new Dimension(240, 35));
        warehouseFilterCombo.addActionListener(e -> filterStocks());
        g.gridx = 0; g.weightx = 0; filters.add(filterLabel("Armazém"), g);

        // Stock status
        stockStatusCombo = new JComboBox<>(new String[]{
                "Todos os artigos",
                "Em stock",
                "Stock baixo (< 5)",
                "Sem stock"
        });
        UIHelper.styleComboBox(stockStatusCombo);
        stockStatusCombo.setPreferredSize(new Dimension(200, 35));
        stockStatusCombo.addActionListener(e -> filterStocks());
        g.gridx = 1; g.weightx = 0; filters.add(filterLabel("Estado"), g);

        // Categoria
        stockCategoryCombo = new JComboBox<>();
        stockCategoryCombo.addItem("Todas as categorias");
        UIHelper.styleComboBox(stockCategoryCombo);
        stockCategoryCombo.setPreferredSize(new Dimension(200, 35));
        stockCategoryCombo.addActionListener(e -> filterStocks());
        g.gridx = 2; g.weightx = 0; filters.add(filterLabel("Categoria"), g);

        // Search
        stockSearchField = new SearchField("Pesquisar por SKU ou nome…");
        stockSearchField.getDocument().addDocumentListener(simpleDocumentListener(this::filterStocks));
        g.gridx = 3; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        filters.add(filterLabel("Pesquisa"), g);

        // Row 1: controls aligned below their labels
        g.gridy = 1; g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; filters.add(warehouseFilterCombo, g);
        g.gridx = 1; g.weightx = 0; filters.add(stockStatusCombo, g);
        g.gridx = 2; g.weightx = 0; filters.add(stockCategoryCombo, g);
        g.gridx = 3; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        filters.add(stockSearchField, g);

        JPanel topStack = new JPanel(new BorderLayout(0, 10));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(filters, BorderLayout.CENTER);
        tab.add(topStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] stockCols = {"Código de Barras", "Referência", "Nome do Produto", "Qtd Unidades", "Qtd Caixas", "Preço", "Estado"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        UIHelper.styleTable(stockTable);
        stockTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        stockTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        // Duplo-clique numa linha → editar esse produto directamente.
        stockTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Long id = selectedStockProductId();
                    if (id != null) editProductDialog(id);
                }
            }
        });
        JScrollPane stockScroll = new JScrollPane(stockTable);
        UIHelper.styleScrollPane(stockScroll);
        card.add(stockScroll, BorderLayout.CENTER);
        card.add(com.phcpro.gui.components.TableFooter.install(stockTable), BorderLayout.SOUTH);
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
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Rastreabilidade e Movimentos (Lotes & Séries)"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(adjustmentBtn);
        header.add(headerActions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] movCols = {"Data", "Artigo", "Armazém", "Qtd Movimentada", "Tipo Mov.", "Nº Lote", "Nº Série", "Descrição"};
        movementsModel = new DefaultTableModel(movCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movementsTable = new JTable(movementsModel);
        UIHelper.styleTable(movementsTable);
        movementsTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        JScrollPane movScroll = new JScrollPane(movementsTable);
        UIHelper.styleScrollPane(movScroll);
        JTextField mvSearch = TableFilter.searchField("Artigo, lote, série ou descrição…");
        JComboBox<String> mvTipo = TableFilter.combo("Todos os tipos",
                "PURCHASE", "ENTRY", "SALE", "TRANSFER", "ADJUSTMENT", "RETURN");
        JComboBox<String> mvPeriodo = TableFilter.periodCombo();
        TableFilter.install(movementsTable, mvSearch,
                java.util.List.of(new TableFilter.ColumnFilter(mvTipo, 4)),
                java.util.List.of(new TableFilter.PeriodFilter(mvPeriodo, 0)));
        JPanel mvBar = TableFilter.bar(mvSearch, TableFilter.label("Tipo:"), mvTipo,
                TableFilter.label("Data:", "fas-calendar-alt"), mvPeriodo);
        mvBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(mvBar, BorderLayout.NORTH);
        card.add(movScroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildTransfersTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton transferBtn = UIHelper.createPrimaryButton("Nova Transferência");
        transferBtn.setIcon(UIHelper.icon("fas-truck", 14));
        ModernButton approveTransferBtn = UIHelper.createSecondaryButton("Aprovar");
        approveTransferBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton rejectTransferBtn = UIHelper.createSecondaryButton("Rejeitar");
        rejectTransferBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton printTransferBtn = UIHelper.createSecondaryButton("Imprimir Guia");
        printTransferBtn.setIcon(UIHelper.icon("fas-print", 14));
        transferBtn.addActionListener(e -> createTransferDialog());
        approveTransferBtn.addActionListener(e -> approveSelectedTransfer());
        rejectTransferBtn.addActionListener(e -> rejectSelectedTransfer());
        printTransferBtn.addActionListener(e -> printSelectedTransfer());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Transferências entre Armazéns"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(approveTransferBtn);
        headerActions.add(rejectTransferBtn);
        headerActions.add(printTransferBtn);
        headerActions.add(transferBtn);
        header.add(headerActions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] transferCols = {"Nº Guia", "Data", "Origem", "Destino", "Linhas", "Estado", "Responsável"};
        transferModel = new DefaultTableModel(transferCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        transferTable = new JTable(transferModel);
        UIHelper.styleTable(transferTable);
        transferTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        JScrollPane transferScroll = new JScrollPane(transferTable);
        UIHelper.styleScrollPane(transferScroll);
        JTextField trSearch = TableFilter.searchField("Nº guia, origem, destino ou responsável…");
        JComboBox<String> trEstado = TableFilter.combo("Todos os estados",
                "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
        JComboBox<String> trPeriodo = TableFilter.periodCombo();
        TableFilter.install(transferTable, trSearch,
                java.util.List.of(new TableFilter.ColumnFilter(trEstado, 5)),
                java.util.List.of(new TableFilter.PeriodFilter(trPeriodo, 1)));
        JPanel trBar = TableFilter.bar(trSearch, TableFilter.label("Estado:"), trEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), trPeriodo);
        trBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(trBar, BorderLayout.NORTH);
        card.add(transferScroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
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
        return "ADMIN".equalsIgnoreCase(CurrentUserContext.getRole());
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
        if (stockLockBtn != null) {
            stockLockBtn.setVisible(isAdmin());
            stockLockBtn.setText(locked ? "Destrancar Stock" : "Trancar Stock");
            stockLockBtn.setIcon(UIHelper.icon(locked ? "fas-lock-open" : "fas-lock", 14));
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
        transfersList = loaded;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (StockTransferDTO t : transfersList) {
            transferModel.addRow(new Object[]{
                    t.transferNumber(),
                    t.transferDate().format(dtf),
                    t.originWarehouseName(),
                    t.destinationWarehouseName(),
                    t.lines() == null ? 0 : t.lines().size(),
                    t.status(),
                    t.responsible() == null ? "-" : t.responsible()
            });
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
        warehouseFilterCombo.addItem("--- Todos os Armazéns ---");

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
        filterStocks();
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

        for (StockDTO s : stocksList) {
            if (filterWarehouseId != null && !filterWarehouseId.equals(s.warehouseId())) continue;

            if (wantCategory != null) {
                String cat = s.categoryName() == null ? "" : s.categoryName();
                if (!wantCategory.equalsIgnoreCase(cat)) continue;
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

            stockModel.addRow(new Object[]{
                    s.barcode() == null ? "—" : s.barcode(),
                    s.reference() == null ? s.sku() : s.reference(),
                    s.productName(),
                    hide ? MASK : String.format("%,.3f", qty),
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
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getMovementsByCompany(companyId), this::applyMovements,
                error -> showStockLoadError("movimentos", error));
    }

    private void applyMovements(List<StockMovementDTO> movements) {
        movementsModel.setRowCount(0);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (StockMovementDTO m : movements) {
            BigDecimal qty = m.quantity();
            String formattedQty = (qty.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + String.format("%,.3f", qty);
            movementsModel.addRow(new Object[]{
                    m.movementDate().format(dtf),
                    m.productName(),
                    m.warehouseName(),
                    formattedQty,
                    m.movementType(),
                    m.batchNumber() != null ? m.batchNumber() : "-",
                    m.serialNumber() != null ? m.serialNumber() : "-",
                    m.description()
            });
        }
    }


    private JPanel buildWarehousesTab() { return warehousesPanel.buildPanel(); }

    private void loadWarehousesManagement() { warehousesPanel.refresh(); }

    private void createWarehouseDialogV2() { warehousesPanel.createWarehouseDialogV2(); }

    private void openPhysicalInventoryDialog() { inventoryCountActions.openPhysicalInventoryDialog(); }

    private void openLabelDialog() {
        java.util.List<ProductDTO> products = new ArrayList<>(catalogProducts);
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre produtos primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel model = new DefaultTableModel(new String[]{"SKU", "Artigo", "Código", "Preço"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        final java.util.List<Long> rowIds = new ArrayList<>();
        for (ProductDTO p : products) {
            rowIds.add(p.id());
            String code = p.barcode() != null && !p.barcode().isBlank() ? p.barcode()
                    : (p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku());
            model.addRow(new Object[]{ p.sku(), p.name(), code,
                    p.unitPrice() == null ? "" : String.format("%,.2f MT", p.unitPrice()) });
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty("noRowInspector", Boolean.TRUE);
        table.putClientProperty("noTableFooter", Boolean.TRUE);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sc = new JScrollPane(table);
        UIHelper.styleScrollPane(sc);
        sc.setPreferredSize(new Dimension(620, 360));

        JTextField copiesField = new JTextField("1", 4);
        UIHelper.styleTextField(copiesField);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JLabel hint = new JLabel("Selecione os produtos (Ctrl/Shift para vários)");
        hint.setForeground(UIHelper.TEXT_MUTED);
        JLabel copiesLbl = new JLabel("   Cópias por etiqueta:");
        copiesLbl.setForeground(UIHelper.TEXT_MUTED);
        top.add(hint);
        top.add(copiesLbl);
        top.add(copiesField);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(top, BorderLayout.NORTH);
        content.add(sc, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Etiquetas de Código de Barras",
                "fas-barcode", "Imprime uma folha A4 com o código de barras, nome e preço", content)
                .setConfirmButton("Imprimir Selecionadas", "fas-print");
        dlg.setOnSave(() -> {
            int[] sel = table.getSelectedRows();
            if (sel.length == 0) throw new IllegalArgumentException("Selecione pelo menos um produto.");
            int copies;
            try {
                copies = Integer.parseInt(copiesField.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Nº de cópias inválido.");
            }
            if (copies < 1 || copies > 200) throw new IllegalArgumentException("Cópias deve estar entre 1 e 200.");
            java.util.List<Long> ids = new ArrayList<>();
            for (int r : sel) ids.add(rowIds.get(table.convertRowIndexToModel(r)));
            Long companyId = CurrentUserContext.getCurrentCompanyId();
            UIHelper.runWithProgress(this, "A gerar etiquetas…",
                    () -> inventoryApiClient.renderProductLabels(companyId, ids, copies),
                    pdf -> com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "etiquetas"),
                    err -> JOptionPane.showMessageDialog(this, "Erro: " + err.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
        });
        dlg.showDialog();
    }

    private void createAdjustmentDialog() {
        List<ProductDTO> products = new ArrayList<>(catalogProducts);
        if (products.isEmpty() || warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Necessário cadastrar produtos e armazéns primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> prodCombo = new JComboBox<>();
        JComboBox<String> whCombo = new JComboBox<>();
        JTextField countedField = new JTextField();
        JTextField reasonField = new JTextField("Contagem física de inventário");

        UIHelper.styleComboBox(prodCombo);
        UIHelper.styleComboBox(whCombo);
        UIHelper.styleTextField(countedField);
        UIHelper.styleTextField(reasonField);

        for (ProductDTO p : products) {
            prodCombo.addItem(p.name());
        }
        for (WarehouseDTO w : warehousesList) {
            whCombo.addItem(w.name());
        }

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Selecionar Artigo:", prodCombo,
                "Selecionar Armazém:", whCombo,
                "Quantidade contada:", countedField,
                "Motivo / Descrição:", reasonField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Contagem / Ajuste de Stock", "fas-clipboard-list", "Acerte a quantidade física em stock", dialogPanel).showDialog();
        if (confirmed) {
            int prodIdx = prodCombo.getSelectedIndex();
            int whIdx = whCombo.getSelectedIndex();

            if (prodIdx < 0 || whIdx < 0) return;

            ProductDTO selectedProductDTO = products.get(prodIdx);
            WarehouseDTO selectedWarehouse = warehousesList.get(whIdx);

            try {
                BigDecimal counted = new BigDecimal(countedField.getText().trim().replace(",", "."));
                String reason = reasonField.getText().trim();
                if (reason.isBlank()) {
                    JOptionPane.showMessageDialog(this, "Indique o motivo do ajuste.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                CreateStockAdjustmentRequest request = new CreateStockAdjustmentRequest(
                        CurrentUserContext.getCurrentCompanyId(),
                        selectedProductDTO.id(),
                        selectedWarehouse.id(),
                        counted,
                        reason);
                UIHelper.runWithProgress(this, "A ajustar stock…", () -> inventoryApiClient.adjustStock(request), ignored -> {
                    JOptionPane.showMessageDialog(this, "Contagem de stock registada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    onPanelSelected();
                }, this::showStockError);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Diálogo profissional de entrada de lote com validade. Se {@code preselected} for indicado,
     * pré-selecciona o produto (usado depois de cadastrar um produto novo).
     */
    /** Inteiro ≥ 0 a partir de texto livre; vazio/inválido → 0 (para os campos de caixas). */
    void createBatchEntryDialog(ProductDTO product) { productActions.createBatchEntryDialog(product); }

    private void createProductDialog() { productActions.createProductDialog(); }

    private void editProductDialog(Long productId) { productActions.editProductDialog(productId); }

    private void createTransferDialog() { transferActions.createTransferDialog(); }

    private void approveSelectedTransfer() { transferActions.approveSelectedTransfer(); }

    private void rejectSelectedTransfer() { transferActions.rejectSelectedTransfer(); }

    private void printSelectedTransfer() { transferActions.printSelectedTransfer(); }

    private void printInventoryReport() {
        if (stockTable != null && stockTable.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nada para imprimir.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long warehouseId = null;
        String fileSuffix = "todos";
        if (warehouseFilterCombo != null) {
            int idx = warehouseFilterCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < warehousesList.size()) {
                WarehouseDTO selectedWarehouse = warehousesList.get(idx - 1);
                warehouseId = selectedWarehouse.id();
                fileSuffix = selectedWarehouse.name()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
                if (fileSuffix.isBlank()) {
                    fileSuffix = "armazem-" + selectedWarehouse.id();
                }
            }
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Long selectedWarehouseId = warehouseId;
        String suffix = fileSuffix;
        UIHelper.runWithProgress(this, "A gerar inventário em PDF…",
                () -> inventoryApiClient.renderInventoryReport(companyId, selectedWarehouseId),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "inventario-stock-" + suffix),
                this::showStockError);
    }
    void showStockLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    void showStockError(Throwable error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private record StockAlerts(List<StockAlertDTO> outOfStock, List<ProductBatchDTO> expiring) {}

    private record CategoryData(
            List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories,
            List<ProductDTO> products) {}
}
