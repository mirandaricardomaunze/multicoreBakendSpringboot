package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.model.OrderKind;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Constrói a vista de listagem/editor de encomendas; o controlador permanece no painel comercial. */
final class CommercialOrdersView {
    private CommercialOrdersView() {}

    /** O que muda ao escolher cada via, dito antes de o operador escolher. */
    private static String kindHint(OrderKind kind) {
        return kind.isThermal()
                ? "Reserva stock e imprime talão para o armazém separar. Não passa por aprovação."
                : "Documento A4 igual à fatura. Passa por aprovação antes de poder ser facturada.";
    }

    static JPanel create(ComercialPanel owner) {
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
        owner.orderClientCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderClientCombo);
        formCard.add(owner.orderClientCombo, gbc);

        gbc.gridx = 1;
        owner.orderWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderWarehouseCombo);
        formCard.add(owner.orderWarehouseCombo, gbc);

        // Row extra: nome livre do comprador (opcional, só relevante se cliente = "Consumidor Final").
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel walkInLbl = new JLabel("Nome do comprador (opcional, se 'Consumidor Final'):");
        walkInLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(walkInLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 8, 12, 8);
        owner.orderClientWalkInField = new JTextField();
        UIHelper.styleTextField(owner.orderClientWalkInField);
        owner.orderClientWalkInField.putClientProperty("JTextField.placeholderText",
                "Escrever nome se a encomenda for para 'Consumidor Final' (deixar vazio caso contrário)");
        formCard.add(owner.orderClientWalkInField, gbc);

        // Row: via da encomenda. É a primeira decisão do documento — decide o formato do papel, se
        // passa por aprovação e se vai ao armazém. Ver docs/ENCOMENDA_DUAS_VIAS_SPEC.md.
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel kindLbl = new JLabel("Tipo de encomenda:");
        kindLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(kindLbl, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(2, 8, 4, 8);
        owner.orderKindCombo = new JComboBox<>(OrderKind.values());
        // Rótulo primeiro, tema depois: styleComboBox envolve o renderer que encontrar.
        owner.orderKindCombo.setRenderer(UIHelper.labelRenderer(OrderKind::label));
        UIHelper.styleComboBox(owner.orderKindCombo);
        owner.orderKindCombo.setSelectedItem(OrderKind.PICKING_REQUEST);
        formCard.add(owner.orderKindCombo, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 8, 12, 8);
        JLabel kindHint = new JLabel();
        kindHint.setForeground(UIHelper.TEXT_MUTED);
        kindHint.setFont(kindHint.getFont().deriveFont(11f));
        formCard.add(kindHint, gbc);
        owner.orderKindCombo.addActionListener(e -> kindHint.setText(kindHint(owner.selectedOrderKind())));
        kindHint.setText(kindHint(OrderKind.PICKING_REQUEST));

        // Row 2: Product Selection (Full Width)
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel prodLbl = new JLabel("Produto / Serviço:");
        prodLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(prodLbl, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(2, 8, 12, 8);
        owner.orderProductCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderProductCombo);
        formCard.add(owner.orderProductCombo, gbc);

        // Row 3: Qtd & Desconto % (Side by Side)
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.insets = new Insets(8, 8, 2, 8);
        owner.orderQuantityLabel = new JLabel("Qtd total (unidades):");
        owner.orderQuantityLabel.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(owner.orderQuantityLabel, gbc);

        gbc.gridx = 1;
        JLabel discLbl = new JLabel("Desconto %:");
        discLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(discLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 10;
        gbc.insets = new Insets(2, 8, 12, 8);
        // Qtd em unidades + helper opcional "Caixas" (grosso): caixas × und/caixa → preenche a Qtd.
        owner.orderPackageEditor = new PackageQuantityEditor();
        owner.orderQuantityField = owner.orderPackageEditor.totalField();
        owner.orderBoxesField = owner.orderPackageEditor.boxesField();
        owner.orderLooseUnitsField = owner.orderPackageEditor.looseUnitsField();
        formCard.add(owner.orderPackageEditor, gbc);

        gbc.gridx = 1;
        owner.orderDiscountField = new DecimalField("0", 2, false);
        formCard.add(owner.orderDiscountField, gbc);

        // Row 4: Lote/Validade (FEFO, read-only) e Série
        gbc.gridx = 0; gbc.gridy = 11;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel batchLbl = new JLabel("Lote / Validade (FEFO):");
        batchLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(batchLbl, gbc);

        gbc.gridx = 1;
        JLabel serialLbl = new JLabel("Série (Opcional):");
        serialLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(serialLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 12;
        gbc.insets = new Insets(2, 8, 12, 8);
        owner.orderBatchField = new JTextField();
        UIHelper.styleTextField(owner.orderBatchField);
        owner.orderBatchField.setEditable(false);
        owner.orderBatchField.setToolTipText("Lote a sair (FEFO) — calculado a partir do produto e armazém.");
        owner.orderBatchField.putClientProperty("JTextField.placeholderText", "— FEFO automático —");
        formCard.add(owner.orderBatchField, gbc);

        gbc.gridx = 1;
        owner.orderSerialField = new JTextField();
        UIHelper.styleTextField(owner.orderSerialField);
        formCard.add(owner.orderSerialField, gbc);

        // Row 5: action aligned below the line fields.
        gbc.gridx = 0; gbc.gridy = 13; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 8, 12, 8);
        ModernButton addLineBtn = UIHelper.createAddLineButton();

        JPanel addLineActionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addLineActionRow.setOpaque(false);
        addLineActionRow.add(addLineBtn);
        formCard.add(addLineActionRow, gbc);

        // ===== Cartão de rascunho: tabela de linhas + total (separado do formulário, igual às Faturas) =====
        String[] lineCols = {"Produto", "Qtd / Caixas", "Peso kg", "% Qtd", "% Peso",
                "Preço Unit.", "Desc %", "Lote/Série", "Total"};
        owner.orderLinesTableModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.orderLinesTable = new JTable(owner.orderLinesTableModel);
        UIHelper.styleTable(owner.orderLinesTable);
        owner.orderLinesTable.setFillsViewportHeight(true);
        owner.orderLinesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        owner.orderLinesTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        owner.orderLinesTable.getColumnModel().getColumn(1).setPreferredWidth(125);
        owner.orderLinesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        owner.orderLinesTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        owner.orderLinesTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        owner.orderLinesTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        owner.orderLinesTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        owner.orderLinesTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        owner.orderLinesTable.getColumnModel().getColumn(8).setPreferredWidth(95);
        JScrollPane linesScroll = new JScrollPane(owner.orderLinesTable);
        UIHelper.styleScrollPane(linesScroll);

        owner.orderTotalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        owner.orderTotalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        owner.orderTotalLabel.setForeground(Color.WHITE);
        owner.orderLoadLabel = new JLabel("Carga: 0.000 kg");
        owner.orderLoadLabel.setForeground(UIHelper.TEXT_MUTED);
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        totalRow.add(owner.orderLoadLabel, BorderLayout.WEST);
        totalRow.add(owner.orderTotalLabel, BorderLayout.EAST);

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
        owner.orderFormContent = formContent;

        // ===== ABA: cabeçalho com acção 'Nova Encomenda…' + lista em ecrã inteiro (igual às Faturas) =====
        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(UIHelper.createHeading("Central de Pedidos e Separação"));
        JLabel workflowHint = new JLabel("Atendimento → Reserva → Separação → Faturação");
        workflowHint.setForeground(UIHelper.TEXT_MUTED);
        titleBlock.add(workflowHint);
        headerBar.add(titleBlock, BorderLayout.WEST);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Novo Pedido de Cliente…");
        newOrderBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newOrderBtn.addActionListener(e -> owner.openOrderEditor());
        headerActions.add(UIHelper.createRefreshButton(owner::loadOrdersTable));
        headerActions.add(newOrderBtn);
        headerBar.add(headerActions, BorderLayout.EAST);
        panel.add(headerBar, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        // A coluna Tipo vai no fim para não deslocar os índices já usados pelas acções.
        String[] ordersCols = {"ID", "Nº Encomenda", "Cliente", "Estado", "Total", "Impressões", "Tipo"};
        owner.ordersTableModel = new DefaultTableModel(ordersCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.ordersTable = new JTable(owner.ordersTableModel);
        UIHelper.styleTable(owner.ordersTable);
        owner.ordersTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        owner.ordersTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        // A célula guarda o OrderKind; o renderer mostra o rótulo PT-MZ. Assim as acções lêem a
        // via em vez de a inferirem de um texto traduzido.
        owner.ordersTable.getColumnModel().getColumn(ComercialPanel.ORDERS_COL_KIND)
                .setCellRenderer(TableCellRenderers.orderKind());
        owner.ordersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        owner.ordersTable.setFillsViewportHeight(true);

        // Hide ID column (col 0)
        owner.ordersTable.getColumnModel().getColumn(0).setMinWidth(0);
        owner.ordersTable.getColumnModel().getColumn(0).setMaxWidth(0);
        owner.ordersTable.getColumnModel().getColumn(0).setWidth(0);
        // Larguras proporcionais — Swing distribui o que faltar pelo restante espaço.
        owner.ordersTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Nº Encomenda
        owner.ordersTable.getColumnModel().getColumn(2).setPreferredWidth(170);  // Cliente
        owner.ordersTable.getColumnModel().getColumn(3).setPreferredWidth(75);   // Estado
        owner.ordersTable.getColumnModel().getColumn(4).setPreferredWidth(95);   // Total
        owner.ordersTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Impressões
        owner.ordersTable.getColumnModel().getColumn(ComercialPanel.ORDERS_COL_KIND)
                .setPreferredWidth(130);                                        // Tipo

        JScrollPane ordersScroll = new JScrollPane(owner.ordersTable);
        UIHelper.styleScrollPane(ordersScroll);

        JTextField ecSearch = TableFilter.searchField("Nº encomenda ou cliente…");
        JComboBox<String> ecEstado = TableFilter.combo("Todos os estados",
                "AWAITING_SEPARATION", "IN_SEPARATION", "SEPARATED", "INVOICED",
                "PENDING", "PENDING_APPROVAL", "GUIDE_PENDING", "GUIDED", "BILLED", "CANCELLED");
        TableFilter.install(owner.ordersTable, ecSearch, new TableFilter.ColumnFilter(ecEstado, 3));
        JPanel ecBar = TableFilter.bar(ecSearch, TableFilter.label("Estado:"), ecEstado);
        ecBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(ecBar, BorderLayout.NORTH);
        listCard.add(ordersScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Ver Detalhes", UIHelper.icon("fas-eye", 14), owner::openSelectedOrderDetails)
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), owner::printSelectedOrder)
                .addAction("Marcar como separado", UIHelper.icon("fas-box-open", 14), owner::completeSelectedOrderSeparation)
                .addAction("Ver histórico operacional", UIHelper.icon("fas-history", 14), owner::showSelectedOrderEvents)
                .addAction("Exportar Tabela", UIHelper.icon("fas-file-pdf", 14), owner::exportOrdersTable);
        ModernButton billOrderBtn = UIHelper.createSuccessButton("Faturar Encomenda");
        billOrderBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        ModernButton convertGuideBtn = UIHelper.createPrimaryButton("Converter em Guia");
        convertGuideBtn.setIcon(UIHelper.icon("fas-truck", 14));
        convertGuideBtn.setToolTipText("Criar uma Guia de Remessa a partir da encomenda aprovada selecionada.");
        ModernButton cancelOrderBtn = UIHelper.createDangerButton("Cancelar Encomenda…");
        cancelOrderBtn.setIcon(UIHelper.icon("fas-ban", 14));
        btnPanel.add(moreBtn);
        btnPanel.add(billOrderBtn);
        btnPanel.add(convertGuideBtn);
        btnPanel.add(cancelOrderBtn);
        listCard.add(btnPanel, BorderLayout.SOUTH);

        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        addLineBtn.addActionListener(e -> owner.addDraftOrderLine());
        owner.orderProductCombo.addActionListener(e -> {
            owner.refreshOrderFEFOHint();
            int index = owner.orderProductCombo.getSelectedIndex();
            if (index >= 0 && index < owner.productsList.size()) {
                int factor = owner.productsList.get(index).unitsPerBox();
                owner.orderPackageEditor.setUnitsPerBox(factor);
                owner.orderQuantityLabel.setText("Qtd total (" + Math.max(1, factor) + " un/caixa):");
            }
        });
        owner.orderWarehouseCombo.addActionListener(e -> owner.refreshOrderFEFOHint());
        billOrderBtn.addActionListener(e -> owner.billSelectedOrder());
        convertGuideBtn.addActionListener(e -> owner.convertSelectedOrderToGuide());
        cancelOrderBtn.addActionListener(e -> owner.openCancelOrderDialog());

        // Documento em painel completo (substitui o modal): a aba alterna lista <-> editor.
        DocumentEditorHost orderEditor = new DocumentEditorHost(
                "Novo Pedido de Cliente", owner.orderFormContent,
                owner::saveOrderFromEditor,
                owner::backToOrdersList,
                () -> !owner.draftOrderLines.isEmpty());
        owner.encomendasCards = new CardLayout();
        owner.encomendasHost = new JPanel(owner.encomendasCards);
        owner.encomendasHost.setOpaque(false);
        owner.encomendasHost.add(panel, "list");
        owner.encomendasHost.add(orderEditor, "editor");
        return owner.encomendasHost;
    }

}
