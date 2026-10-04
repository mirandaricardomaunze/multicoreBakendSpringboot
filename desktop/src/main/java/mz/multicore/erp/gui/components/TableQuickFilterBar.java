package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Barra Universal de Filtro Rápido e Pesquisa em Tabelas (TableQuickFilterBar / Ctrl+F).
 * Permite filtrar linhas em tempo real através de TableRowSorter com correspondência multi-termo,
 * case-insensitive, contador dinâmico e atalhos rápidos de teclado.
 * Em conformidade com SPEC-TBLF-001.
 */
public class TableQuickFilterBar extends JPanel {

    private final JTable table;
    private final TableRowSorter<TableModel> sorter;
    private final JTextField filterField;
    private final JButton clearButton;
    private final JLabel countLabel;
    private final TableModelListener modelListener;

    public TableQuickFilterBar(JTable table) {
        this.table = Objects.requireNonNull(table, "table cannot be null");

        // 1. Configurar ou reutilizar TableRowSorter
        if (table.getRowSorter() instanceof TableRowSorter<?>) {
            @SuppressWarnings("unchecked")
            TableRowSorter<TableModel> existing = (TableRowSorter<TableModel>) table.getRowSorter();
            this.sorter = existing;
        } else {
            this.sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(this.sorter);
        }

        // 2. Layout e Estilo do Container
        setLayout(new BorderLayout(8, 0));
        setOpaque(true);
        updateColors();

        // 3. Painel de Pesquisa com Ícone, Campo e Botão Limpar
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setOpaque(false);

        JLabel searchIcon = new JLabel(UIHelper.icon("fas-search", 13));
        searchIcon.setBorder(new EmptyBorder(0, 4, 0, 0));
        searchBox.add(searchIcon, BorderLayout.WEST);

        this.filterField = new JTextField();
        this.filterField.setToolTipText("Filtrar registos... (Ctrl+F, Limpar: Esc)");
        this.filterField.putClientProperty("JTextField.placeholderText", "Filtrar registos... (Ctrl+F)");
        this.filterField.setPreferredSize(new Dimension(240, 30));
        UIHelper.styleTextField(this.filterField);
        searchBox.add(this.filterField, BorderLayout.CENTER);

        // Botão de Limpeza com ícone vetorial fas-times
        this.clearButton = UIHelper.createSecondaryButton("");
        this.clearButton.setIcon(UIHelper.icon("fas-times", 11));
        this.clearButton.setToolTipText("Limpar filtro (Esc)");
        this.clearButton.setPreferredSize(new Dimension(30, 30));
        this.clearButton.setEnabled(false);
        this.clearButton.addActionListener(e -> clearFilter());
        searchBox.add(this.clearButton, BorderLayout.EAST);

        add(searchBox, BorderLayout.WEST);

        // 4. Painel com Contador de Registos
        this.countLabel = new JLabel();
        this.countLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        this.countLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.countLabel.setBorder(new EmptyBorder(0, 4, 0, 4));
        add(this.countLabel, BorderLayout.EAST);

        // 5. Ouvinte de digitação no campo de pesquisa
        this.filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        // 6. Navegação com Enter e Seta Abaixo para focar a tabela
        this.filterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (table.getRowCount() > 0) {
                        table.requestFocusInWindow();
                        if (table.getSelectedRow() < 0) {
                            table.setRowSelectionInterval(0, 0);
                        }
                    }
                }
            }
        });

        // 7. Ouvinte de alterações de dados no modelo da tabela
        this.modelListener = e -> updateCountLabel();
        if (this.table.getModel() != null) {
            this.table.getModel().addTableModelListener(this.modelListener);
        }

        // 8. Configurar atalhos de teclado (Ctrl+F e Escape)
        setupKeyBindings();

        // 9. Atualizar contagem inicial
        updateCountLabel();
    }

    private void updateColors() {
        if (UIHelper.isHighContrast()) {
            setBackground(Color.BLACK);
            setBorder(new LineBorder(Color.WHITE, 1));
        } else {
            setBackground(UIHelper.BG_CARD);
            setBorder(new EmptyBorder(4, 6, 4, 6));
        }
    }

    private void setupKeyBindings() {
        // Atalho Escape no filterField
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        this.filterField.getInputMap(JComponent.WHEN_FOCUSED).put(escapeKey, "escapeFilter");
        this.filterField.getActionMap().put("escapeFilter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!filterField.getText().isEmpty()) {
                    clearFilter();
                } else {
                    table.requestFocusInWindow();
                }
            }
        });

        // Atalho Ctrl+F na Tabela e neste Container
        int mask = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            }
        } catch (Throwable ignored) {
            mask = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
        KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, mask);

        Action focusAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focusSearch();
            }
        };

        this.table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlF, "quickFilterFocus");
        this.table.getActionMap().put("quickFilterFocus", focusAction);

        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlF, "quickFilterFocus");
        this.getActionMap().put("quickFilterFocus", focusAction);
    }

    public synchronized void applyFilter() {
        String rawText = filterField.getText();
        boolean hasText = rawText != null && !rawText.trim().isEmpty();
        clearButton.setEnabled(hasText);

        if (!hasText) {
            sorter.setRowFilter(null);
            updateCountLabel();
            return;
        }

        String[] tokens = rawText.trim().split("\\s+");
        List<RowFilter<Object, Object>> tokenFilters = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank()) {
                tokenFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(token)));
            }
        }

        if (tokenFilters.isEmpty()) {
            sorter.setRowFilter(null);
        } else if (tokenFilters.size() == 1) {
            sorter.setRowFilter(tokenFilters.get(0));
        } else {
            sorter.setRowFilter(RowFilter.andFilter(tokenFilters));
        }

        updateCountLabel();
    }

    private void updateCountLabel() {
        int filtered = getFilteredCount();
        int total = getTotalCount();
        boolean isFiltered = !filterField.getText().trim().isEmpty();

        if (isFiltered) {
            countLabel.setText(String.format("Exibindo %d de %d registos", filtered, total));
            countLabel.setForeground(filtered > 0 ? UIHelper.ACCENT_CYAN : UIHelper.REJECTED_RED);
        } else {
            countLabel.setText(String.format("Total: %d registos", total));
            countLabel.setForeground(UIHelper.TEXT_MUTED);
        }
    }

    public void setFilterText(String text) {
        filterField.setText(text != null ? text : "");
    }

    public String getFilterText() {
        return filterField.getText();
    }

    public void clearFilter() {
        filterField.setText("");
        filterField.requestFocusInWindow();
    }

    public int getFilteredCount() {
        return table.getRowCount();
    }

    public int getTotalCount() {
        return table.getModel() != null ? table.getModel().getRowCount() : 0;
    }

    public void focusSearch() {
        filterField.requestFocusInWindow();
        filterField.selectAll();
    }

    public JTable getTable() {
        return table;
    }

    public TableRowSorter<TableModel> getSorter() {
        return sorter;
    }

    public JTextField getFilterField() {
        return filterField;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public JLabel getCountLabel() {
        return countLabel;
    }

    public static TableQuickFilterBar attach(JTable table) {
        return new TableQuickFilterBar(table);
    }

    public static JPanel wrapWithFilter(JTable table) {
        return wrapWithFilter(new JScrollPane(table), table);
    }

    public static JPanel wrapWithFilter(JScrollPane scrollPane, JTable table) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setOpaque(false);
        TableQuickFilterBar bar = new TableQuickFilterBar(table);
        wrapper.add(bar, BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
}
