package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ComprasPanel extends JPanel {

    final PurchaseApiClient purchaseApiClient;
    final InventoryApiClient inventoryApiClient;
    final ComercialApiClient comercialApiClient;
    final FinanceApiClient financeApiClient;

    private final PurchaseSuppliersPanel suppliersPanel;
    private final PurchaseBusinessUnitsPanel businessUnitsPanel;
    final PurchaseReorderPanel reorderPanel;
    private final PurchasePayablesPanel payablesPanel;
    final PurchaseOrdersPanel purchaseOrdersPanel;
    private final SupplierStatementPanel supplierStatementPanel;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    JTabbedPane tabbedPane;
    DefaultTableModel reorderModel;
    List<ReorderSuggestionDTO> reorderList = new ArrayList<>();

    // TAB ENCOMENDAS A FORNECEDOR
    JComboBox<String> poSupplierCombo;
    JComboBox<String> poWarehouseCombo;
    ProductSearchComboBox poProductCombo;
    MoneyField poPriceField;
    DefaultTableModel poLinesModel;
    JLabel poTotalLabel;
    JTextField poSearchField;
    JComboBox<String> poBusinessUnitCombo;
    final List<CreatePurchaseOrderLineRequest> poDraftLines = new ArrayList<>();
    List<PurchaseOrderDTO> poList = new ArrayList<>();
    JPanel poFormContent;
    JPanel purchaseFormContent;

    // TAB CONTAS A PAGAR
    DefaultTableModel payablesModel;
    JTable payablesTable;
    List<PayableDTO> payablesList = new ArrayList<>();

    // TAB 1: REGISTO COMPRA ELEMENTS
    JComboBox<String> supplierCombo;
    JComboBox<String> warehouseCombo;
    JComboBox<String> accountCombo;
    ProductSearchComboBox productCombo;
    QuantityField quantityField;
    PackageQuantityEditor purchasePackageEditor;
    MoneyField priceField;
    private JTextField batchField;
    private DateField expirationField;
    private JTextField serialField;
    JTextField purchaseVatField;

    DefaultTableModel draftLinesModel;
    private JTable draftLinesTable;
    JLabel totalLabel;
    JComboBox<String> histBusinessUnitCombo;

    DefaultTableModel purchasesModel;
    private JTable purchasesTable;

    // TAB 2: FORNECEDORES ELEMENTS
    DefaultTableModel suppliersModel;
    JTable suppliersTable;

    // Seeding lists
    List<SupplierDTO> suppliersList = new ArrayList<>();
    List<SupplierDTO> supplierComboList = new ArrayList<>();
    List<WarehouseDTO> warehousesList = new ArrayList<>();
    List<TreasuryAccountDTO> accountsList = new ArrayList<>();
    List<ProductDTO> productsList = new ArrayList<>();

    // Draft items
    final List<CreatePurchaseLineRequest> draftLines = new ArrayList<>();
    BigDecimal draftTotal = BigDecimal.ZERO;

    public ComprasPanel(
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient,
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient
    ) {
        this(purchaseApiClient, inventoryApiClient, comercialApiClient, financeApiClient, null);
    }

    public ComprasPanel(
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient,
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient,
            mz.multicore.erp.desktop.client.AccountStatementApiClient statementApiClient
    ) {
        this.purchaseApiClient = purchaseApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.suppliersPanel = new PurchaseSuppliersPanel(this);
        this.businessUnitsPanel = new PurchaseBusinessUnitsPanel(this);
        this.reorderPanel = new PurchaseReorderPanel(this);
        this.payablesPanel = new PurchasePayablesPanel(this);
        this.purchaseOrdersPanel = new PurchaseOrdersPanel(this);
        this.supplierStatementPanel = statementApiClient != null ? new SupplierStatementPanel(statementApiClient, purchaseApiClient) : null;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        tabbedPane.addTab("Faturas de Compra (V/FT)", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.MODULE_COMPRAS), createComprasTab());
        tabbedPane.addTab("Reposição", UIHelper.icon("fas-cart-arrow-down", 16, UIHelper.ACCENT_ORANGE), createReorderTab());
        tabbedPane.addTab("Encomendas a Fornecedor (EC-F)", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), createPurchaseOrdersTab());
        tabbedPane.addTab("Contas a Pagar", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.REJECTED_RED), createPayablesTab());
        tabbedPane.addTab("Gestão de Fornecedores", UIHelper.icon("fas-truck-loading", 16, UIHelper.ACCENT), createFornecedoresTab());
        tabbedPane.addTab("Gestão de Negócios", UIHelper.icon("fas-briefcase", 16, UIHelper.ACCENT), businessUnitsPanel.buildPanel());

        if (supplierStatementPanel != null) {
            tabbedPane.addTab("Conta Corrente & Reconciliação", UIHelper.icon("fas-file-invoice", 16, UIHelper.ACCENT_CYAN), supplierStatementPanel);
        }

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == supplierStatementPanel && supplierStatementPanel != null) {
                supplierStatementPanel.refreshData();
            }
        });

        add(feedback, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createComprasTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        VScrollForm formCard = new VScrollForm(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        supplierCombo = new JComboBox<>(); UIHelper.styleComboBox(supplierCombo);
        warehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(warehouseCombo);
        accountCombo = new JComboBox<>(); UIHelper.styleComboBox(accountCombo);
        productCombo = new ProductSearchComboBox();
        productCombo.addActionListener(e -> { updateDefaultPrice(); refreshPurchasePackaging(); });
        purchasePackageEditor = new PackageQuantityEditor();
        quantityField = purchasePackageEditor.totalField();
        priceField = new MoneyField();
        batchField = new JTextField(); UIHelper.styleTextField(batchField);
        serialField = new JTextField(); UIHelper.styleTextField(serialField);
        expirationField = new DateField();
        expirationField.putClientProperty("JTextField.placeholderText", "Só obrigatória para perecíveis");
        purchaseVatField = new JTextField(); UIHelper.styleTextField(purchaseVatField);
        purchaseVatField.setToolTipText("Como vem na fatura do fornecedor (ex.: 16). Vazio = taxa do artigo.");

        addFormRow(formCard, gbc, 0, "Fornecedor:", supplierCombo, "Armazém de Destino:", warehouseCombo);
        addFormRow(formCard, gbc, 1, "Conta de Pagamento:", accountCombo, "Artigo / Produto:", productCombo);
        addFormRow(formCard, gbc, 2, "Quantidade:", purchasePackageEditor, "Preço Unit. Custo (MT):", priceField);
        addFormRow(formCard, gbc, 3, "Nº Lote (Opcional):", batchField, "Nº Série (Opcional):", serialField);
        addFormRow(formCard, gbc, 4, "Validade do Lote (yyyy-MM-dd):", expirationField, "IVA da fatura (%):", purchaseVatField);

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 8, 12, 8);
        ModernButton addLineBtn = UIHelper.createAddLineButton();
        JPanel addLineActionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addLineActionRow.setOpaque(false);
        addLineActionRow.add(addLineBtn);
        formCard.add(addLineActionRow, gbc);

        gbc.gridy = 11; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        String[] cols = {"Produto", "Qtd", "Preço Custo", "Lote/Série", "Subtotal", "IVA"};
        draftLinesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        draftLinesTable = new JTable(draftLinesModel);
        UIHelper.styleTable(draftLinesTable);
        UIHelper.ensureHeadersFit(draftLinesTable);
        JScrollPane scroll = new JScrollPane(draftLinesTable);
        UIHelper.styleEmbeddedTableScrollPane(scroll, draftLinesTable, 4);
        formCard.add(scroll, gbc);

        gbc.gridy = 12; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        totalLabel = new JLabel("Total Compra: 0,00 MT (excl. IVA)");
        totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        totalLabel.setForeground(Color.WHITE);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.add(totalLabel, BorderLayout.EAST);
        bottomPanel.add(totalRow);
        formCard.add(bottomPanel, gbc);

        this.purchaseFormContent = formCard;

        ModernButton newPurchaseBtn = UIHelper.createSuccessButton("Registar Compra");
        newPurchaseBtn.setIcon(UIHelper.icon("fas-download", 14));
        newPurchaseBtn.addActionListener(e -> openPurchaseFormDialog());

        ModernPanel historyCard = new ModernPanel(16);
        historyCard.setLayout(new BorderLayout(0, 6));
        historyCard.setBorder(new EmptyBorder(8, 12, 8, 12));

        String[] historyCols = {"Nº Documento", "Fornecedor", "Armazém", "Total Faturado", "Imposto", "Data", "Negócio"};
        purchasesModel = new DefaultTableModel(historyCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        purchasesTable = new JTable(purchasesModel);
        UIHelper.styleTable(purchasesTable);
        UIHelper.ensureHeadersFit(purchasesTable);
        purchasesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        purchasesTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        purchasesTable.getColumnModel().getColumn(6).setPreferredWidth(110);
        JScrollPane histScroll = new JScrollPane(purchasesTable);
        UIHelper.styleScrollPane(histScroll);

        JTextField histSearch = TableFilter.searchField("Nº doc, fornecedor ou armazém…");
        histBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        JComboBox<String> histPeriodo = TableFilter.periodCombo();
        TableFilter.install(purchasesTable, histSearch,
                java.util.List.of(new TableFilter.ColumnFilter(histBusinessUnitCombo, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(histPeriodo, 5)));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadPurchasesHistory);
        ModernButton returnBtn = UIHelper.createSecondaryButton("Devolver ao Fornecedor");
        returnBtn.setIcon(UIHelper.icon("fas-undo", 14));
        returnBtn.addActionListener(e -> SupplierReturnDialog.open(this, purchasesTable));
        JPanel histFilters = UIHelper.filterBar(
                new JComponent[]{histSearch,
                        TableFilter.label("Negócio:"), histBusinessUnitCombo,
                        TableFilter.label("Data:", "fas-calendar-alt"), histPeriodo},
                null);
        histFilters.setBorder(new EmptyBorder(0, 0, 4, 0));
        historyCard.add(UIHelper.tableCardTop("Faturas de Compra Registadas", histFilters,
                refreshBtn, returnBtn, newPurchaseBtn), BorderLayout.NORTH);
        historyCard.add(histScroll, BorderLayout.CENTER);
        TableQuickPeekController.install(purchasesTable, historyCard, (peek, row) -> {
            String doc = String.valueOf(purchasesModel.getValueAt(row, 0));
            String sup = String.valueOf(purchasesModel.getValueAt(row, 1));
            String wh = String.valueOf(purchasesModel.getValueAt(row, 2));
            String total = String.valueOf(purchasesModel.getValueAt(row, 3));
            String imp = String.valueOf(purchasesModel.getValueAt(row, 4));
            String data = String.valueOf(purchasesModel.getValueAt(row, 5));
            String bu = String.valueOf(purchasesModel.getValueAt(row, 6));
            peek.setHeaderIcon("fas-file-invoice-dollar", UIHelper.MODULE_COMPRAS);
            peek.setTitle(doc);
            peek.setSubtitle("Fornecedor: " + sup);
            peek.setStatus("REGISTADO", UIHelper.APPROVED_GREEN);
            peek.setItems(java.util.List.of(
                    new QuickPeekPanel.PeekItem("Armazém Destino", wh, false),
                    new QuickPeekPanel.PeekItem("Linha de Negócio", bu, false),
                    new QuickPeekPanel.PeekItem("Data Compra", data, false),
                    new QuickPeekPanel.PeekItem("Total Imposto", imp, false),
                    new QuickPeekPanel.PeekItem("Total Faturado", total, true)
            ));
        });

        panel.add(historyCard, BorderLayout.CENTER);
        addLineBtn.addActionListener(e -> addDraftLine());
        return panel;
    }

    private void openPurchaseFormDialog() { PurchaseInvoiceOperations.openPurchaseFormDialog(this); }
    private JPanel createFornecedoresTab() { return suppliersPanel.buildPanel(); }
    private JPanel createReorderTab() { return reorderPanel.buildPanel(); }
    private void loadReorderSuggestions() { reorderPanel.refresh(); }
    private JPanel createPayablesTab() { return payablesPanel.buildPanel(); }
    void loadPayables() { payablesPanel.refresh(); }
    void refreshPoCombos() { purchaseOrdersPanel.refreshCombos(); }
    private JPanel createPurchaseOrdersTab() { return purchaseOrdersPanel.buildPanel(); }
    private void loadPurchaseOrders() { purchaseOrdersPanel.refresh(); }
    void loadBusinessUnits() { businessUnitsPanel.loadBusinessUnits(); }

    public void onPanelSelected() {
        loadSuppliers();
        loadWarehouses();
        loadAccounts();
        loadProducts();
        loadPurchasesHistory();
        loadPurchaseOrders();
        loadPayables();
        loadReorderSuggestions();
        loadBusinessUnits();
    }

    void loadSuppliers() { PurchaseMetadataCoordinator.loadSuppliers(this); }
    void loadWarehouses() { PurchaseMetadataCoordinator.loadWarehouses(this); }
    void loadAccounts() { PurchaseMetadataCoordinator.loadAccounts(this); }
    void loadProducts() { PurchaseMetadataCoordinator.loadProducts(this); }
    void updateDefaultPrice() { PurchaseMetadataCoordinator.updateDefaultPrice(this); }
    void refreshPurchasePackaging() { PurchaseMetadataCoordinator.refreshPurchasePackaging(this); }
    String warehouseName(Long warehouseId) { return PurchaseMetadataCoordinator.warehouseName(this, warehouseId); }
    void loadPurchasesHistory() { PurchaseInvoiceOperations.loadPurchasesHistory(this); }

    private void addDraftLine() {
        if (productsList.isEmpty()) return;
        ProductDTO product = productCombo.selectedProduct();
        if (product == null) {
            showPurchaseNotice(FeedbackType.WARNING, "Produto necessário", "Pesquise e seleccione um produto para adicionar.");
            return;
        }

        BigDecimal qty;
        try {
            qty = quantityField.value();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                UIHelper.markFieldInvalid(quantityField, "A quantidade deve ser maior que zero.");
                throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            }
        } catch (IllegalArgumentException e) { return; }

        BigDecimal price;
        try {
            price = priceField.value();
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                UIHelper.markFieldInvalid(priceField, "O preço não pode ser negativo.");
                throw new IllegalArgumentException("O preço não pode ser negativo.");
            }
        } catch (IllegalArgumentException e) { return; }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;

        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        // Validade só obrigatória em produtos perecíveis; loiça, mobília e afins entram sem validade.
        LocalDate expirationDate = null;
        if (!expirationField.getText().isBlank() || product.tracksExpiry()) {
            try {
                expirationDate = expirationField.value();
            } catch (IllegalArgumentException e) {
                if (expirationField.getText().isBlank()) {
                    UIHelper.markFieldInvalid(expirationField,
                            "'" + product.name() + "' é perecível: indique a validade (yyyy-MM-dd).");
                }
                return;
            }
        } else {
            UIHelper.clearFieldInvalid(expirationField);
        }

        BigDecimal invoiceTaxRate;
        try {
            invoiceTaxRate = parsePercentageOrNull(purchaseVatField.getText());
            UIHelper.clearFieldInvalid(purchaseVatField);
        } catch (NumberFormatException e) {
            UIHelper.markFieldInvalid(purchaseVatField, "Indique a percentagem de IVA (ex.: 16) ou deixe o campo vazio.");
            return;
        }

        CreatePurchaseLineRequest line = new CreatePurchaseLineRequest(product.id(), qty, price, batch, expirationDate, serial, invoiceTaxRate);
        draftLines.add(line);

        BigDecimal subtotal = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        draftTotal = draftTotal.add(subtotal);

        String lotSer = "";
        if (batch != null) lotSer += "L: " + batch + " ";
        if (expirationDate != null) lotSer += "V: " + expirationDate + " ";
        if (serial != null) lotSer += "S: " + serial;

        BigDecimal shownRate = invoiceTaxRate != null ? invoiceTaxRate : product.effectiveTaxRate();
        draftLinesModel.addRow(new Object[]{
                product.name(), qty, price + " MT", lotSer.trim(), subtotal + " MT",
                formatRate(shownRate) + (invoiceTaxRate == null ? " (artigo)" : "")
        });

        totalLabel.setText(String.format("Total Compra: %,.2f MT (excl. IVA)", draftTotal));

        purchasePackageEditor.reset();
        batchField.setText("");
        expirationField.setText("");
        serialField.setText("");
        purchaseVatField.setText("");
        updateDefaultPrice();
    }

    static BigDecimal parsePercentageOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        BigDecimal percent = new BigDecimal(raw.trim().replace(',', '.'));
        if (percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new NumberFormatException("Percentagem de IVA fora do intervalo 0–100.");
        }
        return percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    static String formatRate(BigDecimal rate) {
        if (rate == null) return "—";
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }

    void showPurchaseLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area, error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }

    void showPurchaseError(Throwable error) {
        showPurchaseNotice(FeedbackType.ERROR, "Não foi possível concluir a operação", error.getMessage());
    }

    void showPurchaseNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    void showPurchaseSuccess(String message) {
        ToastManager.success(this, message);
    }

    private static void addFormRow(JPanel form, GridBagConstraints gbc, int y, String lbl1, JComponent c1, String lbl2, JComponent c2) {
        gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.gridx = 0; gbc.gridy = y * 2; gbc.insets = new Insets(8, 8, 2, 8);
        JLabel l1 = new JLabel(lbl1); l1.setForeground(UIHelper.TEXT_MUTED); form.add(l1, gbc);
        gbc.gridx = 1;
        JLabel l2 = new JLabel(lbl2); l2.setForeground(UIHelper.TEXT_MUTED); form.add(l2, gbc);
        gbc.gridx = 0; gbc.gridy = y * 2 + 1; gbc.insets = new Insets(2, 8, 12, 8); form.add(c1, gbc);
        gbc.gridx = 1; form.add(c2, gbc);
    }

    private static final class VScrollForm extends JPanel implements Scrollable {
        VScrollForm(LayoutManager layout) { super(layout); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
