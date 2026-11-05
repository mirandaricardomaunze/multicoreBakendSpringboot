package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Centro de Gestão de Stock e Armazéns.
 * Orquestrador desacoplado alinhado com o Design System Multicore e rastreabilidade total.
 */
public class StockPanel extends JPanel {
    final InventoryApiClient inventoryApiClient;
    final PrintApiClient printApiClient;
    final ComercialApiClient comercialApiClient;
    final StockTransferApiClient stockTransferApiClient;
    final InventoryCountApiClient inventoryCountApiClient;
    final ProductCategoryApiClient productCategoryApiClient;
    final StockWasteApiClient stockWasteApiClient;
    final InventoryPhysicalCountingApiClient physicalCountingApiClient;
    private final StockTransferActions transferActions;
    private final StockProductActions productActions;
    private final StockInventoryCountActions inventoryCountActions;
    final StockWarehousesPanel warehousesPanel;
    private final StockCategoriesPanel categoriesPanel;
    private StockSideScreen warehousesScreen, categoriesScreen; // cadastros com ecrã próprio no menu lateral
    private final StockAlertsPanel alertsPanel;
    private final StockBatchesPanel batchesPanel;
    private final StockWastePanel wastePanel;
    private final mz.multicore.erp.gui.inventory.PhysicalInventoryPanel physicalInventoryPanel;
    private final StockLockCoordinator lockCoordinator;
    private final StockMovementsCoordinator movementsCoordinator;
    private final StockWarehouseCoordinator warehouseCoordinator;
    private JTabbedPane stockTabs;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    DefaultTableModel transferModel;
    JTable transferTable;
    List<StockTransferDTO> transfersList = new ArrayList<>();
    CardLayout transferPagesLayout;
    JPanel transferPages;
    DocumentEditorHost transferEditorHost;
    StockTransferEditorForm transferEditorForm;
    final JLabel kpiTransfersSeparation = new JLabel("0"), kpiTransfersPending = new JLabel("0");
    final JLabel kpiTransfersInTransit = new JLabel("0"), kpiTransfersCompleted = new JLabel("0");
    final JLabel kpiTransfersTotalValue = new JLabel("0,00 MT");
    JComboBox<String> warehouseFilterCombo, stockStatusCombo, stockBusinessUnitCombo, stockCategoryCombo;
    JTextField stockSearchField;
    List<WarehouseDTO> warehousesList = new ArrayList<>();
    DefaultTableModel stockModel;
    JTable stockTable;
    List<StockDTO> stocksList = new ArrayList<>();
    final List<Long> stockRowProductIds = new ArrayList<>();
    DefaultTableModel movementsModel;
    JTable movementsTable;
    TablePager movementsPager;
    JComboBox<String> movementProductFilter, movementBusinessUnitFilter, transferBusinessUnitCombo;
    DefaultTableModel categoriesModel;
    JTable categoriesTable;
    JTextField categorySearchField;
    List<mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO> categoriesList = new ArrayList<>(), categoriesFiltered = new ArrayList<>();
    final java.util.Map<Long, Integer> categoryProductCounts = new java.util.HashMap<>();
    List<ProductDTO> catalogProducts = new ArrayList<>();
    DefaultTableModel warehousesModel, alertsOutModel, alertsExpModel, batchesModel;
    JTable warehousesTable, alertsOutTable, alertsExpTable, batchesTable;
    List<WarehouseDTO> warehousesFullList = new ArrayList<>();
    JTextField batchSearchField;
    JComboBox<String> batchExpirationCombo, batchWarehouseCombo, batchBusinessUnitCombo;
    JLabel alertsSummary, batchesSummary;
    List<ProductBatchDTO> batchesList = new ArrayList<>();
    static final int EXPIRY_SOON_DAYS = 30;
    public StockPanel(InventoryApiClient inventoryApiClient, ComercialApiClient comercialApiClient,
                      StockTransferApiClient stockTransferApiClient, InventoryCountApiClient inventoryCountApiClient,
                      ProductCategoryApiClient productCategoryApiClient, PrintApiClient printApiClient,
                      StockWasteApiClient stockWasteApiClient) {
        this(inventoryApiClient, comercialApiClient, stockTransferApiClient, inventoryCountApiClient,
                productCategoryApiClient, printApiClient, stockWasteApiClient, null);
    }

    public StockPanel(InventoryApiClient inventoryApiClient, ComercialApiClient comercialApiClient,
                      StockTransferApiClient stockTransferApiClient, InventoryCountApiClient inventoryCountApiClient,
                      ProductCategoryApiClient productCategoryApiClient, PrintApiClient printApiClient,
                      StockWasteApiClient stockWasteApiClient, InventoryPhysicalCountingApiClient physicalCountingApiClient) {
        this.inventoryApiClient = inventoryApiClient; this.printApiClient = printApiClient;
        this.comercialApiClient = comercialApiClient; this.stockTransferApiClient = stockTransferApiClient;
        this.inventoryCountApiClient = inventoryCountApiClient; this.productCategoryApiClient = productCategoryApiClient;
        this.stockWasteApiClient = stockWasteApiClient; this.physicalCountingApiClient = physicalCountingApiClient;
        this.physicalInventoryPanel = physicalCountingApiClient != null ? new mz.multicore.erp.gui.inventory.PhysicalInventoryPanel(physicalCountingApiClient) : null;

        this.transferActions = new StockTransferActions(this);
        this.productActions = new StockProductActions(this);
        this.inventoryCountActions = new StockInventoryCountActions(this);
        this.warehousesPanel = new StockWarehousesPanel(this);
        this.categoriesPanel = new StockCategoriesPanel(this);
        this.alertsPanel = new StockAlertsPanel(this);
        this.batchesPanel = new StockBatchesPanel(this);
        this.wastePanel = new StockWastePanel(stockWasteApiClient, inventoryApiClient, comercialApiClient);
        this.lockCoordinator = new StockLockCoordinator(this);
        this.movementsCoordinator = new StockMovementsCoordinator(this);
        this.warehouseCoordinator = new StockWarehouseCoordinator(this);

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(UIHelper.createHeading("Controle de Stock"), BorderLayout.WEST);

        ModernButton newProductBtn = UIHelper.createSuccessButton("Registar Produto");
        newProductBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newProductBtn.addActionListener(e -> createProductDialog());

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Inventário Físico", UIHelper.icon("fas-clipboard-check", 14, UIHelper.APPROVED_GREEN), this::openPhysicalInventoryDialog)
                .addAction("Trancar Stock", UIHelper.icon("fas-lock", 14, UIHelper.PENDING_YELLOW), this::toggleStockLock)
                .addAction("Ficha do Artigo", UIHelper.icon("fas-id-card", 14, UIHelper.ACCENT_BLUE), () -> showProductDetailDialog(selectedStockProductId()))
                .addAction("Editar Produto", UIHelper.icon("fas-edit", 14, UIHelper.PENDING_YELLOW), () -> editProductDialog(selectedStockProductId()))
                .addAction("Etiquetas", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT), this::openLabelDialog);

        ModernButton refreshAllBtn = UIHelper.createRefreshButton(() -> onPanelSelected());
        refreshAllBtn.setToolTipText("Actualizar dados de stock e armazéns");
        topBar.add(UIHelper.actionsBar(refreshAllBtn, moreBtn, newProductBtn), BorderLayout.EAST);
        topBar.setVisible(false);

        JLabel stockLockBanner = new JLabel(" ");
        lockCoordinator.initControls(moreBtn, null, stockLockBanner);

        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setOpaque(false);
        northWrap.add(topBar, BorderLayout.NORTH);
        northWrap.add(stockLockBanner, BorderLayout.SOUTH);
        JPanel northStack = new JPanel();
        northStack.setOpaque(false);
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(northWrap);
        northStack.add(feedback);
        add(northStack, BorderLayout.NORTH);

        stockTabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(stockTabs);
        stockTabs.addTab("Níveis de Stock", UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK), buildLevelsTab());
        stockTabs.addTab("Alertas", UIHelper.icon("fas-exclamation-triangle", 16, UIHelper.PENDING_YELLOW), alertsPanel.buildPanel());
        stockTabs.addTab("Lotes & Validades", UIHelper.icon("fas-calendar-times", 16, UIHelper.ACCENT_ORANGE), batchesPanel.buildPanel());
        stockTabs.addTab("Movimentos & Rastreabilidade", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), buildMovementsTab());
        stockTabs.addTab("Transferências entre Armazéns", UIHelper.icon("fas-truck", 16, UIHelper.ACCENT), buildTransfersTab());
        warehousesScreen = new StockSideScreen(warehousesPanel.buildPanel(), () -> { warehouseCoordinator.loadWarehouses(); warehouseCoordinator.loadStocks(); warehousesPanel.refresh(); });
        categoriesScreen = new StockSideScreen(categoriesPanel.buildPanel(), categoriesPanel::refresh);
        stockTabs.addTab("Quebras & Desperdício", UIHelper.icon("fas-trash-alt", 16, UIHelper.REJECTED_RED), wastePanel);
        if (physicalInventoryPanel != null) {
            stockTabs.addTab("Inventário Físico", UIHelper.icon("fas-clipboard-check", 16, UIHelper.APPROVED_GREEN), physicalInventoryPanel);
        }
        stockTabs.addChangeListener(e -> { if (stockTabs.getSelectedComponent() == wastePanel) wastePanel.reload(); });
        add(stockTabs, BorderLayout.CENTER);
    }

    private JPanel buildLevelsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ActionMenuButton printMenu = UIHelper.createActionMenuButton("Imprimir")
                .addAction("Inventário de Stock", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), this::printInventoryReport)
                .addAction("Livro Valorizado (PGC-NIRF)", UIHelper.icon("fas-book", 14, UIHelper.APPROVED_GREEN), this::printValuedInventoryBook);
        printMenu.setToolTipText("Relatórios e mapas de existências");

        ActionMenuButton levelsMoreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Ficha do Artigo", UIHelper.icon("fas-id-card", 14, UIHelper.ACCENT_BLUE), () -> showProductDetailDialog(selectedStockProductId()))
                .addAction("Editar Produto", UIHelper.icon("fas-edit", 14, UIHelper.PENDING_YELLOW), () -> editProductDialog(selectedStockProductId()))
                .addAction("Modo Coletor (Auditoria)", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_BLUE), this::openInventoryBarcodeCollector)
                .addAction("Imprimir Etiquetas", UIHelper.icon("fas-tags", 14, UIHelper.APPROVED_GREEN), this::openLabelDialog)
                .addAction("Trancar Stock", UIHelper.icon("fas-lock", 14, UIHelper.PENDING_YELLOW), this::toggleStockLock);

        ModernButton cardNewProductBtn = UIHelper.createSuccessButton("Registar Produto");
        cardNewProductBtn.setIcon(UIHelper.icon("fas-plus", 14));
        cardNewProductBtn.addActionListener(e -> createProductDialog());

        ModernButton refreshLevelsBtn = UIHelper.createRefreshButton(this::onPanelSelected);
        refreshLevelsBtn.setToolTipText("Actualizar saldos de stock e armazéns");

        warehouseFilterCombo = TableFilter.combo("Todos os armazéns");
        warehouseFilterCombo.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        warehouseFilterCombo.addActionListener(e -> StockLevelsFilter.filterStocks(this));
        stockStatusCombo = TableFilter.combo("Todos os estados", "Disponível", "Stock baixo (< 5)", "Sem stock", "Perecíveis", "Sem validade");
        stockStatusCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        stockStatusCombo.addActionListener(e -> StockLevelsFilter.filterStocks(this));
        stockBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        stockBusinessUnitCombo.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        stockBusinessUnitCombo.addActionListener(e -> StockLevelsFilter.filterStocks(this));
        stockCategoryCombo = TableFilter.combo("Todas as categorias");
        stockCategoryCombo.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        stockCategoryCombo.addActionListener(e -> StockLevelsFilter.filterStocks(this));
        stockSearchField = TableFilter.searchField("Pesquisar por SKU ou nome…");
        stockSearchField.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(stockSearchField, () -> StockLevelsFilter.filterStocks(this));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar artigo seleccionado (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar artigo");
        peekToggleBtn.setPreferredSize(new Dimension(34, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersLeft = TableFilter.bar(stockSearchField, TableFilter.label("Negócio:"), stockBusinessUnitCombo,
                TableFilter.label("Armazém:"), warehouseFilterCombo, TableFilter.label("Estado:"), stockStatusCombo, TableFilter.label("Categoria:"), stockCategoryCombo);
        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        filtersRow.add(filtersLeft, BorderLayout.CENTER);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Níveis de Stock", filtersRow, refreshLevelsBtn, printMenu, levelsMoreBtn, cardNewProductBtn), BorderLayout.NORTH);

        String[] stockCols = {"Código de Barras", "Referência", "Nome do Produto", "Negócio", "Qtd Unidades", "Qtd Caixas", "Preço", "Estado", "Validade"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        UIHelper.styleTable(stockTable);
        stockTable.setAutoCreateRowSorter(true);
        stockTable.putClientProperty("noRowInspector", Boolean.TRUE);
        stockTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        stockTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        stockTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { Long id = selectedStockProductId(); if (id != null) showProductDetailDialog(id); }
            }
        });
        UIHelper.ensureHeadersFit(stockTable);

        TableQuickPeekController peekController = TableQuickPeekController.install(stockTable, card, (peek, mr) -> {
            if (mr < 0 || mr >= stockModel.getRowCount()) return;
            peek.setTitle(String.valueOf(stockModel.getValueAt(mr, 2)));
            peek.setSubtitle("SKU/Ref: " + stockModel.getValueAt(mr, 1));
            peek.setItems(List.of(
                    new QuickPeekPanel.PeekItem("Código de Barras", String.valueOf(stockModel.getValueAt(mr, 0)), false), new QuickPeekPanel.PeekItem("Referência", String.valueOf(stockModel.getValueAt(mr, 1)), false),
                    new QuickPeekPanel.PeekItem("Linha de Negócio", String.valueOf(stockModel.getValueAt(mr, 3)), true), new QuickPeekPanel.PeekItem("Qtd Unidades", String.valueOf(stockModel.getValueAt(mr, 4)), true),
                    new QuickPeekPanel.PeekItem("Qtd Embalagens", String.valueOf(stockModel.getValueAt(mr, 5)), false), new QuickPeekPanel.PeekItem("Preço de Venda", String.valueOf(stockModel.getValueAt(mr, 6)), false),
                    new QuickPeekPanel.PeekItem("Estado", String.valueOf(stockModel.getValueAt(mr, 7)), false)));
            peek.getOpenFullButton().setText("Ficha Executiva");
            peek.setOnOpenFullAction(v -> { Long id = selectedStockProductId(); if (id != null) showProductDetailDialog(id); });
        });
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane stockScroll = new JScrollPane(stockTable);
        UIHelper.styleScrollPane(stockScroll);
        card.add(stockScroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(stockTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildMovementsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton adjustmentBtn = UIHelper.createPrimaryButton("Ajuste / Inventário");
        adjustmentBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        adjustmentBtn.addActionListener(e -> movementsCoordinator.createAdjustmentDialog());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] movCols = {"Data", "Artigo", "Armazém", "Qtd Movimentada", "Tipo Mov.", "Nº Lote", "Nº Série", "Descrição", "Negócio"};
        movementsModel = new DefaultTableModel(movCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        movementsTable = new JTable(movementsModel);
        movementsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(movementsTable);
        movementsTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        JScrollPane movScroll = new JScrollPane(movementsTable);
        UIHelper.styleScrollPane(movScroll);

        JTextField mvSearch = TableFilter.searchField("Lote, série ou descrição…");
        mvSearch.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> mvTipo = TableFilter.combo("Todos os tipos", "Compra", "Entrada", "Venda", "Transferência", "Ajuste", "Devolução", "Estorno");
        UIHelper.styleComboBox(mvTipo);
        mvTipo.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));
        movementProductFilter = TableFilter.combo("Todos os artigos");
        movementProductFilter.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        movementBusinessUnitFilter = TableFilter.combo("Todos os negócios");
        movementBusinessUnitFilter.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        DateField movementFrom = new DateField(null), movementTo = new DateField(null);
        movementFrom.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));
        movementTo.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));

        UIHelper.ensureHeadersFit(movementsTable);
        TableFilter.install(movementsTable, mvSearch,
                List.of(new TableFilter.ColumnFilter(movementProductFilter, 1), new TableFilter.ColumnFilter(mvTipo, 4), new TableFilter.ColumnFilter(movementBusinessUnitFilter, 8)),
                List.of(), List.of(new TableFilter.DateRangeFilter(movementFrom, movementTo, 0)));

        JPanel mvFilters = TableFilter.bar(mvSearch, TableFilter.label("Negócio:"), movementBusinessUnitFilter,
                TableFilter.label("Artigo:"), movementProductFilter, TableFilter.label("Tipo:"), mvTipo, TableFilter.label("De:"), movementFrom, TableFilter.label("Até:"), movementTo);
        mvFilters.setBorder(new EmptyBorder(0, 0, 10, 0));

        ModernButton refreshMovBtn = UIHelper.createRefreshButton(movementsCoordinator::loadMovements);
        ModernButton printMovBtn = UIHelper.createSecondaryButton("Imprimir movimentos");
        printMovBtn.setIcon(UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE));
        printMovBtn.addActionListener(e -> movementsCoordinator.printFilteredMovements(movementProductFilter, movementFrom, movementTo));

        card.add(UIHelper.tableCardTop("Rastreabilidade e Movimentos (Lotes & Séries)", mvFilters, refreshMovBtn, printMovBtn, adjustmentBtn), BorderLayout.NORTH);
        card.add(movScroll, BorderLayout.CENTER);
        movementsPager = new TablePager(movementsCoordinator::loadMovementsPage);
        card.add(movementsPager, BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildTransfersTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ModernButton transferBtn = UIHelper.createSuccessButton("Nova Transferência");
        transferBtn.setIcon(UIHelper.icon("fas-truck", 14));
        transferBtn.addActionListener(e -> transferActions.openCreateEditor());

        ActionMenuButton transferMenu = UIHelper.createActionMenuButton("Fluxo da Transferência")
                .addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), () -> transferActions.openSelectedEditor())
                .addAction("Submeter", UIHelper.icon("fas-paper-plane", 14, UIHelper.PENDING_YELLOW), () -> transferActions.submitSelectedTransfer())
                .addAction("Aprovar", UIHelper.icon("fas-check", 14, UIHelper.APPROVED_GREEN), transferActions::approveSelectedTransfer)
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14, UIHelper.REJECTED_RED), transferActions::rejectSelectedTransfer)
                .addAction("Cancelar", UIHelper.icon("fas-ban", 14, UIHelper.REJECTED_RED), () -> transferActions.cancelSelectedTransfer());

        ActionMenuButton transferDocuments = UIHelper.createActionMenuButton("Documentos")
                .addAction("Guia de Separação", UIHelper.icon("fas-clipboard-list", 14, UIHelper.ACCENT_BLUE), () -> transferActions.printPickingSheetForSelected())
                .addAction("Concluir Separação", UIHelper.icon("fas-tasks", 14, UIHelper.APPROVED_GREEN), () -> transferActions.completeSeparationForSelected())
                .addAction("Imprimir Guia (3 Vias)", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), transferActions::printSelectedTransfer)
                .addAction("Confirmar Recepção", UIHelper.icon("fas-box-open", 14, UIHelper.APPROVED_GREEN), () -> transferActions.receiveSelectedTransfer())
                .addAction("Registar Encomenda", UIHelper.icon("fas-clipboard-check", 14, UIHelper.ACCENT_BLUE), () -> transferActions.recordOrderForSelectedTransfer());

        ModernButton refreshTransfersBtn = UIHelper.createRefreshButton(this::loadTransfers);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        String[] transferCols = {"Nº Guia", "Data", "Origem", "Destino", "Motorista", "Matrícula", "Linhas", "Estado", "Negócio"};
        transferModel = new DefaultTableModel(transferCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        transferTable = new JTable(transferModel);
        UIHelper.styleTable(transferTable);
        transferTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        JScrollPane transferScroll = new JScrollPane(transferTable);
        UIHelper.styleScrollPane(transferScroll);

        JTextField trSearch = TableFilter.searchField("Nº guia, armazém, motorista ou matrícula…");
        JComboBox<String> trEstado = TableFilter.combo("Todos os estados", "Rascunho", "Aguarda separação", "Em separação", "Separado", "Pendente de aprovação", "Aprovado", "Em trânsito", "Recebido", "Recebido c/ divergência", "Rejeitado", "Cancelado");
        UIHelper.styleComboBox(trEstado);
        trEstado.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> trPeriodo = TableFilter.periodCombo();
        UIHelper.styleComboBox(trPeriodo);
        trPeriodo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        transferBusinessUnitCombo = TableFilter.combo("Todos os negócios");

        UIHelper.ensureHeadersFit(transferTable);
        TableFilter.install(transferTable, trSearch,
                List.of(new TableFilter.ColumnFilter(trEstado, 7), new TableFilter.ColumnFilter(transferBusinessUnitCombo, 8)),
                List.of(new TableFilter.PeriodFilter(trPeriodo, 1)));

        JPanel trFilters = TableFilter.bar(trSearch, TableFilter.label("Negócio:"), transferBusinessUnitCombo, TableFilter.label("Estado:"), trEstado, TableFilter.label("Período:"), trPeriodo);
        trFilters.setBorder(new EmptyBorder(0, 0, 4, 0));
        card.add(UIHelper.tableCardTop("Transferências entre Armazéns", trFilters, refreshTransfersBtn, transferDocuments, transferMenu, transferBtn), BorderLayout.NORTH);
        card.add(transferScroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(transferTable), BorderLayout.SOUTH);

        JPanel kpiGrid = KpiCard.createGrid(5);
        kpiGrid.add(KpiCard.createCard("Aguarda Separação", kpiTransfersSeparation, "Picking pendente no armazém", "fas-dolly", UIHelper.PENDING_YELLOW));
        kpiGrid.add(KpiCard.createCard("Aguarda Decisão", kpiTransfersPending, "Guias prontas para aprovação", "fas-clock", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Em Trânsito", kpiTransfersInTransit, "Mercadoria em viagem entre armazéns", "fas-truck", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Concluídas (Mês)", kpiTransfersCompleted, "Transferências finalizadas no período", "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Valor Movimentado", kpiTransfersTotalValue, "Total valorizado em mercadoria", "fas-coins", UIHelper.MODULE_STOCK));

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

    public void showWarehouseTransfers() {
        if (stockTabs == null) return;
        for (int i = 0; i < stockTabs.getTabCount(); i++) {
            if (stockTabs.getTitleAt(i).startsWith("Transferências")) { stockTabs.setSelectedIndex(i); loadTransfers(); return; }
        }
    }

    public void showWasteManagement() { if (stockTabs != null && wastePanel != null) { stockTabs.setSelectedComponent(wastePanel); wastePanel.reload(); } }
    public void selectWasteTab() { showWasteManagement(); }

    public void onPanelSelected() {
        lockCoordinator.refreshStockLock();
        warehouseCoordinator.loadWarehouses();
        warehouseCoordinator.loadStocks();
        movementsCoordinator.loadMovements();
        loadTransfers();
        batchesPanel.refresh(); alertsPanel.refresh(); categoriesPanel.refresh(); warehousesPanel.refresh();
    }

    boolean stockHidden() { return lockCoordinator.isStockHidden(); }
    private void toggleStockLock() { lockCoordinator.toggleStockLock(); }
    private void loadTransfers() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> stockTransferApiClient.findByCompany(companyId), transferActions::applyTransfers,
                error -> showStockLoadError("transferências", error));
    }
    void loadStocks() { warehouseCoordinator.loadStocks(); }
    Long selectedStockProductId() { return warehouseCoordinator.selectedStockProductId(); }
    void openLabelDialog() { warehouseCoordinator.openLabelDialog(); }
    private void openInventoryBarcodeCollector() { warehouseCoordinator.openInventoryBarcodeCollector(); }
    private void printInventoryReport() { warehouseCoordinator.printInventoryReport(); }
    private void printValuedInventoryBook() { warehouseCoordinator.printValuedInventoryBook(); }
    private void openPhysicalInventoryDialog() {
        if (stockTabs != null && physicalInventoryPanel != null) stockTabs.setSelectedComponent(physicalInventoryPanel);
        else inventoryCountActions.openPhysicalInventoryDialog();
    }
    private void createProductDialog() { productActions.createProductDialog(); }
    void createBatchEntryDialog(ProductDTO product) { productActions.createBatchEntryDialog(product); }
    void saveTransferEditor() { transferActions.saveEditor(); }
    void editProductDialog(Long productId) { productActions.editProductDialog(productId); }
    void showProductDetailDialog(Long productId) { StockProductDetailDialog.showFor(this, productId); }
    void exportMovementsTable(String title, String baseName) { TableExportAction.export(this, printApiClient, movementsTable, title, baseName); }
    public StockSideScreen warehousesScreen() { return warehousesScreen; }  public StockSideScreen categoriesScreen() { return categoriesScreen; }
    private InlineFeedbackPanel activeFeedback() { return StockSideScreen.visibleFeedback(feedback, warehousesScreen, categoriesScreen); }
    void showStockLoadError(String area, Throwable error) { activeFeedback().show(FeedbackType.ERROR, "Não foi possível carregar " + area, error.getMessage(), "Tentar novamente", this::onPanelSelected); }
    void showStockError(Throwable error) { showStockNotice(FeedbackType.ERROR, "Não foi possível concluir a operação", error.getMessage()); }
    void showStockNotice(FeedbackType type, String title, String message) { activeFeedback().show(type, title, message, null, null); }
    void showStockSuccess(String message) { ToastManager.success(this, message); }
}
