package mz.multicore.erp.gui.components;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;

/**
 * Barra lateral de navegação para tabelas: <b>Topo · Página acima · Página abaixo · Fundo</b>.
 *
 * <p>Colocada <b>fora da tabela</b>, no lado direito (região EAST) do contentor que aloja o scroll —
 * o mesmo padrão do rodapé de listagem (que vai ao SOUTH). Não sobrepõe células.</p>
 *
 * <p><b>Auto-esconder:</b> só é visível quando a tabela transborda (há mais linhas do que cabem).
 * <b>Teclado:</b> Home/End/PageUp/PageDown na tabela fazem topo/fundo/página.</p>
 *
 * <p>Instalada centralmente por {@link UIHelper#styleScrollPane(JScrollPane)}. Ver
 * docs/TABELAS_NAVEGACAO_SPEC.md e docs/UI_TABELAS_UX_SPEC.md.</p>
 */
public final class TableNavigator {

    private static final String INSTALLED = "tableNavigator.installed";
    private static final String ATTACHED = "tableNavigator.attached";

    private TableNavigator() {}

    public static void install(JScrollPane scroll) {
        if (scroll == null || scroll.getViewport() == null) return;
        if (!(scroll.getViewport().getView() instanceof JTable table)) return;
        if (Boolean.TRUE.equals(scroll.getClientProperty(INSTALLED))) return;
        scroll.putClientProperty(INSTALLED, Boolean.TRUE);

        bindKeys(table, scroll.getVerticalScrollBar());

        scroll.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
                    && !Boolean.TRUE.equals(scroll.getClientProperty(ATTACHED))) {
                SwingUtilities.invokeLater(() -> attachToHost(scroll));
            }
        });
        attachToHost(scroll);
    }

    /** Adiciona a barra ao EAST do contentor do scroll (fora da tabela) e liga o auto-esconder. */
    private static void attachToHost(JScrollPane scroll) {
        if (Boolean.TRUE.equals(scroll.getClientProperty(ATTACHED))) return;
        Container host = scroll.getParent();
        if (!(host instanceof JComponent parent)) return;
        if (!(parent.getLayout() instanceof BorderLayout bl)) return;
        if (!BorderLayout.CENTER.equals(bl.getConstraints(scroll))) return;
        if (bl.getLayoutComponent(BorderLayout.EAST) != null) return;

        scroll.putClientProperty(ATTACHED, Boolean.TRUE);
        JScrollBar sb = scroll.getVerticalScrollBar();
        NavigationBar bar = buildBar(sb);
        parent.add(bar, BorderLayout.EAST);

        // Auto-esconder: só visível quando a lista transborda.
        Runnable sync = () -> {
            boolean show = overflowed(sb.getMinimum(), sb.getMaximum(), sb.getVisibleAmount());
            bar.sync(sb);
            if (bar.isVisible() != show) {
                bar.setVisible(show);
                parent.revalidate();
                parent.repaint();
            }
        };
        sb.getModel().addChangeListener(e -> sync.run());
        sync.run();

        parent.revalidate();
        parent.repaint();
    }

    /** true quando o conteúdo é maior do que a área visível (vale a pena navegar). */
    public static boolean overflowed(int min, int max, int extent) {
        return (max - min) > extent;
    }

    static boolean atTop(int value, int min) {
        return value <= min;
    }

    static boolean atBottom(int value, int max, int extent) {
        return value >= max - extent;
    }

    // ─── Acções de scroll (puras, testáveis sem display) ─────────────────────

    public static void top(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getMinimum());
    }

    public static void bottom(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getMaximum());
    }

    public static void pageUp(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getValue() - page(bar));
    }

    public static void pageDown(JScrollBar bar) {
        if (bar != null) bar.setValue(bar.getValue() + page(bar));
    }

    private static int page(JScrollBar bar) {
        int extent = bar.getVisibleAmount();
        int block = bar.getBlockIncrement();
        return Math.max(1, extent > 0 ? extent : block);
    }

    // ─── Teclado ──────────────────────────────────────────────────────────────

    private static void bindKeys(JTable table, JScrollBar sb) {
        InputMap im = table.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = table.getActionMap();
        bind(im, am, "tnTop", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), () -> top(sb));
        bind(im, am, "tnBottom", KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), () -> bottom(sb));
        bind(im, am, "tnPageUp", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), () -> pageUp(sb));
        bind(im, am, "tnPageDown", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), () -> pageDown(sb));
    }

    private static void bind(InputMap im, ActionMap am, String key, KeyStroke ks, Runnable action) {
        im.put(ks, key);
        am.put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ─── UI ──────────────────────────────────────────────────────────────────

    private static NavigationBar buildBar(JScrollBar bar) {
        return new NavigationBar(bar);
    }

    private static JButton navButton(String iconCode, String tooltip, Runnable action) {
        JButton b = new JButton(UIHelper.icon(iconCode, 13, UIHelper.TEXT_LIGHT));
        b.setRolloverIcon(UIHelper.icon(iconCode, 13, UIHelper.ACCENT));
        b.setToolTipText(tooltip);
        b.getAccessibleContext().setAccessibleName(tooltip);
        b.setPreferredSize(new Dimension(28, 24));
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    private static final class NavigationBar extends JPanel {
        private final JButton top;
        private final JButton pageUp;
        private final JButton pageDown;
        private final JButton bottom;

        NavigationBar(JScrollBar scrollBar) {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            top = navButton("fas-angle-double-up", "Ir para o início da lista (Home)",
                    () -> TableNavigator.top(scrollBar));
            pageUp = navButton("fas-angle-up", "Subir uma página (Page Up)",
                    () -> TableNavigator.pageUp(scrollBar));
            pageDown = navButton("fas-angle-down", "Descer uma página (Page Down)",
                    () -> TableNavigator.pageDown(scrollBar));
            bottom = navButton("fas-angle-double-down", "Ir para o fim da lista (End)",
                    () -> TableNavigator.bottom(scrollBar));
            Strip cluster = new Strip();
            cluster.add(top);
            cluster.add(pageUp);
            cluster.add(pageDown);
            cluster.add(bottom);
            add(cluster, new GridBagConstraints());
        }

        void sync(JScrollBar scrollBar) {
            boolean start = atTop(scrollBar.getValue(), scrollBar.getMinimum());
            boolean end = atBottom(scrollBar.getValue(), scrollBar.getMaximum(),
                    scrollBar.getVisibleAmount());
            top.setEnabled(!start);
            pageUp.setEnabled(!start);
            pageDown.setEnabled(!end);
            bottom.setEnabled(!end);
        }
    }

    private static final class Strip extends JPanel {
        Strip() {
            setOpaque(false);
            setLayout(new GridLayout(4, 1, 0, 3));
            setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
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
}
