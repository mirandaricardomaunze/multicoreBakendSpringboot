package mz.multicore.erp.gui.components;

import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

/** Estilos canónicos de abas (JTabbedPane): clássico e Multicore. */
final class UiTabbedPaneStyler {

    private UiTabbedPaneStyler() {}

    static void styleTabbedPane(JTabbedPane tabbedPane) {
        prepare(tabbedPane);
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
            protected javax.swing.JButton createScrollButton(int direction) {
                return UiTabScrollSupport.button(direction);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? UIHelper.ACCENT : UIHelper.BG_CARD);
                g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, UIHelper.RADIUS_SM, UIHelper.RADIUS_SM);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setFont(font);
                g.setColor(isSelected ? Color.WHITE : UIHelper.TEXT_MUTED);
                int mnemonicIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemonicIndex, textRect.x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (!isSelected) return;
                g.setColor(UIHelper.ACCENT_BLUE);
                g.drawLine(x + 8, y + h - 2, x + w - 8, y + h - 2);
            }

            @Override protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {}
            @Override protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}
        });
    }

    static void styleTabbedPaneMulticore(JTabbedPane tabbedPane) {
        prepare(tabbedPane);
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(4, 0, 0, 0);
                tabInsets = new Insets(8, 10, 8, 10);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            /** "Mais ▾" lists the tabs that do not fit, instead of arrows that hide them. */
            @Override
            protected javax.swing.JButton createScrollButton(int direction) {
                return UiTabOverflow.button(tabPane, direction);
            }

            /** One quiet icon colour: muted, accent only on the selected tab. */
            @Override
            protected void paintIcon(Graphics g, int tabPlacement, int tabIndex, javax.swing.Icon icon, Rectangle iconRect, boolean isSelected) {
                if (icon == null) return;
                UiTabIcons.tinted(icon, isSelected ? UIHelper.ACCENT : UIHelper.TEXT_MUTED).paintIcon(tabPane, g, iconRect.x, iconRect.y);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? new Color(
                        (UIHelper.BG_DARK.getRed() + UIHelper.BG_CARD.getRed()) / 2,
                        (UIHelper.BG_DARK.getGreen() + UIHelper.BG_CARD.getGreen()) / 2,
                        (UIHelper.BG_DARK.getBlue() + UIHelper.BG_CARD.getBlue()) / 2) : UIHelper.BG_DARK);
                g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, UIHelper.RADIUS_SM, UIHelper.RADIUS_SM);
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (!isSelected) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIHelper.ACCENT);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 6, y + h - 2, x + w - 6, y + h - 2);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                // Widths are measured in bold so tabs never jump; only the selected one is drawn bold.
                Font drawn = isSelected ? font : font.deriveFont(Font.PLAIN);
                FontMetrics drawnMetrics = g.getFontMetrics(drawn);
                int x = textRect.x + (textRect.width - drawnMetrics.stringWidth(title)) / 2;
                g.setFont(drawn);
                g.setColor(isSelected ? UIHelper.TEXT_LIGHT : UIHelper.TEXT_MUTED);
                int mnemonicIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemonicIndex, x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                int tabH = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g.setColor(UIHelper.GRID);
                g.fillRect(0, tabH - 1, tabPane.getWidth(), 1);
            }

            @Override protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}
        });
        UiTabMemory.install(tabbedPane);
    }

    private static void prepare(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(UIHelper.BG_DARK);
        tabbedPane.setForeground(UIHelper.TEXT_LIGHT);
        tabbedPane.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        // SCROLL_TAB_LAYOUT evita a 2.ª linha de abas inacessível com muitas abas.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        UiTabScrollSupport.installMouseWheel(tabbedPane);
        UiTabScrollSupport.keepSelectedTabVisible(tabbedPane);
    }
}
