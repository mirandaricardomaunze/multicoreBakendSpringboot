package com.phcpro.gui.commercial;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.client.MovimentosApiClient;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.movimentos.dto.MovimentoDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Aba autónoma da vista unificada de documentos comerciais. */
public final class CommercialMovementsPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MovimentosApiClient apiClient;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search;
    private final JComboBox<String> period;
    private final JLabel footer;
    private List<MovimentoDTO> data = List.of();

    public CommercialMovementsPanel(MovimentosApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Movimentos — Todos os Documentos Comerciais"), BorderLayout.WEST);
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refresh);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        model = new DefaultTableModel(new String[]{"Tipo", "Nº", "Cliente", "Data", "Estado", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        search = TableFilter.searchField("Nº documento ou cliente…");
        period = TableFilter.periodCombo();
        TableFilter.install(table, search, List.of(), List.of(new TableFilter.PeriodFilter(period, 3)));
        UIHelper.onTextChange(search, this::updateFooter);
        period.addActionListener(e -> updateFooter());
        JPanel filterBar = TableFilter.bar(search, TableFilter.label("Data:", "fas-calendar-alt"), period);
        filterBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filterBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        footer = new JLabel(" ");
        footer.setForeground(UIHelper.TEXT_MUTED);
        footer.setBorder(new EmptyBorder(8, 4, 0, 4));
        card.add(footer, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.listar(companyId, "", null, null), this::apply,
                error -> JOptionPane.showMessageDialog(this,
                        "Não foi possível carregar movimentos comerciais: " + error.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private void apply(List<MovimentoDTO> loaded) {
        data = loaded;
        model.setRowCount(0);
        for (MovimentoDTO movement : data) {
            model.addRow(new Object[]{
                    movement.tipo().getLabel(),
                    movement.numero() == null ? "-" : movement.numero(),
                    movement.cliente(),
                    movement.data() == null ? "-" : movement.data().format(DATE_TIME),
                    movement.estado(),
                    movement.total() == null ? BigDecimal.ZERO : movement.total()
            });
        }
        updateFooter();
    }

    private void updateFooter() {
        String query = search.getText();
        String selectedPeriod = String.valueOf(period.getSelectedItem());
        LocalDate today = LocalDate.now();
        int count = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (MovimentoDTO movement : data) {
            BigDecimal total = movement.total() == null ? BigDecimal.ZERO : movement.total();
            String date = movement.data() == null ? "-" : movement.data().format(DATE_TIME);
            List<String> cells = List.of(movement.tipo().getLabel(),
                    movement.numero() == null ? "-" : movement.numero(),
                    movement.cliente() == null ? "" : movement.cliente(), date,
                    movement.estado() == null ? "" : movement.estado(), total.toPlainString());
            if (!TableFilter.rowMatches(cells, query, Map.of())) continue;
            if (!TableFilter.matchesPeriod(TableFilter.parseCellDate(date), selectedPeriod, today)) continue;
            count++;
            sum = sum.add(total);
        }
        footer.setText(String.format("%d documento(s) · Total: %,.2f MT", count, sum));
    }
}
