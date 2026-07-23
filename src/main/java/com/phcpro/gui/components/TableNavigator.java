package com.phcpro.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneLayout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Barra lateral de navegação para tabelas: <b>Topo · Página acima · Página abaixo · Fundo</b>.
 *
 * <p>Instalada centralmente por {@link UIHelper#styleScrollPane(JScrollPane)} — como quase todas as
 * tabelas do sistema passam por lá, uma só ligação cobre-as todas (DRY). Opera sobre a
 * {@link JScrollBar} vertical do scroll, pelo que é independente do modelo/filtro da tabela. Ver
 * docs/TABELAS_NAVEGACAO_SPEC.md.</p>
 */
public final class TableNavigator {

    private static final String INSTALLED = "tableNavigator.installed";

    private TableNavigator() {}

    /**
     * Liga a barra lateral a um scroll cujo conteúdo é uma {@link JTable}. Idempotente e seguro:
     * ignora scrolls sem tabela ou já equipados.
     */
    public static void install(JScrollPane scroll) {
        if (scroll == null || scroll.getViewport() == null) return;
        if (!(scroll.getViewport().getView() instanceof JTable)) return;
        if (Boolean.TRUE.equals(scroll.getClientProperty(INSTALLED))) return;
        scroll.putClientProperty(INSTALLED, Boolean.TRUE);

        JScrollBar bar = scroll.getVerticalScrollBar();
        Strip strip = new Strip();
        strip.add(navButton("fas-angle-double-up", "Ir para o topo", () -> top(bar)));
        strip.add(navButton("fas-angle-up", "Página acima", () -> pageUp(bar)));
        strip.add(navButton("fas-angle-down", "Página abaixo", () -> pageDown(bar)));
        strip.add(navButton("fas-angle-double-down", "Ir para o fundo", () -> bottom(bar)));

        NavLayout layout = new NavLayout(strip);
        scroll.setLayout(layout);
        layout.syncWithScrollPane(scroll);
        scroll.add(strip);
        scroll.setComponentZOrder(strip, 0); // sobre o viewport
        scroll.revalidate();
    }

    // ─── Acções de scroll (puras, testáveis sem display) ─────────────────────

    /** Rola para o topo da lista. */
    public static void top(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getMinimum());
    }

    /** Rola para o fundo da lista (a BoundedRangeModel limita a maximum - extent). */
    public static void bottom(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getMaximum());
    }

    /** Sobe uma página (altura visível). Limita ao topo. */
    public static void pageUp(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getValue() - page(bar));
    }

    /** Desce uma página (altura visível). Limita ao fundo. */
    public static void pageDown(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getValue() + page(bar));
    }

    private static int page(JScrollBar bar) {
        int extent = bar.getVisibleAmount();
        int block = bar.getBlockIncrement();
        return Math.max(1, extent > 0 ? extent : block);
    }

    // ─── UI ──────────────────────────────────────────────────────────────────

    private static JButton navButton(String iconCode, String tooltip, Runnable action) {
        JButton b = new JButton(UIHelper.icon(iconCode, 13, UIHelper.TEXT_LIGHT));
        b.setRolloverIcon(UIHelper.icon(iconCode, 13, UIHelper.ACCENT));
        b.setToolTipText(tooltip);
        b.setPreferredSize(new Dimension(28, 24));
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Cartão arredondado que alberga os 4 botões, para se ler como controlo flutuante. */
    private static final class Strip extends JPanel {
        Strip() {
            setOpaque(false);
            setLayout(new GridLayout(4, 1, 0, 3));
            setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIHelper.BG_CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(UIHelper.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * {@link ScrollPaneLayout} que mantém o comportamento normal do scroll e posiciona a barra
     * flutuante encostada à direita, centrada na vertical — sem ser gerida pelas ranhuras do scroll.
     */
    private static final class NavLayout extends ScrollPaneLayout {
        private final Component strip;

        NavLayout(Component strip) {
            this.strip = strip;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            if (name == null) return; // a barra é posicionada à mão em layoutContainer
            super.addLayoutComponent(name, comp);
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            if (comp == strip) return;
            super.removeLayoutComponent(comp);
        }

        @Override
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            if (strip == null || !strip.isVisible()) return;
            Insets in = parent.getInsets();
            Dimension ps = strip.getPreferredSize();
            int availH = parent.getHeight() - in.top - in.bottom;
            int h = Math.min(ps.height, availH);
            int x = parent.getWidth() - in.right - ps.width - 4;
            int y = in.top + Math.max(0, (availH - h) / 2);
            strip.setBounds(x, y, ps.width, h);
        }
    }
}
