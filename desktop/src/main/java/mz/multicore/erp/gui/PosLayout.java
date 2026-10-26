package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Construtores de layout puros do formulário POS. */
final class PosLayout {
    static final int CART_COMFORTABLE_WIDTH = 620;
    static final int ROOT_VERTICAL_MARGIN = 14;
    static final int SECTION_VERTICAL_GAP = 6;
    static final int CARD_VERTICAL_GAP = 8;
    static final double[] HEADER_FIELD_WEIGHTS = {0.18, 0.20, 0.19, 0.19, 0.24};

    private PosLayout() {}

    static int cartAutoResizeMode(int viewportWidth) {
        return JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
    }

    static void updateCartTableResizeMode(JTable table, int viewportWidth) {
        int mode = cartAutoResizeMode(viewportWidth);
        if (table.getAutoResizeMode() != mode) {
            table.setAutoResizeMode(mode);
        }
    }

    static void configureOperationalCartColumns(JTable table) {
        int[] widths = {250, 55, 90, 65, 105, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    static JPanel stackedPicker(JComponent top, JComponent bottom) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(bottom, BorderLayout.CENTER);
        return p;
    }

    /** Campo de pesquisa com ícone de lupa vectorial **dentro** do input (caixa única, aspecto profissional). */
    static JPanel searchRow(JTextField field) {
        return iconInputBox("fas-search", 13, UIHelper.TEXT_MUTED, field);
    }

    /**
     * Campo de texto de altura única ({@code FORM_CONTROL_HEIGHT}) com um ícone vectorial **dentro**
     * do input, à esquerda. Base de {@link #searchRow} (lupa) e do campo de código de barras
     * (`fas-barcode`), para todos alinharem com os combos do cabeçalho.
     */
    static JPanel iconInputBox(String iconCode, int iconSize, Color iconColor, JTextField field) {
        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UIHelper.CONTROL_ARC, UIHelper.CONTROL_ARC);
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setBackground(UIHelper.FIELD_BG);
        box.setBorder(iconBoxBorder(UIHelper.BORDER, 1));

        JLabel icon = new JLabel(UIHelper.icon(iconCode, iconSize, iconColor));
        box.add(icon, BorderLayout.WEST);

        // O campo herda o aspecto da caixa: sem borda/fundo próprios para o ícone parecer dentro do
        // input. O realce de foco vive na CAIXA (o campo opta por não desenhar a sua borda de foco).
        field.putClientProperty("noFocusBorder", Boolean.TRUE);
        field.setBorder(new EmptyBorder(6, 8, 6, 0));
        field.setOpaque(false);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { box.setBorder(iconBoxBorder(UIHelper.ACCENT, 2)); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { box.setBorder(iconBoxBorder(UIHelper.BORDER, 1)); }
        });
        box.add(field, BorderLayout.CENTER);

        int h = UIHelper.FORM_CONTROL_HEIGHT;
        box.setPreferredSize(new Dimension(box.getPreferredSize().width, h));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        return box;
    }

    static javax.swing.border.Border iconBoxBorder(Color line, int thickness) {
        return UIHelper.fieldBorder(line, thickness);
    }

    static int addSectionHeader(JPanel host, GridBagConstraints gbc, int row, String text) {
        JLabel section = new JLabel(text);
        section.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        section.setForeground(UIHelper.ACCENT);
        section.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIHelper.GRID));

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.weighty = 0.0;
        gbc.insets = new Insets(row == 0 ? 0 : 14, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        host.add(section, gbc);
        return row + 1;
    }

    static int addFullRowField(JPanel host, GridBagConstraints gbc, int row, String label, JComponent control) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setForeground(UIHelper.TEXT_MUTED);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 6, 2, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        host.add(lbl, gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 6, 10, 6);
        host.add(control, gbc);
        return row + 2;
    }

    static int addTwoColumnRow(JPanel host, GridBagConstraints gbc, int row,
                                        String leftLabel, JComponent leftControl,
                                        String rightLabel, JComponent rightControl) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(4, 6, 2, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel l = new JLabel(leftLabel + ":");
        JLabel r = new JLabel(rightLabel + ":");
        l.setForeground(UIHelper.TEXT_MUTED);
        r.setForeground(UIHelper.TEXT_MUTED);

        gbc.gridx = 0; gbc.gridy = row;     host.add(l, gbc);
        gbc.gridx = 1;                       host.add(r, gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 6, 10, 6);
        gbc.gridx = 0; host.add(leftControl, gbc);
        gbc.gridx = 1; host.add(rightControl, gbc);
        return row + 2;
    }

    static mz.multicore.erp.gui.components.ModernPanel createSessionBanner(JLabel statusLabel, JComponent trailing) {
        mz.multicore.erp.gui.components.ModernPanel banner = new mz.multicore.erp.gui.components.ModernPanel(10);
        banner.setLayout(new BorderLayout(12, 0));
        banner.setBorder(new EmptyBorder(4, 12, 4, 6));
        banner.add(statusLabel, BorderLayout.WEST);
        if (trailing != null) {
            banner.add(trailing, BorderLayout.EAST);
        }
        return banner;
    }

    static mz.multicore.erp.gui.components.ModernPanel createSessionBanner(JLabel statusLabel) {
        return createSessionBanner(statusLabel, null);
    }

    static mz.multicore.erp.gui.components.ModernPanel createTotalsRow(JLabel subtotalValue, JLabel ivaValue, JLabel totalValue) {
        mz.multicore.erp.gui.components.ModernPanel totalRow = new mz.multicore.erp.gui.components.ModernPanel(12);
        boolean isLight = UIHelper.isLight();
        totalRow.setBackground(UIHelper.SELECTION_BG);
        totalRow.setLayout(new BorderLayout());
        totalRow.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel taxSummary = new JPanel(new GridLayout(2, 2, 12, 2));
        taxSummary.setOpaque(false);

        Color labelColor = isLight ? UIHelper.TEXT_MUTED : UIHelper.TEXT_LIGHT;
        Color valueColor = isLight ? UIHelper.TEXT_LIGHT : Color.WHITE;
        Color totalColor = isLight ? UIHelper.ACCENT_BLUE : Color.WHITE;

        JLabel subCap = new JLabel("Subtotal s/ IVA");
        subCap.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subCap.setForeground(labelColor);

        JLabel ivaCap = new JLabel("IVA");
        ivaCap.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        ivaCap.setForeground(labelColor);

        subtotalValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        subtotalValue.setForeground(valueColor);
        subtotalValue.setHorizontalAlignment(SwingConstants.RIGHT);

        ivaValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        ivaValue.setForeground(valueColor);
        ivaValue.setHorizontalAlignment(SwingConstants.RIGHT);

        taxSummary.add(subCap);
        taxSummary.add(subtotalValue);
        taxSummary.add(ivaCap);
        taxSummary.add(ivaValue);

        JLabel totalCaption = new JLabel("TOTAL A PAGAR");
        totalCaption.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        totalCaption.setForeground(labelColor);

        totalValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 26));
        totalValue.setForeground(totalColor);

        JPanel payable = new JPanel(new BorderLayout(14, 0));
        payable.setOpaque(false);
        Color dividerColor = UIHelper.BORDER;
        payable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, dividerColor),
                new EmptyBorder(0, 12, 0, 0)));
        payable.add(totalCaption, BorderLayout.WEST);
        payable.add(totalValue, BorderLayout.EAST);

        totalRow.add(taxSummary, BorderLayout.WEST);
        totalRow.add(payable, BorderLayout.EAST);
        return totalRow;
    }
}
