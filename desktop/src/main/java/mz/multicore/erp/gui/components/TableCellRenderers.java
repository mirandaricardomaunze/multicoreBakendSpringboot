package mz.multicore.erp.gui.components;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Font;
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
        return new NumericRenderer(2, "");
    }

    /** Estado como texto semântico, sem aparência de botão dentro da tabela. */
    public static TableCellRenderer status() {
        return new StatusRenderer();
    }

    /**
     * Via da encomenda. A célula guarda o {@code OrderKind}; aqui mostra-se o rótulo PT-MZ.
     *
     * <p>Guardar o enum e traduzir só na apresentação é o que permite às acções perguntarem à
     * linha <em>que via é</em> em vez de compararem texto traduzido.
     */
    public static TableCellRenderer orderKind() {
        return new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof mz.multicore.erp.modules.comercial.model.OrderKind kind
                        ? kind.label() : "—");
            }
        };
    }

    public static TableCellRenderer role() {
        return new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(UIHelper.humanRole(value == null ? null : value.toString()));
            }
        };
    }

    public static String format(BigDecimal value, int scale, String suffix) {
        if (value == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        String decimals = scale == 0 ? "" : "." + "0".repeat(scale);
        DecimalFormat fmt = new DecimalFormat("#,##0" + decimals, symbols);
        fmt.setMinimumFractionDigits(scale);
        fmt.setMaximumFractionDigits(scale);
        return fmt.format(value) + suffix;
    }

    // ── Renderer numérico (dinheiro / quantidade) ─────────────────────────────────────────────

    private static final class NumericRenderer extends DefaultTableCellRenderer {
        private final int scale;
        private final String suffix;

        private NumericRenderer(int scale, String suffix) {
            this.scale = scale;
            this.suffix = suffix;
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override protected void setValue(Object value) {
            BigDecimal number = null;
            if (value instanceof BigDecimal decimal) {
                number = decimal;
            } else if (value instanceof Number n) {
                number = new BigDecimal(n.toString());
            } else if (value instanceof String s && !s.trim().isEmpty() && !s.equals("—") && !s.equals("-")) {
                try {
                    String clean = s.trim().replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$|%|un|kg)\\s*$", "").trim();
                    clean = MoneyField.normalize(clean);
                    if (clean.matches("^[+-]?\\d+(?:\\.\\d+)?$")) {
                        number = new BigDecimal(clean);
                    }
                } catch (Exception ignored) {
                }
            }
            setText(number == null && value != null ? String.valueOf(value) : format(number, scale, suffix));
        }
    }

    // ── Renderer de estado: texto semântico ──────────────────────────────────────────────────

    private static final class StatusRenderer extends DefaultTableCellRenderer {

        private StatusRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, null, selected, focus, row, column);
            String raw = value == null ? "" : value.toString().trim().toUpperCase(java.util.Locale.ROOT);
            String human = UIHelper.humanStatus(raw);
            String label = "EM STOCK".equals(raw) ? "Disponível" : human;
            setText(label.isEmpty() ? label : label.substring(0, 1).toUpperCase(java.util.Locale.forLanguageTag("pt-MZ"))
                    + label.substring(1).toLowerCase(java.util.Locale.forLanguageTag("pt-MZ")));
            StatusTone.Tone tone = StatusTone.toneOf(raw);
            if (tone == StatusTone.Tone.NEUTRAL) tone = StatusTone.toneOf(human);
            setForeground(selected ? table.getSelectionForeground() : StatusTone.colorOf(tone));
            return this;
        }
    }
}
