package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ClientDTO;
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
import com.phcpro.modules.pos.service.POSService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class POSPanel extends JPanel {

    private final POSService posService;
    private final ComercialService comercialService;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final com.phcpro.modules.printing.ReceiptPrintService receiptPrintService;
    private final com.phcpro.modules.company.service.CompanyService companyService;

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
    private JTextField serialField;

    private DefaultTableModel cartModel;
    private JTable cartTable;
    private JLabel totalLabel;

    private ModernButton openSessionBtn;
    private ModernButton closeSessionBtn;
    private ModernButton cashMoveBtn;
    private ModernButton checkoutBtn;
    private JCheckBox creditCheck;

    private List<ProductDTO> productsList = new ArrayList<>();
    private List<ProductDTO> filteredProducts = new ArrayList<>();
    private List<ClientDTO> clientsList = new ArrayList<>();
    private List<ClientDTO> filteredClients = new ArrayList<>();
    private List<Warehouse> warehousesList = new ArrayList<>();
    private List<TreasuryAccountDTO> accountsList = new ArrayList<>();

    // Cart items representation
    private static class CartItem {
        ProductDTO product;
        int qty;
        BigDecimal discount;
        String batch;
        String serial;

        CartItem(ProductDTO product, int qty, BigDecimal discount, String batch, String serial) {
            this.product = product;
            this.qty = qty;
            this.discount = discount;
            this.batch = batch;
            this.serial = serial;
        }

        BigDecimal getSubtotal() {
            BigDecimal base = product.unitPrice().multiply(BigDecimal.valueOf(qty));
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discVal = base.multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                return base.subtract(discVal);
            }
            return base;
        }
    }

    private final List<CartItem> cartItems = new ArrayList<>();

    public POSPanel(
            POSService posService,
            ComercialService comercialService,
            InventoryService inventoryService,
            FinanceService financeService,
            com.phcpro.modules.printing.ReceiptPrintService receiptPrintService,
            com.phcpro.modules.company.service.CompanyService companyService
    ) {
        this.posService = posService;
        this.comercialService = comercialService;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
        this.receiptPrintService = receiptPrintService;
        this.companyService = companyService;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // 1. TILL SESSION CONTROL BAR
        JPanel sessionBar = new JPanel(new GridBagLayout());
        sessionBar.setOpaque(false);

        GridBagConstraints sGbc = new GridBagConstraints();
        sGbc.fill = GridBagConstraints.HORIZONTAL;
        sGbc.weightx = 1.0;

        // Row 1: Heading
        sGbc.gridx = 0; sGbc.gridy = 0; sGbc.weightx = 1.0; sGbc.anchor = GridBagConstraints.WEST;
        JLabel heading = UIHelper.createHeading("Ponto de Venda (POS)");
        sessionBar.add(heading, sGbc);

        // Row 2: Status Label
        sGbc.gridy = 1; sGbc.weightx = 1.0; sGbc.anchor = GridBagConstraints.WEST;
        sGbc.insets = new Insets(10, 0, 0, 0); // space below heading
        statusLabel = new JLabel("Caixa Fechada. Abra uma sessão para vender.");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(UIHelper.PENDING_YELLOW);
        sessionBar.add(statusLabel, sGbc);

        // Row 3: Action Buttons
        sGbc.gridy = 2; sGbc.weightx = 1.0; sGbc.anchor = GridBagConstraints.WEST;
        sGbc.insets = new Insets(10, 0, 0, 0); // space below status
        JPanel sessionActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        sessionActions.setOpaque(false);

        openSessionBtn = new ModernButton("Abrir Caixa", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.brighter());
        openSessionBtn.setIcon(UIHelper.icon("fas-lock-open", 14));
        closeSessionBtn = new ModernButton("Fechar Caixa", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED.brighter());
        closeSessionBtn.setIcon(UIHelper.icon("fas-lock", 14));
        cashMoveBtn = new ModernButton("Sangria / Suprimento", new Color(107, 114, 128), new Color(156, 163, 175));
        cashMoveBtn.setIcon(UIHelper.icon("fas-exchange-alt", 14));

        sessionActions.add(openSessionBtn);
        sessionActions.add(cashMoveBtn);
        sessionActions.add(closeSessionBtn);
        sessionBar.add(sessionActions, sGbc);

        // 1b. BARCODE SCANNER BAR — Enter no campo procura por código e adiciona ao carrinho
        JPanel scannerBar = new JPanel(new BorderLayout(8, 0));
        scannerBar.setOpaque(false);
        scannerBar.setBorder(new EmptyBorder(8, 0, 4, 0));
        JLabel scanIcon = new JLabel(UIHelper.icon("fas-barcode", 20, UIHelper.ACCENT));
        scannerBar.add(scanIcon, BorderLayout.WEST);
        barcodeField = new JTextField();
        UIHelper.styleTextField(barcodeField);
        barcodeField.putClientProperty("JTextField.placeholderText", "Ler código de barras… (Enter para adicionar)");
        barcodeField.setFont(new Font("Segoe UI", Font.BOLD, 14));
        barcodeField.addActionListener(e -> handleBarcodeScan());
        scannerBar.add(barcodeField, BorderLayout.CENTER);

        JPanel topStack = new JPanel(new BorderLayout(0, 0));
        topStack.setOpaque(false);
        topStack.add(sessionBar, BorderLayout.NORTH);
        topStack.add(scannerBar, BorderLayout.SOUTH);
        add(topStack, BorderLayout.NORTH);


        // 2. MAIN POS WORKSPACE: FORM (LEFT) & CART (RIGHT)
        JPanel workspace = new JPanel(new GridLayout(1, 2, 20, 0));
        workspace.setOpaque(false);

        // LEFT: ADD PRODUCT FORM & METADATA
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(UIHelper.createSubheading("Configurações & Artigos"), BorderLayout.NORTH);

        // Scrollable form content (transparent inner panel)
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setOpaque(false);
        formCard.setBorder(new EmptyBorder(4, 4, 4, 4));

        // Instantiate controls
        warehouseCombo = new JComboBox<>();
        accountCombo = new JComboBox<>();
        clientCombo = new JComboBox<>();
        productCombo = new JComboBox<>();
        UIHelper.styleComboBox(warehouseCombo);
        UIHelper.styleComboBox(accountCombo);
        UIHelper.styleComboBox(clientCombo);
        UIHelper.styleComboBox(productCombo);

        clientSearchField = new JTextField();
        productSearchField = new JTextField();
        UIHelper.styleTextField(clientSearchField);
        UIHelper.styleTextField(productSearchField);
        clientSearchField.putClientProperty("JTextField.placeholderText", "🔍 Pesquisar cliente por nome ou NUIT…");
        productSearchField.putClientProperty("JTextField.placeholderText", "🔍 Pesquisar produto por SKU ou nome…");
        clientSearchField.setToolTipText("Filtrar clientes por nome ou NUIT");
        productSearchField.setToolTipText("Filtrar produtos por SKU ou nome");
        clientSearchField.getDocument().addDocumentListener(simpleDocumentListener(() -> filterClients(clientSearchField.getText())));
        productSearchField.getDocument().addDocumentListener(simpleDocumentListener(() -> filterProducts(productSearchField.getText())));

        qtyField = new JTextField("1");
        discountField = new JTextField("0");
        batchField = new JTextField();
        serialField = new JTextField();
        UIHelper.styleTextField(qtyField);
        UIHelper.styleTextField(discountField);
        UIHelper.styleTextField(batchField);
        UIHelper.styleTextField(serialField);

        ModernButton newClientBtn = new ModernButton("+ Novo", new Color(16, 185, 129), new Color(52, 211, 153));
        newClientBtn.setToolTipText("Criar novo cliente");
        newClientBtn.addActionListener(e -> createClientDialog());

        // Composite pickers
        JPanel clientRow = new JPanel(new BorderLayout(6, 0));
        clientRow.setOpaque(false);
        clientRow.add(clientCombo, BorderLayout.CENTER);
        clientRow.add(newClientBtn, BorderLayout.EAST);
        JPanel clientPicker = stackedPicker(clientSearchField, clientRow);
        JPanel productPicker = stackedPicker(productSearchField, productCombo);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        // SECTION: DOCUMENTO
        row = addSectionHeader(formCard, gbc, row, "DOCUMENTO");
        row = addFullRowField(formCard, gbc, row, "Cliente", clientPicker);
        row = addTwoColumnRow(formCard, gbc, row,
                "Armazém Expedição", warehouseCombo,
                "Conta Tesouraria (Recebimento)", accountCombo);

        // SECTION: ARTIGO
        row = addSectionHeader(formCard, gbc, row, "ARTIGO");
        row = addFullRowField(formCard, gbc, row, "Produto", productPicker);
        row = addTwoColumnRow(formCard, gbc, row,
                "Quantidade", qtyField,
                "Desconto %", discountField);
        row = addTwoColumnRow(formCard, gbc, row,
                "Nº Lote (opcional)", batchField,
                "Nº Série (opcional)", serialField);

        // Action button — full width
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(20, 6, 6, 6);
        ModernButton addToCartBtn = new ModernButton("Adicionar Artigo ao Carrinho", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        addToCartBtn.setIcon(UIHelper.icon("fas-cart-plus", 14));
        formCard.add(addToCartBtn, gbc);

        // Wrap the form in a scroll pane so nothing gets clipped on small displays
        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIHelper.styleScrollPane(formScroll);

        ModernPanel formCardWrapper = new ModernPanel(16);
        formCardWrapper.setLayout(new BorderLayout());
        formCardWrapper.setBorder(new EmptyBorder(16, 16, 16, 16));
        formCardWrapper.add(formScroll, BorderLayout.CENTER);

        leftPanel.add(formCardWrapper, BorderLayout.CENTER);
        workspace.add(leftPanel);

        // RIGHT: CART TABLE & CHECKOUT
        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(UIHelper.createSubheading("Carrinho de Vendas (POS)"), BorderLayout.NORTH);

        ModernPanel cartCard = new ModernPanel(16);
        cartCard.setLayout(new BorderLayout(0, 15));
        cartCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cartCols = {"Artigo", "Preço Unit.", "Qtd", "Desc %", "Lote/Série", "Subtotal"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        UIHelper.styleTable(cartTable);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        UIHelper.styleScrollPane(cartScroll);
        cartCard.add(cartScroll, BorderLayout.CENTER);

        // Cart Actions (Remove & Total & Checkout)
        JPanel cartBottom = new JPanel();
        cartBottom.setLayout(new BoxLayout(cartBottom, BoxLayout.Y_AXIS));
        cartBottom.setOpaque(false);

        // Row 1: Total Label (Aligned Right)
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalLabel = new JLabel("Total POS: 0.00 MT");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(Color.WHITE);
        totalRow.add(totalLabel, BorderLayout.EAST);

        // Row 2: Action Buttons
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        ModernButton removeBtn = new ModernButton("Remover Selecionado", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED.brighter());
        removeBtn.setIcon(UIHelper.icon("fas-trash", 14));
        buttonRow.add(removeBtn, BorderLayout.WEST);

        creditCheck = new JCheckBox("Fiado (cliente paga depois)");
        creditCheck.setForeground(UIHelper.TEXT_LIGHT);
        creditCheck.setOpaque(false);
        creditCheck.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel centerActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerActions.setOpaque(false);
        centerActions.add(creditCheck);
        buttonRow.add(centerActions, BorderLayout.CENTER);

        checkoutBtn = new ModernButton("Finalizar Venda (F9)");
        checkoutBtn.setIcon(UIHelper.icon("fas-check-circle", 14));
        checkoutBtn.setGradient(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.darker());
        buttonRow.add(checkoutBtn, BorderLayout.EAST);

        cartBottom.add(totalRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 10)));
        cartBottom.add(buttonRow);
        cartCard.add(cartBottom, BorderLayout.SOUTH);

        rightPanel.add(cartCard, BorderLayout.CENTER);
        workspace.add(rightPanel);

        add(workspace, BorderLayout.CENTER);

        // LISTENERS
        openSessionBtn.addActionListener(e -> openSession());
        closeSessionBtn.addActionListener(e -> closeSession());
        cashMoveBtn.addActionListener(e -> manageCashMovements());
        addToCartBtn.addActionListener(e -> addToCart());
        removeBtn.addActionListener(e -> removeFromCart());
        checkoutBtn.addActionListener(e -> runCheckout());

        refreshSessionState();
    }

    public void onPanelSelected() {
        refreshSessionState();
        loadMetadata();
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
            openSessionBtn.setVisible(false);
            closeSessionBtn.setVisible(true);
            cashMoveBtn.setVisible(true);
            checkoutBtn.setEnabled(true);
        } else {
            activeSession = null;
            statusLabel.setText("Caixa Fechada. É necessário abrir sessão antes de vender.");
            statusLabel.setForeground(UIHelper.PENDING_YELLOW);
            openSessionBtn.setVisible(true);
            closeSessionBtn.setVisible(false);
            cashMoveBtn.setVisible(false);
            checkoutBtn.setEnabled(false);
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
        productCombo.removeAllItems();
        for (ProductDTO p : filteredProducts) {
            productCombo.addItem(productLabel(p) + " (" + p.unitPrice() + " MT)");
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

        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form), "Novo Cliente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

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
        String balStr = JOptionPane.showInputDialog(this, "Saldo de Abertura do Caixa (MT):", "Abrir Caixa", JOptionPane.QUESTION_MESSAGE);
        if (balStr == null) return;
        try {
            BigDecimal bal = new BigDecimal(balStr.trim());
            if (bal.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "O saldo de abertura deve ser positivo ou zero.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String operator = CurrentUserContext.getUsername();
            Long companyId = CurrentUserContext.getCurrentCompanyId();

            posService.openSession(operator, bal, companyId);
            JOptionPane.showMessageDialog(this, "Sessão de caixa aberta com sucesso!", "Informação", JOptionPane.INFORMATION_MESSAGE);
            refreshSessionState();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeSession() {
        if (activeSession == null) return;

        String balStr = JOptionPane.showInputDialog(this, "Saldo Físico no Fecho do Caixa (MT):", "Fechar Caixa", JOptionPane.QUESTION_MESSAGE);
        if (balStr == null) return;
        try {
            BigDecimal closingReal = new BigDecimal(balStr.trim());
            TillSession closed = posService.closeSession(activeSession.getId(), closingReal);

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

    private void addToCart() {
        if (filteredProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    productsList.isEmpty()
                            ? "Não há produtos cadastrados."
                            : "Nenhum produto corresponde à pesquisa.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int prodIdx = productCombo.getSelectedIndex();
        if (prodIdx < 0) return;

        ProductDTO product = filteredProducts.get(prodIdx);
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser um número inteiro superior a 0.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal discount = BigDecimal.ZERO;
        try {
            discount = new BigDecimal(discountField.getText().trim());
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "O desconto deve ser um número entre 0 e 100.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String batch = batchField.getText().trim();
        if (batch.isEmpty()) batch = null;

        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        CartItem item = new CartItem(product, qty, discount, batch, serial);
        cartItems.add(item);

        String lotSer = "";
        if (batch != null) lotSer += "Lote: " + batch + " ";
        if (serial != null) lotSer += "Série: " + serial;
        if (lotSer.isEmpty()) lotSer = "-";

        cartModel.addRow(new Object[]{
                product.name(),
                String.format("%.2f MT", product.unitPrice()),
                qty,
                discount + "%",
                lotSer,
                String.format("%.2f MT", item.getSubtotal())
        });

        updateCartTotal();

        // Clear item-specific fields
        qtyField.setText("1");
        discountField.setText("0");
        batchField.setText("");
        serialField.setText("");
    }

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
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            total = total.add(item.getSubtotal());
        }
        totalLabel.setText(String.format("Total POS: %,.2f MT", total));
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
        if (clientsList.isEmpty() || warehousesList.isEmpty() || accountsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Falta cadastrar clientes, armazéns ou contas de tesouraria.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int clientIdx = clientCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        int accIdx = accountCombo.getSelectedIndex();

        if (clientIdx < 0 || whIdx < 0 || accIdx < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione cliente, armazém e conta de tesouraria.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ClientDTO client = filteredClients.get(clientIdx);
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

        try {
            boolean fiado = creditCheck != null && creditCheck.isSelected();
            java.util.List<com.phcpro.modules.pos.dto.PosPaymentRequest> payments = null;
            Long treasuryAccountId = acc.id();
            if (fiado) {
                // Soma o total do carrinho para enviar como pagamento CREDIT (valor a deber)
                java.math.BigDecimal cartTotal = java.math.BigDecimal.ZERO;
                for (CartItem item : cartItems) cartTotal = cartTotal.add(item.getSubtotal());
                payments = java.util.List.of(new com.phcpro.modules.pos.dto.PosPaymentRequest(
                        "CREDIT", cartTotal, java.math.BigDecimal.ZERO, "Venda a crédito", null));
                treasuryAccountId = null;  // fiado não move tesouraria
            }
            POSCheckoutRequest request = new POSCheckoutRequest(
                    operator,
                    companyId,
                    client.id(),
                    wh.getId(),
                    treasuryAccountId,
                    lines,
                    payments
            );

            Invoice inv = posService.checkout(request);
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
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao processar checkout: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printReceiptIfConfirmed(Invoice invoice) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Deseja imprimir o recibo da venda " + invoice.getInvoiceNumber() + "?",
                "Imprimir Recibo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            byte[] pdf = receiptPrintService.render(invoice.getId());
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "recibo-" + invoice.getInvoiceNumber());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao imprimir recibo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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

        // Adiciona com quantidade 1 ao carrinho — mesmo formato que addToCart usa
        CartItem item = new CartItem(product, 1, BigDecimal.ZERO, null, null);
        cartItems.add(item);
        cartModel.addRow(new Object[]{
                product.name(),
                String.format("%.2f MT", product.unitPrice()),
                1,
                "0%",
                "-",
                String.format("%.2f MT", item.getSubtotal())
        });
        updateCartTotal();
        barcodeField.setText("");
        barcodeField.requestFocusInWindow();
    }

    // ─── Form-layout helpers ────────────────────────────────────────────────────

    private static JPanel stackedPicker(JComponent top, JComponent bottom) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(bottom, BorderLayout.CENTER);
        return p;
    }

    private static int addSectionHeader(JPanel host, GridBagConstraints gbc, int row, String text) {
        JLabel section = new JLabel(text);
        section.setFont(new Font("Segoe UI", Font.BOLD, 11));
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
}
