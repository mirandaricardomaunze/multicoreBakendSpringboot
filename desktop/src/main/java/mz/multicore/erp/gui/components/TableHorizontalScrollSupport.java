package mz.multicore.erp.gui.components;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeListener;

/**
 * Suporte canónico e DRY para rolagem horizontal em tabelas com muitas colunas no Multicore ERP.
 *
 * <p><b>Problema resolvido:</b> Por omissão, o Swing força tabelas a caberem na largura da janela
 * ({@code AUTO_RESIZE_SUBSEQUENT_COLUMNS}), espremendo todas as colunas até truncar textos,
 * números e cabeçalhos em tabelas densas (6+ colunas) sem permitir rolagem horizontal.
 *
 * <p><b>Comportamento adaptativo:</b>
 * <ul>
 *   <li>Se a tabela tiver muitas colunas (≥ 6) ou a soma das larguras preferidas exceder o viewport:
 *       ativa {@link JTable#AUTO_RESIZE_OFF} e disponibiliza rolagem horizontal ergonómica.</li>
 *   <li>Se o viewport for suficientemente largo para acomodar todas as colunas com folga:
 *       ativa {@link JTable#AUTO_RESIZE_SUBSEQUENT_COLUMNS} para preencher 100% da largura sem lacunas vazias.</li>
 *   <li>Instalado centralmente por {@link UIHelper#styleTable(JTable)} e {@link UIHelper#styleScrollPane(JScrollPane)},
 *       assegurando princípio DRY absoluto em todo o sistema.</li>
 * </ul>
 */
public final class TableHorizontalScrollSupport {

    public static final String DISABLED_KEY = "tableHorizontalScroll.disabled";
    public static final String INSTALLED_KEY = "tableHorizontalScroll.installed";
    public static final int MANY_COLUMNS_THRESHOLD = 6;

    private TableHorizontalScrollSupport() {}

    /**
     * Instala o suporte de rolagem horizontal inteligente na tabela.
     */
    public static void install(JTable table) {
        if (table == null || Boolean.TRUE.equals(table.getClientProperty(INSTALLED_KEY))) {
            return;
        }
        table.putClientProperty(INSTALLED_KEY, Boolean.TRUE);

        HierarchyListener hierarchyListener = e -> {
            if ((e.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                wireScrollPane(table);
            }
        };
        table.addHierarchyListener(hierarchyListener);

        PropertyChangeListener propListener = evt -> {
            String prop = evt.getPropertyName();
            if ("model".equals(prop) || "columnModel".equals(prop)) {
                SwingUtilities.invokeLater(() -> update(table));
            }
        };
        table.addPropertyChangeListener(propListener);

        wireScrollPane(table);
    }

    /**
     * Instala o suporte de rolagem horizontal inteligente no scroll pane da tabela.
     */
    public static void install(JScrollPane scroll) {
        if (scroll == null) return;
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        if (!(scroll.getHorizontalScrollBar().getUI() instanceof SlimScrollBarUI)) {
            scroll.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
            scroll.getHorizontalScrollBar().setOpaque(false);
        }
        if (scroll.getViewport() != null && scroll.getViewport().getView() instanceof JTable table) {
            install(table);
            wireScrollPane(table);
        }
    }

    private static void wireScrollPane(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty(DISABLED_KEY))) return;
        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (sp == null) return;

        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        if (!(sp.getHorizontalScrollBar().getUI() instanceof SlimScrollBarUI)) {
            sp.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
            sp.getHorizontalScrollBar().setOpaque(false);
        }

        JViewport viewport = sp.getViewport();
        if (viewport != null && !Boolean.TRUE.equals(viewport.getClientProperty("horizontalScrollSupport.wired"))) {
            viewport.putClientProperty("horizontalScrollSupport.wired", Boolean.TRUE);
            viewport.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    update(table);
                }
            });
        }

        update(table);
    }

    /**
     * Recalcula e atualiza o modo de redimensionamento e scroll horizontal da tabela.
     */
    public static void update(JTable table) {
        if (table == null || Boolean.TRUE.equals(table.getClientProperty(DISABLED_KEY))) return;

        int colCount = table.getColumnCount();
        if (colCount == 0) return;

        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        int viewportWidth = 0;
        if (sp != null && sp.getViewport() != null) {
            viewportWidth = sp.getViewport().getWidth();
        }

        calibrateColumnWidths(table);

        int totalPreferredWidth = calculateTotalPreferredWidth(table);

        boolean manyColumns = colCount >= MANY_COLUMNS_THRESHOLD;
        boolean exceedsViewport = viewportWidth > 0 && totalPreferredWidth > viewportWidth;

        if (viewportWidth <= 0 && manyColumns) {
            if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF) {
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            }
            return;
        }

        if (manyColumns || exceedsViewport) {
            if (viewportWidth > 0 && totalPreferredWidth <= viewportWidth) {
                // Coube com folga na janela: expande uniformemente para não sobrar espaço vazio
                if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS) {
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
                }
            } else {
                // Não cabe ou ultrapassa o viewport: fixa AUTO_RESIZE_OFF para permitir scroll horizontal limpo
                if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF) {
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                }
                restorePreferredWidths(table);
            }
        } else {
            // Poucas colunas que cabem no viewport: modo padrão
            if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS) {
                table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            }
        }
    }

    /**
     * Garante que todas as colunas tenham piso mínimo adequado e larguras preferidas seguras.
     */
    public static void calibrateColumnWidths(JTable table) {
        if (table == null || table.getTableHeader() == null) return;
        FontMetrics metrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        TableColumnModel colModel = table.getColumnModel();
        int count = colModel.getColumnCount();

        for (int i = 0; i < count; i++) {
            TableColumn col = colModel.getColumn(i);
            Object headerValue = col.getHeaderValue();
            String title = headerValue != null ? headerValue.toString() : "";
            int neededHeader = metrics != null ? metrics.stringWidth(title) + 26 : 75;

            int minSafe = Math.max(neededHeader, 65);

            if (col.getMinWidth() < minSafe) {
                col.setMinWidth(minSafe);
            }
            if (col.getPreferredWidth() < minSafe) {
                col.setPreferredWidth(minSafe);
            }
        }
    }

    private static void restorePreferredWidths(JTable table) {
        TableColumnModel colModel = table.getColumnModel();
        int count = colModel.getColumnCount();
        for (int i = 0; i < count; i++) {
            TableColumn col = colModel.getColumn(i);
            if (col.getPreferredWidth() < col.getMinWidth()) {
                col.setPreferredWidth(col.getMinWidth());
            }
        }
    }

    /**
     * Calcula a soma das larguras preferidas das colunas.
     */
    public static int calculateTotalPreferredWidth(JTable table) {
        if (table == null) return 0;
        TableColumnModel colModel = table.getColumnModel();
        int total = 0;
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            total += colModel.getColumn(i).getPreferredWidth();
        }
        return total;
    }
}
