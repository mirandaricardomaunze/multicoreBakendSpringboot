package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
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
import mz.multicore.erp.modules.comercial.model.QuotationValidity;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Formulário embebido reutilizado para criar e editar cotações. */
final class QuotationEditorForm {

    private final QuotationsPanel owner;
    private final JPanel content = new JPanel(new BorderLayout(0, 12));
    private List<ClientDTO> clients = List.of();
    private List<ProductDTO> products = List.of();
    private List<WarehouseDTO> warehouses = List.of();
    private final List<CreateQuotationLineRequest> draftLines = new ArrayList<>();
    private final List<BigDecimal> previewPrices = new ArrayList<>();

    private JComboBox<String> clientCombo;
    private JTextField walkInField;
    private JComboBox<String> warehouseCombo;
    private QuantityField validityField;
    private JTextField paymentTermsField;
    private JTextField deliveryTermsField;
    private JTextField deliveryDaysField;
    private JTextField notesField;
    private ProductSearchComboBox productCombo;
    private QuantityField quantityField;
    private DecimalField discountField;
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

    QuotationEditorForm(QuotationsPanel owner) {
        this.owner = owner;
        content.setOpaque(false);
        content.add(buildHeaderForm(), BorderLayout.NORTH);
        content.add(buildLinesSection(), BorderLayout.CENTER);
        installDirtyTracking();
    }

    JComponent component() { return content; }
    boolean isDirty() { return dirty; }
    QuotationDTO loaded() { return loaded; }

    void setOptions(List<ClientDTO> clients, List<ProductDTO> products, List<WarehouseDTO> warehouses) {
        this.clients = clients == null ? List.of() : clients;
        this.products = products == null ? List.of() : products;
        this.warehouses = warehouses == null ? List.of() : warehouses;
        loading = true;
        clientCombo.removeAllItems();
        clientCombo.addItem("Consumidor Final (sem registo)");
        this.clients.forEach(c -> clientCombo.addItem(c.name() + " (" + c.taxId() + ")"));
        warehouseCombo.removeAllItems();
        this.warehouses.forEach(w -> warehouseCombo.addItem(w.name()));
        productCombo.setProducts(this.products);
        loading = false;
    }

    void prepareCreate() {
        loaded = null;
        loading = true;
        if (clientCombo.getItemCount() > 0) clientCombo.setSelectedIndex(0);
        if (warehouseCombo.getItemCount() > 0) warehouseCombo.setSelectedIndex(0);
        walkInField.setText("");
        validityField.setText(String.valueOf(QuotationValidity.DEFAULT_DAYS));
        paymentTermsField.setText("");
        deliveryTermsField.setText("");
        deliveryDaysField.setText("");
        notesField.setText("");
        draftLines.clear();
        previewPrices.clear();
        linesModel.setRowCount(0);
        loading = false;
        setEditable(true);
        dirty = false;
        refreshTotals();
    }

    void load(QuotationDTO quotation) {
        loaded = quotation;
        loading = true;
        selectClient(quotation);
        selectWarehouse(quotation.warehouseId());
        walkInField.setText(nz(quotation.walkInName()));
        long days = quotation.validUntil() == null ? QuotationValidity.DEFAULT_DAYS
                : Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), quotation.validUntil()));
        validityField.setText(String.valueOf(days));
        paymentTermsField.setText(nz(quotation.paymentTerms()));
        deliveryTermsField.setText(nz(quotation.deliveryTerms()));
        deliveryDaysField.setText(quotation.deliveryDays() == null ? "" : quotation.deliveryDays().toString());
        notesField.setText(nz(quotation.notes()));
        draftLines.clear();
        previewPrices.clear();
        quotation.lines().forEach(line -> {
            draftLines.add(new CreateQuotationLineRequest(
                    line.productId(), line.quantity(), line.discountPercentage()));
            previewPrices.add(line.unitPrice());
        });
        loading = false;
        setEditable("DRAFT".equals(quotation.status()));
        dirty = false;
        refreshRows();
    }

    CreateQuotationRequest createRequest() {
        HeaderValues values = validateHeader();
        return new CreateQuotationRequest(values.clientId(), values.walkInName(),
                CurrentUserContext.getCurrentCompanyId(), values.warehouseId(), values.validityDays(),
                values.paymentTerms(), values.deliveryTerms(), values.deliveryDays(), values.notes(),
                new ArrayList<>(draftLines));
    }

    UpdateQuotationRequest updateRequest() {
        if (loaded == null) throw new IllegalStateException("Nenhuma cotação carregada.");
        HeaderValues values = validateHeader();
        return new UpdateQuotationRequest(loaded.version(), values.clientId(), values.walkInName(),
                values.warehouseId(), values.validityDays(), values.paymentTerms(),
                values.deliveryTerms(), values.deliveryDays(), values.notes(), new ArrayList<>(draftLines));
    }

    void markClean() { dirty = false; }

    private JPanel buildHeaderForm() {
        clientCombo = new JComboBox<>(); UIHelper.styleComboBox(clientCombo);
        walkInField = new JTextField(); UIHelper.styleTextField(walkInField);
        walkInField.putClientProperty("JTextField.placeholderText", "Nome do comprador, se não for cliente registado");
        warehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(warehouseCombo);
        validityField = new QuantityField(String.valueOf(QuotationValidity.DEFAULT_DAYS), true);
        paymentTermsField = new JTextField(); UIHelper.styleTextField(paymentTermsField);
        paymentTermsField.putClientProperty("JTextField.placeholderText", "Ex.: Pronto pagamento");
        deliveryTermsField = new JTextField(); UIHelper.styleTextField(deliveryTermsField);
        deliveryTermsField.putClientProperty("JTextField.placeholderText", "Ex.: Entrega imediata");
        deliveryDaysField = new JTextField(); UIHelper.styleTextField(deliveryDaysField);
        deliveryDaysField.putClientProperty("JTextField.placeholderText", "Dias");
        notesField = new JTextField(); UIHelper.styleTextField(notesField);
        notesField.putClientProperty("JTextField.placeholderText", "Observações gerais da cotação...");

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(fieldGroup("Cliente:", clientCombo, 260));
        row1.add(fieldGroup("Comprador (opcional):", walkInField, 220));
        row1.add(fieldGroup("Armazém:", warehouseCombo, 180));
        row1.add(fieldGroup("Validade (dias):", validityField, 110));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.add(fieldGroup("Condições de pagamento:", paymentTermsField, 190));
        row2.add(fieldGroup("Prazo de entrega:", deliveryTermsField, 160));
        row2.add(fieldGroup("Dias:", deliveryDaysField, 80));
        row2.add(fieldGroup("Observações:", notesField, 320));

        card.add(row1);
        card.add(Box.createVerticalStrut(6));
        card.add(row2);
        return card;
    }

    private static JPanel fieldGroup(String label, JComponent comp, int width) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        group.add(lbl, BorderLayout.NORTH);
        if (width > 0) {
            comp.setPreferredSize(new Dimension(width, 36));
        }
        group.add(comp, BorderLayout.CENTER);
        return group;
    }

    private JComponent buildLinesSection() {
        ModernPanel section = new ModernPanel(16);
        section.setLayout(new BorderLayout(0, 10));
        section.setBorder(new EmptyBorder(14, 16, 14, 16));

        productCombo = new ProductSearchComboBox();
        quantityField = new QuantityField("1", true);
        discountField = new DecimalField("0", 2, false);
        addButton = UIHelper.createPrimaryButton("Adicionar linha");
        addButton.setIcon(UIHelper.icon("fas-plus", 14));
        addButton.addActionListener(e -> addBlankLine());
        removeButton = UIHelper.createDangerButton("Remover item");
        removeButton.setIcon(UIHelper.icon("fas-trash", 14));
        removeButton.addActionListener(e -> removeSelectedLine());

        linesModel = new DefaultTableModel(
                new String[]{"Produto", "Qtd", "Emb.", "Cx.", "% Cx.", "Preço Unit.", "Desc. %",
                        "Total (c/ IVA)"}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return tableEditable && (column <= 3 || column == 6);
            }
        };
        linesTable = new JTable(linesModel); UIHelper.styleTable(linesTable);
        linesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        linesTable.putClientProperty("noTableFooter", Boolean.TRUE);
        UIHelper.installDocumentGridShortcuts(linesTable, this::addBlankLine, this::removeSelectedLine, owner::saveEditor);
        linesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(productCombo));
        linesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof ProductDTO product ? product.name() : "Pesquisar produto...");
            }
        });
        for (int column : List.of(1, 2, 3)) {
            linesTable.getColumnModel().getColumn(column)
                    .setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));
            linesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.quantity());
        }
        linesTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(discountField));
        linesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        linesTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        linesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        linesTable.setSurrendersFocusOnKeystroke(true);
        linesTable.setToolTipText("Edite Produto, Qtd, Emb., Cx. e Desc. directamente nas células.");
        linesModel.addTableModelListener(event -> {
            if (loading || syncingLines || event.getFirstRow() < 0
                    || event.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            syncLineFromGrid(event.getFirstRow(), event.getColumn());
        });
        JScrollPane scroll = new JScrollPane(linesTable); UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(900, 260));

        JPanel tableArea = new JPanel(new BorderLayout(0, 8));
        tableArea.setOpaque(false);
        JLabel instruction = new JLabel("Edite directamente na grelha; embalagens e caixas mantêm-se sincronizadas.");
        instruction.setForeground(UIHelper.TEXT_MUTED);
        tableArea.add(UIHelper.tableCardTop("Itens da Cotação", instruction, addButton, removeButton), BorderLayout.NORTH);
        tableArea.add(scroll, BorderLayout.CENTER);
        totalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        JPanel totalRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        totalRow.setOpaque(false); totalRow.add(totalLabel);
        tableArea.add(totalRow, BorderLayout.SOUTH);

        section.add(tableArea, BorderLayout.CENTER);
        return section;
    }

    private void addBlankLine() {
        if (!tableEditable) return;
        stopCellEditing();
        draftLines.add(new CreateQuotationLineRequest(0L, BigDecimal.ONE, BigDecimal.ZERO));
        previewPrices.add(BigDecimal.ZERO);
        refreshRows();
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
                    linesTable.scrollRectToVisible(linesTable.getCellRect(targetRow, 0, true));
                } catch (Exception ignored) {
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (targetRow < linesTable.getRowCount()) {
                            linesTable.editCellAt(targetRow, 0);
                            if (linesTable.getEditorComponent() != null) linesTable.getEditorComponent().requestFocusInWindow();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        dirty = true;
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
        linesModel.setRowCount(0);
        for (int i = 0; i < draftLines.size(); i++) {
            CreateQuotationLineRequest line = draftLines.get(i);
            ProductDTO product = findProduct(line.productId());
            BigDecimal price = previewPrices.get(i) == null ? BigDecimal.ZERO : previewPrices.get(i);
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            BigDecimal net = price.multiply(line.quantity())
                    .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
            BigDecimal tax = product == null ? BigDecimal.ZERO : product.effectiveTaxRate();
            BigDecimal total = net.multiply(BigDecimal.ONE.add(tax)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal packages = line.quantity().divide(BigDecimal.valueOf(product == null ? 1
                    : Math.max(1, product.unitsPerPackage())), 2, RoundingMode.HALF_UP);
            BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                    : (long) Math.max(1, product.packagesPerBox()) * Math.max(1, product.unitsPerPackage()));
            BigDecimal boxes = line.quantity().divide(unitsPerBox, 2, RoundingMode.HALF_UP);
            BigDecimal percentage = line.quantity().multiply(BigDecimal.valueOf(100))
                    .divide(unitsPerBox, 1, RoundingMode.HALF_UP);
            linesModel.addRow(new Object[]{product, line.quantity(), packages, boxes,
                    percentage.stripTrailingZeros().toPlainString() + "%", price, discount, total});
        }
        syncingLines = false;
        refreshTotals();
    }

    private void syncLineFromGrid(int row, int column) {
        if (row >= draftLines.size()) return;
        ProductDTO product = linesModel.getValueAt(row, 0) instanceof ProductDTO selected ? selected : null;
        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal quantity;
        if (column == 2) quantity = decimal(linesModel.getValueAt(row, 2)).multiply(BigDecimal.valueOf(unitsPerPackage));
        else if (column == 3) quantity = decimal(linesModel.getValueAt(row, 3)).multiply(unitsPerBox);
        else quantity = decimal(linesModel.getValueAt(row, 1));
        BigDecimal discount = decimal(linesModel.getValueAt(row, 6));
        draftLines.set(row, new CreateQuotationLineRequest(product == null ? 0L : product.id(), quantity, discount));
        previewPrices.set(row, product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice());
        dirty = true;
        refreshRows();
    }

    private void refreshTotals() {
        BigDecimal total = BigDecimal.ZERO;
        for (int row = 0; row < linesModel.getRowCount(); row++) {
            total = total.add(decimal(linesModel.getValueAt(row, 7)));
        }
        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", total));
    }

    private HeaderValues validateHeader() {
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
        int wh = warehouseCombo.getSelectedIndex();
        if (wh < 0 || wh >= warehouses.size()) throw new RuntimeException("Selecione o armazém.");
        int validity;
        try {
            validity = validityField.value().intValueExact();
            if (validity <= 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            throw new RuntimeException("A validade deve ser um número inteiro superior a zero.");
        }
        Integer deliveryDays = null;
        if (!deliveryDaysField.getText().isBlank()) {
            try { deliveryDays = Integer.valueOf(deliveryDaysField.getText().trim()); }
            catch (NumberFormatException e) { throw new RuntimeException("Os dias de entrega devem ser um número inteiro."); }
            if (deliveryDays <= 0) throw new RuntimeException("O prazo de entrega deve ser de pelo menos um dia.");
        }
        int client = clientCombo.getSelectedIndex();
        Long clientId = client > 0 && client <= clients.size() ? clients.get(client - 1).id() : null;
        return new HeaderValues(clientId, blank(walkInField.getText()), warehouses.get(wh).id(), validity,
                blank(paymentTermsField.getText()), blank(deliveryTermsField.getText()), deliveryDays,
                blank(notesField.getText()));
    }

    private void selectClient(QuotationDTO quotation) {
        int selected = 0;
        if (quotation.walkInName() == null || quotation.walkInName().isBlank()) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).id().equals(quotation.clientId())) { selected = i + 1; break; }
            }
        }
        if (clientCombo.getItemCount() > selected) clientCombo.setSelectedIndex(selected);
    }

    private void selectWarehouse(Long id) {
        for (int i = 0; i < warehouses.size(); i++) {
            if (warehouses.get(i).id().equals(id)) { warehouseCombo.setSelectedIndex(i); return; }
        }
    }

    private ProductDTO findProduct(Long id) {
        return products.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    }

    private void setEditable(boolean editable) {
        tableEditable = editable;
        clientCombo.setEnabled(editable); walkInField.setEnabled(editable); warehouseCombo.setEnabled(editable);
        validityField.setEnabled(editable); paymentTermsField.setEnabled(editable);
        deliveryTermsField.setEnabled(editable); deliveryDaysField.setEnabled(editable); notesField.setEnabled(editable);
        productCombo.setEnabled(editable); quantityField.setEnabled(editable); discountField.setEnabled(editable);
        addButton.setEnabled(editable); removeButton.setEnabled(editable); linesTable.repaint();
    }

    private void installDirtyTracking() {
        DocumentListener listener = new DocumentListener() {
            private void changed() { if (!loading) dirty = true; }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        };
        for (javax.swing.text.JTextComponent field : List.of(walkInField, validityField,
                paymentTermsField, deliveryTermsField, deliveryDaysField, notesField)) {
            field.getDocument().addDocumentListener(listener);
        }
        clientCombo.addActionListener(e -> { if (!loading) dirty = true; });
        warehouseCombo.addActionListener(e -> { if (!loading) dirty = true; });
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String nz(String value) { return value == null ? "" : value; }
    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value).trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private void stopCellEditing() {
        if (linesTable.isEditing() && !linesTable.getCellEditor().stopCellEditing()) {
            linesTable.getCellEditor().cancelCellEditing();
        }
    }

    private record HeaderValues(Long clientId, String walkInName, Long warehouseId, int validityDays,
                                String paymentTerms, String deliveryTerms, Integer deliveryDays, String notes) {}
}
