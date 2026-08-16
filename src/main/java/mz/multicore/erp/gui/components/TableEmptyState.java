package mz.multicore.erp.gui.components;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ScrollPaneLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

/**
 * Estado vazio das tabelas: quando o modelo não tem linhas, mostra uma mensagem centrada em vez de
 * uma grelha em branco. Texto por omissão "Sem registos."; personalizável por tabela via
 * {@code table.putClientProperty("emptyText", "Sem encomendas.")}.
 *
 * <p>Instalado centralmente por {@link UIHelper#styleScrollPane(JScrollPane)}. A mensagem é um
 * overlay centrado sobre o viewport, só visível quando a tabela está vazia (não tapa dados). Ver
 * docs/UI_TABELAS_UX_SPEC.md.</p>
 */
public final class TableEmptyState {

    private static final String INSTALLED = "tableEmptyState.installed";
    private static final String EMPTY_TEXT = "emptyText";

    private TableEmptyState() {}

    public static void install(JScrollPane scroll) {
        if (scroll == null || scroll.getViewport() == null) return;
        if (!(scroll.getViewport().getView() instanceof JTable table)) return;
        if (Boolean.TRUE.equals(scroll.getClientProperty(INSTALLED))) return;
        scroll.putClientProperty(INSTALLED, Boolean.TRUE);

        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        label.setFocusable(false);
        label.setVisible(false);

        OverlayScrollLayout layout = new OverlayScrollLayout(label);
        scroll.setLayout(layout);
        layout.syncWithScrollPane(scroll);
        scroll.add(label);
        scroll.setComponentZOrder(label, 0);

        Runnable refresh = () -> {
            boolean empty = table.getRowCount() == 0;
            if (empty) label.setText(resolveText(table));
            // Aplicar sempre o estado calculado. Evita que um overlay visível de um estado
            // anterior sobreviva a actualizações consecutivas do modelo/sorter.
            label.setVisible(empty);
            scroll.revalidate();
            scroll.repaint();
        };
        Runnable refreshAfterSwingUpdate = () -> {
            refresh.run();
            // JTable/TableRowSorter também escutam o modelo. A segunda passagem ocorre depois
            // desses listeners e confirma a contagem que está efectivamente visível.
            SwingUtilities.invokeLater(refresh);
        };
        table.getModel().addTableModelListener(e -> refreshAfterSwingUpdate.run());
        table.addPropertyChangeListener("model", e -> {
            table.getModel().addTableModelListener(ev -> refreshAfterSwingUpdate.run());
            refreshAfterSwingUpdate.run();
        });
        table.addPropertyChangeListener("rowSorter", e -> {
            if (table.getRowSorter() != null) {
                table.getRowSorter().addRowSorterListener(ev -> refreshAfterSwingUpdate.run());
            }
            refreshAfterSwingUpdate.run();
        });
        refresh.run();
    }

    /** Texto do estado vazio: client-property {@code emptyText} da tabela, ou "Sem registos.". */
    static String resolveText(JTable table) {
        Object v = table.getClientProperty(EMPTY_TEXT);
        return (v instanceof String s && !s.isBlank()) ? s : "Sem registos.";
    }

    /** {@link ScrollPaneLayout} que, além do normal, centra um overlay sobre o viewport. */
    private static final class OverlayScrollLayout extends ScrollPaneLayout {
        private final Component overlay;

        OverlayScrollLayout(Component overlay) {
            this.overlay = overlay;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            if (name == null) return; // o overlay é posicionado à mão
            super.addLayoutComponent(name, comp);
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            if (comp == overlay) return;
            super.removeLayoutComponent(comp);
        }

        @Override
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            if (overlay == null || !overlay.isVisible()) return;
            JViewport vp = getViewport();
            if (vp == null) return;
            // Última barreira defensiva: nunca deixar o overlay ocupar a área de uma tabela
            // que já tem linhas, mesmo que uma notificação Swing chegue fora de ordem.
            if (vp.getView() instanceof JTable table && table.getRowCount() > 0) {
                overlay.setVisible(false);
                overlay.setBounds(0, 0, 0, 0);
                return;
            }
            Rectangle vb = vp.getBounds();
            Dimension ps = overlay.getPreferredSize();
            int w = Math.min(ps.width, vb.width);
            int h = Math.min(ps.height, vb.height);
            overlay.setBounds(vb.x + (vb.width - w) / 2, vb.y + (vb.height - h) / 2, w, h);
        }
    }
}
