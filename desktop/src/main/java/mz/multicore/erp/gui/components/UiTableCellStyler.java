package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;

/** Renderers canónicos de cabeçalho e células: zebra, alinhamento numérico e tinta de estado. */
final class UiTableCellStyler {

    private UiTableCellStyler() {}

    static void applyBaseAppearance(JTable table) {
        table.setBackground(UIHelper.BG_CARD);
        table.setForeground(UIHelper.TEXT_LIGHT);
        table.setGridColor(UIHelper.GRID);
        table.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        table.setRowHeight(UiDensityManager.getInstance().getDensity().getTableRowHeight());
        table.setSelectionBackground(UIHelper.SELECTION_BG);
        table.setSelectionForeground(UIHelper.TEXT_LIGHT);
        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setBorder(BorderFactory.createLineBorder(UIHelper.GRID));
    }

    static void installHeaderRenderer(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setBackground(UIHelper.TABLE_HEADER_BG);
        header.setForeground(UIHelper.TEXT_LIGHT);
        header.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        header.setPreferredSize(new Dimension(100, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIHelper.GRID));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                boolean numeric = isNumericColumn(t, col);
                setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBackground(UIHelper.TABLE_HEADER_BG);
                setForeground(UIHelper.TEXT_LIGHT);
                setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, UIHelper.GRID),
                        new EmptyBorder(0, numeric ? 8 : 12, 0, numeric ? 14 : 8)));
                return this;
            }
        });
    }

    static void installCellRenderer(JTable table) {
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                boolean numeric = (value instanceof Number) || (value != null && looksNumeric(value.toString()));
                setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, numeric ? 10 : 12, 0, numeric ? 14 : 10));

                if (isSelected) {
                    setBackground(t.getSelectionBackground());
                } else {
                    Color base = row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT;
                    Color status = rowStatusColor(t, row);
                    // Só o que precisa de atenção pinta a linha: o estado normal fica no texto.
                    setBackground(status == null ? base : blend(base, status, 0.10f));
                }

                if (value == null) {
                    setToolTipText(null);
                    return this;
                }
                String valStr = formatValue(value);
                setText(valStr); // igual a toString() quando não há reformatação decimal
                setToolTipText(valStr.length() > 30
                        ? "<html><body style='width: 250px; font-family: Segoe UI; font-size: 11px; padding: 4px;'>" + valStr.replace("\n", "<br>") + "</body></html>"
                        : valStr);

                if (isSelected) {
                    setForeground(UIHelper.TEXT_LIGHT);
                    setFont(getFont().deriveFont(Font.PLAIN));
                } else {
                    Color statusText = numeric ? null : statusColorFor(valStr);
                    setForeground(statusText != null ? statusText : UIHelper.TEXT_LIGHT);
                    setFont(getFont().deriveFont(statusText != null ? Font.BOLD : Font.PLAIN));
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
        table.setDefaultRenderer(Number.class, cellRenderer);
        table.setDefaultRenderer(String.class, cellRenderer);
    }

    /**
     * Texto apresentado na célula: decimais em formato pt-MZ, preservando sufixos (MT, un, kg, %) e o sinal, e datas
     * sempre como dd/MM/yyyy (com HH:mm quando há hora), mesmo quando chegam em ISO (2026-11-14T10:20).
     */
    static String formatValue(Object value) {
        String date = formatDate(value);
        if (date != null) return date;
        if (value instanceof BigDecimal bd) return TableCellRenderers.format(bd, 2, "");
        if (value instanceof Double d) return TableCellRenderers.format(BigDecimal.valueOf(d), 2, "");
        if (value instanceof Float f) return TableCellRenderers.format(BigDecimal.valueOf(f), 2, "");
        if (value instanceof String s && !s.trim().isEmpty() && !s.equals("—") && !s.equals("-")) {
            String trimmed = s.trim();
            boolean hasSign = trimmed.startsWith("+");
            boolean negative = trimmed.startsWith("-");
            String clean = trimmed.replaceAll("^[+-]", "").replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$|%|un|kg)\\s*$", "").trim();
            if (clean.isEmpty()) return s;
            clean = MoneyField.normalize(clean);
            if (clean.matches("^\\d+\\.\\d+$")) {
                try {
                    BigDecimal parsed = new BigDecimal(clean);
                    String suffix = trimmed.endsWith(" MT") ? " MT"
                            : trimmed.endsWith(" un") ? " un"
                            : trimmed.endsWith(" kg") ? " kg"
                            : trimmed.endsWith("%") ? "%" : "";
                    String sign = negative && parsed.signum() != 0 ? "-" : hasSign && parsed.signum() > 0 ? "+" : "";
                    return sign + TableCellRenderers.format(parsed, 2, suffix);
                } catch (Exception ex) {
                    return s;
                }
            }
            return s;
        }
        return value.toString();
    }

    private static final java.util.regex.Pattern ISO_DATE = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final java.util.regex.Pattern ISO_DATE_TIME =
            java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?");

    /** dd/MM/yyyy or dd/MM/yyyy HH:mm for a date value or an ISO date text; null when it is not a date. */
    static String formatDate(Object value) {
        try {
            if (value instanceof java.time.LocalDate d) return UIHelper.formatDate(d);
            if (value instanceof java.time.LocalDateTime d) return UIHelper.formatDateTime(d);
            if (value instanceof java.time.OffsetDateTime d) return UIHelper.formatDateTime(d.atZoneSameInstant(java.time.ZoneId.systemDefault()));
            if (value instanceof java.time.ZonedDateTime d) return UIHelper.formatDateTime(d.withZoneSameInstant(java.time.ZoneId.systemDefault()));
            if (value instanceof String s) {
                String t = s.trim();
                if (ISO_DATE.matcher(t).matches()) return UIHelper.formatDate(java.time.LocalDate.parse(t));
                if (ISO_DATE_TIME.matcher(t).matches()) return UIHelper.formatDateTime(java.time.LocalDateTime.parse(t));
            }
        } catch (java.time.DateTimeException ex) {
            return null;
        }
        return null;
    }

    /** Colour of a status text, or null when the text is not a known status (shared with {@link StatusTone}). */
    static Color statusColorFor(String upper) {
        StatusTone.Tone tone = StatusTone.toneOf(upper);
        return tone == StatusTone.Tone.NEUTRAL ? null : StatusTone.colorOf(tone);
    }

    private static boolean eqAny(String s, String... opts) {
        for (String o : opts) if (o.equals(s)) return true;
        return false;
    }

    private static int statusColumnIndex(JTable t) {
        for (int i = 0; i < t.getColumnCount(); i++) {
            String n = t.getColumnName(i);
            if (n == null) continue;
            String u = n.trim().toLowerCase();
            if (u.equals("estado") || u.equals("status") || u.equals("situação") || u.equals("situacao")) return i;
        }
        return -1;
    }

    private static Color rowStatusColor(JTable t, int viewRow) {
        int col = statusColumnIndex(t);
        if (col < 0) return null;
        Object v = t.getValueAt(viewRow, col);
        if (v == null) return null;
        StatusTone.Tone tone = StatusTone.toneOf(v.toString());
        return tone == StatusTone.Tone.BAD || tone == StatusTone.Tone.WARN ? StatusTone.colorOf(tone) : null;
    }

    private static Color blend(Color base, Color accent, float ratio) {
        float r = 1f - ratio;
        return new Color(
                Math.round(base.getRed() * r + accent.getRed() * ratio),
                Math.round(base.getGreen() * r + accent.getGreen() * ratio),
                Math.round(base.getBlue() * r + accent.getBlue() * ratio));
    }

    static boolean looksNumeric(String raw) {
        if (raw == null) return false;
        String s = raw.trim();
        if (s.isEmpty()) return false;
        String cleaned = s.replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$|%|un|kg)\\s*$", "").trim().replaceFirst("^\\+", "");
        if (cleaned.isEmpty()) return false;
        return cleaned.matches("-?\\d{1,3}([.\\s]\\d{3})*(,\\d+)?")
                || cleaned.matches("-?\\d{1,3}(,\\d{3})*(\\.\\d+)?")
                || cleaned.matches("-?\\d+([.,]\\d+)?");
    }

    static boolean isNumericColumn(JTable table, int viewCol) {
        int modelCol = table.convertColumnIndexToModel(viewCol);
        Class<?> declared = table.getModel().getColumnClass(modelCol);
        if (Number.class.isAssignableFrom(declared)) return true;
        if (declared != Object.class && declared != String.class) return false;
        int rows = Math.min(table.getRowCount(), 25);
        boolean sawValue = false;
        // Defensivo: um paint pode ocorrer com o modelo a meio de uma mutação.
        try {
            for (int r = 0; r < rows && r < table.getRowCount(); r++) {
                Object v = table.getValueAt(r, viewCol);
                if (v == null || v.toString().trim().isEmpty()) continue;
                sawValue = true;
                if (v instanceof Number) continue;
                if (!looksNumeric(v.toString())) return false;
            }
        } catch (RuntimeException ex) {
            return false;
        }
        return sawValue;
    }
}
