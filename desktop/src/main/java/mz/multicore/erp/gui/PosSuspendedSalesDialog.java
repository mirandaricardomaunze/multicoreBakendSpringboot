package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.pos.suspended.PosSuspendedSale;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Lista as vendas em espera do posto para retomar ou eliminar. */
final class PosSuspendedSalesDialog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private PosSuspendedSalesDialog() {}

    static PosSuspendedSale pick(List<PosSuspendedSale> sales, Consumer<PosSuspendedSale> onDiscard) {
        List<PosSuspendedSale> rows = new ArrayList<>(sales);
        DefaultTableModel model = new DefaultTableModel(new String[]{"Hora", "Operador", "Cliente", "Artigos", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        rows.forEach(sale -> model.addRow(row(sale)));

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        table.putClientProperty("noRowInspector", Boolean.TRUE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionInterval(0, 0);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(620, 240));
        UIHelper.styleScrollPane(scroll);

        PosSuspendedSale[] selected = {null};
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Vendas em Espera", "fas-pause-circle",
                "Seleccione a venda a retomar. Os preços são actualizados ao retomar.", scroll)
                .setConfirmButton("Retomar", "fas-play-circle")
                .setOnSave(() -> {
                    if (table.getSelectedRow() < 0) throw new IllegalArgumentException("Seleccione uma venda.");
                    selected[0] = rows.get(table.convertRowIndexToModel(table.getSelectedRow()));
                });

        ModernButton discardBtn = UIHelper.createDangerButton("Eliminar");
        discardBtn.setIcon(UIHelper.icon("fas-trash", 14, Color.WHITE));
        discardBtn.addActionListener(e -> {
            if (table.getSelectedRow() < 0) return;
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            if (!ModernMessageDialog.confirm(dialog.getDialog(), FeedbackType.WARNING, "Eliminar venda em espera",
                    "Esta venda será eliminada e não pode ser recuperada.", "Eliminar")) return;
            onDiscard.accept(rows.remove(row));
            model.removeRow(row);
            if (rows.isEmpty()) dialog.close();
            else table.setRowSelectionInterval(0, 0);
        });
        dialog.addActionButton(discardBtn);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) dialog.submit();
            }
        });

        return dialog.showDialog() ? selected[0] : null;
    }

    private static Object[] row(PosSuspendedSale sale) {
        String client = sale.clientName() == null || sale.clientName().isBlank() ? "Consumidor Final" : sale.clientName();
        return new Object[]{sale.suspendedAt().format(TIME), sale.operator(), client, sale.lines().size(),
                String.format("%,.2f MT", sale.total())};
    }
}
