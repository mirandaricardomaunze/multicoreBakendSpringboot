package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Renderizador visual especializado para a matriz de aging, crachás de risco e situação.
 */
public class CreditRiskCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        setHorizontalAlignment(SwingConstants.LEFT);

        // Alinhamento à direita para valores monetários e atrasos
        if (col >= 3 && col <= 12) {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT);
            c.setForeground(UIHelper.TEXT_LIGHT);

            // Destaque de risco na coluna 13 (Nível Risco)
            if (col == 13 && value != null) {
                String str = value.toString();
                if ("Crítico".equalsIgnoreCase(str)) {
                    c.setForeground(UIHelper.REJECTED_RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("Alto".equalsIgnoreCase(str)) {
                    c.setForeground(UIHelper.KPI_ORANGE_DARK);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("Médio".equalsIgnoreCase(str)) {
                    c.setForeground(UIHelper.PENDING_YELLOW);
                } else {
                    c.setForeground(UIHelper.APPROVED_GREEN);
                }
            }

            // Destaque na coluna 14 (Situação)
            if (col == 14 && value != null) {
                String str = value.toString();
                if ("BLOQUEADO".equalsIgnoreCase(str)) {
                    c.setForeground(UIHelper.REJECTED_RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("EXCEPÇÃO".equalsIgnoreCase(str)) {
                    c.setForeground(UIHelper.ACCENT_BLUE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(UIHelper.APPROVED_GREEN);
                }
            }
        }

        return c;
    }
}
