package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.BarcodeScanField;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ProductLookupDialog;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.UpdateQuotationRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulário embebido reutilizado para criar e editar cotações no padrão PHC.
 * Delega o cabeçalho para {@link QuotationEditorHeaderForm} e cálculos para {@link QuotationEditorRowCalculator}.
 */
final class QuotationEditorForm {

    private final QuotationsPanel owner;
    private final JPanel content = new JPanel(new BorderLayout(0, 12));
    private final QuotationEditorHeaderForm headerForm = new QuotationEditorHeaderForm();

    private List<ProductDTO> products = List.of();
    private final List<CreateQuotationLineRequest> draftLines = new ArrayList<>();
    private final List<BigDecimal> previewPrices = new ArrayList<>();

    private ProductSearchComboBox productCombo;
    private QuantityField quantityField;
    private DecimalField discountField;
    private BarcodeScanField barcodeScanField;
    private DefaultTableModel linesModel;
    private JTable linesTable;
    private JLabel totalLabel;
    private ModernButton addButton;
    private ModernButton removeButton;

    private QuotationDTO loaded;
    private boolean dirty;
    private boolean loading;
    private boolean syncingLines;
    private boolean tableEditable;
    private boolean inStopCellEditing;

    QuotationEditorForm(QuotationsPanel owner) {
        this.owner = owner;
        content.setOpaque(false);
        content.add(headerForm.buildPanel(), BorderLayout.NORTH);
        content.add(buildLinesSection(), BorderLayout.CENTER);
        content.registerKeyboardAction(e -> {
            if (barcodeScanField != null && barcodeScanField.isEnabled()) {
                barcodeScanField.requestFocusInWindow();
                barcodeScanField.selectAll();
            }
        }, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        headerForm.installDirtyTracking(() -> { if (!loading) dirty = true; });
    }

    JComponent component() { return content; }
    boolean isDirty() { return dirty; }
    QuotationDTO loaded() { return loaded; }
    void markClean() { dirty = false; }

    void setOptions(List<ClientDTO> clients, List<ProductDTO> products, List<WarehouseDTO> warehouses) {
        this.products = products == null ? List.of() : products;
        headerForm.setOptions(clients, warehouses);
        loading = true;
        productCombo.setProducts(this.products);
        loading = false;
    }

    void prepareCreate() {
        loaded = null; loading = true; headerForm.prepareCreate();
        draftLines.clear(); previewPrices.clear(); linesModel.setRowCount(0);
        loading = false; setEditable(true); dirty = false; refreshTotals();
    }

    void load(QuotationDTO quotation) {
        loaded = quotation; loading = true; headerForm.load(quotation);
        draftLines.clear(); previewPrices.clear();
        quotation.lines().forEach(line -> {
            draftLines.add(new CreateQuotationLineRequest(line.productId(), line.quantity(), line.discountPercentage()));
            previewPrices.add(line.unitPrice());
        });
        loading = false; setEditable("DRAFT".equals(quotation.status())); dirty = false; refreshRows();
    }

    CreateQuotationRequest createRequest() {
        validateLines();
        QuotationEditorHeaderForm.HeaderValues values = headerForm.validateHeader();
        return new CreateQuotationRequest(values.clientId(), values.walkInName(),
                CurrentUserContext.getCurrentCompanyId(), values.warehouseId(), values.validityDays(),
                values.paymentTerms(), values.deliveryTerms(), values.deliveryDays(), values.notes(),
                new ArrayList<>(draftLines));
    }

    UpdateQuotationRequest updateRequest() {
        if (loaded == null) throw new IllegalStateException("Nenhuma cotação carregada.");
        validateLines();
        QuotationEditorHeaderForm.HeaderValues values = headerForm.validateHeader();
        return new UpdateQuotationRequest(loaded.version(), values.clientId(), values.walkInName(),
                values.warehouseId(), values.validityDays(), values.paymentTerms(),
                values.deliveryTerms(), values.deliveryDays(), values.notes(), new ArrayList<>(draftLines));
    }

    private void validateLines() {
        stopCellEditing();
        if (draftLines.isEmpty()) throw new RuntimeException("Adicione pelo menos uma linha à cotação.");
        for (int row = 0; row < draftLines.size(); row++) {
            CreateQuotationLineRequest line = draftLines.get(row);
            if (findProduct(line.productId()) == null) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity().signum() <= 0) throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("O desconto da linha " + (row + 1) + " deve ficar entre 0 e 100.");
            }
        }
    }

    private JComponent buildLinesSection() {
        ModernPanel section = new ModernPanel(16);
        section.setLayout(new BorderLayout(0, 10));
        section.setBorder(new EmptyBorder(14, 16, 14, 16));

        productCombo = new ProductSearchComboBox();
        quantityField = new QuantityField("1", true);
        discountField = new DecimalField("0", 2, false);
        barcodeScanField = new BarcodeScanField("Bipar código / SKU (F2)...", this::handleBarcodeScanned);
        addButton = UIHelper.createSuccessButton("Adicionar linha"); addButton.setIcon(UIHelper.icon("fas-plus", 14)); addButton.addActionListener(e -> addBlankLine());
        removeButton = UIHelper.createDangerButton("Remover item"); removeButton.setIcon(UIHelper.icon("fas-trash", 14)); removeButton.addActionListener(e -> removeSelectedLine());

        linesModel = new DefaultTableModel(
                new String[]{"Produto", "Qtd", "Emb.", "Cx.", "% Cx.", "Preço Unit.", "Desc. %", "Total (c/ IVA)"}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return tableEditable && (column <= 3 || column == 6);
            }
        };
        linesTable = new JTable(linesModel); UIHelper.styleTable(linesTable);
        UIHelper.ensureHeadersFit(linesTable);
        linesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        linesTable.putClientProperty("noTableFooter", Boolean.TRUE);
        UIHelper.installDocumentGridShortcuts(linesTable, this::addBlankLine, this::removeSelectedLine, owner::saveEditor, this::openProductCatalogLookup);
        linesTable.getColumnModel().getColumn(0).setCellEditor(ProductSearchComboBox.createTableCellEditor(productCombo));
        linesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) { setText(v instanceof ProductDTO p ? p.name() : "Pesquisar produto..."); }
        });
        for (int col : List.of(1, 2, 3)) {
            linesTable.getColumnModel().getColumn(col).setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));
            linesTable.getColumnModel().getColumn(col).setCellRenderer(TableCellRenderers.quantity());
        }
        linesTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(discountField));
        linesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        linesTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        linesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        linesTable.setSurrendersFocusOnKeystroke(true);
        linesTable.setToolTipText("Edite Produto, Qtd, Emb., Cx. e Desc. directamente nas células. Pressione F2 para bipar.");
        linesModel.addTableModelListener(event -> {
            if (loading || syncingLines || event.getFirstRow() < 0 || event.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            syncLineFromGrid(event.getFirstRow(), event.getColumn());
        });
        JScrollPane scroll = new JScrollPane(linesTable); UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(900, 260));

        JPanel tableArea = new JPanel(new BorderLayout(0, 8)); tableArea.setOpaque(false);
        JLabel instruction = new JLabel("Edite na grelha ou use o leitor de código de barras (F2)."); instruction.setForeground(UIHelper.TEXT_MUTED);
        ModernButton lookupButton = UIHelper.createProductLookupButton(this::openProductCatalogLookup);
        tableArea.add(UIHelper.tableCardTop("Itens da Cotação", instruction, barcodeScanField, lookupButton, addButton, removeButton), BorderLayout.NORTH);
        tableArea.add(scroll, BorderLayout.CENTER);
        totalLabel = new JLabel("Total Rascunho: 0,00 MT (incl. IVA)");
        JPanel totalRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); totalRow.setOpaque(false); totalRow.add(totalLabel);
        tableArea.add(totalRow, BorderLayout.SOUTH);

        section.add(tableArea, BorderLayout.CENTER);
        return section;
    }

    private void openProductCatalogLookup() {
        if (!tableEditable) return;
        stopCellEditing();
        if (products == null || products.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Catálogo Vazio", "Não existem produtos registados no sistema.");
            return;
        }
        List<ProductDTO> selected = ProductLookupDialog.showMulti(content, products);
        if (selected != null && !selected.isEmpty()) applySelectedProducts(selected);
    }

    private void applySelectedProducts(List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty()) return;
        stopCellEditing();
        int viewRow = linesTable.getSelectedRow();
        int modelRow = viewRow >= 0 ? linesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < draftLines.size()
                && (draftLines.get(modelRow).productId() == null || draftLines.get(modelRow).productId() <= 0));
        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            BigDecimal price = p != null && p.unitPrice() != null ? p.unitPrice() : BigDecimal.ZERO;
            if (i == 0 && fillCurrent) {
                draftLines.set(modelRow, new CreateQuotationLineRequest(p.id(), BigDecimal.ONE, BigDecimal.ZERO));
                previewPrices.set(modelRow, price);
                targetModelRow = modelRow;
            } else {
                draftLines.add(new CreateQuotationLineRequest(p.id(), BigDecimal.ONE, BigDecimal.ZERO));
                previewPrices.add(price);
                targetModelRow = draftLines.size() - 1;
            }
        }
        refreshRows(); selectAndEditCell(targetModelRow, 1); dirty = true;
    }

    private void addBlankLine() {
        if (!tableEditable) return;
        stopCellEditing();
        draftLines.add(new CreateQuotationLineRequest(0L, BigDecimal.ONE, BigDecimal.ZERO));
        previewPrices.add(BigDecimal.ZERO);
        refreshRows(); selectAndEditCell(linesModel.getRowCount() - 1, 0); dirty = true;
    }

    private void selectAndEditCell(int modelRow, int column) {
        if (modelRow < 0 || linesTable.getRowCount() <= 0) return;
        int viewRow = Math.min(Math.max(0, modelRow), linesTable.getRowCount() - 1);
        try { if (modelRow < linesTable.getRowCount()) viewRow = linesTable.convertRowIndexToView(modelRow); } catch (Exception ignored) {}
        final int vr = viewRow;
        try { linesTable.setRowSelectionInterval(vr, vr); linesTable.scrollRectToVisible(linesTable.getCellRect(vr, column, true)); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            try {
                if (vr < linesTable.getRowCount()) {
                    linesTable.editCellAt(vr, column);
                    if (linesTable.getEditorComponent() != null) linesTable.getEditorComponent().requestFocusInWindow();
                }
            } catch (Exception ignored) {}
        });
    }

    private void removeSelectedLine() {
        int row = linesTable.getSelectedRow();
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione uma linha", "Escolha um item para remover.");
            return;
        }
        int index = linesTable.convertRowIndexToModel(row);
        draftLines.remove(index); previewPrices.remove(index);
        dirty = true; refreshRows();
    }

    private void refreshRows() {
        stopCellEditing();
        syncingLines = true;
        try {
            linesModel.setRowCount(0);
            for (int i = 0; i < draftLines.size(); i++) {
                CreateQuotationLineRequest line = draftLines.get(i);
                ProductDTO product = findProduct(line.productId());
                BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
                QuotationEditorRowCalculator.LineValues values = QuotationEditorRowCalculator.calculate(product, line.quantity(), discount);
                linesModel.addRow(new Object[]{product, line.quantity(), values.packages(), values.boxes(),
                        values.boxPercentage(), previewPrices.get(i), discount, values.total()});
            }
        } finally {
            syncingLines = false;
        }
        refreshTotals();
    }

    private void syncLineFromGrid(int row, int column) {
        if (syncingLines || row < 0 || row >= draftLines.size()) return;
        CreateQuotationLineRequest previous = draftLines.get(row);
        Object cellProduct = linesModel.getValueAt(row, 0);
        ProductDTO product = QuotationEditorRowCalculator.resolveProduct(products, productCombo, cellProduct, previous.productId());

        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal quantity;
        if (column == 2) quantity = decimal(linesModel.getValueAt(row, 2)).multiply(BigDecimal.valueOf(unitsPerPackage));
        else if (column == 3) quantity = decimal(linesModel.getValueAt(row, 3)).multiply(unitsPerBox);
        else quantity = decimal(linesModel.getValueAt(row, 1));
        if (quantity.signum() <= 0) quantity = BigDecimal.ONE;
        BigDecimal discount = decimal(linesModel.getValueAt(row, 6));

        draftLines.set(row, new CreateQuotationLineRequest(product == null ? 0L : product.id(), quantity, discount));
        BigDecimal unitPrice = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
        previewPrices.set(row, unitPrice);
        dirty = true;
        updateLineRow(row, product, quantity, discount);
    }

    private void handleBarcodeScanned(String code) {
        if (!tableEditable) return;
        ProductDTO product = products.stream().filter(p ->
                code.equalsIgnoreCase(p.barcode()) || code.equalsIgnoreCase(p.sku())
                || (p.reference() != null && code.equalsIgnoreCase(p.reference()))).findFirst().orElse(null);
        if (product == null) {
            Toolkit.getDefaultToolkit().beep();
            owner.showNotice(FeedbackType.WARNING, "Artigo não encontrado", "Nenhum produto cadastrado com o código ou SKU: " + code);
            return;
        }
        int existingIndex = -1;
        for (int i = 0; i < draftLines.size(); i++) {
            if (product.id().equals(draftLines.get(i).productId())) { existingIndex = i; break; }
        }
        if (existingIndex >= 0) {
            CreateQuotationLineRequest current = draftLines.get(existingIndex);
            BigDecimal newQty = current.quantity().add(BigDecimal.ONE);
            draftLines.set(existingIndex, new CreateQuotationLineRequest(product.id(), newQty, current.discountPercentage()));
            dirty = true;
            updateLineRow(existingIndex, product, newQty, current.discountPercentage());
            selectAndEditCell(existingIndex, 0);
        } else {
            stopCellEditing();
            draftLines.add(new CreateQuotationLineRequest(product.id(), BigDecimal.ONE, BigDecimal.ZERO));
            previewPrices.add(product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice());
            dirty = true; refreshRows(); selectAndEditCell(linesModel.getRowCount() - 1, 0);
        }
    }

    private void updateLineRow(int row, ProductDTO product, BigDecimal quantity, BigDecimal discount) {
        syncingLines = true;
        try {
            QuotationEditorRowCalculator.LineValues values = QuotationEditorRowCalculator.calculate(product, quantity, discount);
            QuotationEditorRowCalculator.updateRow(linesModel, row, product, quantity, discount, values);
        } finally {
            syncingLines = false;
        }
        refreshTotals();
    }

    private void refreshTotals() { QuotationEditorRowCalculator.updateTotalLabel(linesModel, totalLabel); }
    private ProductDTO findProduct(Long id) { return products.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null); }

    private void setEditable(boolean editable) {
        tableEditable = editable; headerForm.setEditable(editable);
        productCombo.setEnabled(editable); quantityField.setEnabled(editable); discountField.setEnabled(editable);
        addButton.setEnabled(editable); removeButton.setEnabled(editable);
        if (barcodeScanField != null) barcodeScanField.setEnabled(editable);
        linesTable.repaint();
    }

    private static BigDecimal decimal(Object value) { return QuotationEditorRowCalculator.decimal(value); }

    private void stopCellEditing() {
        if (inStopCellEditing) return;
        inStopCellEditing = true;
        try {
            if (linesTable != null && linesTable.isEditing() && linesTable.getCellEditor() != null) {
                if (!linesTable.getCellEditor().stopCellEditing()) {
                    linesTable.getCellEditor().cancelCellEditing();
                }
            }
        } finally {
            inStopCellEditing = false;
        }
    }
}
