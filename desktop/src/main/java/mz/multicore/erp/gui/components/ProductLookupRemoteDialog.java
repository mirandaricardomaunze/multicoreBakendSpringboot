package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static mz.multicore.erp.gui.components.ProductLookupTableHelper.*;

/**
 * Diálogo modal para pesquisa e selecção de produtos com paginação assíncrona remota no servidor.
 */
public final class ProductLookupRemoteDialog {

    private ProductLookupRemoteDialog() {}

    public static List<ProductDTO> show(
            Component parent,
            ProductLookupDialog.ProductPageProvider provider,
            List<ProductLookupDialog.CategoryOption> categories,
            boolean multiSelect
    ) {
        Objects.requireNonNull(provider, "ProductPageProvider não pode ser nulo.");
        final Window parentWindow = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        final List<ProductDTO> result = new ArrayList<>();
        final List<ProductDTO> currentItems = new ArrayList<>();

        DefaultTableModel model = createTableModel();
        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        configureTable(table, multiSelect);

        JTextField searchField = TableFilter.searchField("Pesquisar por nome, SKU, referência ou código de barras…");
        JComboBox<ProductLookupDialog.CategoryOption> categoryCombo = new JComboBox<>();
        UIHelper.styleComboBox(categoryCombo);
        categoryCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        categoryCombo.addItem(new ProductLookupDialog.CategoryOption(null, "Todas as categorias"));
        if (categories != null) {
            for (ProductLookupDialog.CategoryOption cat : categories) {
                if (cat != null) categoryCombo.addItem(cat);
            }
        }

        JComboBox<Integer> pageSizeCombo = new JComboBox<>(new Integer[]{25, 50, 100, 200});
        UIHelper.styleComboBox(pageSizeCombo);
        pageSizeCombo.setSelectedItem(50);
        pageSizeCombo.setPreferredSize(new Dimension(86, UIHelper.FORM_CONTROL_HEIGHT - 4));

        JLabel statusLeft = new JLabel("A carregar catálogo do servidor…");
        statusLeft.setForeground(UIHelper.TEXT_MUTED);
        statusLeft.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        JLabel pageLabel = new JLabel("Página 1 de 1", SwingConstants.CENTER);
        pageLabel.setForeground(UIHelper.TEXT_LIGHT);
        pageLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        var firstBtn = ClientTablePagination.ghostBtn("fas-angle-double-left", "Primeira página");
        var prevBtn = ClientTablePagination.ghostBtn("fas-angle-left", "Página anterior");
        var nextBtn = ClientTablePagination.ghostBtn("fas-angle-right", "Página seguinte");
        var lastBtn = ClientTablePagination.ghostBtn("fas-angle-double-right", "Última página");
        firstBtn.setEnabled(false); prevBtn.setEnabled(false); nextBtn.setEnabled(false); lastBtn.setEnabled(false);

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
        navPanel.add(firstBtn); navPanel.add(prevBtn); navPanel.add(nextBtn); navPanel.add(lastBtn);
        paginationPanel.add(navPanel, BorderLayout.EAST);

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
                "Pesquisa de produtos no servidor. Faça duplo clique para seleccionar.", card
        );
        dialog.setConfirmButton(multiSelect ? "Seleccionar Artigo(s)" : "Seleccionar Artigo", "fas-check");
        dialog.setCancelButton("Cancelar", "fas-times");
        dialog.setSize(940, 560);

        final int[] state = new int[]{0, 1}; // [currentPage, totalPages]
        final AtomicLong requestCounter = new AtomicLong(0);

        final java.util.function.Consumer<Integer> loadPage = targetPage -> {
            long reqId = requestCounter.incrementAndGet();
            String query = searchField.getText().trim();
            ProductLookupDialog.CategoryOption catOpt = (ProductLookupDialog.CategoryOption) categoryCombo.getSelectedItem();
            Long categoryId = catOpt != null ? catOpt.id() : null;
            Integer sizeObj = (Integer) pageSizeCombo.getSelectedItem();
            int size = sizeObj != null ? sizeObj : 50;

            statusLeft.setText("A pesquisar artigos no servidor…");
            firstBtn.setEnabled(false); prevBtn.setEnabled(false); nextBtn.setEnabled(false); lastBtn.setEnabled(false);

            Thread worker = new Thread(() -> {
                try {
                    PageResponse<ProductDTO> pageResp = provider.fetchPage(query, categoryId, targetPage, size);
                    SwingUtilities.invokeLater(() -> {
                        if (requestCounter.get() != reqId) return;
                        currentItems.clear();
                        if (pageResp != null && pageResp.items() != null) currentItems.addAll(pageResp.items());
                        model.setRowCount(0);
                        for (ProductDTO p : currentItems) addProductRow(model, p);

                        int cur = pageResp != null ? pageResp.page() : 0;
                        int tot = pageResp != null ? Math.max(1, pageResp.totalPages()) : 1;
                        long totalItems = pageResp != null ? pageResp.totalElements() : 0;
                        state[0] = cur; state[1] = tot;

                        pageLabel.setText(String.format("Página  %d  de  %d", cur + 1, tot));
                        firstBtn.setEnabled(cur > 0); prevBtn.setEnabled(cur > 0);
                        nextBtn.setEnabled(cur + 1 < tot); lastBtn.setEnabled(cur + 1 < tot);

                        if (totalItems == 0) {
                            statusLeft.setText("0 artigos encontrados");
                        } else {
                            int from = (cur * size) + 1;
                            int to = Math.min((int) totalItems, (cur + 1) * size);
                            statusLeft.setText(String.format("%d - %d de %d artigo(s)", from, to, totalItems));
                        }
                        if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
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
            @Override public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });

        categoryCombo.addActionListener(e -> { debounceTimer.stop(); loadPage.accept(0); });
        pageSizeCombo.addActionListener(e -> { debounceTimer.stop(); loadPage.accept(0); });

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
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) dialog.submit();
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "lookupConfirm");
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
                if (debounceTimer.isRunning()) {
                    debounceTimer.stop();
                    loadPage.accept(0);
                    return;
                }
                if (table.getSelectedRow() < 0 && table.getRowCount() > 0) table.changeSelection(0, 0, false, false);
                if (table.getSelectedRow() >= 0) dialog.submit();
            }
        });

        loadPage.accept(0);
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);
        return dialog.showDialog() ? result : List.of();
    }
}
