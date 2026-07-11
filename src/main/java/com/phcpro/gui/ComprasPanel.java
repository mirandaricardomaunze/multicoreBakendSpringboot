package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.purchases.dto.CreatePurchaseLineRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseOrderRequest;
import com.phcpro.modules.purchases.dto.PurchaseOrderDTO;
import com.phcpro.modules.purchases.dto.PurchaseOrderLineDTO;
import com.phcpro.modules.purchases.dto.ReceivePurchaseOrderRequest;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.model.Purchase;
import com.phcpro.modules.purchases.service.PurchaseOrderService;
import com.phcpro.modules.purchases.service.PurchaseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ComprasPanel extends JPanel {

    private final PurchaseService purchaseService;
    private final PurchaseOrderService purchaseOrderService;
    private final com.phcpro.modules.purchases.service.ReorderService reorderService;
    private final InventoryService inventoryService;

    // Reposição automática
    private JTabbedPane tabbedPane;
    private DefaultTableModel reorderModel;
    private JTable reorderTable;
    private JLabel reorderFooter;
    private java.util.List<com.phcpro.modules.purchases.dto.ReorderSuggestionDTO> reorderList = new java.util.ArrayList<>();
    private final ComercialService comercialService;
    private final FinanceService financeService;

    // TAB ENCOMENDAS A FORNECEDOR
    private JComboBox<String> poSupplierCombo;
    private JComboBox<String> poWarehouseCombo;
    private JComboBox<String> poProductCombo;
    private JTextField poQtyField;
    private JTextField poPriceField;
    private JTextField poExpectedField;
    private DefaultTableModel poLinesModel;
    private JTable poLinesTable;
    private JLabel poTotalLabel;
    private DefaultTableModel poListModel;
    private JTable poListTable;
    private JTextField poSearchField;
    private final List<CreatePurchaseOrderLineRequest> poDraftLines = new ArrayList<>();
    private List<PurchaseOrderDTO> poList = new ArrayList<>();
    private JPanel poFormContent;                   // conteúdo do modal de nova encomenda
    private JPanel purchaseFormContent;             // conteúdo do modal de registar compra
    private JTextField supplierSearchField;

    // TAB CONTAS A PAGAR
    private DefaultTableModel payablesModel;
    private JTable payablesTable;
    private JLabel payablesFooter;
    private List<com.phcpro.modules.purchases.dto.PayableDTO> payablesList = new ArrayList<>();

    // TAB 1: REGISTO COMPRA ELEMENTS
    private JComboBox<String> supplierCombo;
    private JComboBox<String> warehouseCombo;
    private JComboBox<String> accountCombo;
    private JComboBox<String> productCombo;
    private JTextField quantityField;
    private JTextField priceField;
    private JTextField batchField;
    private JTextField expirationField;
    private JTextField serialField;
    
    private DefaultTableModel draftLinesModel;
    private JTable draftLinesTable;
    private JLabel totalLabel;
    
    private DefaultTableModel purchasesModel;
    private JTable purchasesTable;

    // TAB 2: FORNECEDORES ELEMENTS
    // Suppliers form fields são criados dentro do modal — sem refs guardadas aqui
    
    private DefaultTableModel suppliersModel;
    private JTable suppliersTable;

    // Seeding lists
    private List<Supplier> suppliersList = new ArrayList<>();        // linhas da tabela (pode estar filtrada)
    private List<Supplier> supplierComboList = new ArrayList<>();     // activos, para combos de compra/encomenda
    private List<Warehouse> warehousesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();
    private List<ProductDTO> productsList = new ArrayList<>();

    // Draft items
    private final List<CreatePurchaseLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftTotal = BigDecimal.ZERO;

    public ComprasPanel(
            PurchaseService purchaseService,
            PurchaseOrderService purchaseOrderService,
            com.phcpro.modules.purchases.service.ReorderService reorderService,
            InventoryService inventoryService,
            ComercialService comercialService,
            FinanceService financeService
    ) {
        this.purchaseService = purchaseService;
        this.purchaseOrderService = purchaseOrderService;
        this.reorderService = reorderService;
        this.inventoryService = inventoryService;
        this.comercialService = comercialService;
        this.financeService = financeService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabbedPane);

        // Tab 1: Compras
        JPanel tabCompras = createComprasTab();
        tabbedPane.addTab("Faturas de Compra (V/FT)", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT), tabCompras);

        // Tab: Reposição automática (produtos abaixo do mínimo)
        tabbedPane.addTab("Reposição", UIHelper.icon("fas-cart-arrow-down", 16, UIHelper.TEXT_LIGHT), createReorderTab());

        // Tab 2: Encomendas a Fornecedor
        tabbedPane.addTab("Encomendas a Fornecedor (EC-F)", UIHelper.icon("fas-clipboard-list", 16, UIHelper.TEXT_LIGHT), createPurchaseOrdersTab());

        // Tab 3: Contas a Pagar
        tabbedPane.addTab("Contas a Pagar", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.TEXT_LIGHT), createPayablesTab());

        // Tab 4: Fornecedores
        JPanel tabFornecedores = createFornecedoresTab();
        tabbedPane.addTab("Gestão de Fornecedores", UIHelper.icon("fas-truck-loading", 16, UIHelper.TEXT_LIGHT), tabFornecedores);

        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
    }

    private JPanel createComprasTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Host do formulário (vai para modal): transparente e a acompanhar a largura do viewport.
        VScrollForm formCard = new VScrollForm(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 0: Fornecedor & Armazém (Side by Side)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel supLbl = new JLabel("Fornecedor:");
        supLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(supLbl, gbc);

        gbc.gridx = 1;
        JLabel whLbl = new JLabel("Armazém de Destino:");
        whLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(whLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(2, 8, 12, 8);
        supplierCombo = new JComboBox<>();
        UIHelper.styleComboBox(supplierCombo);
        formCard.add(supplierCombo, gbc);

        gbc.gridx = 1;
        warehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(warehouseCombo);
        formCard.add(warehouseCombo, gbc);

        // Row 1: Conta & Artigo (Side by Side)
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel accLbl = new JLabel("Conta de Pagamento:");
        accLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(accLbl, gbc);

        gbc.gridx = 1;
        JLabel prodLbl = new JLabel("Artigo / Produto:");
        prodLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(prodLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.insets = new Insets(2, 8, 12, 8);
        accountCombo = new JComboBox<>();
        UIHelper.styleComboBox(accountCombo);
        formCard.add(accountCombo, gbc);

        gbc.gridx = 1;
        productCombo = new JComboBox<>();
        UIHelper.styleComboBox(productCombo);
        productCombo.addActionListener(e -> updateDefaultPrice());
        formCard.add(productCombo, gbc);

        // Row 2: Quantidade & Preço Custo (Side by Side)
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel qtyLbl = new JLabel("Quantidade:");
        qtyLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(qtyLbl, gbc);

        gbc.gridx = 1;
        JLabel priceLbl = new JLabel("Preço Unit. Custo (MT):");
        priceLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(priceLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.insets = new Insets(2, 8, 12, 8);
        quantityField = new JTextField("1");
        UIHelper.styleTextField(quantityField);
        formCard.add(quantityField, gbc);

        gbc.gridx = 1;
        priceField = new JTextField();
        UIHelper.styleTextField(priceField);
        formCard.add(priceField, gbc);

        // Row 3: Lote e Série (Side by Side)
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel batchLbl = new JLabel("Nº Lote (Opcional):");
        batchLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(batchLbl, gbc);

        gbc.gridx = 1;
        JLabel serialLbl = new JLabel("Nº Série (Opcional):");
        serialLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(serialLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.insets = new Insets(2, 8, 12, 8);
        batchField = new JTextField();
        UIHelper.styleTextField(batchField);
        formCard.add(batchField, gbc);

        gbc.gridx = 1;
        serialField = new JTextField();
        UIHelper.styleTextField(serialField);
        formCard.add(serialField, gbc);

        // Row 3b: Validade do Lote (Full Width)
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel expLbl = new JLabel("Validade do Lote (yyyy-MM-dd):");
        expLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(expLbl, gbc);

        gbc.gridy = 9;
        gbc.insets = new Insets(2, 8, 12, 8);
        expirationField = new JTextField();
        UIHelper.styleTextField(expirationField);
        formCard.add(expirationField, gbc);

        // Row 4: line action
        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 8, 12, 8);
        ModernButton addLineBtn = UIHelper.createAddLineButton();
        JPanel addLineActionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addLineActionRow.setOpaque(false);
        addLineActionRow.add(addLineBtn);
        formCard.add(addLineActionRow, gbc);

        // Row 5: Draft Table (Full Width)
        gbc.gridy = 11; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        String[] cols = {"Produto", "Qtd", "Preço Custo", "Lote/Série", "Subtotal"};
        draftLinesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        draftLinesTable = new JTable(draftLinesModel);
        UIHelper.styleTable(draftLinesTable);
        JScrollPane scroll = new JScrollPane(draftLinesTable);
        UIHelper.styleEmbeddedTableScrollPane(scroll, draftLinesTable, 4);
        formCard.add(scroll, gbc);

        // Row 6: Checkout purchase (Full Width)
        gbc.gridy = 12; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        totalLabel = new JLabel("Total Compra: 0.00 MT (excl. IVA)");
        totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        totalLabel.setForeground(Color.WHITE);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.add(totalLabel, BorderLayout.EAST);

        bottomPanel.add(totalRow);
        formCard.add(bottomPanel, gbc);

        // Conteúdo do formulário vai para o modal responsivo (com scroll).
        this.purchaseFormContent = formCard;

        // TAB: cabeçalho com acção + histórico de compras em ecrã inteiro.
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        headerBar.add(UIHelper.createHeading("Faturas de Compra Registadas"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton newPurchaseBtn = UIHelper.createSuccessButton("Registar Compra…");
        newPurchaseBtn.setIcon(UIHelper.icon("fas-download", 14));
        newPurchaseBtn.addActionListener(e -> openPurchaseFormDialog());
        headerActions.add(newPurchaseBtn);
        headerBar.add(headerActions, BorderLayout.EAST);
        panel.add(headerBar, BorderLayout.NORTH);

        ModernPanel historyCard = new ModernPanel(16);
        historyCard.setLayout(new BorderLayout(0, 10));
        historyCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] historyCols = {"Nº Documento", "Fornecedor", "Armazém", "Total Faturado", "Imposto", "Data"};
        purchasesModel = new DefaultTableModel(historyCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        purchasesTable = new JTable(purchasesModel);
        UIHelper.styleTable(purchasesTable);
        JScrollPane histScroll = new JScrollPane(purchasesTable);
        UIHelper.styleScrollPane(histScroll);

        JTextField histSearch = TableFilter.searchField("Nº doc, fornecedor ou armazém…");
        JComboBox<String> histPeriodo = TableFilter.periodCombo();
        TableFilter.install(purchasesTable, histSearch,
                java.util.List.of(),
                java.util.List.of(new TableFilter.PeriodFilter(histPeriodo, 5)));
        JPanel histBar = TableFilter.bar(histSearch,
                TableFilter.label("Data:", "fas-calendar-alt"), histPeriodo);
        histBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        historyCard.add(histBar, BorderLayout.NORTH);
        historyCard.add(histScroll, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionRow.setOpaque(false);
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar Compras");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        actionRow.add(refreshBtn);
        historyCard.add(actionRow, BorderLayout.SOUTH);

        // Histórico de compras ocupa a tab inteira; o formulário vive no modal.
        panel.add(historyCard, BorderLayout.CENTER);

        // LISTENERS
        addLineBtn.addActionListener(e -> addDraftLine());
        refreshBtn.addActionListener(e -> loadPurchasesHistory());

        return panel;
    }

    private void openPurchaseFormDialog() {
        if (supplierComboList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre um fornecedor activo primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Reset do rascunho ao abrir.
        draftLines.clear();
        draftTotal = BigDecimal.ZERO;
        if (draftLinesModel != null) draftLinesModel.setRowCount(0);
        totalLabel.setText("Total Compra: 0.00 MT (excl. IVA)");
        Window parent = SwingUtilities.getWindowAncestor(this);
        ModernFormDialog dlg = new ModernFormDialog(parent, "Registar Compra (Entrada de Stock)", purchaseFormContent);
        dlg.setSize(900, 680);
        dlg.setOnSave(this::submitPurchaseOrThrow);
        if (dlg.showDialog()) {
            loadPurchasesHistory();
            loadAccounts();
            loadPayables();
        }
    }

    private JPanel createFornecedoresTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header: title + action buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Fornecedores Cadastrados"), BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton editSupBtn = UIHelper.createSecondaryButton("Editar");
        editSupBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton toggleSupBtn = UIHelper.createSecondaryButton("Activar/Desactivar");
        toggleSupBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        ModernButton refreshSupsBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshSupsBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        ModernButton newSupBtn = UIHelper.createSuccessButton("Novo Fornecedor");
        newSupBtn.setIcon(UIHelper.icon("fas-plus", 14));
        headerActions.add(editSupBtn);
        headerActions.add(toggleSupBtn);
        headerActions.add(refreshSupsBtn);
        headerActions.add(newSupBtn);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Table full-width
        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] supCols = {"Nome do Fornecedor", "NUIT/NIF", "Telefone", "Contacto", "Correio Eletrónico", "Endereço", "Estado"};
        suppliersModel = new DefaultTableModel(supCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        suppliersTable = new JTable(suppliersModel);
        UIHelper.styleTable(suppliersTable);
        JScrollPane scroll = new JScrollPane(suppliersTable);
        UIHelper.styleScrollPane(scroll);

        supplierSearchField = TableFilter.searchField("Nome ou NUIT…");
        JComboBox<String> supEstado = TableFilter.combo("Todos os estados", "Activo", "Inactivo");
        TableFilter.install(suppliersTable, supplierSearchField,
                new TableFilter.ColumnFilter(supEstado, 6));
        JPanel supBar = TableFilter.bar(supplierSearchField,
                TableFilter.label("Estado:"), supEstado);
        supBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(supBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        refreshSupsBtn.addActionListener(e -> { supplierSearchField.setText(""); loadSuppliers(); });
        newSupBtn.addActionListener(e -> openSupplierDialog(null));
        editSupBtn.addActionListener(e -> {
            Supplier sel = selectedSupplier();
            if (sel != null) openSupplierDialog(sel);
        });
        toggleSupBtn.addActionListener(e -> toggleSelectedSupplier());

        return panel;
    }

    private Supplier selectedSupplier() {
        int row = TableFilter.selectedModelRow(suppliersTable);
        if (row < 0 || row >= suppliersList.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return suppliersList.get(row);
    }

    private void toggleSelectedSupplier() {
        Supplier sel = selectedSupplier();
        if (sel == null) return;
        try {
            purchaseService.setSupplierActive(sel.getId(), CurrentUserContext.getCurrentCompanyId(), !sel.isActive());
            loadSuppliers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSupplierDialog(Supplier existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.getName() : "");
        JTextField taxIdField = new JTextField(editing ? existing.getTaxId() : "");
        JTextField phoneField = new JTextField(editing && existing.getPhone() != null ? existing.getPhone() : "");
        JTextField contactField = new JTextField(editing && existing.getContactPerson() != null ? existing.getContactPerson() : "");
        JTextField emailField = new JTextField(editing && existing.getEmail() != null ? existing.getEmail() : "");
        JTextField addressField = new JTextField(editing && existing.getAddress() != null ? existing.getAddress() : "");

        JPanel form = UIHelper.createDialogForm(
                "Nome / Empresa:", nameField,
                "NUIT / NIF (9 dígitos):", taxIdField,
                "Telefone:", phoneField,
                "Pessoa de Contacto:", contactField,
                "Correio Eletrónico:", emailField,
                "Endereço:", addressField
        );

        Window parent = SwingUtilities.getWindowAncestor(this);
        ModernFormDialog dlg = new ModernFormDialog(parent, editing ? "Editar Fornecedor" : "Novo Fornecedor", form);
        dlg.setSize(520, 480);
        dlg.setOnSave(() -> {
            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            if (name.isEmpty() || taxId.isEmpty()) {
                throw new RuntimeException("Nome e NUIT/NIF são campos obrigatórios.");
            }
            com.phcpro.modules.purchases.dto.CreateSupplierRequest req =
                    new com.phcpro.modules.purchases.dto.CreateSupplierRequest(
                            name, taxId,
                            emailField.getText().trim(),
                            addressField.getText().trim(),
                            phoneField.getText().trim(),
                            contactField.getText().trim(),
                            CurrentUserContext.getCurrentCompanyId());
            if (editing) {
                purchaseService.updateSupplier(existing.getId(), req);
            } else {
                purchaseService.createSupplier(req);
            }
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this,
                    "Fornecedor '" + nameField.getText().trim() + (editing ? "' actualizado." : "' registado."),
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadSuppliers();
        }
    }

    public void onPanelSelected() {
        loadSuppliers();
        loadWarehouses();
        loadAccounts();
        loadProducts();
        loadPurchasesHistory();
        refreshPoCombos();
        loadPurchaseOrders();
        loadPayables();
        loadReorderSuggestions();
    }

    // ===== Reposição automática =====

    private JPanel createReorderTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Reposição Automática (abaixo do mínimo)"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton orderBtn = UIHelper.createSuccessButton("Criar Encomenda");
        orderBtn.setIcon(UIHelper.icon("fas-clipboard-list", 14));
        orderBtn.setToolTipText("Abre a aba Encomendas a Fornecedor para encomendar os produtos em falta.");
        orderBtn.addActionListener(e -> tabbedPane.setSelectedIndex(2)); // Encomendas a Fornecedor
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadReorderSuggestions());
        actions.add(refreshBtn); actions.add(orderBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Produto", "SKU", "Stock Atual", "Mínimo", "Und/Caixa", "Sugerido (caixas)", "Sugerido (unidades)", "Estado"};
        reorderModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        reorderTable = new JTable(reorderModel);
        UIHelper.styleTable(reorderTable);
        JScrollPane scroll = new JScrollPane(reorderTable);
        UIHelper.styleScrollPane(scroll);

        JTextField reorderSearch = TableFilter.searchField("Produto ou SKU…");
        JComboBox<String> reorderEstado = TableFilter.combo("Todos os estados", "ESGOTADO", "BAIXO");
        TableFilter.install(reorderTable, reorderSearch,
                new TableFilter.ColumnFilter(reorderEstado, 7));
        JPanel reorderBar = TableFilter.bar(reorderSearch,
                TableFilter.label("Estado:"), reorderEstado);
        reorderBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(reorderBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        reorderFooter = new JLabel(" ");
        reorderFooter.setForeground(UIHelper.TEXT_MUTED);
        reorderFooter.setBorder(new EmptyBorder(8, 4, 0, 4));
        card.add(reorderFooter, BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadReorderSuggestions() {
        if (reorderModel == null) return;
        reorderList = reorderService.suggestions(CurrentUserContext.getCurrentCompanyId());
        reorderModel.setRowCount(0);
        for (var s : reorderList) {
            String estado = s.currentStock().signum() <= 0 ? "ESGOTADO" : "BAIXO";
            reorderModel.addRow(new Object[]{
                    s.name(), s.sku(),
                    String.format("%,.3f", s.currentStock()),
                    String.format("%,.3f", s.minStock()),
                    s.unitsPerBox(),
                    String.format("%,.0f", s.suggestedBoxes()),
                    String.format("%,.0f", s.suggestedUnits()),
                    estado});
        }
        reorderFooter.setText(reorderList.isEmpty()
                ? "Sem reposições pendentes — todo o stock está acima do mínimo."
                : String.format("%d produto(s) a repor.", reorderList.size()));
    }

    // ===== Contas a Pagar =====

    private JPanel createPayablesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Contas a Pagar a Fornecedores"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton payBtn = UIHelper.createSuccessButton("Registar Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> openSupplierPaymentDialog());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadPayables());
        actions.add(refreshBtn); actions.add(payBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Nº Compra", "Fornecedor", "Total", "Pago", "Em Dívida", "Data"};
        payablesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payablesTable = new JTable(payablesModel);
        UIHelper.styleTable(payablesTable);
        JScrollPane scroll = new JScrollPane(payablesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField paySearch = TableFilter.searchField("Nº compra ou fornecedor…");
        JComboBox<String> payPeriodo = TableFilter.periodCombo();
        TableFilter.install(payablesTable, paySearch,
                java.util.List.of(),
                java.util.List.of(new TableFilter.PeriodFilter(payPeriodo, 5)));
        JPanel payBar = TableFilter.bar(paySearch,
                TableFilter.label("Data:", "fas-calendar-alt"), payPeriodo);
        payBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(payBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        payablesFooter = new JLabel(" ");
        payablesFooter.setForeground(UIHelper.TEXT_MUTED);
        payablesFooter.setBorder(new EmptyBorder(8, 4, 0, 4));
        card.add(payablesFooter, BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadPayables() {
        if (payablesModel == null) return;
        payablesList = purchaseService.findPayablesByCompany(CurrentUserContext.getCurrentCompanyId());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        BigDecimal totalDivida = BigDecimal.ZERO;
        payablesModel.setRowCount(0);
        for (var pa : payablesList) {
            totalDivida = totalDivida.add(pa.outstanding());
            payablesModel.addRow(new Object[]{
                    pa.purchaseNumber(), pa.supplierName(),
                    String.format("%,.2f MT", pa.totalAmount()),
                    String.format("%,.2f MT", pa.amountPaid()),
                    String.format("%,.2f MT", pa.outstanding()),
                    pa.purchaseDate() == null ? "-" : pa.purchaseDate().format(dtf)});
        }
        payablesFooter.setText(String.format("%d fatura(s) em dívida · Total a pagar: %,.2f MT",
                payablesList.size(), totalDivida));
    }

    private void openSupplierPaymentDialog() {
        int row = TableFilter.selectedModelRow(payablesTable);
        if (row < 0 || row >= payablesList.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma conta a pagar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var pa = payablesList.get(row);
        if (accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Falta cadastrar contas de tesouraria.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JComboBox<String> accCombo = new JComboBox<>();
        for (TreasuryAccountDTO a : accountsList) accCombo.addItem(a.name());
        UIHelper.styleComboBox(accCombo);
        JTextField amountField = new JTextField(pa.outstanding().toPlainString());
        UIHelper.styleTextField(amountField);
        JTextField refField = new JTextField();
        UIHelper.styleTextField(refField);

        JLabel info = new JLabel(String.format(
                "<html><b>Compra:</b> %s · <b>Fornecedor:</b> %s<br><b>Em dívida:</b> %,.2f MT</html>",
                pa.purchaseNumber(), pa.supplierName(), pa.outstanding()));
        info.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Resumo:", info,
                "Conta de Tesouraria:", accCombo,
                "Valor a Pagar (MT):", amountField,
                "Referência:", refField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Pagar a Fornecedor — " + pa.purchaseNumber(), "fas-money-bill-wave", "Liquidação de compra a crédito", form)
                .setConfirmButton("Pagar", "fas-money-bill-wave").showDialog();
        if (!confirmed) return;
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            Long accountId = accountsList.get(accCombo.getSelectedIndex()).id();
            String ref = refField.getText().trim();
            purchaseService.registerSupplierPayment(pa.purchaseId(), amount, accountId, ref.isEmpty() ? null : ref);
            JOptionPane.showMessageDialog(this, "Pagamento registado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadPayables();
            loadPurchasesHistory();
            loadAccounts();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshPoCombos() {
        if (poWarehouseCombo == null) return;
        poWarehouseCombo.removeAllItems();
        for (Warehouse w : warehousesList) poWarehouseCombo.addItem(w.getName());
        poProductCombo.removeAllItems();
        for (ProductDTO p : productsList) poProductCombo.addItem(p.name() + " (" + p.sku() + ")");
    }

    // ===== Encomendas a Fornecedor =====

    private JPanel createPurchaseOrdersTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        // ---- formulário (topo) ----
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(4, 8, 4, 8); g.weightx = 1;

        poSupplierCombo = new JComboBox<>(); UIHelper.styleComboBox(poSupplierCombo);
        poWarehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(poWarehouseCombo);
        poProductCombo = new JComboBox<>(); UIHelper.styleComboBox(poProductCombo);
        poQtyField = new JTextField("1"); UIHelper.styleTextField(poQtyField);
        poPriceField = new JTextField("0"); UIHelper.styleTextField(poPriceField);
        poExpectedField = new JTextField(); UIHelper.styleTextField(poExpectedField);
        poExpectedField.setToolTipText("Data prevista de entrega (aaaa-MM-dd) — opcional");

        g.gridx = 0; g.gridy = 0; g.weightx = 0.5; formCard.add(label("Fornecedor:"), g);
        g.gridx = 1; formCard.add(label("Armazém de destino:"), g);
        g.gridx = 0; g.gridy = 1; formCard.add(poSupplierCombo, g);
        g.gridx = 1; formCard.add(poWarehouseCombo, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.weightx = 1; formCard.add(label("Produto:"), g);
        g.gridy = 3; formCard.add(poProductCombo, g);
        g.gridwidth = 1; g.weightx = 0.33;
        g.gridx = 0; g.gridy = 4; formCard.add(label("Qtd:"), g);
        g.gridx = 1; formCard.add(label("Preço unit. (compra):"), g);
        g.gridx = 2; formCard.add(label("Entrega prevista:"), g);
        g.gridx = 0; g.gridy = 5; formCard.add(poQtyField, g);
        g.gridx = 1; formCard.add(poPriceField, g);
        g.gridx = 2; formCard.add(poExpectedField, g);

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        addLineBtn.addActionListener(e -> addPoDraftLine());
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); addRow.setOpaque(false);
        addRow.add(addLineBtn);
        g.gridx = 0; g.gridy = 6; g.gridwidth = 3; g.weightx = 1; g.insets = new Insets(10, 8, 4, 8);
        formCard.add(addRow, g);

        // ---- linhas (rascunho) ----
        String[] lineCols = {"Produto", "Qtd", "Preço Unit.", "Lote", "Validade", "Total"};
        poLinesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        poLinesTable = new JTable(poLinesModel);
        UIHelper.styleTable(poLinesTable);
        JScrollPane linesScroll = new JScrollPane(poLinesTable);
        UIHelper.styleScrollPane(linesScroll);

        poTotalLabel = new JLabel("Total da Encomenda: 0.00 MT");
        poTotalLabel.setForeground(Color.WHITE);
        poTotalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        JPanel poFooter = new JPanel(new BorderLayout()); poFooter.setOpaque(false);
        poFooter.setBorder(new EmptyBorder(8, 0, 0, 0));
        poFooter.add(poTotalLabel, BorderLayout.WEST);

        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        draftCard.add(UIHelper.createSubheading("Linhas da Encomenda"), BorderLayout.NORTH);
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(poFooter, BorderLayout.SOUTH);

        // Conteúdo do formulário (modal responsivo): inputs + linhas.
        JPanel poFormContentPanel = new JPanel(new BorderLayout(0, 10));
        poFormContentPanel.setOpaque(false);
        poFormContentPanel.add(formCard, BorderLayout.NORTH);
        poFormContentPanel.add(draftCard, BorderLayout.CENTER);
        this.poFormContent = poFormContentPanel;

        // ---- lista de encomendas (base) ----
        JPanel listHeader = new JPanel(new BorderLayout(8, 0)); listHeader.setOpaque(false);
        listHeader.add(UIHelper.createHeading("Encomendas Registadas"), BorderLayout.WEST);
        JPanel listActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); listActions.setOpaque(false);
        ModernButton receiveBtn = UIHelper.createSuccessButton("Receber");
        receiveBtn.setIcon(UIHelper.icon("fas-dolly", 14));
        receiveBtn.addActionListener(e -> receiveSelectedPO());
        ModernButton receivePartialBtn = UIHelper.createPrimaryButton("Receber Parcial…");
        receivePartialBtn.setIcon(UIHelper.icon("fas-dolly-flatbed", 14));
        receivePartialBtn.addActionListener(e -> receivePartialSelectedPO());
        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        cancelBtn.addActionListener(e -> cancelSelectedPO());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> { poSearchField.setText(""); loadPurchaseOrders(); });
        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Nova Encomenda…");
        newOrderBtn.setIcon(UIHelper.icon("fas-clipboard-check", 14));
        newOrderBtn.addActionListener(e -> openPurchaseOrderFormDialog());
        listActions.add(receiveBtn); listActions.add(receivePartialBtn); listActions.add(cancelBtn); listActions.add(refreshBtn);
        listActions.add(newOrderBtn);
        listHeader.add(listActions, BorderLayout.EAST);

        String[] cols = {"Nº", "Fornecedor", "Estado", "Total", "Data", "Entrega prev."};
        poListModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        poListTable = new JTable(poListModel);
        UIHelper.styleTable(poListTable);
        JScrollPane listScroll = new JScrollPane(poListTable);
        UIHelper.styleScrollPane(listScroll);

        poSearchField = TableFilter.searchField("Nº ou fornecedor…");
        JComboBox<String> poEstado = TableFilter.combo("Todos os estados",
                "ORDERED", "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED");
        JComboBox<String> poPeriodo = TableFilter.periodCombo();
        TableFilter.install(poListTable, poSearchField,
                java.util.List.of(new TableFilter.ColumnFilter(poEstado, 2)),
                java.util.List.of(new TableFilter.PeriodFilter(poPeriodo, 4)));
        JPanel poBar = TableFilter.bar(poSearchField,
                TableFilter.label("Estado:"), poEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), poPeriodo);
        poBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        listHeader.add(poBar, BorderLayout.SOUTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        listCard.add(listHeader, BorderLayout.NORTH);
        listCard.add(listScroll, BorderLayout.CENTER);

        // Lista de encomendas ocupa a tab inteira; o formulário vive no modal.
        tab.add(listCard, BorderLayout.CENTER);
        return tab;
    }

    private void openPurchaseOrderFormDialog() {
        if (supplierComboList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre um fornecedor activo primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Reset do rascunho ao abrir.
        poDraftLines.clear();
        if (poLinesModel != null) poLinesModel.setRowCount(0);
        recomputePoTotal();
        poExpectedField.setText("");
        Window parent = SwingUtilities.getWindowAncestor(this);
        ModernFormDialog dlg = new ModernFormDialog(parent, "Nova Encomenda a Fornecedor", poFormContent);
        dlg.setSize(880, 640);
        dlg.setOnSave(this::submitPurchaseOrderOrThrow);
        if (dlg.showDialog()) {
            loadPurchaseOrders();
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private void addPoDraftLine() {
        int prodIdx = poProductCombo.getSelectedIndex();
        if (prodIdx < 0 || prodIdx >= productsList.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BigDecimal qty = new BigDecimal(poQtyField.getText().trim());
            BigDecimal price = new BigDecimal(poPriceField.getText().trim());
            if (qty.signum() <= 0 || price.signum() < 0) throw new NumberFormatException();
            ProductDTO product = productsList.get(prodIdx);
            poDraftLines.add(new CreatePurchaseOrderLineRequest(
                    product.id(), qty, price, null, null, null));
            poLinesModel.addRow(new Object[]{
                    product.name(), qty.toPlainString(),
                    String.format("%,.2f", price), "-", "-",
                    String.format("%,.2f MT", qty.multiply(price))});
            recomputePoTotal();
            poQtyField.setText("1"); poPriceField.setText("0");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade/preço inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recomputePoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CreatePurchaseOrderLineRequest l : poDraftLines) {
            total = total.add(l.quantity().multiply(l.unitPrice()));
        }
        poTotalLabel.setText(String.format("Total da Encomenda: %,.2f MT", total));
    }

    /** Validação + criação da encomenda. Lança RuntimeException em erro (mantém o modal aberto). */
    private void submitPurchaseOrderOrThrow() {
        int supIdx = poSupplierCombo.getSelectedIndex();
        int whIdx = poWarehouseCombo.getSelectedIndex();
        if (supIdx < 0 || supIdx >= supplierComboList.size() || whIdx < 0 || whIdx >= warehousesList.size()) {
            throw new RuntimeException("Selecione fornecedor e armazém.");
        }
        if (poDraftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos uma linha.");
        }
        LocalDate expected = null;
        String t = poExpectedField.getText().trim();
        if (!t.isEmpty()) {
            try {
                expected = LocalDate.parse(t);
            } catch (DateTimeParseException ex) {
                throw new RuntimeException("Data de entrega inválida (aaaa-MM-dd).");
            }
        }
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest(
                supplierComboList.get(supIdx).getId(),
                warehousesList.get(whIdx).getId(),
                CurrentUserContext.getCurrentCompanyId(),
                expected, null, new ArrayList<>(poDraftLines));
        PurchaseOrderDTO dto = purchaseOrderService.createOrder(req);
        poDraftLines.clear();
        if (poLinesModel != null) poLinesModel.setRowCount(0);
        recomputePoTotal();
        poExpectedField.setText("");
        JOptionPane.showMessageDialog(this, "Encomenda " + dto.orderNumber() + " criada.",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadPurchaseOrders() {
        if (poListModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        // Carrega todas; a pesquisa/estado/data é aplicada pelo TableFilter (cliente).
        poList = purchaseOrderService.findOrdersByCompany(companyId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        poListModel.setRowCount(0);
        for (PurchaseOrderDTO o : poList) {
            poListModel.addRow(new Object[]{
                    o.orderNumber(), o.supplierName(), o.status(),
                    String.format("%,.2f MT", o.totalAmount() == null ? BigDecimal.ZERO : o.totalAmount()),
                    o.orderDate() == null ? "-" : o.orderDate().format(dtf),
                    o.expectedDate() == null ? "-" : o.expectedDate().toString()});
        }
    }

    private PurchaseOrderDTO selectedPO() {
        int row = TableFilter.selectedModelRow(poListTable);
        if (row < 0 || row >= poList.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return poList.get(row);
    }

    private void receiveSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Receber a encomenda " + sel.orderNumber() + "? O stock do armazém será actualizado.",
                "Confirmar Recepção", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            purchaseOrderService.receiveOrder(sel.id());
            JOptionPane.showMessageDialog(this, "Encomenda recebida e stock actualizado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadPurchaseOrders();
            loadPurchasesHistory();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receivePartialSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!"ORDERED".equals(sel.status()) && !"PARTIALLY_RECEIVED".equals(sel.status())) {
            JOptionPane.showMessageDialog(this, "Só encomendas por receber podem ser recebidas.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tabela: Produto | Encomendado | Recebido | Em falta | A receber agora (editável).
        String[] cols = {"Produto", "Encomendado", "Recebido", "Em falta", "A receber agora"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        List<PurchaseOrderLineDTO> lines = sel.lines();
        for (PurchaseOrderLineDTO l : lines) {
            BigDecimal outstanding = l.outstandingQuantity();
            model.addRow(new Object[]{
                    l.productName(),
                    l.quantity().toPlainString(),
                    l.receivedQuantity().toPlainString(),
                    outstanding.toPlainString(),
                    outstanding.toPlainString() // pré-preenche com o em falta
            });
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 220));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(new JLabel("Encomenda " + sel.orderNumber() + " — indique a quantidade a receber agora:"),
                BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(this, panel, "Recepção Parcial",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        List<ReceivePurchaseOrderRequest.ReceiveLine> toReceive = new ArrayList<>();
        try {
            for (int i = 0; i < lines.size(); i++) {
                String raw = String.valueOf(model.getValueAt(i, 4)).trim().replace(',', '.');
                if (raw.isEmpty()) continue;
                BigDecimal qty = new BigDecimal(raw);
                if (qty.signum() > 0) {
                    toReceive.add(new ReceivePurchaseOrderRequest.ReceiveLine(lines.get(i).id(), qty));
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (toReceive.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique pelo menos uma quantidade a receber.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            PurchaseOrderDTO updated = purchaseOrderService.receivePartial(
                    sel.id(), new ReceivePurchaseOrderRequest(toReceive));
            JOptionPane.showMessageDialog(this,
                    "Recepção registada. Estado da encomenda: " + updated.status() + ".",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadPurchaseOrders();
            loadPurchasesHistory();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        String reason = UIHelper.promptRequiredText("Cancelar Encomenda", "fas-ban",
                "Encomenda " + sel.orderNumber(), "Motivo do cancelamento:");
        if (reason == null) return;
        try {
            purchaseOrderService.cancelOrder(sel.id(), reason);
            loadPurchaseOrders();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSuppliers() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<Supplier> all = purchaseService.getSuppliersByCompany(companyId);

        // Combos de compra/encomenda: só fornecedores activos.
        supplierComboList = all.stream().filter(Supplier::isActive).toList();
        supplierCombo.removeAllItems();
        for (Supplier s : supplierComboList) supplierCombo.addItem(s.getName() + " (" + s.getTaxId() + ")");
        if (poSupplierCombo != null) {
            poSupplierCombo.removeAllItems();
            for (Supplier s : supplierComboList) poSupplierCombo.addItem(s.getName() + " (" + s.getTaxId() + ")");
        }

        // Tabela: carrega todos; a pesquisa/estado é aplicada pelo TableFilter (cliente).
        suppliersList = all;
        suppliersModel.setRowCount(0);
        for (Supplier s : suppliersList) {
            suppliersModel.addRow(new Object[]{
                    s.getName(),
                    s.getTaxId(),
                    dash(s.getPhone()),
                    dash(s.getContactPerson()),
                    dash(s.getEmail()),
                    dash(s.getAddress()),
                    s.isActive() ? "Activo" : "Inactivo"
            });
        }
    }

    private static String dash(String v) {
        return v != null && !v.isBlank() ? v : "-";
    }

    private void loadWarehouses() {
        warehouseCombo.removeAllItems();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        warehousesList = inventoryService.getWarehousesByCompany(companyId);

        for (Warehouse w : warehousesList) {
            warehouseCombo.addItem(w.getName());
        }
    }

    private void loadAccounts() {
        accountCombo.removeAllItems();
        accountsList = financeService.getAllAccounts();

        accountCombo.addItem("— A crédito (pagar depois) —");
        for (TreasuryAccountDTO acc : accountsList) {
            accountCombo.addItem(acc.name() + " (" + String.format("%.2f", acc.balance()) + " MT)");
        }
    }

    private void loadProducts() {
        productCombo.removeAllItems();
        productsList = comercialService.getAllProducts();

        for (ProductDTO p : productsList) {
            productCombo.addItem(productLabel(p));
        }
        updateDefaultPrice();
    }

    private String productLabel(ProductDTO p) {
        String code = p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        if (p.barcode() != null && !p.barcode().isBlank()) {
            return code + " | " + p.barcode() + " - " + p.name();
        }
        return code + " - " + p.name();
    }

    private void updateDefaultPrice() {
        int idx = productCombo.getSelectedIndex();
        if (idx >= 0 && idx < productsList.size()) {
            // Purchases price is typically empty or less than unit sale price
            // Pre-fill with a reasonable cost (e.g. 60% of unit price)
            BigDecimal sellPrice = productsList.get(idx).unitPrice();
            BigDecimal costPrice = sellPrice.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
            priceField.setText(costPrice.toString());
        }
    }

    private void addDraftLine() {
        if (productsList.isEmpty()) return;
        int prodIdx = productCombo.getSelectedIndex();
        if (prodIdx < 0) return;

        ProductDTO product = productsList.get(prodIdx);
        
        BigDecimal qty;
        try {
            qty = new BigDecimal(quantityField.getText().trim());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser um número superior a zero.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim());
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "O preço de custo deve ser maior ou igual a zero.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;

        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        String expRaw = expirationField.getText().trim();
        if (expRaw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Validade do lote é obrigatória (formato yyyy-MM-dd).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(expRaw);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Validade inválida. Use o formato yyyy-MM-dd (ex: 2026-12-31).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CreatePurchaseLineRequest line = new CreatePurchaseLineRequest(
                product.id(),
                qty,
                price,
                batch,
                expirationDate,
                serial
        );
        draftLines.add(line);

        BigDecimal subtotal = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        draftTotal = draftTotal.add(subtotal);

        String lotSer = "";
        if (batch != null) lotSer += "L: " + batch + " ";
        lotSer += "V: " + expirationDate + " ";
        if (serial != null) lotSer += "S: " + serial;

        draftLinesModel.addRow(new Object[]{
                product.name(),
                qty,
                price + " MT",
                lotSer.trim(),
                subtotal + " MT"
        });

        totalLabel.setText(String.format("Total Compra: %,.2f MT (excl. IVA)", draftTotal));

        // Reset details
        quantityField.setText("1");
        batchField.setText("");
        expirationField.setText("");
        serialField.setText("");
        updateDefaultPrice();
    }

    /** Validação + registo da compra. Lança RuntimeException em erro (mantém o modal aberto). */
    private void submitPurchaseOrThrow() {
        if (warehousesList.isEmpty()) {
            throw new RuntimeException("Falta cadastrar armazéns.");
        }
        if (draftLines.isEmpty()) {
            throw new RuntimeException("Nenhum produto adicionado à compra.");
        }
        int supIdx = supplierCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        int accIdx = accountCombo.getSelectedIndex();
        if (supIdx < 0 || supIdx >= supplierComboList.size() || whIdx < 0) {
            throw new RuntimeException("Selecione fornecedor e armazém.");
        }

        Supplier supplier = supplierComboList.get(supIdx);
        Warehouse warehouse = warehousesList.get(whIdx);
        // Índice 0 = "a crédito" (sem conta → conta a pagar); restantes mapeiam accountsList[idx-1].
        boolean onCredit = accIdx <= 0;
        Long financeAccountId = onCredit ? null : accountsList.get(accIdx - 1).id();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        CreatePurchaseRequest request = new CreatePurchaseRequest(
                supplier.getId(), warehouse.getId(), companyId, financeAccountId, draftLines);
        Purchase p = purchaseService.createPurchase(request);
        JOptionPane.showMessageDialog(this, "Compra " + p.getPurchaseNumber() + " registada com sucesso!\n" +
                (onCredit
                    ? "Stock atualizado. Compra a crédito — ver tab Contas a Pagar."
                    : "Stock atualizado e saldo deduzido de " + p.getTotalAmount() + " MT."),
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);

        draftLines.clear();
        draftLinesModel.setRowCount(0);
        draftTotal = BigDecimal.ZERO;
        totalLabel.setText("Total Compra: 0.00 MT (excl. IVA)");
    }

    private void loadPurchasesHistory() {
        purchasesModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<Purchase> purchases = purchaseService.getPurchasesByCompany(companyId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Purchase p : purchases) {
            purchasesModel.addRow(new Object[]{
                    p.getPurchaseNumber(),
                    p.getSupplier().getName(),
                    p.getWarehouse().getName(),
                    p.getTotalAmount() + " MT",
                    p.getTaxAmount() + " MT",
                    p.getPurchaseDate().format(dtf)
            });
        }
    }

    /**
     * Painel de formulário transparente que acompanha a largura do viewport mas não a altura,
     * para um {@link JScrollPane} dar apenas scroll vertical mantendo os campos a largura toda.
     */
    private static final class VScrollForm extends JPanel implements Scrollable {
        VScrollForm(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

}
