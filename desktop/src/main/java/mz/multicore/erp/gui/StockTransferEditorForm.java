package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.BarcodeScanField;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Editor de transferência com cabeçalho em linha canónico e grelha de linhas editável directamente (estilo PHC/Encomendas). */
final class StockTransferEditorForm {

    static final int COL_PRODUCT = StockTransferEditorRowCalculator.COL_PRODUCT;
    static final int COL_QUANTITY = StockTransferEditorRowCalculator.COL_QUANTITY;
    static final int COL_PACKAGES = StockTransferEditorRowCalculator.COL_PACKAGES;
    static final int COL_BOXES = StockTransferEditorRowCalculator.COL_BOXES;
    static final int COL_BOX_PERCENTAGE = StockTransferEditorRowCalculator.COL_BOX_PERCENTAGE;
    static final int COL_REFERENCE = StockTransferEditorRowCalculator.COL_REFERENCE;
    static final int COL_BARCODE = StockTransferEditorRowCalculator.COL_BARCODE;
    static final int COL_UNIT_PRICE = StockTransferEditorRowCalculator.COL_UNIT_PRICE;
    static final int COL_TAX = StockTransferEditorRowCalculator.COL_TAX;
    static final int COL_TOTAL = StockTransferEditorRowCalculator.COL_TOTAL;

    private final StockPanel owner;
    private final JPanel content = new JPanel(new BorderLayout(0, 12));
    private List<WarehouseDTO> warehouses = List.of();
    private List<ProductDTO> products = List.of();

    private final JComboBox<String> originCombo = new JComboBox<>();
    private final JComboBox<String> destinationCombo = new JComboBox<>();
    private final JTextField driverField = new JTextField();
    private final JTextField plateField = new JTextField();
    private final JTextField responsibleField = new JTextField();
    private final JTextField vehicleField = new JTextField();
    private final JTextField notesField = new JTextField();
    private final JCheckBox requiresReceiptCheck = new JCheckBox("Exigir confirmação de recepção no destino (stock em trânsito)");

    private final DefaultTableModel headerModel;
    private final JTable headerTable;

    private final ProductSearchComboBox productCellEditor = new ProductSearchComboBox();
    private final BarcodeScanField barcodeScanField;
    private final ModernButton searchCatalogButton = UIHelper.createSecondaryButton("Pesquisar Catálogo (F3)");
    private final ModernButton suggestReplenishmentButton = UIHelper.createSecondaryButton("Sugerir Reposição");
    private final ModernButton addButton = UIHelper.createAddLineButton();
    private final ModernButton removeButton = UIHelper.createDangerButton("Remover item");
    private final DefaultTableModel linesModel;
    private final JTable linesTable;
    private final JLabel totalLinesLabel = new JLabel("Itens: 0 | Qtd Total: 0");
    private final JLabel totalValueLabel = new JLabel("Total da Transferência: 0.00 MT");

    private StockTransferDTO loaded;
    private boolean loading;
    private boolean recalculating;
    private boolean tableEditable;
    private boolean dirty;

    StockTransferEditorForm(StockPanel owner) {
        this.owner = owner;
        content.setOpaque(false);

        UIHelper.styleComboBox(originCombo); UIHelper.styleComboBox(destinationCombo);
        for (JTextField f : List.of(driverField, plateField, responsibleField, vehicleField, notesField)) UIHelper.styleTextField(f);
        requiresReceiptCheck.setOpaque(false);
        requiresReceiptCheck.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        requiresReceiptCheck.setSelected(true);

        headerModel = new DefaultTableModel(
                new String[]{"Origem *", "Destino *", "Motorista *", "Matrícula *", "Responsável", "Viatura", "Observações"}, 1) {
            @Override public boolean isCellEditable(int row, int column) { return tableEditable; }
        };
        headerTable = new JTable(headerModel);
        headerTable.putClientProperty("noTableFooter", Boolean.TRUE);
        headerTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);

        content.add(StockTransferEditorHeaderSection.build(
                "Dados da Transferência",
                "Uma linha de dados gerais; edite as células directamente.",
                originCombo, destinationCombo, driverField, plateField,
                responsibleField, vehicleField, notesField, requiresReceiptCheck), BorderLayout.NORTH);

        String[] lineCols = {"Produto", "Qtd", "Emb.", "Cx.", "% Cx.", "Ref.", "Cód. Barras", "Valor Unit.", "IVA", "Total"};
        linesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return tableEditable && column >= COL_PRODUCT && column <= COL_BOXES;
            }
        };
        linesTable = new JTable(linesModel);
        linesTable.putClientProperty("noTableFooter", Boolean.TRUE);
        linesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(linesTable);
        UIHelper.ensureHeadersFit(linesTable);
        UIHelper.installDocumentGridShortcuts(linesTable, this::addLineAndStartEditing,
                this::removeSelectedLine, owner::saveTransferEditor, this::openProductCatalogLookup);
        StockTransferEditorGridConfigurator.configure(linesTable, productCellEditor);
        barcodeScanField = new BarcodeScanField("Bipar código / SKU (F2)...", this::handleBarcodeScanned);
        content.add(buildLinesSection(), BorderLayout.CENTER);
        installListeners();
    }

    JComponent component() { return content; }
    boolean isDirty() { return dirty; }
    StockTransferDTO loaded() { return loaded; }
    void markClean() { dirty = false; }

    void setOptions(List<WarehouseDTO> warehouses, List<ProductDTO> products) {
        this.warehouses = warehouses == null ? List.of() : new ArrayList<>(warehouses);
        this.products = products == null ? List.of() : new ArrayList<>(products);
        loading = true;
        originCombo.removeAllItems();
        destinationCombo.removeAllItems();
        for (WarehouseDTO w : this.warehouses) {
            originCombo.addItem(w.name());
            destinationCombo.addItem(w.name());
        }
        productCellEditor.setProducts(this.products);
        loading = false;
    }

    void prepareCreate() {
        stopCellEditing();
        loaded = null;
        loading = true;
        if (originCombo.getItemCount() > 0) originCombo.setSelectedIndex(0);
        if (destinationCombo.getItemCount() > 1) destinationCombo.setSelectedIndex(1);
        for (JTextField f : List.of(driverField, plateField, responsibleField, vehicleField, notesField)) f.setText("");
        requiresReceiptCheck.setSelected(true);
        syncHeaderModel();
        linesModel.setRowCount(0);
        loading = false;
        setEditable(true);
        dirty = false;
        addLineAndStartEditing();
        dirty = false;
    }

    void load(StockTransferDTO transfer) {
        stopCellEditing();
        loaded = transfer;
        loading = true;
        originCombo.setSelectedItem(warehouseName(transfer.originWarehouseId()));
        destinationCombo.setSelectedItem(warehouseName(transfer.destinationWarehouseId()));
        driverField.setText(nz(transfer.driverName()));
        plateField.setText(nz(transfer.vehiclePlate()));
        responsibleField.setText(nz(transfer.responsible()));
        vehicleField.setText(nz(transfer.vehicle()));
        notesField.setText(nz(transfer.notes()));
        requiresReceiptCheck.setSelected(transfer.requiresReceipt());
        syncHeaderModel();
        linesModel.setRowCount(0);
        if (transfer.lines() != null) {
            transfer.lines().forEach(line -> addLineRow(findProduct(line.productId()), line.quantity()));
        }
        loading = false;
        setEditable("DRAFT".equals(transfer.status()));
        dirty = false;
        updateTotals();
    }

    CreateStockTransferRequest createRequest() {
        Values values = validateValues();
        return new CreateStockTransferRequest(CurrentUserContext.getCurrentCompanyId(), values.originId(),
                values.destinationId(), values.responsible(), values.vehicle(), values.notes(),
                readLines(), values.driver(), values.plate(), requiresReceiptCheck.isSelected());
    }

    UpdateStockTransferRequest updateRequest() {
        if (loaded == null) throw new IllegalStateException("Nenhuma transferência carregada.");
        Values values = validateValues();
        return new UpdateStockTransferRequest(loaded.version(), values.originId(), values.destinationId(),
                values.responsible(), values.vehicle(), values.notes(), readLines(),
                values.driver(), values.plate(), requiresReceiptCheck.isSelected());
    }

    private JComponent buildLinesSection() {
        ModernPanel section = new ModernPanel(16);
        section.setLayout(new BorderLayout(0, 10));
        section.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel instruction = new JLabel("Edite directamente na grelha. Qtd, Emb. e Cx. mantêm-se sincronizadas.");
        instruction.setForeground(UIHelper.TEXT_MUTED);

        barcodeScanField.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        searchCatalogButton.setIcon(UIHelper.icon("fas-search", 14, UIHelper.ACCENT_BLUE));
        searchCatalogButton.setToolTipText("Abrir catálogo paginado para pesquisar e seleccionar artigos (F3)");
        searchCatalogButton.addActionListener(event -> openProductCatalogLookup());
        suggestReplenishmentButton.setIcon(UIHelper.icon("fas-magic", 14, UIHelper.ACCENT_BLUE));
        suggestReplenishmentButton.setToolTipText("Carrega produtos em falta no armazém de destino");
        suggestReplenishmentButton.addActionListener(event -> suggestReplenishmentForDestination());

        addButton.setText("Adicionar linha");
        addButton.setIcon(UIHelper.icon("fas-plus", 14));
        addButton.addActionListener(event -> addLineAndStartEditing());
        removeButton.setText("Remover item");
        removeButton.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removeButton.addActionListener(event -> removeSelectedLine());

        JScrollPane scroll = new JScrollPane(linesTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(0, 330));
        JPanel tableArea = new JPanel(new BorderLayout(0, 8));
        tableArea.setOpaque(false);
        tableArea.add(UIHelper.tableCardTop("Itens da Transferência", instruction,
                barcodeScanField, searchCatalogButton, suggestReplenishmentButton, addButton, removeButton), BorderLayout.NORTH);
        tableArea.add(scroll, BorderLayout.CENTER);

        totalLinesLabel.setForeground(UIHelper.TEXT_MUTED);
        totalValueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        totalValueLabel.setForeground(Color.WHITE);
        JPanel totalsRow = new JPanel(new BorderLayout());
        totalsRow.setOpaque(false);
        totalsRow.setBorder(new EmptyBorder(8, 4, 0, 4));
        totalsRow.add(totalLinesLabel, BorderLayout.WEST);
        totalsRow.add(totalValueLabel, BorderLayout.EAST);
        tableArea.add(totalsRow, BorderLayout.SOUTH);

        section.add(tableArea, BorderLayout.CENTER);
        return section;
    }

    private void addLineAndStartEditing() {
        if (!tableEditable) return;
        stopCellEditing();
        addLineRow(null, BigDecimal.ONE);
        int modelRow = linesModel.getRowCount() - 1, rowCount = linesTable.getRowCount();
        if (modelRow >= 0 && rowCount > 0) {
            int targetRow = modelRow < rowCount ? linesTable.convertRowIndexToView(modelRow) : rowCount - 1;
            try { linesTable.setRowSelectionInterval(targetRow, targetRow); } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> {
                try {
                    if (targetRow < linesTable.getRowCount() && linesTable.isShowing()) {
                        linesTable.scrollRectToVisible(linesTable.getCellRect(targetRow, COL_PRODUCT, true));
                        linesTable.editCellAt(targetRow, COL_PRODUCT);
                        Component editor = linesTable.getEditorComponent();
                        if (editor != null) editor.requestFocusInWindow();
                    }
                } catch (Exception ignored) {}
            });
        }
        dirty = true;
        updateTotals();
    }

    private void addLineRow(ProductDTO product, BigDecimal quantity) {
        linesModel.addRow(new Object[]{product, quantity, BigDecimal.ZERO, BigDecimal.ZERO, "0%", "—", "—", BigDecimal.ZERO, "0%", BigDecimal.ZERO});
        recalculateRow(linesModel.getRowCount() - 1, COL_QUANTITY);
    }

    private void openProductCatalogLookup() {
        StockTransferCatalogCoordinator.openCatalog(owner, content, products, tableEditable,
                this::stopCellEditing, this::applySelectedProducts);
    }

    private void applySelectedProducts(List<ProductDTO> selected) {
        StockTransferCatalogCoordinator.applySelectedProducts(linesTable, linesModel, selected,
                this::stopCellEditing, this::recalculateRow, this::addLineRow);
        dirty = true;
        updateTotals();
    }

    private void removeSelectedLine() {
        int viewRow = linesTable.getSelectedRow();
        if (viewRow < 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma linha", "Escolha um item para remover.");
            return;
        }
        stopCellEditing();
        linesModel.removeRow(linesTable.convertRowIndexToModel(viewRow));
        dirty = true;
        updateTotals();
    }

    private List<CreateStockTransferLineRequest> readLines() {
        stopCellEditing();
        if (linesModel.getRowCount() == 0) throw new IllegalArgumentException("Adicione pelo menos um produto à transferência.");
        List<CreateStockTransferLineRequest> lines = new ArrayList<>();
        for (int row = 0; row < linesModel.getRowCount(); row++) {
            Object productValue = linesModel.getValueAt(row, COL_PRODUCT);
            ProductDTO product = productCellEditor.resolve(productValue);
            if (product == null) {
                if (row < linesTable.getRowCount()) {
                    try { linesTable.setRowSelectionInterval(row, row); linesTable.editCellAt(row, COL_PRODUCT); } catch (Exception ignored) {}
                }
                throw new IllegalArgumentException("Pesquise e seleccione o produto da linha " + (row + 1) + ".");
            }
            BigDecimal quantity = decimal(linesModel.getValueAt(row, COL_QUANTITY));
            if (quantity.signum() <= 0) throw new IllegalArgumentException("A quantidade da linha " + (row + 1) + " deve ser superior a zero.");
            lines.add(new CreateStockTransferLineRequest(product.id(), quantity));
        }
        return lines;
    }

    private void recalculateRow(int row, int editedColumn) {
        if (recalculating || row < 0 || row >= linesModel.getRowCount()) return;
        recalculating = true;
        try {
            StockTransferEditorRowCalculator.recalculate(linesModel, productCellEditor, row, editedColumn);
        } finally {
            recalculating = false;
        }
        updateTotals();
    }

    private void updateTotals() {
        StockTransferEditorRowCalculator.updateTotals(linesModel, productCellEditor, vehicleField, totalLinesLabel, totalValueLabel);
    }

    private Values validateValues() {
        stopCellEditing();
        syncHeaderModel();
        int origin = originCombo.getSelectedIndex(), destination = destinationCombo.getSelectedIndex();
        if (origin < 0 || destination < 0) throw new IllegalArgumentException("Seleccione os armazéns de origem e destino.");
        if (origin == destination) throw new IllegalArgumentException("O armazém de origem e o de destino devem ser diferentes.");
        String driver = blank(driverField.getText()), plate = blank(plateField.getText());
        if (driver == null) { driverField.requestFocusInWindow(); throw new IllegalArgumentException("O nome do motorista é obrigatório."); }
        if (plate == null) { plateField.requestFocusInWindow(); throw new IllegalArgumentException("A matrícula do veículo é obrigatória."); }
        return new Values(warehouses.get(origin).id(), warehouses.get(destination).id(), driver, plate,
                blank(responsibleField.getText()), blank(vehicleField.getText()), blank(notesField.getText()));
    }

    private void syncHeaderModel() {
        if (headerModel == null) return;
        Object[] vals = {originCombo.getSelectedItem(), destinationCombo.getSelectedItem(), driverField.getText(), plateField.getText(), responsibleField.getText(), vehicleField.getText(), notesField.getText()};
        for (int i = 0; i < vals.length; i++) headerModel.setValueAt(vals[i], 0, i);
    }

    private void installListeners() {
        originCombo.addActionListener(e -> { if (!loading) dirty = true; });
        destinationCombo.addActionListener(e -> { if (!loading) dirty = true; });
        for (JTextField f : List.of(driverField, plateField, responsibleField, vehicleField, notesField)) {
            UIHelper.onTextChange(f, () -> { if (!loading) dirty = true; });
        }
        requiresReceiptCheck.addActionListener(e -> { if (!loading) dirty = true; });

        linesModel.addTableModelListener(event -> {
            if (loading || recalculating || event.getFirstRow() < 0) return;
            if (event.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int column = event.getColumn();
                if (column >= COL_PRODUCT && column <= COL_BOXES) recalculateRow(event.getFirstRow(), column);
            }
            dirty = true;
        });
    }

    private void setEditable(boolean editable) {
        tableEditable = editable;
        for (JComponent c : List.of(originCombo, destinationCombo, requiresReceiptCheck, addButton, removeButton)) c.setEnabled(editable);
        for (JTextField f : List.of(driverField, plateField, responsibleField, vehicleField, notesField)) f.setEditable(editable);
        if (barcodeScanField != null) barcodeScanField.setEnabled(editable);
        if (searchCatalogButton != null) searchCatalogButton.setEnabled(editable);
        if (suggestReplenishmentButton != null) suggestReplenishmentButton.setEnabled(editable);
        linesTable.repaint();
    }

    private void handleBarcodeScanned(String code) {
        StockTransferCatalogCoordinator.handleBarcodeScanned(code, owner, linesTable, linesModel,
                productCellEditor, products, tableEditable, this::stopCellEditing,
                this::recalculateRow, this::addLineRow);
        dirty = true;
        updateTotals();
    }

    private void suggestReplenishmentForDestination() {
        if (!tableEditable) return;
        StockTransferReplenishmentHelper.suggest(owner, content, warehouses, products,
                originCombo.getSelectedIndex(), destinationCombo.getSelectedIndex(), suggestions -> {
                    stopCellEditing();
                    if (linesModel.getRowCount() == 1) {
                        Object val = linesModel.getValueAt(0, COL_PRODUCT);
                        if (val == null || (val instanceof String s && s.isBlank())) linesModel.removeRow(0);
                    }
                    for (StockTransferReplenishmentHelper.ReplenishmentItem item : suggestions) {
                        addLineRow(item.product(), item.qtyNeeded());
                    }
                    dirty = true;
                    updateTotals();
                });
    }

    private void stopCellEditing() {
        if (linesTable.isEditing() && !linesTable.getCellEditor().stopCellEditing()) {
            linesTable.getCellEditor().cancelCellEditing();
        }
    }

    private String warehouseName(Long id) {
        for (WarehouseDTO w : warehouses) if (w.id().equals(id)) return w.name();
        return "";
    }

    private ProductDTO findProduct(Long id) {
        return products.stream().filter(product -> product.id().equals(id)).findFirst().orElse(null);
    }

    private static BigDecimal decimal(Object value) {
        return StockTransferEditorRowCalculator.decimal(value);
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String nz(String value) { return value == null ? "" : value; }

    private record Values(Long originId, Long destinationId, String driver, String plate,
                          String responsible, String vehicle, String notes) {}
}
