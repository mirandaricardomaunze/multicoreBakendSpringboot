package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inspector corporativo universal, DRY e só-leitura para qualquer linha de tabela do Multicore ERP.
 */
public final class RecordDetailsDialog {

    public record PropertyItem(String label, String value) {}

    private static final AtomicBoolean OPENING = new AtomicBoolean(false);

    private RecordDetailsDialog() {}

    public static ProductDTO findProduct(JTable table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) return null;
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (table.getValueAt(row, col) instanceof ProductDTO p) return p;
        }
        return null;
    }

    public static ClientDTO findClient(JTable table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) return null;
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (table.getValueAt(row, col) instanceof ClientDTO c) return c;
        }
        return null;
    }

    public static OrderDTO findOrder(JTable table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) return null;
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (table.getValueAt(row, col) instanceof OrderDTO o) return o;
        }
        return null;
    }

    public static void show(JTable table) {
        if (table == null) return;
        if (table.getSelectedRow() < 0) {
            ToastManager.show(table, FeedbackType.INFO, "Seleccione uma linha na tabela para inspecionar os detalhes.");
            return;
        }
        if (!OPENING.compareAndSet(false, true)) return;
        try {
            showInternal(table);
        } finally {
            SwingUtilities.invokeLater(() -> OPENING.set(false));
        }
    }

    private static void showInternal(JTable table) {
        int selectedRow = table.getSelectedRow();
        String mainTitle = "Detalhes do Registo";
        String mainSubtitle = "Inspeção de dados da grelha (Linha #" + (selectedRow + 1) + " de " + table.getRowCount() + ")";

        ProductDTO product = findProduct(table, selectedRow);
        ClientDTO client = findClient(table, selectedRow);
        OrderDTO order = findOrder(table, selectedRow);

        if (order != null) {
            mainTitle = "Encomenda " + order.orderNumber();
            mainSubtitle = "Cliente: " + order.clientName() + (order.createdAt() != null ? " • " + order.createdAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        } else if (product != null) {
            mainTitle = product.name() != null && !product.name().isBlank() ? product.name() : "Artigo #" + product.id();
            String refPart = product.reference() != null && !product.reference().isBlank() ? "Ref: " + product.reference() + " • " : "";
            String skuPart = product.sku() != null && !product.sku().isBlank() ? "SKU: " + product.sku() : "";
            String eanPart = product.barcode() != null && !product.barcode().isBlank() ? " • EAN: " + product.barcode() : "";
            mainSubtitle = refPart + skuPart + eanPart;
            if (mainSubtitle.isBlank()) mainSubtitle = "Ficha cadastral do artigo";
        } else if (client != null) {
            mainTitle = client.name() != null && !client.name().isBlank() ? client.name() : "Cliente #" + client.id();
            mainSubtitle = "NUIT: " + (client.taxId() != null ? client.taxId() : "—") + (client.code() != null ? " • Cód: " + client.code() : "");
        } else if (table.getColumnCount() > 0) {
            String text0 = valueText(table.getValueAt(selectedRow, 0));
            if (!text0.isBlank()) mainTitle = text0;
            if (table.getColumnCount() > 1) {
                String text1 = valueText(table.getValueAt(selectedRow, 1));
                if (!text1.isBlank()) {
                    if ("Detalhes do Registo".equals(mainTitle)) mainTitle = text1;
                    else mainSubtitle = text1 + " • Linha #" + (selectedRow + 1);
                }
            }
        }

        RecordDetailsPropertyExtractor.HeaderTheme theme = RecordDetailsPropertyExtractor.resolveHeaderTheme(table);
        if (order != null) theme = new RecordDetailsPropertyExtractor.HeaderTheme("fas-shopping-bag", UIHelper.ACCENT_BLUE);
        else if (product != null) theme = new RecordDetailsPropertyExtractor.HeaderTheme("fas-box-open", UIHelper.APPROVED_GREEN);
        else if (client != null) theme = new RecordDetailsPropertyExtractor.HeaderTheme("fas-user-tie", UIHelper.ACCENT_BLUE);

        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(table, "Ficha de Detalhes — " + mainTitle);
        dialog.setTitle(mainTitle).setSubtitle(mainSubtitle).setHeaderIcon(theme.icon(), 28, theme.accent());

        RecordDetailsPropertyExtractor.configureDialogKpis(dialog, table, selectedRow, order, product);

        List<PropertyItem> properties = RecordDetailsPropertyExtractor.extractProperties(table, selectedRow, product, client, order);
        dialog.addTab("Propriedades do Registo", "fas-id-card", UIHelper.ACCENT_BLUE, buildPropertySheet(properties));

        if (order != null && order.lines() != null && !order.lines().isEmpty()) {
            dialog.addTab("Itens do Documento", "fas-list", UIHelper.APPROVED_GREEN, RecordDetailsTabBuilder.buildDocumentLinesTable(order));
        }
        dialog.addTab("Rastreabilidade & Auditoria", "fas-shield-alt", UIHelper.APPROVED_GREEN, RecordDetailsTabBuilder.buildAuditTable(table, selectedRow));
        dialog.addTab("Partilha Executiva", "fas-share-alt", UIHelper.ACCENT_CYAN, RecordDetailsExporter.buildSharePreviewTab(mainTitle, properties));

        RecordDetailsExporter.setupFooterActions(dialog, table, selectedRow, mainTitle, properties);
        dialog.showDialog();
    }

    public static JComponent buildPropertySheet(List<PropertyItem> items) {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cols = {"Propriedade", "Valor Registado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (PropertyItem item : items) model.addRow(new Object[]{item.label(), item.value()});

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        table.setRowHeight(28);
        table.setCellSelectionEnabled(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(0).setMaxWidth(340);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);

        table.getColumnModel().getColumn(0).setCellRenderer((t, val, sel, foc, r, c) -> {
            JLabel lbl = new JLabel("  " + (val != null ? val : ""));
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(sel ? Color.WHITE : UIHelper.TEXT_MUTED);
            lbl.setBackground(sel ? UIHelper.ACCENT_BLUE : (r % 2 == 0 ? UIHelper.BG_CARD : UIHelper.BG_DARK));
            lbl.setOpaque(true);
            return lbl;
        });

        table.getColumnModel().getColumn(1).setCellRenderer((t, val, sel, foc, r, c) -> {
            String str = val != null ? String.valueOf(val) : "—";
            JLabel lbl = new JLabel("  " + str);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            boolean isMoney = str.contains("MT") || str.contains("MZN");
            lbl.setForeground(sel ? Color.WHITE : (isMoney ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_LIGHT));
            lbl.setBackground(sel ? UIHelper.ACCENT_BLUE : (r % 2 == 0 ? UIHelper.BG_CARD : UIHelper.BG_DARK));
            lbl.setOpaque(true);
            return lbl;
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField searchField = TableFilter.searchField("Filtrar propriedades do registo...");
        UIHelper.onTextChange(searchField, () -> {
            String q = searchField.getText().trim();
            sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
        });

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);
        topBar.add(searchField, BorderLayout.CENTER);
        JLabel countLbl = new JLabel(items.size() + " atributos");
        countLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        countLbl.setForeground(UIHelper.TEXT_MUTED);
        countLbl.setBorder(new EmptyBorder(0, 8, 0, 0));
        topBar.add(countLbl, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    public static boolean isHidden(JTable table, int viewColumn) {
        var column = table.getColumnModel().getColumn(viewColumn);
        return column.getWidth() == 0 && column.getMaxWidth() == 0;
    }

    public static String valueText(Object value) {
        if (value == null) return "";
        if (value instanceof ProductDTO p) return p.name() != null && !p.name().isBlank() ? p.name() : (p.reference() != null ? p.reference() : "Produto #" + p.id());
        if (value instanceof ClientDTO c) return c.name() != null && !c.name().isBlank() ? c.name() : "Cliente #" + c.id();
        if (value instanceof OrderDTO o) return o.orderNumber() != null ? o.orderNumber() : "Encomenda #" + o.id();
        if (value instanceof mz.multicore.erp.modules.purchases.dto.SupplierDTO s) return s.name() != null && !s.name().isBlank() ? s.name() : "Fornecedor #" + s.id();
        if (value instanceof mz.multicore.erp.modules.inventory.dto.WarehouseDTO w) return w.name() != null && !w.name().isBlank() ? w.name() : "Armazém #" + w.id();
        String s = String.valueOf(value);
        if (s.startsWith("ProductDTO[") && s.contains("name=")) {
            int idx = s.indexOf("name="), end = s.indexOf(",", idx);
            if (end == -1) end = s.indexOf("]", idx);
            if (end > idx + 5) return s.substring(idx + 5, end).trim();
        }
        if (s.startsWith("ClientDTO[") && s.contains("name=")) {
            int idx = s.indexOf("name=");
            int end = s.indexOf(",", idx);
            if (end == -1) end = s.indexOf("]", idx);
            if (end > idx + 5) return s.substring(idx + 5, end).trim();
        }
        return s;
    }
}
