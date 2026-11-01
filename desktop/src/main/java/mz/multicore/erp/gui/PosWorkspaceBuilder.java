package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.pos.contingency.PosContingencyDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Construtor modular dos componentes e hierarquia visual do POS (layout e workspace).
 */
final class PosWorkspaceBuilder {

    private PosWorkspaceBuilder() {}

    static JPanel buildNorthStack(POSPanel owner) {
        owner.tabVendaBtn = new ModernButton("Venda POS");
        owner.tabVendaBtn.setIcon(UIHelper.icon("fas-cash-register", 14));
        owner.tabVendaBtn.addActionListener(e -> owner.selectView(false));

        owner.tabHistBtn = new ModernButton("Histórico de Vendas");
        owner.tabHistBtn.setIcon(UIHelper.icon("fas-history", 14));
        owner.tabHistBtn.addActionListener(e -> owner.selectView(true));

        owner.quotationBtn = UIHelper.createButton("Cotação", UIHelper.icon("fas-file-import", 14, Color.WHITE), UIHelper.ACCENT_CYAN, e -> owner.quotationActions.openImportDialog());
        owner.quotationBtn.setForeground(Color.WHITE);
        owner.quotationBtn.setShortcut("F7");
        owner.quotationBtn.setToolTipText("Importar Cotação / Pró-forma aberta para o carrinho");

        owner.loyaltyBtn = UIHelper.createButton("Fidelidade", UIHelper.icon("fas-star", 14, Color.WHITE), UIHelper.ACCENT, e -> owner.openLoyaltyDialog());
        owner.loyaltyBtn.setForeground(Color.WHITE);

        owner.contingencyBtn = UIHelper.createWarningButton("Contingência");
        owner.contingencyBtn.setIcon(UIHelper.icon("fas-exclamation-triangle", 14));
        owner.contingencyBtn.setVisible(false);
        owner.contingencyBtn.addActionListener(e -> new PosContingencyDialog(
                SwingUtilities.getWindowAncestor(owner), owner.contingencyManager, owner.contingencySyncService).setVisible(true));

        owner.contingencyManager.addListener(count -> SwingUtilities.invokeLater(() -> {
            owner.contingencyBtn.setVisible(count > 0);
            owner.contingencyBtn.setText("Contingência (" + count + ")");
        }));
        if (owner.contingencyManager.getPendingCount() > 0) {
            owner.contingencyBtn.setVisible(true);
            owner.contingencyBtn.setText("Contingência (" + owner.contingencyManager.getPendingCount() + ")");
        }

        owner.suspendedSalesBtn = new ModernButton("Em espera", UIHelper.SECONDARY, UIHelper.SECONDARY_HOVER);
        owner.suspendedSalesBtn.setIcon(UIHelper.icon("fas-pause-circle", 14));
        owner.suspendedSalesBtn.setToolTipText("Retomar uma venda em espera (Ctrl+R)");
        owner.suspendedSalesBtn.addActionListener(e -> owner.suspendedSaleActions.openResumeDialog());

        owner.operationsMenu = UIHelper.createActionMenuButton("Caixa e Turno")
                .addAction("Importar Cotação (F7)",UIHelper.icon("fas-file-import", 14, UIHelper.ACCENT_CYAN), () -> owner.quotationActions.openImportDialog())
                .addAction("Programa de Fidelidade", UIHelper.icon("fas-star", 14, UIHelper.ACCENT), owner::openLoyaltyDialog)
                .addAction("Histórico de Fechos (Z)", UIHelper.icon("fas-file-invoice-dollar", 14, UIHelper.ACCENT_BLUE), () -> owner.cashSessionActions.showSessionHistory())
                .addAction("Sangria / Suprimento", UIHelper.icon("fas-exchange-alt", 14, UIHelper.PENDING_YELLOW), () -> owner.cashSessionActions.manageCashMovements())
                .addAction("Passar Turno", UIHelper.icon("fas-people-arrows", 14, UIHelper.APPROVED_GREEN), () -> owner.cashSessionActions.shiftHandover());
        owner.operationsMenu.setColors(UIHelper.SECONDARY, UIHelper.SECONDARY_HOVER);
        owner.operationsMenu.setToolTipText("Ações e ferramentas adicionais do ponto de venda");

        JPanel segmented = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        segmented.setOpaque(false);
        segmented.add(owner.tabVendaBtn);
        segmented.add(owner.tabHistBtn);
        segmented.add(owner.operationsMenu);
        segmented.add(owner.suspendedSalesBtn);
        segmented.add(owner.contingencyBtn);

        owner.openSessionBtn = UIHelper.createSuccessButton("Abrir Caixa");
        owner.openSessionBtn.setIcon(UIHelper.icon("fas-lock-open", 14));
        owner.openSessionBtn.addActionListener(e -> owner.cashSessionActions.openSession());

        owner.closeSessionBtn = UIHelper.createDangerButton("Fechar Caixa");
        owner.closeSessionBtn.setShortcut("F12");
        owner.closeSessionBtn.setColors(UIHelper.ROW_ALT, UIHelper.GRID);
        owner.closeSessionBtn.setForeground(UIHelper.REJECTED_RED);
        owner.closeSessionBtn.setIcon(UIHelper.icon("fas-lock", 14, UIHelper.REJECTED_RED));
        owner.closeSessionBtn.addActionListener(e -> owner.cashSessionActions.closeSession());
        owner.closeSessionBtn.setVisible(false);

        owner.cashMoveBtn = UIHelper.createWarningButton("Sangria / Suprimento");
        owner.cashMoveBtn.setIcon(UIHelper.icon("fas-exchange-alt", 14));
        owner.cashMoveBtn.addActionListener(e -> owner.cashSessionActions.manageCashMovements());
        owner.cashMoveBtn.setVisible(false);

        owner.shiftHandoverBtn = new ModernButton("Passar Turno", UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN_HOVER);
        owner.shiftHandoverBtn.setIcon(UIHelper.icon("fas-people-arrows", 14, Color.WHITE));
        owner.shiftHandoverBtn.setForeground(Color.WHITE);
        owner.shiftHandoverBtn.setToolTipText("Passagem de turno entre operadores");
        owner.shiftHandoverBtn.addActionListener(e -> owner.cashSessionActions.shiftHandover());
        owner.shiftHandoverBtn.setVisible(false);

        JPanel sessionActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        sessionActions.setOpaque(false);
        sessionActions.add(owner.scaleWidget);
        ModernButton refreshPosBtn = UIHelper.createIconButton("Actualizar dados", "fas-sync-alt");
        refreshPosBtn.addActionListener(e -> owner.refreshOperationalData());
        sessionActions.add(refreshPosBtn);
        sessionActions.add(Box.createHorizontalStrut(16));
        sessionActions.add(owner.openSessionBtn);
        sessionActions.add(owner.closeSessionBtn);

        JPanel topBar = new JPanel(new GridBagLayout());
        topBar.setOpaque(false);
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.gridy = 0;
        topGbc.gridx = 0;
        topGbc.anchor = GridBagConstraints.WEST;
        topGbc.weightx = 0.0;
        topBar.add(segmented, topGbc);
        topGbc.gridx = 1;
        topGbc.weightx = 1.0;
        topGbc.fill = GridBagConstraints.HORIZONTAL;
        topBar.add(Box.createHorizontalGlue(), topGbc);
        topGbc.gridx = 2;
        topGbc.weightx = 0.0;
        topGbc.anchor = GridBagConstraints.EAST;
        topGbc.fill = GridBagConstraints.NONE;
        topBar.add(sessionActions, topGbc);

        owner.statusLabel = new JLabel("Caixa Fechada. Abra uma sessão para vender.");
        owner.statusLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        owner.statusLabel.setForeground(UIHelper.PENDING_YELLOW);
        owner.sessionBanner = PosLayout.createSessionBanner(owner.statusLabel, null);
        owner.sessionBanner.setBackground(UIHelper.ROW_ALT);
        owner.sessionBanner.setBorder(new EmptyBorder(2, 10, 2, 10));

        JPanel sessionBar = new JPanel(new BorderLayout(0, 2));
        sessionBar.setOpaque(false);
        sessionBar.add(topBar, BorderLayout.NORTH);
        sessionBar.add(owner.sessionBanner, BorderLayout.SOUTH);

        JPanel northStack = new JPanel();
        northStack.setOpaque(false);
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        sessionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        owner.feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        northStack.add(sessionBar);
        northStack.add(Box.createVerticalStrut(6));
        northStack.add(owner.feedback);
        return northStack;
    }

    static JPanel buildTopSelectsBar(POSPanel owner, JPanel barcodeBox) {
        owner.topSelectsBar = new JPanel(new GridBagLayout());
        owner.topSelectsBar.setOpaque(false);
        owner.topSelectsBar.setBorder(new EmptyBorder(4, 0, 4, 0));
        GridBagConstraints tg = new GridBagConstraints();
        tg.fill = GridBagConstraints.HORIZONTAL;
        tg.anchor = GridBagConstraints.NORTH;
        tg.gridy = 0;
        tg.gridx = 0;
        tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[0];
        tg.insets = new Insets(0, 0, 0, 6);
        owner.topSelectsBar.add(labeledField("Código de barras", barcodeBox), tg);
        JPanel clientPicker = new JPanel(new GridLayout(1, 2, 6, 0));
        clientPicker.setOpaque(false);
        clientPicker.add(PosLayout.searchRow(owner.clientSearchField));
        clientPicker.add(owner.clientCombo);
        tg.gridx = 1;
        tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[1];
        owner.topSelectsBar.add(labeledField("Cliente", clientPicker), tg);
        tg.gridx = 2;
        tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[2];
        owner.topSelectsBar.add(labeledField("Armazém", owner.warehouseCombo), tg);
        tg.gridx = 3;
        tg.weightx = PosLayout.HEADER_FIELD_WEIGHTS[3];
        tg.insets = new Insets(0, 0, 0, 0);
        owner.topSelectsBar.add(labeledField("Conta", owner.accountCombo), tg);
        PosLayout.showSelectionInTooltip(owner.clientCombo);
        PosLayout.showSelectionInTooltip(owner.warehouseCombo);
        PosLayout.showSelectionInTooltip(owner.accountCombo);
        return owner.topSelectsBar;
    }

    static JSplitPane buildWorkspace(POSPanel owner) {
        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workspace.setOpaque(false);
        workspace.setBorder(null);
        workspace.setDividerSize(10);
        workspace.setContinuousLayout(true);
        workspace.setResizeWeight(POSPanel.CATALOG_WIDTH_RATIO);
        workspace.setBackground(UIHelper.BG_DARK);

        // LEFT: Catálogo
        JPanel leftPanel = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        leftPanel.setOpaque(false);
        leftPanel.setMinimumSize(new Dimension(POSPanel.CATALOG_MIN_WIDTH, 10));

        JPanel catalogHeader = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        catalogHeader.setOpaque(false);
        JLabel prodTitle = new JLabel("Catálogo de Produtos", UIHelper.icon("fas-boxes", 15, UIHelper.ACCENT_BLUE), SwingConstants.LEFT);
        prodTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        prodTitle.setForeground(UIHelper.TEXT_LIGHT);
        catalogHeader.add(prodTitle, BorderLayout.NORTH);

        JComboBox<String> availabilityFilter = new JComboBox<>(new String[]{"Todos", "Disponíveis"});
        UIHelper.styleComboBox(availabilityFilter);
        availabilityFilter.setToolTipText("Mostrar todos os produtos ou apenas os disponíveis para venda");
        availabilityFilter.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        availabilityFilter.addActionListener(e -> {
            owner.showAllProducts = availabilityFilter.getSelectedIndex() == 0;
            owner.catalogController.loadCatalogPage(0);
        });

        JPanel catalogFilters = new JPanel(new BorderLayout(8, 0));
        catalogFilters.setOpaque(false);
        catalogFilters.add(PosLayout.searchRow(owner.productSearchField), BorderLayout.CENTER);
        catalogFilters.add(availabilityFilter, BorderLayout.EAST);
        catalogHeader.add(catalogFilters, BorderLayout.SOUTH);
        leftPanel.add(catalogHeader, BorderLayout.NORTH);

        owner.productGrid = PosLayout.catalogGrid();
        owner.productGrid.setOpaque(false);
        owner.productGridScroll = new JScrollPane(owner.productGrid);
        owner.productGridScroll.setBorder(BorderFactory.createEmptyBorder());
        owner.productGridScroll.getViewport().setOpaque(false);
        owner.productGridScroll.setOpaque(false);
        owner.productGridScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        owner.productGridScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        owner.productGridScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIHelper.styleScrollPane(owner.productGridScroll);
        owner.productGridScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                ((GridLayout) owner.productGrid.getLayout()).setColumns(PosLayout.catalogColumns(e.getComponent().getWidth()));
                owner.productGrid.revalidate();
            }
        });
        owner.formScroll = owner.productGridScroll;

        ModernPanel catalogCard = new ModernPanel(16);
        catalogCard.setLayout(new BorderLayout());
        catalogCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        catalogCard.add(owner.productGridScroll, BorderLayout.CENTER);
        catalogCard.add(owner.catalogController.buildPaginationBar(), BorderLayout.SOUTH);
        leftPanel.add(catalogCard, BorderLayout.CENTER);
        workspace.setLeftComponent(leftPanel);

        // RIGHT: Carrinho
        JPanel rightPanel = new JPanel(new BorderLayout(0, PosLayout.SECTION_VERTICAL_GAP));
        rightPanel.setOpaque(false);

        JPanel cartHeader = new JPanel(new BorderLayout(8, 0));
        cartHeader.setOpaque(false);
        JPanel cartTitleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cartTitleBox.setOpaque(false);
        JLabel cartTitle = new JLabel("Carrinho de Vendas (POS)", UIHelper.icon("fas-shopping-cart", 15, UIHelper.ACCENT_BLUE), SwingConstants.LEFT);
        cartTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        cartTitle.setForeground(UIHelper.TEXT_LIGHT);
        cartTitleBox.add(cartTitle);

        owner.cartItemCountLabel = new JLabel("• 0 artigos");
        owner.cartItemCountLabel.setForeground(UIHelper.TEXT_MUTED);
        owner.cartItemCountLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        cartTitleBox.add(owner.cartItemCountLabel);
        cartHeader.add(cartTitleBox, BorderLayout.WEST);

        owner.quotationBannerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        owner.quotationBannerPanel.setOpaque(false);
        owner.quotationBannerLabel = new JLabel("", UIHelper.icon("fas-file-invoice-dollar", 13, UIHelper.APPROVED_GREEN), SwingConstants.LEFT);
        owner.quotationBannerLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        owner.quotationBannerLabel.setForeground(UIHelper.APPROVED_GREEN);
        JButton clearQuotBtn = new JButton(UIHelper.icon("fas-times", 11, UIHelper.TEXT_MUTED));
        clearQuotBtn.setBorderPainted(false);
        clearQuotBtn.setContentAreaFilled(false);
        clearQuotBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearQuotBtn.setToolTipText("Remover vínculo da cotação");
        clearQuotBtn.addActionListener(e -> owner.quotationActions.clearQuotationLink());
        owner.quotationBannerPanel.add(owner.quotationBannerLabel);
        owner.quotationBannerPanel.add(clearQuotBtn);
        owner.quotationBannerPanel.setVisible(false);
        cartHeader.add(owner.quotationBannerPanel, BorderLayout.EAST);
        rightPanel.add(cartHeader, BorderLayout.NORTH);

        ModernPanel cartCard = new ModernPanel(16);
        cartCard.setLayout(new BorderLayout(0, PosLayout.CARD_VERTICAL_GAP));
        cartCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cartCols = {"Artigo", "Qtd", "Preço", "Desc.", "IVA", "Total"};
        owner.cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.cartTable = new JTable(owner.cartModel) {
            @Override public String getToolTipText(MouseEvent event) {
                int viewRow = rowAtPoint(event.getPoint());
                if (viewRow < 0) return null;
                int modelRow = convertRowIndexToModel(viewRow);
                if (modelRow < 0 || modelRow >= owner.cartItems.size()) return null;
                POSPanel.CartItem item = owner.cartItems.get(modelRow);
                String detail = item.note == null || item.note.isBlank() ? "Sem promoção" : item.note;
                if (item.batch != null && !item.batch.isBlank()) detail += " | Lote: " + item.batch;
                if (item.serial != null && !item.serial.isBlank()) detail += " | Série: " + item.serial;
                return detail;
            }
        };
        UIHelper.styleTable(owner.cartTable);
        owner.cartTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        owner.cartTable.putClientProperty("noRowInspector", Boolean.TRUE);
        owner.cartTable.setRowHeight(42);
        owner.cartTable.setFillsViewportHeight(true);
        owner.cartTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        owner.cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        PosLayout.configureOperationalCartColumns(owner.cartTable);
        UIHelper.ensureHeadersFit(owner.cartTable);
        owner.cartTable.setPreferredScrollableViewportSize(new Dimension(620, owner.cartTable.getRowHeight() * 12));
        JScrollPane cartScroll = new JScrollPane(owner.cartTable);
        cartScroll.setMinimumSize(new Dimension(0, 126));
        cartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        cartScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIHelper.styleScrollPane(cartScroll);
        owner.cartTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    owner.editSelectedCartQuantity();
                }
            }
        });

        owner.cartCenter = new JPanel(new CardLayout());
        owner.cartCenter.setOpaque(false);
        owner.cartCenter.setMinimumSize(new Dimension(0, 126));
        owner.cartCenter.add(PosCartController.buildEmptyStatePanel(), "empty");
        owner.cartCenter.add(cartScroll, "table");
        cartCard.add(owner.cartCenter, BorderLayout.CENTER);

        JPanel cartBottom = new JPanel();
        cartBottom.setLayout(new BoxLayout(cartBottom, BoxLayout.Y_AXIS));
        cartBottom.setOpaque(false);

        owner.subtotalValueLabel = new JLabel("0,00 MT");
        owner.ivaValueLabel = new JLabel("0,00 MT");
        owner.totalLabel = new JLabel("0,00 MT");
        ModernPanel totalRow = PosLayout.createTotalsRow(owner.subtotalValueLabel, owner.ivaValueLabel, owner.totalLabel);

        owner.creditCheck = new JCheckBox("Venda a Crédito (Conta Corrente)");
        owner.creditCheck.setToolTipText("Registar a venda a crédito na conta corrente do cliente");
        owner.creditCheck.setForeground(UIHelper.TEXT_LIGHT);
        owner.creditCheck.setOpaque(false);
        owner.creditCheck.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        JPanel creditRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        creditRow.setOpaque(false);
        creditRow.add(owner.creditCheck);

        JPanel buttonRow = new JPanel(new BorderLayout(8, 0));
        buttonRow.setOpaque(false);

        ModernButton removeBtn = UIHelper.createDangerButton("Remover");
        removeBtn.setShortcut("Del");
        removeBtn.setIcon(UIHelper.icon("fas-trash", 14, Color.WHITE));

        ModernButton decreaseBtn = UIHelper.createPrimaryButton("−");
        decreaseBtn.setColors(UIHelper.KPI_NEUTRAL_END, UIHelper.KPI_NEUTRAL_DARK);
        decreaseBtn.setIcon(null);
        decreaseBtn.setForeground(Color.WHITE);
        decreaseBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        decreaseBtn.setPreferredSize(new Dimension(38, 36));
        decreaseBtn.setToolTipText("Diminuir quantidade");

        ModernButton editQtyBtn = UIHelper.createPrimaryButton("Quantidade");
        editQtyBtn.setColors(UIHelper.KPI_NEUTRAL_END, UIHelper.KPI_NEUTRAL_DARK);
        editQtyBtn.setShortcut("F6");
        editQtyBtn.setIcon(UIHelper.icon("fas-sort-numeric-up", 12, Color.WHITE));
        editQtyBtn.setForeground(Color.WHITE);
        editQtyBtn.setToolTipText("Alterar quantidade seleccionada (F6)");

        ModernButton increaseBtn = UIHelper.createPrimaryButton("+");
        increaseBtn.setColors(UIHelper.KPI_NEUTRAL_END, UIHelper.KPI_NEUTRAL_DARK);
        increaseBtn.setIcon(null);
        increaseBtn.setForeground(Color.WHITE);
        increaseBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        increaseBtn.setPreferredSize(new Dimension(38, 36));
        increaseBtn.setToolTipText("Aumentar quantidade");

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftActions.setOpaque(false);
        leftActions.add(removeBtn);
        leftActions.add(decreaseBtn);
        leftActions.add(editQtyBtn);
        leftActions.add(increaseBtn);
        ModernButton suspendBtn = UIHelper.createIconButton("Suspender venda (Ctrl+S)", "fas-pause-circle");
        suspendBtn.addActionListener(e -> owner.suspendedSaleActions.suspendCurrentSale());
        leftActions.add(suspendBtn);
        buttonRow.add(leftActions, BorderLayout.WEST);

        owner.cartLineButtons = java.util.List.of(removeBtn, decreaseBtn, editQtyBtn, increaseBtn, suspendBtn);
        owner.checkoutBtn = UIHelper.createSuccessButton("Finalizar Venda");
        owner.checkoutBtn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        owner.checkoutBtn.setShortcut("F10");
        owner.checkoutBtn.setIcon(UIHelper.icon("fas-check-circle", 14, Color.WHITE));
        buttonRow.add(owner.checkoutBtn, BorderLayout.CENTER);

        cartBottom.add(totalRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 4)));
        cartBottom.add(creditRow);
        cartBottom.add(Box.createRigidArea(new Dimension(0, 6)));
        cartBottom.add(buttonRow);
        cartCard.add(cartBottom, BorderLayout.SOUTH);

        rightPanel.add(cartCard, BorderLayout.CENTER);
        rightPanel.setMinimumSize(new Dimension(POSPanel.CART_MIN_WIDTH, 10));
        workspace.setRightComponent(rightPanel);
        SwingUtilities.invokeLater(() -> workspace.setDividerLocation(POSPanel.CATALOG_WIDTH_RATIO));

        removeBtn.addActionListener(e -> owner.removeFromCart());
        decreaseBtn.addActionListener(e -> owner.changeSelectedQuantity(java.math.BigDecimal.ONE.negate()));
        editQtyBtn.addActionListener(e -> owner.editSelectedCartQuantity());
        increaseBtn.addActionListener(e -> owner.changeSelectedQuantity(java.math.BigDecimal.ONE));
        owner.checkoutBtn.addActionListener(e -> owner.runCheckout());

        return workspace;
    }

    private static JPanel labeledField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        // Largura mínima: quem reparte o espaço são os pesos, não o texto mais longo de cada selector.
        p.setPreferredSize(new Dimension(1, p.getPreferredSize().height));
        return p;
    }
}
