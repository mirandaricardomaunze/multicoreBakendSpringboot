package com.phcpro.gui;

import com.phcpro.architecture.pricing.TaxRates;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.DocumentEditorHost;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.CreditNoteApiClient;
import com.phcpro.desktop.client.DebitNoteApiClient;
import com.phcpro.desktop.client.FinanceApiClient;
import com.phcpro.desktop.client.InventoryApiClient;
import com.phcpro.desktop.client.MovimentosApiClient;
import com.phcpro.desktop.client.POSApiClient;
import com.phcpro.desktop.client.PromotionApiClient;
import com.phcpro.modules.comercial.dto.*;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ComercialPanel extends JPanel {

    private final ComercialApiClient comercialApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final FinanceApiClient financeApiClient;

    // TAB 1: FATURAÇÃO ELEMENTS
    private JComboBox<String> clientCombo;
    private JComboBox<String> warehouseCombo;
    private JComboBox<String> productCombo;
    private JTextField quantityField;
    private JTextField invoiceBoxesField;
    private JTextField discountField;
    private JTextField batchField;
    private JTextField serialField;
    private DefaultTableModel linesTableModel;
    private JTable linesTable;
    private JLabel totalLabel;

    private DefaultTableModel invoicesTableModel;
    private JTable invoicesTable;

    // TAB 2: RECIBOS ELEMENTS
    private DefaultTableModel receiptsTableModel;
    private JTable receiptsTable;

    // TAB 3: REGISTAR CLIENTE ELEMENTS

    // TAB 4: ENCOMENDAS ELEMENTS
    private JComboBox<String> orderClientCombo;
    private JTextField orderClientWalkInField;
    private JComboBox<String> orderWarehouseCombo;
    private JComboBox<String> orderProductCombo;
    private JTextField orderQuantityField;
    private JTextField orderBoxesField;
    private JTextField orderDiscountField;
    private JTextField orderBatchField;
    private JTextField orderSerialField;
    private DefaultTableModel orderLinesTableModel;
    private JTable orderLinesTable;
    private JLabel orderTotalLabel;

    private DefaultTableModel ordersTableModel;
    private JTable ordersTable;

    // TAB 5: GUIAS DE REMESSA ELEMENTS
    private DefaultTableModel deliveryGuidesTableModel;
    private JTable deliveryGuidesTable;

    // Seeding lists for selections
    private List<ClientDTO> clientsList = new ArrayList<>();
    private List<ProductDTO> productsList = new ArrayList<>();
    private List<WarehouseDTO> warehousesList = new ArrayList<>();
    
    // In-memory line items of the invoice currently being drafted
    private final List<CreateInvoiceLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftSubtotal = BigDecimal.ZERO;
    private BigDecimal draftTax = BigDecimal.ZERO;
    private BigDecimal draftTotal = BigDecimal.ZERO;

    // In-memory line items of the order currently being drafted
    private final List<CreateInvoiceLineRequest> draftOrderLines = new ArrayList<>();
    private BigDecimal draftOrderSubtotal = BigDecimal.ZERO;
    private BigDecimal draftOrderTax = BigDecimal.ZERO;
    private BigDecimal draftOrderTotal = BigDecimal.ZERO;


    private final CreditNoteApiClient creditNoteApiClient;
    private final DebitNoteApiClient debitNoteApiClient;
    private final POSApiClient posApiClient;
    private final MovimentosApiClient movimentosApiClient;

    private JPanel invoiceFormContent;              // conteúdo do modal de nova fatura
    private com.phcpro.modules.comercial.dto.InvoiceDTO lastCreatedInvoice;
    private JPanel orderFormContent;                // conteúdo do editor de nova encomenda
    private OrderDTO lastCreatedOrder;
    private CardLayout encomendasCards;             // alterna lista <-> editor na aba Encomendas
    private JPanel encomendasHost;
    private CardLayout faturacaoCards;              // alterna lista <-> editor na aba Faturação
    private JPanel faturacaoHost;
    private DefaultTableModel movimentosModel;
    private JTable movimentosTable;
    private JTextField movimentosSearch;
    private JComboBox<String> movimentosPeriod;
    private java.util.List<com.phcpro.modules.movimentos.dto.MovimentoDTO> movimentosData = java.util.List.of();
    private JLabel movimentosFooter;

    public ComercialPanel(
            ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient,
            CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient,
            POSApiClient posApiClient,
            MovimentosApiClient movimentosApiClient,
            PromotionApiClient promotionApiClient
    ) {
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.financeApiClient = financeApiClient;
        this.creditNoteApiClient = creditNoteApiClient;
        this.debitNoteApiClient = debitNoteApiClient;
        this.posApiClient = posApiClient;
        this.movimentosApiClient = movimentosApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabbedPane);

        // TAB 1: FATURAÇÃO
        JPanel tabFaturacao = createFaturacaoTab();
        tabbedPane.addTab("Faturação (FT)", UIHelper.icon("fas-file-invoice", 16, UIHelper.TEXT_LIGHT), tabFaturacao);

        // TAB 2: RECIBOS
        JPanel tabRecibos = createRecibosTab();
        tabbedPane.addTab("Recibos (RC)", UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT), tabRecibos);

        // Registo/gestão de clientes vive na área "Clientes" do menu de topo, não aqui.

        // TAB 4: ENCOMENDAS (EC)
        JPanel tabEncomendas = createEncomendasTab();
        tabbedPane.addTab("Encomendas (EC)", UIHelper.icon("fas-file-signature", 16, UIHelper.TEXT_LIGHT), tabEncomendas);

        // TAB 5: GUIAS DE REMESSA (GR)
        tabbedPane.addTab("Guias de Remessa (GR)", UIHelper.icon("fas-truck", 16, UIHelper.TEXT_LIGHT),
                createDeliveryGuidesTab());

        // TAB 6: NOTAS DE CRÉDITO (NC)
        tabbedPane.addTab("Notas de Crédito (NC)", UIHelper.icon("fas-undo-alt", 16, UIHelper.TEXT_LIGHT), createCreditNotesTab());

        // TAB 7: NOTAS DE DÉBITO (ND)
        tabbedPane.addTab("Notas de Débito (ND)", UIHelper.icon("fas-plus-circle", 16, UIHelper.TEXT_LIGHT), createDebitNotesTab());

        // TAB 8: CONTAS CORRENTES (FIADOS)
        tabbedPane.addTab("Contas Correntes", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.TEXT_LIGHT), createOutstandingTab());

        // TAB 9: PROMOÇÕES
        tabbedPane.addTab("Promoções", UIHelper.icon("fas-tags", 16, UIHelper.TEXT_LIGHT),
                new PromotionsPanel(promotionApiClient, comercialApiClient));

        // TAB 10: MOVIMENTOS (vista unificada de todos os documentos comerciais)
        tabbedPane.addTab("Movimentos", UIHelper.icon("fas-list-alt", 16, UIHelper.TEXT_LIGHT), createMovimentosTab());

        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
    }


    private JPanel createFaturacaoTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // LEFT COLUMN: CREATE INVOICE FORM
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);

        JPanel leftHeader = new JPanel(new BorderLayout(8, 0));
        leftHeader.setOpaque(false);
        JLabel leftTitle = UIHelper.createHeading("Emitir Nova Fatura");
        leftHeader.add(leftTitle, BorderLayout.WEST);
        ModernButton billFromOrderBtn = UIHelper.createPrimaryButton("Faturar Encomenda…");
        billFromOrderBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        billFromOrderBtn.setToolTipText("Escolher uma encomenda pendente e gerar fatura automaticamente.");
        billFromOrderBtn.addActionListener(e -> openBillFromOrderDialog());
        leftHeader.add(billFromOrderBtn, BorderLayout.EAST);
        leftPanel.add(leftHeader, BorderLayout.NORTH);

        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 1: Client & Warehouse Selection (Side by Side)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.insets = new Insets(4, 8, 2, 8);
        JLabel clientLbl = new JLabel("Cliente:");
        clientLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(clientLbl, gbc);

        gbc.gridx = 1;
        JLabel warehouseLbl = new JLabel("Armazém de Expedição:");
        warehouseLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(warehouseLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(2, 8, 8, 8);
        clientCombo = new JComboBox<>();
        UIHelper.styleComboBox(clientCombo);
        formCard.add(clientCombo, gbc);

        gbc.gridx = 1;
        warehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(warehouseCombo);
        formCard.add(warehouseCombo, gbc);

        // Row 2: Product Selection (Full Width)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 8, 2, 8);
        JLabel prodLbl = new JLabel("Produto / Serviço:");
        prodLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(prodLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 8, 8, 8);
        productCombo = new JComboBox<>();
        UIHelper.styleComboBox(productCombo);
        formCard.add(productCombo, gbc);

        // Row 3: Qtd & Desconto % (Side by Side)
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel qtyLbl = new JLabel("Qtd (unidades):");
        qtyLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(qtyLbl, gbc);

        gbc.gridx = 1;
        JLabel discLbl = new JLabel("Desconto %:");
        discLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(discLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.insets = new Insets(2, 8, 12, 8);
        // Qtd em unidades + helper opcional "Caixas" (grosso): caixas × und/caixa → preenche a Qtd.
        JPanel qtyRow = new JPanel(new BorderLayout(6, 0));
        qtyRow.setOpaque(false);
        quantityField = new JTextField("1");
        UIHelper.styleTextField(quantityField);
        JPanel boxHelper = new JPanel(new BorderLayout(4, 0));
        boxHelper.setOpaque(false);
        JLabel cxLbl = new JLabel("Caixas:");
        cxLbl.setForeground(UIHelper.TEXT_MUTED);
        invoiceBoxesField = new JTextField(4);
        UIHelper.styleTextField(invoiceBoxesField);
        invoiceBoxesField.setToolTipText("Venda ao grosso: preenche a Qtd em unidades = caixas × unidades/caixa do produto.");
        boxHelper.add(cxLbl, BorderLayout.WEST);
        boxHelper.add(invoiceBoxesField, BorderLayout.CENTER);
        qtyRow.add(quantityField, BorderLayout.CENTER);
        qtyRow.add(boxHelper, BorderLayout.EAST);
        formCard.add(qtyRow, gbc);

        gbc.gridx = 1;
        discountField = new JTextField("0");
        UIHelper.styleTextField(discountField);
        formCard.add(discountField, gbc);

        // Row 4: Lote/Validade (FEFO, read-only) e Série
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel batchLbl = new JLabel("Lote / Validade (FEFO):");
        batchLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(batchLbl, gbc);

        gbc.gridx = 1;
        JLabel serialLbl = new JLabel("Série (Opcional):");
        serialLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(serialLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.insets = new Insets(2, 8, 12, 8);
        batchField = new JTextField();
        UIHelper.styleTextField(batchField);
        batchField.setEditable(false);
        batchField.setToolTipText("Lote a sair (FEFO) — calculado a partir do produto e armazém.");
        batchField.putClientProperty("JTextField.placeholderText", "— FEFO automático —");
        formCard.add(batchField, gbc);

        gbc.gridx = 1;
        serialField = new JTextField();
        UIHelper.styleTextField(serialField);
        formCard.add(serialField, gbc);

        // Row 5: line action
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 8, 12, 8);
        ModernButton addLineBtn = UIHelper.createAddLineButton();
        JPanel addLineActionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addLineActionRow.setOpaque(false);
        addLineActionRow.add(addLineBtn);
        formCard.add(addLineActionRow, gbc);

        // Row 6: Draft Lines Table (Full Width)
        gbc.gridy = 9; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        String[] lineCols = {"Produto", "Qtd", "Preço Unit.", "Desc %", "Lote/Série", "Total"};
        linesTableModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        linesTable = new JTable(linesTableModel);
        UIHelper.styleTable(linesTable);
        JScrollPane linesScroll = new JScrollPane(linesTable);
        UIHelper.styleEmbeddedTableScrollPane(linesScroll, linesTable, 4);
        // Draft table is placed in its own card below the input form.

        // Row 7: Total summary (a emissão é feita pelo botão Gravar do modal)
        totalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        totalLabel.setForeground(Color.WHITE);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        totalRow.add(totalLabel, BorderLayout.EAST);

        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        draftCard.setPreferredSize(new Dimension(0, 280));
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(totalRow, BorderLayout.SOUTH);

        // Conteúdo do formulário (vai para o modal responsivo): inputs + linhas de rascunho.
        JPanel formContent = new JPanel(new BorderLayout(0, 12));
        formContent.setOpaque(false);
        formContent.add(formCard, BorderLayout.NORTH);
        JPanel draftWrap = new JPanel(new BorderLayout(0, 8));
        draftWrap.setOpaque(false);
        draftWrap.add(UIHelper.createSubheading("Linhas da Fatura (Rascunho)"), BorderLayout.NORTH);
        draftWrap.add(draftCard, BorderLayout.CENTER);
        formContent.add(draftWrap, BorderLayout.CENTER);
        this.invoiceFormContent = formContent;

        // TAB: cabeçalho com acções + lista de faturas em ecrã inteiro.
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        headerBar.add(UIHelper.createHeading("Faturas Recentes"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton newInvoiceBtn = UIHelper.createPrimaryButton("Nova Fatura…");
        newInvoiceBtn.setIcon(UIHelper.icon("fas-file-invoice", 14));
        newInvoiceBtn.addActionListener(e -> openInvoiceEditor());
        headerActions.add(billFromOrderBtn);
        headerActions.add(newInvoiceBtn);
        headerBar.add(headerActions, BorderLayout.EAST);
        panel.add(headerBar, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] invoicesCols = {"ID", "Nº Fatura", "Cliente", "Estado", "Total"};
        invoicesTableModel = new DefaultTableModel(invoicesCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        invoicesTable = new JTable(invoicesTableModel);
        UIHelper.styleTable(invoicesTable);
        
        // Hide ID column
        invoicesTable.getColumnModel().getColumn(0).setMinWidth(0);
        invoicesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        invoicesTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane invoicesScroll = new JScrollPane(invoicesTable);
        UIHelper.styleScrollPane(invoicesScroll);
        JTextField invSearch = TableFilter.searchField("Nº fatura ou cliente…");
        JComboBox<String> invEstado = TableFilter.combo("Todos os estados",
                "DRAFT", "PENDING_APPROVAL", "PENDING_DISCOUNT_APPROVAL", "APPROVED",
                "PARTIALLY_PAID", "REJECTED", "PAID", "CANCELLED");
        TableFilter.install(invoicesTable, invSearch, new TableFilter.ColumnFilter(invEstado, 3));
        JPanel invBar = TableFilter.bar(invSearch, TableFilter.label("Estado:"), invEstado);
        invBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(invBar, BorderLayout.NORTH);
        listCard.add(invoicesScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        ModernButton printInvoiceBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printInvoiceBtn.setIcon(UIHelper.icon("fas-print", 14));
        ModernButton printGuideBtn = UIHelper.createSecondaryButton("Imprimir Guia");
        printGuideBtn.setIcon(UIHelper.icon("fas-truck", 14));
        ModernButton exportTableBtn = UIHelper.createSecondaryButton("Exportar Tabela");
        exportTableBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        ModernButton cancelInvoiceBtn = UIHelper.createDangerButton("Anular Fatura");
        cancelInvoiceBtn.setIcon(UIHelper.icon("fas-ban", 14));
        ModernButton payInvoiceBtn = UIHelper.createSuccessButton("Liquidar (RC)");
        payInvoiceBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        btnPanel.add(printInvoiceBtn);
        btnPanel.add(printGuideBtn);
        btnPanel.add(exportTableBtn);
        btnPanel.add(cancelInvoiceBtn);
        btnPanel.add(payInvoiceBtn);
        btnPanel.add(refreshBtn);
        listCard.add(btnPanel, BorderLayout.SOUTH);

        // Lista de faturas ocupa a tab inteira; o formulário vive no modal.
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        addLineBtn.addActionListener(e -> addDraftLine());
        productCombo.addActionListener(e -> { refreshInvoiceFEFOHint(); applyInvoiceBoxes(); });
        warehouseCombo.addActionListener(e -> refreshInvoiceFEFOHint());
        UIHelper.onTextChange(invoiceBoxesField, this::applyInvoiceBoxes);
        cancelInvoiceBtn.addActionListener(e -> cancelSelectedInvoice());
        payInvoiceBtn.addActionListener(e -> paySelectedInvoice());
        refreshBtn.addActionListener(e -> loadInvoicesTable());
        printInvoiceBtn.addActionListener(e -> printSelectedInvoice());
        printGuideBtn.addActionListener(e -> printSelectedGuide());
        exportTableBtn.addActionListener(e -> exportInvoicesTable());

        // Documento em painel completo (substitui o modal): a aba alterna lista <-> editor.
        DocumentEditorHost invoiceEditor = new DocumentEditorHost(
                "Nova Fatura", invoiceFormContent,
                this::saveInvoiceFromEditor,
                this::backToInvoicesList,
                () -> !draftLines.isEmpty());
        faturacaoCards = new CardLayout();
        faturacaoHost = new JPanel(faturacaoCards);
        faturacaoHost.setOpaque(false);
        faturacaoHost.add(panel, "list");
        faturacaoHost.add(invoiceEditor, "editor");
        return faturacaoHost;
    }

    /** Abre o editor de nova fatura (painel completo, substitui o modal). */
    private void openInvoiceEditor() {
        if (clientsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum cliente disponível.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum armazém disponível para a empresa atual.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resetInvoiceDraft();
        lastCreatedInvoice = null;
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "editor");
        }
    }

    private void backToInvoicesList() {
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "list");
        }
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    private void saveInvoiceFromEditor() {
        try {
            submitInvoiceOrThrow();
            InvoiceDTO created = lastCreatedInvoice;
            if (created.status() == InvoiceStatus.PENDING_DISCOUNT_APPROVAL) {
                JOptionPane.showMessageDialog(this, "Fatura " + created.invoiceNumber() + " emitida!\n"
                        + "Bloqueada para Aprovação de Desconto (superior a 10%).\n"
                        + "Valor: " + created.totalAmount() + " MT.", "Bloqueio de Desconto", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Fatura " + created.invoiceNumber() + " emitida com sucesso!\n"
                        + "Valor: " + created.totalAmount() + " MT.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
            loadInvoicesTable();
            backToInvoicesList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Falha ao emitir fatura." : ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createRecibosTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel title = UIHelper.createHeading("Recibos Emitidos (Liquidações)");
        panel.add(title, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] cols = {"ID", "Nº Recibo", "Fatura", "Cliente", "Montante Pago", "Método Pag.", "Estado", "Data"};
        receiptsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        receiptsTable = new JTable(receiptsTableModel);
        UIHelper.styleTable(receiptsTable);

        receiptsTable.getColumnModel().getColumn(0).setMinWidth(0);
        receiptsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        receiptsTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scroll = new JScrollPane(receiptsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField rcSearch = TableFilter.searchField("Nº recibo, fatura ou cliente…");
        JComboBox<String> rcMetodo = TableFilter.combo("Todos os métodos",
                "CASH", "BANK_TRANSFER", "CARD");
        JComboBox<String> rcEstado = TableFilter.combo("Todos os estados",
                "COMPLETED", "CANCELLED");
        JComboBox<String> rcPeriodo = TableFilter.periodCombo();
        TableFilter.install(receiptsTable, rcSearch,
                java.util.List.of(new TableFilter.ColumnFilter(rcMetodo, 5),
                        new TableFilter.ColumnFilter(rcEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(rcPeriodo, 7)));
        JPanel rcBar = TableFilter.bar(rcSearch,
                TableFilter.label("Método:"), rcMetodo,
                TableFilter.label("Estado:"), rcEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), rcPeriodo);
        rcBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(rcBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton cancelReceiptBtn = UIHelper.createDangerButton("Anular Recibo");
        cancelReceiptBtn.setIcon(UIHelper.icon("fas-ban", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        btnPanel.add(cancelReceiptBtn);
        btnPanel.add(refreshBtn);
        card.add(btnPanel, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);

        // LISTENERS
        cancelReceiptBtn.addActionListener(e -> cancelSelectedReceipt());
        refreshBtn.addActionListener(e -> loadReceiptsTable());

        return panel;
    }

    public void onPanelSelected() {
        loadClientsAndProducts();
        loadWarehouses();
        loadInvoicesTable();
        loadReceiptsTable();
        loadOrdersTable();
        loadDeliveryGuidesTable();
        loadCreditNotesTable();
        loadDebitNotesTable();
        loadOutstandingTable();
        loadMovimentosTable();
    }

    private void loadClientsAndProducts() {
        clientCombo.removeAllItems();
        productCombo.removeAllItems();
        orderClientCombo.removeAllItems();
        orderProductCombo.removeAllItems();

        clientsList = comercialApiClient.getAllClients();
        productsList = comercialApiClient.getAllProducts();

        // Encomendas aceitam venda sem cliente registado — primeiro item do combo.
        orderClientCombo.addItem("— Consumidor Final (sem registo) —");

        for (ClientDTO c : clientsList) {
            clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
            orderClientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }

        for (ProductDTO p : productsList) {
            productCombo.addItem(productLabel(p) + " - " + p.unitPrice() + " MT");
            orderProductCombo.addItem(productLabel(p) + " - " + p.unitPrice() + " MT");
        }
    }

    private String productLabel(ProductDTO p) {
        String code = p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        if (p.barcode() != null && !p.barcode().isBlank()) {
            return code + " | " + p.barcode() + " - " + p.name();
        }
        return code + " - " + p.name();
    }

    private void loadWarehouses() {
        warehouseCombo.removeAllItems();
        orderWarehouseCombo.removeAllItems();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        warehousesList = inventoryApiClient.getWarehousesByCompany(companyId);

        for (WarehouseDTO w : warehousesList) {
            warehouseCombo.addItem(w.name());
            orderWarehouseCombo.addItem(w.name());
        }
    }


    /**
     * Helper de venda ao grosso: se o operador indicar um nº de caixas e houver produto seleccionado,
     * preenche a Qtd em UNIDADES = caixas × unidades/caixa. O cálculo de dinheiro continua por unidade
     * (a caixa é só conversão). Campo vazio não mexe na Qtd (permite entrada directa em unidades).
     */
    private void applyInvoiceBoxes() {
        if (invoiceBoxesField == null) return;
        String raw = invoiceBoxesField.getText().trim();
        if (raw.isEmpty()) return;
        int idx = productCombo.getSelectedIndex();
        if (idx < 0 || idx >= productsList.size()) return;
        int upb = Math.max(1, productsList.get(idx).unitsPerBox());
        try {
            int boxes = Integer.parseInt(raw);
            if (boxes <= 0) return;
            quantityField.setText(String.valueOf(boxes * upb));
        } catch (NumberFormatException ignore) {
            // texto inválido → não altera a Qtd
        }
    }

    private void addDraftLine() {
        if (productsList.isEmpty()) return;

        int selectedProdIdx = productCombo.getSelectedIndex();
        if (selectedProdIdx < 0) return;

        ProductDTO product = productsList.get(selectedProdIdx);

        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser um número inteiro superior a zero.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal discount = BigDecimal.ZERO;
        try {
            discount = new BigDecimal(discountField.getText().trim());
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "O desconto deve ser um número decimal entre 0 e 100.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Lote é decidido por FEFO no backend — batchField mostra apenas previsão.
        String previewBatch = batchField.getText().trim();
        String batch = null;

        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        BigDecimal taxRate = TaxRates.STANDARD_VAT;

        CreateInvoiceLineRequest lineRequest = new CreateInvoiceLineRequest(
                product.id(),
                qty,
                taxRate,
                discount,
                batch,
                serial
        );
        draftLines.add(lineRequest);

        // Add to GUI table
        BigDecimal subTotal = product.unitPrice().multiply(BigDecimal.valueOf(qty));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discAmt = subTotal.multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            subTotal = subTotal.subtract(discAmt);
        }
        BigDecimal tax = subTotal.multiply(taxRate);
        BigDecimal total = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        String lotSer = "";
        if (!previewBatch.isEmpty() && !"Sem stock".equals(previewBatch)) {
            lotSer += "FEFO: " + previewBatch + " ";
        }
        if (serial != null) lotSer += "S: " + serial;
        if (lotSer.isEmpty()) lotSer = "-";

        linesTableModel.addRow(new Object[]{
                product.name(),
                qty,
                product.unitPrice() + " MT",
                discount + "%",
                lotSer,
                total + " MT"
        });

        // Accumulate totals
        draftSubtotal = draftSubtotal.add(subTotal);
        draftTax = draftTax.add(tax);
        draftTotal = draftTotal.add(total);

        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));

        // Clear details
        quantityField.setText("1");
        invoiceBoxesField.setText("");
        discountField.setText("0");
        serialField.setText("");
        refreshInvoiceFEFOHint();
    }

    /** Abre o formulário de nova fatura num modal responsivo (com scroll). */
    /** Validação + emissão. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private void submitInvoiceOrThrow() {
        if (draftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à fatura.");
        }
        int clientIdx = clientCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        if (clientIdx < 0 || whIdx < 0) {
            throw new RuntimeException("Selecione cliente e armazém.");
        }
        ClientDTO client = clientsList.get(clientIdx);
        WarehouseDTO warehouse = warehousesList.get(whIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        CreateInvoiceRequest request = new CreateInvoiceRequest(client.id(), companyId, warehouse.id(), draftLines);
        lastCreatedInvoice = comercialApiClient.createInvoice(request);
        resetInvoiceDraft();
    }

    private void resetInvoiceDraft() {
        draftLines.clear();
        draftSubtotal = BigDecimal.ZERO;
        draftTax = BigDecimal.ZERO;
        draftTotal = BigDecimal.ZERO;
        if (linesTableModel != null) linesTableModel.setRowCount(0);
        if (totalLabel != null) totalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
    }

    private void cancelSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela para anular.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);

        String reason = UIHelper.promptRequiredText("Anular Fatura", "fas-ban",
                "Fatura " + invoiceNum, "Motivo da anulação:");
        if (reason == null) return;

        try {
            comercialApiClient.cancelInvoice(invoiceId, reason);
            JOptionPane.showMessageDialog(this, "Fatura " + invoiceNum + " anulada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadInvoicesTable();
            loadReceiptsTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao anular fatura: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void paySelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela para liquidar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);
        String statusStr = (String) invoicesTableModel.getValueAt(row, 3);
        String totalStr = (String) invoicesTableModel.getValueAt(row, 4).toString().replace(" MT", "").replace(",", ".");

        if (!"APPROVED".equalsIgnoreCase(statusStr)) {
            JOptionPane.showMessageDialog(this, "Apenas faturas no estado APPROVED podem ser liquidadas. Estado atual: " + statusStr, "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal invoiceTotal = new BigDecimal(totalStr);

        // Load accounts
        List<TreasuryAccountDTO> accounts = financeApiClient.getAllAccounts();
        if (accounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem contas de tesouraria registadas.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JComboBox<String> accCombo = new JComboBox<>();
        UIHelper.styleComboBox(accCombo);
        for (TreasuryAccountDTO acc : accounts) {
            accCombo.addItem(acc.name() + " (" + acc.balance() + " MT)");
        }

        String[] paymentMethods = {"DINHEIRO", "TRANSFERÊNCIA", "M-PESA", "CARTÃO"};
        JComboBox<String> methodCombo = new JComboBox<>(paymentMethods);
        UIHelper.styleComboBox(methodCombo);

        JTextField amountField = new JTextField(invoiceTotal.toString());
        UIHelper.styleTextField(amountField);

        JLabel invoiceLbl = new JLabel(invoiceNum);
        invoiceLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        invoiceLbl.setForeground(UIHelper.TEXT_LIGHT);

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Fatura:", invoiceLbl,
                "Conta de Tesouraria:", accCombo,
                "Método de Pagamento:", methodCombo,
                "Montante a Receber (MT):", amountField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Registar Recebimento (Emitir Recibo)", "fas-receipt", "Recebimento de cliente e emissão de recibo", dialogPanel).setConfirmButton("Receber", "fas-money-bill-wave").showDialog();
        if (confirmed) {
            try {
                int accIdx = accCombo.getSelectedIndex();
                if (accIdx < 0) return;
                Long accId = accounts.get(accIdx).id();

                String paymentMethod = (String) methodCombo.getSelectedItem();
                BigDecimal amountPaid = new BigDecimal(amountField.getText().trim());

                if (amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(this, "O valor pago deve ser maior que zero.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                comercialApiClient.createReceipt(invoiceId, accId, paymentMethod, amountPaid);
                JOptionPane.showMessageDialog(this, "Fatura liquidada com sucesso! Recibo (RC) emitido.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
                loadInvoicesTable();
                loadReceiptsTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Valor inválido para o montante pago.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao liquidar fatura: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void loadInvoicesTable() {
        invoicesTableModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<InvoiceDTO> invoices = comercialApiClient.getInvoicesByCompany(companyId);
        for (InvoiceDTO invoice : invoices) {
            invoicesTableModel.addRow(new Object[]{
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.clientName(),
                    invoice.status().name(),
                    invoice.totalAmount() + " MT"
            });
        }
    }

    private void loadReceiptsTable() {
        receiptsTableModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<ReceiptDTO> receipts = comercialApiClient.getReceiptsByCompany(companyId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (ReceiptDTO r : receipts) {
            receiptsTableModel.addRow(new Object[]{
                    r.id(),
                    r.receiptNumber(),
                    r.invoiceNumber(),
                    r.clientName(),
                    r.amountPaid() + " MT",
                    r.paymentMethod(),
                    r.status(),
                    r.receiptDate().format(dtf)
            });
        }
    }

    private void cancelSelectedReceipt() {
        int row = TableFilter.selectedModelRow(receiptsTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um recibo na tabela para anular.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long receiptId = (Long) receiptsTableModel.getValueAt(row, 0);
        String receiptNum = (String) receiptsTableModel.getValueAt(row, 1);

        String reason = UIHelper.promptRequiredText("Anular Recibo", "fas-ban",
                "Recibo " + receiptNum, "Motivo da anulação:");
        if (reason == null) return;

        try {
            comercialApiClient.cancelReceipt(receiptId, reason);
            JOptionPane.showMessageDialog(this, "Recibo " + receiptNum + " anulado com sucesso!\nStatus da fatura revertido para APROVADA.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadInvoicesTable();
            loadReceiptsTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao anular recibo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createEncomendasTab() {
        // Igual às Faturas: o formulário vive num modal ('Nova Encomenda'); a lista ocupa a aba inteira.
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // ===== FORMULÁRIO (conteúdo do modal): inputs de cabeçalho + linha =====
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 1: Client & Warehouse Selection (Side by Side)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel clientLbl = new JLabel("Cliente:");
        clientLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(clientLbl, gbc);

        gbc.gridx = 1;
        JLabel warehouseLbl = new JLabel("Armazém:");
        warehouseLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(warehouseLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(2, 8, 12, 8);
        orderClientCombo = new JComboBox<>();
        UIHelper.styleComboBox(orderClientCombo);
        formCard.add(orderClientCombo, gbc);

        gbc.gridx = 1;
        orderWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(orderWarehouseCombo);
        formCard.add(orderWarehouseCombo, gbc);

        // Row extra: nome livre do comprador (opcional, só relevante se cliente = "Consumidor Final").
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel walkInLbl = new JLabel("Nome do comprador (opcional, se 'Consumidor Final'):");
        walkInLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(walkInLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 8, 12, 8);
        orderClientWalkInField = new JTextField();
        UIHelper.styleTextField(orderClientWalkInField);
        orderClientWalkInField.putClientProperty("JTextField.placeholderText",
                "Escrever nome se a encomenda for para 'Consumidor Final' (deixar vazio caso contrário)");
        formCard.add(orderClientWalkInField, gbc);

        // Row 2: Product Selection (Full Width)
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel prodLbl = new JLabel("Produto / Serviço:");
        prodLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(prodLbl, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(2, 8, 12, 8);
        orderProductCombo = new JComboBox<>();
        UIHelper.styleComboBox(orderProductCombo);
        formCard.add(orderProductCombo, gbc);

        // Row 3: Qtd & Desconto % (Side by Side)
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel qtyLbl = new JLabel("Qtd (unidades):");
        qtyLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(qtyLbl, gbc);

        gbc.gridx = 1;
        JLabel discLbl = new JLabel("Desconto %:");
        discLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(discLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.insets = new Insets(2, 8, 12, 8);
        // Qtd em unidades + helper opcional "Caixas" (grosso): caixas × und/caixa → preenche a Qtd.
        JPanel orderQtyRow = new JPanel(new BorderLayout(6, 0));
        orderQtyRow.setOpaque(false);
        orderQuantityField = new JTextField("1");
        UIHelper.styleTextField(orderQuantityField);
        JPanel orderBoxHelper = new JPanel(new BorderLayout(4, 0));
        orderBoxHelper.setOpaque(false);
        JLabel orderCxLbl = new JLabel("Caixas:");
        orderCxLbl.setForeground(UIHelper.TEXT_MUTED);
        orderBoxesField = new JTextField(4);
        UIHelper.styleTextField(orderBoxesField);
        orderBoxesField.setToolTipText("Venda ao grosso: preenche a Qtd em unidades = caixas × unidades/caixa do produto.");
        orderBoxHelper.add(orderCxLbl, BorderLayout.WEST);
        orderBoxHelper.add(orderBoxesField, BorderLayout.CENTER);
        orderQtyRow.add(orderQuantityField, BorderLayout.CENTER);
        orderQtyRow.add(orderBoxHelper, BorderLayout.EAST);
        formCard.add(orderQtyRow, gbc);

        gbc.gridx = 1;
        orderDiscountField = new JTextField("0");
        UIHelper.styleTextField(orderDiscountField);
        formCard.add(orderDiscountField, gbc);

        // Row 4: Lote/Validade (FEFO, read-only) e Série
        gbc.gridx = 0; gbc.gridy = 8;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel batchLbl = new JLabel("Lote / Validade (FEFO):");
        batchLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(batchLbl, gbc);

        gbc.gridx = 1;
        JLabel serialLbl = new JLabel("Série (Opcional):");
        serialLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(serialLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        gbc.insets = new Insets(2, 8, 12, 8);
        orderBatchField = new JTextField();
        UIHelper.styleTextField(orderBatchField);
        orderBatchField.setEditable(false);
        orderBatchField.setToolTipText("Lote a sair (FEFO) — calculado a partir do produto e armazém.");
        orderBatchField.putClientProperty("JTextField.placeholderText", "— FEFO automático —");
        formCard.add(orderBatchField, gbc);

        gbc.gridx = 1;
        orderSerialField = new JTextField();
        UIHelper.styleTextField(orderSerialField);
        formCard.add(orderSerialField, gbc);

        // Row 5: action aligned below the line fields.
        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 8, 12, 8);
        ModernButton addLineBtn = UIHelper.createAddLineButton();

        JPanel addLineActionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addLineActionRow.setOpaque(false);
        addLineActionRow.add(addLineBtn);
        formCard.add(addLineActionRow, gbc);

        // ===== Cartão de rascunho: tabela de linhas + total (separado do formulário, igual às Faturas) =====
        String[] lineCols = {"Produto", "Qtd", "Preço Unit.", "Desc %", "Lote/Série", "Total"};
        orderLinesTableModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        orderLinesTable = new JTable(orderLinesTableModel);
        UIHelper.styleTable(orderLinesTable);
        orderLinesTable.setFillsViewportHeight(true);
        orderLinesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        orderLinesTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        orderLinesTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        orderLinesTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        orderLinesTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        orderLinesTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        orderLinesTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        JScrollPane linesScroll = new JScrollPane(orderLinesTable);
        UIHelper.styleScrollPane(linesScroll);

        orderTotalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        orderTotalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        orderTotalLabel.setForeground(Color.WHITE);
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        totalRow.add(orderTotalLabel, BorderLayout.EAST);

        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        draftCard.setPreferredSize(new Dimension(0, 280));
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(totalRow, BorderLayout.SOUTH);

        // Conteúdo do modal 'Nova Encomenda': inputs (NORTH) + linhas de rascunho (CENTER).
        JPanel formContent = new JPanel(new BorderLayout(0, 12));
        formContent.setOpaque(false);
        formContent.add(formCard, BorderLayout.NORTH);
        JPanel draftWrap = new JPanel(new BorderLayout(0, 8));
        draftWrap.setOpaque(false);
        draftWrap.add(UIHelper.createSubheading("Linhas da Encomenda (Rascunho)"), BorderLayout.NORTH);
        draftWrap.add(draftCard, BorderLayout.CENTER);
        formContent.add(draftWrap, BorderLayout.CENTER);
        this.orderFormContent = formContent;

        // ===== ABA: cabeçalho com acção 'Nova Encomenda…' + lista em ecrã inteiro (igual às Faturas) =====
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        headerBar.add(UIHelper.createHeading("Encomendas Recentes"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Nova Encomenda…");
        newOrderBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newOrderBtn.addActionListener(e -> openOrderEditor());
        headerActions.add(newOrderBtn);
        headerBar.add(headerActions, BorderLayout.EAST);
        panel.add(headerBar, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] ordersCols = {"ID", "Nº Encomenda", "Cliente", "Estado", "Total", "Impressões"};
        ordersTableModel = new DefaultTableModel(ordersCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ordersTable = new JTable(ordersTableModel);
        UIHelper.styleTable(ordersTable);
        ordersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        ordersTable.setFillsViewportHeight(true);

        // Hide ID column (col 0)
        ordersTable.getColumnModel().getColumn(0).setMinWidth(0);
        ordersTable.getColumnModel().getColumn(0).setMaxWidth(0);
        ordersTable.getColumnModel().getColumn(0).setWidth(0);
        // Larguras proporcionais — Swing distribui o que faltar pelo restante espaço.
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Nº Encomenda
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(170);  // Cliente
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(75);   // Estado
        ordersTable.getColumnModel().getColumn(4).setPreferredWidth(95);   // Total
        ordersTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Impressões

        JScrollPane ordersScroll = new JScrollPane(ordersTable);
        UIHelper.styleScrollPane(ordersScroll);

        JTextField ecSearch = TableFilter.searchField("Nº encomenda ou cliente…");
        JComboBox<String> ecEstado = TableFilter.combo("Todos os estados",
                "PENDING", "PENDING_APPROVAL", "GUIDE_PENDING", "GUIDED", "BILLED", "CANCELLED");
        TableFilter.install(ordersTable, ecSearch, new TableFilter.ColumnFilter(ecEstado, 3));
        JPanel ecBar = TableFilter.bar(ecSearch, TableFilter.label("Estado:"), ecEstado);
        ecBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(ecBar, BorderLayout.NORTH);
        listCard.add(ordersScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        ModernButton viewDetailsBtn = UIHelper.createSecondaryButton("Ver Detalhes");
        viewDetailsBtn.setIcon(UIHelper.icon("fas-eye", 14));
        ModernButton printOrderBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printOrderBtn.setIcon(UIHelper.icon("fas-print", 14));
        ModernButton exportOrdersBtn = UIHelper.createSecondaryButton("Exportar Tabela");
        exportOrdersBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        ModernButton billOrderBtn = UIHelper.createSuccessButton("Faturar Encomenda");
        billOrderBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        ModernButton convertGuideBtn = UIHelper.createPrimaryButton("Converter em Guia");
        convertGuideBtn.setIcon(UIHelper.icon("fas-truck", 14));
        convertGuideBtn.setToolTipText("Criar uma Guia de Remessa a partir da encomenda aprovada selecionada.");
        ModernButton cancelOrderBtn = UIHelper.createDangerButton("Cancelar Encomenda…");
        cancelOrderBtn.setIcon(UIHelper.icon("fas-ban", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        btnPanel.add(viewDetailsBtn);
        btnPanel.add(printOrderBtn);
        btnPanel.add(exportOrdersBtn);
        btnPanel.add(billOrderBtn);
        btnPanel.add(convertGuideBtn);
        btnPanel.add(cancelOrderBtn);
        btnPanel.add(refreshBtn);
        listCard.add(btnPanel, BorderLayout.SOUTH);

        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        addLineBtn.addActionListener(e -> addDraftOrderLine());
        orderProductCombo.addActionListener(e -> { refreshOrderFEFOHint(); applyOrderBoxes(); });
        UIHelper.onTextChange(orderBoxesField, this::applyOrderBoxes);
        orderWarehouseCombo.addActionListener(e -> refreshOrderFEFOHint());
        billOrderBtn.addActionListener(e -> billSelectedOrder());
        convertGuideBtn.addActionListener(e -> convertSelectedOrderToGuide());
        cancelOrderBtn.addActionListener(e -> openCancelOrderDialog());
        refreshBtn.addActionListener(e -> loadOrdersTable());
        viewDetailsBtn.addActionListener(e -> showSelectedOrderDetails());
        printOrderBtn.addActionListener(e -> printSelectedOrder());
        exportOrdersBtn.addActionListener(e -> exportOrdersTable());

        // Documento em painel completo (substitui o modal): a aba alterna lista <-> editor.
        DocumentEditorHost orderEditor = new DocumentEditorHost(
                "Nova Encomenda", orderFormContent,
                this::saveOrderFromEditor,
                this::backToOrdersList,
                () -> !draftOrderLines.isEmpty());
        encomendasCards = new CardLayout();
        encomendasHost = new JPanel(encomendasCards);
        encomendasHost.setOpaque(false);
        encomendasHost.add(panel, "list");
        encomendasHost.add(orderEditor, "editor");
        return encomendasHost;
    }

    /** Abre o editor de nova encomenda (painel completo, substitui o modal). */
    private void openOrderEditor() {
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum armazém disponível para a empresa atual.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resetOrderDraft();
        lastCreatedOrder = null;
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "editor");
        }
    }

    private void backToOrdersList() {
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "list");
        }
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    private void saveOrderFromEditor() {
        try {
            issueOrderOrThrow();
            OrderDTO created = lastCreatedOrder;
            String estadoMsg = "PENDING_APPROVAL".equals(created.status())
                    ? "Submetida para aprovação (valor: " + created.totalAmount() + " MT)."
                    : "Estado: " + created.status() + " (valor: " + created.totalAmount() + " MT).";
            JOptionPane.showMessageDialog(this, "Encomenda " + created.orderNumber() + " criada!\n" + estadoMsg,
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadOrdersTable();
            backToOrdersList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Falha ao criar encomenda." : ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createDeliveryGuidesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Guias de Remessa"), BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] columns = {"ID", "Nº Guia", "Data", "Encomenda", "Cliente", "Armazém",
                "Responsável", "Viatura", "Total", "Estado"};
        deliveryGuidesTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        deliveryGuidesTable = new JTable(deliveryGuidesTableModel);
        UIHelper.styleTable(deliveryGuidesTable);
        deliveryGuidesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        deliveryGuidesTable.setFillsViewportHeight(true);
        hideTableColumn(deliveryGuidesTable, 0);
        deliveryGuidesTable.getColumnModel().getColumn(1).setPreferredWidth(105);
        deliveryGuidesTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        deliveryGuidesTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        deliveryGuidesTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        deliveryGuidesTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        deliveryGuidesTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        deliveryGuidesTable.getColumnModel().getColumn(7).setPreferredWidth(90);
        deliveryGuidesTable.getColumnModel().getColumn(8).setPreferredWidth(90);
        deliveryGuidesTable.getColumnModel().getColumn(9).setPreferredWidth(115);

        JTextField search = TableFilter.searchField("Nº guia, encomenda, cliente ou viatura…");
        JComboBox<String> status = TableFilter.combo("Todos os estados",
                "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
        TableFilter.install(deliveryGuidesTable, search, new TableFilter.ColumnFilter(status, 9));
        JPanel filterBar = TableFilter.bar(search, TableFilter.label("Estado:"), status);
        filterBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(deliveryGuidesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(printBtn);
        actions.add(cancelBtn);
        actions.add(rejectBtn);
        actions.add(approveBtn);
        actions.add(refreshBtn);
        card.add(actions, BorderLayout.SOUTH);

        printBtn.addActionListener(e -> printSelectedDeliveryGuide());
        cancelBtn.addActionListener(e -> cancelSelectedDeliveryGuide());
        rejectBtn.addActionListener(e -> rejectSelectedDeliveryGuide());
        approveBtn.addActionListener(e -> approveSelectedDeliveryGuide());
        refreshBtn.addActionListener(e -> loadDeliveryGuidesTable());

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private static void hideTableColumn(JTable table, int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }

    /** Helper de venda ao grosso na encomenda: nº de caixas → Qtd em unidades. Ver {@link #applyInvoiceBoxes()}. */
    private void applyOrderBoxes() {
        if (orderBoxesField == null) return;
        String raw = orderBoxesField.getText().trim();
        if (raw.isEmpty()) return;
        int idx = orderProductCombo.getSelectedIndex();
        if (idx < 0 || idx >= productsList.size()) return;
        int upb = Math.max(1, productsList.get(idx).unitsPerBox());
        try {
            int boxes = Integer.parseInt(raw);
            if (boxes <= 0) return;
            orderQuantityField.setText(String.valueOf(boxes * upb));
        } catch (NumberFormatException ignore) {
            // texto inválido → não altera a Qtd
        }
    }

    private void addDraftOrderLine() {
        if (productsList.isEmpty()) return;

        int selectedProdIdx = orderProductCombo.getSelectedIndex();
        if (selectedProdIdx < 0) return;

        ProductDTO product = productsList.get(selectedProdIdx);

        int qty;
        try {
            qty = Integer.parseInt(orderQuantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser um número inteiro superior a zero.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal discount = BigDecimal.ZERO;
        try {
            discount = new BigDecimal(orderDiscountField.getText().trim());
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "O desconto deve ser um número decimal entre 0 e 100.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Lote é decidido por FEFO no backend — orderBatchField mostra apenas previsão.
        String previewBatch = orderBatchField.getText().trim();
        String batch = null;

        String serial = orderSerialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        BigDecimal taxRate = TaxRates.STANDARD_VAT;

        CreateInvoiceLineRequest lineRequest = new CreateInvoiceLineRequest(
                product.id(),
                qty,
                taxRate,
                discount,
                batch,
                serial
        );
        draftOrderLines.add(lineRequest);

        // Add to GUI table
        BigDecimal subTotal = product.unitPrice().multiply(BigDecimal.valueOf(qty));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discAmt = subTotal.multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            subTotal = subTotal.subtract(discAmt);
        }
        BigDecimal tax = subTotal.multiply(taxRate);
        BigDecimal total = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        String lotSer = "";
        if (!previewBatch.isEmpty() && !"Sem stock".equals(previewBatch)) {
            lotSer += "FEFO: " + previewBatch + " ";
        }
        if (serial != null) lotSer += "S: " + serial;
        if (lotSer.isEmpty()) lotSer = "-";

        orderLinesTableModel.addRow(new Object[]{
                product.name(),
                qty,
                product.unitPrice() + " MT",
                discount + "%",
                lotSer,
                total + " MT"
        });

        // Accumulate totals
        draftOrderSubtotal = draftOrderSubtotal.add(subTotal);
        draftOrderTax = draftOrderTax.add(tax);
        draftOrderTotal = draftOrderTotal.add(total);

        orderTotalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftOrderTotal));

        // Clear details
        orderQuantityField.setText("1");
        orderBoxesField.setText("");
        orderDiscountField.setText("0");
        orderSerialField.setText("");
        refreshOrderFEFOHint();
    }

    /**
     * Pré-visualiza o lote/validade que vai sair (FEFO) no ecrã de faturas, com base no produto e
     * armazém escolhidos. Quando a linha for confirmada, o backend volta a aplicar FEFO em
     * transacção — esta consulta serve só para mostrar a previsão ao utilizador.
     */
    private void refreshInvoiceFEFOHint() {
        renderFEFOHint(productCombo, warehouseCombo, batchField, productsList);
    }

    private void refreshOrderFEFOHint() {
        renderFEFOHint(orderProductCombo, orderWarehouseCombo, orderBatchField, productsList);
    }

    private void renderFEFOHint(JComboBox<String> productBox, JComboBox<String> warehouseBox,
                                  JTextField targetField, List<ProductDTO> sourceProducts) {
        if (targetField == null) return;
        int prodIdx = productBox.getSelectedIndex();
        int whIdx = warehouseBox.getSelectedIndex();
        if (prodIdx < 0 || whIdx < 0
                || sourceProducts == null || prodIdx >= sourceProducts.size()
                || warehousesList.isEmpty() || whIdx >= warehousesList.size()) {
            targetField.setText("");
            return;
        }
        ProductDTO product = sourceProducts.get(prodIdx);
        WarehouseDTO warehouse = warehousesList.get(whIdx);
        try {
            inventoryApiClient.findNextFEFO(product.id(), warehouse.id()).ifPresentOrElse(
                    b -> {
                        String lote = b.batchNumber() == null ? "—" : b.batchNumber();
                        String val = b.expirationDate() == null
                                ? "—"
                                : b.expirationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        targetField.setText(lote + "  •  " + val);
                    },
                    () -> targetField.setText("Sem stock"));
        } catch (Exception ex) {
            targetField.setText("");
        }
    }

    /** Validação + emissão da encomenda. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private void issueOrderOrThrow() {
        if (warehousesList.isEmpty()) {
            throw new RuntimeException("Nenhum armazém disponível para a empresa atual.");
        }
        if (draftOrderLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à encomenda.");
        }
        int clientIdx = orderClientCombo.getSelectedIndex();
        int whIdx = orderWarehouseCombo.getSelectedIndex();
        if (whIdx < 0) {
            throw new RuntimeException("Selecione o armazém.");
        }

        // O índice 0 do combo é "Consumidor Final"; índices >0 mapeiam para clientsList[idx-1].
        Long clientId = null;
        String walkInName = null;
        if (clientIdx > 0 && (clientIdx - 1) < clientsList.size()) {
            clientId = clientsList.get(clientIdx - 1).id();
        } else {
            String typed = orderClientWalkInField == null ? "" : orderClientWalkInField.getText().trim();
            if (!typed.isEmpty()) walkInName = typed;
        }

        WarehouseDTO warehouse = warehousesList.get(whIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        com.phcpro.modules.comercial.dto.CreateOrderRequest request =
                new com.phcpro.modules.comercial.dto.CreateOrderRequest(
                        clientId, walkInName, companyId, warehouse.id(), draftOrderLines);
        lastCreatedOrder = comercialApiClient.createOrder(request);
    }

    /** Limpa o rascunho da encomenda (linhas, totais e selecção) antes de abrir o modal. */
    private void resetOrderDraft() {
        draftOrderLines.clear();
        draftOrderSubtotal = BigDecimal.ZERO;
        draftOrderTax = BigDecimal.ZERO;
        draftOrderTotal = BigDecimal.ZERO;
        if (orderLinesTableModel != null) orderLinesTableModel.setRowCount(0);
        if (orderTotalLabel != null) orderTotalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
        if (orderClientWalkInField != null) orderClientWalkInField.setText("");
        if (orderClientCombo != null && orderClientCombo.getItemCount() > 0) orderClientCombo.setSelectedIndex(0);
    }

    private void billSelectedOrder() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela para faturar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        String orderNum = (String) ordersTableModel.getValueAt(row, 1);
        String statusStr = (String) ordersTableModel.getValueAt(row, 3);

        if (!"PENDING".equalsIgnoreCase(statusStr)) {
            JOptionPane.showMessageDialog(this, "Apenas encomendas no estado PENDING podem ser faturadas. Estado atual: " + statusStr, "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            InvoiceDTO invoice = comercialApiClient.billOrder(orderId);
            JOptionPane.showMessageDialog(this, "Encomenda " + orderNum + " faturada com sucesso!\n" +
                    "Fatura " + invoice.invoiceNumber() + " gerada com o mesmo número de sequência.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            loadOrdersTable();
            loadInvoicesTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao faturar encomenda: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void convertSelectedOrderToGuide() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela para converter em guia.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        String status = String.valueOf(ordersTableModel.getValueAt(row, 3));
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Apenas encomendas aprovadas no estado PENDING podem ser convertidas em guia. Estado atual: " + status,
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        OrderDTO order;
        try {
            order = comercialApiClient.getOrderById(orderId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField orderField = new JTextField(order.orderNumber());
        JTextField clientField = new JTextField(order.clientName());
        JTextField totalField = new JTextField(String.format("%,.2f MT", order.totalAmount()));
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField field : List.of(orderField, clientField, totalField, responsibleField, vehicleField)) {
            UIHelper.styleTextField(field);
        }
        orderField.setEditable(false);
        clientField.setEditable(false);
        totalField.setEditable(false);
        responsibleField.putClientProperty("JTextField.placeholderText", "Nome de quem acompanha a expedição");
        vehicleField.putClientProperty("JTextField.placeholderText", "Matrícula ou identificação da viatura");
        UIHelper.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        UIHelper.styleScrollPane(notesScroll);

        JPanel form = UIHelper.createDialogForm(
                "Encomenda:", orderField,
                "Cliente:", clientField,
                "Total:", totalField,
                "Responsável pelo transporte:", responsibleField,
                "Viatura / Matrícula:", vehicleField,
                "Observações:", notesScroll
        );

        DeliveryGuideDTO[] created = new DeliveryGuideDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(this),
                "Converter em Guia", "fas-truck",
                "Dados de transporte da Guia de Remessa", form)
                .setConfirmButton("Criar Guia", "fas-truck")
                .setOnSave(() -> created[0] = comercialApiClient.createDeliveryGuide(
                        order.id(), blankToNull(responsibleField.getText()), blankToNull(vehicleField.getText()),
                        blankToNull(notesArea.getText())));

        if (dialog.showDialog() && created[0] != null) {
            JOptionPane.showMessageDialog(this,
                    "Guia " + created[0].guideNumber() + " criada e submetida para aprovação.\n"
                            + "O stock só será movimentado quando a guia for aprovada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadOrdersTable();
            loadDeliveryGuidesTable();
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void loadDeliveryGuidesTable() {
        if (deliveryGuidesTableModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        // Carrega fora do EDT (cursor de espera) e preenche ao chegar — não congela a UI.
        UIHelper.loadAsync(this,
                () -> comercialApiClient.getDeliveryGuidesByCompany(companyId),
                guides -> {
                    deliveryGuidesTableModel.setRowCount(0);
                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    for (DeliveryGuideDTO guide : guides) {
                        deliveryGuidesTableModel.addRow(new Object[]{
                                guide.id(),
                                guide.guideNumber(),
                                guide.guideDate() == null ? "—" : guide.guideDate().format(dateFormat),
                                guide.orderNumber(),
                                guide.clientName(),
                                guide.warehouseName(),
                                guide.responsible() == null || guide.responsible().isBlank() ? "—" : guide.responsible(),
                                guide.vehicle() == null || guide.vehicle().isBlank() ? "—" : guide.vehicle(),
                                String.format("%,.2f MT", guide.totalAmount()),
                                guide.status()
                        });
                    }
                });
    }

    private void approveSelectedDeliveryGuide() {
        int row = selectedDeliveryGuideRow("aprovar");
        if (row < 0) return;
        if (!ensurePendingDeliveryGuide(row)) return;

        String number = String.valueOf(deliveryGuidesTableModel.getValueAt(row, 1));
        int confirm = JOptionPane.showConfirmDialog(this,
                "A aprovação da guia " + number + " dará saída definitiva ao stock.\n\nPretende continuar?",
                "Confirmar Aprovação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Long id = (Long) deliveryGuidesTableModel.getValueAt(row, 0);
        try {
            comercialApiClient.approveDeliveryGuide(id);
            JOptionPane.showMessageDialog(this, "Guia " + number + " aprovada. O stock foi atualizado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadDeliveryGuidesTable();
            loadOrdersTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedDeliveryGuide() {
        int row = selectedDeliveryGuideRow("rejeitar");
        if (row < 0) return;
        if (!ensurePendingDeliveryGuide(row)) return;

        String number = String.valueOf(deliveryGuidesTableModel.getValueAt(row, 1));
        String reason = UIHelper.promptRequiredText("Rejeitar Guia de Remessa", "fas-times",
                "Guia " + number, "Motivo da rejeição:");
        if (reason == null) return;

        Long id = (Long) deliveryGuidesTableModel.getValueAt(row, 0);
        try {
            comercialApiClient.rejectDeliveryGuide(id, reason);
            JOptionPane.showMessageDialog(this,
                    "Guia " + number + " rejeitada. A encomenda voltou a ficar disponível.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadDeliveryGuidesTable();
            loadOrdersTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedDeliveryGuide() {
        int row = selectedDeliveryGuideRow("cancelar");
        if (row < 0) return;
        if (!ensurePendingDeliveryGuide(row)) return;

        String number = String.valueOf(deliveryGuidesTableModel.getValueAt(row, 1));
        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancelar a guia " + number + "?\nA encomenda voltará a ficar disponível para faturação ou nova guia.",
                "Confirmar Cancelamento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Long id = (Long) deliveryGuidesTableModel.getValueAt(row, 0);
        try {
            comercialApiClient.cancelDeliveryGuide(id);
            JOptionPane.showMessageDialog(this, "Guia " + number + " cancelada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadDeliveryGuidesTable();
            loadOrdersTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedDeliveryGuide() {
        int row = selectedDeliveryGuideRow("imprimir");
        if (row < 0) return;
        Long id = (Long) deliveryGuidesTableModel.getValueAt(row, 0);
        String number = String.valueOf(deliveryGuidesTableModel.getValueAt(row, 1));
        try {
            byte[] pdf = comercialApiClient.renderDeliveryGuide(id);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "guia-remessa-" + number);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar Guia de Remessa: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int selectedDeliveryGuideRow(String action) {
        int row = TableFilter.selectedModelRow(deliveryGuidesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma guia na tabela para " + action + ".",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
        return row;
    }

    private boolean ensurePendingDeliveryGuide(int row) {
        String status = String.valueOf(deliveryGuidesTableModel.getValueAt(row, 9));
        if ("PENDING_APPROVAL".equals(status)) return true;
        JOptionPane.showMessageDialog(this,
                "Esta ação só é permitida para guias pendentes de aprovação. Estado atual: " + status,
                "Erro", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    public void loadOrdersTable() {
        ordersTableModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<OrderDTO> orders = comercialApiClient.getOrdersByCompany(companyId);
        java.time.format.DateTimeFormatter dtfShort =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (OrderDTO order : orders) {
            String clientLabel = order.clientName();
            if (order.walkInName() != null && !order.walkInName().isBlank()) {
                clientLabel += " — " + order.walkInName();
            }
            // Coluna combinada: "—" se nunca; "N × dd/MM/yyyy" se impressa.
            // Operador + hora completos ficam no diálogo "Ver Detalhes".
            String impressoes;
            if (order.printCount() <= 0) {
                impressoes = "—";
            } else if (order.printedAt() != null) {
                impressoes = order.printCount() + " × " + order.printedAt().format(dtfShort);
            } else {
                impressoes = String.valueOf(order.printCount());
            }
            ordersTableModel.addRow(new Object[]{
                    order.id(),
                    order.orderNumber(),
                    clientLabel,
                    order.status(),
                    order.totalAmount() + " MT",
                    impressoes
            });
        }
    }

    /**
     * Abre diálogo modal com cabeçalho da encomenda, linhas, e estado de impressão
     * (cópias já feitas + data da última). Permite imprimir a partir do diálogo, com
     * confirmação obrigatória se já foi impressa antes.
     */
    private void showSelectedOrderDetails() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        OrderDTO order;
        try {
            order = comercialApiClient.getOrderById(orderId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Cabeçalho
        StringBuilder header = new StringBuilder("<html><body style='font-family:sans-serif;'>")
                .append("<b>Nº Encomenda:</b> ").append(order.orderNumber()).append("<br>")
                .append("<b>Cliente:</b> ").append(order.clientName());
        if (order.walkInName() != null && !order.walkInName().isBlank()) {
            header.append(" <i>(comprador: ").append(order.walkInName()).append(")</i>");
        }
        header.append("<br><b>Data:</b> ")
                .append(order.createdAt() != null ? order.createdAt().format(dtf) : "—")
                .append("<br><b>Estado:</b> ").append(order.status())
                .append("<br><b>Total:</b> ").append(order.totalAmount()).append(" MT</body></html>");
        JLabel headerLabel = new JLabel(header.toString());

        // Tabela de linhas
        String[] cols = {"Produto", "Lote", "Qtd", "Preço Unit.", "Desc %", "IVA", "Total"};
        DefaultTableModel lm = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (var l : order.lines()) {
            lm.addRow(new Object[]{
                    l.productName(),
                    l.batchNumber() == null ? "—" : l.batchNumber(),
                    l.quantity(),
                    l.unitPrice() + " MT",
                    l.discountPercentage() == null ? "0" : l.discountPercentage().toPlainString(),
                    l.taxRate() == null ? "—" : l.taxRate().toPlainString(),
                    l.lineTotal() + " MT"
            });
        }
        JTable linesTable = new JTable(lm);
        UIHelper.styleTable(linesTable);
        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(660, 200));

        // Bloco de impressão
        JLabel printStatus;
        if (order.printCount() > 0) {
            String msg = String.format(
                    "<html><body style='color:#d97706;font-weight:bold;'>" +
                    "⚠ Já impressa %d vez(es). Última: %s%s</body></html>",
                    order.printCount(),
                    order.printedAt() != null ? order.printedAt().format(dtf) : "—",
                    order.lastPrintedBy() != null ? " por " + order.lastPrintedBy() : "");
            printStatus = new JLabel(msg);
        } else {
            printStatus = new JLabel("<html><body style='color:#16a34a;'>Ainda não foi impressa.</body></html>");
        }

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(headerLabel, BorderLayout.NORTH);
        content.add(linesScroll, BorderLayout.CENTER);
        content.add(printStatus, BorderLayout.SOUTH);

        String[] options = {"Imprimir", "Fechar"};
        int choice = JOptionPane.showOptionDialog(this, content,
                "Detalhes da Encomenda " + order.orderNumber(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if (choice == 0) {
            printOrderWithConfirmation(order);
            loadOrdersTable();
        }
    }

    /**
     * Imprime a encomenda; se já tiver sido impressa antes, pede confirmação explícita
     * (anti-duplicação). Após imprimir, regista a cópia via {@code markOrderPrinted}.
     */
    private void printOrderWithConfirmation(OrderDTO order) {
        if (order.printCount() > 0) {
            String last = order.printedAt() != null
                    ? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(
                            java.util.Date.from(order.printedAt().atZone(
                                    java.time.ZoneId.systemDefault()).toInstant()))
                    : "—";
            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("Esta encomenda já foi impressa %d vez(es) (última em %s%s).%n%n"
                                    + "Tem a certeza que pretende imprimir novamente?",
                            order.printCount(), last,
                            order.lastPrintedBy() != null ? " por " + order.lastPrintedBy() : ""),
                    "Confirmar reimpressão",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        try {
            byte[] pdf = comercialApiClient.renderOrder(order.id());
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "encomenda-" + order.orderNumber());
            comercialApiClient.markOrderPrinted(order.id(),
                    com.phcpro.architecture.security.CurrentUserContext.getUsername());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Diálogo "Faturar Encomenda": mostra encomendas PENDENTES (não faturadas) e, ao confirmar,
     * delega para {@code ComercialService.billOrder(...)} — que valida atomicamente o estado e
     * impede dupla faturação.
     */
    /** Rótulo pequeno/esbatido para campos de resumo em diálogos. */
    private static JLabel dialogMutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    /** Valor destacado para campos de resumo em diálogos. */
    private static JLabel dialogValueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        l.setForeground(UIHelper.TEXT_LIGHT);
        return l;
    }

    private void openBillFromOrderDialog() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        // Lista mutável: a pesquisa substitui o conteúdo, mantendo a referência final para os lambdas.
        java.util.List<OrderDTO> pending = new ArrayList<>(comercialApiClient.getPendingOrdersByCompany(companyId));
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Não há encomendas pendentes para faturar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Pesquisa ao estilo PHC: filtra encomendas pendentes por nº ou cliente.
        JTextField searchField = new JTextField();
        UIHelper.styleTextField(searchField);

        JComboBox<String> orderCombo = new JComboBox<>();
        UIHelper.styleComboBox(orderCombo);

        // Preview das linhas da encomenda seleccionada
        String[] cols = {"Produto", "Lote", "Qtd", "Preço Unit.", "Total"};
        DefaultTableModel preview = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable previewTable = new JTable(preview);
        UIHelper.styleTable(previewTable);
        JScrollPane previewScroll = new JScrollPane(previewTable);
        previewScroll.setPreferredSize(new Dimension(560, 180));

        JLabel orderNumVal = dialogValueLabel("—");
        JLabel clientVal = dialogValueLabel("—");
        JLabel dateVal = dialogValueLabel("—");
        JLabel totalVal = new JLabel("0,00 MT");
        totalVal.setFont(new Font(UIHelper.FONT, Font.BOLD, 24));
        totalVal.setForeground(UIHelper.APPROVED_GREEN);
        Runnable refresh = () -> {
            preview.setRowCount(0);
            int idx = orderCombo.getSelectedIndex();
            if (idx < 0 || idx >= pending.size()) {
                orderNumVal.setText("—"); clientVal.setText("—"); dateVal.setText("—");
                totalVal.setText("0,00 MT");
                return;
            }
            OrderDTO o = pending.get(idx);
            String walk = (o.walkInName() != null && !o.walkInName().isBlank())
                    ? "  (" + o.walkInName() + ")" : "";
            orderNumVal.setText(o.orderNumber());
            clientVal.setText(o.clientName() + walk);
            dateVal.setText(o.createdAt() != null ? o.createdAt().format(dtf) : "—");
            totalVal.setText(String.format("%,.2f MT", o.totalAmount()));
            for (var l : o.lines()) {
                preview.addRow(new Object[]{
                        l.productName(),
                        l.batchNumber() == null ? "—" : l.batchNumber(),
                        l.quantity(),
                        String.format("%,.2f MT", l.unitPrice()),
                        String.format("%,.2f MT", l.lineTotal())
                });
            }
        };
        orderCombo.addActionListener(e -> refresh.run());

        Runnable rebuildCombo = () -> {
            orderCombo.removeAllItems();
            for (OrderDTO o : pending) {
                String walk = (o.walkInName() != null && !o.walkInName().isBlank())
                        ? " (" + o.walkInName() + ")" : "";
                orderCombo.addItem(String.format("%s — %s%s — %s MT",
                        o.orderNumber(), o.clientName(), walk, o.totalAmount()));
            }
            if (orderCombo.getItemCount() > 0) orderCombo.setSelectedIndex(0);
            refresh.run();
        };
        UIHelper.onTextChange(searchField, () -> {
            pending.clear();
            pending.addAll(comercialApiClient.searchPendingOrders(searchField.getText()));
            rebuildCombo.run();
        });
        rebuildCombo.run();

        // Selector (pesquisa + combo) com o estilo do projecto.
        JPanel selectorForm = UIHelper.createDialogForm(
                "Pesquisar (nº ou cliente):", searchField,
                "Encomenda a faturar:", orderCombo
        );

        // Card de resumo: dados da encomenda à esquerda, TOTAL destacado à direita.
        ModernPanel summary = new ModernPanel(14);
        summary.setLayout(new BorderLayout(16, 0));
        summary.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel info = new JPanel(new GridBagLayout());
        info.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(3, 0, 3, 16);
        g.gridx = 0; g.gridy = 0; info.add(dialogMutedLabel("Encomenda"), g);
        g.gridx = 1; info.add(orderNumVal, g);
        g.gridx = 0; g.gridy = 1; info.add(dialogMutedLabel("Cliente"), g);
        g.gridx = 1; info.add(clientVal, g);
        g.gridx = 0; g.gridy = 2; info.add(dialogMutedLabel("Data"), g);
        g.gridx = 1; info.add(dateVal, g);

        JPanel totalBox = new JPanel();
        totalBox.setOpaque(false);
        totalBox.setLayout(new BoxLayout(totalBox, BoxLayout.Y_AXIS));
        JLabel totalCap = dialogMutedLabel("TOTAL A FATURAR");
        totalCap.setAlignmentX(Component.RIGHT_ALIGNMENT);
        totalVal.setAlignmentX(Component.RIGHT_ALIGNMENT);
        totalBox.add(totalCap);
        totalBox.add(totalVal);
        summary.add(info, BorderLayout.CENTER);
        summary.add(totalBox, BorderLayout.EAST);

        // Card dos itens.
        ModernPanel linesCard = new ModernPanel(14);
        linesCard.setLayout(new BorderLayout(0, 8));
        linesCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        linesCard.add(UIHelper.createSubheading("Itens da encomenda"), BorderLayout.NORTH);
        linesCard.add(previewScroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(summary, BorderLayout.NORTH);
        center.add(linesCard, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(UIHelper.BG_DARK);
        content.setBorder(new EmptyBorder(6, 8, 6, 8));
        content.add(selectorForm, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.setPreferredSize(new Dimension(640, 480));

        String[] options = {"Faturar Encomenda", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(this, content,
                "Faturar Encomenda",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice != 0) return;

        int idx = orderCombo.getSelectedIndex();
        if (idx < 0) return;
        OrderDTO chosen = pending.get(idx);

        try {
            InvoiceDTO invoice = comercialApiClient.billOrder(chosen.id());
            JOptionPane.showMessageDialog(this,
                    "Fatura " + invoice.invoiceNumber() + " emitida a partir da encomenda "
                            + chosen.orderNumber() + ".",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadInvoicesTable();
            loadOrdersTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Diálogo "Cancelar Encomenda" (estilo PHC): pesquisa por nº ou cliente, selecciona a encomenda
     * cancelável (não faturada) e exige motivo. Delega para {@code ComercialService.cancelOrder(...)},
     * que valida permissão/estado e fecha o pedido de aprovação aberto.
     */
    private void openCancelOrderDialog() {
        java.util.List<OrderDTO> cancellable = new ArrayList<>(comercialApiClient.searchCancellableOrders(""));
        if (cancellable.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Não há encomendas canceláveis. Apenas encomendas ainda não faturadas podem ser canceladas.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField searchField = new JTextField();
        UIHelper.styleTextField(searchField);
        JComboBox<String> orderCombo = new JComboBox<>();
        UIHelper.styleComboBox(orderCombo);
        JTextField reasonField = new JTextField();
        UIHelper.styleTextField(reasonField);

        Runnable rebuildCombo = () -> {
            orderCombo.removeAllItems();
            for (OrderDTO o : cancellable) {
                String estado = "PENDING_APPROVAL".equals(o.status()) ? "por aprovar" : "aprovada";
                orderCombo.addItem(String.format("%s — %s — %s MT (%s)",
                        o.orderNumber(), o.clientName(), o.totalAmount(), estado));
            }
            if (orderCombo.getItemCount() > 0) orderCombo.setSelectedIndex(0);
        };
        UIHelper.onTextChange(searchField, () -> {
            cancellable.clear();
            cancellable.addAll(comercialApiClient.searchCancellableOrders(searchField.getText()));
            rebuildCombo.run();
        });
        rebuildCombo.run();

        JPanel form = UIHelper.createDialogForm(
                "Pesquisar (nº ou cliente):", searchField,
                "Encomenda a cancelar:", orderCombo,
                "Motivo do cancelamento:", reasonField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Cancelar Encomenda", "fas-ban", "Anular uma encomenda pendente", form).setConfirmButton("Confirmar", "fas-check").showDialog();
        if (!confirmed) return;

        int idx = orderCombo.getSelectedIndex();
        if (idx < 0 || idx >= cancellable.size()) return;
        OrderDTO chosen = cancellable.get(idx);
        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique o motivo do cancelamento.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            comercialApiClient.cancelOrder(chosen.id(), reason);
            JOptionPane.showMessageDialog(this,
                    "Encomenda " + chosen.orderNumber() + " cancelada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadOrdersTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        try {
            byte[] pdf = comercialApiClient.renderInvoice(invoiceId);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "fatura-" + invoiceNum);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedGuide() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        try {
            byte[] pdf = comercialApiClient.renderGuide(invoiceId);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "guia-remessa-" + invoiceNum);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar Guia: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportInvoicesTable() {
        try {
            com.phcpro.modules.company.model.Company company = currentCompany();
            byte[] pdf = com.phcpro.modules.printing.TablePdfExporter.renderFromSwing(company, "Faturas Emitidas", invoicesTable);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "faturas-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedOrder() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        try {
            OrderDTO order = comercialApiClient.getOrderById(orderId);
            printOrderWithConfirmation(order);
            loadOrdersTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportOrdersTable() {
        try {
            com.phcpro.modules.company.model.Company company = currentCompany();
            byte[] pdf = com.phcpro.modules.printing.TablePdfExporter.renderFromSwing(company, "Encomendas", ordersTable);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "encomendas-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private com.phcpro.modules.company.model.Company currentCompany() {
        // Cliente-fino: o exportador de tabelas só precisa do id da empresa activa (cabeçalho do PDF).
        com.phcpro.modules.company.model.Company company = new com.phcpro.modules.company.model.Company();
        company.setId(CurrentUserContext.getCurrentCompanyId());
        return company;
    }

    // ─── Notas de Crédito / Débito ─────────────────────────────────────────────

    private DefaultTableModel creditNotesModel;
    private JTable creditNotesTable;
    private java.util.List<com.phcpro.modules.comercial.dto.CreditNoteDTO> creditNotesList = new ArrayList<>();

    private DefaultTableModel debitNotesModel;
    private JTable debitNotesTable;
    private java.util.List<com.phcpro.modules.comercial.dto.DebitNoteDTO> debitNotesList = new ArrayList<>();

    private static final String[] CREDIT_REASONS = {"RETURN", "DISCOUNT", "ERROR", "CANCELLATION"};
    private static final String[] DEBIT_REASONS = {"FREIGHT", "SURCHARGE", "CORRECTION", "OTHER"};

    private JPanel createCreditNotesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Notas de Crédito"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Emitir Nota de Crédito");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        newBtn.addActionListener(e -> openCreateCreditNoteDialog());
        approveBtn.addActionListener(e -> approveSelectedCreditNote());
        rejectBtn.addActionListener(e -> rejectSelectedCreditNote());
        printBtn.addActionListener(e -> printSelectedCreditNote());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(printBtn);
        actions.add(rejectBtn);
        actions.add(approveBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº", "Data", "Fatura", "Cliente", "Motivo", "Armazém", "Total", "Estado"};
        creditNotesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        creditNotesTable = new JTable(creditNotesModel);
        UIHelper.styleTable(creditNotesTable);
        JScrollPane scroll = new JScrollPane(creditNotesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField ncSearch = TableFilter.searchField("Nº, fatura ou cliente…");
        JComboBox<String> ncMotivo = TableFilter.combo("Todos os motivos",
                "RETURN", "DISCOUNT", "ERROR", "CANCELLATION");
        JComboBox<String> ncEstado = TableFilter.combo("Todos os estados",
                "DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
        JComboBox<String> ncPeriodo = TableFilter.periodCombo();
        TableFilter.install(creditNotesTable, ncSearch,
                java.util.List.of(new TableFilter.ColumnFilter(ncMotivo, 4),
                        new TableFilter.ColumnFilter(ncEstado, 7)),
                java.util.List.of(new TableFilter.PeriodFilter(ncPeriodo, 1)));
        JPanel ncBar = TableFilter.bar(ncSearch,
                TableFilter.label("Motivo:"), ncMotivo,
                TableFilter.label("Estado:"), ncEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), ncPeriodo);
        ncBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(ncBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel createDebitNotesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Notas de Débito"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createPrimaryButton("Emitir Nota de Débito");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        newBtn.addActionListener(e -> openCreateDebitNoteDialog());
        approveBtn.addActionListener(e -> approveSelectedDebitNote());
        rejectBtn.addActionListener(e -> rejectSelectedDebitNote());
        printBtn.addActionListener(e -> printSelectedDebitNote());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(printBtn);
        actions.add(rejectBtn);
        actions.add(approveBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº", "Data", "Fatura", "Cliente", "Motivo", "Total", "Estado"};
        debitNotesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        debitNotesTable = new JTable(debitNotesModel);
        UIHelper.styleTable(debitNotesTable);
        JScrollPane scroll = new JScrollPane(debitNotesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField ndSearch = TableFilter.searchField("Nº, fatura ou cliente…");
        JComboBox<String> ndMotivo = TableFilter.combo("Todos os motivos",
                "FREIGHT", "SURCHARGE", "CORRECTION", "OTHER");
        JComboBox<String> ndEstado = TableFilter.combo("Todos os estados",
                "DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
        JComboBox<String> ndPeriodo = TableFilter.periodCombo();
        TableFilter.install(debitNotesTable, ndSearch,
                java.util.List.of(new TableFilter.ColumnFilter(ndMotivo, 4),
                        new TableFilter.ColumnFilter(ndEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(ndPeriodo, 1)));
        JPanel ndBar = TableFilter.bar(ndSearch,
                TableFilter.label("Motivo:"), ndMotivo,
                TableFilter.label("Estado:"), ndEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), ndPeriodo);
        ndBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(ndBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadCreditNotesTable() {
        if (creditNotesModel == null) return;
        creditNotesModel.setRowCount(0);
        creditNotesList = creditNoteApiClient.findByCompany(
                com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId());
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (var n : creditNotesList) {
            creditNotesModel.addRow(new Object[]{
                    n.noteNumber(),
                    n.issueDate().format(dtf),
                    n.invoiceNumber(),
                    n.clientName(),
                    n.reason(),
                    n.warehouseName() == null ? "-" : n.warehouseName(),
                    String.format("%,.2f MT", n.totalAmount()),
                    n.status()
            });
        }
    }

    private void loadDebitNotesTable() {
        if (debitNotesModel == null) return;
        debitNotesModel.setRowCount(0);
        debitNotesList = debitNoteApiClient.findByCompany(
                com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId());
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (var n : debitNotesList) {
            debitNotesModel.addRow(new Object[]{
                    n.noteNumber(),
                    n.issueDate().format(dtf),
                    n.invoiceNumber(),
                    n.clientName(),
                    n.reason(),
                    String.format("%,.2f MT", n.totalAmount()),
                    n.status()
            });
        }
    }

    private com.phcpro.modules.comercial.dto.CreditNoteDTO selectedCreditNote() {
        int row = TableFilter.selectedModelRow(creditNotesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma nota na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return creditNotesList.get(row);
    }

    private com.phcpro.modules.comercial.dto.DebitNoteDTO selectedDebitNote() {
        int row = TableFilter.selectedModelRow(debitNotesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma nota na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return debitNotesList.get(row);
    }

    private void approveSelectedCreditNote() {
        var sel = selectedCreditNote();
        if (sel == null) return;
        try {
            var approved = creditNoteApiClient.approve(sel.id());
            String msg = "Nota " + approved.noteNumber() + " aprovada.";
            if ("RETURN".equals(approved.reason())) {
                msg += "\nStock foi devolvido ao armazém " + approved.warehouseName() + ".";
            }
            JOptionPane.showMessageDialog(this, msg, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadCreditNotesTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedCreditNote() {
        var sel = selectedCreditNote();
        if (sel == null) return;
        String reason = UIHelper.promptRequiredText("Rejeitar Nota de Crédito", "fas-times-circle",
                "Indique o motivo da rejeição", "Motivo da rejeição:");
        if (reason == null) return;
        try {
            creditNoteApiClient.reject(sel.id(), reason);
            loadCreditNotesTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedCreditNote() {
        var sel = selectedCreditNote();
        if (sel == null) return;
        try {
            byte[] pdf = creditNoteApiClient.renderCreditNote(sel.id());
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "nota-credito-" + sel.noteNumber());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void approveSelectedDebitNote() {
        var sel = selectedDebitNote();
        if (sel == null) return;
        try {
            debitNoteApiClient.approve(sel.id());
            JOptionPane.showMessageDialog(this, "Nota " + sel.noteNumber() + " aprovada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadDebitNotesTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedDebitNote() {
        var sel = selectedDebitNote();
        if (sel == null) return;
        String reason = UIHelper.promptRequiredText("Rejeitar Nota de Débito", "fas-times-circle",
                "Indique o motivo da rejeição", "Motivo da rejeição:");
        if (reason == null) return;
        try {
            debitNoteApiClient.reject(sel.id(), reason);
            loadDebitNotesTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedDebitNote() {
        var sel = selectedDebitNote();
        if (sel == null) return;
        try {
            byte[] pdf = debitNoteApiClient.renderDebitNote(sel.id());
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "nota-debito-" + sel.noteNumber());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCreateCreditNoteDialog() {
        // Lista mutável: a pesquisa substitui o conteúdo (estilo PHC — localizar a fatura de origem).
        java.util.List<InvoiceDTO> invoices = new ArrayList<>(comercialApiClient.getInvoicesByCompany(
                com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId()));
        if (invoices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Precisa de pelo menos uma fatura cadastrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField invoiceSearch = new JTextField();
        UIHelper.styleTextField(invoiceSearch);
        JComboBox<String> invoiceCombo = new JComboBox<>();
        UIHelper.styleComboBox(invoiceCombo);

        JComboBox<String> reasonCombo = new JComboBox<>(CREDIT_REASONS);
        UIHelper.styleComboBox(reasonCombo);

        JComboBox<String> warehouseCombo = new JComboBox<>();
        for (var w : warehousesList) warehouseCombo.addItem(w.name());
        UIHelper.styleComboBox(warehouseCombo);

        JTextField descField = new JTextField();
        UIHelper.styleTextField(descField);

        // Tabela só com colunas derivadas da fatura. Operador só edita coluna 5 (Qty a Devolver).
        // Coluna 0 (oculta) guarda invoiceLineId.
        String[] lineCols = {"#linhaId", "Produto", "Lote", "Qty Vendida", "Já Devolvida", "Restante", "Qty a Devolver"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);
        // Esconder a coluna do ID
        linesTable.getColumnModel().getColumn(0).setMinWidth(0);
        linesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        linesTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(680, 200));

        // Helper para popular a tabela com as linhas da fatura escolhida.
        Runnable populateLines = () -> {
            linesModel.setRowCount(0);
            int idx = invoiceCombo.getSelectedIndex();
            if (idx < 0) return;
            var invoice = invoices.get(idx);
            java.util.Map<Long, BigDecimal> alreadyReturned;
            try {
                alreadyReturned = creditNoteApiClient.getReturnedQuantitiesByInvoiceLine(invoice.id());
            } catch (Exception ex) {
                alreadyReturned = java.util.Collections.emptyMap();
            }
            for (var il : invoice.lines()) {
                BigDecimal sold = il.quantity();
                BigDecimal returned = alreadyReturned.getOrDefault(il.id(), BigDecimal.ZERO);
                BigDecimal remaining = sold.subtract(returned);
                linesModel.addRow(new Object[]{
                        il.id(),
                        il.productName(),
                        il.batchNumber() == null ? "—" : il.batchNumber(),
                        sold.toPlainString(),
                        returned.toPlainString(),
                        remaining.toPlainString(),
                        "0"
                });
            }
        };
        invoiceCombo.addActionListener(e -> populateLines.run());

        Runnable rebuildInvoiceCombo = () -> {
            invoiceCombo.removeAllItems();
            for (var i : invoices) invoiceCombo.addItem(i.invoiceNumber() + " — " + i.clientName());
            if (invoiceCombo.getItemCount() > 0) invoiceCombo.setSelectedIndex(0);
            populateLines.run();
        };
        UIHelper.onTextChange(invoiceSearch, () -> {
            invoices.clear();
            invoices.addAll(comercialApiClient.searchInvoices(invoiceSearch.getText()));
            rebuildInvoiceCombo.run();
        });
        rebuildInvoiceCombo.run();

        JPanel form = UIHelper.createDialogForm(
                "Pesquisar (nº ou cliente):", invoiceSearch,
                "Fatura:", invoiceCombo,
                "Motivo:", reasonCombo,
                "Armazém (devolução):", warehouseCombo,
                "Descrição:", descField
        );

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.setOpaque(false);
        dialogPanel.add(form, BorderLayout.NORTH);
        JPanel linesWrap = new JPanel(new BorderLayout(0, 6));
        linesWrap.setOpaque(false);
        linesWrap.add(new JLabel("Linhas da Fatura — indique 'Qty a Devolver' nas linhas que aplica:"), BorderLayout.NORTH);
        linesWrap.add(linesScroll, BorderLayout.CENTER);
        dialogPanel.add(linesWrap, BorderLayout.CENTER);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Emitir Nota de Crédito", "fas-file-invoice-dollar", "Crédito sobre fatura (devolução/correção)", dialogPanel).setConfirmButton("Emitir", "fas-check").showDialog();
        if (!confirmed) return;

        if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();

        java.util.List<com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                Long invoiceLineId = (Long) linesModel.getValueAt(i, 0);
                String qtyStr = String.valueOf(linesModel.getValueAt(i, 6)).trim();
                if (qtyStr.isEmpty()) continue;
                BigDecimal qty = new BigDecimal(qtyStr);
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue; // ignora linhas sem devolução
                lines.add(new com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest(invoiceLineId, qty));
            }
            if (lines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Indique uma quantidade a devolver em pelo menos uma linha.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            var req = new com.phcpro.modules.comercial.dto.CreateCreditNoteRequest(
                    invoices.get(invoiceCombo.getSelectedIndex()).id(),
                    (String) reasonCombo.getSelectedItem(),
                    warehousesList.isEmpty() ? null : warehousesList.get(warehouseCombo.getSelectedIndex()).id(),
                    descField.getText().trim().isEmpty() ? null : descField.getText().trim(),
                    lines
            );
            var created = creditNoteApiClient.create(req);
            JOptionPane.showMessageDialog(this, "Nota " + created.noteNumber() + " emitida (pendente de aprovação).", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadCreditNotesTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida em alguma linha.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCreateDebitNoteDialog() {
        // Lista mutável: a pesquisa substitui o conteúdo (estilo PHC — localizar a fatura de origem).
        java.util.List<InvoiceDTO> invoices = new ArrayList<>(comercialApiClient.getInvoicesByCompany(
                com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId()));
        if (invoices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Precisa de pelo menos uma fatura cadastrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField invoiceSearch = new JTextField();
        UIHelper.styleTextField(invoiceSearch);
        JComboBox<String> invoiceCombo = new JComboBox<>();
        UIHelper.styleComboBox(invoiceCombo);
        Runnable rebuildInvoiceCombo = () -> {
            invoiceCombo.removeAllItems();
            for (var i : invoices) invoiceCombo.addItem(i.invoiceNumber() + " — " + i.clientName());
            if (invoiceCombo.getItemCount() > 0) invoiceCombo.setSelectedIndex(0);
        };
        UIHelper.onTextChange(invoiceSearch, () -> {
            invoices.clear();
            invoices.addAll(comercialApiClient.searchInvoices(invoiceSearch.getText()));
            rebuildInvoiceCombo.run();
        });
        rebuildInvoiceCombo.run();

        JComboBox<String> reasonCombo = new JComboBox<>(DEBIT_REASONS);
        UIHelper.styleComboBox(reasonCombo);

        JTextField descField = new JTextField();
        UIHelper.styleTextField(descField);

        String[] lineCols = {"Descrição", "Valor", "IVA (0.16)"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(520, 160));

        ModernButton addLine = UIHelper.createAddLineButton();
        ModernButton removeLine = UIHelper.createDangerButton("- Remover");
        addLine.addActionListener(e -> linesModel.addRow(new Object[]{"", "0", "0"}));
        removeLine.addActionListener(e -> {
            int sel = linesTable.getSelectedRow();
            if (sel >= 0) linesModel.removeRow(sel);
        });
        JPanel lineBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineBtns.setOpaque(false);
        lineBtns.add(addLine);
        lineBtns.add(removeLine);

        linesModel.addRow(new Object[]{"Frete adicional", "0", "0"});

        JPanel form = UIHelper.createDialogForm(
                "Pesquisar (nº ou cliente):", invoiceSearch,
                "Fatura:", invoiceCombo,
                "Motivo:", reasonCombo,
                "Descrição:", descField
        );

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.setOpaque(false);
        dialogPanel.add(form, BorderLayout.NORTH);
        JPanel linesWrap = new JPanel(new BorderLayout(0, 6));
        linesWrap.setOpaque(false);
        linesWrap.add(new JLabel("Linhas da Nota:"), BorderLayout.NORTH);
        linesWrap.add(linesScroll, BorderLayout.CENTER);
        linesWrap.add(lineBtns, BorderLayout.SOUTH);
        dialogPanel.add(linesWrap, BorderLayout.CENTER);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Emitir Nota de Débito", "fas-file-invoice-dollar", "Débito adicional sobre fatura", dialogPanel).setConfirmButton("Emitir", "fas-check").showDialog();
        if (!confirmed) return;

        if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
        if (linesModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Adicione pelo menos uma linha.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.util.List<com.phcpro.modules.comercial.dto.CreateDebitNoteLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                String desc = String.valueOf(linesModel.getValueAt(i, 0)).trim();
                BigDecimal amount = new BigDecimal(String.valueOf(linesModel.getValueAt(i, 1)).trim());
                BigDecimal tax = new BigDecimal(String.valueOf(linesModel.getValueAt(i, 2)).trim());
                if (desc.isEmpty()) throw new IllegalArgumentException("Descrição da linha não pode estar vazia.");
                lines.add(new com.phcpro.modules.comercial.dto.CreateDebitNoteLineRequest(desc, amount, tax));
            }
            var req = new com.phcpro.modules.comercial.dto.CreateDebitNoteRequest(
                    invoices.get(invoiceCombo.getSelectedIndex()).id(),
                    (String) reasonCombo.getSelectedItem(),
                    descField.getText().trim().isEmpty() ? null : descField.getText().trim(),
                    lines
            );
            var created = debitNoteApiClient.create(req);
            JOptionPane.showMessageDialog(this, "Nota " + created.noteNumber() + " emitida (pendente de aprovação).", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadDebitNotesTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Contas Correntes / Fiados ─────────────────────────────────────────

    private DefaultTableModel outstandingModel;
    private JTable outstandingTable;
    private java.util.List<com.phcpro.modules.comercial.dto.InvoiceDTO> outstandingList = new ArrayList<>();

    private JPanel createMovimentosTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Movimentos — Todos os Documentos Comerciais"), BorderLayout.WEST);

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadMovimentosTable());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Tipo", "Nº", "Cliente", "Data", "Estado", "Total"};
        movimentosModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        movimentosTable = new JTable(movimentosModel);
        UIHelper.styleTable(movimentosTable);
        JScrollPane scroll = new JScrollPane(movimentosTable);
        UIHelper.styleScrollPane(scroll);
        movimentosSearch = TableFilter.searchField("Nº documento ou cliente…");
        movimentosPeriod = TableFilter.periodCombo();
        TableFilter.install(movimentosTable, movimentosSearch,
                java.util.List.of(),
                java.util.List.of(new TableFilter.PeriodFilter(movimentosPeriod, 3)));
        // Rodapé (contagem + soma) acompanha o filtro client-side.
        UIHelper.onTextChange(movimentosSearch, this::updateMovimentosFooter);
        movimentosPeriod.addActionListener(e -> updateMovimentosFooter());
        JPanel mvBar = TableFilter.bar(movimentosSearch, TableFilter.label("Data:", "fas-calendar-alt"), movimentosPeriod);
        mvBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(mvBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        movimentosFooter = new JLabel(" ");
        movimentosFooter.setForeground(UIHelper.TEXT_MUTED);
        movimentosFooter.setBorder(new EmptyBorder(8, 4, 0, 4));
        card.add(movimentosFooter, BorderLayout.SOUTH);

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadMovimentosTable() {
        if (movimentosModel == null) return;
        movimentosModel.setRowCount(0);
        movimentosData = movimentosApiClient.listar(
                com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId(), "", null, null);
        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (var m : movimentosData) {
            java.math.BigDecimal total = m.total() == null ? java.math.BigDecimal.ZERO : m.total();
            movimentosModel.addRow(new Object[]{
                    m.tipo().getLabel(),
                    m.numero() == null ? "-" : m.numero(),
                    m.cliente(),
                    m.data() == null ? "-" : m.data().format(dtf),
                    m.estado(),
                    String.format("%,.2f MT", total)
            });
        }
        updateMovimentosFooter();
    }

    /** Rodapé: contagem + soma dos movimentos actualmente visíveis (após pesquisa/período). */
    private void updateMovimentosFooter() {
        if (movimentosFooter == null) return;
        String q = movimentosSearch == null ? "" : movimentosSearch.getText();
        String period = movimentosPeriod == null ? null : String.valueOf(movimentosPeriod.getSelectedItem());
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        int count = 0;
        java.math.BigDecimal soma = java.math.BigDecimal.ZERO;
        for (var m : movimentosData) {
            java.math.BigDecimal total = m.total() == null ? java.math.BigDecimal.ZERO : m.total();
            String dataStr = m.data() == null ? "-" : m.data().format(dtf);
            java.util.List<String> cells = java.util.List.of(
                    m.tipo().getLabel(),
                    m.numero() == null ? "-" : m.numero(),
                    m.cliente() == null ? "" : m.cliente(),
                    dataStr,
                    m.estado() == null ? "" : m.estado(),
                    String.format("%,.2f MT", total));
            if (!TableFilter.rowMatches(cells, q, java.util.Map.of())) continue;
            if (!TableFilter.matchesPeriod(TableFilter.parseCellDate(dataStr), period, today)) continue;
            count++;
            soma = soma.add(total);
        }
        movimentosFooter.setText(String.format("%d documento(s) · Total: %,.2f MT", count, soma));
    }

    private JPanel createOutstandingTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Contas Correntes — Faturas com Saldo em Dívida"), BorderLayout.WEST);

        ModernButton payBtn = UIHelper.createSuccessButton("Receber Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        payBtn.addActionListener(e -> openReceivePaymentDialog());
        refreshBtn.addActionListener(e -> loadOutstandingTable());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(payBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº Fatura", "Data", "Cliente", "NUIT", "Total", "Pago", "Em Dívida", "Estado"};
        outstandingModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        outstandingTable = new JTable(outstandingModel);
        UIHelper.styleTable(outstandingTable);
        JScrollPane scroll = new JScrollPane(outstandingTable);
        UIHelper.styleScrollPane(scroll);

        JTextField ccSearch = TableFilter.searchField("Nº fatura, cliente ou NUIT…");
        JComboBox<String> ccEstado = TableFilter.combo("Todos os estados",
                "APPROVED", "PARTIALLY_PAID");
        JComboBox<String> ccPeriodo = TableFilter.periodCombo();
        TableFilter.install(outstandingTable, ccSearch,
                java.util.List.of(new TableFilter.ColumnFilter(ccEstado, 7)),
                java.util.List.of(new TableFilter.PeriodFilter(ccPeriodo, 1)));
        JPanel ccBar = TableFilter.bar(ccSearch,
                TableFilter.label("Estado:"), ccEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), ccPeriodo);
        ccBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(ccBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadOutstandingTable() {
        if (outstandingModel == null) return;
        outstandingModel.setRowCount(0);
        outstandingList = comercialApiClient.getOutstandingInvoicesByCompany(
                com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId());
        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (var inv : outstandingList) {
            java.math.BigDecimal paid = inv.amountPaid() == null ? java.math.BigDecimal.ZERO : inv.amountPaid();
            java.math.BigDecimal outstanding = inv.totalAmount().subtract(paid);
            outstandingModel.addRow(new Object[]{
                    inv.invoiceNumber(),
                    inv.createdAt() == null ? "-" : inv.createdAt().format(dtf),
                    inv.clientName(),
                    inv.clientTaxId(),
                    String.format("%,.2f MT", inv.totalAmount()),
                    String.format("%,.2f MT", paid),
                    String.format("%,.2f MT", outstanding),
                    inv.status().name()
            });
        }
    }

    private com.phcpro.modules.comercial.dto.InvoiceDTO selectedOutstanding() {
        int row = TableFilter.selectedModelRow(outstandingTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return outstandingList.get(row);
    }

    private void openReceivePaymentDialog() {
        var sel = selectedOutstanding();
        if (sel == null) return;
        java.math.BigDecimal paid = sel.amountPaid() == null ? java.math.BigDecimal.ZERO : sel.amountPaid();
        java.math.BigDecimal outstanding = sel.totalAmount().subtract(paid);

        JComboBox<String> methodCombo = new JComboBox<>(
                new String[]{"CASH", "CARD", "BANK_TRANSFER"});
        UIHelper.styleComboBox(methodCombo);
        JTextField amountField = new JTextField(outstanding.toPlainString());
        JTextField referenceField = new JTextField();
        UIHelper.styleTextField(amountField);
        UIHelper.styleTextField(referenceField);

        var accounts = financeApiClient.getAllAccounts();
        JComboBox<String> accountCombo = new JComboBox<>();
        for (var a : accounts) accountCombo.addItem(a.name());
        UIHelper.styleComboBox(accountCombo);

        JLabel info = new JLabel(String.format(
                "<html><b>Fatura:</b> %s · <b>Cliente:</b> %s<br>"
              + "<b>Total:</b> %,.2f MT &nbsp; <b>Pago:</b> %,.2f MT &nbsp; <b>Em dívida:</b> %,.2f MT</html>",
                sel.invoiceNumber(), sel.clientName(),
                sel.totalAmount(), paid, outstanding));
        info.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Resumo:", info,
                "Método:", methodCombo,
                "Conta de Tesouraria:", accountCombo,
                "Valor a Receber (MT):", amountField,
                "Referência (Nº recibo/transação):", referenceField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Receber Pagamento — " + sel.invoiceNumber(), "fas-money-bill-wave", "Liquidação de fatura em dívida", form)
                .setConfirmButton("Receber", "fas-money-bill-wave").showDialog();
        if (!confirmed) return;

        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(amountField.getText().trim());
            if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
            Long accountId = accounts.isEmpty()
                    ? null
                    : accounts.get(accountCombo.getSelectedIndex()).id();
            com.phcpro.modules.pos.dto.PosPaymentRequest req =
                    new com.phcpro.modules.pos.dto.PosPaymentRequest(
                            (String) methodCombo.getSelectedItem(),
                            amount,
                            amount,  // tendered = amount (sem troco para late payments)
                            referenceField.getText().trim().isEmpty() ? null : referenceField.getText().trim(),
                            accountId);
            posApiClient.registerLatePayment(sel.id(), req);
            JOptionPane.showMessageDialog(this, "Pagamento registado com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadOutstandingTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
