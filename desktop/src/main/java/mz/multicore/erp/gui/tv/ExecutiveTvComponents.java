package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.SlimScrollBarUI;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * Peças visuais do painel de TV Executiva: cartão, mosaico de KPI, crachá, botões e estado vazio.
 * Cores sempre de {@link ExecutiveTvTheme}; ícones FontAwesome via {@link UIHelper#icon}.
 */
public final class ExecutiveTvComponents {

    public static final int RADIUS = 16;
    public static final int GAP = 14;
    public static final int CONTROL_HEIGHT = 38;

    private ExecutiveTvComponents() {}

    public static Font font(int style, int size) {
        return new Font(UIHelper.FONT, style, size);
    }

    public static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    /** Rótulo pequeno em maiúsculas com espaçamento entre letras (títulos de KPI). */
    public static JLabel overline(String text, boolean lightMode) {
        JLabel label = label(text.toUpperCase(), Font.BOLD, 11, ExecutiveTvTheme.getTextMutedColor(lightMode));
        label.setFont(label.getFont().deriveFont(Map.of(TextAttribute.TRACKING, 0.06f)));
        return label;
    }

    /** Cartão arredondado; o contorno é desenhado com o mesmo raio do fundo. */
    public static JPanel card(boolean lightMode, LayoutManager layout, int padding) {
        JPanel card = new RoundedSurface(layout, ExecutiveTvTheme.getCardBgColor(lightMode),
                ExecutiveTvTheme.getCardBorderColor(lightMode), RADIUS);
        card.setBorder(new EmptyBorder(padding, padding + 2, padding, padding + 2));
        return card;
    }

    public static JPanel sectionHeader(String title, String caption, boolean lightMode) {
        JPanel header = transparent(new BorderLayout(12, 0));
        header.add(label(title, Font.BOLD, 15, ExecutiveTvTheme.getTextLightColor(lightMode)), BorderLayout.WEST);
        if (caption != null && !caption.isBlank()) {
            header.add(label(caption, Font.PLAIN, 12, ExecutiveTvTheme.getTextMutedColor(lightMode)), BorderLayout.EAST);
        }
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        return header;
    }

    /** Crachá de estado: fundo suave da cor semântica, texto na própria cor. */
    public static JLabel pill(String text, Color accent, boolean lightMode) {
        Color base = ExecutiveTvTheme.getCardBgColor(lightMode);
        Color fill = ExecutiveTvTheme.tint(accent, base, lightMode ? 0.12 : 0.18);
        Color stroke = ExecutiveTvTheme.tint(accent, base, lightMode ? 0.35 : 0.40);
        Color fg = lightMode ? ExecutiveTvTheme.tint(Color.BLACK, accent, 0.25) : ExecutiveTvTheme.tint(Color.WHITE, accent, 0.15);
        JLabel pill = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = antialiased(g);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setFont(font(Font.BOLD, 11));
        pill.setForeground(fg);
        pill.setBorder(new EmptyBorder(3, 10, 3, 10));
        return pill;
    }

    public static JPanel kpiTile(String iconCode, String title, String value, String caption, Color accent, boolean lightMode) {
        JPanel tile = card(lightMode, new BorderLayout(0, 4), 12);
        JPanel top = transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (iconCode != null) {
            top.add(iconChip(iconCode, accent, lightMode));
            top.add(Box.createHorizontalStrut(10));
        }
        top.add(overline(title, lightMode));
        tile.add(top, BorderLayout.NORTH);
        JLabel valueLabel = label(value, Font.BOLD, 22, ExecutiveTvTheme.getTextLightColor(lightMode));
        valueLabel.setToolTipText(value);
        tile.add(valueLabel, BorderLayout.CENTER);
        tile.add(label(caption, Font.PLAIN, 12, ExecutiveTvTheme.getTextMutedColor(lightMode)), BorderLayout.SOUTH);
        return tile;
    }

    public static JComponent iconChip(String iconCode, Color accent, boolean lightMode) {
        JPanel chip = new RoundedSurface(new BorderLayout(),
                ExecutiveTvTheme.tint(accent, ExecutiveTvTheme.getCardBgColor(lightMode), lightMode ? 0.12 : 0.20), null, 10);
        JLabel icon = new JLabel(UIHelper.icon(iconCode, 14, accent), SwingConstants.CENTER);
        chip.add(icon, BorderLayout.CENTER);
        chip.setPreferredSize(new Dimension(30, 30));
        chip.setMaximumSize(new Dimension(30, 30));
        return chip;
    }

    /** Botão só com ícone (ações do cabeçalho); o texto vai para a dica e para leitores de ecrã. */
    public static ModernButton iconButton(String iconCode, String tooltip, Runnable action, boolean lightMode) {
        ModernButton button = ghostButton("", iconCode, ExecutiveTvTheme.getTextLightColor(lightMode), action, lightMode);
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(CONTROL_HEIGHT, CONTROL_HEIGHT));
        return button;
    }

    public static ModernButton ghostButton(String text, String iconCode, Color iconColor, Runnable action, boolean lightMode) {
        ModernButton button = new ModernButton(text);
        Color base = ExecutiveTvTheme.getSurfaceAltColor(lightMode);
        button.setColors(base, ExecutiveTvTheme.tint(ExecutiveTvTheme.getTextLightColor(lightMode), base, 0.10));
        button.setIcon(UIHelper.icon(iconCode, 15, iconColor)); // depois de setColors, que recolore ícones
        button.setFont(font(Font.BOLD, 13));
        button.setCornerRadius(10);
        button.addActionListener(e -> action.run());
        if (!text.isEmpty()) {
            Dimension pref = button.getPreferredSize();
            button.setPreferredSize(new Dimension(Math.max(130, pref.width + 16), CONTROL_HEIGHT));
        }
        return button;
    }

    /** Selector no tema do painel (o estilo global do ERP é claro e ficaria branco no tema escuro). */
    public static void styleCombo(JComboBox<?> combo, int width, boolean lightMode) {
        Color field = ExecutiveTvTheme.getSurfaceAltColor(lightMode);
        combo.setFont(font(Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(width, CONTROL_HEIGHT));
        combo.setBackground(field);
        combo.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        combo.putClientProperty("FlatLaf.style", String.format(
                "arc: 10; borderColor: %s; buttonBackground: %s; buttonArrowColor: %s; popupBackground: %s",
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getCardBorderColor(lightMode)), ExecutiveTvTheme.hex(field),
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getTextMutedColor(lightMode)),
                ExecutiveTvTheme.hex(ExecutiveTvTheme.getCardBgColor(lightMode))));
    }

    public static JPanel emptyState(String iconCode, String title, String caption, boolean lightMode) {
        JPanel box = transparent(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Box.createVerticalGlue());
        for (JComponent c : new JComponent[]{
                new JLabel(UIHelper.icon(iconCode, 34, ExecutiveTvTheme.getTextFaintColor(lightMode))),
                label(title, Font.BOLD, 15, ExecutiveTvTheme.getTextLightColor(lightMode)),
                label(caption, Font.PLAIN, 12, ExecutiveTvTheme.getTextMutedColor(lightMode))}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(c);
            box.add(Box.createVerticalStrut(8));
        }
        box.add(Box.createVerticalGlue());
        return box;
    }

    /**
     * Rolagem só vertical: o conteúdo acompanha a largura disponível (sem barra horizontal)
     * e a barra é fina e transparente, como no resto do ERP.
     */
    public static JScrollPane verticalScroll(JComponent content) {
        WidthTrackingPanel holder = new WidthTrackingPanel();
        holder.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    public static JComponent divider(boolean lightMode) {
        JPanel line = new JPanel();
        line.setOpaque(true);
        line.setBackground(ExecutiveTvTheme.getCardBorderColor(lightMode));
        line.setPreferredSize(new Dimension(1, 24));
        return line;
    }

    public static JPanel transparent(LayoutManager layout) {
        JPanel panel = layout != null ? new JPanel(layout) : new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    static Graphics2D antialiased(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Superfície arredondada não opaca (o canto mostra o fundo do pai). */
    static final class RoundedSurface extends JPanel {
        private final Color fill;
        private final Color stroke;
        private final int radius;

        RoundedSurface(LayoutManager layout, Color fill, Color stroke, int radius) {
            super(layout);
            this.fill = fill;
            this.stroke = stroke;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = antialiased(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (stroke != null) {
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
