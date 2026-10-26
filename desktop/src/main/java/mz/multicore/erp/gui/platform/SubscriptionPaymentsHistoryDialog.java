package mz.multicore.erp.gui.platform;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;

/**
 * Diálogo especializado para consulta do histórico de pagamentos de uma assinatura.
 */
public class SubscriptionPaymentsHistoryDialog {

    public static void show(Window owner, SubscriptionDTO sub, List<SubscriptionPaymentDTO> payments) {
        String[] cols = {"Pago em", "Valor", "Método", "Referência", "Detalhes", "Período", "Nota"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (SubscriptionPaymentDTO p : payments) {
            String period = (p.periodStart() == null ? "—" : p.periodStart().toString())
                    + " a " + (p.periodEnd() == null ? "—" : p.periodEnd().toString());
            String ref = p.reference() == null || p.reference().isBlank() ? "—" : p.reference();
            String details = p.paymentDetails() == null || p.paymentDetails().isBlank() ? "—" : p.paymentDetails();
            model.addRow(new Object[]{
                    p.paidAt() == null ? "" : p.paidAt().toString(),
                    p.amount(),
                    p.methodLabel(),
                    ref,
                    details,
                    period,
                    p.note() == null ? "" : p.note()
            });
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setMinimumSize(new Dimension(720, 280));

        ModernFormDialog dlg = new ModernFormDialog(owner, "Histórico de Pagamentos",
                "fas-receipt", sub.companyName(), scroll).asReadOnly("Fechar");
        dlg.setSize(820, 460);
        dlg.showDialog();
    }
}
