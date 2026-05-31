package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.purchases.dto.CreatePurchaseLineRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseRequest;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.model.Purchase;
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
    private final InventoryService inventoryService;
    private final ComercialService comercialService;
    private final FinanceService financeService;

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
    private List<Supplier> suppliersList = new ArrayList<>();
    private List<Warehouse> warehousesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();
    private List<ProductDTO> productsList = new ArrayList<>();

    // Draft items
    private final List<CreatePurchaseLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftTotal = BigDecimal.ZERO;

    public ComprasPanel(
            PurchaseService purchaseService,
            InventoryService inventoryService,
            ComercialService comercialService,
            FinanceService financeService
    ) {
        this.purchaseService = purchaseService;
        this.inventoryService = inventoryService;
        this.comercialService = comercialService;
        this.financeService = financeService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPane(tabbedPane);

        // Tab 1: Compras
        JPanel tabCompras = createComprasTab();
        tabbedPane.addTab("Faturas de Compra (V/FT)", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT), tabCompras);

        // Tab 2: Fornecedores
        JPanel tabFornecedores = createFornecedoresTab();
        tabbedPane.addTab("Gestão de Fornecedores", UIHelper.icon("fas-truck-loading", 16, UIHelper.TEXT_LIGHT), tabFornecedores);

        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
    }

    private JPanel createComprasTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // LEFT COLUMN: PURCHASE ENTRY FORM
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(UIHelper.createHeading("Registar Compra (Entrada Stock)"), BorderLayout.NORTH);

        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(15, 15, 15, 15));

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

        // Row 4: Add Button (Full Width)
        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 8, 12, 8);
        ModernButton addLineBtn = new ModernButton("Adicionar Produto", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        addLineBtn.setIcon(UIHelper.icon("fas-plus", 14));
        formCard.add(addLineBtn, gbc);

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
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalLabel.setForeground(Color.WHITE);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.add(totalLabel, BorderLayout.EAST);

        ModernButton registerBtn = new ModernButton("Registar Compra");
        registerBtn.setIcon(UIHelper.icon("fas-download", 14));
        registerBtn.setGradient(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.darker());

        JPanel btnRow = new JPanel(new BorderLayout());
        btnRow.setOpaque(false);
        btnRow.add(registerBtn, BorderLayout.EAST);

        bottomPanel.add(totalRow);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        bottomPanel.add(btnRow);
        formCard.add(bottomPanel, gbc);

        leftPanel.add(formCard, BorderLayout.CENTER);
        panel.add(leftPanel);

        // RIGHT COLUMN: PURCHASES HISTORY
        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(UIHelper.createHeading("Faturas de Compra Registadas"), BorderLayout.NORTH);

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
        historyCard.add(histScroll, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionRow.setOpaque(false);
        ModernButton refreshBtn = new ModernButton("Atualizar Compras", new Color(75, 85, 99), new Color(107, 114, 128));
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        actionRow.add(refreshBtn);
        historyCard.add(actionRow, BorderLayout.SOUTH);

        rightPanel.add(historyCard, BorderLayout.CENTER);
        panel.add(rightPanel);

        // LISTENERS
        addLineBtn.addActionListener(e -> addDraftLine());
        registerBtn.addActionListener(e -> registerPurchase());
        refreshBtn.addActionListener(e -> loadPurchasesHistory());

        return panel;
    }

    private JPanel createFornecedoresTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header: title + action buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Fornecedores Cadastrados"), BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        ModernButton refreshSupsBtn = new ModernButton("Atualizar", new Color(75, 85, 99), new Color(107, 114, 128));
        refreshSupsBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        ModernButton newSupBtn = new ModernButton("Novo Fornecedor", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.brighter());
        newSupBtn.setIcon(UIHelper.icon("fas-plus", 14));
        headerActions.add(refreshSupsBtn);
        headerActions.add(newSupBtn);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Table full-width
        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] supCols = {"Nome do Fornecedor", "NUIT/NIF", "Correio Eletrónico", "Endereço"};
        suppliersModel = new DefaultTableModel(supCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        suppliersTable = new JTable(suppliersModel);
        UIHelper.styleTable(suppliersTable);
        JScrollPane scroll = new JScrollPane(suppliersTable);
        UIHelper.styleScrollPane(scroll);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        refreshSupsBtn.addActionListener(e -> loadSuppliers());
        newSupBtn.addActionListener(e -> openNewSupplierDialog());

        return panel;
    }

    private void openNewSupplierDialog() {
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();

        JPanel form = UIHelper.createDialogForm(
                "Nome / Empresa:", nameField,
                "NUIT / NIF (9 dígitos):", taxIdField,
                "Correio Eletrónico:", emailField,
                "Endereço:", addressField
        );

        Window parent = SwingUtilities.getWindowAncestor(this);
        ModernFormDialog dlg = new ModernFormDialog(parent, "Novo Fornecedor", form);
        dlg.setSize(520, 420);
        dlg.setOnSave(() -> {
            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            if (name.isEmpty() || taxId.isEmpty()) {
                throw new RuntimeException("Nome e NUIT/NIF são campos obrigatórios.");
            }
            purchaseService.createSupplier(
                    name, taxId,
                    emailField.getText().trim(),
                    addressField.getText().trim(),
                    CurrentUserContext.getCurrentCompanyId()
            );
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this,
                    "Fornecedor '" + nameField.getText().trim() + "' registado com sucesso!",
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
    }

    private void loadSuppliers() {
        supplierCombo.removeAllItems();
        suppliersModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        suppliersList = purchaseService.getSuppliersByCompany(companyId);

        for (Supplier s : suppliersList) {
            supplierCombo.addItem(s.getName() + " (" + s.getTaxId() + ")");
            suppliersModel.addRow(new Object[]{
                    s.getName(),
                    s.getTaxId(),
                    s.getEmail() != null ? s.getEmail() : "-",
                    s.getAddress() != null ? s.getAddress() : "-"
            });
        }
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

    private void registerPurchase() {
        if (suppliersList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Falta cadastrar fornecedores.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Falta cadastrar armazéns.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Falta cadastrar contas financeiras.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (draftLines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum produto adicionado à compra.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int supIdx = supplierCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        int accIdx = accountCombo.getSelectedIndex();

        if (supIdx < 0 || whIdx < 0 || accIdx < 0) return;

        Supplier supplier = suppliersList.get(supIdx);
        Warehouse warehouse = warehousesList.get(whIdx);
        TreasuryAccountDTO account = accountsList.get(accIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        try {
            CreatePurchaseRequest request = new CreatePurchaseRequest(
                    supplier.getId(),
                    warehouse.getId(),
                    companyId,
                    account.id(),
                    draftLines
            );

            Purchase p = purchaseService.createPurchase(request);
            JOptionPane.showMessageDialog(this, "Compra " + p.getPurchaseNumber() + " registada com sucesso!\n" +
                    "Stock atualizado nos armazéns e saldo deduzido de " + p.getTotalAmount() + " MT.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            // Clear draft
            draftLines.clear();
            draftLinesModel.setRowCount(0);
            draftTotal = BigDecimal.ZERO;
            totalLabel.setText("Total Compra: 0.00 MT (excl. IVA)");

            loadPurchasesHistory();
            loadAccounts(); // refresh balance
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao registar compra: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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

}
