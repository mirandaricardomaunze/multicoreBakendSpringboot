package mz.multicore.erp.gui.components;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Renderers canónicos para valores repetidos nas tabelas do ERP. */
public final class TableCellRenderers {

    private TableCellRenderers() {}

    public static TableCellRenderer money() {
        return new NumericRenderer(2, " MT");
    }

    public static TableCellRenderer quantity() {
        return new NumericRenderer(3, "");
    }

    public static TableCellRenderer status() {
        return new StatusRenderer();
    }

    public static TableCellRenderer role() {
        return new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(UIHelper.humanRole(value == null ? null : value.toString()));
            }
        };
    }

    static String format(BigDecimal value, int scale, String suffix) {
        if (value == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        // O pattern de DecimalFormat usa sempre '.' para decimal e ',' para grouping;
        // os símbolos abaixo é que determinam a apresentação PT (vírgula + espaço).
        String decimals = scale == 0 ? "" : "." + "0".repeat(scale);
        DecimalFormat format = new DecimalFormat("#,##0" + decimals, symbols);
        format.setMinimumFractionDigits(scale);
        format.setMaximumFractionDigits(scale);
        return format.format(value) + suffix;
    }

    private static final class NumericRenderer extends DefaultTableCellRenderer {
        private final int scale;
        private final String suffix;

        private NumericRenderer(int scale, String suffix) {
            this.scale = scale;
            this.suffix = suffix;
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override protected void setValue(Object value) {
            BigDecimal number = value instanceof BigDecimal decimal
                    ? decimal
                    : value instanceof Number n ? new BigDecimal(n.toString()) : null;
            setText(number == null && value != null ? String.valueOf(value) : format(number, scale, suffix));
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        private StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(4, 8, 4, 8));
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, UIHelper.humanStatus(value == null ? null : value.toString()), selected, focus, row, column);
            if (!selected) {
                String status = value == null ? "" : value.toString().toUpperCase();
                Color color = switch (status) {
                    case "ACTIVE", "ACTIVA", "APPROVED", "PAID", "OPEN" -> UIHelper.APPROVED_GREEN;
                    case "PENDING", "PENDING_APPROVAL", "PARTIALLY_PAID" -> UIHelper.PENDING_YELLOW;
                    case "INACTIVE", "INACTIVA", "REJECTED", "CANCELLED", "OVERDUE" -> UIHelper.REJECTED_RED;
                    default -> UIHelper.TEXT_MUTED;
                };
                label.setForeground(color);
                label.setBackground(UIHelper.BG_CARD);
            }
            return label;
        }
    }
}
