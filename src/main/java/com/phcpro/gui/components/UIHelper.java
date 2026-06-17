package com.phcpro.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UIHelper {

    public static final Color BG_DARK = new Color(17, 24, 39);      // Gray-900 (#111827)
    public static final Color BG_CARD = new Color(31, 41, 55);      // Gray-800 (#1F2937)
    public static final Color TEXT_LIGHT = new Color(243, 244, 246); // Gray-100 (#F3F4F6)
    public static final Color TEXT_MUTED = new Color(156, 163, 175); // Gray-400 (#9CA3AF)
    public static final Color ACCENT = new Color(139, 92, 246);      // Violet-500 (#8B5CF6)
    public static final Color ACCENT_BLUE = new Color(59, 130, 246); // Blue-500 (#3B82F6)
    public static final Color APPROVED_GREEN = new Color(16, 185, 129); // Emerald-500 (#10B981)
    public static final Color REJECTED_RED = new Color(239, 68, 68);    // Red-500 (#EF4444)
    public static final Color PENDING_YELLOW = new Color(245, 158, 11);  // Amber-500 (#F59E0B)
    private static final Color SECONDARY = new Color(75, 85, 99);       // Gray-600 (#4B5563)
    private static final Color SECONDARY_HOVER = new Color(107, 114, 128); // Gray-500 (#6B7280)
    public static final int FORM_CONTROL_HEIGHT = 38;
    public static final int DIALOG_FORM_MIN_WIDTH = 560;

    public static ModernButton createPrimaryButton(String text) {
        return new ModernButton(text, ACCENT_BLUE, ACCENT_BLUE.brighter());
    }

    public static ModernButton createSuccessButton(String text) {
        return new ModernButton(text, APPROVED_GREEN, APPROVED_GREEN.brighter());
    }

    public static ModernButton createDangerButton(String text) {
        return new ModernButton(text, REJECTED_RED, REJECTED_RED.brighter());
    }

    public static ModernButton createSecondaryButton(String text) {
        return new ModernButton(text, SECONDARY, SECONDARY_HOVER);
    }

    public static ModernButton createAddLineButton() {
        ModernButton button = createPrimaryButton("Adicionar Linha");
        button.setIcon(icon("fas-plus", 14));
        button.setPreferredSize(new Dimension(180, FORM_CONTROL_HEIGHT));
        button.setMinimumSize(new Dimension(180, FORM_CONTROL_HEIGHT));
        return button;
    }

    public static void initGlobalTheme() {
        try {
            // Style OptionPane and dialogs for Dark Theme
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("OptionPane.background", BG_DARK);
            UIManager.put("OptionPane.messageForeground", TEXT_LIGHT);
            UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.BOLD, 13));
            UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 12));
            
            // Buttons inside dialogs
            UIManager.put("Button.background", BG_CARD);
            UIManager.put("Button.foreground", TEXT_LIGHT);
            UIManager.put("Button.select", new Color(55, 65, 81));
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            
            // TabbedPane dark theme consistency
            UIManager.put("TabbedPane.background", BG_CARD);
            UIManager.put("TabbedPane.foreground", TEXT_LIGHT);
            UIManager.put("TabbedPane.selected", ACCENT);
            UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
            UIManager.put("TabbedPane.focusInputMap", new UIDefaults.LazyInputMap(new Object[]{}));
            UIManager.put("TabbedPane.shadow", BG_DARK);
            UIManager.put("TabbedPane.darkShadow", BG_DARK);
            UIManager.put("TabbedPane.light", BG_CARD);
            UIManager.put("TabbedPane.highlight", BG_CARD);
            UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
            
            // Labels
            UIManager.put("Label.foreground", TEXT_LIGHT);
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));

            // ComboBox and TextField
            UIManager.put("ComboBox.background", BG_CARD);
            UIManager.put("ComboBox.foreground", TEXT_LIGHT);
            UIManager.put("ComboBox.selectionBackground", new Color(55, 65, 81));
            UIManager.put("ComboBox.selectionForeground", TEXT_LIGHT);
            UIManager.put("TextField.background", BG_CARD);
            UIManager.put("TextField.foreground", TEXT_LIGHT);
            UIManager.put("TextField.caretForeground", TEXT_LIGHT);

            // PasswordField and TextArea
            UIManager.put("PasswordField.background", BG_CARD);
            UIManager.put("PasswordField.foreground", TEXT_LIGHT);
            UIManager.put("PasswordField.caretForeground", TEXT_LIGHT);
            UIManager.put("TextArea.background", BG_DARK);
            UIManager.put("TextArea.foreground", TEXT_LIGHT);
            UIManager.put("TextArea.caretForeground", TEXT_LIGHT);
        } catch (Exception ignored) {}
    }

    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(BG_DARK);
        tabbedPane.setForeground(TEXT_LIGHT);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
                tabInsets = new Insets(9, 16, 9, 16);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? ACCENT : BG_CARD);
                g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                     int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setFont(font);
                g.setColor(isSelected ? Color.WHITE : TEXT_MUTED);
                int mnemonicIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemonicIndex,
                        textRect.x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                if (!isSelected) {
                    return;
                }
                g.setColor(ACCENT_BLUE);
                g.drawLine(x + 8, y + h - 2, x + w - 8, y + h - 2);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // The surrounding panels already provide the app frame.
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // Avoid low-contrast focus rectangles from the platform Look & Feel.
            }
        });
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_LIGHT);
        table.setGridColor(new Color(55, 65, 81)); // Gray-700
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setSelectionBackground(new Color(55, 65, 81));
        table.setSelectionForeground(TEXT_LIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setBorder(BorderFactory.createEmptyBorder());

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(15, 23, 42)); // Dark Slate
        header.setForeground(TEXT_LIGHT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(100, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(55, 65, 81)));

        // Center / Left cell alignments and paddings
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10)); // Add horizontal padding to cell text
                
                if (isSelected) {
                    setBackground(t.getSelectionBackground());
                } else {
                    setBackground(row % 2 == 0 ? BG_CARD : new Color(24, 32, 47));
                }
                
                // Colorize status values if present
                if (value != null) {
                    String valStr = value.toString();
                    if (valStr.length() > 30) {
                        setToolTipText("<html><body style='width: 250px; font-family: Segoe UI; font-size: 11px; padding: 4px;'>" 
                                       + valStr.replace("\n", "<br>") + "</body></html>");
                    } else {
                        setToolTipText(valStr);
                    }

                    String upperStr = valStr.toUpperCase();
                    if (upperStr.equals("APPROVED") || upperStr.equals("APROVADO") || upperStr.equals("RESOLVED") || upperStr.equals("PAID") || upperStr.equals("PAGO")) {
                        setForeground(APPROVED_GREEN);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (upperStr.equals("REJECTED") || upperStr.equals("REJEITADO")) {
                        setForeground(REJECTED_RED);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (upperStr.contains("PENDING") || upperStr.contains("PENDENTE")) {
                        setForeground(PENDING_YELLOW);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(TEXT_LIGHT);
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                } else {
                    setToolTipText(null);
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
        table.setDefaultRenderer(Number.class, cellRenderer);
        table.setDefaultRenderer(String.class, cellRenderer);

        // Double click to view details
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    showRowDetailsDialog(table);
                }
            }
        });
    }

    public static void showRowDetailsDialog(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;

        Window parentWindow = SwingUtilities.getWindowAncestor(table);
        JDialog dialog = new JDialog(parentWindow, "Detalhes do Registo", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setSize(600, 480);
        dialog.setLocationRelativeTo(parentWindow);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = createHeading("Detalhes do Registo");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int rowCount = 0;
        for (int col = 0; col < table.getColumnCount(); col++) {
            // Skip columns with 0 width (like hidden ID column)
            if (table.getColumnModel().getColumn(col).getWidth() == 0 && 
                table.getColumnModel().getColumn(col).getMaxWidth() == 0) {
                continue;
            }

            String colName = table.getColumnName(col);
            Object colVal = table.getValueAt(selectedRow, col);
            String valStr = (colVal != null) ? colVal.toString() : "";

            // Label for column name
            gbc.gridx = 0;
            gbc.gridy = rowCount;
            gbc.weightx = 0.3;
            JLabel nameLabel = new JLabel(colName + ":");
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nameLabel.setForeground(ACCENT);
            fieldsPanel.add(nameLabel, gbc);

            // Component for value
            gbc.gridx = 1;
            gbc.weightx = 0.7;
            if (valStr.length() > 50 || valStr.contains("\n")) {
                JTextArea valArea = new JTextArea(valStr);
                valArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                valArea.setBackground(BG_CARD);
                valArea.setForeground(TEXT_LIGHT);
                valArea.setEditable(false);
                valArea.setLineWrap(true);
                valArea.setWrapStyleWord(true);
                valArea.setCaretColor(TEXT_LIGHT);
                
                JScrollPane scrollArea = new JScrollPane(valArea);
                scrollArea.setPreferredSize(new Dimension(350, 80));
                scrollArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(75, 85, 99), 1),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)
                ));
                fieldsPanel.add(scrollArea, gbc);
            } else {
                JTextField valField = new JTextField(valStr);
                valField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                valField.setBackground(BG_CARD);
                valField.setForeground(TEXT_LIGHT);
                valField.setEditable(false);
                valField.setCaretColor(TEXT_LIGHT);
                valField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(75, 85, 99), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                fieldsPanel.add(valField, gbc);
            }
            rowCount++;
        }

        JScrollPane centerScroll = new JScrollPane(fieldsPanel);
        centerScroll.setBorder(null);
        centerScroll.getViewport().setBackground(BG_DARK);
        mainPanel.add(centerScroll, BorderLayout.CENTER);

        // Close button at bottom
        ModernButton closeBtn = createSecondaryButton("Fechar");
        closeBtn.setPreferredSize(new Dimension(100, 35));
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(closeBtn);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Vector icon helper backed by Ikonli + FontAwesome 5.
     * Use FontAwesome icon codes like "fas-users", "fas-print", "fas-file-pdf".
     * See https://fontawesome.com/v5/search?o=r&m=free for the catalogue.
     */
    public static javax.swing.Icon icon(String code, int size, Color color) {
        org.kordamp.ikonli.swing.FontIcon fi = org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.valueOf(toFaEnum(code)));
        fi.setIconSize(size);
        fi.setIconColor(color);
        return fi;
    }

    public static javax.swing.Icon icon(String code, int size) {
        return icon(code, size, Color.WHITE);
    }

    /** Maps "fas-cart-plus" → "CART_PLUS" for the FontAwesomeSolid enum. */
    private static String toFaEnum(String code) {
        String s = code.startsWith("fas-") ? code.substring(4) : code;
        return s.replace('-', '_').toUpperCase();
    }

    /**
     * Renders a FontAwesome icon as a square AWT {@link java.awt.Image} suitable for
     * {@code JFrame.setIconImage(...)} / {@code JDialog.setIconImage(...)}.
     */
    public static java.awt.Image iconImage(String code, int size, Color color) {
        javax.swing.Icon ic = icon(code, size, color);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        ic.paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    public static void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81), 1));
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setBackground(BG_DARK);
        scroll.getHorizontalScrollBar().setBackground(BG_DARK);
    }

    /**
     * Wraps tall modal-dialog content in a vertical scroll pane capped to ~78% of
     * screen height so the OK/Cancel buttons stay visible on small displays.
     * If the content already fits, it is returned unchanged.
     */
    public static JComponent makeDialogScrollable(JPanel content) {
        Dimension contentSize = content.getPreferredSize();
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        int maxHeight = (int) (screenHeight * 0.78);
        if (contentSize.height <= maxHeight) {
            return content;
        }
        int width = Math.max(contentSize.width, DIALOG_FORM_MIN_WIDTH) + 24;
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(width, maxHeight));
        return scroll;
    }

    public static void styleEmbeddedTableScrollPane(JScrollPane scroll, JTable table, int visibleRows) {
        styleScrollPane(scroll);
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(420, table.getRowHeight() * visibleRows));
        int headerHeight = table.getTableHeader() != null
                ? table.getTableHeader().getPreferredSize().height
                : 0;
        int height = headerHeight + (table.getRowHeight() * visibleRows) + 26;
        scroll.setPreferredSize(new Dimension(420, height));
        scroll.setMinimumSize(new Dimension(280, height));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    public static void styleTextField(JTextField field) {
        field.setBackground(BG_CARD);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(75, 85, 99), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        applyFormControlHeight(field);
    }

    public static void stylePasswordField(JPasswordField field) {
        field.setBackground(BG_CARD);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(75, 85, 99), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        applyFormControlHeight(field);
    }

    public static void styleTextArea(JTextArea area) {
        area.setBackground(BG_CARD);
        area.setForeground(TEXT_LIGHT);
        area.setCaretColor(TEXT_LIGHT);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(75, 85, 99), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
    }

    public static void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_LIGHT);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99), 1));
        applyFormControlHeight(combo);
        // Simple UI cell renderer for elements in dropdown list
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(55, 65, 81) : BG_CARD);
                setForeground(TEXT_LIGHT);
                setBorder(new EmptyBorder(5, 8, 5, 8));
                return this;
            }
        });
    }

    private static void applyFormControlHeight(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        int width = Math.max(preferred.width, component.getMinimumSize().width);
        Dimension uniform = new Dimension(width, FORM_CONTROL_HEIGHT);
        component.setPreferredSize(uniform);
        component.setMinimumSize(new Dimension(0, FORM_CONTROL_HEIGHT));
    }

    public static JLabel createHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setForeground(TEXT_LIGHT);
        return label;
    }

    public static JLabel createSubheading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(ACCENT);
        return label;
    }

    public static JPanel createDialogForm(Object... labelsAndComponents) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        
        int row = 0;
        for (int i = 0; i < labelsAndComponents.length; i += 2) {
            Object labelObj = labelsAndComponents[i];
            Object compObj = labelsAndComponents[i + 1];
            
            // Label
            gbc.gridy = row++;
            gbc.insets = new Insets(8, 0, 2, 0);
            if (labelObj instanceof String) {
                JLabel lbl = new JLabel((String) labelObj);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(ACCENT);
                panel.add(lbl, gbc);
            } else if (labelObj instanceof Component) {
                panel.add((Component) labelObj, gbc);
            }
            
            // Component
            gbc.gridy = row++;
            gbc.insets = new Insets(2, 0, 12, 0);
            if (compObj instanceof Component) {
                Component c = (Component) compObj;
                if (c instanceof JTextField) {
                    if (c instanceof JPasswordField) {
                        stylePasswordField((JPasswordField) c);
                    } else {
                        styleTextField((JTextField) c);
                    }
                } else if (c instanceof JComboBox) {
                    styleComboBox((JComboBox<?>) c);
                } else if (c instanceof JTextArea) {
                    styleTextArea((JTextArea) c);
                }
                panel.add(c, gbc);
            }
        }
        Dimension preferred = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.max(DIALOG_FORM_MIN_WIDTH, preferred.width), preferred.height));
        panel.setMinimumSize(new Dimension(DIALOG_FORM_MIN_WIDTH, preferred.height));
        return panel;
    }
}
