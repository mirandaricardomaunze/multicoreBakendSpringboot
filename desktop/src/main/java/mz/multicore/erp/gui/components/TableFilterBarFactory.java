package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TableFilterBarFactory {

    private TableFilterBarFactory() {}

    static void install(JTable table, JTextField search,
                        List<TableFilter.ColumnFilter> columnFilters,
                        List<TableFilter.PeriodFilter> periodFilters,
                        List<TableFilter.DateRangeFilter> dateRangeFilters,
                        List<TableFilter.ContainsColumnFilter> containsColumnFilters) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        Runnable apply = () -> {
            String q = search == null ? "" : search.getText();
            Map<Integer, String> exact = new HashMap<>();
            for (TableFilter.ColumnFilter cf : columnFilters) {
                if (cf.combo.getSelectedIndex() > 0) {
                    exact.put(cf.column, String.valueOf(cf.combo.getSelectedItem()));
                }
            }
            LocalDate today = LocalDate.now();
            ClientTablePagination.setBaseFilter(table, new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> e) {
                    List<String> cells = new ArrayList<>();
                    for (int i = 0; i < e.getValueCount(); i++) {
                        Object v = e.getValue(i);
                        cells.add(v == null ? "" : v.toString());
                    }
                    if (!TableFilterMatcher.rowMatches(cells, q, exact)) return false;
                    for (TableFilter.PeriodFilter pf : periodFilters) {
                        if (pf.combo.getSelectedIndex() <= 0) continue;
                        String cell = pf.column >= 0 && pf.column < cells.size() ? cells.get(pf.column) : "";
                        if (!TableFilterMatcher.matchesPeriod(TableFilterMatcher.parseCellDate(cell), String.valueOf(pf.combo.getSelectedItem()), today)) {
                            return false;
                        }
                    }
                    for (TableFilter.DateRangeFilter rf : dateRangeFilters) {
                        String cell = rf.column >= 0 && rf.column < cells.size() ? cells.get(rf.column) : "";
                        if (!TableFilterMatcher.matchesDateRange(TableFilterMatcher.parseCellDate(cell), rf.from.getDate(), rf.to.getDate())) {
                            return false;
                        }
                    }
                    for (TableFilter.ContainsColumnFilter ccf : containsColumnFilters) {
                        if (ccf.combo.getSelectedIndex() <= 0) continue;
                        String want = String.valueOf(ccf.combo.getSelectedItem()).trim().toLowerCase(Locale.ROOT);
                        String cell = ccf.column >= 0 && ccf.column < cells.size() ? cells.get(ccf.column) : "";
                        if (!cell.toLowerCase(Locale.ROOT).contains(want)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        };

        Runnable clearAll = () -> {
            if (search != null) search.setText("");
            for (TableFilter.ColumnFilter cf : columnFilters) if (cf.combo.getItemCount() > 0) cf.combo.setSelectedIndex(0);
            for (TableFilter.PeriodFilter pf : periodFilters) if (pf.combo.getItemCount() > 0) pf.combo.setSelectedIndex(0);
            for (TableFilter.DateRangeFilter rf : dateRangeFilters) {
                rf.from.setText("");
                rf.to.setText("");
            }
            for (TableFilter.ContainsColumnFilter ccf : containsColumnFilters) if (ccf.combo.getItemCount() > 0) ccf.combo.setSelectedIndex(0);
        };
        table.putClientProperty(TableFilter.CLEAR_ACTION_KEY, clearAll);

        if (search != null) {
            search.registerKeyboardAction(
                    e -> clearAll.run(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_FOCUSED
            );
            search.getDocument().addDocumentListener(listener(apply));
        }
        for (TableFilter.ColumnFilter cf : columnFilters) cf.combo.addActionListener(e -> apply.run());
        for (TableFilter.PeriodFilter pf : periodFilters) pf.combo.addActionListener(e -> apply.run());
        for (TableFilter.DateRangeFilter rf : dateRangeFilters) {
            rf.from.getDocument().addDocumentListener(listener(apply));
            rf.to.getDocument().addDocumentListener(listener(apply));
        }
        for (TableFilter.ContainsColumnFilter ccf : containsColumnFilters) ccf.combo.addActionListener(e -> apply.run());
        apply.run();
    }

    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    static JComboBox<String> combo(String... options) {
        JComboBox<String> c = new JComboBox<>(options);
        UIHelper.styleComboBox(c);
        return c;
    }

    static JLabel label(String text, String iconCode) {
        JLabel l = new JLabel(text);
        if (iconCode != null) l.setIcon(UIHelper.icon(iconCode, 13, UIHelper.TEXT_MUTED));
        l.setIconTextGap(5);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    static JPanel bar(JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0) {
            @Override
            public void layoutContainer(Container target) {
                synchronized (target.getTreeLock()) {
                    Insets insets = target.getInsets();
                    int x = insets.left;
                    int hgap = getHgap();
                    int targetHeight = target.getHeight() - (insets.top + insets.bottom);
                    for (int i = 0; i < target.getComponentCount(); i++) {
                        Component m = target.getComponent(i);
                        if (m.isVisible()) {
                            Dimension d = m.getPreferredSize();
                            int y = insets.top + Math.max(0, (targetHeight - d.height) / 2);
                            m.setBounds(x, y, d.width, d.height);
                            x += d.width + hgap;
                        }
                    }
                }
            }
        });
        p.setOpaque(false);
        for (JComponent c : comps) {
            if (c != null) p.add(c);
        }
        return p;
    }

    static JPanel toolbar(JComponent[] filters, JComponent[] actions) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        if (filters != null && filters.length > 0) {
            p.add(bar(filters), BorderLayout.WEST);
        }
        if (actions != null && actions.length > 0) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0) {
                @Override
                public void layoutContainer(Container target) {
                    synchronized (target.getTreeLock()) {
                        Insets insets = target.getInsets();
                        int x = target.getWidth() - insets.right;
                        int hgap = getHgap();
                        int targetHeight = target.getHeight() - (insets.top + insets.bottom);
                        for (int i = target.getComponentCount() - 1; i >= 0; i--) {
                            Component m = target.getComponent(i);
                            if (m.isVisible()) {
                                Dimension d = m.getPreferredSize();
                                x -= d.width;
                                int y = insets.top + Math.max(0, (targetHeight - d.height) / 2);
                                m.setBounds(x, y, d.width, d.height);
                                x -= hgap;
                            }
                        }
                    }
                }
            });
            right.setOpaque(false);
            for (JComponent c : actions) {
                if (c != null) right.add(c);
            }
            p.add(right, BorderLayout.EAST);
        }
        return p;
    }

    static JComponent separator() {
        JPanel sep = new JPanel();
        sep.setOpaque(true);
        sep.setBackground(UIHelper.BORDER);
        sep.setPreferredSize(new Dimension(1, 20));
        return sep;
    }
}
