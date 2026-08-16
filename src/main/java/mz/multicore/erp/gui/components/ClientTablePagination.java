package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Paginação local uniforme para tabelas de listagem já carregadas pelo desktop. */
public final class ClientTablePagination {

    public static final String DISABLED = "clientPagination.disabled";
    private static final String INSTANCE = "clientPagination.instance";
    private static final Integer[] PAGE_SIZES = {25, 50, 100, 200};
    static final int CONTROL_GAP = 8;

    private final JTable table;
    private final TableRowSorter<TableModel> sorter;
    private final JLabel status = new JLabel(" ");
    private final ModernButton first = button("fas-angle-double-left", "Primeira página");
    private final ModernButton previous = button("fas-angle-left", "Página anterior");
    private final ModernButton next = button("fas-angle-right", "Página seguinte");
    private final ModernButton last = button("fas-angle-double-right", "Última página");
    private final JComboBox<Integer> pageSize = new JComboBox<>(PAGE_SIZES);
    private final JPanel component = new JPanel(new BorderLayout());

    private RowFilter<TableModel, Integer> baseFilter;
    private int page;
    private int matchingRows;
    private boolean applying;

    @SuppressWarnings("unchecked")
    private ClientTablePagination(JTable table, TableRowSorter<TableModel> sorter) {
        this.table = table;
        this.sorter = sorter;
        this.baseFilter = (RowFilter<TableModel, Integer>) (RowFilter<?, ?>) sorter.getRowFilter();
        component.setOpaque(false);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(8, 4, 8, 4)));
        status.setForeground(UIHelper.TEXT_MUTED);
        status.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        component.add(status, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, CONTROL_GAP, 0));
        controls.setOpaque(false);
        UIHelper.styleComboBox(pageSize);
        pageSize.setSelectedItem(50);
        pageSize.getAccessibleContext().setAccessibleName("Registos por página");
        controls.add(new JLabel("Por página:"));
        controls.add(pageSize);
        controls.add(first);
        controls.add(previous);
        controls.add(next);
        controls.add(last);
        component.add(controls, BorderLayout.EAST);

        pageSize.addActionListener(e -> { page = 0; apply(); });
        first.addActionListener(e -> go(0));
        previous.addActionListener(e -> go(page - 1));
        next.addActionListener(e -> go(page + 1));
        last.addActionListener(e -> go(totalPages() - 1));
        table.getModel().addTableModelListener(this::modelChanged);
        apply();
    }

    /** Instala a barra e devolve o componente a colocar sob a tabela. */
    @SuppressWarnings("unchecked")
    public static JPanel install(JTable table) {
        Object existing = table.getClientProperty(INSTANCE);
        if (existing instanceof ClientTablePagination pagination) return pagination.component;
        if (!(table.getRowSorter() instanceof TableRowSorter<?> raw)) return new JPanel();
        ClientTablePagination pagination = new ClientTablePagination(
                table, (TableRowSorter<TableModel>) raw);
        table.putClientProperty(INSTANCE, pagination);
        return pagination.component;
    }

    /** Define o filtro funcional da listagem; a página é aplicada depois dele. */
    @SuppressWarnings("unchecked")
    public static void setBaseFilter(JTable table, RowFilter<? extends TableModel, ? extends Integer> filter) {
        Object value = table.getClientProperty(INSTANCE);
        if (value instanceof ClientTablePagination pagination) {
            pagination.baseFilter = (RowFilter<TableModel, Integer>) filter;
            pagination.page = 0;
            pagination.apply();
        } else if (table.getRowSorter() instanceof TableRowSorter<?> sorter) {
            ((TableRowSorter<TableModel>) sorter).setRowFilter(
                    (RowFilter<TableModel, Integer>) filter);
        }
    }

    private void modelChanged(TableModelEvent ignored) {
        SwingUtilities.invokeLater(() -> { page = Math.min(page, totalPages() - 1); apply(); });
    }

    private void go(int target) {
        page = Math.max(0, Math.min(target, totalPages() - 1));
        apply();
    }

    private void apply() {
        if (applying) return;
        applying = true;
        try {
            List<Integer> matches = new ArrayList<>();
            for (int row = 0; row < table.getModel().getRowCount(); row++) {
                ModelEntry entry = new ModelEntry(table.getModel(), row);
                if (baseFilter == null || baseFilter.include(entry)) matches.add(row);
            }
            matchingRows = matches.size();
            int pages = totalPages();
            page = Math.max(0, Math.min(page, pages - 1));
            int size = selectedPageSize();
            int from = Math.min(page * size, matchingRows);
            int to = Math.min(from + size, matchingRows);
            Set<Integer> visible = new HashSet<>(matches.subList(from, to));
            sorter.setRowFilter(new RowFilter<>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    return visible.contains(entry.getIdentifier());
                }
            });
            status.setText(matchingRows == 0 ? "Sem registos"
                    : String.format("Página %d de %d · %d registo(s)", page + 1, pages, matchingRows));
            first.setEnabled(page > 0);
            previous.setEnabled(page > 0);
            next.setEnabled(page + 1 < pages);
            last.setEnabled(page + 1 < pages);
        } finally {
            applying = false;
        }
    }

    private int selectedPageSize() { return (Integer) pageSize.getSelectedItem(); }
    private int totalPages() { return Math.max(1, (matchingRows + selectedPageSize() - 1) / selectedPageSize()); }

    private static ModernButton button(String icon, String tooltip) {
        ModernButton button = UIHelper.createSecondaryButton("");
        button.setIcon(UIHelper.icon(icon, 12));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private static final class ModelEntry extends RowFilter.Entry<TableModel, Integer> {
        private final TableModel model;
        private final int row;
        private ModelEntry(TableModel model, int row) { this.model = model; this.row = row; }
        @Override public TableModel getModel() { return model; }
        @Override public int getValueCount() { return model.getColumnCount(); }
        @Override public Object getValue(int index) { return model.getValueAt(row, index); }
        @Override public String getStringValue(int index) {
            Object value = getValue(index);
            return value == null ? "" : value.toString();
        }
        @Override public Integer getIdentifier() { return row; }
    }
}
