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
                    // Só o que precisa de atenção pinta a linha: o estado normal fica na etiqueta.
                    setBackground(status == null || status.equals(UIHelper.APPROVED_GREEN) ? base : blend(base, status, 0.10f));
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
                    Color statusText = statusColorFor(valStr.toUpperCase());
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

    /** Texto apresentado na célula: decimais em formato pt-MZ, preservando sufixos (MT, un, kg, %). */
    static String formatValue(Object value) {
        if (value instanceof BigDecimal bd) return TableCellRenderers.format(bd, 2, "");
        if (value instanceof Double d) return TableCellRenderers.format(BigDecimal.valueOf(d), 2, "");
        if (value instanceof Float f) return TableCellRenderers.format(BigDecimal.valueOf(f), 2, "");
        if (value instanceof String s && !s.trim().isEmpty() && !s.equals("—") && !s.equals("-")) {
            String trimmed = s.trim();
            boolean hasSign = trimmed.startsWith("+");
            String clean = trimmed.replaceAll("^[+]", "").replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$|%|un|kg)\\s*$", "").trim();
            if (clean.isEmpty()) return s;
            clean = MoneyField.normalize(clean);
            if (clean.matches("^\\d+\\.\\d+$")) {
                try {
                    BigDecimal parsed = new BigDecimal(clean);
                    String suffix = trimmed.endsWith(" MT") ? " MT"
                            : trimmed.endsWith(" un") ? " un"
                            : trimmed.endsWith(" kg") ? " kg"
                            : trimmed.endsWith("%") ? "%" : "";
                    return (hasSign && parsed.signum() > 0 ? "+" : "") + TableCellRenderers.format(parsed, 2, suffix);
                } catch (Exception ex) {
                    return s;
                }
            }
            return s;
        }
        return value.toString();
    }

    static Color statusColorFor(String upper) {
        if (upper == null) return null;
        if (eqAny(upper, "APPROVED", "APROVADO", "APROVADA", "RESOLVED", "PAID", "PAGO",
                "RECEIVED", "RECEBIDO", "RECEBIDA", "ATIVO", "ACTIVO", "ATIVA", "ACTIVA", "OK", "EM STOCK")) {
            return UIHelper.APPROVED_GREEN;
        }
        if (eqAny(upper, "REJECTED", "REJEITADO", "CANCELLED", "CANCELADO", "CANCELADA",
                "ANULADO", "ANULADA", "ESGOTADO", "SEM STOCK", "INATIVO", "INACTIVO") || upper.startsWith("VENCIDO")) {
            return UIHelper.REJECTED_RED;
        }
        if (upper.contains("PENDING") || upper.contains("PENDENTE") || upper.startsWith("VENCE")
                || upper.equals("BAIXO") || upper.equals("STOCK BAIXO")
                || upper.contains("DÍVIDA") || upper.contains("DIVIDA")
                || upper.contains("PARCIAL") || upper.contains("PARTIALLY")
                || upper.contains("DIVERG") || upper.contains("DISCREPANCY")) {
            return UIHelper.PENDING_YELLOW;
        }
        return null;
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
        return v == null ? null : statusColorFor(v.toString().toUpperCase());
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
        String cleaned = s.replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$|%)\\s*$", "").trim();
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
