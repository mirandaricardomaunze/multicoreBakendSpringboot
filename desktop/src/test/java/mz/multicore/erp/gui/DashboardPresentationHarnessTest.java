package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.DashboardTrendCalculator;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.StatusBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/DASHBOARD_APRESENTACAO_SPEC.md. */
class DashboardPresentationHarnessTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Test
    @DisplayName("DA-01: Sem vendas hoje, o cartão mostra o valor de ontem")
    void da01_idleCardShowsPreviousPeriod() {
        assertThat(DashboardTrendCalculator.activityHint(ZERO, new BigDecimal("1500.50"), "vs ontem", "activo"))
                .isEqualTo("Ontem: 1.500,50 MT");
        assertThat(DashboardTrendCalculator.activityHint(ZERO, null, "vs semana anterior", "activo"))
                .isEqualTo("Semana anterior: 0,00 MT");
    }

    @Test
    @DisplayName("DA-02: Com movimento, ou sem período anterior, mantém-se o texto normal")
    void da02_activeCardKeepsItsText() {
        assertThat(DashboardTrendCalculator.activityHint(BigDecimal.TEN, ZERO, "vs ontem", "activo")).isEqualTo("activo");
        assertThat(DashboardTrendCalculator.activityHint(ZERO, null, "histórico", "activo")).isEqualTo("activo");
    }

    @Test
    @DisplayName("DA-03: Gráfico só tem dados com um valor positivo")
    void da03_chartEmptyState() {
        assertThat(SimpleBarChart.hasData(new BigDecimal[]{ZERO, null})).isFalse();
        assertThat(SimpleBarChart.hasData(null)).isFalse();
        assertThat(SimpleBarChart.hasData(new BigDecimal[]{ZERO, BigDecimal.ONE})).isTrue();
    }

    @Test
    @DisplayName("DA-04: Pendências vêm primeiro e os gráficos financeiros vazios desaparecem")
    void da04_prioritiesFirstAndEmptyChartsHidden() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardOverviewTab tab = new DashboardOverviewTab(null);
            JPanel root = tab.buildTab();
            List<SimpleBarChart> charts = new ArrayList<>();
            collect(root, charts);

            assertThat(charts.get(0).getEmptyMessage()).isEqualTo("Sem pendências");

            tab.applyData(data(ZERO, ZERO));
            assertThat(tab.financialRow.isVisible()).isFalse();

            tab.applyData(data(new BigDecimal("250.00"), ZERO));
            assertThat(tab.financialRow.isVisible()).isTrue();
        });
    }

    @Test
    @DisplayName("DA-05: Cartão POS conta as vendas do período, com percentagem em formato local")
    void da05_posCountFollowsPeriod() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardOverviewTab tab = new DashboardOverviewTab(null);
            JPanel root = tab.buildTab();

            tab.applyData(data(ZERO, new BigDecimal("900.00")));

            assertThat(texts(root)).contains("12 vendas · média: 75,00 MT");
        });
        assertThat(DashboardTrendCalculator.formatPercentage(new BigDecimal("8.5"))).isEqualTo("+8,5%");
        assertThat(DashboardTrendCalculator.formatPercentage(ZERO)).isEqualTo("0,0%");
    }

    @Test
    @DisplayName("DA-06: Painel TV é uma acção separada dos filtros de período")
    void da06_tvIsSeparatedFromPeriods() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardPanel panel = new DashboardPanel(
                    Mockito.mock(ComercialApiClient.class), Mockito.mock(FinanceApiClient.class),
                    Mockito.mock(ApprovalApiClient.class), Mockito.mock(CRMApiClient.class),
                    Mockito.mock(PurchaseApiClient.class), Mockito.mock(InventoryApiClient.class));
            ModernButton tv = find(panel, ModernButton.class).stream()
                    .filter(button -> button.getToolTipText() != null && button.getToolTipText().contains("Painel TV"))
                    .findFirst().orElseThrow();

            Component[] bar = tv.getParent().getComponents();
            assertThat(tv.getText()).isNullOrEmpty();
            assertThat(bar[0]).isSameAs(tv);
            assertThat(bar[1]).isInstanceOf(Box.Filler.class);
        });
    }

    @Test
    @DisplayName("DA-07: Barra de estado não repete módulo nem empresa")
    void da07_statusBarHasNoRepeatedContext() throws Exception {
        SwingUtilities.invokeAndWait(() -> assertThat(texts(new StatusBar()))
                .filteredOn(text -> text != null && !text.isBlank())
                .hasSize(2)
                .contains("Multicore ERP • v1.0.0"));
    }

    private static <T> List<T> find(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container nested) found.addAll(find(nested, type));
        }
        return found;
    }

    private static DashboardDataCoordinator.DashboardData data(BigDecimal sales, BigDecimal posSales) {
        return new DashboardDataCoordinator.DashboardData(ZERO, sales, posSales, ZERO, 12L, ZERO, ZERO,
                ZERO, ZERO, "vs ontem", ZERO, new BigDecimal("75.00"), 0, 0L, ZERO, ZERO, ZERO, ZERO,
                0L, 0, 0L, 0L, List.of(), List.of(), null);
    }

    private static void collect(Container container, List<SimpleBarChart> charts) {
        for (Component child : container.getComponents()) {
            if (child instanceof SimpleBarChart chart) charts.add(chart);
            if (child instanceof Container nested) collect(nested, charts);
        }
    }

    private static List<String> texts(Container container) {
        List<String> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label) found.add(label.getText());
            if (child instanceof Container nested) found.addAll(texts(nested));
        }
        return found;
    }
}
