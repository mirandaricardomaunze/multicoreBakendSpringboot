package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private TableRowSorter<TableModel> sorter;
    private final JLabel statusLeft  = new JLabel(" ");   // contagem de registos (esquerda)
    private final JLabel pageLabel   = new JLabel(" ");   // "Página X de Y" (centro)
    private final GhostNavButton first    = ghostBtn("fas-angle-double-left",  "Primeira página");
    private final GhostNavButton previous = ghostBtn("fas-angle-left",         "Página anterior");
    private final GhostNavButton next     = ghostBtn("fas-angle-right",        "Página seguinte");
    private final GhostNavButton last     = ghostBtn("fas-angle-double-right", "Última página");
    private final JComboBox<Integer> pageSize = new JComboBox<>(PAGE_SIZES);
    private final JPanel component = new JPanel(new BorderLayout(0, 0));

    private RowFilter<TableModel, Integer> baseFilter;
    private int page;
    private int matchingRows;
    private boolean applying;

    @SuppressWarnings("unchecked")
    private ClientTablePagination(JTable table, TableRowSorter<TableModel> sorter) {
        this.table = table;
        this.sorter = sorter;
        this.baseFilter = sorter != null ? (RowFilter<TableModel, Integer>) (RowFilter<?, ?>) sorter.getRowFilter() : null;
        component.setOpaque(false);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(6, 8, 6, 8)));

        // Esquerda: contagem de registos
        statusLeft.setForeground(UIHelper.TEXT_MUTED);
        statusLeft.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPanel.setOpaque(false);
        JLabel perPageLabel = new JLabel("Por página:");
        perPageLabel.setForeground(UIHelper.TEXT_MUTED);
        perPageLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        UIHelper.styleComboBox(pageSize);
        pageSize.setSelectedItem(50);
        pageSize.setPreferredSize(new Dimension(86, UIHelper.FORM_CONTROL_HEIGHT - 4));
        pageSize.getAccessibleContext().setAccessibleName("Registos por página");
        leftPanel.add(perPageLabel);
        leftPanel.add(pageSize);
        leftPanel.add(statusLeft);
        component.add(leftPanel, BorderLayout.WEST);

        // Centro: rótulo "Página X de Y"
        pageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pageLabel.setForeground(UIHelper.TEXT_LIGHT);
        pageLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        component.add(pageLabel, BorderLayout.CENTER);

        // Direita: botões de navegação ghost
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navPanel.setOpaque(false);
        navPanel.add(first);
        navPanel.add(previous);
        navPanel.add(next);
        navPanel.add(last);
        component.add(navPanel, BorderLayout.EAST);

        pageSize.addActionListener(e -> { page = 0; apply(); });
        first.addActionListener(e -> go(0));
        previous.addActionListener(e -> go(page - 1));
        next.addActionListener(e -> go(page + 1));
        last.addActionListener(e -> go(totalPages() - 1));
        table.getModel().addTableModelListener(this::modelChanged);

        table.addPropertyChangeListener("rowSorter", evt -> {
            if (evt.getNewValue() instanceof TableRowSorter<?> newSorter) {
                this.sorter = (TableRowSorter<TableModel>) newSorter;
                apply();
            }
        });
        table.addPropertyChangeListener("model", evt -> {
            if (evt.getNewValue() instanceof TableModel newModel) {
                newModel.addTableModelListener(this::modelChanged);
                if (this.sorter != null) {
                    this.sorter.setModel(newModel);
                } else {
                    this.sorter = new TableRowSorter<>(newModel);
                    table.setRowSorter(this.sorter);
                }
                page = 0;
                apply();
            }
        });

        apply();
    }

    /** Instala a barra e devolve o componente a colocar sob a tabela. */
    @SuppressWarnings("unchecked")
    public static JPanel install(JTable table) {
        Object existing = table.getClientProperty(INSTANCE);
        if (existing instanceof ClientTablePagination pagination) return pagination.component;
        TableRowSorter<TableModel> sorter;
        if (table.getRowSorter() instanceof TableRowSorter<?> raw) {
            sorter = (TableRowSorter<TableModel>) raw;
        } else {
            sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);
        }
        ClientTablePagination pagination = new ClientTablePagination(table, sorter);
        table.putClientProperty(INSTANCE, pagination);
        return pagination.component;
    }

    /** Verifica se a paginação de cliente já foi instalada para esta tabela. */
    public static boolean isInstalled(JTable table) {
        return table.getClientProperty(INSTANCE) instanceof ClientTablePagination;
    }

    public JPanel component() {
        return component;
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

    /**
     * Linhas do modelo que passam o filtro da listagem, <b>ignorando a paginação</b>.
     *
     * <p>É isto que uma exportação deve levar: o que o operador filtrou, e não a página que
     * calhou estar a ver. Exportar só a página seria mentir sobre o que está no ecrã; exportar o
     * modelo inteiro seria ignorar o filtro que ele acabou de escrever.</p>
     *
     * <p>Funciona com ou sem paginação instalada — sem ela, o filtro vive directamente no
     * {@code RowSorter}.</p>
     */
    @SuppressWarnings("unchecked")
    public static List<Integer> filteredModelRows(JTable table) {
        TableModel model = table.getModel();
        RowFilter<TableModel, Integer> filter = null;
        Object value = table.getClientProperty(INSTANCE);
        if (value instanceof ClientTablePagination pagination) {
            filter = pagination.baseFilter;
        } else if (table.getRowSorter() instanceof TableRowSorter<?> sorter) {
            filter = (RowFilter<TableModel, Integer>) ((TableRowSorter<TableModel>) sorter).getRowFilter();
        }
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (filter == null || filter.include(new ModelEntry(model, row))) {
                rows.add(row);
            }
        }
        return rows;
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
            if (matchingRows == 0) {
                statusLeft.setText("Sem registos");
                pageLabel.setText("");
            } else {
                statusLeft.setText(matchingRows + " registo(s)");
                pageLabel.setText(String.format("Página  %d  de  %d", page + 1, pages));
            }
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

    /**
     * Botão de navegação ghost circular (30×30).
     * Fundo transparente em repouso; acento suave ao passar o rato; desativado com opacidade reduzida.
     */
    private static GhostNavButton ghostBtn(String icon, String tooltip) {
        return new GhostNavButton(UIHelper.icon(icon, 12), tooltip);
    }

    private static final class GhostNavButton extends javax.swing.JButton {
        private boolean hovered;
        private static final int SIZE = 30;
        private static final int ARC  = SIZE; // círculo perfeito

        GhostNavButton(javax.swing.Icon icon, String tooltip) {
            super(icon);
            setToolTipText(tooltip);
            getAccessibleContext().setAccessibleName(tooltip);
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (!isEnabled()) {
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
            } else if (hovered) {
                g2.setColor(new Color(UIHelper.ACCENT_BLUE.getRed(),
                        UIHelper.ACCENT_BLUE.getGreen(), UIHelper.ACCENT_BLUE.getBlue(), 40));
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);
                g2.setColor(new Color(UIHelper.ACCENT_BLUE.getRed(),
                        UIHelper.ACCENT_BLUE.getGreen(), UIHelper.ACCENT_BLUE.getBlue(), 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
            }
            super.paintComponent(g);
            g2.dispose();
        }
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
