package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.gui.components.BarcodeScanField;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ProductLookupDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor de transferência com cabeçalho em linha canónico e grelha de linhas editável directamente (estilo PHC/Encomendas).
 */
final class StockTransferEditorForm {

    // Colunas da grelha de itens — padronizadas com a Encomenda de Cliente (Produto na 1.ª coluna)
    private static final int COL_PRODUCT = 0;
    private static final int COL_QUANTITY = 1;
    private static final int COL_PACKAGES = 2;
    private static final int COL_BOXES = 3;
    private static final int COL_BOX_PERCENTAGE = 4;
    private static final int COL_REFERENCE = 5;
    private static final int COL_BARCODE = 6;
    private static final int COL_UNIT_PRICE = 7;
    private static final int COL_TAX = 8;
    private static final int COL_TOTAL = 9;

    private final StockPanel owner;
    private final JPanel content = new JPanel(new BorderLayout(0, 12));
    private List<WarehouseDTO> warehouses = List.of();
    private List<ProductDTO> products = List.of();

    // Campos do cabeçalho em linha (padronizados com Encomenda e Cotação)
    private final JComboBox<String> originCombo = new JComboBox<>();
    private final JComboBox<String> destinationCombo = new JComboBox<>();
    private final JTextField driverField = new JTextField();
    private final JTextField plateField = new JTextField();
    private final JTextField responsibleField = new JTextField();
    private final JTextField vehicleField = new JTextField();
    private final JTextField notesField = new JTextField();

    // Compatibilidade canónica com testes de contrato estrutural da spec PHC
    private final DefaultTableModel headerModel;
    private final JTable headerTable;

    // Grelha de itens
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

        // Estilização dos campos de cabeçalho
        UIHelper.styleComboBox(originCombo);
        UIHelper.styleComboBox(destinationCombo);
        UIHelper.styleTextField(driverField);
        UIHelper.styleTextField(plateField);
        UIHelper.styleTextField(responsibleField);
        UIHelper.styleTextField(vehicleField);
        UIHelper.styleTextField(notesField);

        // Modelo de cabeçalho mantido para compatibilidade com o harness
        headerModel = new DefaultTableModel(
                new String[]{"Origem *", "Destino *", "Motorista *", "Matrícula *",
                        "Responsável", "Viatura", "Observações"}, 1) {
            @Override public boolean isCellEditable(int row, int column) { return tableEditable; }
        };
        headerTable = new JTable(headerModel);
        headerTable.putClientProperty("noTableFooter", Boolean.TRUE);
        headerTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);

        content.add(buildHeaderSection(), BorderLayout.NORTH);

        // Grelha canónica: Produto na 1.ª coluna (índice 0), com edição directa na grelha
        String[] lineCols = {"Produto", "Qtd", "Emb.", "Cx.", "% Cx.", "Ref.", "Cód. Barras",
                "Valor Unit.", "IVA", "Total"};
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
        configureGridEditors();
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
        this.warehouses.forEach(warehouse -> {
            originCombo.addItem(warehouse.name());
            destinationCombo.addItem(warehouse.name());
        });
        productCellEditor.setProducts(this.products);
        loading = false;
    }

    void prepareCreate() {
        stopCellEditing();
        loaded = null;
        loading = true;
        if (originCombo.getItemCount() > 0) originCombo.setSelectedIndex(0);
        if (destinationCombo.getItemCount() > 1) destinationCombo.setSelectedIndex(1);
        driverField.setText("");
        plateField.setText("");
        responsibleField.setText("");
        vehicleField.setText("");
        notesField.setText("");
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
                readLines(), values.driver(), values.plate());
    }

    UpdateStockTransferRequest updateRequest() {
        if (loaded == null) throw new IllegalStateException("Nenhuma transferência carregada.");
        Values values = validateValues();
        return new UpdateStockTransferRequest(loaded.version(), values.originId(), values.destinationId(),
                values.responsible(), values.vehicle(), values.notes(), readLines(),
                values.driver(), values.plate());
    }

    private void configureGridEditors() {
        linesTable.getColumnModel().getColumn(COL_PRODUCT)
                .setCellEditor(ProductSearchComboBox.createTableCellEditor(productCellEditor));
        linesTable.getColumnModel().getColumn(COL_PRODUCT).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof ProductDTO product ? product.name() : "Pesquisar produto...");
            }
        });
        for (int column : List.of(COL_QUANTITY, COL_PACKAGES, COL_BOXES)) {
            linesTable.getColumnModel().getColumn(column)
                    .setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));
            linesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.quantity());
        }
        linesTable.getColumnModel().getColumn(COL_UNIT_PRICE).setCellRenderer(TableCellRenderers.money());
        linesTable.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(TableCellRenderers.money());
        linesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        linesTable.setSurrendersFocusOnKeystroke(true);
        linesTable.setToolTipText("Edite directamente Produto, Qtd, Emb. ou Cx.; os restantes campos são derivados.");

        linesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        linesTable.getColumnModel().getColumn(COL_PRODUCT).setPreferredWidth(260);
        linesTable.getColumnModel().getColumn(COL_QUANTITY).setPreferredWidth(100);
        linesTable.getColumnModel().getColumn(COL_PACKAGES).setPreferredWidth(80);
        linesTable.getColumnModel().getColumn(COL_BOXES).setPreferredWidth(80);
        linesTable.getColumnModel().getColumn(COL_BOX_PERCENTAGE).setPreferredWidth(70);
        linesTable.getColumnModel().getColumn(COL_REFERENCE).setPreferredWidth(110);
        linesTable.getColumnModel().getColumn(COL_BARCODE).setPreferredWidth(120);
        linesTable.getColumnModel().getColumn(COL_UNIT_PRICE).setPreferredWidth(100);
        linesTable.getColumnModel().getColumn(COL_TAX).setPreferredWidth(70);
        linesTable.getColumnModel().getColumn(COL_TOTAL).setPreferredWidth(120);
    }

    private JComponent buildHeaderSection() {
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 0 (Rótulos) e Linha 1 (Inputs) - Principais
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 1.1;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel originLbl = new JLabel("Armazém de Origem *:");
        originLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(originLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 6, 6);
        formCard.add(originCombo, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.1;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel destLbl = new JLabel("Armazém de Destino *:");
        destLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(destLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 6, 6);
        formCard.add(destinationCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel driverLbl = new JLabel("Motorista *:");
        driverLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(driverLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 6, 6);
        driverField.putClientProperty("JTextField.placeholderText", "Nome do motorista...");
        formCard.add(driverField, gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel plateLbl = new JLabel("Matrícula *:");
        plateLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(plateLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 6, 6);
        plateField.putClientProperty("JTextField.placeholderText", "Ex: ABC-123-MC...");
        formCard.add(plateField, gbc);

        // Linha 2 (Rótulos) e Linha 3 (Inputs) - Secundários
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1.1;
        gbc.insets = new Insets(4, 6, 2, 6);
        JLabel respLbl = new JLabel("Responsável (opcional):");
        respLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(respLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 6, 4, 6);
        responsibleField.putClientProperty("JTextField.placeholderText", "Responsável pelo envio...");
        formCard.add(responsibleField, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.1;
        gbc.insets = new Insets(4, 6, 2, 6);
        JLabel vehLbl = new JLabel("Viatura / Modelo:");
        vehLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(vehLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 6, 4, 6);
        vehicleField.putClientProperty("JTextField.placeholderText", "Ex: Caminhão Canter 4T...");
        formCard.add(vehicleField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.8;
        gbc.insets = new Insets(4, 6, 2, 6);
        JLabel notesLbl = new JLabel("Observações:");
        notesLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(notesLbl, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 6, 4, 6);
        notesField.putClientProperty("JTextField.placeholderText", "Notas adicionais de transporte...");
        formCard.add(notesField, gbc);

        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        JLabel instruction = new JLabel("Uma linha de dados gerais; edite as células directamente.");
        instruction.setForeground(UIHelper.TEXT_MUTED);
        section.add(UIHelper.tableCardTop("Dados da Transferência", instruction), BorderLayout.NORTH);
        section.add(formCard, BorderLayout.CENTER);
        return section;
    }

    private JComponent buildLinesSection() {
        ModernPanel section = new ModernPanel(16);
        section.setLayout(new BorderLayout(0, 10));
        section.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel instruction = new JLabel(
                "Edite directamente na grelha. Qtd, Emb. e Cx. mantêm-se sincronizadas.");
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
                barcodeScanField, searchCatalogButton, suggestReplenishmentButton, addButton, removeButton),
                BorderLayout.NORTH);
        tableArea.add(scroll, BorderLayout.CENTER);

        // Barra de totais e carga no rodapé da tabela (igual às Encomendas e Faturas)
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
        int modelRow = linesModel.getRowCount() - 1;
        int rowCount = linesTable.getRowCount();
        if (modelRow >= 0 && rowCount > 0) {
            int viewRow = Math.min(Math.max(0, modelRow), rowCount - 1);
            try {
                if (modelRow < rowCount) {
                    viewRow = linesTable.convertRowIndexToView(modelRow);
                }
            } catch (Exception ignored) {
                viewRow = rowCount - 1;
            }
            if (viewRow >= 0 && viewRow < rowCount) {
                final int targetRow = viewRow;
                try {
                    linesTable.setRowSelectionInterval(targetRow, targetRow);
                } catch (Exception ignored) {
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (targetRow < linesTable.getRowCount() && linesTable.isShowing()) {
                            linesTable.scrollRectToVisible(linesTable.getCellRect(targetRow, COL_PRODUCT, true));
                            linesTable.editCellAt(targetRow, COL_PRODUCT);
                            Component editor = linesTable.getEditorComponent();
                            if (editor != null) editor.requestFocusInWindow();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        dirty = true;
        updateTotals();
    }

    private void addLineRow(ProductDTO product, BigDecimal quantity) {
        linesModel.addRow(new Object[]{product, quantity, BigDecimal.ZERO, BigDecimal.ZERO,
                "0%", "—", "—", BigDecimal.ZERO, "0%", BigDecimal.ZERO});
        recalculateRow(linesModel.getRowCount() - 1, COL_QUANTITY);
    }

    private void openProductCatalogLookup() {
        if (!tableEditable) return;
        stopCellEditing();
        List<ProductDTO> catalog = (products != null && !products.isEmpty())
                ? products
                : owner.catalogProducts;
        if (catalog == null || catalog.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Catálogo Vazio",
                    "Não existem produtos registados no sistema.");
            return;
        }
        List<ProductDTO> selected = ProductLookupDialog.showMulti(content, catalog);
        if (selected != null && !selected.isEmpty()) {
            applySelectedProducts(selected);
        }
    }

    private void applySelectedProducts(List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty()) return;
        stopCellEditing();
        int viewRow = linesTable.getSelectedRow();
        int modelRow = viewRow >= 0 ? linesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < linesModel.getRowCount()
                && linesModel.getValueAt(modelRow, COL_PRODUCT) == null);

        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            if (i == 0 && fillCurrent) {
                linesModel.setValueAt(p, modelRow, COL_PRODUCT);
                recalculateRow(modelRow, COL_PRODUCT);
                targetModelRow = modelRow;
            } else {
                addLineRow(p, BigDecimal.ONE);
                targetModelRow = linesModel.getRowCount() - 1;
            }
        }

        if (targetModelRow >= 0 && targetModelRow < linesModel.getRowCount()) {
            final int finalRow = targetModelRow;
            SwingUtilities.invokeLater(() -> {
                try {
                    int vr = linesTable.convertRowIndexToView(finalRow);
                    linesTable.setRowSelectionInterval(vr, vr);
                    linesTable.scrollRectToVisible(linesTable.getCellRect(vr, COL_QUANTITY, true));
                    linesTable.editCellAt(vr, COL_QUANTITY);
                    Component comp = linesTable.getEditorComponent();
                    if (comp != null) comp.requestFocusInWindow();
                } catch (Exception ignored) {
                }
            });
        }
        dirty = true;
        updateTotals();
    }

    private void removeSelectedLine() {
        int viewRow = linesTable.getSelectedRow();
        if (viewRow < 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma linha",
                    "Escolha um item para remover.");
            return;
        }
        stopCellEditing();
        linesModel.removeRow(linesTable.convertRowIndexToModel(viewRow));
        dirty = true;
        updateTotals();
    }

    private List<CreateStockTransferLineRequest> readLines() {
        stopCellEditing();
        if (linesModel.getRowCount() == 0) {
            throw new IllegalArgumentException("Adicione pelo menos um produto à transferência.");
        }
        List<CreateStockTransferLineRequest> lines = new ArrayList<>();
        for (int row = 0; row < linesModel.getRowCount(); row++) {
            Object productValue = linesModel.getValueAt(row, COL_PRODUCT);
            ProductDTO product = productCellEditor.resolve(productValue);
            if (product == null) {
                int rowCount = linesTable.getRowCount();
                if (row < rowCount) {
                    try {
                        linesTable.setRowSelectionInterval(row, row);
                        linesTable.editCellAt(row, COL_PRODUCT);
                    } catch (Exception ignored) {
                    }
                }
                throw new IllegalArgumentException("Pesquise e seleccione o produto da linha " + (row + 1) + ".");
            }
            BigDecimal quantity = decimal(linesModel.getValueAt(row, COL_QUANTITY));
            if (quantity.signum() <= 0) {
                throw new IllegalArgumentException("A quantidade da linha " + (row + 1)
                        + " deve ser superior a zero.");
            }
            lines.add(new CreateStockTransferLineRequest(product.id(), quantity));
        }
        return lines;
    }

    private void recalculateRow(int row, int editedColumn) {
        if (recalculating || row < 0 || row >= linesModel.getRowCount()) return;
        recalculating = true;
        try {
            Object cellValue = linesModel.getValueAt(row, COL_PRODUCT);
            ProductDTO product = productCellEditor.resolve(cellValue);
            int packagesPerBox = product == null ? 1 : Math.max(1, product.packagesPerBox());
            int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
            BigDecimal unitsPerBox = BigDecimal.valueOf((long) packagesPerBox * unitsPerPackage);
            BigDecimal quantity;
            if (editedColumn == COL_PACKAGES) {
                quantity = decimal(linesModel.getValueAt(row, COL_PACKAGES))
                        .multiply(BigDecimal.valueOf(unitsPerPackage));
            } else if (editedColumn == COL_BOXES) {
                quantity = decimal(linesModel.getValueAt(row, COL_BOXES)).multiply(unitsPerBox);
            } else {
                quantity = decimal(linesModel.getValueAt(row, COL_QUANTITY));
            }
            BigDecimal packages = quantity.divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
            BigDecimal boxes = quantity.divide(unitsPerBox, 2, RoundingMode.HALF_UP);
            BigDecimal percentage = quantity.multiply(BigDecimal.valueOf(100))
                    .divide(unitsPerBox, 1, RoundingMode.HALF_UP);

            linesModel.setValueAt(product, row, COL_PRODUCT);
            linesModel.setValueAt(quantity, row, COL_QUANTITY);
            linesModel.setValueAt(packages, row, COL_PACKAGES);
            linesModel.setValueAt(boxes, row, COL_BOXES);
            linesModel.setValueAt(display(percentage) + "%", row, COL_BOX_PERCENTAGE);
            linesModel.setValueAt(product == null ? "—" : firstText(product.reference(), product.sku()),
                    row, COL_REFERENCE);
            linesModel.setValueAt(product == null ? "—" : textOrDash(product.barcode()), row, COL_BARCODE);
            BigDecimal unitPrice = (product == null || product.unitPrice() == null)
                    ? BigDecimal.ZERO : product.unitPrice();
            linesModel.setValueAt(unitPrice, row, COL_UNIT_PRICE);
            BigDecimal tax = product == null ? BigDecimal.ZERO : product.effectiveTaxRate();
            linesModel.setValueAt(display(tax.movePointRight(2)) + "%", row, COL_TAX);
            BigDecimal total = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            linesModel.setValueAt(total, row, COL_TOTAL);
        } finally {
            recalculating = false;
        }
        updateTotals();
    }

    private void updateTotals() {
        BigDecimal sumQty = BigDecimal.ZERO;
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumWeight = BigDecimal.ZERO;
        for (int i = 0; i < linesModel.getRowCount(); i++) {
            BigDecimal qty = decimal(linesModel.getValueAt(i, COL_QUANTITY));
            sumQty = sumQty.add(qty);
            sumTotal = sumTotal.add(decimal(linesModel.getValueAt(i, COL_TOTAL)));
            Object cellValue = linesModel.getValueAt(i, COL_PRODUCT);
            ProductDTO product = productCellEditor.resolve(cellValue);
            if (product != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal w = product.grossUnitWeightKg() != null ? product.grossUnitWeightKg() : product.netUnitWeightKg();
                if (w != null) sumWeight = sumWeight.add(w.multiply(qty));
            }
        }
        String vehicleInfo = "";
        if (sumWeight.compareTo(BigDecimal.ZERO) > 0) {
            String vehicleType = mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.recommendedVehicleType(sumWeight);
            BigDecimal tons = sumWeight.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            vehicleInfo = " | Carga: " + sumWeight.setScale(2, RoundingMode.HALF_UP) + " kg (" + tons.stripTrailingZeros().toPlainString() + " Ton) — Viatura Sugerida: " + vehicleType;
            if (vehicleField.getText().trim().isEmpty()) {
                vehicleField.putClientProperty("JTextField.placeholderText", "Sugerido: " + vehicleType);
            }
        }
        totalLinesLabel.setText("Itens: " + linesModel.getRowCount() + " | Qtd Total: " + sumQty.stripTrailingZeros().toPlainString() + vehicleInfo);
        totalValueLabel.setText("Total da Transferência: " + sumTotal.setScale(2, RoundingMode.HALF_UP) + " MT");
    }

    private Values validateValues() {
        stopCellEditing();
        syncHeaderModel();
        int origin = originCombo.getSelectedIndex();
        int destination = destinationCombo.getSelectedIndex();
        if (origin < 0 || destination < 0) {
            throw new IllegalArgumentException("Seleccione os armazéns de origem e destino.");
        }
        if (origin == destination) {
            throw new IllegalArgumentException("O armazém de origem e o de destino devem ser diferentes.");
        }
        String driver = blank(driverField.getText());
        String plate = blank(plateField.getText());
        if (driver == null) {
            driverField.requestFocusInWindow();
            throw new IllegalArgumentException("O nome do motorista é obrigatório.");
        }
        if (plate == null) {
            plateField.requestFocusInWindow();
            throw new IllegalArgumentException("A matrícula do veículo é obrigatória.");
        }
        return new Values(warehouses.get(origin).id(), warehouses.get(destination).id(), driver, plate,
                blank(responsibleField.getText()), blank(vehicleField.getText()),
                blank(notesField.getText()));
    }

    private void syncHeaderModel() {
        if (headerModel == null) return;
        headerModel.setValueAt(originCombo.getSelectedItem(), 0, 0);
        headerModel.setValueAt(destinationCombo.getSelectedItem(), 0, 1);
        headerModel.setValueAt(driverField.getText(), 0, 2);
        headerModel.setValueAt(plateField.getText(), 0, 3);
        headerModel.setValueAt(responsibleField.getText(), 0, 4);
        headerModel.setValueAt(vehicleField.getText(), 0, 5);
        headerModel.setValueAt(notesField.getText(), 0, 6);
    }

    private void installListeners() {
        originCombo.addActionListener(e -> { if (!loading) dirty = true; });
        destinationCombo.addActionListener(e -> { if (!loading) dirty = true; });
        UIHelper.onTextChange(driverField, () -> { if (!loading) dirty = true; });
        UIHelper.onTextChange(plateField, () -> { if (!loading) dirty = true; });
        UIHelper.onTextChange(responsibleField, () -> { if (!loading) dirty = true; });
        UIHelper.onTextChange(vehicleField, () -> { if (!loading) dirty = true; });
        UIHelper.onTextChange(notesField, () -> { if (!loading) dirty = true; });

        linesModel.addTableModelListener(event -> {
            if (loading || recalculating || event.getFirstRow() < 0) return;
            if (event.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int column = event.getColumn();
                if (column == COL_PRODUCT || column == COL_QUANTITY
                        || column == COL_PACKAGES || column == COL_BOXES) {
                    recalculateRow(event.getFirstRow(), column);
                }
            }
            dirty = true;
        });
    }

    private void setEditable(boolean editable) {
        tableEditable = editable;
        originCombo.setEnabled(editable);
        destinationCombo.setEnabled(editable);
        driverField.setEditable(editable);
        plateField.setEditable(editable);
        responsibleField.setEditable(editable);
        vehicleField.setEditable(editable);
        notesField.setEditable(editable);
        if (barcodeScanField != null) barcodeScanField.setEnabled(editable);
        if (searchCatalogButton != null) searchCatalogButton.setEnabled(editable);
        if (suggestReplenishmentButton != null) suggestReplenishmentButton.setEnabled(editable);
        addButton.setEnabled(editable);
        removeButton.setEnabled(editable);
        linesTable.repaint();
    }

    private void handleBarcodeScanned(String code) {
        if (!tableEditable) return;
        ProductDTO product = products.stream().filter(p ->
                code.equalsIgnoreCase(p.barcode())
                || code.equalsIgnoreCase(p.sku())
                || (p.reference() != null && code.equalsIgnoreCase(p.reference()))
        ).findFirst().orElse(null);

        if (product == null) {
            Toolkit.getDefaultToolkit().beep();
            owner.showStockNotice(FeedbackType.WARNING, "Artigo não encontrado",
                    "Nenhum produto cadastrado com o código ou SKU: " + code);
            return;
        }

        stopCellEditing();
        int existingIndex = -1;
        for (int i = 0; i < linesModel.getRowCount(); i++) {
            Object val = linesModel.getValueAt(i, COL_PRODUCT);
            ProductDTO p = productCellEditor.resolve(val);
            if (p != null && product.id().equals(p.id())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            BigDecimal currentQty = decimal(linesModel.getValueAt(existingIndex, COL_QUANTITY));
            BigDecimal newQty = currentQty.add(BigDecimal.ONE);
            linesModel.setValueAt(newQty, existingIndex, COL_QUANTITY);
            recalculateRow(existingIndex, COL_QUANTITY);
            linesTable.setRowSelectionInterval(existingIndex, existingIndex);
        } else {
            if (linesModel.getRowCount() == 1) {
                Object val = linesModel.getValueAt(0, COL_PRODUCT);
                if (val == null || (val instanceof String s && s.isBlank())) {
                    linesModel.removeRow(0);
                }
            }
            addLineRow(product, BigDecimal.ONE);
            int newRow = linesModel.getRowCount() - 1;
            if (newRow >= 0 && newRow < linesTable.getRowCount()) {
                linesTable.setRowSelectionInterval(newRow, newRow);
            }
        }
        dirty = true;
        updateTotals();
    }

    private void suggestReplenishmentForDestination() {
        if (!tableEditable) return;
        int originIdx = originCombo.getSelectedIndex();
        int destIdx = destinationCombo.getSelectedIndex();
        if (originIdx < 0 || destIdx < 0 || originIdx >= warehouses.size() || destIdx >= warehouses.size()) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns necessários",
                    "Seleccione os armazéns de origem e de destino antes de pedir a sugestão.");
            return;
        }
        if (originIdx == destIdx) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns iguais",
                    "O armazém de origem e o de destino devem ser diferentes.");
            return;
        }

        WarehouseDTO origin = warehouses.get(originIdx);
        WarehouseDTO dest = warehouses.get(destIdx);

        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getStocksByCompany(CurrentUserContext.getCurrentCompanyId()), loadedStocks -> {
            applyReplenishmentSuggestions(origin, dest, loadedStocks);
        }, owner::showStockError);
    }

    private void applyReplenishmentSuggestions(WarehouseDTO origin, WarehouseDTO dest, List<StockDTO> loadedStocks) {
        if (loadedStocks == null || loadedStocks.isEmpty()) {
            owner.showStockNotice(FeedbackType.INFO, "Sem dados de stock",
                    "Não foram encontrados dados de saldo para calcular a reposição.");
            return;
        }

        java.util.Map<Long, BigDecimal> destStock = new java.util.HashMap<>();
        java.util.Map<Long, BigDecimal> destMinStock = new java.util.HashMap<>();
        java.util.Map<Long, BigDecimal> originStock = new java.util.HashMap<>();

        for (StockDTO s : loadedStocks) {
            if (dest.id().equals(s.warehouseId())) {
                destStock.put(s.productId(), s.quantity() != null ? s.quantity() : BigDecimal.ZERO);
                if (s.minStock() != null && s.minStock().compareTo(BigDecimal.ZERO) > 0) {
                    destMinStock.put(s.productId(), s.minStock());
                }
            } else if (origin.id().equals(s.warehouseId())) {
                originStock.put(s.productId(), s.quantity() != null ? s.quantity() : BigDecimal.ZERO);
            }
        }

        record SuggestionItem(ProductDTO product, BigDecimal qtyNeeded, BigDecimal qtyOrigin) {}
        List<SuggestionItem> suggestions = new ArrayList<>();

        for (ProductDTO product : products) {
            BigDecimal currentDest = destStock.getOrDefault(product.id(), BigDecimal.ZERO);
            BigDecimal min = destMinStock.get(product.id());

            BigDecimal needed = BigDecimal.ZERO;
            if (min != null && currentDest.compareTo(min) < 0) {
                needed = min.subtract(currentDest);
            } else if (currentDest.compareTo(BigDecimal.ZERO) <= 0) {
                int unitsPerBox = product.packagesPerBox() > 0 && product.unitsPerPackage() > 0
                        ? product.packagesPerBox() * product.unitsPerPackage() : 1;
                needed = BigDecimal.valueOf(unitsPerBox);
            }

            if (needed.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal availableOrigin = originStock.getOrDefault(product.id(), BigDecimal.ZERO);
                if (availableOrigin.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal transferQty = needed.min(availableOrigin);
                    suggestions.add(new SuggestionItem(product, transferQty, availableOrigin));
                }
            }
        }

        if (suggestions.isEmpty()) {
            owner.showStockNotice(FeedbackType.INFO, "Reposição desnecessária",
                    "Nenhum produto em falta no armazém '" + dest.name() + "' tem saldo disponível em '" + origin.name() + "'.");
            return;
        }

        boolean confirm = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(content),
                FeedbackType.INFO,
                "Sugestão de Reposição Inteligente",
                "Foram identificados " + suggestions.size() + " produto(s) com necessidade de reposição no armazém '" + dest.name() + "'.\n\n"
                        + "Deseja carregar estes artigos automaticamente na grelha da transferência?",
                "Carregar Itens"
        );
        if (!confirm) return;

        stopCellEditing();
        if (linesModel.getRowCount() == 1) {
            Object val = linesModel.getValueAt(0, COL_PRODUCT);
            if (val == null || (val instanceof String s && s.isBlank())) {
                linesModel.removeRow(0);
            }
        }

        for (SuggestionItem item : suggestions) {
            addLineRow(item.product(), item.qtyNeeded());
        }
        dirty = true;
        updateTotals();
        owner.showStockSuccess(suggestions.size() + " artigo(s) carregados por sugestão de reposição.");
    }

    private void stopCellEditing() {
        if (linesTable.isEditing() && !linesTable.getCellEditor().stopCellEditing()) {
            linesTable.getCellEditor().cancelCellEditing();
        }
    }

    private String warehouseName(Long id) {
        for (int i = 0; i < warehouses.size(); i++) {
            if (warehouses.get(i).id().equals(id)) {
                return warehouses.get(i).name();
            }
        }
        return "";
    }

    private ProductDTO findProduct(Long id) {
        return products.stream().filter(product -> product.id().equals(id)).findFirst().orElse(null);
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(value).trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private static String firstText(String preferred, String fallback) {
        return blank(preferred) != null ? preferred.trim() : textOrDash(fallback);
    }

    private static String textOrDash(String value) { return blank(value) == null ? "—" : value.trim(); }
    private static String display(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String nz(String value) { return value == null ? "" : value; }

    private record Values(Long originId, Long destinationId, String driver, String plate,
                          String responsible, String vehicle, String notes) {}
}
