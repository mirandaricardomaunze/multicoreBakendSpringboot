package com.phcpro.gui;

import com.phcpro.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Constrói a vista de listagem/editor de faturação. */
final class CommercialInvoicesView {
    private CommercialInvoicesView() {}

    static JPanel create(ComercialPanel owner) {
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
        billFromOrderBtn.addActionListener(e -> owner.openBillFromOrderDialog());
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
        owner.clientCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.clientCombo);
        formCard.add(owner.clientCombo, gbc);

        gbc.gridx = 1;
        owner.warehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.warehouseCombo);
        formCard.add(owner.warehouseCombo, gbc);

        // Row 2: Product Selection (Full Width)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 8, 2, 8);
        JLabel prodLbl = new JLabel("Produto / Serviço:");
        prodLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(prodLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 8, 8, 8);
        owner.productCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.productCombo);
        formCard.add(owner.productCombo, gbc);

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
        owner.quantityField = new QuantityField("1", true);
        JPanel boxHelper = new JPanel(new BorderLayout(4, 0));
        boxHelper.setOpaque(false);
        JLabel cxLbl = new JLabel("Caixas:");
        cxLbl.setForeground(UIHelper.TEXT_MUTED);
        owner.invoiceBoxesField = new QuantityField("", true);
        owner.invoiceBoxesField.setToolTipText("Venda ao grosso: preenche a Qtd em unidades = caixas × unidades/caixa do produto.");
        boxHelper.add(cxLbl, BorderLayout.WEST);
        boxHelper.add(owner.invoiceBoxesField, BorderLayout.CENTER);
        qtyRow.add(owner.quantityField, BorderLayout.CENTER);
        qtyRow.add(boxHelper, BorderLayout.EAST);
        formCard.add(qtyRow, gbc);

        gbc.gridx = 1;
        owner.discountField = new DecimalField("0", 2, false);
        formCard.add(owner.discountField, gbc);

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
        owner.batchField = new JTextField();
        UIHelper.styleTextField(owner.batchField);
        owner.batchField.setEditable(false);
        owner.batchField.setToolTipText("Lote a sair (FEFO) — calculado a partir do produto e armazém.");
        owner.batchField.putClientProperty("JTextField.placeholderText", "— FEFO automático —");
        formCard.add(owner.batchField, gbc);

        gbc.gridx = 1;
        owner.serialField = new JTextField();
        UIHelper.styleTextField(owner.serialField);
        formCard.add(owner.serialField, gbc);

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
        owner.linesTableModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.linesTable = new JTable(owner.linesTableModel);
        UIHelper.styleTable(owner.linesTable);
        JScrollPane linesScroll = new JScrollPane(owner.linesTable);
        UIHelper.styleEmbeddedTableScrollPane(linesScroll, owner.linesTable, 4);
        // Draft table is placed in its own card below the input form.

        // Row 7: Total summary (a emissão é feita pelo botão Gravar do modal)
        owner.totalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        owner.totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        owner.totalLabel.setForeground(Color.WHITE);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        totalRow.add(owner.totalLabel, BorderLayout.EAST);

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
        owner.invoiceFormContent = formContent;

        // TAB: cabeçalho com acções + lista de faturas em ecrã inteiro.
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        headerBar.add(UIHelper.createHeading("Faturas Recentes"), BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton newInvoiceBtn = UIHelper.createPrimaryButton("Nova Fatura…");
        newInvoiceBtn.setIcon(UIHelper.icon("fas-file-invoice", 14));
        newInvoiceBtn.addActionListener(e -> owner.openInvoiceEditor());
        headerActions.add(billFromOrderBtn);
        headerActions.add(newInvoiceBtn);
        headerBar.add(headerActions, BorderLayout.EAST);
        panel.add(headerBar, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] invoicesCols = {"ID", "Nº Fatura", "Cliente", "Estado", "Total", "Em Dívida"};
        owner.invoicesTableModel = new DefaultTableModel(invoicesCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.invoicesTable = new JTable(owner.invoicesTableModel);
        UIHelper.styleTable(owner.invoicesTable);
        owner.invoicesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        owner.invoicesTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        owner.invoicesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        
        // Hide ID column
        owner.invoicesTable.getColumnModel().getColumn(0).setMinWidth(0);
        owner.invoicesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        owner.invoicesTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane invoicesScroll = new JScrollPane(owner.invoicesTable);
        UIHelper.styleScrollPane(invoicesScroll);
        JTextField invSearch = TableFilter.searchField("Nº fatura ou cliente…");
        JComboBox<String> invEstado = TableFilter.combo("Todos os estados",
                "DRAFT", "PENDING_APPROVAL", "PENDING_DISCOUNT_APPROVAL", "APPROVED",
                "PARTIALLY_PAID", "REJECTED", "PAID", "CANCELLED");
        TableFilter.install(owner.invoicesTable, invSearch, new TableFilter.ColumnFilter(invEstado, 3));
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
        addLineBtn.addActionListener(e -> owner.addDraftLine());
        owner.productCombo.addActionListener(e -> { owner.refreshInvoiceFEFOHint(); owner.applyInvoiceBoxes(); });
        owner.warehouseCombo.addActionListener(e -> owner.refreshInvoiceFEFOHint());
        UIHelper.onTextChange(owner.invoiceBoxesField, owner::applyInvoiceBoxes);
        cancelInvoiceBtn.addActionListener(e -> owner.cancelSelectedInvoice());
        payInvoiceBtn.addActionListener(e -> owner.paySelectedInvoice());
        refreshBtn.addActionListener(e -> owner.loadInvoicesTable());
        printInvoiceBtn.addActionListener(e -> owner.printSelectedInvoice());
        printGuideBtn.addActionListener(e -> owner.printSelectedGuide());
        exportTableBtn.addActionListener(e -> owner.exportInvoicesTable());

        // Documento em painel completo (substitui o modal): a aba alterna lista <-> editor.
        DocumentEditorHost invoiceEditor = new DocumentEditorHost(
                "Nova Fatura", owner.invoiceFormContent,
                owner::saveInvoiceFromEditor,
                owner::backToInvoicesList,
                () -> !owner.draftLines.isEmpty());
        owner.faturacaoCards = new CardLayout();
        owner.faturacaoHost = new JPanel(owner.faturacaoCards);
        owner.faturacaoHost.setOpaque(false);
        owner.faturacaoHost.add(panel, "list");
        owner.faturacaoHost.add(invoiceEditor, "editor");
        return owner.faturacaoHost;
    }

    /** Abre o editor de nova fatura (painel completo, substitui o modal). */
}
