package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.SearchField;
import com.phcpro.gui.components.TableFilter;
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

    private final InventoryApiClient inventoryApiClient;
    private final ComercialApiClient comercialApiClient;
    private final StockTransferApiClient stockTransferApiClient;
    private final InventoryCountApiClient inventoryCountApiClient;
    private final ProductCategoryApiClient productCategoryApiClient;

    // Transfer history
    private DefaultTableModel transferModel;
    private JTable transferTable;
    private List<StockTransferDTO> transfersList = new ArrayList<>();

    // Warehouses list
    private JComboBox<String> warehouseFilterCombo;
    private List<WarehouseDTO> warehousesList = new ArrayList<>();

    // Stock levels
    private DefaultTableModel stockModel;
    private JTable stockTable;
    private List<StockDTO> stocksList = new ArrayList<>();

    // Bloqueio de stock (contagem cega): quantidades ocultas a não-administradores.
    private static final String MASK = "•••";
    private JLabel stockLockBanner;
    private ModernButton stockLockBtn;

    // Gestão de armazéns
    private DefaultTableModel warehousesModel;
    private JTable warehousesTable;
    private List<WarehouseDTO> warehousesFullList = new ArrayList<>();
    // ID do produto por linha visível da tabela (paralelo às linhas, respeita filtros).
    private final java.util.List<Long> stockRowProductIds = new java.util.ArrayList<>();

    // Movements log
    private DefaultTableModel movementsModel;
    private JTable movementsTable;

    // Categorias
    private DefaultTableModel categoriesModel;
    private JTable categoriesTable;
    private JTextField categorySearchField;
    private java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categoriesList = new ArrayList<>();
    private java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categoriesFiltered = new ArrayList<>();
    private final java.util.Map<Long, Integer> categoryProductCounts = new java.util.HashMap<>();

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
    private DefaultTableModel alertsOutModel;
    private JTable alertsOutTable;
    private DefaultTableModel alertsExpModel;
    private JTable alertsExpTable;
    private JLabel alertsSummary;

    private DefaultTableModel batchesModel;
    private JTable batchesTable;
    private JTextField batchSearchField;
    private JComboBox<String> batchExpirationCombo;
    private JComboBox<String> batchWarehouseCombo;
    private JLabel batchesSummary;
    private List<com.phcpro.modules.inventory.dto.ProductBatchDTO> batchesList = new ArrayList<>();

    /** Horizonte (dias) considerado "a vencer" no resumo de validades. */
    private static final int EXPIRY_SOON_DAYS = 30;

    /** Aba de alertas: produtos esgotados e lotes expirados / a expirar. Cada lista na sua sub-aba. */
    private JPanel buildAlertsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Alertas de Stock"), BorderLayout.WEST);
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadAlerts());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        alertsSummary = new JLabel(" ");
        alertsSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        alertsSummary.setForeground(UIHelper.TEXT_MUTED);
        alertsSummary.setBorder(new EmptyBorder(2, 2, 0, 0));

        JPanel topStack = new JPanel(new BorderLayout(0, 8));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(alertsSummary, BorderLayout.SOUTH);
        tab.add(topStack, BorderLayout.NORTH);

        JTabbedPane sub = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(sub);

        // Sub-aba: produtos esgotados (com pesquisa)
        String[] outCols = {"Artigo (SKU)", "Nome do Artigo", "Stock"};
        alertsOutModel = new DefaultTableModel(outCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        alertsOutTable = new JTable(alertsOutModel);
        UIHelper.styleTable(alertsOutTable);
        ModernPanel outCard = new ModernPanel(16);
        outCard.setLayout(new BorderLayout());
        outCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane outScroll = new JScrollPane(alertsOutTable);
        UIHelper.styleScrollPane(outScroll);
        outCard.add(outScroll, BorderLayout.CENTER);
        JTextField outSearch = TableFilter.searchField("SKU ou nome…");
        TableFilter.install(alertsOutTable, outSearch);
        JPanel outWrap = new JPanel(new BorderLayout(0, 8));
        outWrap.setOpaque(false);
        outWrap.add(TableFilter.bar(outSearch), BorderLayout.NORTH);
        outWrap.add(outCard, BorderLayout.CENTER);
        sub.addTab("Esgotados", UIHelper.icon("fas-ban", 15, UIHelper.TEXT_LIGHT), outWrap);

        // Sub-aba: validades (expirados / a expirar) — com pesquisa + filtro de estado
        String[] expCols = {"SKU", "Nome do Artigo", "Nº Lote", "Armazém", "Validade", "Dias", "Qtd", "Estado"};
        alertsExpModel = new DefaultTableModel(expCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        alertsExpTable = new JTable(alertsExpModel);
        UIHelper.styleTable(alertsExpTable);
        // Vermelho para expirados, amarelo para a expirar (coluna Dias < 0 vs ≥ 0). Converte o índice
        // da vista para o modelo, porque o filtro instala um TableRowSorter.
        javax.swing.table.TableCellRenderer expBase = alertsExpTable.getDefaultRenderer(Object.class);
        alertsExpTable.setDefaultRenderer(Object.class, (t, v, sel, foc, row, col) -> {
            java.awt.Component c = expBase.getTableCellRendererComponent(t, v, sel, foc, row, col);
            int modelRow = row >= 0 ? alertsExpTable.convertRowIndexToModel(row) : -1;
            if (!sel && modelRow >= 0 && modelRow < alertsExpModel.getRowCount()) {
                Object d = alertsExpModel.getValueAt(modelRow, 5);
                long days = d instanceof Number n ? n.longValue() : 0;
                c.setForeground(days < 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            }
            return c;
        });
        ModernPanel expCard = new ModernPanel(16);
        expCard.setLayout(new BorderLayout());
        expCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane expScroll = new JScrollPane(alertsExpTable);
        UIHelper.styleScrollPane(expScroll);
        expCard.add(expScroll, BorderLayout.CENTER);
        JTextField expSearch = TableFilter.searchField("SKU, nome ou lote…");
        JComboBox<String> expEstado = TableFilter.combo("Todos", "Expirado", "A expirar");
        TableFilter.install(alertsExpTable, expSearch, new TableFilter.ColumnFilter(expEstado, 7));
        JPanel expWrap = new JPanel(new BorderLayout(0, 8));
        expWrap.setOpaque(false);
        expWrap.add(TableFilter.bar(expSearch, TableFilter.label("Estado:"), expEstado), BorderLayout.NORTH);
        expWrap.add(expCard, BorderLayout.CENTER);
        sub.addTab("Validade (expirados / a expirar)", UIHelper.icon("fas-calendar-times", 15, UIHelper.TEXT_LIGHT), expWrap);

        tab.add(sub, BorderLayout.CENTER);
        return tab;
    }

    /** Carrega os alertas: esgotados (saldo ≤ 0) e lotes expirados/a expirar em ≤ 30 dias (com stock). */
    private void loadAlerts() {
        if (alertsOutModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        boolean hide = stockHidden();

        var esgotados = inventoryApiClient.findOutOfStockProducts(companyId);
        alertsOutModel.setRowCount(0);
        for (var a : esgotados) {
            alertsOutModel.addRow(new Object[]{
                    a.sku(), a.name(),
                    hide ? MASK : (a.currentStock() == null ? "0" : a.currentStock().toPlainString())});
        }

        var expiring = inventoryApiClient.findExpiringBatches(companyId, 30);
        alertsExpModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        long expired = 0, soon = 0;
        for (var b : expiring) {
            long days = b.expirationDate() == null ? 0
                    : java.time.temporal.ChronoUnit.DAYS.between(today, b.expirationDate());
            boolean isExpired = days < 0;
            if (isExpired) expired++; else soon++;
            alertsExpModel.addRow(new Object[]{
                    b.sku(), b.productName(), b.batchNumber(), b.warehouseName(),
                    b.expirationDate() == null ? "—" : b.expirationDate().toString(),
                    days, hide ? MASK : (b.quantity() == null ? "" : b.quantity().toPlainString()),
                    isExpired ? "Expirado" : "A expirar"});
        }

        if (alertsSummary != null) {
            alertsSummary.setText(hide
                    ? "Quantidades ocultas — stock trancado (visível só para administradores)."
                    : esgotados.size() + " produto(s) esgotado(s)  ·  "
                    + expired + " lote(s) expirado(s)  ·  " + soon + " a expirar (≤30 dias)");
        }
    }

    private JPanel buildBatchesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Lotes & Validades"), BorderLayout.WEST);
        ModernButton addBatchBtn = UIHelper.createSuccessButton("Adicionar Lote/Validade");
        addBatchBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBatchBtn.addActionListener(e -> createBatchEntryDialog(null));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportBatchesPdf());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(addBatchBtn);
        actions.add(exportBtn);
        header.add(actions, BorderLayout.EAST);

        // Filter bar
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        batchWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(batchWarehouseCombo);
        batchWarehouseCombo.setPreferredSize(new Dimension(220, 35));
        batchWarehouseCombo.addActionListener(e -> filterBatches());

        batchExpirationCombo = new JComboBox<>(new String[]{
                "Todos os lotes",
                "Vencidos",
                "Vence em ≤ 30 dias",
                "Vence em ≤ 90 dias",
                "Válidos (> 90 dias)"
        });
        UIHelper.styleComboBox(batchExpirationCombo);
        batchExpirationCombo.setPreferredSize(new Dimension(200, 35));
        batchExpirationCombo.addActionListener(e -> filterBatches());

        batchSearchField = new SearchField("Pesquisar por SKU, nome ou lote…");
        batchSearchField.getDocument().addDocumentListener(simpleDocumentListener(this::filterBatches));

        g.gridy = 0;
        g.gridx = 0; g.weightx = 0; filters.add(filterLabel("Armazém"), g);
        g.gridx = 1; g.weightx = 0; filters.add(filterLabel("Validade"), g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        filters.add(filterLabel("Pesquisa"), g);
        g.gridy = 1; g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; filters.add(batchWarehouseCombo, g);
        g.gridx = 1; g.weightx = 0; filters.add(batchExpirationCombo, g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        filters.add(batchSearchField, g);

        batchesSummary = new JLabel(" ");
        batchesSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        batchesSummary.setForeground(UIHelper.TEXT_MUTED);
        batchesSummary.setBorder(new EmptyBorder(2, 2, 0, 0));

        JPanel topStack = new JPanel(new BorderLayout(0, 10));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(filters, BorderLayout.CENTER);
        topStack.add(batchesSummary, BorderLayout.SOUTH);
        tab.add(topStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Artigo (SKU)", "Nome do Artigo", "Armazém", "Nº Lote", "Validade", "Dias", "Quantidade", "Estado"};
        batchesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        batchesTable = new JTable(batchesModel);
        UIHelper.styleTable(batchesTable);
        JScrollPane scroll = new JScrollPane(batchesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        card.add(com.phcpro.gui.components.TableFooter.install(batchesTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadBatches() {
        if (batchesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        batchesList = inventoryApiClient.findBatchesByCompany(companyId);

        // Sync the batch warehouse combo with the warehouses list (skipping the first time)
        if (batchWarehouseCombo != null) {
            Object selected = batchWarehouseCombo.getSelectedItem();
            batchWarehouseCombo.removeAllItems();
            batchWarehouseCombo.addItem("--- Todos os Armazéns ---");
            for (WarehouseDTO w : warehousesList) {
                batchWarehouseCombo.addItem(w.name());
            }
            if (selected != null) batchWarehouseCombo.setSelectedItem(selected);
        }
        updateBatchesSummary();
        filterBatches();
    }

    /** Resumo proativo: conta lotes (com stock) vencidos e a vencer em ≤ {@link #EXPIRY_SOON_DAYS} dias. */
    private void updateBatchesSummary() {
        if (batchesSummary == null) return;
        LocalDate today = LocalDate.now();
        long expired = 0;
        long soon = 0;
        for (var b : batchesList) {
            if (b.expirationDate() == null) continue;
            if (b.quantity() == null || b.quantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, b.expirationDate());
            if (days < 0) expired++;
            else if (days <= EXPIRY_SOON_DAYS) soon++;
        }
        if (expired == 0 && soon == 0) {
            batchesSummary.setForeground(UIHelper.APPROVED_GREEN);
            batchesSummary.setText("Sem lotes vencidos ou a vencer em breve.");
        } else {
            batchesSummary.setForeground(expired > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            batchesSummary.setText(String.format("%d lote%s vencido%s · %d a vencer em ≤ %d dias",
                    expired, expired == 1 ? "" : "s", expired == 1 ? "" : "s", soon, EXPIRY_SOON_DAYS));
        }
    }

    private void filterBatches() {
        if (batchesModel == null) return;
        batchesModel.setRowCount(0);
        boolean hide = stockHidden();

        Long filterWarehouseId = null;
        if (batchWarehouseCombo != null) {
            int idx = batchWarehouseCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < warehousesList.size()) {
                filterWarehouseId = warehousesList.get(idx - 1).id();
            }
        }

        String query = batchSearchField == null ? "" : batchSearchField.getText().trim().toLowerCase();
        int expIdx = batchExpirationCombo == null ? 0 : batchExpirationCombo.getSelectedIndex();
        java.time.LocalDate today = java.time.LocalDate.now();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (var b : batchesList) {
            if (filterWarehouseId != null && !b.warehouseId().equals(filterWarehouseId)) continue;

            if (!query.isEmpty()) {
                String sku = b.sku() == null ? "" : b.sku().toLowerCase();
                String name = b.productName() == null ? "" : b.productName().toLowerCase();
                String lote = b.batchNumber() == null ? "" : b.batchNumber().toLowerCase();
                if (!sku.contains(query) && !name.contains(query) && !lote.contains(query)) continue;
            }

            long daysToExp = b.expirationDate() == null
                    ? Long.MAX_VALUE
                    : java.time.temporal.ChronoUnit.DAYS.between(today, b.expirationDate());

            boolean match = switch (expIdx) {
                case 1 -> daysToExp < 0;                   // Vencidos
                case 2 -> daysToExp >= 0 && daysToExp <= 30;
                case 3 -> daysToExp >= 0 && daysToExp <= 90;
                case 4 -> daysToExp > 90;                  // Válidos
                default -> true;
            };
            if (!match) continue;

            String status;
            if (daysToExp == Long.MAX_VALUE) status = "—";
            else if (daysToExp < 0) status = "VENCIDO";
            else if (daysToExp <= 30) status = "VENCE EM BREVE";
            else status = "VÁLIDO";

            String daysCell = daysToExp == Long.MAX_VALUE ? "—" : String.valueOf(daysToExp);

            batchesModel.addRow(new Object[]{
                    b.sku(),
                    b.productName(),
                    b.warehouseName(),
                    b.batchNumber(),
                    b.expirationDate() == null ? "—" : b.expirationDate().format(fmt),
                    daysCell,
                    hide ? MASK : String.format("%,.3f", b.quantity()),
                    status
            });
        }
    }

    private void exportBatchesPdf() {
        if (batchesTable.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nada para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            com.phcpro.modules.company.model.Company company = new com.phcpro.modules.company.model.Company();
            company.setId(CurrentUserContext.getCurrentCompanyId());
            byte[] pdf = com.phcpro.modules.printing.TablePdfExporter.renderFromSwing(company, "Lotes & Validades", batchesTable);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "lotes-validades");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

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
    private boolean stockHidden() {
        try {
            return inventoryApiClient.isStockCountLocked(CurrentUserContext.getCurrentCompanyId()) && !isAdmin();
        } catch (RuntimeException ex) {
            return false; // à prova de falha: em dúvida, não oculta
        }
    }

    /** Actualiza o texto/estado do botão de bloqueio e o banner conforme o estado actual. */
    private void refreshStockLock() {
        boolean locked;
        try {
            locked = inventoryApiClient.isStockCountLocked(CurrentUserContext.getCurrentCompanyId());
        } catch (RuntimeException ex) {
            locked = false;
        }
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
        boolean locked;
        try {
            locked = inventoryApiClient.isStockCountLocked(companyId);
        } catch (RuntimeException ex) {
            locked = false;
        }
        try {
            inventoryApiClient.setStockCountLocked(companyId, !locked);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        onPanelSelected(); // recarrega tabelas (mascaradas ou não) + banner + botão
    }

    // ===== Categorias de produto =====

    private JPanel buildCategoriesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Categorias de Produto"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton newBtn = UIHelper.createSuccessButton("Nova Categoria");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openCategoryDialog(null));
        ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> {
            var sel = selectedCategory();
            if (sel != null) openCategoryDialog(sel);
        });
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar/Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelectedCategory());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadCategories());
        actions.add(newBtn); actions.add(editBtn); actions.add(toggleBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        // Pesquisa por código/nome
        categorySearchField = new SearchField("Pesquisar categoria por código ou nome…");
        UIHelper.onTextChange(categorySearchField, () -> filterCategories(categorySearchField.getText()));
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel sIcon = new JLabel(UIHelper.icon("fas-search", 13, UIHelper.TEXT_MUTED));
        searchRow.add(sIcon, BorderLayout.WEST);
        searchRow.add(categorySearchField, BorderLayout.CENTER);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.add(header, BorderLayout.NORTH);
        headerWrap.add(searchRow, BorderLayout.SOUTH);
        tab.add(headerWrap, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Código", "Nome", "Cor", "Produtos", "Estado"};
        categoriesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        categoriesTable = new JTable(categoriesModel);
        UIHelper.styleTable(categoriesTable);
        // Coluna "Cor" com amostra visível da cor da categoria
        categoriesTable.getColumnModel().getColumn(2).setCellRenderer(new ColorCellRenderer());
        categoriesTable.getColumnModel().getColumn(3).setMaxWidth(90);
        JScrollPane scroll = new JScrollPane(categoriesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        card.add(com.phcpro.gui.components.TableFooter.install(categoriesTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadCategories() {
        if (categoriesModel == null) return;
        categoriesList = productCategoryApiClient.getAll();
        // Contagem de produtos por categoria (gestão profissional: saber o que está em uso)
        categoryProductCounts.clear();
        for (ProductDTO p : comercialApiClient.getAllProducts()) {
            if (p.categoryId() != null) {
                categoryProductCounts.merge(p.categoryId(), 1, Integer::sum);
            }
        }
        filterCategories(categorySearchField == null ? "" : categorySearchField.getText());
    }

    private void filterCategories(String query) {
        if (categoriesModel == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        categoriesFiltered = categoriesList.stream()
                .filter(c -> q.isEmpty()
                        || (c.code() != null && c.code().toLowerCase().contains(q))
                        || (c.name() != null && c.name().toLowerCase().contains(q)))
                .toList();
        categoriesModel.setRowCount(0);
        for (var c : categoriesFiltered) {
            categoriesModel.addRow(new Object[]{
                    c.code(), c.name(),
                    c.colorHex() != null && !c.colorHex().isBlank() ? c.colorHex() : "—",
                    categoryProductCounts.getOrDefault(c.id(), 0),
                    c.active() ? "Activa" : "Inactiva"});
        }
    }

    private com.phcpro.modules.comercial.dto.ProductCategoryDTO selectedCategory() {
        int row = categoriesTable.getSelectedRow();
        if (row < 0 || row >= categoriesFiltered.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma categoria.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return categoriesFiltered.get(row);
    }

    /** Renderiza a coluna "Cor" com uma amostra (quadrado) da cor hex da categoria + o código hex. */
    private static class ColorCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
            setBackground(isSelected ? t.getSelectionBackground() : (row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT));
            setForeground(UIHelper.TEXT_LIGHT);
            String hex = value == null ? "" : value.toString();
            setIcon(colorSwatchIcon(hex, 14));
            setText("  " + (hex.isBlank() ? "—" : hex));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }

    /** Ícone quadrado preenchido com a cor hex (ou contorno cinza quando inválida/ausente). */
    private static javax.swing.Icon colorSwatchIcon(String hex, int size) {
        Color c = null;
        try {
            if (hex != null && hex.trim().startsWith("#")) c = Color.decode(hex.trim());
        } catch (NumberFormatException ignored) { }
        final Color fill = c;
        return new javax.swing.Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (fill != null) {
                    g2.setColor(fill);
                    g2.fillRoundRect(x, y, size, size, 4, 4);
                } else {
                    g2.setColor(new Color(107, 114, 128));
                    g2.drawRoundRect(x, y, size - 1, size - 1, 4, 4);
                }
                g2.dispose();
            }
        };
    }

    private void toggleSelectedCategory() {
        var sel = selectedCategory();
        if (sel == null) return;
        try {
            productCategoryApiClient.setActive(sel.id(), !sel.active());
            loadCategories();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCategoryDialog(com.phcpro.modules.comercial.dto.ProductCategoryDTO existing) {
        boolean editing = existing != null;
        JTextField codeField = new JTextField(editing ? existing.code() : "");
        JTextField nameField = new JTextField(editing ? existing.name() : "");

        // Seletor de cor profissional: amostra + escolher (JColorChooser) + limpar. Hex guardado num holder.
        final String[] colorHolder = { editing && existing.colorHex() != null ? existing.colorHex() : "" };
        JLabel swatch = new JLabel();
        swatch.setOpaque(true);
        swatch.setPreferredSize(new Dimension(40, UIHelper.FORM_CONTROL_HEIGHT));
        swatch.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99), 1, true));
        Runnable applySwatch = () -> {
            Color c = null;
            try { if (colorHolder[0].startsWith("#")) c = Color.decode(colorHolder[0]); } catch (NumberFormatException ignored) { }
            swatch.setBackground(c != null ? c : UIHelper.BG_CARD);
            swatch.setText(c == null ? "  sem cor" : "");
            swatch.setForeground(UIHelper.TEXT_MUTED);
        };
        applySwatch.run();
        ModernButton pickBtn = UIHelper.createSecondaryButton("Escolher…");
        pickBtn.setIcon(UIHelper.icon("fas-palette", 14));
        pickBtn.addActionListener(ev -> {
            Color initial = UIHelper.ACCENT_BLUE;
            try { if (colorHolder[0].startsWith("#")) initial = Color.decode(colorHolder[0]); } catch (NumberFormatException ignored) { }
            Color chosen = JColorChooser.showDialog(this, "Cor da categoria", initial);
            if (chosen != null) {
                colorHolder[0] = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
                applySwatch.run();
            }
        });
        ModernButton clearBtn = UIHelper.createSecondaryButton("Limpar");
        clearBtn.setIcon(UIHelper.icon("fas-times", 14));
        clearBtn.addActionListener(ev -> { colorHolder[0] = ""; applySwatch.run(); });
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        colorPanel.setOpaque(false);
        colorPanel.add(swatch);
        colorPanel.add(pickBtn);
        colorPanel.add(clearBtn);

        JPanel form = UIHelper.createDialogForm(
                "Código:", codeField,
                "Nome:", nameField,
                "Cor (opcional):", colorPanel);

        Window parent = SwingUtilities.getWindowAncestor(this);
        ModernFormDialog dlg = new ModernFormDialog(parent,
                editing ? "Editar Categoria" : "Nova Categoria", "fas-tags",
                "Organize os produtos em categorias da loja", form);
        dlg.setSize(480, 360);
        dlg.setOnSave(() -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            if (code.isEmpty() || name.isEmpty()) {
                throw new RuntimeException("Código e nome são obrigatórios.");
            }
            var req = new com.phcpro.modules.comercial.dto.CreateProductCategoryRequest(
                    code, name, colorHolder[0].isBlank() ? null : colorHolder[0]);
            if (editing) productCategoryApiClient.update(existing.id(), req);
            else productCategoryApiClient.create(req);
        });
        if (dlg.showDialog()) {
            loadCategories();
        }
    }

    private void loadTransfers() {
        transferModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        transfersList = stockTransferApiClient.findByCompany(companyId);

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
        warehousesList = inventoryApiClient.getWarehousesByCompany(companyId);

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
        stocksList = inventoryApiClient.getStocksByCompany(companyId);
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
                    String.format("%,.2f MT", price),
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
        movementsModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<StockMovementDTO> movements = inventoryApiClient.getMovementsByCompany(companyId);

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


    private JPanel buildWarehousesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Gestão de Armazéns"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton newBtn = UIHelper.createSuccessButton("Novo Armazém");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> warehouseDialog(null));
        ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> { WarehouseDTO w = selectedManagedWarehouse(); if (w != null) warehouseDialog(w); });
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar/Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelectedWarehouse());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadWarehousesManagement());
        actions.add(refreshBtn); actions.add(toggleBtn); actions.add(editBtn); actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Nome", "Nº", "Tipo", "Capacidade", "Localização", "Responsável", "Telefone", "Vendas", "Estado"};
        warehousesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        warehousesTable = new JTable(warehousesModel);
        UIHelper.styleTable(warehousesTable);
        warehousesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { WarehouseDTO w = selectedManagedWarehouse(); if (w != null) warehouseDialog(w); }
            }
        });
        JScrollPane scroll = new JScrollPane(warehousesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField whSearch = TableFilter.searchField("Nome, nº, localização ou responsável…");
        JComboBox<String> whTipo = TableFilter.combo("Todos os tipos",
                "Loja", "Depósito", "Armazém Central", "Trânsito");
        JComboBox<String> whEstado = TableFilter.combo("Todos os estados", "ACTIVO", "INATIVO");
        TableFilter.install(warehousesTable, whSearch,
                new TableFilter.ColumnFilter(whTipo, 2),
                new TableFilter.ColumnFilter(whEstado, 8));
        JPanel whBar = TableFilter.bar(whSearch,
                TableFilter.label("Tipo:"), whTipo,
                TableFilter.label("Estado:"), whEstado);
        whBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(whBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadWarehousesManagement() {
        if (warehousesModel == null) return;
        warehousesFullList = inventoryApiClient.getAllWarehousesByCompany(CurrentUserContext.getCurrentCompanyId());
        warehousesModel.setRowCount(0);
        for (WarehouseDTO w : warehousesFullList) {
            warehousesModel.addRow(new Object[]{
                    w.name(),
                    w.warehouseNumber() == null ? "—" : w.warehouseNumber(),
                    w.type() == null ? "—" : w.type().label(),
                    w.capacity() == null ? "—" : String.format("%,.2f", w.capacity()),
                    w.location() == null ? "—" : w.location(),
                    w.manager() == null ? "—" : w.manager(),
                    w.phone() == null ? "—" : w.phone(),
                    w.allowsSales() ? "Sim" : "Não",
                    w.active() ? "ACTIVO" : "INATIVO"});
        }
    }

    private WarehouseDTO selectedManagedWarehouse() {
        int row = warehousesTable == null ? -1 : warehousesTable.getSelectedRow();
        if (row < 0 || row >= warehousesFullList.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um armazém.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return warehousesFullList.get(warehousesTable.convertRowIndexToModel(row));
    }

    private void toggleSelectedWarehouse() {
        WarehouseDTO w = selectedManagedWarehouse();
        if (w == null) return;
        try {
            inventoryApiClient.setWarehouseActive(w.id(), !w.active());
            onPanelSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createWarehouseDialogV2() {
        warehouseDialog(null);
    }

    /** Diálogo de criar/editar armazém. {@code existing == null} → criar; senão → editar. */
    private void warehouseDialog(WarehouseDTO existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.name() : "");
        JTextField numberField = new JTextField(editing && existing.warehouseNumber() != null ? existing.warehouseNumber() : "");
        JTextField capacityField = new JTextField(editing && existing.capacity() != null ? existing.capacity().toPlainString() : "0");
        JTextField locField = new JTextField(editing && existing.location() != null ? existing.location() : "");
        JComboBox<String> typeCombo = new JComboBox<>();
        for (var t : com.phcpro.modules.inventory.model.WarehouseType.values()) typeCombo.addItem(t.label());
        UIHelper.styleComboBox(typeCombo);
        if (editing && existing.type() != null) typeCombo.setSelectedIndex(existing.type().ordinal());
        JTextField managerField = new JTextField(editing && existing.manager() != null ? existing.manager() : "");
        JTextField phoneField = new JTextField(editing && existing.phone() != null ? existing.phone() : "");
        UIHelper.styleTextField(managerField);
        UIHelper.styleTextField(phoneField);
        JCheckBox allowsSalesCheck = new JCheckBox("Permite vendas ao balcão (POS)", editing ? existing.allowsSales() : true);
        allowsSalesCheck.setOpaque(false);
        allowsSalesCheck.setForeground(UIHelper.TEXT_LIGHT);

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Nome do Armazem:", nameField,
                "Numero do Armazem:", numberField,
                "Tipo:", typeCombo,
                "Capacidade:", capacityField,
                "Localizacao / Endereco:", locField,
                "Responsável:", managerField,
                "Telefone:", phoneField,
                "Vendas:", allowsSalesCheck
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                editing ? "Editar Armazém" : "Criar Novo Armazém", "fas-warehouse",
                editing ? "Actualize os dados do local" : "Registe um novo local de stock", dialogPanel).showDialog();
        if (confirmed) {
            String name = nameField.getText().trim();
            String warehouseNumber = numberField.getText().trim();
            String capacityStr = capacityField.getText().trim();
            String location = locField.getText().trim();

            if (name.isEmpty() || warehouseNumber.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome e numero do armazem sao obrigatorios.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                BigDecimal capacity = capacityStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(capacityStr);
                if (capacity.compareTo(BigDecimal.ZERO) < 0) {
                    JOptionPane.showMessageDialog(this, "A capacidade deve ser zero ou superior.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                com.phcpro.modules.inventory.model.WarehouseType type =
                        com.phcpro.modules.inventory.model.WarehouseType.values()[Math.max(0, typeCombo.getSelectedIndex())];

                if (editing) {
                    inventoryApiClient.updateWarehouse(existing.id(), new UpdateWarehouseRequest(
                            name, warehouseNumber, capacity, location,
                            type, allowsSalesCheck.isSelected(),
                            managerField.getText().trim(), phoneField.getText().trim()));
                    JOptionPane.showMessageDialog(this, "Armazém '" + name + "' actualizado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    inventoryApiClient.createWarehouse(new CreateWarehouseRequest(
                            name, warehouseNumber, capacity, location, CurrentUserContext.getCurrentCompanyId(),
                            type, allowsSalesCheck.isSelected(),
                            managerField.getText().trim(), phoneField.getText().trim()));
                    JOptionPane.showMessageDialog(this, "Armazem '" + name + "' criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                }
                onPanelSelected();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "A capacidade deve ser um valor numerico.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gravar armazem: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Inventário físico (contagem cega): imprime a folha de contagem, permite introduzir as contagens
     * por artigo de um armazém e reconcilia — cada artigo contado gera um ajuste de stock (define a
     * quantidade contada) e mostra a diferença face ao sistema. Artigos deixados em branco não são
     * tocados (só se ajusta o que foi efectivamente contado).
     */
    private void openPhysicalInventoryDialog() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"#", "Armazém", "Estado", "Itens", "Contados", "Criado por", "Data"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty("noRowInspector", Boolean.TRUE);
        table.putClientProperty("noTableFooter", Boolean.TRUE);
        JScrollPane sc = new JScrollPane(table);
        UIHelper.styleScrollPane(sc);
        sc.setPreferredSize(new Dimension(660, 340));

        final java.util.List<com.phcpro.modules.inventory.dto.InventoryCountDTO> rows = new ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Runnable reload = () -> {
            model.setRowCount(0);
            rows.clear();
            for (var s : inventoryCountApiClient.listSessions(companyId)) {
                rows.add(s);
                model.addRow(new Object[]{ s.id(), s.warehouseName(), inventoryCountStatusLabel(s.status()),
                        s.totalItems(), s.countedItems(), s.createdBy(),
                        s.createdAt() == null ? "" : s.createdAt().format(dtf) });
            }
        };
        try {
            reload.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(sc, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Inventário Físico",
                "fas-clipboard-list", "Sessões de contagem — crie, retome e aplique", content)
                .asReadOnly("Fechar");

        ModernButton newBtn = UIHelper.createPrimaryButton("Nova Contagem");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> { if (startNewCountSession()) reload.run(); });

        Runnable openSelected = () -> {
            int r = table.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(this, "Selecione uma contagem.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            openCountSession(rows.get(r).id());
            reload.run();
        };
        ModernButton openBtn = UIHelper.createSecondaryButton("Abrir");
        openBtn.setIcon(UIHelper.icon("fas-folder-open", 14));
        openBtn.addActionListener(e -> openSelected.run());

        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar Sessão");
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        cancelBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(this, "Selecione uma contagem.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            var s = rows.get(r);
            if (!"DRAFT".equals(s.status())) { JOptionPane.showMessageDialog(this, "Só é possível cancelar contagens em curso.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            if (JOptionPane.showConfirmDialog(this, "Cancelar a contagem #" + s.id() + "?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try { inventoryCountApiClient.cancelSession(s.id()); reload.run(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE); }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) openSelected.run();
            }
        });

        dlg.addActionButton(newBtn);
        dlg.addActionButton(openBtn);
        dlg.addActionButton(cancelBtn);
        dlg.showDialog();
        onPanelSelected(); // o stock pode ter mudado se alguma sessão foi aplicada
    }

    /** Cria uma nova sessão de contagem para um armazém e abre-a já para contar. */
    private boolean startNewCountSession() {
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre um armazém primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        JComboBox<String> whCombo = new JComboBox<>();
        UIHelper.styleComboBox(whCombo);
        for (WarehouseDTO w : warehousesList) whCombo.addItem(w.name());
        JPanel form = UIHelper.createDialogForm("Armazém a contar:", whCombo);
        boolean ok = new ModernFormDialog(UIHelper.mainWindow, "Nova Contagem", "fas-clipboard-list",
                "Cria uma sessão com uma linha por artigo do armazém", form)
                .setConfirmButton("Criar", "fas-check").showDialog();
        if (!ok) return false;
        int idx = whCombo.getSelectedIndex();
        if (idx < 0) return false;
        try {
            var session = inventoryCountApiClient.createSession(warehousesList.get(idx).id(), null);
            openCountSession(session.id());
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Abre uma sessão: DRAFT = editar/guardar/aplicar; aplicada/cancelada = só-leitura com diferenças. */
    private void openCountSession(Long sessionId) {
        com.phcpro.modules.inventory.dto.InventoryCountDTO session;
        try {
            session = inventoryCountApiClient.getSession(sessionId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean draft = "DRAFT".equals(session.status());

        String[] cols = draft ? new String[]{"SKU", "Artigo", "Contagem"}
                              : new String[]{"SKU", "Artigo", "Contagem", "Sistema", "Diferença"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return draft && c == 2; }
        };
        final java.util.List<Long> productIds = new ArrayList<>();
        for (var line : session.lines()) {
            productIds.add(line.productId());
            String counted = line.countedQuantity() == null ? "" : line.countedQuantity().stripTrailingZeros().toPlainString();
            if (draft) {
                model.addRow(new Object[]{ line.sku(), line.name(), counted });
            } else {
                String sys = line.systemQuantity() == null ? "" : line.systemQuantity().stripTrailingZeros().toPlainString();
                String diff = line.difference() == null ? "" :
                        (line.difference().signum() > 0 ? "+" : "") + line.difference().stripTrailingZeros().toPlainString();
                model.addRow(new Object[]{ line.sku(), line.name(), counted, sys, diff });
            }
        }
        JTable countTable = new JTable(model);
        UIHelper.styleTable(countTable);
        countTable.putClientProperty("noRowInspector", Boolean.TRUE);
        countTable.putClientProperty("noTableFooter", Boolean.TRUE);
        JScrollPane sc = new JScrollPane(countTable);
        UIHelper.styleScrollPane(sc);
        sc.setPreferredSize(new Dimension(620, 400));
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(sc, BorderLayout.CENTER);

        String subtitle = "Armazém: " + session.warehouseName() + " · " + inventoryCountStatusLabel(session.status());

        if (!draft) {
            new ModernFormDialog(UIHelper.mainWindow, "Contagem #" + sessionId, "fas-clipboard-check",
                    subtitle, content).asReadOnly("Fechar").showDialog();
            return;
        }

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Contagem #" + sessionId,
                "fas-clipboard-check", subtitle + " — em branco = não conta", content)
                .setConfirmButton("Aplicar Ajustes", "fas-check");

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Folha");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> {
            try {
                byte[] pdf = inventoryApiClient.renderCountSheet(
                        CurrentUserContext.getCurrentCompanyId(), session.warehouseId());
                com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "folha-contagem-" + session.warehouseName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        ModernButton saveBtn = UIHelper.createSecondaryButton("Guardar Rascunho");
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.addActionListener(e -> {
            try {
                inventoryCountApiClient.saveCounts(sessionId, readCountsFromTable(countTable, productIds));
                JOptionPane.showMessageDialog(this, "Contagem guardada.", "Rascunho", JOptionPane.INFORMATION_MESSAGE);
                dlg.close();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dlg.addActionButton(printBtn);
        dlg.addActionButton(saveBtn);
        dlg.setOnSave(() -> {
            inventoryCountApiClient.saveCounts(sessionId, readCountsFromTable(countTable, productIds));
            var result = inventoryCountApiClient.applySession(sessionId);
            onPanelSelected();
            int applied = 0;
            StringBuilder diffs = new StringBuilder();
            for (var line : result.lines()) {
                if (line.countedQuantity() == null) continue;
                applied++;
                BigDecimal d = line.difference();
                if (d != null && d.signum() != 0) {
                    diffs.append(String.format("• %s: sistema %s → contado %s (%s%s)%n",
                            line.name(),
                            line.systemQuantity() == null ? "?" : line.systemQuantity().stripTrailingZeros().toPlainString(),
                            line.countedQuantity().stripTrailingZeros().toPlainString(),
                            d.signum() > 0 ? "+" : "", d.stripTrailingZeros().toPlainString()));
                }
            }
            JOptionPane.showMessageDialog(this,
                    applied + " artigo(s) reconciliado(s).\n\n"
                            + (diffs.length() == 0 ? "Sem diferenças face ao sistema." : "Diferenças:\n" + diffs),
                    "Inventário aplicado", JOptionPane.INFORMATION_MESSAGE);
        });
        dlg.showDialog();
    }

    /** Lê a coluna "Contagem" da tabela para um mapa productId → quantidade (ignora vazios/não numéricos). */
    private java.util.Map<Long, BigDecimal> readCountsFromTable(JTable table, java.util.List<Long> productIds) {
        if (table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
        java.util.Map<Long, BigDecimal> counts = new java.util.HashMap<>();
        for (int i = 0; i < table.getRowCount() && i < productIds.size(); i++) {
            Object v = table.getValueAt(i, 2);
            String txt = v == null ? "" : v.toString().trim();
            if (txt.isEmpty()) continue;
            try {
                counts.put(productIds.get(i), new BigDecimal(txt.replace(",", ".")));
            } catch (NumberFormatException ignored) { /* ignora contagem não numérica */ }
        }
        return counts;
    }

    private static String inventoryCountStatusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "Em curso";
            case "APPLIED" -> "Aplicada";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    /** Diálogo de impressão de etiquetas: escolher produtos (multi-selecção) + cópias → folha PDF. */
    private void openLabelDialog() {
        java.util.List<ProductDTO> products = comercialApiClient.getAllProducts();
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
        List<ProductDTO> products = comercialApiClient.getAllProducts();
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

                inventoryApiClient.adjustStock(new CreateStockAdjustmentRequest(
                        CurrentUserContext.getCurrentCompanyId(),
                        selectedProductDTO.id(),
                        selectedWarehouse.id(),
                        counted,
                        reason
                ));

                JOptionPane.showMessageDialog(this, "Contagem de stock registada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                onPanelSelected();
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
    private static int parseIntOrZero(String raw) {
        if (raw == null) return 0;
        try {
            int v = Integer.parseInt(raw.trim());
            return Math.max(0, v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** Decimal > 0 a partir de texto livre; vazio/inválido/≤0 → null (campos opcionais de grosso). */
    private static BigDecimal parsePositiveOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            BigDecimal v = new BigDecimal(raw.trim());
            return v.signum() > 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Decimal ≥ 0 a partir de texto livre; vazio/inválido → 0 (para unidades soltas). */
    private static BigDecimal parseDecimalOrZero(String raw) {
        if (raw == null || raw.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal v = new BigDecimal(raw.trim());
            return v.signum() < 0 ? BigDecimal.ZERO : v;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private void createBatchEntryDialog(ProductDTO preselected) {
        List<ProductDTO> products = comercialApiClient.getAllProducts();
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre primeiro um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Crie primeiro um armazém.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> prodCombo = new JComboBox<>();
        JComboBox<String> whCombo = new JComboBox<>();
        // Entrada por caixas: nº de caixas + unidades soltas → total em unidades (read-only).
        // O stock é sempre persistido/movimentado em UNIDADES; a caixa é só camada de entrada.
        JTextField boxesField = new JTextField("0");
        JTextField looseField = new JTextField("0");
        JTextField totalUnitsField = new JTextField();
        totalUnitsField.setEditable(false);
        JLabel unitsPerBoxHint = new JLabel(" ");
        unitsPerBoxHint.setForeground(UIHelper.TEXT_MUTED);
        JTextField expirationField = new JTextField();
        JTextField batchField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField descField = new JTextField("Entrada de lote/validade");

        UIHelper.styleComboBox(prodCombo);
        UIHelper.styleComboBox(whCombo);
        UIHelper.styleTextField(boxesField);
        UIHelper.styleTextField(looseField);
        UIHelper.styleTextField(totalUnitsField);
        UIHelper.styleTextField(expirationField);
        UIHelper.styleTextField(batchField);
        UIHelper.styleTextField(serialField);
        UIHelper.styleTextField(descField);

        expirationField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd (ex: 2027-12-31)");
        batchField.putClientProperty("JTextField.placeholderText", "Opcional — gerado a partir da validade se vazio");
        serialField.putClientProperty("JTextField.placeholderText", "Opcional");

        for (ProductDTO p : products) {
            prodCombo.addItem(p.sku() + " — " + p.name());
        }
        for (WarehouseDTO w : warehousesList) {
            whCombo.addItem(w.name());
        }
        if (preselected != null) {
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).id().equals(preselected.id())) {
                    prodCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Total (unidades) = nº caixas × unidades/caixa + unidades soltas. Recalcula ao mudar
        // produto (logo unidades/caixa), nº de caixas ou unidades soltas.
        Runnable recomputeTotal = () -> {
            int idx = prodCombo.getSelectedIndex();
            int upb = (idx >= 0 && idx < products.size())
                    ? Math.max(1, products.get(idx).unitsPerBox()) : 1;
            unitsPerBoxHint.setText(upb + " unidade(s) por caixa");
            int boxes = parseIntOrZero(boxesField.getText());
            BigDecimal loose = parseDecimalOrZero(looseField.getText());
            BigDecimal total = BigDecimal.valueOf((long) boxes * upb).add(loose);
            totalUnitsField.setText(total.stripTrailingZeros().toPlainString());
        };
        prodCombo.addActionListener(e -> recomputeTotal.run());
        UIHelper.onTextChange(boxesField, recomputeTotal);
        UIHelper.onTextChange(looseField, recomputeTotal);
        recomputeTotal.run();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Produto:", prodCombo,
                "Armazém:", whCombo,
                "Nº de Caixas:", boxesField,
                "Unidades soltas:", looseField,
                "Unidades / caixa:", unitsPerBoxHint,
                "Total (unidades):", totalUnitsField,
                "Validade (yyyy-MM-dd):", expirationField,
                "Nº Lote:", batchField,
                "Nº Série:", serialField,
                "Descrição:", descField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Adicionar Lote / Validade", "fas-boxes", "Registe lote e data de validade (FEFO)", dialogPanel).showDialog();
        if (!confirmed) return;

        int prodIdx = prodCombo.getSelectedIndex();
        int whIdx = whCombo.getSelectedIndex();
        if (prodIdx < 0 || whIdx < 0) return;

        BigDecimal qty;
        try {
            // A quantidade gravada é o total em unidades (caixas × und/caixa + soltas).
            qty = new BigDecimal(totalUnitsField.getText().trim());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Quantidade deve ser maior que zero. Indique o nº de caixas e/ou unidades soltas.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String expRaw = expirationField.getText().trim();
        if (expRaw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Validade é obrigatória (formato yyyy-MM-dd).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(expRaw);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Validade inválida. Use o formato yyyy-MM-dd (ex: 2027-12-31).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (expirationDate.isBefore(LocalDate.now())) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "A validade já está expirada. Pretende registar mesmo assim?",
                    "Confirmação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;
        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;
        String desc = descField.getText().trim();

        ProductDTO selectedDTO = products.get(prodIdx);
        WarehouseDTO selectedWarehouse = warehousesList.get(whIdx);

        try {
            inventoryApiClient.registerMovement(new RegisterMovementRequest(
                    selectedDTO.id(), selectedWarehouse.id(), qty, "ENTRY",
                    batch, serial, desc, expirationDate));
            JOptionPane.showMessageDialog(this,
                    "Lote registado com sucesso para '" + selectedDTO.name() + "'.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            onPanelSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createProductDialog() {
        JTextField skuField = new JTextField();
        JTextField referenceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField salesPriceField = new JTextField();
        JTextField purchasePriceField = new JTextField();
        JTextField minStockField = new JTextField("0");
        JTextField unitsPerBoxField = new JTextField("1");
        JTextField wholesalePriceField = new JTextField();
        JTextField wholesaleMinQtyField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>();

        UIHelper.styleTextField(skuField);
        UIHelper.styleTextField(referenceField);
        UIHelper.styleTextField(barcodeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(salesPriceField);
        UIHelper.styleTextField(purchasePriceField);
        UIHelper.styleTextField(minStockField);
        UIHelper.styleTextField(unitsPerBoxField);
        UIHelper.styleTextField(wholesalePriceField);
        UIHelper.styleTextField(wholesaleMinQtyField);
        UIHelper.styleTextField(descField);
        UIHelper.styleComboBox(categoryCombo);
        wholesalePriceField.putClientProperty("JTextField.placeholderText", "Opcional — preço ao grosso");
        wholesaleMinQtyField.putClientProperty("JTextField.placeholderText", "Qtd (unidades) a partir da qual aplica");

        java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories =
                comercialApiClient.getActiveCategories();
        categoryCombo.addItem("— Sem categoria —");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");

        // IVA dinâmico: taxa de IVA por produto (default = IVA Normal 16%).
        JComboBox<String> taxCombo = new JComboBox<>();
        UIHelper.styleComboBox(taxCombo);
        java.util.List<com.phcpro.modules.fiscal.dto.TaxRateDTO> vatRates =
                comercialApiClient.getActiveVatRates();
        int defaultTaxIdx = 0;
        for (int i = 0; i < vatRates.size(); i++) {
            taxCombo.addItem(vatRates.get(i).name());
            if ("IVA_STANDARD".equals(vatRates.get(i).type())) defaultTaxIdx = i;
        }
        if (taxCombo.getItemCount() > 0) taxCombo.setSelectedIndex(defaultTaxIdx);

        // Selector de imagem (opcional) — guardada como thumbnail na BD para o catálogo POS em cards.
        final byte[][] imageHolder = {null};
        JLabel imagePreview = new JLabel("Sem imagem", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(96, 96));
        imagePreview.setOpaque(true);
        imagePreview.setBackground(UIHelper.BG_CARD);
        imagePreview.setForeground(UIHelper.TEXT_MUTED);
        imagePreview.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99), 1, true));
        ModernButton chooseImageBtn = UIHelper.createSecondaryButton("Escolher Imagem…");
        chooseImageBtn.setIcon(UIHelper.icon("fas-image", 14));
        chooseImageBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens (png, jpg)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    JOptionPane.showMessageDialog(this, "Não foi possível ler a imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                imageHolder[0] = bytes;
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(bytes, 96, 96));
            }
        });
        JPanel imagePanel = new JPanel(new BorderLayout(10, 0));
        imagePanel.setOpaque(false);
        imagePanel.add(imagePreview, BorderLayout.WEST);
        imagePanel.add(chooseImageBtn, BorderLayout.CENTER);

        JPanel dialogPanel = UIHelper.createDialogForm(
                "SKU / Codigo (Unico):", skuField,
                "Referencia:", referenceField,
                "Codigo de Barras:", barcodeField,
                "Nome do Produto:", nameField,
                "Categoria:", categoryCombo,
                "Taxa de IVA:", taxCombo,
                "Preço de Venda (MT):", salesPriceField,
                "Preço de Compra (MT):", purchasePriceField,
                "Stock Mínimo:", minStockField,
                "Unidades por Caixa:", unitsPerBoxField,
                "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField,
                "Descrição:", descField,
                "Imagem (opcional):", imagePanel
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Cadastrar Novo Produto", "fas-boxes", "Defina os dados e o IVA do artigo", dialogPanel).showDialog();
        if (confirmed) {
            String sku = skuField.getText().trim();
            String reference = referenceField.getText().trim();
            String barcode = barcodeField.getText().trim();
            String name = nameField.getText().trim();
            String salesPriceStr = salesPriceField.getText().trim();
            String purchasePriceStr = purchasePriceField.getText().trim();
            String minStockStr = minStockField.getText().trim();
            String unitsPerBoxStr = unitsPerBoxField.getText().trim();
            String desc = descField.getText().trim();

            if (sku.isEmpty() || name.isEmpty() || salesPriceStr.isEmpty() || purchasePriceStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "SKU, Nome, Preço de Venda e Preço de Compra são campos obrigatórios.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                BigDecimal salesPrice = new BigDecimal(salesPriceStr);
                BigDecimal purchasePrice = new BigDecimal(purchasePriceStr);
                BigDecimal minStock = new BigDecimal(minStockStr);
                int unitsPerBox;
                try {
                    unitsPerBox = unitsPerBoxStr.isEmpty() ? 1 : Integer.parseInt(unitsPerBoxStr);
                    if (unitsPerBox < 1) unitsPerBox = 1;
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Unidades por caixa deve ser um número inteiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int catIdx = categoryCombo.getSelectedIndex();
                Long categoryId = null;
                if (catIdx > 0 && (catIdx - 1) < categories.size()) {
                    categoryId = categories.get(catIdx - 1).id();
                }
                Long taxRateId = null;
                int taxIdx = taxCombo.getSelectedIndex();
                if (taxIdx >= 0 && taxIdx < vatRates.size()) {
                    taxRateId = vatRates.get(taxIdx).id();
                }
                BigDecimal wholesalePrice = parsePositiveOrNull(wholesalePriceField.getText());
                BigDecimal wholesaleMinQty = parsePositiveOrNull(wholesaleMinQtyField.getText());

                ProductDTO created = comercialApiClient.createProduct(
                        sku,
                        reference.isEmpty() ? null : reference,
                        barcode.isEmpty() ? null : barcode,
                        name,
                        salesPrice,
                        purchasePrice,
                        minStock,
                        unitsPerBox,
                        categoryId,
                        "UNIT",
                        true,
                        taxRateId,
                        desc.isEmpty() ? null : desc,
                        wholesalePrice,
                        wholesaleMinQty);

                if (imageHolder[0] != null) {
                    comercialApiClient.updateProductImage(created.id(), imageHolder[0]);
                }

                onPanelSelected();

                int addStock = JOptionPane.showConfirmDialog(this,
                        "Produto '" + name + "' cadastrado.\nDeseja adicionar stock inicial com validade agora?",
                        "Adicionar stock inicial", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (addStock == JOptionPane.YES_OPTION) {
                    createBatchEntryDialog(created);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Os valores de preço e stock mínimo devem ser numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao cadastrar produto: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Editar um produto existente: selecciona-se o artigo num combo e o formulário pré-preenche-se.
     * O SKU é imutável (identidade); os restantes dados — incluindo unidades/caixa e IVA — são
     * actualizáveis. Não mexe no stock. Delega em {@code ComercialApiClient.updateProduct}.
     */
    private void editProductDialog(Long preselectedProductId) {
        java.util.List<ProductDTO> products = comercialApiClient.getAllProducts();
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre primeiro um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> productCombo = new JComboBox<>();
        JTextField skuField = new JTextField();
        skuField.setEditable(false);
        JTextField referenceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField salesPriceField = new JTextField();
        JTextField purchasePriceField = new JTextField();
        JTextField minStockField = new JTextField("0");
        JTextField unitsPerBoxField = new JTextField("1");
        JTextField wholesalePriceField = new JTextField();
        JTextField wholesaleMinQtyField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>();

        UIHelper.styleComboBox(productCombo);
        UIHelper.styleTextField(wholesalePriceField);
        UIHelper.styleTextField(wholesaleMinQtyField);
        UIHelper.styleTextField(skuField);
        UIHelper.styleTextField(referenceField);
        UIHelper.styleTextField(barcodeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(salesPriceField);
        UIHelper.styleTextField(purchasePriceField);
        UIHelper.styleTextField(minStockField);
        UIHelper.styleTextField(unitsPerBoxField);
        UIHelper.styleTextField(descField);
        UIHelper.styleComboBox(categoryCombo);

        for (ProductDTO p : products) productCombo.addItem(p.sku() + " — " + p.name());
        // Abre já no produto seleccionado no inventário (ou no primeiro, se nenhum).
        if (preselectedProductId != null) {
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).id().equals(preselectedProductId)) { productCombo.setSelectedIndex(i); break; }
            }
        }

        java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories =
                comercialApiClient.getActiveCategories();
        categoryCombo.addItem("— Sem categoria —");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");

        JComboBox<String> taxCombo = new JComboBox<>();
        UIHelper.styleComboBox(taxCombo);
        java.util.List<com.phcpro.modules.fiscal.dto.TaxRateDTO> vatRates =
                comercialApiClient.getActiveVatRates();
        for (var r : vatRates) taxCombo.addItem(r.name());

        final byte[][] imageHolder = {null};
        JLabel imagePreview = new JLabel("Sem imagem", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(96, 96));
        imagePreview.setOpaque(true);
        imagePreview.setBackground(UIHelper.BG_CARD);
        imagePreview.setForeground(UIHelper.TEXT_MUTED);
        imagePreview.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99), 1, true));
        ModernButton chooseImageBtn = UIHelper.createSecondaryButton("Escolher Imagem…");
        chooseImageBtn.setIcon(UIHelper.icon("fas-image", 14));
        chooseImageBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens (png, jpg)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    JOptionPane.showMessageDialog(this, "Não foi possível ler a imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                imageHolder[0] = bytes;
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(bytes, 96, 96));
            }
        });
        JPanel imagePanel = new JPanel(new BorderLayout(10, 0));
        imagePanel.setOpaque(false);
        imagePanel.add(imagePreview, BorderLayout.WEST);
        imagePanel.add(chooseImageBtn, BorderLayout.CENTER);

        // Pré-preenche o formulário com o produto seleccionado (e limpa imagem por enviar).
        Runnable prefill = () -> {
            int idx = productCombo.getSelectedIndex();
            if (idx < 0 || idx >= products.size()) return;
            ProductDTO p = products.get(idx);
            skuField.setText(p.sku());
            referenceField.setText(p.reference() == null ? "" : p.reference());
            barcodeField.setText(p.barcode() == null ? "" : p.barcode());
            nameField.setText(p.name());
            salesPriceField.setText(p.unitPrice() == null ? "" : p.unitPrice().toPlainString());
            purchasePriceField.setText(p.purchasePrice() == null ? "0" : p.purchasePrice().toPlainString());
            minStockField.setText(p.minStock() == null ? "0" : p.minStock().toPlainString());
            unitsPerBoxField.setText(String.valueOf(p.unitsPerBox()));
            wholesalePriceField.setText(p.wholesalePrice() == null ? "" : p.wholesalePrice().toPlainString());
            wholesaleMinQtyField.setText(p.wholesaleMinQty() == null ? "" : p.wholesaleMinQty().toPlainString());
            descField.setText(p.description() == null ? "" : p.description());

            categoryCombo.setSelectedIndex(0);
            if (p.categoryId() != null) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).id().equals(p.categoryId())) { categoryCombo.setSelectedIndex(i + 1); break; }
                }
            }
            if (p.taxRateId() != null) {
                for (int i = 0; i < vatRates.size(); i++) {
                    if (vatRates.get(i).id().equals(p.taxRateId())) { taxCombo.setSelectedIndex(i); break; }
                }
            }
            imageHolder[0] = null; // só reenvia imagem se o operador escolher uma nova
            if (p.image() != null && p.image().length > 0) {
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(p.image(), 96, 96));
            } else {
                imagePreview.setIcon(null);
                imagePreview.setText("Sem imagem");
            }
        };
        productCombo.addActionListener(e -> prefill.run());
        prefill.run();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Produto:", productCombo,
                "SKU / Codigo:", skuField,
                "Referencia:", referenceField,
                "Codigo de Barras:", barcodeField,
                "Nome do Produto:", nameField,
                "Categoria:", categoryCombo,
                "Taxa de IVA:", taxCombo,
                "Preço de Venda (MT):", salesPriceField,
                "Preço de Compra (MT):", purchasePriceField,
                "Stock Mínimo:", minStockField,
                "Unidades por Caixa:", unitsPerBoxField,
                "Preço Grosso (MT):", wholesalePriceField,
                "Qtd mín. grosso:", wholesaleMinQtyField,
                "Descrição:", descField,
                "Imagem (opcional):", imagePanel
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Editar Produto", "fas-edit", "Actualize os dados do artigo", dialogPanel).showDialog();
        if (!confirmed) return;

        int idx = productCombo.getSelectedIndex();
        if (idx < 0 || idx >= products.size()) return;
        ProductDTO selected = products.get(idx);

        String reference = referenceField.getText().trim();
        String barcode = barcodeField.getText().trim();
        String name = nameField.getText().trim();
        String salesPriceStr = salesPriceField.getText().trim();
        String purchasePriceStr = purchasePriceField.getText().trim();
        String minStockStr = minStockField.getText().trim();
        String unitsPerBoxStr = unitsPerBoxField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty() || salesPriceStr.isEmpty() || purchasePriceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome, Preço de Venda e Preço de Compra são campos obrigatórios.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            BigDecimal salesPrice = new BigDecimal(salesPriceStr);
            BigDecimal purchasePrice = new BigDecimal(purchasePriceStr);
            BigDecimal minStock = new BigDecimal(minStockStr.isEmpty() ? "0" : minStockStr);
            int unitsPerBox;
            try {
                unitsPerBox = unitsPerBoxStr.isEmpty() ? 1 : Integer.parseInt(unitsPerBoxStr);
                if (unitsPerBox < 1) unitsPerBox = 1;
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Unidades por caixa deve ser um número inteiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int catIdx = categoryCombo.getSelectedIndex();
            Long categoryId = null;
            if (catIdx > 0 && (catIdx - 1) < categories.size()) {
                categoryId = categories.get(catIdx - 1).id();
            }
            Long taxRateId = null;
            int taxIdx = taxCombo.getSelectedIndex();
            if (taxIdx >= 0 && taxIdx < vatRates.size()) {
                taxRateId = vatRates.get(taxIdx).id();
            }

            BigDecimal wholesalePrice = parsePositiveOrNull(wholesalePriceField.getText());
            BigDecimal wholesaleMinQty = parsePositiveOrNull(wholesaleMinQtyField.getText());

            comercialApiClient.updateProduct(
                    selected.id(),
                    reference.isEmpty() ? null : reference,
                    barcode.isEmpty() ? null : barcode,
                    name,
                    salesPrice,
                    purchasePrice,
                    minStock,
                    unitsPerBox,
                    categoryId,
                    selected.saleType(),
                    selected.stockTracked(),
                    taxRateId,
                    desc.isEmpty() ? null : desc,
                    wholesalePrice,
                    wholesaleMinQty);

            if (imageHolder[0] != null) {
                comercialApiClient.updateProductImage(selected.id(), imageHolder[0]);
            }

            onPanelSelected();
            JOptionPane.showMessageDialog(this, "Produto '" + name + "' actualizado com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Os valores de preço e stock mínimo devem ser numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao actualizar produto: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createTransferDialog() {
        if (warehousesList.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "É necessário pelo menos 2 armazéns para realizar uma transferência.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ProductDTO> products = comercialApiClient.getAllProducts();
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "É necessário cadastrar produtos antes de transferir.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> originCombo = new JComboBox<>();
        JComboBox<String> destinationCombo = new JComboBox<>();
        UIHelper.styleComboBox(originCombo);
        UIHelper.styleComboBox(destinationCombo);
        for (WarehouseDTO w : warehousesList) {
            originCombo.addItem(w.name());
            destinationCombo.addItem(w.name());
        }
        if (warehousesList.size() > 1) destinationCombo.setSelectedIndex(1);

        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(responsibleField);
        UIHelper.styleTextField(vehicleField);
        UIHelper.styleTextField(notesField);

        String[] lineCols = {"Produto", "Quantidade", "Lote (FEFO)", "Validade (FEFO)"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 0 || c == 1; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);

        JComboBox<String> productEditorCombo = new JComboBox<>();
        for (ProductDTO p : products) productEditorCombo.addItem(p.name());
        linesTable.getColumnModel().getColumn(0)
                .setCellEditor(new DefaultCellEditor(productEditorCombo));

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(640, 200));

        Runnable refreshTransferFEFO = () -> {
            int wIdx = originCombo.getSelectedIndex();
            WarehouseDTO origin = (wIdx >= 0 && wIdx < warehousesList.size()) ? warehousesList.get(wIdx) : null;
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                String name = String.valueOf(linesModel.getValueAt(i, 0));
                ProductDTO p = products.stream().filter(x -> x.name().equals(name)).findFirst().orElse(null);
                if (p == null || origin == null) {
                    linesModel.setValueAt("", i, 2);
                    linesModel.setValueAt("", i, 3);
                    continue;
                }
                try {
                    var opt = inventoryApiClient.findNextFEFO(p.id(), origin.id());
                    if (opt.isPresent()) {
                        var b = opt.get();
                        linesModel.setValueAt(b.batchNumber() == null ? "—" : b.batchNumber(), i, 2);
                        linesModel.setValueAt(b.expirationDate() == null
                                ? "—"
                                : b.expirationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), i, 3);
                    } else {
                        linesModel.setValueAt("Sem stock", i, 2);
                        linesModel.setValueAt("—", i, 3);
                    }
                } catch (Exception ignored) {
                    linesModel.setValueAt("", i, 2);
                    linesModel.setValueAt("", i, 3);
                }
            }
        };

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        ModernButton removeLineBtn = UIHelper.createDangerButton("- Remover");
        addLineBtn.addActionListener(ev -> {
            linesModel.addRow(new Object[]{products.get(0).name(), "1", "", ""});
            refreshTransferFEFO.run();
        });
        removeLineBtn.addActionListener(ev -> {
            int sel = linesTable.getSelectedRow();
            if (sel >= 0) linesModel.removeRow(sel);
        });
        JPanel lineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineButtons.setOpaque(false);
        lineButtons.add(addLineBtn);
        lineButtons.add(removeLineBtn);

        linesModel.addRow(new Object[]{products.get(0).name(), "1", "", ""});
        originCombo.addActionListener(ev -> refreshTransferFEFO.run());
        linesModel.addTableModelListener(ev -> {
            if (ev.getColumn() == 0) refreshTransferFEFO.run();
        });
        refreshTransferFEFO.run();

        JPanel header = UIHelper.createDialogForm(
                "Armazém de Origem:", originCombo,
                "Armazém de Destino:", destinationCombo,
                "Responsável:", responsibleField,
                "Veículo / Transporte:", vehicleField,
                "Observações:", notesField
        );

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.setOpaque(false);
        dialogPanel.add(header, BorderLayout.NORTH);
        JPanel linesWrap = new JPanel(new BorderLayout(0, 6));
        linesWrap.setOpaque(false);
        linesWrap.add(new JLabel("Linhas da Transferência:"), BorderLayout.NORTH);
        linesWrap.add(linesScroll, BorderLayout.CENTER);
        linesWrap.add(lineButtons, BorderLayout.SOUTH);
        dialogPanel.add(linesWrap, BorderLayout.CENTER);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Nova Transferência de Stock", "fas-exchange-alt", "Mova stock entre armazéns", dialogPanel).showDialog();
        if (!confirmed) return;

        int originIdx = originCombo.getSelectedIndex();
        int destIdx = destinationCombo.getSelectedIndex();
        if (originIdx == destIdx) {
            JOptionPane.showMessageDialog(this, "Armazém de origem e destino devem ser diferentes.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
        if (linesModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Adicione pelo menos uma linha.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<CreateStockTransferLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                String productName = (String) linesModel.getValueAt(i, 0);
                String qtyStr = String.valueOf(linesModel.getValueAt(i, 1)).trim();
                BigDecimal qty = new BigDecimal(qtyStr);
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException("Quantidade deve ser positiva na linha " + (i + 1));
                }
                ProductDTO product = products.stream()
                        .filter(p -> p.name().equals(productName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + productName));
                lines.add(new CreateStockTransferLineRequest(product.id(), qty));
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        WarehouseDTO origin = warehousesList.get(originIdx);
        WarehouseDTO destination = warehousesList.get(destIdx);

        try {
            CreateStockTransferRequest request = new CreateStockTransferRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    origin.id(),
                    destination.id(),
                    responsibleField.getText().trim(),
                    vehicleField.getText().trim(),
                    notesField.getText().trim(),
                    lines
            );
            StockTransferDTO created = stockTransferApiClient.create(request);
            onPanelSelected();

            int print = JOptionPane.showConfirmDialog(this,
                    "Guia " + created.transferNumber() + " registada e PENDENTE DE APROVAÇÃO.\n"
                            + "O stock só sai do armazém de origem após aprovação (MANAGER/ADMIN).\n\n"
                            + "Deseja imprimir a Guia de Transferência agora?",
                    "Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (print == JOptionPane.YES_OPTION) {
                printTransfer(created.id(), created.transferNumber());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void approveSelectedTransfer() {
        int row = TableFilter.selectedModelRow(transferTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma guia na tabela primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockTransferDTO selected = transfersList.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Aprovar a guia " + selected.transferNumber() + "?\n"
                        + "O stock vai sair de '" + selected.originWarehouseName()
                        + "' e entrar em '" + selected.destinationWarehouseName() + "'.",
                "Confirmar aprovação", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            stockTransferApiClient.approve(selected.id());
            onPanelSelected();
            JOptionPane.showMessageDialog(this,
                    "Guia " + selected.transferNumber() + " aprovada. Stock movido — ver aba Movimentos.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedTransfer() {
        int row = TableFilter.selectedModelRow(transferTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma guia na tabela primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockTransferDTO selected = transfersList.get(row);
        String reason = UIHelper.promptRequiredText("Rejeitar Guia", "fas-times-circle",
                "Guia " + selected.transferNumber(), "Motivo da rejeição:");
        if (reason == null) return;
        try {
            stockTransferApiClient.reject(selected.id(), reason);
            onPanelSelected();
            JOptionPane.showMessageDialog(this,
                    "Guia " + selected.transferNumber() + " rejeitada. Nenhum stock foi movido.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedTransfer() {
        int row = TableFilter.selectedModelRow(transferTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma transferência na tabela primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockTransferDTO selected = transfersList.get(row);
        printTransfer(selected.id(), selected.transferNumber());
    }

    private void printTransfer(Long transferId, String transferNumber) {
        try {
            byte[] pdf = stockTransferApiClient.renderTransfer(transferId);
            PdfFileSaver.saveAndOpen(pdf, "transferencia-" + transferNumber);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

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

        try {
            byte[] pdf = inventoryApiClient.renderInventoryReport(CurrentUserContext.getCurrentCompanyId(), warehouseId);
            PdfFileSaver.saveAndOpen(pdf, "inventario-stock-" + fileSuffix);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
