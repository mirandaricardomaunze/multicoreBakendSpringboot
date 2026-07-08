package com.phcpro.gui.components;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Filtro reutilizável de tabelas: pesquisa livre (em todas as colunas) + dropdowns por coluna
 * (tipo/estado), via {@link TableRowSorter}. A lógica de correspondência ({@link #rowMatches}) é pura
 * e testável. Como instala um sorter, a selecção por índice deve passar por {@link #selectedModelRow}.
 */
public final class TableFilter {

    private TableFilter() {}

    /** Dropdown que filtra uma coluna pelo texto exacto; índice 0 do combo = "sem filtro". */
    public static final class ColumnFilter {
        final JComboBox<String> combo;
        final int column;
        public ColumnFilter(JComboBox<String> combo, int column) {
            this.combo = combo;
            this.column = column;
        }
    }

    /** Instala pesquisa livre + filtros por coluna. {@code search} pode ser null. */
    public static void install(JTable table, JTextField search, ColumnFilter... columnFilters) {
        install(table, search, java.util.Arrays.asList(columnFilters), java.util.List.of());
    }

    /** Instala pesquisa + filtros por coluna (tipo/estado) + filtros por período (data). */
    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        Runnable apply = () -> {
            String q = search == null ? "" : search.getText();
            Map<Integer, String> exact = new HashMap<>();
            for (ColumnFilter cf : columnFilters) {
                if (cf.combo.getSelectedIndex() > 0) {
                    exact.put(cf.column, String.valueOf(cf.combo.getSelectedItem()));
                }
            }
            java.time.LocalDate today = java.time.LocalDate.now();
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> e) {
                    List<String> cells = new ArrayList<>();
                    for (int i = 0; i < e.getValueCount(); i++) {
                        Object v = e.getValue(i);
                        cells.add(v == null ? "" : v.toString());
                    }
                    if (!rowMatches(cells, q, exact)) return false;
                    for (PeriodFilter pf : periodFilters) {
                        if (pf.combo.getSelectedIndex() <= 0) continue;
                        String cell = pf.column >= 0 && pf.column < cells.size() ? cells.get(pf.column) : "";
                        if (!matchesPeriod(parseCellDate(cell), String.valueOf(pf.combo.getSelectedItem()), today)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        };

        if (search != null) {
            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { apply.run(); }
                @Override public void removeUpdate(DocumentEvent e) { apply.run(); }
                @Override public void changedUpdate(DocumentEvent e) { apply.run(); }
            });
        }
        for (ColumnFilter cf : columnFilters) cf.combo.addActionListener(e -> apply.run());
        for (PeriodFilter pf : periodFilters) pf.combo.addActionListener(e -> apply.run());
        apply.run();
    }

    /**
     * Lógica pura: a linha passa se o texto de pesquisa aparecer em alguma célula (case-insensitive)
     * e cada coluna com filtro corresponder exactamente ao valor escolhido.
     */
    public static boolean rowMatches(List<String> cells, String search, Map<Integer, String> exactByColumn) {
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            boolean any = false;
            for (String c : cells) {
                if (c != null && c.toLowerCase(Locale.ROOT).contains(q)) { any = true; break; }
            }
            if (!any) return false;
        }
        if (exactByColumn != null) {
            for (Map.Entry<Integer, String> en : exactByColumn.entrySet()) {
                String want = en.getValue();
                if (want == null || want.isBlank()) continue;
                int col = en.getKey();
                String cell = (col >= 0 && col < cells.size()) ? cells.get(col) : "";
                if (!want.equalsIgnoreCase(cell == null ? "" : cell.trim())) return false;
            }
        }
        return true;
    }

    /** Linha do modelo correspondente à selecção (converte da vista); -1 se nada seleccionado. */
    public static int selectedModelRow(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    // ---- Filtro por período (coluna com data dd/MM/yyyy) ----

    /** Dropdown de período sobre uma coluna com data (dd/MM/yyyy…); índice 0 = "todo o período". */
    public static final class PeriodFilter {
        final JComboBox<String> combo;
        final int column;
        public PeriodFilter(JComboBox<String> combo, int column) {
            this.combo = combo;
            this.column = column;
        }
    }

    /** Combo de período pronto (com ícone de calendário via label na barra). */
    public static JComboBox<String> periodCombo() {
        return combo("Todo o período", "Hoje", "Últimos 7 dias", "Últimos 30 dias", "Este mês");
    }

    /** Extrai a data do início da célula (formato dd/MM/yyyy, com ou sem hora). Null se não parsear. */
    public static java.time.LocalDate parseCellDate(String cell) {
        if (cell == null || cell.length() < 10) return null;
        try {
            return java.time.LocalDate.parse(cell.substring(0, 10),
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Lógica pura: a data cai no período escolhido? Opção nula/"Todo…" ⇒ sempre verdadeiro. */
    public static boolean matchesPeriod(java.time.LocalDate date, String option, java.time.LocalDate today) {
        if (option == null || option.isBlank() || option.startsWith("Todo")) return true;
        if (date == null) return false;
        return switch (option) {
            case "Hoje" -> date.isEqual(today);
            case "Últimos 7 dias" -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today);
            case "Últimos 30 dias" -> !date.isBefore(today.minusDays(29)) && !date.isAfter(today);
            case "Este mês" -> date.getMonthValue() == today.getMonthValue() && date.getYear() == today.getYear();
            default -> true;
        };
    }

    // ---- Fábricas de UI (FlatLaf) ----

    /** Campo de pesquisa profissional: ícone de lupa + dica desenhados (qualquer Look&Feel). */
    public static JTextField searchField(String placeholder) {
        return new SearchField(placeholder);
    }

    public static JComboBox<String> combo(String... options) {
        JComboBox<String> c = new JComboBox<>(options);
        UIHelper.styleComboBox(c);
        return c;
    }

    /** Etiqueta com um ícone à esquerda (ex.: estado, calendário, categoria). */
    public static JLabel label(String text, String iconCode) {
        JLabel l = new JLabel(text);
        if (iconCode != null) l.setIcon(UIHelper.icon(iconCode, 13, UIHelper.TEXT_MUTED));
        l.setIconTextGap(5);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    public static JLabel label(String text) {
        return label(text, null);
    }

    /** Barra de filtros: ícone de funil como âncora visual + os componentes. */
    public static JPanel bar(JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        JLabel funnel = new JLabel(UIHelper.icon("fas-filter", 13, UIHelper.TEXT_MUTED));
        funnel.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 2));
        p.add(funnel);
        for (JComponent c : comps) p.add(c);
        return p;
    }
}
