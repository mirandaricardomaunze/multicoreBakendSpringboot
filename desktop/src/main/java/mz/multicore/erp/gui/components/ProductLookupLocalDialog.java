package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import static mz.multicore.erp.gui.components.ProductLookupTableHelper.*;

/**
 * Diálogo modal para pesquisa e selecção de produtos a partir de catálogo local em memória.
 */
public final class ProductLookupLocalDialog {

    private ProductLookupLocalDialog() {}

    public static List<ProductDTO> show(Component parent, List<ProductDTO> products, boolean multiSelect) {
        final List<ProductDTO> catalog = products == null ? List.of() : List.copyOf(products);
        final Window parentWindow = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        final List<ProductDTO> result = new ArrayList<>();

        DefaultTableModel model = createTableModel();
        for (ProductDTO p : catalog) addProductRow(model, p);

        JTable table = new JTable(model);
        configureTable(table, multiSelect);

        JTextField searchField = TableFilter.searchField("Pesquisar por nome, SKU, referência ou código de barras…");
        JComboBox<String> categoryCombo = new JComboBox<>();
        UIHelper.styleComboBox(categoryCombo);
        categoryCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        categoryCombo.addItem("Todas as categorias");

        Set<String> categories = new LinkedHashSet<>();
        for (ProductDTO p : catalog) {
            if (p.categoryName() != null && !p.categoryName().isBlank()) {
                categories.add(p.categoryName().trim());
            }
        }
        List<String> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);
        for (String cat : sortedCategories) categoryCombo.addItem(cat);

        JPanel paginationPanel = ClientTablePagination.install(table);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.add(TableFilter.bar(searchField, categoryCombo), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(860, 310));
        card.add(scroll, BorderLayout.CENTER);
        card.add(paginationPanel, BorderLayout.SOUTH);

        ModernFormDialog dialog = new ModernFormDialog(
                parentWindow, "Catálogo de Artigos & Produtos", "fas-boxes",
                "Pesquisa de produtos com paginação. Faça duplo clique para seleccionar.", card
        );
        dialog.setConfirmButton(multiSelect ? "Seleccionar Artigo(s)" : "Seleccionar Artigo", "fas-check");
        dialog.setCancelButton("Cancelar", "fas-times");
        dialog.setSize(940, 560);

        Runnable applyFilter = () -> {
            String query = normalize(searchField.getText());
            String selectedCat = (String) categoryCombo.getSelectedItem();
            boolean filterCat = selectedCat != null && !selectedCat.isBlank() && !"Todas as categorias".equalsIgnoreCase(selectedCat);
            String[] tokens = query.isBlank() ? new String[0] : query.split("\\s+");

            ClientTablePagination.setBaseFilter(table, new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    int modelRow = entry.getIdentifier();
                    if (modelRow < 0 || modelRow >= catalog.size()) return false;
                    ProductDTO p = catalog.get(modelRow);
                    if (filterCat && !selectedCat.equalsIgnoreCase(p.categoryName())) return false;
                    if (tokens.length == 0) return true;
                    String searchTarget = normalize(String.join(" ",
                            safe(p.name()), safe(p.sku()), safe(p.reference()),
                            safe(p.barcode()), safe(p.categoryName()), safe(p.description())));
                    for (String token : tokens) {
                        if (!searchTarget.contains(token)) return false;
                    }
                    return true;
                }
            });
        };

        UIHelper.onTextChange(searchField, applyFilter);
        categoryCombo.addActionListener(e -> applyFilter.run());

        dialog.setOnSave(() -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows == null || selectedRows.length == 0) {
                throw new IllegalArgumentException("Seleccione pelo menos um artigo na tabela.");
            }
            result.clear();
            for (int viewRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (modelRow >= 0 && modelRow < catalog.size()) {
                    result.add(catalog.get(modelRow));
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) dialog.submit();
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "lookupConfirm");
        table.getActionMap().put("lookupConfirm", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() >= 0) dialog.submit();
            }
        });

        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "focusTable");
        searchField.getActionMap().put("focusTable", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (table.getRowCount() > 0) {
                    table.requestFocusInWindow();
                    if (table.getSelectedRow() < 0) table.changeSelection(0, 0, false, false);
                }
            }
        });

        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "searchConfirm");
        searchField.getActionMap().put("searchConfirm", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() < 0 && table.getRowCount() > 0) table.changeSelection(0, 0, false, false);
                if (table.getSelectedRow() >= 0) dialog.submit();
            }
        });

        if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);

        return dialog.showDialog() ? result : List.of();
    }
}
