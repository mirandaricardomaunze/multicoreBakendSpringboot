package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PickupApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pickup.dto.PendingPickupStockDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Read-only view of sold material still in the yard. A physical count finds this material on the floor
 * although the system stock no longer has it; this list explains the difference article by article.
 */
final class PendingYardStockDialog {

    private PendingYardStockDialog() {
    }

    static void show(JComponent parent, PickupApiClient api, InlineFeedbackPanel feedback) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(parent, () -> api.pendingStock(companyId), PendingYardStockDialog::open,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o material no estaleiro",
                        error.getMessage(), "Tentar novamente", () -> show(parent, api, feedback)));
    }

    private static void open(List<PendingPickupStockDTO> rows) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Código", "Artigo", "Armazém", "Unid.", "Por levantar", "Faturas"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (PendingPickupStockDTO row : rows) {
            model.addRow(new Object[]{row.productSku(), row.productName(), row.warehouseName(), PickupsPanel.unit(row.unitCode()),
                    row.pendingQuantity(), row.accountCount()});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.quantity());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        scroll.setPreferredSize(new Dimension(720, 360));

        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel hint = new JLabel("<html>Material já faturado (e já fora do stock do sistema) que continua fisicamente no "
                + "estaleiro. Some estas quantidades ao stock do sistema quando conferir uma contagem física.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);
        card.add(hint, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        new ModernFormDialog(UIHelper.mainWindow, "Material Vendido no Estaleiro", "fas-warehouse",
                rows.size() + (rows.size() == 1 ? " artigo por levantar" : " artigos por levantar"), card)
                .asReadOnly("Fechar")
                .showDialog();
    }
}
