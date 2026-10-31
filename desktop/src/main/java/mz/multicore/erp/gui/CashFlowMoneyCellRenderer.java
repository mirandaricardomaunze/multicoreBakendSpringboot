package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.math.BigDecimal;

/** Renderizador semafórico de células monetárias para a matriz de fluxo de caixa previsional. */
public class CashFlowMoneyCellRenderer extends DefaultTableCellRenderer {

    private final boolean isCumulative;

    public CashFlowMoneyCellRenderer(boolean isCumulative) {
        this.isCumulative = isCumulative;
        setHorizontalAlignment(RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        if (value instanceof BigDecimal val) {
            setText(String.format(DisplayLocale.NUMBERS, "%,.2f MT", val));
            if (isCumulative) {
                setFont(getFont().deriveFont(Font.BOLD));
                if (val.compareTo(BigDecimal.ZERO) < 0) {
                    setForeground(UIHelper.REJECTED_RED);
                } else {
                    setForeground(UIHelper.APPROVED_GREEN);
                }
            } else if (col == 3) {
                if (val.compareTo(BigDecimal.ZERO) < 0) {
                    setForeground(UIHelper.REJECTED_RED);
                } else if (val.compareTo(BigDecimal.ZERO) > 0) {
                    setForeground(UIHelper.APPROVED_GREEN);
                }
            }
        }
        return c;
    }
}
