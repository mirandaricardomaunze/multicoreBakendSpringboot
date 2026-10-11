package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Editor, listagem e recepção de encomendas a fornecedor. */
final class PurchaseOrdersPanel {
    private final ComprasPanel owner;
    private QuantityField poQtyField;
    private PackageQuantityEditor poPackageEditor;
    private JTextField poExpectedField;
    private JTable poLinesTable;
    private DefaultTableModel poListModel;
    private JTable poListTable;
    private final CardLayout pages = new CardLayout();
    private DocumentEditorHost editorHost;
    private PurchaseOrderDTO editingOrder;
    private boolean lineGridEditable;
    private boolean syncingPoLines;
    private boolean editorDirty;
    PurchaseOrdersPanel(ComprasPanel owner) { this.owner = owner; }

    public void refreshCombos() {
        if (owner.poWarehouseCombo == null) return;
        owner.poWarehouseCombo.removeAllItems();
        for (WarehouseDTO w : owner.warehousesList) owner.poWarehouseCombo.addItem(w.name());
        if (owner.poProductCombo != null) {
            owner.poProductCombo.setProducts(owner.productsList);
        }
        refreshPackagingFactor();
    }

    // ===== Encomendas a Fornecedor =====

    public JPanel buildPanel() {
        JPanel tab = new JPanel(pages);
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        // ---- formulário (topo) ----
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(4, 8, 4, 8); g.weightx = 1;

        owner.poSupplierCombo = new JComboBox<>(); UIHelper.styleComboBox(owner.poSupplierCombo);
        owner.poWarehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(owner.poWarehouseCombo);
        owner.poProductCombo = new mz.multicore.erp.gui.components.ProductSearchComboBox();
        poPackageEditor = new PackageQuantityEditor();
        poQtyField = poPackageEditor.totalField();
        owner.poProductCombo.addActionListener(event -> refreshPackagingFactor());
        owner.poPriceField = new MoneyField("0");
        poExpectedField = new JTextField(); UIHelper.styleTextField(poExpectedField);
        poExpectedField.setToolTipText("Data prevista de entrega (aaaa-MM-dd) — opcional");

        g.gridx = 0; g.gridy = 0; g.weightx = 0.5; formCard.add(label("Fornecedor:"), g);
        g.gridx = 1; formCard.add(label("Armazém de destino:"), g);
        g.gridx = 0; g.gridy = 1; formCard.add(owner.poSupplierCombo, g);
        g.gridx = 1; formCard.add(owner.poWarehouseCombo, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.weightx = 1; formCard.add(label("Produto:"), g);
        g.gridy = 3; formCard.add(owner.poProductCombo, g);
        g.gridwidth = 1; g.weightx = 0.33;
        g.gridx = 0; g.gridy = 4; formCard.add(label("Qtd:"), g);
        g.gridx = 1; formCard.add(label("Preço unit. (compra):"), g);
        g.gridx = 2; formCard.add(label("Entrega prevista:"), g);
        g.gridx = 0; g.gridy = 5; formCard.add(poPackageEditor, g);
        g.gridx = 1; formCard.add(owner.poPriceField, g);
        g.gridx = 2; formCard.add(poExpectedField, g);

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        addLineBtn.addActionListener(e -> addPoDraftLine());
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); addRow.setOpaque(false);
        addRow.add(addLineBtn);
        g.gridx = 0; g.gridy = 6; g.gridwidth = 3; g.weightx = 1; g.insets = new Insets(10, 8, 4, 8);
        formCard.add(addRow, g);

        // ---- linhas (rascunho) ----
        String[] lineCols = {"Produto", "Qtd", "Emb.", "Cx.", "A receber agora", "Preço Unit.", "Lote", "Validade", "Série", "Total"};
        owner.poLinesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                if (!lineGridEditable) return false;
                if (editingOrder != null && ("ORDERED".equals(editingOrder.status())
                        || "PARTIALLY_RECEIVED".equals(editingOrder.status()))) return c == 4;
                return lineGridEditable && (c <= 3 || (c >= 5 && c <= 8));
            }
        };
        poLinesTable = new JTable(owner.poLinesModel);
        UIHelper.styleTable(poLinesTable);
        poLinesTable.setSurrendersFocusOnKeystroke(true);
        poLinesTable.getModel().addTableModelListener(event -> {
            if (syncingPoLines || event.getFirstRow() < 0
                    || event.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            syncPoLineFromGrid(event.getFirstRow(), event.getColumn());
        });
        UIHelper.installDocumentGridShortcuts(poLinesTable, this::addBlankPoLine,
                this::removeSelectedPoLine, this::savePurchaseOrder, this::openPoProductLookup);
        JScrollPane linesScroll = new JScrollPane(poLinesTable);
        UIHelper.styleScrollPane(linesScroll);

        owner.poTotalLabel = new JLabel("Total da Encomenda: 0.00 MT");
        owner.poTotalLabel.setForeground(Color.WHITE);
        owner.poTotalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        JPanel poFooter = new JPanel(new BorderLayout()); poFooter.setOpaque(false);
        poFooter.setBorder(new EmptyBorder(8, 0, 0, 0));
        poFooter.add(owner.poTotalLabel, BorderLayout.WEST);

        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        ModernButton addGridLineBtn = UIHelper.createAddLineButton();
        addGridLineBtn.setText("Adicionar linha");
        addGridLineBtn.addActionListener(event -> addBlankPoLine());
        ModernButton removeGridLineBtn = UIHelper.createDangerButton("Remover item");
        removeGridLineBtn.setIcon(UIHelper.icon("fas-trash", 14));
        removeGridLineBtn.addActionListener(event -> removeSelectedPoLine());
        ModernButton lookupBtn = UIHelper.createProductLookupButton(this::openPoProductLookup);
        JLabel gridHint = new JLabel("Edição directa na grelha: Produto, Qtd, Emb., Cx., preço, lote, validade e série.");
        gridHint.setForeground(UIHelper.TEXT_MUTED);
        draftCard.add(UIHelper.tableCardTop("Linhas da Encomenda", gridHint,
                lookupBtn, addGridLineBtn, removeGridLineBtn), BorderLayout.NORTH);
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(poFooter, BorderLayout.SOUTH);

        // Conteúdo do formulário (modal responsivo): inputs + linhas.
        JPanel poFormContentPanel = new JPanel(new BorderLayout(0, 10));
        poFormContentPanel.setOpaque(false);
        poFormContentPanel.add(formCard, BorderLayout.NORTH);
        poFormContentPanel.add(draftCard, BorderLayout.CENTER);
        owner.poFormContent = poFormContentPanel;

        // ---- lista de encomendas (base) ----
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> { owner.poSearchField.setText(""); refresh(); });

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Ações da Encomenda");
        actionsMenu.addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), this::openSelectedEditor);
        actionsMenu.addAction("Receber Encomenda", UIHelper.icon("fas-dolly", 14, UIHelper.APPROVED_GREEN), this::receiveSelectedPO);
        actionsMenu.addAction("Receber Parcial", UIHelper.icon("fas-dolly-flatbed", 14, UIHelper.ACCENT_BLUE), this::receivePartialSelectedPO);
        actionsMenu.addAction("Imprimir Etiquetas", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT), this::printSelectedPOReceiptLabels);
        actionsMenu.addAction("Cancelar Encomenda", UIHelper.icon("fas-ban", 14, UIHelper.REJECTED_RED), this::cancelSelectedPO);

        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Nova Encomenda");
        newOrderBtn.setIcon(UIHelper.icon("fas-clipboard-check", 14));
        newOrderBtn.addActionListener(e -> openPurchaseOrderFormDialog());

        String[] cols = {"Nº", "Fornecedor", "Estado", "Total", "Data", "Entrega prev.", "Negócio"};
        poListModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        poListTable = new JTable(poListModel);
        UIHelper.styleTable(poListTable);
        poListTable.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.status());
        poListTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        poListTable.getColumnModel().getColumn(6).setPreferredWidth(110);
        UIHelper.ensureHeadersFit(poListTable);
        JScrollPane listScroll = new JScrollPane(poListTable);
        UIHelper.styleScrollPane(listScroll);

        owner.poSearchField = TableFilter.searchField("Nº ou fornecedor…");
        owner.poBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        JComboBox<String> poEstado = TableFilter.combo("Todos os estados",
                "Encomendado", "Parcialmente recebido", "Recebido", "Cancelado");
        JComboBox<String> poPeriodo = TableFilter.periodCombo();

        TableFilter.install(poListTable, owner.poSearchField,
                java.util.List.of(
                        new TableFilter.ColumnFilter(owner.poBusinessUnitCombo, 6),
                        new TableFilter.ColumnFilter(poEstado, 2)),
                java.util.List.of(new TableFilter.PeriodFilter(poPeriodo, 4)));

        JPanel poFilters = TableFilter.bar(
                owner.poSearchField,
                TableFilter.label("Negócio:"), owner.poBusinessUnitCombo,
                TableFilter.label("Estado:"), poEstado,
                TableFilter.label("Período:"), poPeriodo);

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da encomenda (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        filtersRow.add(poFilters, BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        listCard.add(UIHelper.tableCardTop("Encomendas Registadas", filtersRow,
                refreshBtn, actionsMenu, newOrderBtn), BorderLayout.NORTH);
        listCard.add(listScroll, BorderLayout.CENTER);
        listCard.add(ClientTablePagination.install(poListTable), BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(poListTable, listCard, this::populatePeek);
        peekToggleBtn.addActionListener(e -> peek.toggle());

        JPanel listPage = new JPanel(new BorderLayout());
        listPage.setOpaque(false);
        listPage.add(listCard, BorderLayout.CENTER);
        editorHost = new DocumentEditorHost("Nova Encomenda", owner.poFormContent,
                this::savePurchaseOrder, () -> pages.show(tab, "LIST"), () -> editorDirty);
        tab.add(listPage, "LIST");
        tab.add(editorHost, "EDITOR");
        pages.show(tab, "LIST");
        return tab;
    }

    private void populatePeek(QuickPeekPanel peek, int modelRow) {
        if (owner.poList == null || modelRow < 0 || modelRow >= owner.poList.size()) return;
        PurchaseOrderDTO order = owner.poList.get(modelRow);
        String status = order.status() != null ? order.status() : "PENDING";
        Color statusColor = "RECEIVED".equalsIgnoreCase(status) ? UIHelper.APPROVED_GREEN
                : ("CANCELLED".equalsIgnoreCase(status) ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);

        peek.setHeaderIcon("fas-clipboard-check", UIHelper.ACCENT_BLUE);
        peek.setTitle("Encomenda " + (order.orderNumber() != null ? order.orderNumber() : ""));
        peek.setSubtitle(order.supplierName() != null ? order.supplierName() : "—");
        peek.setStatus(UIHelper.humanStatus(status), statusColor);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Fornecedor", order.supplierName() != null ? order.supplierName() : "-", false));
        if (order.businessUnitName() != null && !order.businessUnitName().isBlank()) {
            items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", order.businessUnitName(), false));
        }
        items.add(new QuickPeekPanel.PeekItem("Data Emissão", order.orderDate() != null ? order.orderDate().format(dtf) : "-", false));
        items.add(new QuickPeekPanel.PeekItem("Entrega Prevista", order.expectedDate() != null ? order.expectedDate().toString() : "-", false));
        int itemCount = order.lines() != null ? order.lines().size() : 0;
        items.add(new QuickPeekPanel.PeekItem("Itens Encomendados", String.valueOf(itemCount), false));
        BigDecimal total = order.totalAmount() != null ? order.totalAmount() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Valor Total", String.format("%,.2f MT", total), true));
        peek.setItems(items);

        if ("ORDERED".equalsIgnoreCase(status) || "PARTIALLY_RECEIVED".equalsIgnoreCase(status)) {
            peek.getOpenFullButton().setText("Receber Encomenda");
            peek.setOnOpenFullAction(ignored -> receiveSelectedPO());
        } else {
            peek.setOnOpenFullAction(null);
        }
    }

    void openPurchaseOrderFormDialog() {
        openPurchaseOrderForSuggestion(null, null, null, null);
    }

    void openPurchaseOrderForSuggestion(Long supplierId, Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        if (owner.supplierComboList.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Fornecedor necessário",
                    "Registe um fornecedor activo antes de criar a encomenda.");
            return;
        }
        editingOrder = null;
        lineGridEditable = true;
        editorDirty = false;
        // Reset do rascunho ao abrir.
        owner.poDraftLines.clear();
        if (owner.poLinesModel != null) owner.poLinesModel.setRowCount(0);
        recomputePoTotal();
        poExpectedField.setText("");

        // Pré-selecionar fornecedor se fornecido
        if (supplierId != null) {
            for (int i = 0; i < owner.supplierComboList.size(); i++) {
                if (supplierId.equals(owner.supplierComboList.get(i).id())) {
                    owner.poSupplierCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Pré-selecionar produto e pré-adicionar linha se fornecido
        if (productId != null) {
            owner.poProductCombo.selectProduct(productId);
            refreshPackagingFactor();
            if (quantity != null && quantity.signum() > 0) {
                poQtyField.setText(quantity.toPlainString());
            }
            if (unitPrice != null && unitPrice.signum() >= 0) {
                owner.poPriceField.setText(unitPrice.toPlainString());
            }
            addPoDraftLine();
        }

        configureProductCellEditor();
        editorHost.setEditorTitle("Nova Encomenda a Fornecedor");
        editorHost.setSaveText("Emitir Encomenda");
        editorHost.setSaveEnabled(true);
        showEditorPage();
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private void addPoDraftLine() {
        ProductDTO product = owner.poProductCombo.selectedProduct();
        if (product == null) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Produto necessário", "Seleccione um produto para adicionar.");
            return;
        }
        try {
            BigDecimal qty = poQtyField.value();
            BigDecimal price = owner.poPriceField.value();
            if (qty.signum() <= 0 || price.signum() < 0) throw new NumberFormatException();
            owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                    product.id(), qty, price, null, null, null));
            refreshPoRows();
            editorDirty = true;
            poPackageEditor.reset(); owner.poPriceField.setText("0");
        } catch (IllegalArgumentException ignored) {
            // Os campos canónicos já apresentam o estado inválido no formulário activo.
        }
    }

    private void addBlankPoLine() {
        if (!lineGridEditable) return;
        stopPoCellEditing();
        ProductDTO product = owner.productsList.isEmpty() ? null : owner.productsList.get(0);
        owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                product == null ? 0L : product.id(), BigDecimal.ONE, BigDecimal.ZERO,
                null, null, null));
        refreshPoRows();
        editorDirty = true;
        int row = poLinesTable.getRowCount() - 1;
        if (row >= 0) {
            poLinesTable.setRowSelectionInterval(row, row);
            poLinesTable.editCellAt(row, 0);
        }
    }

    private void removeSelectedPoLine() {
        if (!lineGridEditable) return;
        stopPoCellEditing();
        int viewRow = poLinesTable.getSelectedRow();
        if (viewRow < 0) return;
        int row = poLinesTable.convertRowIndexToModel(viewRow);
        if (row >= 0 && row < owner.poDraftLines.size()) {
            owner.poDraftLines.remove(row);
            refreshPoRows();
            editorDirty = true;
        }
    }

    private void configureProductCellEditor() {
        if (poLinesTable == null || poLinesTable.getColumnCount() == 0) return;
        ProductSearchComboBox poCellSearchCombo = new ProductSearchComboBox(owner.productsList);
        poCellSearchCombo.setOnLookupRequested(() -> {
            stopPoCellEditing();
            openPoProductLookup();
        });
        poLinesTable.getColumnModel().getColumn(0).setCellEditor(ProductSearchComboBox.createTableCellEditor(poCellSearchCombo));
    }

    private void openPoProductLookup() {
        if (!lineGridEditable) return;
        stopPoCellEditing();
        Window win = SwingUtilities.getWindowAncestor(poLinesTable != null ? poLinesTable : owner);
        Frame frame = win instanceof Frame f ? f : null;
        ProductLookupDialog.show(frame, owner.productsList, this::applySelectedPoProducts);
    }

    private void applySelectedPoProducts(List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty() || !lineGridEditable) return;
        stopPoCellEditing();
        int viewRow = poLinesTable != null ? poLinesTable.getSelectedRow() : -1;
        int modelRow = (viewRow >= 0 && poLinesTable != null) ? poLinesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < owner.poDraftLines.size()
                && (owner.poDraftLines.get(modelRow).productId() == null || owner.poDraftLines.get(modelRow).productId() <= 0L));

        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            if (p == null) continue;
            BigDecimal price = p.purchasePrice() != null ? p.purchasePrice() : (p.unitPrice() != null ? p.unitPrice() : BigDecimal.ZERO);
            if (i == 0 && fillCurrent) {
                owner.poDraftLines.set(modelRow, new CreatePurchaseOrderLineRequest(
                        p.id(), BigDecimal.ONE, price, null, null, null));
                targetModelRow = modelRow;
            } else {
                owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                        p.id(), BigDecimal.ONE, price, null, null, null));
                targetModelRow = owner.poDraftLines.size() - 1;
            }
        }
        refreshPoRows();
        editorDirty = true;
        if (targetModelRow >= 0 && poLinesTable != null && targetModelRow < poLinesTable.getRowCount()) {
            final int finalRow = targetModelRow;
            SwingUtilities.invokeLater(() -> {
                try {
                    int vr = poLinesTable.convertRowIndexToView(finalRow);
                    poLinesTable.setRowSelectionInterval(vr, vr);
                    poLinesTable.scrollRectToVisible(poLinesTable.getCellRect(vr, 1, true));
                    poLinesTable.editCellAt(vr, 1);
                    Component comp = poLinesTable.getEditorComponent();
                    if (comp != null) comp.requestFocusInWindow();
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void syncPoLineFromGrid(int row, int column) {
        if (row < 0 || row >= owner.poDraftLines.size()) return;
        ProductDTO product = owner.poLinesModel.getValueAt(row, 0) instanceof ProductDTO p ? p : null;
        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal units = decimalAt(row, 1);
        BigDecimal packages = decimalAt(row, 2).multiply(BigDecimal.valueOf(unitsPerPackage));
        BigDecimal boxes = decimalAt(row, 3).multiply(unitsPerBox);
        BigDecimal quantity = units.add(packages).add(boxes);
        BigDecimal price = decimalAt(row, 5);
        CreatePurchaseOrderLineRequest previous = owner.poDraftLines.get(row);
        owner.poDraftLines.set(row, new CreatePurchaseOrderLineRequest(
                product == null ? previous.productId() : product.id(), quantity, price,
                textAt(row, 6), dateAt(row, 7), textAt(row, 8)));
        editorDirty = true;
        refreshPoRows();
    }

    private void refreshPoRows() {
        stopPoCellEditing();
        syncingPoLines = true;
        owner.poLinesModel.setRowCount(0);
        for (CreatePurchaseOrderLineRequest line : owner.poDraftLines) {
            ProductDTO product = productById(line.productId());
            owner.poLinesModel.addRow(new Object[]{
                    product, line.quantity(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    line.unitPrice(), blank(line.batchNumber()),
                    line.expirationDate() == null ? "" : line.expirationDate().toString(),
                    blank(line.serialNumber()), line.quantity().multiply(line.unitPrice())});
        }
        syncingPoLines = false;
        recomputePoTotal();
    }

    private ProductDTO productById(Long id) {
        return owner.productsList.stream().filter(product -> product.id().equals(id)).findFirst().orElse(null);
    }

    private BigDecimal decimalAt(int row, int column) {
        Object value = owner.poLinesModel.getValueAt(row, column);
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value).trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private String textAt(int row, int column) {
        String value = String.valueOf(owner.poLinesModel.getValueAt(row, column)).trim();
        return value.isBlank() || "null".equals(value) ? null : value;
    }

    private LocalDate dateAt(int row, int column) {
        String value = textAt(row, column);
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException error) { throw new RuntimeException("Validade inválida na linha " + (row + 1) + ". Use aaaa-MM-dd."); }
    }

    private String blank(String value) { return value == null ? "" : value; }

    private void stopPoCellEditing() {
        if (poLinesTable.isEditing() && !poLinesTable.getCellEditor().stopCellEditing()) {
            poLinesTable.getCellEditor().cancelCellEditing();
        }
    }

    private void refreshPackagingFactor() {
        if (poPackageEditor == null) return;
        ProductDTO p = owner.poProductCombo != null ? owner.poProductCombo.selectedProduct() : null;
        if (p != null) {
            poPackageEditor.setUnitsPerBox(p.unitsPerBox());
        }
    }

    private void recomputePoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CreatePurchaseOrderLineRequest l : owner.poDraftLines) {
            total = total.add(l.quantity().multiply(l.unitPrice()));
        }
        owner.poTotalLabel.setText(String.format("Total da Encomenda: %,.2f MT", total));
    }

    /** Validação + criação da encomenda. Lança RuntimeException em erro (mantém o modal aberto). */
    private CreatePurchaseOrderRequest buildPurchaseOrderRequest() {
        stopPoCellEditing();
        int supIdx = owner.poSupplierCombo.getSelectedIndex();
        int whIdx = owner.poWarehouseCombo.getSelectedIndex();
        if (supIdx < 0 || supIdx >= owner.supplierComboList.size() || whIdx < 0 || whIdx >= owner.warehousesList.size()) {
            throw new RuntimeException("Selecione fornecedor e armazém.");
        }
        if (owner.poDraftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos uma linha.");
        }
        for (int row = 0; row < owner.poDraftLines.size(); row++) {
            CreatePurchaseOrderLineRequest line = owner.poDraftLines.get(row);
            if (productById(line.productId()) == null) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity().signum() <= 0) throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            if (line.unitPrice().signum() < 0) throw new RuntimeException("O preço da linha " + (row + 1) + " não pode ser negativo.");
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
        return new CreatePurchaseOrderRequest(
                owner.supplierComboList.get(supIdx).id(),
                owner.warehousesList.get(whIdx).id(),
                CurrentUserContext.getCurrentCompanyId(),
                expected, null, new ArrayList<>(owner.poDraftLines));
    }

    public void refresh() {
        if (poListModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        // Carrega todas; a pesquisa/estado/data é aplicada pelo TableFilter (cliente).
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.findOrdersByCompany(companyId), this::applyPurchaseOrders,
                error -> owner.showPurchaseLoadError("encomendas", error));
    }

    private void applyPurchaseOrders(List<PurchaseOrderDTO> loaded) {
        owner.poList = loaded;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        poListModel.setRowCount(0);
        java.util.Set<String> bus = new java.util.TreeSet<>();
        for (PurchaseOrderDTO o : owner.poList) {
            String buName = o.businessUnitName() != null && !o.businessUnitName().isBlank() ? o.businessUnitName() : "—";
            if (!"—".equals(buName)) bus.add(buName);
            poListModel.addRow(new Object[]{
                    o.orderNumber(), o.supplierName(), o.status(),
                    o.totalAmount() == null ? BigDecimal.ZERO : o.totalAmount(),
                    o.orderDate() == null ? "-" : o.orderDate().format(dtf),
                    o.expectedDate() == null ? "-" : o.expectedDate().toString(),
                    buName});
        }
        if (owner.poBusinessUnitCombo != null) {
            Object prev = owner.poBusinessUnitCombo.getSelectedItem();
            owner.poBusinessUnitCombo.removeAllItems();
            owner.poBusinessUnitCombo.addItem("Todos os negócios");
            owner.poBusinessUnitCombo.addItem("—");
            for (String b : bus) owner.poBusinessUnitCombo.addItem(b);
            if (prev != null) owner.poBusinessUnitCombo.setSelectedItem(prev);
        }
    }

    private void openSelectedEditor() {
        PurchaseOrderDTO selected = selectedPO();
        if (selected == null) return;
        editingOrder = selected;
        lineGridEditable = "ORDERED".equals(selected.status())
                && selected.lines().stream().noneMatch(line -> line.receivedQuantity() != null
                && line.receivedQuantity().signum() > 0);
        editorDirty = false;
        selectSupplier(selected.supplierId());
        selectWarehouse(selected.warehouseId());
        poExpectedField.setText(selected.expectedDate() == null ? "" : selected.expectedDate().toString());
        owner.poDraftLines.clear();
        if (selected.lines() != null) {
            for (PurchaseOrderLineDTO line : selected.lines()) {
                owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                        line.productId(), line.quantity(), line.unitPrice(), line.batchNumber(),
                        line.expirationDate(), line.serialNumber()));
            }
        }
        configureProductCellEditor();
        refreshPoRows();
        editorDirty = false;
        editorHost.setEditorTitle((lineGridEditable ? "Editar Encomenda " : "Consultar Encomenda ")
                + selected.orderNumber());
        editorHost.setSaveText("Guardar alterações");
        editorHost.setSaveEnabled(lineGridEditable);
        showEditorPage();
    }

    private void savePurchaseOrder() {
        try {
            CreatePurchaseOrderRequest create = buildPurchaseOrderRequest();
            if (editingOrder == null) {
                UIHelper.runWithProgress(owner, "A emitir encomenda…",
                        () -> owner.purchaseApiClient.createOrder(create),
                        saved -> finishPurchaseOrderSave(saved, "criada"), owner::showPurchaseError);
            } else {
                UpdatePurchaseOrderRequest request = new UpdatePurchaseOrderRequest(
                        editingOrder.version(), create.supplierId(), create.warehouseId(),
                        create.expectedDate(), create.notes(), create.lines());
                UIHelper.runWithProgress(owner, "A guardar alterações…",
                        () -> owner.purchaseApiClient.updateOrder(editingOrder.id(), request),
                        saved -> finishPurchaseOrderSave(saved, "actualizada"), owner::showPurchaseError);
            }
        } catch (RuntimeException error) {
            owner.showPurchaseNotice(FeedbackType.ERROR, "Dados incompletos", error.getMessage());
        }
    }

    private void finishPurchaseOrderSave(PurchaseOrderDTO saved, String action) {
        editorDirty = false;
        owner.showPurchaseSuccess("Encomenda " + saved.orderNumber() + " " + action + ".");
        Container parent = editorHost.getParent();
        if (parent != null && parent.getLayout() instanceof CardLayout layout) layout.show(parent, "LIST");
        refresh();
        owner.reorderPanel.refresh();
    }

    private void showEditorPage() {
        Container parent = editorHost.getParent();
        if (parent != null && parent.getLayout() instanceof CardLayout layout) layout.show(parent, "EDITOR");
    }

    private void selectSupplier(Long id) {
        for (int i = 0; i < owner.supplierComboList.size(); i++) {
            if (id != null && id.equals(owner.supplierComboList.get(i).id())) {
                owner.poSupplierCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectWarehouse(Long id) {
        for (int i = 0; i < owner.warehousesList.size(); i++) {
            if (id != null && id.equals(owner.warehousesList.get(i).id())) {
                owner.poWarehouseCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private PurchaseOrderDTO selectedPO() {
        int row = TableFilter.selectedModelRow(poListTable);
        if (row < 0 || row >= owner.poList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha uma encomenda na tabela para continuar.");
            return null;
        }
        return owner.poList.get(row);
    }

    private void receiveSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.WARNING,
                "Confirmar recepção",
                "Receber a encomenda " + sel.orderNumber() + "? O stock do armazém será actualizado.",
                "Receber encomenda")) return;
        UIHelper.runWithProgress(owner, "A receber encomenda…", () -> owner.purchaseApiClient.receiveOrder(sel.id()), ignored -> {
            owner.showPurchaseSuccess("Encomenda recebida e stock actualizado.");
            refresh();
            owner.loadPurchasesHistory();
        }, owner::showPurchaseError);
    }

    private void receivePartialSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!"ORDERED".equals(sel.status()) && !"PARTIALLY_RECEIVED".equals(sel.status())) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Recepção indisponível",
                    "Só encomendas por receber podem ser recebidas.");
            return;
        }

        PurchaseOrderReceivingDialog.show(SwingUtilities.getWindowAncestor(owner), sel, request -> {
            UIHelper.runWithProgress(owner, "A registar recepção parcial…",
                    () -> owner.purchaseApiClient.receivePartial(sel.id(), request), updated -> {
                owner.showPurchaseSuccess("Recepção registada · " + UIHelper.humanStatus(updated.status()) + ".");
                refresh();
                owner.loadPurchasesHistory();
            }, owner::showPurchaseError);
        });
    }

    private void cancelSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        String reason = UIHelper.promptRequiredText("Cancelar Encomenda", "fas-ban",
                "Encomenda " + sel.orderNumber(), "Motivo do cancelamento:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A cancelar encomenda…", () -> {
            owner.purchaseApiClient.cancelOrder(sel.id(), reason);
            return null;
        }, ignored -> refresh(), owner::showPurchaseError);
    }


    void printSelectedPOReceiptLabels() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        List<ProductDTO> products = extractReceivedProducts(sel);
        if (products.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Sem artigos recebidos", "Esta encomenda ainda não possui quantidades recebidas para emissão de etiquetas.");
            return;
        }
        ShelfLabelsDialog.show(SwingUtilities.getWindowAncestor(owner), products);
    }

    public static List<ProductDTO> extractReceivedProducts(PurchaseOrderDTO order) {
        List<ProductDTO> result = new ArrayList<>();
        if (order == null || order.lines() == null) return result;
        for (PurchaseOrderLineDTO line : order.lines()) {
            BigDecimal qty = line.receivedQuantity();
            if (qty != null && qty.signum() > 0) {
                String sku = line.productSku() != null && !line.productSku().isBlank()
                        ? line.productSku() : "ART-" + line.productId();
                String barcode = (line.productSku() != null && !line.productSku().isBlank())
                        ? line.productSku()
                        : (line.serialNumber() != null ? line.serialNumber() : sku);
                BigDecimal price = line.unitPrice() != null ? line.unitPrice() : BigDecimal.ZERO;
                result.add(new ProductDTO(
                        line.productId(),
                        sku,
                        sku,
                        barcode,
                        line.productName(),
                        price,
                        price,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        1,
                        "UNIT",
                        true,
                        null,
                        "Geral",
                        null,
                        line.taxRate() != null ? line.taxRate() : BigDecimal.ZERO,
                        "IVA",
                        "Entrada por Encomenda",
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            }
        }
        return result;
    }

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
