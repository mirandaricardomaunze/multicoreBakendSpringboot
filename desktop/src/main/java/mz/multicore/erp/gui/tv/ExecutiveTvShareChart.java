package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Participação de cada unidade de negócio nas vendas: anel à esquerda, legenda com nome,
 * percentagem e valor à direita (até {@value #MAX_SLICES} unidades; o resto agrupa em «Outras»).
 */
public final class ExecutiveTvShareChart extends JComponent {

    private static final int MAX_SLICES = 5;

    record Slice(String name, BigDecimal value, Color color) {}

    private final List<Slice> slices;
    private final BigDecimal total;
    private final boolean lightMode;

    private ExecutiveTvShareChart(List<Slice> slices, boolean lightMode) {
        this.slices = slices;
        this.total = slices.stream().map(Slice::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.lightMode = lightMode;
        setPreferredSize(new Dimension(320, 150));
    }

    public static JPanel card(List<BusinessUnitDTO> units, List<InvoiceDTO> invoices, boolean lightMode) {
        JPanel card = ExecutiveTvComponents.card(lightMode, new BorderLayout(), 16);
        card.add(ExecutiveTvComponents.sectionHeader("Participação nas vendas", null, lightMode), BorderLayout.NORTH);
        List<Slice> slices = slices(units, invoices);
        card.add(slices.isEmpty()
                ? ExecutiveTvComponents.emptyState("fas-chart-pie", "Sem vendas por unidade",
                "As faturas do período não têm unidade de negócio.", lightMode)
                : new ExecutiveTvShareChart(slices, lightMode), BorderLayout.CENTER);
        return card;
    }

    static List<Slice> slices(List<BusinessUnitDTO> units, List<InvoiceDTO> invoices) {
        List<Slice> all = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            BigDecimal value = ExecutiveTvCalculationHelper.calculateTotalBilled(invoices, units.get(i).id());
            if (value.signum() > 0) all.add(new Slice(units.get(i).name(), value, ExecutiveTvTheme.seriesColor(i)));
        }
        all.sort((a, b) -> b.value().compareTo(a.value()));
        if (all.size() <= MAX_SLICES) return all;
        List<Slice> top = new ArrayList<>(all.subList(0, MAX_SLICES - 1));
        BigDecimal rest = all.subList(MAX_SLICES - 1, all.size()).stream().map(Slice::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        top.add(new Slice("Outras", rest, ExecutiveTvTheme.getTextFaintColor(false)));
        return top;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = ExecutiveTvComponents.antialiased(g);
        int size = Math.min(getHeight() - 4, 150);
        int x = 2, y = (getHeight() - size) / 2;
        int stroke = Math.max(14, size / 7);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        double start = 90;
        for (Slice slice : slices) {
            double extent = -360.0 * share(slice);
            g2.setColor(slice.color());
            int inset = stroke / 2;
            g2.draw(new Arc2D.Double(x + inset, y + inset, size - stroke, size - stroke, start, extent, Arc2D.OPEN));
            start += extent;
        }
        drawCenter(g2, x, y, size);
        drawLegend(g2, x + size + 24, Math.max(0, y + (size - Math.min(slices.size(), 5) * 22) / 2));
        g2.dispose();
    }

    /** Número de unidades no centro; a legenda «unidades» só cabe em anéis grandes. */
    private void drawCenter(Graphics2D g2, int x, int y, int size) {
        String count = String.valueOf(slices.size());
        boolean roomy = size >= 120;
        g2.setFont(ExecutiveTvComponents.font(Font.BOLD, roomy ? 22 : 16));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(ExecutiveTvTheme.getTextLightColor(lightMode));
        g2.drawString(count, x + (size - fm.stringWidth(count)) / 2, y + (size + fm.getAscent()) / 2 - (roomy ? 8 : 2));
        if (!roomy) return;
        String caption = slices.size() == 1 ? "unidade" : "unidades";
        g2.setFont(ExecutiveTvComponents.font(Font.PLAIN, 11));
        fm = g2.getFontMetrics();
        g2.setColor(ExecutiveTvTheme.getTextMutedColor(lightMode));
        g2.drawString(caption, x + (size - fm.stringWidth(caption)) / 2, y + size / 2 + 18);
    }

    /** Legenda com as linhas que cabem; as restantes resumem-se em «+N outras». */
    private void drawLegend(Graphics2D g2, int left, int top) {
        int width = getWidth() - left - 4;
        int rowH = 22;
        int fit = Math.max(1, (getHeight() - top) / rowH);
        int shown = slices.size() <= fit ? slices.size() : fit - 1;
        if (shown < slices.size()) {
            g2.setFont(ExecutiveTvComponents.font(Font.PLAIN, 12));
            g2.setColor(ExecutiveTvTheme.getTextMutedColor(lightMode));
            g2.drawString("+" + (slices.size() - shown) + " outras", left + 18, top + shown * rowH + 12);
        }
        for (int i = 0; i < shown; i++) {
            Slice slice = slices.get(i);
            int baseline = top + i * rowH + 12;
            g2.setColor(slice.color());
            g2.fillRoundRect(left, baseline - 9, 10, 10, 4, 4);
            String pct = String.format("%.0f%%", share(slice) * 100);
            g2.setFont(ExecutiveTvComponents.font(Font.BOLD, 13));
            FontMetrics bold = g2.getFontMetrics();
            g2.setColor(ExecutiveTvTheme.getTextLightColor(lightMode));
            g2.drawString(pct, left + width - bold.stringWidth(pct), baseline);
            g2.setFont(ExecutiveTvComponents.font(Font.PLAIN, 13));
            g2.drawString(fit(slice.name(), g2.getFontMetrics(), width - 70), left + 18, baseline);
        }
    }

    private double share(Slice slice) {
        return total.signum() == 0 ? 0 : slice.value().divide(total, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private static String fit(String text, FontMetrics fm, int max) {
        if (fm.stringWidth(text) <= max) return text;
        String cut = text;
        while (cut.length() > 1 && fm.stringWidth(cut + "...") > max) cut = cut.substring(0, cut.length() - 1);
        return cut + "...";
    }
}
