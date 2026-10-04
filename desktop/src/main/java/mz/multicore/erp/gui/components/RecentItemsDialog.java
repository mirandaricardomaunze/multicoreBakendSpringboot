package mz.multicore.erp.gui.components;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo modal executivo de Histórico de Itens Recentes & Quick-Recall (Ctrl+H).
 * Permite filtrar por texto e reabrir registos ou módulos com um só clique ou Enter.
 */
public class RecentItemsDialog extends JDialog {

    private final Consumer<RecentItem> onSelect;
    private final JTextField searchField = new JTextField();
    private final DefaultListModel<RecentItem> listModel = new DefaultListModel<>();
    private final JList<RecentItem> itemList = new JList<>(listModel);
    private List<RecentItem> allItems;

    public RecentItemsDialog(Window parent, Consumer<RecentItem> onSelect) {
        super(parent, "Histórico de Itens Recentes", ModalityType.APPLICATION_MODAL);
        this.onSelect = onSelect;
        this.allItems = RecentItemsHistoryManager.getInstance().getRecentItems();

        initUi();
        refreshFilteredList();
        setupKeyBindings();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIHelper.BG_DARK);
        setSize(700, 500);
        setMinimumSize(new Dimension(550, 400));
        UIHelper.containWithinMain(this);

        // Cabeçalho Premium
        JComponent header = UIHelper.buildPremiumHeader(
                "Histórico de Itens Recentes",
                "Alterne e abra instantaneamente os últimos registos acedidos (Ctrl+H)",
                "fas-history",
                UIHelper.ACCENT_CYAN
        );
        add(header, BorderLayout.NORTH);

        // Corpo Central
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Barra de Pesquisa Rápida
        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setOpaque(false);
        JLabel searchIcon = new JLabel(UIHelper.icon("fas-search", 14, UIHelper.TEXT_MUTED));
        searchField.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        searchField.setBackground(UIHelper.FIELD_BG);
        searchField.setForeground(UIHelper.TEXT_LIGHT);
        searchField.setCaretColor(UIHelper.TEXT_LIGHT);
        searchField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Filtrar por nome, NUIT, referência ou categoria...");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshFilteredList(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshFilteredList(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshFilteredList(); }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && !listModel.isEmpty()) {
                    itemList.requestFocusInWindow();
                    itemList.setSelectedIndex(0);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && !listModel.isEmpty()) {
                    triggerSelection(listModel.getElementAt(0));
                }
            }
        });

        searchBox.add(searchIcon, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);
        body.add(searchBox, BorderLayout.NORTH);

        // Lista de Itens Recentes
        itemList.setBackground(UIHelper.BG_CARD);
        itemList.setForeground(UIHelper.TEXT_LIGHT);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setCellRenderer(new RecentItemCellRenderer());
        itemList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    RecentItem selected = itemList.getSelectedValue();
                    if (selected != null) {
                        triggerSelection(selected);
                    }
                }
            }
        });
        itemList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    RecentItem selected = itemList.getSelectedValue();
                    if (selected != null) {
                        triggerSelection(selected);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(itemList);
        UIHelper.styleScrollPane(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        body.add(scroll, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        // Rodapé de Ações
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 16, 14, 16));

        ModernButton clearBtn = UIHelper.createSecondaryButton("Limpar Histórico");
        clearBtn.setIcon(UIHelper.icon("fas-trash-alt", 13, UIHelper.REJECTED_RED));
        clearBtn.addActionListener(e -> {
            boolean confirmed = ModernMessageDialog.confirm(
                    this,
                    FeedbackType.WARNING,
                    "Limpar Histórico",
                    "Deseja limpar todo o histórico de itens recentes?",
                    "Limpar Histórico"
            );
            if (confirmed) {
                RecentItemsHistoryManager.getInstance().clear();
                allItems = List.of();
                refreshFilteredList();
            }
        });

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        ModernButton openBtn = UIHelper.createPrimaryButton("Abrir Registo");
        openBtn.setIcon(UIHelper.icon("fas-arrow-right", 14, Color.WHITE));
        openBtn.addActionListener(e -> {
            RecentItem selected = itemList.getSelectedValue();
            if (selected != null) {
                triggerSelection(selected);
            }
        });

        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.addActionListener(e -> dispose());

        rightActions.add(clearBtn);
        rightActions.add(closeBtn);
        rightActions.add(openBtn);
        footer.add(rightActions, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    private void refreshFilteredList() {
        String filter = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        listModel.clear();
        for (RecentItem item : allItems) {
            if (filter.isEmpty() ||
                    item.title().toLowerCase().contains(filter) ||
                    (item.subtitle() != null && item.subtitle().toLowerCase().contains(filter)) ||
                    item.category().toLowerCase().contains(filter)) {
                listModel.addElement(item);
            }
        }
        if (!listModel.isEmpty()) {
            itemList.setSelectedIndex(0);
        }
    }

    private void triggerSelection(RecentItem item) {
        dispose();
        if (onSelect != null && item != null) {
            onSelect.accept(item);
        }
    }

    private void setupKeyBindings() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    public static void show(Window parent, Consumer<RecentItem> onSelect) {
        RecentItemsDialog dialog = new RecentItemsDialog(parent, onSelect);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Renderizador visual dos itens recentes na lista.
     */
    private static class RecentItemCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof RecentItem item)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            JPanel panel = new JPanel(new BorderLayout(12, 0));
            panel.setBorder(new EmptyBorder(8, 10, 8, 10));
            panel.setBackground(isSelected ? UIHelper.SELECTION_BG : (index % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT));

            // Ícone Temático à Esquerda
            Color iconTint = resolveCategoryColor(item.category());
            JLabel iconLabel = new JLabel(UIHelper.icon(item.iconCode(), 20, iconTint));
            iconLabel.setPreferredSize(new Dimension(32, 32));
            panel.add(iconLabel, BorderLayout.WEST);

            // Centro: Título, Categoria e Subtítulo
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleRow.setOpaque(false);

            JLabel titleLabel = new JLabel(item.title());
            titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
            titleLabel.setForeground(UIHelper.TEXT_LIGHT);
            titleRow.add(titleLabel);

            JLabel catBadge = new JLabel(" " + item.category() + " ");
            catBadge.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            catBadge.setOpaque(true);
            catBadge.setBackground(iconTint);
            catBadge.setForeground(UIHelper.readableTextOn(iconTint));
            titleRow.add(catBadge);

            center.add(titleRow);

            if (item.subtitle() != null && !item.subtitle().isBlank()) {
                JLabel subLabel = new JLabel(item.subtitle());
                subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
                subLabel.setForeground(UIHelper.TEXT_MUTED);
                center.add(Box.createVerticalStrut(2));
                center.add(subLabel);
            }

            panel.add(center, BorderLayout.CENTER);

            // Direita: Tempo decorrido
            JLabel timeLabel = new JLabel(item.formattedTimeAgo());
            timeLabel.setFont(new Font(UIHelper.FONT, Font.ITALIC, 11));
            timeLabel.setForeground(UIHelper.TEXT_MUTED);
            panel.add(timeLabel, BorderLayout.EAST);

            return panel;
        }

        private Color resolveCategoryColor(String category) {
            if (category == null) return UIHelper.ACCENT_BLUE;
            return switch (category.toLowerCase()) {
                case "cliente", "clientes" -> UIHelper.MODULE_CLIENTES;
                case "produto", "artigo", "stock" -> UIHelper.MODULE_STOCK;
                case "fatura", "cotação", "venda", "comercial" -> UIHelper.MODULE_COMERCIAL;
                case "pos", "caixa" -> UIHelper.MODULE_POS;
                case "compra", "fornecedor" -> UIHelper.MODULE_COMPRAS;
                case "colaborador", "rh" -> UIHelper.MODULE_HR;
                default -> UIHelper.ACCENT_CYAN;
            };
        }
    }
}
