package com.phcpro.gui;

import com.phcpro.gui.components.UIHelper;

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
    static final double[] HEADER_FIELD_WEIGHTS = {0.20, 0.22, 0.16, 0.18, 0.24};

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
        JPanel box = new JPanel(new BorderLayout(8, 0));
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
        return BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(line, thickness, true),
                new EmptyBorder(0, 10 - (thickness - 1), 0, 8));
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

}
