package mz.multicore.erp.gui.components;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
 * Estado vazio das tabelas: quando o modelo não tem linhas, mostra um painel composto centrado
 * com ícone + título + subtítulo. Texto por omissão "Sem registos."; personalizável por tabela
 * via {@code table.putClientProperty("emptyText", "Sem encomendas.")} e
 * {@code table.putClientProperty("emptySubtext", "Crie a primeira encomenda para começar.")}.
 *
 * <p>Instalado centralmente por {@link UIHelper#styleScrollPane(JScrollPane)}. O painel é um
 * overlay centrado sobre o viewport, só visível quando a tabela está vazia (não tapa dados). Ver
 * docs/UI_TABELAS_UX_SPEC.md.</p>
 */
public final class TableEmptyState {

    private static final String INSTALLED     = "tableEmptyState.installed";
    private static final String EMPTY_TEXT    = "emptyText";
    private static final String EMPTY_SUBTEXT = "emptySubtext";
    /** Client property: the screen's main action (a button), offered when the table has no data. */
    static final String EMPTY_ACTION  = "emptyState.action";
    /** Client property: the filters container, cleared by "Limpar filtros" when they hide every row. */
    static final String EMPTY_FILTERS = "emptyState.filters";
    static final String CLEAR_FILTERS = "Limpar filtros";

    private TableEmptyState() {}

    public static void install(JScrollPane scroll) {
        if (scroll == null || scroll.getViewport() == null) return;
        if (!(scroll.getViewport().getView() instanceof JTable table)) return;
        if (Boolean.TRUE.equals(scroll.getClientProperty(INSTALLED))) return;
        scroll.putClientProperty(INSTALLED, Boolean.TRUE);

        // Painel composto: ícone + título + subtítulo
        JPanel overlay = buildOverlayPanel(table);
        overlay.setVisible(false);

        OverlayScrollLayout layout = new OverlayScrollLayout(overlay);
        scroll.setLayout(layout);
        layout.syncWithScrollPane(scroll);
        scroll.add(overlay);
        scroll.setComponentZOrder(overlay, 0);

        Runnable refresh = () -> {
            boolean empty = table.getRowCount() == 0;
            if (empty) updateOverlayText(overlay, table);
            // Aplicar sempre o estado calculado. Evita que um overlay visível de um estado
            // anterior sobreviva a actualizações consecutivas do modelo/sorter.
            overlay.setVisible(empty);
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

    /** Constrói o painel composto (ícone + título + subtítulo). */
    private static JPanel buildOverlayPanel(JTable table) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setFocusable(false);

        // Ícone elegante proporcional (fas-inbox, 32px)
        JLabel iconLbl = new JLabel(UIHelper.icon("fas-inbox", 32, UIHelper.TEXT_MUTED));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(iconLbl);

        // Título em negrito
        JLabel titleLbl = new JLabel(resolveText(table));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLbl);

        // Subtítulo em muted
        JLabel subLbl = new JLabel(resolveSubtext(table));
        subLbl.setForeground(UIHelper.TEXT_MUTED);
        subLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subLbl.setHorizontalAlignment(SwingConstants.CENTER);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        subLbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(subLbl);

        // Acção: a principal do ecrã quando não há dados, ou "Limpar filtros" quando os filtros escondem tudo
        ModernButton actionBtn = UIHelper.createSuccessButton(" ");
        actionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionBtn.setVisible(false);
        actionBtn.addActionListener(e -> runAction(table));
        JPanel actionRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        actionRow.setOpaque(false);
        actionRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 0, 0));
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionRow.add(actionBtn);
        panel.add(actionRow);

        // Guardar referências para actualização posterior
        panel.putClientProperty("titleLabel", titleLbl);
        panel.putClientProperty("subLabel",   subLbl);
        panel.putClientProperty("actionButton", actionBtn);
        return panel;
    }

    /** Actualiza os textos do painel quando o estado vazio muda de tabela/filtro. */
    private static void updateOverlayText(JPanel overlay, JTable table) {
        Object titleRef = overlay.getClientProperty("titleLabel");
        Object subRef   = overlay.getClientProperty("subLabel");
        if (titleRef instanceof JLabel title) title.setText(resolveText(table));
        if (subRef   instanceof JLabel sub)   sub.setText(resolveSubtext(table));
        if (overlay.getClientProperty("actionButton") instanceof ModernButton button) {
            String label = resolveActionLabel(table);
            button.setVisible(label != null);
            if (label != null) {
                button.setText(label);
                if (CLEAR_FILTERS.equals(label)) {
                    ButtonStyles.outline(button);
                    button.setIcon(UIHelper.icon("fas-filter", 13));
                } else {
                    button.setColors(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
                    button.putClientProperty("button.outline", null);
                    javax.swing.Icon icon = ((javax.swing.AbstractButton) table.getClientProperty(EMPTY_ACTION)).getIcon();
                    button.setIcon(icon);
                }
            }
        }
    }

    /**
     * "Limpar filtros" when rows exist but the filters hide them all; the screen's main action when the
     * table has no data at all; nothing when neither applies (or the action is hidden or disabled).
     */
    static String resolveActionLabel(JTable table) {
        if (table.getModel().getRowCount() > 0) {
            return table.getClientProperty(EMPTY_FILTERS) instanceof Container ? CLEAR_FILTERS : null;
        }
        if (table.getClientProperty(EMPTY_ACTION) instanceof javax.swing.AbstractButton main
                && main.isVisible() && main.isEnabled() && main.getText() != null && !main.getText().isBlank()) {
            return ModernButton.stripEllipsis(main.getText());
        }
        return null;
    }

    static void runAction(JTable table) {
        if (table.getModel().getRowCount() > 0) {
            if (table.getClientProperty(EMPTY_FILTERS) instanceof Container filters) clearFilters(filters);
        } else if (table.getClientProperty(EMPTY_ACTION) instanceof javax.swing.AbstractButton main) {
            main.doClick();
        }
    }

    /** Empties search fields and puts every filter combo back on its first option ("Todos…"). */
    static void clearFilters(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JComboBox<?> combo) {
                if (combo.getItemCount() > 0 && combo.getSelectedIndex() != 0) combo.setSelectedIndex(0);
            } else if (child instanceof javax.swing.JTextField field) {
                if (!field.getText().isEmpty()) field.setText("");
            } else if (child instanceof Container nested) {
                clearFilters(nested);
            }
        }
    }

    /** Título do estado vazio: client-property {@code emptyText} da tabela, ou "Sem registos.". */
    static String resolveText(JTable table) {
        Object v = table.getClientProperty(EMPTY_TEXT);
        return (v instanceof String s && !s.isBlank()) ? s : "Sem registos.";
    }

    /**
     * Subtítulo do estado vazio: client-property {@code emptySubtext}, ou dica contextual quando
     * um filtro esconde todas as linhas, ou string vazia.
     */
    static String resolveSubtext(JTable table) {
        Object v = table.getClientProperty(EMPTY_SUBTEXT);
        if (v instanceof String s && !s.isBlank()) return s;
        if (table.getModel().getRowCount() > 0) return "Tente alterar ou limpar o filtro de pesquisa.";
        return "";
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
