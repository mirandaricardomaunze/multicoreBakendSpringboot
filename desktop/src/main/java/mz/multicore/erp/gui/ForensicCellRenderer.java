package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Renderizador semafórico de células para a grelha de anomalias forenses. */
public class ForensicCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFocus, int row, int col) {
        Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);

        if (!isSel) {
            c.setBackground(row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.BG_DARK);
        }

        if (col == 1) { // Severidade
            String sev = String.valueOf(val);
            if ("CRITICAL".equals(sev)) {
                c.setForeground(UIHelper.REJECTED_RED);
                setFont(getFont().deriveFont(Font.BOLD));
            } else if ("SUSPICIOUS".equals(sev)) {
                c.setForeground(UIHelper.PENDING_YELLOW);
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                c.setForeground(UIHelper.TEXT_MUTED);
            }
            setHorizontalAlignment(CENTER);
        } else if (col == 5) { // Impacto
            setHorizontalAlignment(RIGHT);
            c.setForeground(UIHelper.TEXT_LIGHT);
        } else {
            c.setForeground(UIHelper.TEXT_LIGHT);
            setHorizontalAlignment(LEFT);
        }

        return c;
    }
}
