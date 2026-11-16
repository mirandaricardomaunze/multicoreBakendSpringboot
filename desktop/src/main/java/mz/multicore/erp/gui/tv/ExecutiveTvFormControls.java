package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Controlos de formulário e tabela no tema do Painel Executivo. Os estilos globais do ERP
 * ({@code UIHelper.styleTextField}, {@code styleTable}) seguem o tema da aplicação e ficavam brancos
 * dentro de um modal escuro do painel. Tudo aqui lê {@link ExecutiveTvTheme}.
 */
final class ExecutiveTvFormControls {

    private ExecutiveTvFormControls() {}

    static JTextField textField(String text, int width, boolean lightMode) {
        JTextField field = new JTextField(text);
        Color surface = ExecutiveTvTheme.getSurfaceAltColor(lightMode);
        field.setFont(ExecutiveTvComponents.font(Font.PLAIN, 13));
        field.setBackground(surface);
        field.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        field.setCaretColor(ExecutiveTvTheme.getTextLightColor(lightMode));
        field.setDisabledTextColor(ExecutiveTvTheme.getTextFaintColor(lightMode));
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setPreferredSize(new Dimension(width, ExecutiveTvComponents.CONTROL_HEIGHT));
        field.putClientProperty("FlatLaf.style", String.format(
                "arc: 10; borderColor: %s; focusedBorderColor: %s; disabledBackground: %s; background: %s",
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getCardBorderColor(lightMode)), ExecutiveTvTheme.hex(UIHelper.ACCENT_BLUE),
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getCardBgColor(lightMode)), ExecutiveTvTheme.hex(surface)));
        return field;
    }

    static JCheckBox checkBox(boolean selected, boolean lightMode) {
        JCheckBox box = new JCheckBox();
        box.setSelected(selected);
        box.setOpaque(false);
        box.putClientProperty("FlatLaf.style", String.format(
                "icon.background: %s; icon.borderColor: %s; icon.selectedBackground: %s; icon.selectedBorderColor: %s",
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getSurfaceAltColor(lightMode)),
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getCardBorderColor(lightMode)),
                ExecutiveTvTheme.hex(UIHelper.ACCENT_BLUE), ExecutiveTvTheme.hex(UIHelper.ACCENT_BLUE)));
        return box;
    }

    /** Acção principal do modal (Gravar): verde de confirmação, texto branco em ambos os temas. */
    static ModernButton primaryButton(String text, String iconCode, Runnable action) {
        ModernButton button = new ModernButton(text);
        button.setColors(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
        button.setForeground(Color.WHITE);
        button.setIcon(UIHelper.icon(iconCode, 14, Color.WHITE));
        button.setFont(ExecutiveTvComponents.font(Font.BOLD, 13));
        button.setCornerRadius(10);
        button.addActionListener(e -> action.run());
        Dimension pref = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(130, pref.width + 16), ExecutiveTvComponents.CONTROL_HEIGHT));
        return button;
    }

    /** Tabela de leitura: cabeçalho, linhas alternadas e selecção no tema do painel. */
    static JScrollPane table(JTable table, boolean lightMode, int[] centered, int[] rightAligned) {
        Color card = ExecutiveTvTheme.getCardBgColor(lightMode);
        Color alt = ExecutiveTvTheme.getSurfaceAltColor(lightMode);
        Color text = ExecutiveTvTheme.getTextLightColor(lightMode);
        Color border = ExecutiveTvTheme.getCardBorderColor(lightMode);
        table.setFont(ExecutiveTvComponents.font(Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(border);
        table.setBackground(card);
        table.setForeground(text);
        table.setSelectionBackground(ExecutiveTvTheme.tint(UIHelper.ACCENT_BLUE, card, lightMode ? 0.18 : 0.35));
        table.setSelectionForeground(text);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                label.setFont(ExecutiveTvComponents.font(Font.BOLD, 12));
                label.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
                label.setBackground(alt);
                label.setOpaque(true);
                label.setHorizontalAlignment(alignmentOf(col, centered, rightAligned));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, border), new EmptyBorder(0, 10, 0, 10)));
                return label;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, selected, false, row, col);
                label.setBorder(new EmptyBorder(0, 10, 0, 10));
                label.setHorizontalAlignment(alignmentOf(col, centered, rightAligned));
                if (!selected) {
                    label.setBackground(row % 2 == 0 ? card : alt);
                    label.setForeground(text);
                }
                return label;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(border, 1, true));
        scroll.getViewport().setBackground(card);
        return scroll;
    }

    private static int alignmentOf(int col, int[] centered, int[] rightAligned) {
        for (int c : rightAligned) if (c == col) return SwingConstants.RIGHT;
        for (int c : centered) if (c == col) return SwingConstants.CENTER;
        return SwingConstants.LEFT;
    }
}
