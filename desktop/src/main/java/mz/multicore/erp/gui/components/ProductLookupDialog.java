package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Diálogo modal corporativo para pesquisa e selecção de produtos com suporte duplo:
 * <ul>
 *   <li>Paginação local em memória para catálogos já descarregados;</li>
 *   <li>Paginação remota de servidor (server-side) assíncrona para catálogos com 10.000–50.000+ artigos.</li>
 * </ul>
 */
public final class ProductLookupDialog {

    private static final String[] COLUMNS = {
            "Código / SKU", "Referência", "Artigo", "Categoria", "Preço Venda", "Embalagem", "Cód. Barras"
    };

    private static final int COL_SKU = 0;
    private static final int COL_REF = 1;
    private static final int COL_NAME = 2;
    private static final int COL_CATEGORY = 3;
    private static final int COL_PRICE = 4;
    private static final int COL_PACKAGING = 5;
    private static final int COL_BARCODE = 6;

    /** Provedor assíncrono para pesquisa e paginação de artigos no servidor. */
    @FunctionalInterface
    public interface ProductPageProvider {
        PageResponse<ProductDTO> fetchPage(String query, Long categoryId, int page, int size) throws Exception;
    }

    /** Opção de categoria para o selector de filtros remotos. */
    public record CategoryOption(Long id, String name) {
        @Override
        public String toString() {
            return name == null ? "" : name;
        }
    }

    private ProductLookupDialog() {
    }

    // ==========================================
    // MÉTODOS LOCAIS (IN-MEMORY)
    // ==========================================

    /**
     * Exibe o diálogo para selecção de um único produto a partir de lista em memória.
     */
    public static ProductDTO show(Component parent, List<ProductDTO> products) {
        List<ProductDTO> selected = showInternal(parent, products, false);
        return selected.isEmpty() ? null : selected.get(0);
    }

    /**
     * Exibe o diálogo para selecção de múltiplos produtos a partir de lista em memória.
     */
    public static List<ProductDTO> showMulti(Component parent, List<ProductDTO> products) {
        return showInternal(parent, products, true);
    }

    /**
     * Exibe o diálogo para selecção e repassa os produtos seleccionados a um consumidor.
     */
    public static void show(Component parent, List<ProductDTO> products, java.util.function.Consumer<List<ProductDTO>> onSelected) {
        List<ProductDTO> selected = showMulti(parent, products);
        if (selected != null && !selected.isEmpty() && onSelected != null) {
            onSelected.accept(selected);
        }
    }

    // ==========================================
    // MÉTODOS REMOTOS (SERVER-SIDE PAGINATION)
    // ==========================================

    /**
     * Exibe o diálogo com pesquisa e paginação remota no servidor (selecção única).
     */
    public static ProductDTO showRemote(Component parent, ProductPageProvider provider) {
        return showRemote(parent, provider, (List<CategoryOption>) null);
    }

    /**
     * Exibe o diálogo com pesquisa e paginação remota no servidor (selecção única com categorias).
     */
    public static ProductDTO showRemote(Component parent, ProductPageProvider provider, List<CategoryOption> categories) {
        List<ProductDTO> selected = showInternalRemote(parent, provider, categories, false);
        return selected.isEmpty() ? null : selected.get(0);
    }

    /**
     * Exibe o diálogo com pesquisa e paginação remota no servidor (selecção única com {@link ProductCategoryDTO}).
     */
    public static ProductDTO showRemoteCategories(Component parent, ProductPageProvider provider, List<ProductCategoryDTO> categories) {
        return showRemote(parent, provider, toOptions(categories));
    }

    /**
     * Exibe o diálogo com pesquisa e paginação remota no servidor (múltipla selecção).
     */
    public static List<ProductDTO> showMultiRemote(Component parent, ProductPageProvider provider) {
        return showMultiRemote(parent, provider, (List<CategoryOption>) null);
    }

    /**
     * Exibe o diálogo com pesquisa e paginação remota no servidor (múltipla selecção com categorias).
     */
    public static List<ProductDTO> showMultiRemote(Component parent, ProductPageProvider provider, List<CategoryOption> categories) {
        return showInternalRemote(parent, provider, categories, true);
    }

    /**
     * Exibe o diálogo com pesquisa e paginação remota no servidor (múltipla selecção com {@link ProductCategoryDTO}).
     */
    public static List<ProductDTO> showMultiRemoteCategories(Component parent, ProductPageProvider provider, List<ProductCategoryDTO> categories) {
        return showMultiRemote(parent, provider, toOptions(categories));
    }

    /**
     * Exibe o diálogo com pesquisa remota e entrega os resultados seleccionados a um consumidor.
     */
    public static void showRemote(Component parent, ProductPageProvider provider, List<CategoryOption> categories, java.util.function.Consumer<List<ProductDTO>> onSelected) {
        List<ProductDTO> selected = showMultiRemote(parent, provider, categories);
        if (selected != null && !selected.isEmpty() && onSelected != null) {
            onSelected.accept(selected);
        }
    }

    // ==========================================
    // IMPLEMENTAÇÃO LOCAL
    // ==========================================

    private static List<ProductDTO> showInternal(Component parent, List<ProductDTO> products, boolean multiSelect) {
        final List<ProductDTO> catalog = products == null ? List.of() : List.copyOf(products);
        final Window parentWindow = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        final List<ProductDTO> result = new ArrayList<>();

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ProductDTO p : catalog) {
            model.addRow(new Object[]{
                    firstText(p.sku(), "—"),
                    firstText(p.reference(), "—"),
                    safe(p.name()),
                    firstText(p.categoryName(), "Sem categoria"),
                    p.unitPrice() != null ? p.unitPrice() : BigDecimal.ZERO,
                    formatPackaging(p),
                    firstText(p.barcode(), "—")
            });
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.setSelectionMode(multiSelect
                ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                : ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(COL_PRICE).setCellRenderer(TableCellRenderers.money());

        table.getColumnModel().getColumn(COL_SKU).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_REF).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(260);
        table.getColumnModel().getColumn(COL_CATEGORY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_PRICE).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_PACKAGING).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_BARCODE).setPreferredWidth(110);

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
        for (String cat : sortedCategories) {
            categoryCombo.addItem(cat);
        }

        JPanel paginationPanel = ClientTablePagination.install(table);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel filterBar = TableFilter.bar(searchField, categoryCombo);
        card.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(860, 310));
        card.add(scroll, BorderLayout.CENTER);
        card.add(paginationPanel, BorderLayout.SOUTH);

        ModernFormDialog dialog = new ModernFormDialog(
                parentWindow,
                "Catálogo de Artigos & Produtos",
                "fas-boxes",
                "Pesquisa de produtos com paginação. Faça duplo clique para seleccionar.",
                card
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
                    if (filterCat && !selectedCat.equalsIgnoreCase(p.categoryName())) {
                        return false;
                    }
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
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    dialog.submit();
                }
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "lookupConfirm");
        table.getActionMap().put("lookupConfirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    dialog.submit();
                }
            }
        });

        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "focusTable");
        searchField.getActionMap().put("focusTable", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getRowCount() > 0) {
                    table.requestFocusInWindow();
                    if (table.getSelectedRow() < 0) {
                        table.changeSelection(0, 0, false, false);
                    }
                }
            }
        });

        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "searchConfirm");
        searchField.getActionMap().put("searchConfirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() < 0 && table.getRowCount() > 0) {
                    table.changeSelection(0, 0, false, false);
                }
                if (table.getSelectedRow() >= 0) {
                    dialog.submit();
                }
            }
        });

        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }

        SwingUtilities.invokeLater(searchField::requestFocusInWindow);

        boolean confirmed = dialog.showDialog();
        return confirmed ? result : List.of();
    }

    // ==========================================
    // IMPLEMENTAÇÃO REMOTA (SERVER-SIDE)
    // ==========================================

    private static List<ProductDTO> showInternalRemote(
            Component parent,
            ProductPageProvider provider,
            List<CategoryOption> categories,
            boolean multiSelect
    ) {
        Objects.requireNonNull(provider, "ProductPageProvider não pode ser nulo.");
        final Window parentWindow = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        final List<ProductDTO> result = new ArrayList<>();
        final List<ProductDTO> currentItems = new ArrayList<>();

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        table.setSelectionMode(multiSelect
                ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                : ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(COL_PRICE).setCellRenderer(TableCellRenderers.money());

        table.getColumnModel().getColumn(COL_SKU).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_REF).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(260);
        table.getColumnModel().getColumn(COL_CATEGORY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_PRICE).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_PACKAGING).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_BARCODE).setPreferredWidth(110);

        JTextField searchField = TableFilter.searchField("Pesquisar por nome, SKU, referência ou código de barras…");
        JComboBox<CategoryOption> categoryCombo = new JComboBox<>();
        UIHelper.styleComboBox(categoryCombo);
        categoryCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        categoryCombo.addItem(new CategoryOption(null, "Todas as categorias"));
        if (categories != null) {
            for (CategoryOption cat : categories) {
                if (cat != null) categoryCombo.addItem(cat);
            }
        }

        Integer[] pageSizes = {25, 50, 100, 200};
        JComboBox<Integer> pageSizeCombo = new JComboBox<>(pageSizes);
        UIHelper.styleComboBox(pageSizeCombo);
        pageSizeCombo.setSelectedItem(50);
        pageSizeCombo.setPreferredSize(new Dimension(86, UIHelper.FORM_CONTROL_HEIGHT - 4));

        JLabel statusLeft = new JLabel("A carregar catálogo do servidor…");
        statusLeft.setForeground(UIHelper.TEXT_MUTED);
        statusLeft.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        JLabel pageLabel = new JLabel("Página 1 de 1");
        pageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pageLabel.setForeground(UIHelper.TEXT_LIGHT);
        pageLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        var firstBtn = ClientTablePagination.ghostBtn("fas-angle-double-left", "Primeira página");
        var prevBtn = ClientTablePagination.ghostBtn("fas-angle-left", "Página anterior");
        var nextBtn = ClientTablePagination.ghostBtn("fas-angle-right", "Página seguinte");
        var lastBtn = ClientTablePagination.ghostBtn("fas-angle-double-right", "Última página");

        firstBtn.setEnabled(false);
        prevBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        lastBtn.setEnabled(false);

        JPanel paginationPanel = new JPanel(new BorderLayout(0, 0));
        paginationPanel.setOpaque(false);
        paginationPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(6, 8, 6, 8)));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPanel.setOpaque(false);
        JLabel perPageLabel = new JLabel("Por página:");
        perPageLabel.setForeground(UIHelper.TEXT_MUTED);
        perPageLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        leftPanel.add(perPageLabel);
        leftPanel.add(pageSizeCombo);
        leftPanel.add(statusLeft);
        paginationPanel.add(leftPanel, BorderLayout.WEST);

        paginationPanel.add(pageLabel, BorderLayout.CENTER);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navPanel.setOpaque(false);
        navPanel.add(firstBtn);
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        navPanel.add(lastBtn);
        paginationPanel.add(navPanel, BorderLayout.EAST);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel filterBar = TableFilter.bar(searchField, categoryCombo);
        card.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(860, 310));
        card.add(scroll, BorderLayout.CENTER);
        card.add(paginationPanel, BorderLayout.SOUTH);

        ModernFormDialog dialog = new ModernFormDialog(
                parentWindow,
                "Catálogo de Artigos & Produtos",
                "fas-boxes",
                "Pesquisa de produtos no servidor. Faça duplo clique para seleccionar.",
                card
        );
        dialog.setConfirmButton(multiSelect ? "Seleccionar Artigo(s)" : "Seleccionar Artigo", "fas-check");
        dialog.setCancelButton("Cancelar", "fas-times");
        dialog.setSize(940, 560);

        final int[] state = new int[]{0, 1}; // [currentPage, totalPages]
        final AtomicLong requestCounter = new AtomicLong(0);

        final java.util.function.Consumer<Integer> loadPage = targetPage -> {
            long reqId = requestCounter.incrementAndGet();
            String query = searchField.getText().trim();
            CategoryOption catOpt = (CategoryOption) categoryCombo.getSelectedItem();
            Long categoryId = catOpt != null ? catOpt.id() : null;
            Integer sizeObj = (Integer) pageSizeCombo.getSelectedItem();
            int size = sizeObj != null ? sizeObj : 50;

            statusLeft.setText("A pesquisar artigos no servidor…");
            firstBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            lastBtn.setEnabled(false);

            Thread worker = new Thread(() -> {
                try {
                    PageResponse<ProductDTO> pageResp = provider.fetchPage(query, categoryId, targetPage, size);
                    SwingUtilities.invokeLater(() -> {
                        if (requestCounter.get() != reqId) return;
                        currentItems.clear();
                        if (pageResp != null && pageResp.items() != null) {
                            currentItems.addAll(pageResp.items());
                        }
                        model.setRowCount(0);
                        for (ProductDTO p : currentItems) {
                            model.addRow(new Object[]{
                                    firstText(p.sku(), "—"),
                                    firstText(p.reference(), "—"),
                                    safe(p.name()),
                                    firstText(p.categoryName(), "Sem categoria"),
                                    p.unitPrice() != null ? p.unitPrice() : BigDecimal.ZERO,
                                    formatPackaging(p),
                                    firstText(p.barcode(), "—")
                            });
                        }

                        int cur = pageResp != null ? pageResp.page() : 0;
                        int tot = pageResp != null ? Math.max(1, pageResp.totalPages()) : 1;
                        long totalItems = pageResp != null ? pageResp.totalElements() : 0;
                        state[0] = cur;
                        state[1] = tot;

                        pageLabel.setText(String.format("Página  %d  de  %d", cur + 1, tot));
                        firstBtn.setEnabled(cur > 0);
                        prevBtn.setEnabled(cur > 0);
                        nextBtn.setEnabled(cur + 1 < tot);
                        lastBtn.setEnabled(cur + 1 < tot);

                        if (totalItems == 0) {
                            statusLeft.setText("0 artigos encontrados");
                        } else {
                            int from = (cur * size) + 1;
                            int to = Math.min((int) totalItems, (cur + 1) * size);
                            statusLeft.setText(String.format("%d - %d de %d artigo(s)", from, to, totalItems));
                        }

                        if (table.getRowCount() > 0) {
                            table.setRowSelectionInterval(0, 0);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        if (requestCounter.get() != reqId) return;
                        statusLeft.setText("Erro ao carregar: " + (ex.getMessage() != null ? ex.getMessage() : "Falha de rede"));
                    });
                }
            });
            worker.setDaemon(true);
            worker.start();
        };

        Timer debounceTimer = new Timer(350, e -> loadPage.accept(0));
        debounceTimer.setRepeats(false);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
            @Override
            public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            @Override
            public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });

        categoryCombo.addActionListener(e -> {
            debounceTimer.stop();
            loadPage.accept(0);
        });

        pageSizeCombo.addActionListener(e -> {
            debounceTimer.stop();
            loadPage.accept(0);
        });

        firstBtn.addActionListener(e -> loadPage.accept(0));
        prevBtn.addActionListener(e -> loadPage.accept(Math.max(0, state[0] - 1)));
        nextBtn.addActionListener(e -> loadPage.accept(Math.min(state[1] - 1, state[0] + 1)));
        lastBtn.addActionListener(e -> loadPage.accept(Math.max(0, state[1] - 1)));

        dialog.setOnSave(() -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows == null || selectedRows.length == 0) {
                throw new IllegalArgumentException("Seleccione pelo menos um artigo na tabela.");
            }
            result.clear();
            for (int viewRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (modelRow >= 0 && modelRow < currentItems.size()) {
                    result.add(currentItems.get(modelRow));
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    dialog.submit();
                }
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "lookupConfirm");
        table.getActionMap().put("lookupConfirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    dialog.submit();
                }
            }
        });

        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "focusTable");
        searchField.getActionMap().put("focusTable", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.getRowCount() > 0) {
                    table.requestFocusInWindow();
                    if (table.getSelectedRow() < 0) {
                        table.changeSelection(0, 0, false, false);
                    }
                }
            }
        });

        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "searchConfirm");
        searchField.getActionMap().put("searchConfirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (debounceTimer.isRunning()) {
                    debounceTimer.stop();
                    loadPage.accept(0);
                    return;
                }
                if (table.getSelectedRow() < 0 && table.getRowCount() > 0) {
                    table.changeSelection(0, 0, false, false);
                }
                if (table.getSelectedRow() >= 0) {
                    dialog.submit();
                }
            }
        });

        // Carga inicial
        loadPage.accept(0);
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);

        boolean confirmed = dialog.showDialog();
        return confirmed ? result : List.of();
    }

    private static List<CategoryOption> toOptions(List<ProductCategoryDTO> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        List<CategoryOption> options = new ArrayList<>();
        for (ProductCategoryDTO c : categories) {
            if (c != null && c.name() != null) {
                options.add(new CategoryOption(c.id(), c.name()));
            }
        }
        return options;
    }

    private static String formatPackaging(ProductDTO p) {
        if (p == null) return "—";
        int unitsPerBox = p.unitsPerBox();
        int packages = p.packagesPerBox();
        int unitsPerPkg = p.unitsPerPackage();
        if (unitsPerBox > 1) {
            if (packages > 1 && unitsPerPkg > 1) {
                return packages + " emb x " + unitsPerPkg + " un (" + unitsPerBox + " un/cx)";
            }
            return unitsPerBox + " un/cx";
        }
        return "1 un";
    }

    private static String normalize(String value) {
        String plain = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).trim();
    }

    private static String firstText(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary.trim() : fallback;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
