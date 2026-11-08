package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.gui.components.TrendBadge;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies dashboard readability with realistic data. See DA-08 through DA-13 in the dashboard specification. */
class DashboardReadabilityHarnessTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Test
    @DisplayName("DA-08: circular com uma só fatia mostra a quota, não um disco inteiro")
    void da08_singleSliceShowsShare() {
        SimplePieChart pie = new SimplePieChart("Canais", false);
        pie.setData(new String[]{"Balcão POS", "Faturas"}, new BigDecimal[]{new BigDecimal("1460"), ZERO},
                new Color[]{UIHelper.APPROVED_GREEN, UIHelper.ACCENT_BLUE});
        pie.setSize(400, 220);
        BufferedImage image = paint(pie);
        // Without a disc, the former circle centre must not contain a slice colour.
        assertThat(new Color(image.getRGB(20 + 80, 40 + 82), true)).isNotEqualTo(UIHelper.APPROVED_GREEN);
        assertThat(pie.getSliceIndexAt(new Point(380, 200))).isZero();

        pie.setData(new String[]{"A", "B"}, new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE}, null);
        assertThat(pie.getSliceIndexAt(new Point(380, 200))).isEqualTo(-1);
    }

    @Test
    @DisplayName("DA-09: barra com valor zero não se pinta")
    void da09_zeroBarHasNoFill() {
        SimpleBarChart chart = new SimpleBarChart("Vendas, Compras e IVA");
        chart.setData(new String[]{"Vendas", "Compras"}, new BigDecimal[]{new BigDecimal("1460"), ZERO},
                new Color[]{UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN});
        chart.setSize(400, 220);
        BufferedImage image = paint(chart);
        Rectangle zero = chart.getBarBounds()[1];
        int baseline = zero.y + zero.height;
        assertThat(new Color(image.getRGB(zero.x + zero.width / 2, baseline - 2), true)).isNotEqualTo(UIHelper.APPROVED_GREEN);
        assertThat(chart.getBarIndexAt(new Point(zero.x + zero.width / 2, baseline - 4))).isEqualTo(1);
    }

    @Test
    @DisplayName("DA-10: em Todo o Período não há variação a mostrar")
    void da10_trendHiddenWithoutComparison() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardOverviewTab tab = new DashboardOverviewTab(null);
            JPanel root = tab.buildTab();
            tab.applyData(data("histórico", ZERO));
            assertThat(find(root, TrendBadge.class)).isNotEmpty().allMatch(badge -> !badge.isVisible());
            tab.applyData(data("vs ontem", ZERO));
            assertThat(find(root, TrendBadge.class)).allMatch(Component::isVisible);
        });
    }

    @Test
    @DisplayName("DA-11: o cartão do IVA mostra só o montante; o sentido vai para a legenda")
    void da11_vatValueIsOnlyTheAmount() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardOverviewTab tab = new DashboardOverviewTab(null);
            JPanel root = tab.buildTab();
            tab.applyData(data("vs ontem", new BigDecimal("-50")));
            List<String> texts = find(root, JLabel.class).stream().map(JLabel::getText).toList();
            assertThat(texts).contains("IVA DO PERÍODO", "50,00 MT", "Saldo actual de caixa e bancos")
                    .anyMatch(t -> t != null && t.startsWith("A recuperar · Liquidado"))
                    .noneMatch(t -> t != null && t.startsWith("IVA a "));
        });
    }

    @Test
    @DisplayName("DA-12: períodos num só controlo, com cor só no activo")
    void da12_segmentedPeriods() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardPeriodBar bar = new DashboardPeriodBar(() -> { }, period -> { });
            bar.select(DashboardPanel.PeriodFilter.ESTE_MES);
            List<ModernButton> periods = find(bar.component(), ModernButton.class).stream()
                    .filter(b -> !DashboardPeriodBar.TV_LABEL.equals(b.getText())).toList();
            assertThat(periods).hasSize(DashboardPanel.PeriodFilter.values().length);
            assertThat(periods.stream().map(Component::getParent).distinct()).hasSize(1);
            assertThat(periods).filteredOn(b -> b.getBackground().equals(UIHelper.ACCENT_BLUE))
                    .extracting(ModernButton::getText).containsExactly("Este Mês");
        });
    }

    @Test
    @DisplayName("DA-13: com ou sem pílula de variação, o valor começa no mesmo sítio")
    void da13_valueNotIndentedByTrendBadge() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JLabel value = KpiCard.valueLabel("1.460,00 MT", 19);
            KpiCard.create("VENDAS POS", "fas-cash-register", UIHelper.KPI_SUCCESS_SOFT, value,
                    new KpiCard.TrendBadge(BigDecimal.TEN), new JLabel("1 venda"), UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN);
            value.getParent().setSize(300, 40);
            value.getParent().doLayout();
            assertThat(value.getX()).isZero();
        });
    }

    private static BufferedImage paint(JPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image;
    }

    private static <T> List<T> find(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container nested) found.addAll(find(nested, type));
        }
        return found;
    }

    private static DashboardDataCoordinator.DashboardData data(String compLabel, BigDecimal netVat) {
        return new DashboardDataCoordinator.DashboardData(ZERO, new BigDecimal("100"), ZERO, ZERO, 0L, ZERO, ZERO,
                BigDecimal.TEN, ZERO, compLabel, ZERO, ZERO, 0, 0L, ZERO, ZERO, netVat, ZERO,
                0L, 0, 0L, 0L, List.of(), List.of(), null);
    }
}
