package mz.multicore.erp.gui.components;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

    /**
     * Renderer de estado moderno com badge pill.
     * Pinta fundo arredondado semitransparente via Graphics2D — compatível com temas claro e escuro.
     */
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

    static String format(BigDecimal value, int scale, String suffix) {
        if (value == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
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
            BigDecimal number = value instanceof BigDecimal decimal
                    ? decimal
                    : value instanceof Number n ? new BigDecimal(n.toString()) : null;
            setText(number == null && value != null ? String.valueOf(value) : format(number, scale, suffix));
        }
    }

    // ── Renderer de estado: badge pill moderno ───────────────────────────────────────────────

    private static final class StatusRenderer extends DefaultTableCellRenderer {

        // Cores de fundo do badge (alpha ~30% — semitransparente sobre qualquer linha zebra)
        private static final Color BG_GREEN  = new Color(16,  185, 129, 45);
        private static final Color BG_YELLOW = new Color(245, 158,  11, 45);
        private static final Color BG_RED    = new Color(239,  68,  68, 45);
        private static final Color BG_BLUE   = new Color( 59, 130, 246, 45);
        private static final Color BG_GRAY   = new Color(107, 114, 128, 35);

        // Cor do contorno do badge (alpha ~60%)
        private static final Color BORDER_GREEN  = new Color(16,  185, 129, 90);
        private static final Color BORDER_YELLOW = new Color(245, 158,  11, 90);
        private static final Color BORDER_RED    = new Color(239,  68,  68, 90);
        private static final Color BORDER_BLUE   = new Color( 59, 130, 246, 90);
        private static final Color BORDER_GRAY   = new Color(107, 114, 128, 60);

        private Color badgeBg     = BG_GRAY;
        private Color badgeBorder = BORDER_GRAY;

        private StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(false); // pintamos o fundo manualmente via paintComponent
            setBorder(new EmptyBorder(3, 10, 3, 10));
            setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, null, selected, focus, row, column);
            setText(UIHelper.humanStatus(value == null ? null : value.toString()));

            if (selected) {
                badgeBg     = new Color(255, 255, 255, 30);
                badgeBorder = new Color(255, 255, 255, 70);
                setForeground(UIHelper.TEXT_LIGHT);
                setBackground(table.getSelectionBackground());
                setOpaque(true);
            } else {
                setOpaque(false);
                String status = value == null ? "" : value.toString().toUpperCase();
                if (isGreen(status)) {
                    badgeBg = BG_GREEN; badgeBorder = BORDER_GREEN;
                    setForeground(UIHelper.APPROVED_GREEN);
                } else if (isRed(status)) {
                    badgeBg = BG_RED; badgeBorder = BORDER_RED;
                    setForeground(UIHelper.REJECTED_RED);
                } else if (isYellow(status)) {
                    badgeBg = BG_YELLOW; badgeBorder = BORDER_YELLOW;
                    setForeground(UIHelper.PENDING_YELLOW);
                } else if (isBlue(status)) {
                    badgeBg = BG_BLUE; badgeBorder = BORDER_BLUE;
                    setForeground(UIHelper.ACCENT_BLUE);
                } else {
                    badgeBg = BG_GRAY; badgeBorder = BORDER_GRAY;
                    setForeground(UIHelper.TEXT_MUTED);
                }
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!isOpaque()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int hPad = 4;  // margem horizontal da célula ao badge
                int vPad = 4;  // margem vertical
                int bw = w - hPad * 2;
                int bh = h - vPad * 2;
                int arc = Math.min(bh, 20); // pill: arco = altura total
                g2.setColor(badgeBg);
                g2.fillRoundRect(hPad, vPad, bw, bh, arc, arc);
                g2.setColor(badgeBorder);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(hPad, vPad, bw - 1, bh - 1, arc, arc);
                g2.dispose();
            }
            super.paintComponent(g);
        }

        private static boolean isGreen(String s) {
            return switch (s) {
                case "ACTIVE", "ACTIVA", "APPROVED", "PAID", "OPEN",
                     "ACTIVO", "APROVADO", "PAGA", "ABERTO", "RESOLVED",
                     "RESOLVIDO", "FATURADA", "MATCHED", "CONCILIADO" -> true;
                default -> false;
            };
        }

        private static boolean isRed(String s) {
            return switch (s) {
                case "INACTIVE", "INACTIVA", "REJECTED", "CANCELLED", "OVERDUE",
                     "INACTIVO", "REJEITADO", "ANULADO", "ANULADA", "EM ATRASO",
                     "IGNORED", "IGNORADO", "ESGOTADO" -> true;
                default -> false;
            };
        }

        private static boolean isYellow(String s) {
            return switch (s) {
                case "PENDING", "PENDING_APPROVAL", "PARTIALLY_PAID",
                     "PENDENTE", "IN_PROGRESS", "EM CURSO", "POR FATURAR",
                     "UNMATCHED", "POR CONCILIAR", "CRÍTICO", "CRITICO" -> true;
                default -> false;
            };
        }

        private static boolean isBlue(String s) {
            return switch (s) {
                case "PROCESSING", "EM PROCESSAMENTO", "SHIPPED", "ENVIADO",
                     "SCHEDULED", "AGENDADO", "BAIXO" -> true;
                default -> false;
            };
        }
    }
}
