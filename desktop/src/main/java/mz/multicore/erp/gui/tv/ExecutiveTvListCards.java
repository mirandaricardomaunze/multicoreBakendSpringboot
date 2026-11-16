package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Cartões de lista do painel de TV: ranking de artigos e últimas faturas. */
public final class ExecutiveTvListCards {

    private static final int TOP_LIMIT = 5;
    private static final int INVOICE_LIMIT = 6;

    private ExecutiveTvListCards() {}

    public static JPanel topProducts(List<InvoiceDTO> invoices, Long unitId, boolean lightMode) {
        List<ExecutiveTvCalculationHelper.ProductMetric> metrics =
                ExecutiveTvCalculationHelper.aggregateTopProducts(invoices, unitId);
        if (metrics.isEmpty()) {
            return withBody("Top artigos", ExecutiveTvComponents.emptyState("fas-box-open", "Sem vendas",
                    "Nenhum artigo vendido no período.", lightMode), false, lightMode);
        }
        JPanel list = verticalList();
        BigDecimal max = metrics.get(0).revenue();
        for (int i = 0; i < Math.min(TOP_LIMIT, metrics.size()); i++) {
            list.add(productRow(i + 1, metrics.get(i), max, lightMode));
            list.add(Box.createVerticalStrut(10));
        }
        return withBody("Top artigos", list, true, lightMode);
    }

    public static JPanel recentInvoices(List<InvoiceDTO> invoices, Long unitId, boolean lightMode) {
        List<InvoiceDTO> rows = invoices.stream()
                .filter(inv -> ExecutiveTvCalculationHelper.belongsTo(inv, unitId)).limit(INVOICE_LIMIT).toList();
        if (rows.isEmpty()) {
            return withBody("Últimas faturas", ExecutiveTvComponents.emptyState("fas-file-invoice", "Sem faturas",
                    "Nenhuma fatura desta unidade no período.", lightMode), false, lightMode);
        }
        JPanel list = verticalList();
        for (int i = 0; i < rows.size(); i++) list.add(invoiceRow(rows.get(i), i > 0, lightMode));
        return withBody("Últimas faturas", list, true, lightMode);
    }

    /** Listas ficam no topo do cartão; estados vazios ficam centrados. */
    private static JPanel withBody(String title, JComponent body, boolean isList, boolean lightMode) {
        JPanel card = ExecutiveTvComponents.card(lightMode, new BorderLayout(), 16);
        card.add(ExecutiveTvComponents.sectionHeader(title, null, lightMode), BorderLayout.NORTH);
        JPanel holder = ExecutiveTvComponents.transparent(new BorderLayout());
        holder.add(body, isList ? BorderLayout.NORTH : BorderLayout.CENTER);
        card.add(holder, BorderLayout.CENTER);
        return card;
    }

    private static JPanel verticalList() {
        JPanel list = ExecutiveTvComponents.transparent(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        return list;
    }

    private static JPanel productRow(int rank, ExecutiveTvCalculationHelper.ProductMetric metric, BigDecimal max, boolean lightMode) {
        Color text = ExecutiveTvTheme.getTextLightColor(lightMode);
        Color accent = rank == 1 ? UIHelper.PENDING_YELLOW : ExecutiveTvTheme.seriesColor(0);
        JPanel row = ExecutiveTvComponents.transparent(new BorderLayout(10, 4));
        JPanel badgeHolder = ExecutiveTvComponents.transparent(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 2));
        badgeHolder.add(rankBadge(rank, accent, lightMode)); // sem esticar: o círculo mantém-se redondo
        row.add(badgeHolder, BorderLayout.WEST);

        JPanel line = ExecutiveTvComponents.transparent(new BorderLayout(8, 0));
        JLabel name = ExecutiveTvComponents.label(metric.name(), Font.BOLD, 13, text);
        name.setToolTipText(metric.name());
        line.add(name, BorderLayout.CENTER);
        JLabel value = ExecutiveTvComponents.label(ExecutiveTvSummaryBuilder.money(metric.revenue()), Font.BOLD, 13, text);
        value.setToolTipText(String.format("%,.0f unidades", metric.quantity()));
        line.add(value, BorderLayout.EAST);

        double share = max.signum() > 0
                ? metric.revenue().multiply(BigDecimal.valueOf(100)).divide(max, 1, RoundingMode.HALF_UP).doubleValue() : 0;
        JPanel stack = ExecutiveTvComponents.transparent(new GridLayout(2, 1, 0, 4));
        stack.add(line);
        stack.add(miniBar(share, accent, lightMode));
        row.add(stack, BorderLayout.CENTER);
        row.setAlignmentX(0f);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent rankBadge(int rank, Color accent, boolean lightMode) {
        JLabel badge = new JLabel(String.valueOf(rank), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = ExecutiveTvComponents.antialiased(g);
                g2.setColor(ExecutiveTvTheme.tint(accent, ExecutiveTvTheme.getCardBgColor(lightMode), 0.22));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(ExecutiveTvComponents.font(Font.BOLD, 12));
        badge.setForeground(accent);
        badge.setPreferredSize(new Dimension(26, 26));
        return badge;
    }

    private static JComponent miniBar(double percent, Color accent, boolean lightMode) {
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = ExecutiveTvComponents.antialiased(g);
                int y = (getHeight() - 5) / 2;
                g2.setColor(ExecutiveTvTheme.getTrackColor(lightMode));
                g2.fillRoundRect(0, y, getWidth(), 5, 5, 5);
                g2.setColor(accent);
                g2.fillRoundRect(0, y, (int) Math.round(percent / 100.0 * getWidth()), 5, 5, 5);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 6));
        return bar;
    }

    private static JPanel invoiceRow(InvoiceDTO inv, boolean separator, boolean lightMode) {
        JPanel row = ExecutiveTvComponents.transparent(new BorderLayout(12, 0));
        row.setBorder(separator
                ? new javax.swing.border.CompoundBorder(new MatteBorder(1, 0, 0, 0, ExecutiveTvTheme.getCardBorderColor(lightMode)),
                new EmptyBorder(8, 0, 8, 0))
                : new EmptyBorder(0, 0, 8, 0));
        JPanel left = ExecutiveTvComponents.transparent(new GridLayout(2, 1, 0, 1));
        left.add(ExecutiveTvComponents.label(inv.invoiceNumber(), Font.BOLD, 13, ExecutiveTvTheme.getTextLightColor(lightMode)));
        left.add(ExecutiveTvComponents.label(inv.buyerName() != null ? inv.buyerName() : "Cliente diverso", Font.PLAIN, 12,
                ExecutiveTvTheme.getTextMutedColor(lightMode)));
        row.add(left, BorderLayout.CENTER);
        row.add(ExecutiveTvComponents.label(ExecutiveTvSummaryBuilder.money(inv.totalAmount()), Font.BOLD, 14,
                ExecutiveTvTheme.getTextLightColor(lightMode)), BorderLayout.EAST);
        row.setAlignmentX(0f);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
