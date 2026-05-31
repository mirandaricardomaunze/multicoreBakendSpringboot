package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.inventory.dto.CreateStockTransferLineRequest;
import com.phcpro.modules.inventory.dto.CreateStockTransferRequest;
import com.phcpro.modules.inventory.dto.StockTransferDTO;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.model.StockMovement;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.inventory.service.StockTransferService;
import com.phcpro.modules.printing.InventoryReportPrintService;
import com.phcpro.modules.printing.PdfFileSaver;
import com.phcpro.modules.printing.StockTransferPrintService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockPanel extends JPanel {

    private final InventoryService inventoryService;
    private final ComercialService comercialService;
    private final StockTransferService stockTransferService;
    private final StockTransferPrintService stockTransferPrintService;
    private final InventoryReportPrintService inventoryReportPrintService;

    // Transfer history
    private DefaultTableModel transferModel;
    private JTable transferTable;
    private List<StockTransferDTO> transfersList = new ArrayList<>();

    // Warehouses list
    private JComboBox<String> warehouseFilterCombo;
    private List<Warehouse> warehousesList = new ArrayList<>();

    // Stock levels
    private DefaultTableModel stockModel;
    private JTable stockTable;
    private List<Stock> stocksList = new ArrayList<>();

    // Movements log
    private DefaultTableModel movementsModel;
    private JTable movementsTable;

    public StockPanel(InventoryService inventoryService,
                       ComercialService comercialService,
                       StockTransferService stockTransferService,
                       StockTransferPrintService stockTransferPrintService,
                       InventoryReportPrintService inventoryReportPrintService) {
        this.inventoryService = inventoryService;
        this.comercialService = comercialService;
        this.stockTransferService = stockTransferService;
        this.stockTransferPrintService = stockTransferPrintService;
        this.inventoryReportPrintService = inventoryReportPrintService;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // TOP BAR: heading + globally-relevant catalogue buttons
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        topBar.add(UIHelper.createHeading("Controle de Stock & Armazéns"), BorderLayout.WEST);

        ModernButton newProductBtn = new ModernButton("Cadastrar Produto", new Color(16, 185, 129), new Color(52, 211, 153));
        newProductBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton newWarehouseBtn = new ModernButton("Criar Armazém", new Color(139, 92, 246), new Color(167, 139, 250));
        newWarehouseBtn.setIcon(UIHelper.icon("fas-warehouse", 14));
        JPanel catalogueGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        catalogueGroup.setOpaque(false);
        catalogueGroup.add(newProductBtn);
        catalogueGroup.add(newWarehouseBtn);
        topBar.add(catalogueGroup, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // TABS: Níveis | Movimentos | Transferências
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPane(tabs);

        tabs.addTab("Níveis de Stock",               UIHelper.icon("fas-boxes", 16, UIHelper.TEXT_LIGHT),         buildLevelsTab());
        tabs.addTab("Lotes & Validades",             UIHelper.icon("fas-calendar-times", 16, UIHelper.TEXT_LIGHT),buildBatchesTab());
        tabs.addTab("Movimentos & Rastreabilidade",  UIHelper.icon("fas-clipboard-list", 16, UIHelper.TEXT_LIGHT),buildMovementsTab());
        tabs.addTab("Transferências entre Armazéns", UIHelper.icon("fas-truck", 16, UIHelper.TEXT_LIGHT),         buildTransfersTab());

        add(tabs, BorderLayout.CENTER);

        // GLOBAL ACTION LISTENERS
        newProductBtn.addActionListener(e -> createProductDialog());
        newWarehouseBtn.addActionListener(e -> createWarehouseDialogV2());

        onPanelSelected();
    }

    private JTextField stockSearchField;
    private JComboBox<String> stockStatusCombo;

    // Batches tab
    private DefaultTableModel batchesModel;
    private JTable batchesTable;
    private JTextField batchSearchField;
    private JComboBox<String> batchExpirationCombo;
    private JComboBox<String> batchWarehouseCombo;
    private List<com.phcpro.modules.inventory.dto.ProductBatchDTO> batchesList = new ArrayList<>();

    private JPanel buildBatchesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Lotes & Validades"), BorderLayout.WEST);
        ModernButton exportBtn = new ModernButton("Exportar PDF", new Color(99, 102, 241), new Color(129, 140, 248));
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportBatchesPdf());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
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

        batchSearchField = new JTextField();
        UIHelper.styleTextField(batchSearchField);
        batchSearchField.putClientProperty("JTextField.placeholderText", "🔍 Pesquisar por SKU, nome ou lote…");
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

        JPanel topStack = new JPanel(new BorderLayout(0, 10));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(filters, BorderLayout.CENTER);
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
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadBatches() {
        if (batchesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        batchesList = inventoryService.findBatchesByCompany(companyId);

        // Sync the batch warehouse combo with the warehouses list (skipping the first time)
        if (batchWarehouseCombo != null) {
            Object selected = batchWarehouseCombo.getSelectedItem();
            batchWarehouseCombo.removeAllItems();
            batchWarehouseCombo.addItem("--- Todos os Armazéns ---");
            for (com.phcpro.modules.inventory.model.Warehouse w : warehousesList) {
                batchWarehouseCombo.addItem(w.getName());
            }
            if (selected != null) batchWarehouseCombo.setSelectedItem(selected);
        }
        filterBatches();
    }

    private void filterBatches() {
        if (batchesModel == null) return;
        batchesModel.setRowCount(0);

        Long filterWarehouseId = null;
        if (batchWarehouseCombo != null) {
            int idx = batchWarehouseCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < warehousesList.size()) {
                filterWarehouseId = warehousesList.get(idx - 1).getId();
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
                    String.format("%,.3f", b.quantity()),
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
        ModernButton printInventoryBtn = new ModernButton("Imprimir Inventário", new Color(99, 102, 241), new Color(129, 140, 248));
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

        // Search
        stockSearchField = new JTextField();
        UIHelper.styleTextField(stockSearchField);
        stockSearchField.putClientProperty("JTextField.placeholderText", "🔍 Pesquisar por SKU ou nome…");
        stockSearchField.getDocument().addDocumentListener(simpleDocumentListener(this::filterStocks));
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        filters.add(filterLabel("Pesquisa"), g);

        // Row 1: controls aligned below their labels
        g.gridy = 1; g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; filters.add(warehouseFilterCombo, g);
        g.gridx = 1; g.weightx = 0; filters.add(stockStatusCombo, g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        filters.add(stockSearchField, g);

        JPanel topStack = new JPanel(new BorderLayout(0, 10));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(filters, BorderLayout.CENTER);
        tab.add(topStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] stockCols = {"Código de Barras", "Referência", "Nome do Produto", "Qtd Unidades", "Qtd Caixas", "Preço"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        UIHelper.styleTable(stockTable);
        JScrollPane stockScroll = new JScrollPane(stockTable);
        UIHelper.styleScrollPane(stockScroll);
        card.add(stockScroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
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

        ModernButton adjustmentBtn = new ModernButton("Ajuste / Inventário", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
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
        card.add(movScroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildTransfersTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton transferBtn = new ModernButton("Nova Transferência", new Color(245, 158, 11), new Color(251, 191, 36));
        transferBtn.setIcon(UIHelper.icon("fas-truck", 14));
        ModernButton printTransferBtn = new ModernButton("Imprimir Guia", new Color(99, 102, 241), new Color(129, 140, 248));
        printTransferBtn.setIcon(UIHelper.icon("fas-print", 14));
        transferBtn.addActionListener(e -> createTransferDialog());
        printTransferBtn.addActionListener(e -> printSelectedTransfer());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Transferências entre Armazéns"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
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
        card.add(transferScroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }


    public void onPanelSelected() {
        loadWarehouses();
        loadStocks();
        loadMovements();
        loadTransfers();
        loadBatches();
    }

    private void loadTransfers() {
        transferModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        transfersList = stockTransferService.findByCompany(companyId);

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
        warehousesList = inventoryService.getWarehousesByCompany(companyId);

        // Disable listeners temporarily
        Object selectedItem = warehouseFilterCombo.getSelectedItem();
        warehouseFilterCombo.removeAllItems();
        warehouseFilterCombo.addItem("--- Todos os Armazéns ---");

        for (Warehouse w : warehousesList) {
            warehouseFilterCombo.addItem(w.getName());
        }

        if (selectedItem != null) {
            warehouseFilterCombo.setSelectedItem(selectedItem);
        }
    }

    private void loadStocks() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        stocksList = inventoryService.getStocksByCompany(companyId);
        filterStocks();
    }

    private void filterStocks() {
        stockModel.setRowCount(0);

        int filterIdx = warehouseFilterCombo.getSelectedIndex();
        Long filterWarehouseId = null;
        if (filterIdx > 0 && (filterIdx - 1) < warehousesList.size()) {
            filterWarehouseId = warehousesList.get(filterIdx - 1).getId();
        }

        String query = stockSearchField == null ? "" : stockSearchField.getText().trim().toLowerCase();
        int statusIdx = stockStatusCombo == null ? 0 : stockStatusCombo.getSelectedIndex();
        java.math.BigDecimal lowThreshold = java.math.BigDecimal.valueOf(5);

        for (Stock s : stocksList) {
            if (filterWarehouseId != null && !s.getWarehouse().getId().equals(filterWarehouseId)) continue;

            if (!query.isEmpty()) {
                String sku = s.getProduct().getSku() == null ? "" : s.getProduct().getSku().toLowerCase();
                String reference = s.getProduct().getReference() == null ? "" : s.getProduct().getReference().toLowerCase();
                String barcode = s.getProduct().getBarcode() == null ? "" : s.getProduct().getBarcode().toLowerCase();
                String name = s.getProduct().getName() == null ? "" : s.getProduct().getName().toLowerCase();
                if (!sku.contains(query) && !reference.contains(query) && !barcode.contains(query) && !name.contains(query)) continue;
            }

            java.math.BigDecimal qty = s.getQuantity() == null ? java.math.BigDecimal.ZERO : s.getQuantity();
            int statusOk = switch (statusIdx) {
                case 1 -> qty.compareTo(java.math.BigDecimal.ZERO) > 0 ? 1 : 0;        // Em stock
                case 2 -> qty.compareTo(java.math.BigDecimal.ZERO) > 0
                        && qty.compareTo(lowThreshold) < 0 ? 1 : 0;                    // Stock baixo
                case 3 -> qty.compareTo(java.math.BigDecimal.ZERO) <= 0 ? 1 : 0;       // Sem stock
                default -> 1;
            };
            if (statusOk == 0) continue;

            int unitsPerBox = s.getProduct().getUnitsPerBox() <= 0 ? 1 : s.getProduct().getUnitsPerBox();
            java.math.BigDecimal qtyBoxes = qty.divide(
                    java.math.BigDecimal.valueOf(unitsPerBox), 2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal price = s.getProduct().getUnitPrice() == null
                    ? java.math.BigDecimal.ZERO : s.getProduct().getUnitPrice();

            stockModel.addRow(new Object[]{
                    s.getProduct().getBarcode() == null ? "—" : s.getProduct().getBarcode(),
                    s.getProduct().getReference() == null ? s.getProduct().getSku() : s.getProduct().getReference(),
                    s.getProduct().getName(),
                    String.format("%,.3f", qty),
                    String.format("%,.2f", qtyBoxes),
                    String.format("%,.2f MT", price)
            });
        }
    }

    private void loadMovements() {
        movementsModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<StockMovement> movements = inventoryService.getMovementsByCompany(companyId);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (StockMovement m : movements) {
            BigDecimal qty = m.getQuantity();
            String formattedQty = (qty.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + String.format("%,.3f", qty);
            movementsModel.addRow(new Object[]{
                    m.getMovementDate().format(dtf),
                    m.getProduct().getName(),
                    m.getWarehouse().getName(),
                    formattedQty,
                    m.getMovementType(),
                    m.getBatchNumber() != null ? m.getBatchNumber() : "-",
                    m.getSerialNumber() != null ? m.getSerialNumber() : "-",
                    m.getDescription()
            });
        }
    }

    private void createWarehouseDialog() {
        JTextField nameField = new JTextField();
        JTextField locField = new JTextField();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Nome do Armazém:", nameField,
                "Localização / Endereço:", locField
        );

        int option = JOptionPane.showConfirmDialog(this, dialogPanel, "Criar Novo Armazém", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String location = locField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O nome do armazém é obrigatório.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // We need the active company model to create
                // Let's resolve the current company object from DB via the id
                // Note that the service will handle company association
                // To keep it simple, we can fetch all company details later or pass details
                // In inventoryService, it accepts Company object, so we find it by ID first
                // Let's find company by ID
                com.phcpro.modules.company.model.Company currentCompany = new com.phcpro.modules.company.model.Company();
                currentCompany.setId(CurrentUserContext.getCurrentCompanyId());

                inventoryService.createWarehouse(name, location, currentCompany);
                JOptionPane.showMessageDialog(this, "Armazém '" + name + "' criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                onPanelSelected();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao criar armazém: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createWarehouseDialogV2() {
        JTextField nameField = new JTextField();
        JTextField numberField = new JTextField();
        JTextField capacityField = new JTextField("0");
        JTextField locField = new JTextField();

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Nome do Armazem:", nameField,
                "Numero do Armazem:", numberField,
                "Capacidade:", capacityField,
                "Localizacao / Endereco:", locField
        );

        int option = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(dialogPanel), "Criar Novo Armazem", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
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

                com.phcpro.modules.company.model.Company currentCompany = new com.phcpro.modules.company.model.Company();
                currentCompany.setId(CurrentUserContext.getCurrentCompanyId());

                inventoryService.createWarehouse(name, warehouseNumber, capacity, location, currentCompany);
                JOptionPane.showMessageDialog(this, "Armazem '" + name + "' criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                onPanelSelected();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "A capacidade deve ser um valor numerico.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao criar armazem: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createAdjustmentDialog() {
        List<ProductDTO> products = comercialService.getAllProducts();
        if (products.isEmpty() || warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Necessário cadastrar produtos e armazéns primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> prodCombo = new JComboBox<>();
        JComboBox<String> whCombo = new JComboBox<>();
        JTextField qtyField = new JTextField();
        JTextField batchField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField descField = new JTextField("Ajuste manual de inventário");

        UIHelper.styleComboBox(prodCombo);
        UIHelper.styleComboBox(whCombo);
        UIHelper.styleTextField(qtyField);
        UIHelper.styleTextField(batchField);
        UIHelper.styleTextField(serialField);
        UIHelper.styleTextField(descField);

        for (ProductDTO p : products) {
            prodCombo.addItem(p.name());
        }
        for (Warehouse w : warehousesList) {
            whCombo.addItem(w.getName());
        }

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Selecionar Artigo:", prodCombo,
                "Selecionar Armazém:", whCombo,
                "Quantidade (use - para saídas, ex: -10):", qtyField,
                "Número de Lote (se aplicável):", batchField,
                "Número de Série (se aplicável):", serialField,
                "Motivo / Descrição:", descField
        );

        int option = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(dialogPanel), "Registar Ajuste de Inventário", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            int prodIdx = prodCombo.getSelectedIndex();
            int whIdx = whCombo.getSelectedIndex();

            if (prodIdx < 0 || whIdx < 0) return;

            ProductDTO selectedProductDTO = products.get(prodIdx);
            Warehouse selectedWarehouse = warehousesList.get(whIdx);

            try {
                BigDecimal qty = new BigDecimal(qtyField.getText().trim());
                String batch = batchField.getText().trim();
                if (batch.isEmpty()) batch = null;

                String serial = serialField.getText().trim();
                if (serial.isEmpty()) serial = null;

                String desc = descField.getText().trim();

                // Map ProductDTO to Product entity
                com.phcpro.modules.comercial.model.Product p = new com.phcpro.modules.comercial.model.Product();
                p.setId(selectedProductDTO.id());
                p.setName(selectedProductDTO.name());
                p.setSku(selectedProductDTO.sku());

                inventoryService.registerMovement(
                        p,
                        selectedWarehouse,
                        qty,
                        "ADJUSTMENT",
                        batch,
                        serial,
                        desc
                );

                JOptionPane.showMessageDialog(this, "Ajuste de inventário registado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                onPanelSelected();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
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
        UIHelper.styleTextField(descField);
        UIHelper.styleComboBox(categoryCombo);

        java.util.List<com.phcpro.modules.comercial.dto.ProductCategoryDTO> categories =
                comercialService.getActiveCategories();
        categoryCombo.addItem("— Sem categoria —");
        for (var c : categories) categoryCombo.addItem(c.name() + "  (" + c.code() + ")");

        JPanel dialogPanel = UIHelper.createDialogForm(
                "SKU / Codigo (Unico):", skuField,
                "Referencia:", referenceField,
                "Codigo de Barras:", barcodeField,
                "Nome do Produto:", nameField,
                "Categoria:", categoryCombo,
                "Preço de Venda (MT):", salesPriceField,
                "Preço de Compra (MT):", purchasePriceField,
                "Stock Mínimo:", minStockField,
                "Unidades por Caixa:", unitsPerBoxField,
                "Descrição:", descField
        );

        int option = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(dialogPanel), "Cadastrar Novo Produto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
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
                comercialService.createProduct(
                        sku,
                        reference.isEmpty() ? null : reference,
                        barcode.isEmpty() ? null : barcode,
                        name,
                        salesPrice,
                        purchasePrice,
                        minStock,
                        unitsPerBox,
                        categoryId,
                        desc.isEmpty() ? null : desc);
                JOptionPane.showMessageDialog(this, "Produto '" + name + "' cadastrado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

                onPanelSelected();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Os valores de preço e stock mínimo devem ser numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao cadastrar produto: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createTransferDialog() {
        if (warehousesList.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "É necessário pelo menos 2 armazéns para realizar uma transferência.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ProductDTO> products = comercialService.getAllProducts();
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
        for (Warehouse w : warehousesList) {
            originCombo.addItem(w.getName());
            destinationCombo.addItem(w.getName());
        }
        if (warehousesList.size() > 1) destinationCombo.setSelectedIndex(1);

        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(responsibleField);
        UIHelper.styleTextField(vehicleField);
        UIHelper.styleTextField(notesField);

        String[] lineCols = {"Produto", "Quantidade"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 1; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);

        JComboBox<String> productEditorCombo = new JComboBox<>();
        for (ProductDTO p : products) productEditorCombo.addItem(p.name());
        linesTable.getColumnModel().getColumn(0)
                .setCellEditor(new DefaultCellEditor(productEditorCombo));

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(520, 180));

        ModernButton addLineBtn = new ModernButton("+ Linha", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        ModernButton removeLineBtn = new ModernButton("- Remover", new Color(220, 38, 38), new Color(248, 113, 113));
        addLineBtn.addActionListener(ev -> linesModel.addRow(new Object[]{products.get(0).name(), "1"}));
        removeLineBtn.addActionListener(ev -> {
            int sel = linesTable.getSelectedRow();
            if (sel >= 0) linesModel.removeRow(sel);
        });
        JPanel lineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineButtons.setOpaque(false);
        lineButtons.add(addLineBtn);
        lineButtons.add(removeLineBtn);

        linesModel.addRow(new Object[]{products.get(0).name(), "1"});

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

        int option = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(dialogPanel),
                "Nova Transferência de Stock", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;

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

        Warehouse origin = warehousesList.get(originIdx);
        Warehouse destination = warehousesList.get(destIdx);

        try {
            CreateStockTransferRequest request = new CreateStockTransferRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    origin.getId(),
                    destination.getId(),
                    responsibleField.getText().trim(),
                    vehicleField.getText().trim(),
                    notesField.getText().trim(),
                    lines
            );
            StockTransferDTO created = stockTransferService.create(request);
            onPanelSelected();

            int print = JOptionPane.showConfirmDialog(this,
                    "Transferência " + created.transferNumber() + " registada com sucesso.\n"
                            + "Deseja imprimir a Guia de Transferência agora?",
                    "Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (print == JOptionPane.YES_OPTION) {
                printTransfer(created.id(), created.transferNumber());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedTransfer() {
        int row = transferTable.getSelectedRow();
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
            byte[] pdf = stockTransferPrintService.render(transferId);
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
                Warehouse selectedWarehouse = warehousesList.get(idx - 1);
                warehouseId = selectedWarehouse.getId();
                fileSuffix = selectedWarehouse.getName()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
                if (fileSuffix.isBlank()) {
                    fileSuffix = "armazem-" + selectedWarehouse.getId();
                }
            }
        }

        try {
            byte[] pdf = inventoryReportPrintService.render(CurrentUserContext.getCurrentCompanyId(), warehouseId);
            PdfFileSaver.saveAndOpen(pdf, "inventario-stock-" + fileSuffix);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
