package mz.multicore.erp.gui.components;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Filtro reutilizável de tabelas: fachada fina sobre {@link TableFilterMatcher} e {@link TableFilterBarFactory}.
 */
public final class TableFilter {

    public static final String CLEAR_ACTION_KEY = "tableFilter.clearAction";

    private TableFilter() {}

    public static final class ContainsColumnFilter {
        final JComboBox<String> combo;
        final int column;
        public ContainsColumnFilter(JComboBox<String> combo, int column) {
            this.combo = combo;
            this.column = column;
        }
    }

    public static final class ColumnFilter {
        final JComboBox<String> combo;
        final int column;
        public ColumnFilter(JComboBox<String> combo, int column) {
            this.combo = combo;
            this.column = column;
        }
    }

    public static final class PeriodFilter {
        final JComboBox<String> combo;
        final int column;
        public PeriodFilter(JComboBox<String> combo, int column) {
            this.combo = combo;
            this.column = column;
        }
    }

    public static final class DateRangeFilter {
        final DateField from;
        final DateField to;
        final int column;
        public DateRangeFilter(DateField from, DateField to, int column) {
            this.from = from;
            this.to = to;
            this.column = column;
        }
    }

    public static void install(JTable table, JTextField search, ColumnFilter... columnFilters) {
        install(table, search, Arrays.asList(columnFilters), List.of());
    }

    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters) {
        install(table, search, columnFilters, periodFilters, List.of());
    }

    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters,
                               List<DateRangeFilter> dateRangeFilters) {
        install(table, search, columnFilters, periodFilters, dateRangeFilters, List.of());
    }

    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters,
                               List<DateRangeFilter> dateRangeFilters,
                               List<ContainsColumnFilter> containsColumnFilters) {
        TableFilterBarFactory.install(table, search, columnFilters, periodFilters, dateRangeFilters, containsColumnFilters);
    }

    public static void clear(JTable table) {
        if (table == null) return;
        Object action = table.getClientProperty(CLEAR_ACTION_KEY);
        if (action instanceof Runnable r) r.run();
    }

    public static boolean rowMatches(List<String> cells, String search, Map<Integer, String> exactByColumn) {
        return TableFilterMatcher.rowMatches(cells, search, exactByColumn);
    }

    public static boolean cellMatches(String cell, String want) {
        return TableFilterMatcher.cellMatches(cell, want);
    }

    public static int selectedModelRow(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    public static boolean matchesDateRange(LocalDate date, LocalDate from, LocalDate to) {
        return TableFilterMatcher.matchesDateRange(date, from, to);
    }

    public static JComboBox<String> periodCombo() {
        return combo("Todo o período", "Hoje", "Ontem", "Esta semana", "Últimos 7 dias", "Este mês", "Últimos 30 dias", "Este ano");
    }

    public static LocalDate parseCellDate(String cell) {
        return TableFilterMatcher.parseCellDate(cell);
    }

    public static boolean matchesPeriod(LocalDate date, String option, LocalDate today) {
        return TableFilterMatcher.matchesPeriod(date, option, today);
    }

    public static JTextField searchField(String placeholder) {
        return new SearchField(placeholder);
    }

    public static JComboBox<String> combo(String... options) {
        return TableFilterBarFactory.combo(options);
    }

    public static JLabel label(String text, String iconCode) {
        return TableFilterBarFactory.label(text, iconCode);
    }

    public static JLabel label(String text) {
        return label(text, null);
    }

    public static JPanel bar(JComponent... comps) {
        return TableFilterBarFactory.bar(comps);
    }

    public static JPanel toolbar(JComponent[] filters, JComponent[] actions) {
        return TableFilterBarFactory.toolbar(filters, actions);
    }

    public static JComponent separator() {
        return TableFilterBarFactory.separator();
    }
}
