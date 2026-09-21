package mz.multicore.erp.gui.components;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gráfico de pizza clássico / donut desenhado em Java 2D vetorial anti-aliased.
 * Exibe fatias coloridas, percentagens desenhadas diretamente no interior das fatias,
 * linhas divisórias nítidas e legenda lateral com totais.
 */
public class SimplePieChart extends JPanel {

    private final String title;
    private String[] labels = new String[0];
    private BigDecimal[] values = new BigDecimal[0];
    private Color[] colors = new Color[0];
    private boolean donut = false;
    private int hoveredSliceIndex = -1;

    private static final Color[] DEFAULT_PALETTE = {
            new Color(0, 174, 239),   // Cyan / Sky Blue (como na fatia de 40%)
            new Color(255, 165, 0),   // Laranja / Amber (como na fatia de 10%)
            new Color(43, 124, 211),  // Azul Médio (como na fatia de 30%)
            new Color(124, 179, 5),   // Verde Lima (como na fatia de 7%)
            new Color(100, 116, 139), // Cinzento Ardósia (como na fatia de 13%)
            new Color(139, 92, 246),  // Violeta
            new Color(236, 72, 153)   // Rosa
    };

    public SimplePieChart(String title) {
        this(title, false);
    }

    public SimplePieChart(String title, boolean donut) {
        this.title = title;
        this.donut = donut;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
        setPreferredSize(new Dimension(340, 220));
        setMinimumSize(new Dimension(280, 180));

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int index = getSliceIndexAt(e.getPoint());
                if (index != hoveredSliceIndex) {
                    hoveredSliceIndex = index;
                    if (hoveredSliceIndex >= 0 && hoveredSliceIndex < values.length && values[hoveredSliceIndex] != null) {
                        BigDecimal total = getTotal();
                        BigDecimal val = values[hoveredSliceIndex];
                        String lbl = hoveredSliceIndex < labels.length && labels[hoveredSliceIndex] != null
                                ? labels[hoveredSliceIndex] : "Item " + (hoveredSliceIndex + 1);
                        double pct = total.compareTo(BigDecimal.ZERO) > 0
                                ? val.divide(total, 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
                        setToolTipText(lbl + ": " + formatFull(val) + " (" + String.format("%.1f%%", pct) + ")");
                    } else {
                        setToolTipText(null);
                    }
                    repaint();
                }
            }
        });
    }

    public void setDonut(boolean donut) {
        this.donut = donut;
        repaint();
    }

    public boolean isDonut() {
        return donut;
    }

    public void setData(String[] labels, BigDecimal[] values, Color[] colors) {
        this.labels = labels != null ? labels : new String[0];
        this.values = values != null ? values : new BigDecimal[0];
        this.colors = colors != null ? colors : new Color[0];
        this.hoveredSliceIndex = -1;
        repaint();
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(val);
            }
        }
        return total;
    }

    public int getSliceIndexAt(java.awt.Point point) {
        if (point == null || values.length == 0) return -1;
        int width = getWidth();
        int height = getHeight();
        int topMargin = 40;
        int availableHeight = height - topMargin - 16;
        int pieSize = Math.max(80, Math.min(availableHeight, (width / 2) - 10));
        int pieX = 20;
        int pieY = topMargin + (availableHeight - pieSize) / 2;

        if (donut) {
            int holeSize = (int) (pieSize * 0.54);
            int holeX = pieX + (pieSize - holeSize) / 2;
            int holeY = pieY + (pieSize - holeSize) / 2;
            Ellipse2D.Double hole = new Ellipse2D.Double(holeX, holeY, holeSize, holeSize);
            if (hole.contains(point.getX(), point.getY())) {
                return -1;
            }
        }

        BigDecimal total = getTotal();
        if (total.compareTo(BigDecimal.ZERO) <= 0) return -1;

        double currentAngle = 90.0;
        for (int i = 0; i < values.length; i++) {
            BigDecimal val = values[i] == null ? BigDecimal.ZERO : values[i];
            if (val.compareTo(BigDecimal.ZERO) <= 0) continue;
            double sliceRatio = val.divide(total, 6, RoundingMode.HALF_UP).doubleValue();
            double arcAngle = sliceRatio * 360.0;
            Arc2D.Double sliceArc = new Arc2D.Double(pieX, pieY, pieSize, pieSize, currentAngle, -arcAngle, Arc2D.PIE);
            if (sliceArc.contains(point.getX(), point.getY())) {
                return i;
            }
            currentAngle -= arcAngle;
        }
        return -1;
    }

    public static String formatFull(BigDecimal value) {
        if (value == null) return "0,00 MT";
        return String.format("%,.2f MT", value);
    }

    public String[] getLabels() {
        return labels.clone();
    }

    public BigDecimal[] getValues() {
        return values.clone();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Título do gráfico
        g.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        g.setColor(UIHelper.TEXT_LIGHT);
        g.drawString(title, 16, 26);

        // 2. Cálculo do total
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(val);
            }
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0 || values.length == 0) {
            g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            g.setColor(UIHelper.TEXT_MUTED);
            g.drawString("Sem dados disponíveis", 20, 70);
            g.dispose();
            return;
        }

        // 3. Layout: Disco à esquerda / Legenda à direita
        int topMargin = 40;
        int availableHeight = height - topMargin - 16;
        int pieSize = Math.max(80, Math.min(availableHeight, (width / 2) - 10));
        int pieX = 20;
        int pieY = topMargin + (availableHeight - pieSize) / 2;
        double centerX = pieX + (pieSize / 2.0);
        double centerY = pieY + (pieSize / 2.0);

        // Linha divisória entre fatias
        Color dividerColor = UIHelper.isLight() ? Color.WHITE : UIHelper.BG_CARD;
        BasicStroke sliceStroke = new BasicStroke(1.5f);

        // 3.1 Desenho das fatias (arcos preenchidos e contornos)
        double currentAngle = 90.0;
        for (int i = 0; i < values.length; i++) {
            BigDecimal val = values[i] == null ? BigDecimal.ZERO : values[i];
            if (val.compareTo(BigDecimal.ZERO) <= 0) continue;

            double sliceRatio = val.divide(total, 6, RoundingMode.HALF_UP).doubleValue();
            double arcAngle = sliceRatio * 360.0;

            Color sliceColor = (i < colors.length && colors[i] != null)
                    ? colors[i]
                    : DEFAULT_PALETTE[i % DEFAULT_PALETTE.length];

            // Preenchimento da fatia
            Arc2D.Double sliceArc = new Arc2D.Double(pieX, pieY, pieSize, pieSize, currentAngle, -arcAngle, Arc2D.PIE);
            g.setColor(sliceColor);
            g.fill(sliceArc);

            // Borda divisória da fatia (com destaque dinâmico quando sob o cursor)
            if (i == hoveredSliceIndex) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.5f));
            } else {
                g.setColor(dividerColor);
                g.setStroke(sliceStroke);
            }
            g.draw(sliceArc);

            currentAngle -= arcAngle;
        }

        // 3.2 Se for estilo Donut, desenha o orifício central
        if (donut) {
            int holeSize = (int) (pieSize * 0.54);
            int holeX = pieX + (pieSize - holeSize) / 2;
            int holeY = pieY + (pieSize - holeSize) / 2;

            Color holeBg = UIHelper.isLight() ? new Color(255, 255, 255) : UIHelper.BG_CARD;
            g.setColor(holeBg);
            g.fill(new Ellipse2D.Double(holeX, holeY, holeSize, holeSize));

            // Total no centro do donut
            g.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            g.setColor(UIHelper.TEXT_LIGHT);
            String compactTotal = formatCompact(total);
            FontMetrics fm = g.getFontMetrics();
            int strW = fm.stringWidth(compactTotal);
            g.drawString(compactTotal, holeX + (holeSize - strW) / 2, holeY + (holeSize / 2) + 4);
        }

        // 3.3 Percentagens impressas diretamente dentro de cada fatia
        currentAngle = 90.0;
        for (int i = 0; i < values.length; i++) {
            BigDecimal val = values[i] == null ? BigDecimal.ZERO : values[i];
            if (val.compareTo(BigDecimal.ZERO) <= 0) continue;

            double sliceRatio = val.divide(total, 6, RoundingMode.HALF_UP).doubleValue();
            double arcAngle = sliceRatio * 360.0;
            double pct = sliceRatio * 100.0;

            // Só desenha o texto dentro da fatia se houver espaço angular suficiente (> 4%)
            if (pct >= 4.0) {
                double midAngleDeg = currentAngle - (arcAngle / 2.0);
                double midAngleRad = Math.toRadians(midAngleDeg);

                // Distância do centro para o texto (~62% do raio em pizza sólida, ~78% em donut)
                double textRadius = (pieSize / 2.0) * (donut ? 0.78 : 0.62);
                double textCenterX = centerX + textRadius * Math.cos(midAngleRad);
                double textCenterY = centerY - textRadius * Math.sin(midAngleRad);

                String pctText = (pct >= 9.95 || pct == Math.floor(pct))
                        ? String.format("%.0f%%", pct)
                        : String.format("%.1f%%", pct);

                Color sliceColor = (i < colors.length && colors[i] != null)
                        ? colors[i]
                        : DEFAULT_PALETTE[i % DEFAULT_PALETTE.length];

                // Contraste automático: texto escuro para cores claras, texto branco para escuras
                Color textContrastColor = getContrastingTextColor(sliceColor);

                g.setFont(new Font(UIHelper.FONT, Font.BOLD, (pct >= 15.0 && !donut) ? 12 : 10));
                g.setColor(textContrastColor);
                FontMetrics fm = g.getFontMetrics();
                int textW = fm.stringWidth(pctText);
                int textH = fm.getAscent() - fm.getDescent();

                g.drawString(pctText, (float) (textCenterX - (textW / 2.0)), (float) (textCenterY + (textH / 2.0)));
            }

            currentAngle -= arcAngle;
        }

        // 4. Legenda à direita
        int legendX = pieX + pieSize + 22;
        int legendY = topMargin + 12;
        int maxLegendWidth = width - legendX - 10;

        if (maxLegendWidth > 60) {
            for (int i = 0; i < values.length; i++) {
                if (legendY + 18 > height) break;

                BigDecimal val = values[i] == null ? BigDecimal.ZERO : values[i];
                double sliceRatio = total.compareTo(BigDecimal.ZERO) > 0 && val.compareTo(BigDecimal.ZERO) > 0
                        ? val.divide(total, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                        : 0.0;

                Color sliceColor = (i < colors.length && colors[i] != null)
                        ? colors[i]
                        : DEFAULT_PALETTE[i % DEFAULT_PALETTE.length];

                // Ponto / Quadrado de cor
                g.setColor(sliceColor);
                g.fillRoundRect(legendX, legendY - 8, 10, 10, 3, 3);

                // Texto do rótulo e percentagem
                String labelText = (i < labels.length && labels[i] != null) ? labels[i] : "Item " + (i + 1);
                String pctText = String.format("%.1f%%", sliceRatio);

                g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
                g.setColor(UIHelper.TEXT_LIGHT);
                g.drawString(labelText, legendX + 16, legendY);

                g.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
                g.setColor(UIHelper.TEXT_MUTED);
                g.drawString(pctText, legendX + 16, legendY + 12);

                legendY += 28;
            }
        }

        g.dispose();
    }

    /**
     * Calcula se o fundo é claro ou escuro usando luminosidade ponderada (W3C),
     * devolvendo texto escuro para fundos claros e branco para fundos escuros.
     */
    private static Color getContrastingTextColor(Color bg) {
        if (bg == null) return Color.WHITE;
        double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue());
        return luminance > 150 ? new Color(20, 24, 33) : Color.WHITE;
    }

    private static String formatCompact(BigDecimal value) {
        if (value == null) return "0";
        double d = value.doubleValue();
        if (Math.abs(d) >= 1_000_000) {
            return String.format("%.1fM", d / 1_000_000);
        }
        if (Math.abs(d) >= 1_000) {
            return String.format("%.1fk", d / 1_000);
        }
        return String.format("%,.0f", d);
    }
}
