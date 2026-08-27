package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.PackageQuantityEditor;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;

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

    final PurchaseApiClient purchaseApiClient;
    private final PurchaseSuppliersPanel suppliersPanel;
    private final PurchaseReorderPanel reorderPanel;
    private final PurchasePayablesPanel payablesPanel;
    private final PurchaseOrdersPanel purchaseOrdersPanel;
    private final InventoryApiClient inventoryApiClient;

    // Reposição automática
    JTabbedPane tabbedPane;
    DefaultTableModel reorderModel;
    private JTable reorderTable;
    private JLabel reorderFooter;
    java.util.List<mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO> reorderList = new java.util.ArrayList<>();
    private final ComercialApiClient comercialApiClient;
    private final FinanceApiClient financeApiClient;

    // TAB ENCOMENDAS A FORNECEDOR
    JComboBox<String> poSupplierCombo;
    JComboBox<String> poWarehouseCombo;
    JComboBox<String> poProductCombo;
    private QuantityField poQtyField;
    MoneyField poPriceField;
    private JTextField poExpectedField;
    DefaultTableModel poLinesModel;
    private JTable poLinesTable;
    JLabel poTotalLabel;
    private DefaultTableModel poListModel;
    private JTable poListTable;
    JTextField poSearchField;
    final List<CreatePurchaseOrderLineRequest> poDraftLines = new ArrayList<>();
    List<PurchaseOrderDTO> poList = new ArrayList<>();
    JPanel poFormContent;                   // conteúdo do modal de nova encomenda
    private JPanel purchaseFormContent;             // conteúdo do modal de registar compra
    private JTextField supplierSearchField;

    // TAB CONTAS A PAGAR
    DefaultTableModel payablesModel;
    JTable payablesTable;
    private JLabel payablesFooter;
    List<mz.multicore.erp.modules.purchases.dto.PayableDTO> payablesList = new ArrayList<>();

    // TAB 1: REGISTO COMPRA ELEMENTS
    private JComboBox<String> supplierCombo;
    private JComboBox<String> warehouseCombo;
    private JComboBox<String> accountCombo;
    private JComboBox<String> productCombo;
    private QuantityField quantityField;
    private PackageQuantityEditor purchasePackageEditor;
    private MoneyField priceField;
    private JTextField batchField;
    private DateField expirationField;
    private JTextField serialField;
    /** IVA da linha como vem na factura do fornecedor; vazio ⇒ taxa do artigo (resolvida no backend). */
    private JTextField purchaseVatField;
    
    private DefaultTableModel draftLinesModel;
    private JTable draftLinesTable;
    private JLabel totalLabel;
    
    private DefaultTableModel purchasesModel;
    private JTable purchasesTable;

    // TAB 2: FORNECEDORES ELEMENTS
    // Suppliers form fields são criados dentro do modal — sem refs guardadas aqui
    
    DefaultTableModel suppliersModel;
    JTable suppliersTable;

    // Seeding lists
    List<SupplierDTO> suppliersList = new ArrayList<>();        // linhas da tabela (pode estar filtrada)
    List<SupplierDTO> supplierComboList = new ArrayList<>();     // activos, para combos de compra/encomenda
    List<WarehouseDTO> warehousesList = new ArrayList<>();
    List<TreasuryAccountDTO> accountsList = new ArrayList<>();
    List<ProductDTO> productsList = new ArrayList<>();

    // Draft items
    private final List<CreatePurchaseLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftTotal = BigDecimal.ZERO;

    public ComprasPanel(
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient,
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient
    ) {
        this.purchaseApiClient = purchaseApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.suppliersPanel = new PurchaseSuppliersPanel(this);
        this.reorderPanel = new PurchaseReorderPanel(this);
        this.payablesPanel = new PurchasePayablesPanel(this);
        this.purchaseOrdersPanel = new PurchaseOrdersPanel(this);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

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

        // Carregamento preguiçoso: os dados vêm por HTTP em onPanelSelected() (via navigate), não no
        // construtor — evita chamadas à API no arranque para quem não tem empresa activa.
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
        productCombo.addActionListener(e -> {
            updateDefaultPrice();
            refreshPurchasePackaging();
        });
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
        purchasePackageEditor = new PackageQuantityEditor();
        quantityField = purchasePackageEditor.totalField();
        formCard.add(purchasePackageEditor, gbc);

        gbc.gridx = 1;
        priceField = new MoneyField();
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

        // Row 3b: Validade do Lote + IVA da factura do fornecedor (lado a lado)
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel expLbl = new JLabel("Validade do Lote (yyyy-MM-dd):");
        expLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(expLbl, gbc);

        gbc.gridx = 1;
        JLabel vatLbl = new JLabel("IVA da factura (%):");
        vatLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(vatLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        gbc.insets = new Insets(2, 8, 12, 8);
        expirationField = new DateField();
        formCard.add(expirationField, gbc);

        // Numa compra quem manda é a factura do fornecedor; vazio ⇒ taxa do artigo.
        gbc.gridx = 1;
        purchaseVatField = new JTextField();
        UIHelper.styleTextField(purchaseVatField);
        purchaseVatField.setToolTipText("Como vem na factura do fornecedor (ex.: 16). Vazio = taxa do artigo.");
        formCard.add(purchaseVatField, gbc);

        gbc.gridwidth = 2; // repor para as linhas seguintes

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
        String[] cols = {"Produto", "Qtd", "Preço Custo", "Lote/Série", "Subtotal", "IVA"};
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
        purchasesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        purchasesTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
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
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar Compras");
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
        PurchaseDTO[] created = new PurchaseDTO[1];
        boolean[] onCredit = new boolean[1];
        dlg.setOnSaveAsync(() -> {
            CreatePurchaseRequest request = buildPurchaseRequest(onCredit);
            return () -> created[0] = purchaseApiClient.createPurchase(request);
        });
        if (dlg.showDialog()) {
            PurchaseDTO purchase = created[0];
            JOptionPane.showMessageDialog(this, "Compra " + purchase.purchaseNumber() + " registada com sucesso!\n" +
                            (onCredit[0] ? "Stock atualizado. Compra a crédito — ver tab Contas a Pagar."
                                    : "Stock atualizado e saldo deduzido de " + purchase.totalAmount() + " MT."),
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            clearPurchaseDraft();
            loadPurchasesHistory();
            loadAccounts();
            loadPayables();
        }
    }

    private JPanel createFornecedoresTab() { return suppliersPanel.buildPanel(); }

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

    private JPanel createReorderTab() { return reorderPanel.buildPanel(); }

    private void loadReorderSuggestions() { reorderPanel.refresh(); }

    private JPanel createPayablesTab() { return payablesPanel.buildPanel(); }

    private void loadPayables() { payablesPanel.refresh(); }

    private void refreshPoCombos() { purchaseOrdersPanel.refreshCombos(); }

    private JPanel createPurchaseOrdersTab() { return purchaseOrdersPanel.buildPanel(); }

    private void loadPurchaseOrders() { purchaseOrdersPanel.refresh(); }

    void loadSuppliers() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> purchaseApiClient.getSuppliersByCompany(companyId), this::applySuppliers,
                error -> showPurchaseLoadError("fornecedores", error));
    }

    private void applySuppliers(List<SupplierDTO> all) {

        // Combos de compra/encomenda: só fornecedores activos.
        supplierComboList = all.stream().filter(SupplierDTO::active).toList();
        supplierCombo.removeAllItems();
        for (SupplierDTO s : supplierComboList) supplierCombo.addItem(s.name() + " (" + s.taxId() + ")");
        if (poSupplierCombo != null) {
            poSupplierCombo.removeAllItems();
            for (SupplierDTO s : supplierComboList) poSupplierCombo.addItem(s.name() + " (" + s.taxId() + ")");
        }

        // Tabela: carrega todos; a pesquisa/estado é aplicada pelo TableFilter (cliente).
        suppliersList = all;
        suppliersModel.setRowCount(0);
        for (SupplierDTO s : suppliersList) {
            suppliersModel.addRow(new Object[]{
                    s.name(),
                    s.taxId(),
                    dash(s.phone()),
                    dash(s.contactPerson()),
                    dash(s.email()),
                    dash(s.address()),
                    s.active() ? "Activo" : "Inactivo"
            });
        }
    }

    private static String dash(String v) {
        return v != null && !v.isBlank() ? v : "-";
    }

    private void loadWarehouses() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getWarehousesByCompany(companyId), this::applyWarehouses,
                error -> showPurchaseLoadError("armazéns", error));
    }

    private void applyWarehouses(List<WarehouseDTO> loaded) {
        warehouseCombo.removeAllItems();
        warehousesList = loaded;

        for (WarehouseDTO w : warehousesList) {
            warehouseCombo.addItem(w.name());
        }
    }

    void loadAccounts() {
        UIHelper.loadAsync(this, financeApiClient::getAllAccounts, this::applyAccounts,
                error -> showPurchaseLoadError("contas de tesouraria", error));
    }

    private void applyAccounts(List<TreasuryAccountDTO> loaded) {
        accountCombo.removeAllItems();
        accountsList = loaded;

        accountCombo.addItem("— A crédito (pagar depois) —");
        for (TreasuryAccountDTO acc : accountsList) {
            accountCombo.addItem(acc.name() + " (" + String.format("%.2f", acc.balance()) + " MT)");
        }
    }

    private void loadProducts() {
        UIHelper.loadAsync(this, comercialApiClient::getAllProducts, this::applyProducts,
                error -> showPurchaseLoadError("produtos", error));
    }

    private void applyProducts(List<ProductDTO> loaded) {
        productCombo.removeAllItems();
        productsList = loaded;

        for (ProductDTO p : productsList) {
            productCombo.addItem(productLabel(p));
        }
        updateDefaultPrice();
        refreshPurchasePackaging();
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

    private void refreshPurchasePackaging() {
        if (purchasePackageEditor == null) return;
        int index = productCombo.getSelectedIndex();
        if (index >= 0 && index < productsList.size()) {
            purchasePackageEditor.setUnitsPerBox(productsList.get(index).unitsPerBox());
        }
    }

    private void addDraftLine() {
        if (productsList.isEmpty()) return;
        int prodIdx = productCombo.getSelectedIndex();
        if (prodIdx < 0) return;

        ProductDTO product = productsList.get(prodIdx);
        
        BigDecimal qty;
        try {
            qty = quantityField.value();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal price;
        try {
            price = priceField.value();
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;

        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        LocalDate expirationDate;
        try {
            expirationDate = expirationField.value();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // IVA da factura do fornecedor (percentagem). Vazio ⇒ null ⇒ o backend usa a taxa do artigo.
        BigDecimal invoiceTaxRate;
        try {
            invoiceTaxRate = parsePercentageOrNull(purchaseVatField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "IVA da factura inválido. Indique a percentagem (ex.: 16) ou deixe vazio para usar a taxa do artigo.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CreatePurchaseLineRequest line = new CreatePurchaseLineRequest(
                product.id(),
                qty,
                price,
                batch,
                expirationDate,
                serial,
                invoiceTaxRate
        );
        draftLines.add(line);

        BigDecimal subtotal = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        draftTotal = draftTotal.add(subtotal);

        String lotSer = "";
        if (batch != null) lotSer += "L: " + batch + " ";
        lotSer += "V: " + expirationDate + " ";
        if (serial != null) lotSer += "S: " + serial;

        BigDecimal shownRate = invoiceTaxRate != null ? invoiceTaxRate : product.effectiveTaxRate();
        draftLinesModel.addRow(new Object[]{
                product.name(),
                qty,
                price + " MT",
                lotSer.trim(),
                subtotal + " MT",
                formatRate(shownRate) + (invoiceTaxRate == null ? " (artigo)" : "")
        });

        totalLabel.setText(String.format("Total Compra: %,.2f MT (excl. IVA)", draftTotal));

        // Reset details
        purchasePackageEditor.reset();
        batchField.setText("");
        expirationField.setText("");
        serialField.setText("");
        purchaseVatField.setText("");
        updateDefaultPrice();
    }

    /**
     * Percentagem escrita pelo operador (ex.: "16", "5,5") convertida na fracção que o backend usa
     * (0.16, 0.055). Vazio devolve {@code null} — o backend cai então na taxa do artigo. Pura e
     * testável (cenários IV-08..IV-10).
     */
    static BigDecimal parsePercentageOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        BigDecimal percent = new BigDecimal(raw.trim().replace(',', '.'));
        if (percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new NumberFormatException("Percentagem de IVA fora do intervalo 0–100.");
        }
        return percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /** Fracção (0.16) apresentada como percentagem legível ("16%"). */
    static String formatRate(BigDecimal rate) {
        if (rate == null) return "—";
        return rate.multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros().toPlainString() + "%";
    }

    /** Validação + registo da compra. Lança RuntimeException em erro (mantém o modal aberto). */
    private CreatePurchaseRequest buildPurchaseRequest(boolean[] onCreditResult) {
        if (warehousesList.isEmpty()) {
            throw new RuntimeException("Falta registar armazéns.");
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

        SupplierDTO supplier = supplierComboList.get(supIdx);
        WarehouseDTO warehouse = warehousesList.get(whIdx);
        // Índice 0 = "a crédito" (sem conta → conta a pagar); restantes mapeiam accountsList[idx-1].
        boolean onCredit = accIdx <= 0;
        onCreditResult[0] = onCredit;
        Long financeAccountId = onCredit ? null : accountsList.get(accIdx - 1).id();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        return new CreatePurchaseRequest(supplier.id(), warehouse.id(), companyId, financeAccountId,
                new ArrayList<>(draftLines));
    }

    private void clearPurchaseDraft() {
        draftLines.clear();
        draftLinesModel.setRowCount(0);
        draftTotal = BigDecimal.ZERO;
        totalLabel.setText("Total Compra: 0.00 MT (excl. IVA)");
    }

    void loadPurchasesHistory() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> purchaseApiClient.getPurchasesByCompany(companyId), this::applyPurchasesHistory,
                error -> showPurchaseLoadError("histórico de compras", error));
    }

    private void applyPurchasesHistory(List<PurchaseDTO> purchases) {
        purchasesModel.setRowCount(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (PurchaseDTO p : purchases) {
            purchasesModel.addRow(new Object[]{
                    p.purchaseNumber(),
                    p.supplierName(),
                    warehouseName(p.warehouseId()),
                    p.totalAmount(),
                    p.taxAmount(),
                    p.purchaseDate().format(dtf)
            });
        }
    }

    void showPurchaseLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    void showPurchaseError(Throwable error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /** Nome do armazém a partir do id, via lista de armazéns carregada (o PurchaseDTO só traz o id). */
    private String warehouseName(Long warehouseId) {
        if (warehouseId == null) return "—";
        return warehousesList.stream()
                .filter(w -> warehouseId.equals(w.id()))
                .map(WarehouseDTO::name)
                .findFirst()
                .orElse("#" + warehouseId);
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
