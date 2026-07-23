package com.phcpro.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
import java.awt.event.HierarchyEvent;

/**
 * Barra lateral de navegação para tabelas: <b>Topo · Página acima · Página abaixo · Fundo</b>.
 *
 * <p>Colocada <b>fora da tabela</b>, no lado direito (região EAST) do contentor que aloja o scroll —
 * exactamente o mesmo padrão que o rodapé de listagem já usa (ver
 * {@code UIHelper.maybeAddListingFooter}, que adiciona ao SOUTH). Não sobrepõe células.</p>
 *
 * <p>Instalada centralmente por {@link UIHelper#styleScrollPane(JScrollPane)} — como quase todas as
 * tabelas passam por lá, uma só ligação cobre-as todas (DRY). As acções operam sobre a
 * {@link JScrollBar} vertical do scroll, pelo que são independentes do modelo/filtro. Ver
 * docs/TABELAS_NAVEGACAO_SPEC.md.</p>
 */
public final class TableNavigator {

    private static final String INSTALLED = "tableNavigator.installed";
    private static final String ATTACHED = "tableNavigator.attached";

    private TableNavigator() {}

    /**
     * Liga a barra a um scroll cujo conteúdo é uma {@link JTable}. A barra só é anexada quando o
     * scroll já está num contentor {@link BorderLayout} com a região EAST livre (adiada até lá).
     * Idempotente e segura.
     */
    public static void install(JScrollPane scroll) {
        if (scroll == null || scroll.getViewport() == null) return;
        if (!(scroll.getViewport().getView() instanceof JTable)) return;
        if (Boolean.TRUE.equals(scroll.getClientProperty(INSTALLED))) return;
        scroll.putClientProperty(INSTALLED, Boolean.TRUE);

        // O scroll ainda não tem contentor quando styleScrollPane corre; anexa quando ganhar um.
        scroll.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
                    && !Boolean.TRUE.equals(scroll.getClientProperty(ATTACHED))) {
                SwingUtilities.invokeLater(() -> attachToHost(scroll));
            }
        });
        attachToHost(scroll); // defensivo, caso já esteja na árvore
    }

    /** Adiciona a barra ao EAST do contentor do scroll, se este for um BorderLayout com EAST livre. */
    private static void attachToHost(JScrollPane scroll) {
        if (Boolean.TRUE.equals(scroll.getClientProperty(ATTACHED))) return;
        Container host = scroll.getParent();
        if (!(host instanceof JComponent parent)) return;
        if (!(parent.getLayout() instanceof BorderLayout bl)) return;
        if (!BorderLayout.CENTER.equals(bl.getConstraints(scroll))) return; // barra só faz sentido ao lado do centro
        if (bl.getLayoutComponent(BorderLayout.EAST) != null) return;        // EAST ocupado — não intromete

        scroll.putClientProperty(ATTACHED, Boolean.TRUE);
        parent.add(buildBar(scroll.getVerticalScrollBar()), BorderLayout.EAST);
        parent.revalidate();
        parent.repaint();
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

    /** Coluna EAST: cluster de 4 botões num cartão arredondado, centrado na vertical, com folga à esquerda. */
    private static JComponent buildBar(JScrollBar bar) {
        Strip cluster = new Strip();
        cluster.add(navButton("fas-angle-double-up", "Ir para o topo", () -> top(bar)));
        cluster.add(navButton("fas-angle-up", "Página acima", () -> pageUp(bar)));
        cluster.add(navButton("fas-angle-down", "Página abaixo", () -> pageDown(bar)));
        cluster.add(navButton("fas-angle-double-down", "Ir para o fundo", () -> bottom(bar)));

        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0)); // separa da tabela
        column.add(cluster, new GridBagConstraints()); // default: centrado
        return column;
    }

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

    /** Cartão arredondado que alberga os 4 botões. */
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
