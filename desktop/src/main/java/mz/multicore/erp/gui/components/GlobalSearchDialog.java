package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Diálogo de Pesquisa Global Rápida (Spotlight / Command Palette).
 * Permite navegar instantaneamente para qualquer módulo ou ação via teclado ou rato.
 */
public class GlobalSearchDialog extends JDialog {

    /** Representa um item pesquisável no sistema. */
    public record SearchItem(
            String id,
            String title,
            String category,
            String shortcutHint,
            Icon icon,
            Color accentColor,
            Runnable action,
            List<String> keywords
    ) {
        public SearchItem {
            keywords = keywords != null ? List.copyOf(keywords) : List.of();
        }

        public boolean matches(String query) {
            if (query == null || query.isBlank()) return true;
            String q = query.trim().toLowerCase();
            if (title.toLowerCase().contains(q)) return true;
            if (category != null && category.toLowerCase().contains(q)) return true;
            for (String kw : keywords) {
                if (kw.toLowerCase().contains(q)) return true;
            }
            return false;
        }
    }

    private final List<SearchItem> allItems = new ArrayList<>();
    private final DefaultListModel<SearchItem> listModel = new DefaultListModel<>();
    private final JList<SearchItem> resultList = new JList<>(listModel);
    private final JTextField searchField = new JTextField();
    private final JLabel countLabel = new JLabel();

    public GlobalSearchDialog(Window owner, List<SearchItem> items) {
        super(owner, "Pesquisa Global (Ctrl+K)", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(640, 420);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UIHelper.BG_DARK);

        if (items != null) {
            this.allItems.addAll(items);
        }

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(203, 213, 225) : new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        // Top Search Bar
        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setOpaque(false);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(226, 232, 240) : new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel searchIcon = new JLabel(UIHelper.icon("fas-search", 18, UIHelper.ACCENT_BLUE));
        searchField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 15));
        searchField.setForeground(UIHelper.TEXT_LIGHT);
        searchField.setCaretColor(UIHelper.TEXT_LIGHT);
        searchField.setOpaque(false);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.putClientProperty("JTextField.placeholderText", "Pesquisar módulos, opções e atalhos rápidos...");

        JPanel escPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        escPill.setOpaque(false);
        JLabel escLabel = new JLabel("ESC");
        escLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        escLabel.setForeground(UIHelper.TEXT_MUTED);
        escPill.add(escLabel);

        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(escPill, BorderLayout.EAST);

        root.add(searchBar, BorderLayout.NORTH);

        // Results List
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setOpaque(false);
        resultList.setCellRenderer(new SearchItemRenderer());
        resultList.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        root.add(scrollPane, BorderLayout.CENTER);

        // Footer Bar
        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setOpaque(false);
        footerBar.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));

        countLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        countLabel.setForeground(UIHelper.TEXT_MUTED);

        JLabel navHelp = new JLabel("↑↓ Navegar   ↵ Abrir   ESC Fechar");
        navHelp.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        navHelp.setForeground(UIHelper.TEXT_MUTED);

        footerBar.add(countLabel, BorderLayout.WEST);
        footerBar.add(navHelp, BorderLayout.EAST);
        root.add(footerBar, BorderLayout.SOUTH);

        add(root);

        // Filter listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
        });

        // Key bindings
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_DOWN) {
                    moveSelection(1);
                    e.consume();
                } else if (code == KeyEvent.VK_UP) {
                    moveSelection(-1);
                    e.consume();
                } else if (code == KeyEvent.VK_ENTER) {
                    executeSelected();
                    e.consume();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    dispose();
                    e.consume();
                }
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    int index = resultList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        resultList.setSelectedIndex(index);
                        executeSelected();
                    }
                }
            }
        });

        // Global ESC to close dialog
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        filter();
    }

    public static void show(Window owner, List<SearchItem> items) {
        SwingUtilities.invokeLater(() -> {
            GlobalSearchDialog dialog = new GlobalSearchDialog(owner, items);
            dialog.setVisible(true);
        });
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JList<SearchItem> getResultList() {
        return resultList;
    }

    public List<SearchItem> getAllItems() {
        return Collections.unmodifiableList(allItems);
    }

    public List<SearchItem> getFilteredItems() {
        List<SearchItem> res = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            res.add(listModel.get(i));
        }
        return Collections.unmodifiableList(res);
    }

    public void filter() {
        String query = searchField.getText();
        listModel.clear();
        for (SearchItem item : allItems) {
            if (item.matches(query)) {
                listModel.addElement(item);
            }
        }
        if (!listModel.isEmpty()) {
            resultList.setSelectedIndex(0);
        }
        int total = listModel.size();
        countLabel.setText(total + (total == 1 ? " resultado" : " resultados"));
    }

    private void moveSelection(int delta) {
        int size = listModel.size();
        if (size == 0) return;
        int current = resultList.getSelectedIndex();
        int next = current + delta;
        if (next < 0) next = 0;
        if (next >= size) next = size - 1;
        resultList.setSelectedIndex(next);
        resultList.ensureIndexIsVisible(next);
    }

    public void executeSelected() {
        SearchItem selected = resultList.getSelectedValue();
        if (selected != null) {
            dispose();
            if (selected.action() != null) {
                SwingUtilities.invokeLater(selected.action());
            }
        }
    }

    /** Renderer elegante para os itens da lista de pesquisa. */
    private static final class SearchItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
        ) {
            if (!(value instanceof SearchItem item)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            JPanel itemPanel = new JPanel(new BorderLayout(12, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (isSelected) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(UIHelper.isLight() ? new Color(237, 242, 247) : new Color(30, 41, 59));
                        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
                        g2.setColor(item.accentColor() != null ? item.accentColor() : UIHelper.ACCENT_BLUE);
                        g2.fillRoundRect(2, 6, 4, getHeight() - 12, 4, 4);
                        g2.dispose();
                    }
                }
            };
            itemPanel.setOpaque(false);
            itemPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            // Left icon
            JLabel iconLabel = new JLabel(item.icon());
            itemPanel.add(iconLabel, BorderLayout.WEST);

            // Center: Title + Category
            JPanel textStack = new JPanel(new BorderLayout(6, 0));
            textStack.setOpaque(false);

            JLabel titleLabel = new JLabel(item.title());
            titleLabel.setFont(new Font(UIHelper.FONT, isSelected ? Font.BOLD : Font.PLAIN, 13));
            titleLabel.setForeground(UIHelper.TEXT_LIGHT);

            JLabel catLabel = new JLabel(item.category() != null ? item.category() : "");
            catLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            catLabel.setForeground(UIHelper.TEXT_MUTED);

            textStack.add(titleLabel, BorderLayout.WEST);
            textStack.add(catLabel, BorderLayout.EAST);
            itemPanel.add(textStack, BorderLayout.CENTER);

            // Right: Shortcut hint pill
            if (item.shortcutHint() != null && !item.shortcutHint().isBlank()) {
                JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                hintPanel.setOpaque(false);
                JLabel hintLabel = new JLabel(item.shortcutHint());
                hintLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
                hintLabel.setForeground(isSelected ? UIHelper.ACCENT_BLUE : UIHelper.TEXT_MUTED);
                hintLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(203, 213, 225) : new Color(71, 85, 105), 1),
                        BorderFactory.createEmptyBorder(2, 6, 2, 6)
                ));
                hintPanel.add(hintLabel);
                itemPanel.add(hintPanel, BorderLayout.EAST);
            }

            return itemPanel;
        }
    }
}
