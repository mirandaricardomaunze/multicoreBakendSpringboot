package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.OrderKind;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
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
        // A aba alterna lista <-> editor único; criar e actualizar nunca abrem formulário modal.
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // ===== EDITOR ÚNICO: inputs de cabeçalho agrupados em linha horizontal =====
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 10, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Coluna 0: Cliente
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 1.2;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel clientLbl = new JLabel("Cliente:");
        clientLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(clientLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderClientCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderClientCombo);
        formCard.add(owner.orderClientCombo, gbc);

        // Coluna 1: Nome do Comprador (Walk-in se 'Consumidor Final')
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel walkInLbl = new JLabel("Comprador (se Consumidor Final):");
        walkInLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(walkInLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderClientWalkInField = new JTextField();
        UIHelper.styleTextField(owner.orderClientWalkInField);
        owner.orderClientWalkInField.putClientProperty("JTextField.placeholderText", "Nome do comprador...");
        formCard.add(owner.orderClientWalkInField, gbc);

        // Coluna 2: Armazém Origem
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel warehouseLbl = new JLabel("Armazém:");
        warehouseLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(warehouseLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderWarehouseCombo);
        formCard.add(owner.orderWarehouseCombo, gbc);

        // Coluna 3: Tipo de Encomenda
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel kindLbl = new JLabel("Tipo de encomenda:");
        kindLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(kindLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderKindCombo = new JComboBox<>(OrderKind.values());
        owner.orderKindCombo.setRenderer(UIHelper.labelRenderer(OrderKind::label));
        UIHelper.styleComboBox(owner.orderKindCombo);
        owner.orderKindCombo.setSelectedItem(OrderKind.PICKING_REQUEST);
        formCard.add(owner.orderKindCombo, gbc);

        // Coluna 4: Armazém Destino (visível apenas para reposição interna)
        JLabel destLbl = new JLabel("Armazém de destino:");
        destLbl.setForeground(UIHelper.TEXT_MUTED);
        owner.orderDestinationCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderDestinationCombo);

        JPanel destPanel = new JPanel(new BorderLayout(0, 2));
        destPanel.setOpaque(false);
        destPanel.add(destLbl, BorderLayout.NORTH);
        destPanel.add(owner.orderDestinationCombo, BorderLayout.CENTER);

        gbc.gridx = 4; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 4, 6);
        formCard.add(destPanel, gbc);

        // Linha 2: Dica contextual da via selecionada
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 5; gbc.gridheight = 1; gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 6, 2, 6);
        JLabel kindHint = new JLabel();
        kindHint.setForeground(UIHelper.TEXT_MUTED);
        kindHint.setFont(kindHint.getFont().deriveFont(11f));
        kindHint.setIcon(UIHelper.semanticIcon("fas-info-circle", 12));
        formCard.add(kindHint, gbc);

        Runnable syncKind = () -> {
            OrderKind kind = owner.selectedOrderKind();
            kindHint.setText(" " + kindHint(kind));
            boolean replenishment = kind.requiresDestinationWarehouse();
            destPanel.setVisible(replenishment);
            GridBagLayout layout = (GridBagLayout) formCard.getLayout();
            GridBagConstraints c = layout.getConstraints(destPanel);
            c.weightx = replenishment ? 0.8 : 0.0;
            layout.setConstraints(destPanel, c);
            formCard.revalidate();
            formCard.repaint();
        };
        owner.orderKindCombo.addActionListener(e -> syncKind.run());
        syncKind.run();

        // Editores reutilizados apenas pelas células da grelha
        owner.orderProductCombo = new ProductSearchComboBox();
        owner.orderDiscountField = new DecimalField("0", 2, false);
        owner.orderSerialField = new JTextField();
        UIHelper.styleTextField(owner.orderSerialField);

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        addLineBtn.setText("Adicionar linha");

        // ===== Cartão de rascunho: tabela de linhas + total (separado do formulário, igual às Faturas) =====
        String[] lineCols = {"Produto", "Qtd", "Emb.", "Cx.", "% Cx.", "Peso kg", "% Qtd",
                "% Peso", "Preço Unit.", "Desc. %", "Série", "Total"};
        owner.orderLinesTableModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return owner.orderGridEditable && (c <= 3 || c == 9 || c == 10);
            }
        };
        owner.orderLinesTable = new JTable(owner.orderLinesTableModel);
        UIHelper.styleTable(owner.orderLinesTable);
        owner.orderLinesTable.getColumnModel().getColumn(0)
                .setCellEditor(ProductSearchComboBox.createTableCellEditor(owner.orderProductCombo));
        owner.orderLinesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof ProductDTO product ? product.name() : "Pesquisar produto...");
            }
        });
        for (int column : java.util.List.of(1, 2, 3)) {
            owner.orderLinesTable.getColumnModel().getColumn(column)
                    .setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));
            owner.orderLinesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.quantity());
        }
        owner.orderLinesTable.getColumnModel().getColumn(9)
                .setCellEditor(new DefaultCellEditor(owner.orderDiscountField));
        owner.orderLinesTable.getColumnModel().getColumn(10)
                .setCellEditor(new DefaultCellEditor(owner.orderSerialField));
        owner.orderLinesTable.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        owner.orderLinesTable.getColumnModel().getColumn(11).setCellRenderer(TableCellRenderers.money());
        owner.orderLinesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        owner.orderLinesTable.setSurrendersFocusOnKeystroke(true);
        owner.orderLinesTable.setToolTipText("Edite Produto, Qtd, Emb., Cx., desconto e série directamente.");
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
        owner.orderLinesTable.getColumnModel().getColumn(9).setPreferredWidth(75);
        owner.orderLinesTable.getColumnModel().getColumn(10).setPreferredWidth(120);
        owner.orderLinesTable.getColumnModel().getColumn(11).setPreferredWidth(95);
        JScrollPane linesScroll = new JScrollPane(owner.orderLinesTable);
        UIHelper.styleScrollPane(linesScroll);
        linesScroll.setPreferredSize(new Dimension(0, 380));
        linesScroll.setMinimumSize(new Dimension(0, 260));

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
        draftCard.setPreferredSize(new Dimension(0, 520));
        JPanel emptyFilters = new JPanel();
        emptyFilters.setOpaque(false);
        ModernButton removeItemBtn = UIHelper.createDangerButton("Remover item");
        removeItemBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removeItemBtn.addActionListener(e -> owner.removeSelectedDraftOrderLine());
        JLabel lineHint = new JLabel("Edição directa na grelha; embalagens e caixas permanecem sincronizadas.");
        lineHint.setForeground(UIHelper.TEXT_MUTED);
        draftCard.add(UIHelper.tableCardTop("Itens da Encomenda", lineHint,
                addLineBtn, removeItemBtn), BorderLayout.NORTH);
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(totalRow, BorderLayout.SOUTH);

        // Conteúdo partilhado pelos modos criar e editar: cabeçalho (NORTH) + itens (CENTER).
        JPanel formContent = new JPanel(new BorderLayout(0, 12));
        formContent.setOpaque(false);
        formContent.add(formCard, BorderLayout.NORTH);
        formContent.add(draftCard, BorderLayout.CENTER);
        owner.orderFormContent = formContent;

        // ===== ABA: acções, filtros e lista dentro do mesmo card =====
        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Novo Pedido de Cliente");
        newOrderBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newOrderBtn.addActionListener(e -> owner.openOrderEditor());

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Colunas novas vão sempre para o fim, para não deslocar os índices já usados pelas acções.
        String[] ordersCols = {"ID", "Nº Encomenda", "Cliente", "Estado", "Total", "Impressões", "Tipo",
                "Origem", "Entrega prevista"};
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
        owner.ordersTable.getColumnModel().getColumn(ComercialPanel.ORDERS_COL_ORIGIN)
                .setPreferredWidth(110);                                        // Origem
        owner.ordersTable.getColumnModel().getColumn(ComercialPanel.ORDERS_COL_DELIVERY)
                .setPreferredWidth(130);                                        // Entrega prevista

        JScrollPane ordersScroll = new JScrollPane(owner.ordersTable);
        UIHelper.styleScrollPane(ordersScroll);

        JTextField ecSearch = TableFilter.searchField("Nº encomenda ou cliente…");
        JComboBox<String> ecEstado = TableFilter.combo("Todos os estados",
                "AWAITING_SEPARATION", "IN_SEPARATION", "SEPARATED", "INVOICED",
                "PENDING", "PENDING_APPROVAL", "GUIDE_PENDING", "GUIDED", "BILLED", "CANCELLED");
        JComboBox<String> ecPeriodo = TableFilter.periodCombo();
        TableFilter.install(owner.ordersTable, ecSearch,
                java.util.List.of(new TableFilter.ColumnFilter(ecEstado, 3)),
                java.util.List.of(new TableFilter.PeriodFilter(ecPeriodo, ComercialPanel.ORDERS_COL_DELIVERY)));
        listCard.add(ordersScroll, BorderLayout.CENTER);
        TableQuickPeekController ordersPeek = UIHelper.installQuickPeek(owner.ordersTable, listCard);
        ordersPeek.setOnOpenFullCallback(modelRow -> owner.openSelectedOrderEditor());

        ModernButton refreshBtn = UIHelper.createRefreshButton(owner::loadOrdersTable);

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), owner::openSelectedOrderEditor)
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), owner::printSelectedOrder)
                .addAction("Ver histórico operacional", UIHelper.icon("fas-history", 14), owner::showSelectedOrderEvents)
                .addAction("Exportar Tabela", UIHelper.icon("fas-file-pdf", 14), owner::exportOrdersTable);
        ActionMenuButton convertMenu = UIHelper.createActionMenuButton("Processo")
                .addAction("Marcar como separado", UIHelper.icon("fas-box-open", 14), owner::completeSelectedOrderSeparation)
                .addAction("Converter em Guia", UIHelper.icon("fas-truck", 14), owner::convertSelectedOrderToGuide)
                .addAction("Converter em Transferência", UIHelper.icon("fas-dolly", 14), owner::convertSelectedOrderToTransfer)
                .addAction("Cancelar Encomenda", UIHelper.icon("fas-ban", 14), owner::openCancelOrderDialog)
                .addAction("Faturar Encomenda", UIHelper.icon("fas-file-invoice-dollar", 14, UIHelper.APPROVED_GREEN), owner::billSelectedOrder);
        convertMenu.setIcon(UIHelper.icon("fas-exchange-alt", 14));
        convertMenu.setToolTipText("Separação, conversão ou cancelamento da encomenda seleccionada.");

        JPanel ecFilters = UIHelper.filterBar(
                new JComponent[]{ecSearch, TableFilter.label("Estado:"), ecEstado,
                        TableFilter.label("Entrega:", "fas-calendar-alt"), ecPeriodo},
                null);
        ecFilters.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Central de Pedidos e Separação", ecFilters,
                refreshBtn, moreBtn, convertMenu, newOrderBtn), BorderLayout.NORTH);

        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        UIHelper.installDoubleClick(owner.ordersTable, owner::openSelectedOrderEditor);
        UIHelper.installDocumentGridShortcuts(owner.orderLinesTable,
                owner::addDraftOrderLine, owner::removeSelectedDraftOrderLine, owner::saveOrderFromEditor);
        addLineBtn.addActionListener(e -> owner.addDraftOrderLine());
        owner.orderLinesTableModel.addTableModelListener(event -> {
            if (owner.syncingOrderGrid || event.getFirstRow() < 0
                    || event.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            CommercialOrderEditorActions.syncLineFromGrid(owner, event.getFirstRow(), event.getColumn());
        });
        owner.orderClientCombo.addActionListener(e -> owner.markOrderEditorDirty());
        owner.orderWarehouseCombo.addActionListener(e -> owner.markOrderEditorDirty());
        owner.orderDestinationCombo.addActionListener(e -> owner.markOrderEditorDirty());
        owner.orderKindCombo.addActionListener(e -> owner.markOrderEditorDirty());
        UIHelper.onTextChange(owner.orderClientWalkInField, owner::markOrderEditorDirty);
        /*
         * As acções que não se aplicam à linha seleccionada ficam DESACTIVADAS, não escondidas.
         *
         * Uma encomenda segue uma via só: a venda a cliente termina em fatura ou guia de remessa, a
         * reposição interna termina em transferência entre armazéns. Com tudo sempre activo, dois
         * dos três botões ofereciam um caminho que o servidor ia recusar — o operador só descobria
         * qual, carregando. Desactivar diz-lhe antes, sem lhe tirar a acção da vista (regra do
         * harness SCUI: Faturar, Converter e Cancelar continuam explícitas).
         */
        Runnable syncOrderActions = () -> {
            int row = TableFilter.selectedModelRow(owner.ordersTable);
            OrderKind kind = null;
            if (row >= 0) {
                Object cell = owner.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_KIND);
                kind = cell instanceof OrderKind k ? k : OrderKind.FORMAL_ORDER;
            }
            boolean replenishment = kind != null && kind.usesWarehouseTransfer();
            boolean sale = kind != null && !replenishment;

            convertMenu.setEnabled(row >= 0);
            convertMenu.setActionEnabled(0, row >= 0);
            convertMenu.setActionEnabled(1, sale);
            convertMenu.setActionEnabled(2, replenishment);
            convertMenu.setActionEnabled(3, row >= 0);
            convertMenu.setActionEnabled(4, sale);

            convertMenu.setToolTipText(sale ? "Criar uma Guia de Remessa a partir da encomenda selecionada."
                    : replenishment ? "Criar a transferência entre armazéns que cumpre esta reposição."
                    : kind == null ? "Selecione uma encomenda na tabela."
                    : "Opções de conversão para a encomenda selecionada.");
        };
        owner.ordersTable.getSelectionModel()
                .addListSelectionListener(e -> syncOrderActions.run());
        syncOrderActions.run();

        // Documento em painel completo (substitui o modal): a aba alterna lista <-> editor.
        owner.orderEditor = new DocumentEditorHost(
                "Novo Pedido de Cliente", owner.orderFormContent,
                owner::saveOrderFromEditor,
                owner::backToOrdersList,
                owner::isOrderEditorDirty);
        owner.encomendasCards = new CardLayout();
        owner.encomendasHost = new JPanel(owner.encomendasCards);
        owner.encomendasHost.setOpaque(false);
        owner.encomendasHost.add(panel, "list");
        owner.encomendasHost.add(owner.orderEditor, "editor");
        return owner.encomendasHost;
    }

}
