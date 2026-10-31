package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;

/** Rodapé paginado automático e calha de selecção de linha das tabelas de listagem. */
final class UiTableFooterSupport {

    private UiTableFooterSupport() {}

    static void installListingFooter(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("listingFooterWired"))) return;
        table.putClientProperty("listingFooterWired", Boolean.TRUE);
        table.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                maybeAddListingFooter(table);
            }
        });
        table.addPropertyChangeListener("rowSorter", e -> maybeAddListingFooter(table));
        table.addPropertyChangeListener("model", e -> maybeAddListingFooter(table));
        table.addPropertyChangeListener(ClientTablePagination.DISABLED, evt -> {
            if (!Boolean.TRUE.equals(evt.getNewValue())) return;
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            if (sp != null && sp.getParent() instanceof JComponent parent
                    && parent.getClientProperty("paginationSouthComposite") instanceof UIHelper.PaginationSouthComposite comp) {
                JComponent extra = comp.getExtraFooter();
                parent.remove(comp);
                parent.putClientProperty("paginationSouthComposite", null);
                if (extra != null) {
                    comp.setExtraFooter(null);
                    parent.add(extra, BorderLayout.SOUTH);
                }
                parent.revalidate();
                parent.repaint();
            }
        });
    }

    private static void maybeAddListingFooter(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("noTableFooter"))) return;
        if (Boolean.TRUE.equals(table.getClientProperty(ClientTablePagination.DISABLED))) return;
        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (sp == null) return;
        if (sp.getParent() == null) {
            if (!Boolean.TRUE.equals(sp.getClientProperty("paginationHierarchyWired"))) {
                sp.putClientProperty("paginationHierarchyWired", Boolean.TRUE);
                sp.addHierarchyListener(ev -> {
                    if ((ev.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                        maybeAddListingFooter(table);
                    }
                });
            }
            return;
        }
        if (!(sp.getParent() instanceof JComponent parent)) return;
        if (parent.getLayout() instanceof BorderLayout bl) {
            installSouthGuard(parent, table);
            wirePaginationComposite(parent, bl, table);
        } else if (parent.getLayout() instanceof BoxLayout && !Boolean.TRUE.equals(parent.getClientProperty("boxPaginationInstalled"))) {
            parent.putClientProperty("boxPaginationInstalled", Boolean.TRUE);
            int spIdx = -1;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                if (parent.getComponent(i) == sp) { spIdx = i; break; }
            }
            JPanel pager = ClientTablePagination.install(table);
            if (spIdx >= 0) parent.add(pager, spIdx + 1);
            else parent.add(pager);
            parent.revalidate();
            parent.repaint();
        }
    }

    private static void wirePaginationComposite(JComponent parent, BorderLayout bl, JTable table) {
        Component currentSouth = bl.getLayoutComponent(BorderLayout.SOUTH);
        if (currentSouth instanceof TablePager) {
            table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
            return;
        }
        JPanel pager = ClientTablePagination.install(table);
        if (currentSouth instanceof UIHelper.PaginationSouthComposite composite) {
            if (composite.getPagination() != pager) composite.setPagination(pager);
            return;
        }
        UIHelper.PaginationSouthComposite composite = new UIHelper.PaginationSouthComposite(pager);
        parent.putClientProperty("paginationSouthComposite", composite);
        if (currentSouth instanceof JComponent extra) {
            parent.remove(extra);
            composite.setExtraFooter(extra);
        }
        parent.add(composite, BorderLayout.SOUTH);
        parent.revalidate();
        parent.repaint();
    }

    private static void installSouthGuard(JComponent parent, JTable table) {
        if (Boolean.TRUE.equals(parent.getClientProperty("paginationGuardInstalled"))) return;
        parent.putClientProperty("paginationGuardInstalled", Boolean.TRUE);
        parent.addContainerListener(new ContainerAdapter() {
            private boolean rearranging = false;
            @Override public void componentAdded(ContainerEvent e) {
                if (rearranging) return;
                Component child = e.getChild();
                if (child instanceof UIHelper.PaginationSouthComposite) return;
                if (!(parent.getLayout() instanceof BorderLayout bl) || bl.getLayoutComponent(BorderLayout.SOUTH) != child
                        || !(child instanceof JComponent extra)) {
                    return;
                }
                if (extra instanceof TablePager
                        || Boolean.TRUE.equals(table.getClientProperty(ClientTablePagination.DISABLED))
                        || Boolean.TRUE.equals(table.getClientProperty("noTableFooter"))) {
                    if (extra instanceof TablePager) table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
                    if (parent.getClientProperty("paginationSouthComposite") instanceof UIHelper.PaginationSouthComposite existing) {
                        parent.remove(existing);
                        parent.putClientProperty("paginationSouthComposite", null);
                        parent.revalidate();
                        parent.repaint();
                    }
                    return;
                }
                rearranging = true;
                try {
                    parent.remove(extra);
                    UIHelper.PaginationSouthComposite comp =
                            parent.getClientProperty("paginationSouthComposite") instanceof UIHelper.PaginationSouthComposite existing
                                    ? existing : new UIHelper.PaginationSouthComposite(ClientTablePagination.install(table));
                    parent.putClientProperty("paginationSouthComposite", comp);
                    comp.setExtraFooter(extra);
                    parent.add(comp, BorderLayout.SOUTH);
                    parent.revalidate();
                    parent.repaint();
                } finally {
                    rearranging = false;
                }
            }
        });
    }

    static void installRowSelector(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("rowSelectorWired"))) return;
        table.putClientProperty("rowSelectorWired", Boolean.TRUE);
        table.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == 0) return;
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            if (sp == null || Boolean.TRUE.equals(sp.getClientProperty("rowSelectorGutter"))) return;
            sp.putClientProperty("rowSelectorGutter", Boolean.TRUE);
            sp.setRowHeaderView(buildRowSelectorGutter(table));
            JPanel corner = new JPanel();
            corner.setBackground(UIHelper.TABLE_HEADER_BG);
            corner.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 1, UIHelper.GRID));
            sp.setCorner(JScrollPane.UPPER_LEFT_CORNER, corner);
        });
    }

    private static JComponent buildRowSelectorGutter(JTable table) {
        final int width = 24;
        JComponent gutter = new JComponent() {
            @Override public Dimension getPreferredSize() {
                int h = table.getHeight() > 0 ? table.getHeight() : table.getPreferredSize().height;
                return new Dimension(width, h);
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIHelper.TABLE_HEADER_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                int rows = table.getRowCount();
                g2.setColor(UIHelper.GRID);
                for (int r = 0; r < rows; r++) {
                    Rectangle cr = table.getCellRect(r, 0, true);
                    g2.drawLine(0, cr.y + cr.height - 1, getWidth(), cr.y + cr.height - 1);
                }
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                int sel = table.getSelectedRow();
                if (sel >= 0 && sel < rows) {
                    Rectangle cr = table.getCellRect(sel, 0, true);
                    g2.setColor(UIHelper.SELECTION_BG);
                    g2.fillRect(0, cr.y, getWidth() - 1, cr.height);
                    int cy = cr.y + cr.height / 2;
                    int x = 8;
                    g2.setColor(UIHelper.ACCENT);
                    g2.fill(new Polygon(new int[]{x, x + 7, x}, new int[]{cy - 5, cy, cy + 5}, 3));
                }
                g2.dispose();
            }
        };
        gutter.setOpaque(true);
        Runnable sync = () -> { gutter.revalidate(); gutter.repaint(); };
        table.getSelectionModel().addListSelectionListener(e -> gutter.repaint());
        table.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { sync.run(); }
        });
        TableModelListener tml = e -> sync.run();
        table.getModel().addTableModelListener(tml);
        table.addPropertyChangeListener("model", evt -> {
            if (evt.getNewValue() instanceof TableModel m) m.addTableModelListener(tml);
            sync.run();
        });
        return gutter;
    }
}
