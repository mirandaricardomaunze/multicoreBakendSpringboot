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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Editor, listagem e recepção de encomendas a fornecedor. */
final class PurchaseOrdersPanel {
    private final ComprasPanel owner;
    private final PurchaseOrderOperationsCoordinator operations;
    private final PurchaseOrderEditorCoordinator editor;

    private QuantityField poQtyField;
    private PackageQuantityEditor poPackageEditor;
    private JTextField poExpectedField;
    private JTable poLinesTable, poListTable;
    private DefaultTableModel poListModel;
    private final CardLayout pages = new CardLayout();
    private DocumentEditorHost editorHost;
    private PurchaseOrderDTO editingOrder;
    private boolean lineGridEditable, syncingPoLines, editorDirty;

    PurchaseOrdersPanel(ComprasPanel owner) {
        this.owner = owner;
        this.operations = new PurchaseOrderOperationsCoordinator(owner, this);
        this.editor = new PurchaseOrderEditorCoordinator(owner, this);
    }

    public void refreshCombos() {
        if (owner.poWarehouseCombo == null) return;
        owner.poWarehouseCombo.removeAllItems();
        for (WarehouseDTO w : owner.warehousesList) owner.poWarehouseCombo.addItem(w.name());
        if (owner.poProductCombo != null) owner.poProductCombo.setProducts(owner.productsList);
        refreshPackagingFactor();
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(pages);
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

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

        g.gridx = 0; g.gridy = 0; g.weightx = 0.5; formCard.add(label("Fornecedor:"), g); g.gridx = 1; formCard.add(label("Armazém de destino:"), g);
        g.gridx = 0; g.gridy = 1; formCard.add(owner.poSupplierCombo, g); g.gridx = 1; formCard.add(owner.poWarehouseCombo, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.weightx = 1; formCard.add(label("Produto:"), g);
        g.gridy = 3; formCard.add(owner.poProductCombo, g);
        g.gridwidth = 1; g.weightx = 0.33;
        g.gridx = 0; g.gridy = 4; formCard.add(label("Qtd:"), g); g.gridx = 1; formCard.add(label("Preço unit. (compra):"), g); g.gridx = 2; formCard.add(label("Entrega prevista:"), g);
        g.gridx = 0; g.gridy = 5; formCard.add(poPackageEditor, g); g.gridx = 1; formCard.add(owner.poPriceField, g); g.gridx = 2; formCard.add(poExpectedField, g);

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        addLineBtn.addActionListener(e -> addPoDraftLine());
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); addRow.setOpaque(false);
        addRow.add(addLineBtn);
        g.gridx = 0; g.gridy = 6; g.gridwidth = 3; g.weightx = 1; g.insets = new Insets(10, 8, 4, 8);
        formCard.add(addRow, g);

        String[] lineCols = {"Produto", "Qtd", "Emb.", "Cx.", "A receber agora", "Preço Unit.", "Lote", "Validade", "Série", "Total"};
        owner.poLinesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                if (!lineGridEditable) return false;
                if (editingOrder != null && ("ORDERED".equals(editingOrder.status()) || "PARTIALLY_RECEIVED".equals(editingOrder.status()))) return c == 4;
                return lineGridEditable && (c <= 3 || (c >= 5 && c <= 8));
            }
        };
        poLinesTable = new JTable(owner.poLinesModel);
        UIHelper.styleTable(poLinesTable);
        poLinesTable.setSurrendersFocusOnKeystroke(true);
        poLinesTable.getModel().addTableModelListener(event -> {
            if (!syncingPoLines && event.getFirstRow() >= 0 && event.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                syncPoLineFromGrid(event.getFirstRow(), event.getColumn());
            }
        });
        UIHelper.installDocumentGridShortcuts(poLinesTable, this::addBlankPoLine,
                this::removeSelectedPoLine, this::savePurchaseOrder, this::openPoProductLookup);
        JScrollPane linesScroll = new JScrollPane(poLinesTable);
        UIHelper.styleScrollPane(linesScroll);

        owner.poTotalLabel = new JLabel("Total da Encomenda: 0,00 MT");
        owner.poTotalLabel.setForeground(Color.WHITE);
        owner.poTotalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        JPanel poFooter = new JPanel(new BorderLayout()); poFooter.setOpaque(false);
        poFooter.setBorder(new EmptyBorder(8, 0, 0, 0));
        poFooter.add(owner.poTotalLabel, BorderLayout.WEST);

        // Contratos PHC/EDE: "Adicionar linha", "Remover item"
        ModernPanel draftCard = editor.buildDraftCard(linesScroll, poFooter,
                this::addBlankPoLine, this::removeSelectedPoLine, this::openPoProductLookup);

        JPanel poFormContentPanel = new JPanel(new BorderLayout(0, 10));
        poFormContentPanel.setOpaque(false);
        poFormContentPanel.add(formCard, BorderLayout.NORTH);
        poFormContentPanel.add(draftCard, BorderLayout.CENTER);
        owner.poFormContent = poFormContentPanel;

        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> { owner.poSearchField.setText(""); refresh(); });

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Acções da Encomenda");
        actionsMenu.addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), operations::openSelectedEditor);
        actionsMenu.addAction("Receber Encomenda", UIHelper.icon("fas-dolly", 14, UIHelper.APPROVED_GREEN), operations::receiveSelectedPO);
        actionsMenu.addAction("Receber Parcial", UIHelper.icon("fas-dolly-flatbed", 14, UIHelper.ACCENT_BLUE), this::receivePartialSelectedPO);
        actionsMenu.addAction("Imprimir Etiquetas", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT), operations::printSelectedPOReceiptLabels);
        actionsMenu.addAction("Cancelar Encomenda", UIHelper.icon("fas-ban", 14, UIHelper.REJECTED_RED), operations::cancelSelectedPO);

        ModernButton newOrderBtn = UIHelper.createSuccessButton("Nova Encomenda");
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
        JComboBox<String> poEstado = TableFilter.combo("Todos os estados", "Encomendado", "Parcialmente recebido", "Recebido", "Cancelado");
        JComboBox<String> poPeriodo = TableFilter.periodCombo();
        TableFilter.install(poListTable, owner.poSearchField,
                java.util.List.of(new TableFilter.ColumnFilter(owner.poBusinessUnitCombo, 6), new TableFilter.ColumnFilter(poEstado, 2)),
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
        TableQuickPeekController peek = TableQuickPeekController.install(poListTable, listCard, operations::populatePeek);
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

    void openPurchaseOrderFormDialog() { openPurchaseOrderForSuggestion(null, null, null, null); }

    void openPurchaseOrderForSuggestion(Long supplierId, Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        if (owner.supplierComboList.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Fornecedor necessário", "Registe um fornecedor activo antes de criar a encomenda.");
            return;
        }
        editingOrder = null; lineGridEditable = true; editorDirty = false;
        owner.poDraftLines.clear();
        if (owner.poLinesModel != null) owner.poLinesModel.setRowCount(0);
        recomputePoTotal();
        poExpectedField.setText("");

        if (supplierId != null) {
            for (int i = 0; i < owner.supplierComboList.size(); i++) {
                if (supplierId.equals(owner.supplierComboList.get(i).id())) { owner.poSupplierCombo.setSelectedIndex(i); break; }
            }
        }
        if (productId != null) {
            owner.poProductCombo.selectProduct(productId);
            refreshPackagingFactor();
            if (quantity != null && quantity.signum() > 0) poQtyField.setText(quantity.toPlainString());
            if (unitPrice != null && unitPrice.signum() >= 0) owner.poPriceField.setText(unitPrice.toPlainString());
            addPoDraftLine();
        }
        configureProductCellEditor();
        editorHost.setEditorTitle("Nova Encomenda a Fornecedor");
        editorHost.setSaveText("Emitir Encomenda");
        editorHost.setSaveEnabled(true);
        showEditorPage();
    }

    private JLabel label(String text) { JLabel l = new JLabel(text); l.setForeground(UIHelper.TEXT_MUTED); return l; }

    private void addPoDraftLine() {
        ProductDTO product = owner.poProductCombo.selectedProduct();
        if (product == null) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Produto necessário", "Seleccione um produto para adicionar.");
            return;
        }
        try {
            BigDecimal qty = poQtyField.value(), price = owner.poPriceField.value();
            if (qty.signum() <= 0 || price.signum() < 0) throw new NumberFormatException();
            owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(product.id(), qty, price, null, null, null));
            refreshPoRows();
            editorDirty = true;
            poPackageEditor.reset();
            owner.poPriceField.setText("0");
        } catch (IllegalArgumentException ignored) {}
    }

    void addBlankPoLine() { editor.addBlankPoLine(poLinesTable); }
    void removeSelectedPoLine() { editor.removeSelectedPoLine(poLinesTable); }
    void configureProductCellEditor() { editor.configureProductCellEditor(poLinesTable); }
    void openPoProductLookup() { editor.openPoProductLookup(poLinesTable); }
    void openSelectedEditor() { operations.openSelectedEditor(); }

    void syncPoLineFromGrid(int row, int column) {
        if (row < 0 || row >= owner.poDraftLines.size()) return;
        ProductDTO product = owner.poLinesModel.getValueAt(row, 0) instanceof ProductDTO p ? p : null;
        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal units = editor.decimalAt(row, 1);
        BigDecimal packages = editor.decimalAt(row, 2).multiply(BigDecimal.valueOf(unitsPerPackage));
        BigDecimal boxes = editor.decimalAt(row, 3).multiply(unitsPerBox);
        BigDecimal quantity = units.add(packages).add(boxes);
        BigDecimal price = editor.decimalAt(row, 5);
        CreatePurchaseOrderLineRequest previous = owner.poDraftLines.get(row);
        owner.poDraftLines.set(row, new CreatePurchaseOrderLineRequest(
                product == null ? previous.productId() : product.id(), quantity, price,
                editor.textAt(row, 6), editor.dateAt(row, 7), editor.textAt(row, 8)));
        editorDirty = true;
        refreshPoRows();
    }

    void refreshPoRows() {
        stopPoCellEditing();
        syncingPoLines = true;
        owner.poLinesModel.setRowCount(0);
        for (CreatePurchaseOrderLineRequest line : owner.poDraftLines) {
            ProductDTO product = editor.productById(line.productId());
            owner.poLinesModel.addRow(new Object[]{
                    product, line.quantity(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    line.unitPrice(), line.batchNumber() == null ? "" : line.batchNumber(),
                    line.expirationDate() == null ? "" : line.expirationDate().toString(),
                    line.serialNumber() == null ? "" : line.serialNumber(), line.quantity().multiply(line.unitPrice())});
        }
        syncingPoLines = false;
        recomputePoTotal();
    }

    void stopPoCellEditing() {
        if (poLinesTable != null && poLinesTable.isEditing() && !poLinesTable.getCellEditor().stopCellEditing()) {
            poLinesTable.getCellEditor().cancelCellEditing();
        }
    }

    private void refreshPackagingFactor() {
        if (poPackageEditor == null) return;
        ProductDTO p = owner.poProductCombo != null ? owner.poProductCombo.selectedProduct() : null;
        if (p != null) poPackageEditor.setUnitsPerBox(p.unitsPerBox());
    }

    private void recomputePoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CreatePurchaseOrderLineRequest l : owner.poDraftLines) total = total.add(l.quantity().multiply(l.unitPrice()));
        owner.poTotalLabel.setText(String.format("Total da Encomenda: %,.2f MT", total));
    }

    private CreatePurchaseOrderRequest buildPurchaseOrderRequest() {
        stopPoCellEditing();
        int supIdx = owner.poSupplierCombo.getSelectedIndex(), whIdx = owner.poWarehouseCombo.getSelectedIndex();
        if (supIdx < 0 || supIdx >= owner.supplierComboList.size() || whIdx < 0 || whIdx >= owner.warehousesList.size()) {
            throw new RuntimeException("Selecione fornecedor e armazém.");
        }
        if (owner.poDraftLines.isEmpty()) throw new RuntimeException("Adicione pelo menos uma linha.");
        for (int row = 0; row < owner.poDraftLines.size(); row++) {
            CreatePurchaseOrderLineRequest line = owner.poDraftLines.get(row);
            if (editor.productById(line.productId()) == null) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity().signum() <= 0) throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            if (line.unitPrice().signum() < 0) throw new RuntimeException("O preço da linha " + (row + 1) + " não pode ser negativo.");
        }
        LocalDate expected = null;
        String t = poExpectedField.getText().trim();
        if (!t.isEmpty()) {
            try { expected = LocalDate.parse(t); }
            catch (DateTimeParseException ex) { throw new RuntimeException("Data de entrega inválida (aaaa-MM-dd)."); }
        }
        return new CreatePurchaseOrderRequest(
                owner.supplierComboList.get(supIdx).id(), owner.warehousesList.get(whIdx).id(),
                CurrentUserContext.getCurrentCompanyId(), expected, null, new ArrayList<>(owner.poDraftLines));
    }

    public void refresh() {
        if (poListModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.findOrdersByCompany(companyId),
                operations::applyPurchaseOrders, error -> owner.showPurchaseLoadError("encomendas", error));
    }

    private void savePurchaseOrder() {
        try { operations.executeSave(buildPurchaseOrderRequest(), editingOrder); }
        catch (RuntimeException error) { owner.showPurchaseNotice(FeedbackType.ERROR, "Dados incompletos", error.getMessage()); }
    }

    void showEditorPage() {
        Container parent = editorHost.getParent();
        if (parent != null && parent.getLayout() instanceof CardLayout layout) layout.show(parent, "EDITOR");
    }

    PurchaseOrderDTO selectedPO() {
        int row = TableFilter.selectedModelRow(poListTable);
        if (row < 0 || row >= owner.poList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha uma encomenda na tabela para continuar.");
            return null;
        }
        return owner.poList.get(row);
    }

    void receivePartialSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!"ORDERED".equals(sel.status()) && !"PARTIALLY_RECEIVED".equals(sel.status())) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Recepção indisponível", "Só encomendas por receber podem ser recebidas.");
            return;
        }
        PurchaseOrderReceivingDialog.show(SwingUtilities.getWindowAncestor(owner), sel, req -> operations.receivePartial(sel, req));
    }

    public static List<ProductDTO> extractReceivedProducts(PurchaseOrderDTO order) {
        return PurchaseOrderOperationsCoordinator.extractReceivedProducts(order);
    }

    DefaultTableModel getPoListModel() { return poListModel; }
    void setEditingOrder(PurchaseOrderDTO order) { this.editingOrder = order; }
    void setLineGridEditable(boolean editable) { this.lineGridEditable = editable; }
    boolean isLineGridEditable() { return lineGridEditable; }
    void setEditorDirty(boolean dirty) { this.editorDirty = dirty; }
    JTextField getPoExpectedField() { return poExpectedField; }
    DocumentEditorHost getEditorHost() { return editorHost; }
}
