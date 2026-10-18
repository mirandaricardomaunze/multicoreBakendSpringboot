package mz.multicore.erp.gui.components;

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
import java.awt.BorderLayout;
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
    /** Dropdown que filtra uma coluna verificando se a celula contem a opcao escolhida; indice 0 = "sem filtro". */
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

    /** Instala pesquisa livre + filtros por coluna. {@code search} pode ser null. */
    public static void install(JTable table, JTextField search, ColumnFilter... columnFilters) {
        install(table, search, java.util.Arrays.asList(columnFilters), java.util.List.of());
    }

    /** Instala pesquisa + filtros por coluna (tipo/estado) + filtros por período (data). */
    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters) {
        install(table, search, columnFilters, periodFilters, List.of());
    }

    /** Instala pesquisa, filtros exactos, períodos predefinidos e intervalos de datas livres. */
    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters,
                               List<DateRangeFilter> dateRangeFilters) {
        install(table, search, columnFilters, periodFilters, dateRangeFilters, List.of());
    }

    public static void install(JTable table, JTextField search,
                               List<ColumnFilter> columnFilters, List<PeriodFilter> periodFilters,
                               List<DateRangeFilter> dateRangeFilters,
                               List<ContainsColumnFilter> containsColumnFilters) {
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
            ClientTablePagination.setBaseFilter(table, new RowFilter<TableModel, Integer>() {
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
                    for (DateRangeFilter rf : dateRangeFilters) {
                        String cell = rf.column >= 0 && rf.column < cells.size() ? cells.get(rf.column) : "";
                        if (!matchesDateRange(parseCellDate(cell), rf.from.getDate(), rf.to.getDate())) {
                            return false;
                        }
                    }
                    for (ContainsColumnFilter ccf : containsColumnFilters) {
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
            for (ColumnFilter cf : columnFilters) if (cf.combo.getItemCount() > 0) cf.combo.setSelectedIndex(0);
            for (PeriodFilter pf : periodFilters) if (pf.combo.getItemCount() > 0) pf.combo.setSelectedIndex(0);
            for (DateRangeFilter rf : dateRangeFilters) {
                rf.from.setText("");
                rf.to.setText("");
            }
            for (ContainsColumnFilter ccf : containsColumnFilters) if (ccf.combo.getItemCount() > 0) ccf.combo.setSelectedIndex(0);
        };
        table.putClientProperty(CLEAR_ACTION_KEY, clearAll);

        if (search != null) {
            search.registerKeyboardAction(
                    e -> clearAll.run(),
                    javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_FOCUSED
            );
            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { apply.run(); }
                @Override public void removeUpdate(DocumentEvent e) { apply.run(); }
                @Override public void changedUpdate(DocumentEvent e) { apply.run(); }
            });
        }
        for (ColumnFilter cf : columnFilters) cf.combo.addActionListener(e -> apply.run());
        for (PeriodFilter pf : periodFilters) pf.combo.addActionListener(e -> apply.run());
        for (DateRangeFilter rf : dateRangeFilters) {
            rf.from.getDocument().addDocumentListener(listener(apply));
            rf.to.getDocument().addDocumentListener(listener(apply));
        }
        for (ContainsColumnFilter ccf : containsColumnFilters) ccf.combo.addActionListener(e -> apply.run());
        apply.run();
    }

    public static final String CLEAR_ACTION_KEY = "tableFilter.clearAction";

    /** Limpa a pesquisa e redefine todos os filtros da tabela para o estado inicial. */
    public static void clear(JTable table) {
        if (table == null) return;
        Object action = table.getClientProperty(CLEAR_ACTION_KEY);
        if (action instanceof Runnable r) {
            r.run();
        }
    }

    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    /**
     * Lógica pura: a linha passa se o texto de pesquisa aparecer em alguma célula (case-insensitive)
     * e cada coluna com filtro corresponder ao valor escolhido (com suporte bi-direcional a enums e rótulos PT-MZ).
     */
    public static boolean rowMatches(List<String> cells, String search, Map<Integer, String> exactByColumn) {
        if (search != null && !search.isBlank()) {
            String q = normalizeText(search);
            boolean any = false;
            for (String c : cells) {
                if (c != null) {
                    String cNorm = normalizeText(c);
                    String hNorm = normalizeText(UIHelper.humanStatus(c));
                    if (cNorm.contains(q) || hNorm.contains(q)) {
                        any = true;
                        break;
                    }
                }
            }
            if (!any) return false;
        }
        if (exactByColumn != null) {
            for (Map.Entry<Integer, String> en : exactByColumn.entrySet()) {
                String want = en.getValue();
                if (want == null || want.isBlank()) continue;
                int col = en.getKey();
                String cell = (col >= 0 && col < cells.size()) ? cells.get(col) : "";
                if (!cellMatches(cell, want)) return false;
            }
        }
        return true;
    }

    /**
     * Verifica correspondência entre a célula e o critério de filtro.
     * Suporta tanto o enum em inglês (`PENDING`, `BILLED`) quanto o rótulo PT-MZ (`Pendente`, `Facturado`).
     */
    public static boolean cellMatches(String cell, String want) {
        if (want == null || want.isBlank() || want.startsWith("Todos") || want.startsWith("Todas") || want.equalsIgnoreCase("Todos")) {
            return true;
        }
        if (cell == null) cell = "";
        cell = cell.trim();
        want = want.trim();
        if (want.equalsIgnoreCase(cell)) return true;

        String cellNorm = normalizeText(cell);
        String wantNorm = normalizeText(want);
        if (cellNorm.equalsIgnoreCase(wantNorm)) return true;

        String cellHuman = UIHelper.humanStatus(cell);
        if (want.equalsIgnoreCase(cellHuman) || wantNorm.equalsIgnoreCase(normalizeText(cellHuman))) return true;

        String wantHuman = UIHelper.humanStatus(want);
        if (cell.equalsIgnoreCase(wantHuman) || cellNorm.equalsIgnoreCase(normalizeText(wantHuman))) return true;

        if (cellHuman.equalsIgnoreCase(wantHuman) || normalizeText(cellHuman).equalsIgnoreCase(normalizeText(wantHuman))) return true;

        // Equivalência canónica de encomenda: "PENDING" corresponde a "Pendente" ou "Por facturar"
        if (("PENDING".equalsIgnoreCase(cell) || "POR FACTURAR".equalsIgnoreCase(cellNorm) || "PENDENTE".equalsIgnoreCase(cellNorm))
                && ("PENDENTE".equalsIgnoreCase(wantNorm) || "POR FACTURAR".equalsIgnoreCase(wantNorm) || "PENDING".equalsIgnoreCase(want))) {
            return true;
        }
        return false;
    }

    private static String normalizeText(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
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

    /** Intervalo inclusivo aplicado a uma coluna cuja data começa por dd/MM/yyyy. */
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

    public static boolean matchesDateRange(java.time.LocalDate date, java.time.LocalDate from,
                                           java.time.LocalDate to) {
        if (from == null && to == null) return true;
        if (date == null) return false;
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    /** Combo de período pronto (com ícone de calendário via label na barra). */
    /** Períodos para colunas de datas <b>passadas</b>: emissão, pagamento, movimento. */
    public static JComboBox<String> periodCombo() {
        return combo("Todo o período", "Hoje", "Ontem", "Esta semana", "Últimos 7 dias", "Este mês", "Últimos 30 dias", "Este ano");
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
            case "Ontem" -> date.isEqual(today.minusDays(1));
            case "Esta semana" -> {
                java.time.LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                yield !date.isBefore(monday) && !date.isAfter(today);
            }
            case "Últimos 7 dias" -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today);
            case "Este mês" -> date.getMonthValue() == today.getMonthValue() && date.getYear() == today.getYear() && !date.isAfter(today);
            case "Últimos 30 dias" -> !date.isBefore(today.minusDays(29)) && !date.isAfter(today);
            case "Este ano" -> date.getYear() == today.getYear() && !date.isAfter(today);
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
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    public static JLabel label(String text) {
        return label(text, null);
    }

    /**
     * Barra de filtros simples: apenas os componentes à esquerda.
     * A pesquisa já tem a lupa <b>dentro</b> do input ({@link SearchField}).
     */
    public static JPanel bar(JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0) {
            @Override
            public void layoutContainer(java.awt.Container target) {
                synchronized (target.getTreeLock()) {
                    java.awt.Insets insets = target.getInsets();
                    int x = insets.left;
                    int hgap = getHgap();
                    int targetHeight = target.getHeight() - (insets.top + insets.bottom);
                    for (int i = 0; i < target.getComponentCount(); i++) {
                        java.awt.Component m = target.getComponent(i);
                        if (m.isVisible()) {
                            java.awt.Dimension d = m.getPreferredSize();
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

    /**
     * Barra de ferramentas canónica: filtros/pesquisa à esquerda, botões de acção à direita.
     * Gap padrão de 8 px entre elementos; fundo transparente. Usar em todos os painéis.
     *
     * <pre>
     * [ SearchField ]  [ Label: ]  [ Combo ]  ──→ glue ←──  [ BtnSec ]  [ BtnPrimary ]
     * </pre>
     *
     * @param filters componentes de filtro (SearchField, labels, combos) — montados à esquerda
     * @param actions botões de acção — montados à direita
     */
    public static JPanel toolbar(JComponent[] filters, JComponent[] actions) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        if (filters != null && filters.length > 0) {
            JPanel left = bar(filters);
            p.add(left, BorderLayout.WEST);
        }
        if (actions != null && actions.length > 0) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0) {
                @Override
                public void layoutContainer(java.awt.Container target) {
                    synchronized (target.getTreeLock()) {
                        java.awt.Insets insets = target.getInsets();
                        int x = target.getWidth() - insets.right;
                        int hgap = getHgap();
                        int targetHeight = target.getHeight() - (insets.top + insets.bottom);
                        for (int i = target.getComponentCount() - 1; i >= 0; i--) {
                            java.awt.Component m = target.getComponent(i);
                            if (m.isVisible()) {
                                java.awt.Dimension d = m.getPreferredSize();
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

    /**
     * Separador visual vertical entre grupos de botões na toolbar.
     * Largura de 1 px, 20 px de altura, cor {@link UIHelper#BORDER}.
     */
    public static JComponent separator() {
        JPanel sep = new JPanel();
        sep.setOpaque(true);
        sep.setBackground(UIHelper.BORDER);
        sep.setPreferredSize(new java.awt.Dimension(1, 20));
        return sep;
    }
}

