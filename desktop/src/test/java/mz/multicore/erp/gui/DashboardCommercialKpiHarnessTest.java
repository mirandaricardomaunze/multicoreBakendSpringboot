package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DashboardTrendCalculator;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Dashboard Comercial & Executivo — Variação de Vendas, Ticket Médio e Margem Bruta")
class DashboardCommercialKpiHarnessTest {

    private static final LocalDate ANCHOR_DATE = LocalDate.of(2026, 9, 25);

    @Test
    @DisplayName("DKPI-01: Cálculo determinístico de períodos comparativos para cada filtro temporal")
    void dkpi01_comparativePeriodsResolution() {
        // 1. HOJE -> atual [2026-09-25, 2026-09-25], anterior [2026-09-24, 2026-09-24]
        var hoje = DashboardTrendCalculator.resolvePeriods(DashboardPanel.PeriodFilter.HOJE, ANCHOR_DATE);
        assertThat(hoje.current().from()).isEqualTo(ANCHOR_DATE);
        assertThat(hoje.current().to()).isEqualTo(ANCHOR_DATE);
        assertThat(hoje.previous().from()).isEqualTo(ANCHOR_DATE.minusDays(1));
        assertThat(hoje.previous().to()).isEqualTo(ANCHOR_DATE.minusDays(1));
        assertThat(hoje.comparisonLabel()).isEqualTo("vs ontem");

        // 2. ESTA_SEMANA -> atual [2026-09-18, 2026-09-25], anterior [2026-09-11, 2026-09-18]
        var semana = DashboardTrendCalculator.resolvePeriods(DashboardPanel.PeriodFilter.ESTA_SEMANA, ANCHOR_DATE);
        assertThat(semana.current().from()).isEqualTo(ANCHOR_DATE.minusDays(7));
        assertThat(semana.current().to()).isEqualTo(ANCHOR_DATE);
        assertThat(semana.previous().from()).isEqualTo(ANCHOR_DATE.minusDays(14));
        assertThat(semana.previous().to()).isEqualTo(ANCHOR_DATE.minusDays(7));
        assertThat(semana.comparisonLabel()).isEqualTo("vs semana anterior");

        // 3. ESTE_MES -> atual [2026-09-01, 2026-09-25], anterior [2026-08-01, 2026-08-31]
        var mes = DashboardTrendCalculator.resolvePeriods(DashboardPanel.PeriodFilter.ESTE_MES, ANCHOR_DATE);
        assertThat(mes.current().from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(mes.current().to()).isEqualTo(ANCHOR_DATE);
        assertThat(mes.previous().from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(mes.previous().to()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(mes.comparisonLabel()).isEqualTo("vs mês anterior");

        // 4. ESTE_ANO -> atual [2026-01-01, 2026-09-25], anterior [2025-01-01, 2025-12-31]
        var ano = DashboardTrendCalculator.resolvePeriods(DashboardPanel.PeriodFilter.ESTE_ANO, ANCHOR_DATE);
        assertThat(ano.current().from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(ano.current().to()).isEqualTo(ANCHOR_DATE);
        assertThat(ano.previous().from()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(ano.previous().to()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(ano.comparisonLabel()).isEqualTo("vs ano anterior");

        // 5. TODOS -> histórico total sem período comparativo
        var todos = DashboardTrendCalculator.resolvePeriods(DashboardPanel.PeriodFilter.TODOS, ANCHOR_DATE);
        assertThat(todos.current().from()).isNull();
        assertThat(todos.current().to()).isNull();
        assertThat(todos.previous()).isNull();
        assertThat(todos.comparisonLabel()).isEqualTo("histórico");
    }

    @Test
    @DisplayName("DKPI-02: Variação percentual normal de vendas (crescimento e decrescimento)")
    void dkpi02_normalPercentageVariation() {
        // Crescimento de 1.000 MT para 1.500 MT (+50.0%)
        BigDecimal growth = DashboardTrendCalculator.calculatePercentageChange(
                new BigDecimal("1500.00"), new BigDecimal("1000.00"));
        assertThat(growth).isEqualByComparingTo("50.0");
        assertThat(DashboardTrendCalculator.formatPercentage(growth)).isEqualTo("+50.0%");

        // Decrescimento de 1.000 MT para 800 MT (-20.0%)
        BigDecimal drop = DashboardTrendCalculator.calculatePercentageChange(
                new BigDecimal("800.00"), new BigDecimal("1000.00"));
        assertThat(drop).isEqualByComparingTo("-20.0");
        assertThat(DashboardTrendCalculator.formatPercentage(drop)).isEqualTo("-20.0%");
    }

    @Test
    @DisplayName("DKPI-03: Casos de borda na variação percentual (divisão por zero, nulos e valores zerados)")
    void dkpi03_edgeCasesPercentageVariation() {
        // 1. Anterior zero e atual positivo -> +100.0%
        BigDecimal fromZero = DashboardTrendCalculator.calculatePercentageChange(
                new BigDecimal("450.00"), BigDecimal.ZERO);
        assertThat(fromZero).isEqualByComparingTo("100.0");

        // 2. Anterior zero e atual zero -> 0.0%
        BigDecimal bothZero = DashboardTrendCalculator.calculatePercentageChange(
                BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(bothZero).isEqualByComparingTo("0.0");

        // 3. Anterior positivo e atual zero -> -100.0%
        BigDecimal toZero = DashboardTrendCalculator.calculatePercentageChange(
                BigDecimal.ZERO, new BigDecimal("800.00"));
        assertThat(toZero).isEqualByComparingTo("-100.0");

        // 4. Parâmetros nulos -> retorno seguro sem NPE
        BigDecimal nullSafe = DashboardTrendCalculator.calculatePercentageChange(null, null);
        assertThat(nullSafe).isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("DKPI-04: Cálculo rigoroso do Ticket Médio Comercial")
    void dkpi04_averageTicketCalculation() {
        // Receita 15.000 MT em 10 transações -> 1.500,00 MT/venda
        BigDecimal avg = DashboardTrendCalculator.calculateAverageTicket(new BigDecimal("15000.00"), 10);
        assertThat(avg).isEqualByComparingTo("1500.00");
        assertThat(DashboardTrendCalculator.formatAverageTicket(avg)).contains("1.500,00 MT/venda");

        // 0 transações -> 0.00 MT/venda (sem divisão por zero)
        BigDecimal zeroCount = DashboardTrendCalculator.calculateAverageTicket(new BigDecimal("5000.00"), 0);
        assertThat(zeroCount).isEqualByComparingTo("0.00");

        // Receita nula ou negativa
        BigDecimal nullRevenue = DashboardTrendCalculator.calculateAverageTicket(null, 5);
        assertThat(nullRevenue).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("DKPI-05: Cálculo da Margem Bruta Comercial Estimada (%)")
    void dkpi05_grossMarginCalculation() {
        // Receita 20.000 MT com Lucro Bruto 5.000 MT -> 25.0%
        BigDecimal margin = DashboardTrendCalculator.calculateGrossMarginPercentage(
                new BigDecimal("5000.00"), new BigDecimal("20000.00"));
        assertThat(margin).isEqualByComparingTo("25.0");

        // Receita zero -> 0.0%
        BigDecimal zeroMargin = DashboardTrendCalculator.calculateGrossMarginPercentage(
                new BigDecimal("1000.00"), BigDecimal.ZERO);
        assertThat(zeroMargin).isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("DKPI-06: Componente visual TrendBadge com atualização dinâmica e sem caracteres quebrados")
    void dkpi06_trendBadgeDynamicUpdates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            KpiCard.TrendBadge badge = new KpiCard.TrendBadge();
            assertTrue(badge.isZero());

            // 1. Atualizar para crescimento positivo
            badge.updateTrend(new BigDecimal("18.4"), "vs mês anterior");
            assertTrue(badge.isPositive());
            assertFalse(badge.isNegative());
            assertThat(badge.getText()).contains("+18.4%");
            assertThat(badge.getLabel().getForeground()).isEqualTo(UIHelper.APPROVED_GREEN);
            assertThat(badge.getToolTipText()).contains("vs mês anterior");

            // 2. Atualizar para decrescimento
            badge.updateTrend(new BigDecimal("-6.5"), "vs semana anterior");
            assertTrue(badge.isNegative());
            assertFalse(badge.isPositive());
            assertThat(badge.getText()).contains("-6.5%");
            assertThat(badge.getLabel().getForeground()).isEqualTo(UIHelper.REJECTED_RED);
            assertThat(badge.getToolTipText()).contains("vs semana anterior");

            // 3. Atualizar para neutro / nulo
            badge.updateTrend(null, "histórico");
            assertTrue(badge.isZero());
            assertThat(badge.getText()).contains("0.0%");
            assertThat(badge.getLabel().getForeground()).isEqualTo(UIHelper.TEXT_MUTED);
        });
    }

    @Test
    @DisplayName("DKPI-07: Resiliência do DateRange e contenção de datas")
    void dkpi07_dateRangeFiltering() {
        var range = new DashboardTrendCalculator.DateRange(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertTrue(range.contains(LocalDate.of(2026, 9, 15)));
        assertTrue(range.contains(LocalDate.of(2026, 9, 1)));
        assertTrue(range.contains(LocalDate.of(2026, 9, 30)));
        assertFalse(range.contains(LocalDate.of(2026, 8, 31)));
        assertFalse(range.contains(LocalDate.of(2026, 10, 1)));
        assertFalse(range.contains(null));

        // Limites abertos (TODOS)
        var openRange = new DashboardTrendCalculator.DateRange(null, null);
        assertTrue(openRange.contains(LocalDate.of(2020, 1, 1)));
        assertTrue(openRange.contains(LocalDate.of(2030, 12, 31)));
    }

    @Test
    @DisplayName("DKPI-08: Decomposição de Linhas e conformidade arquitetural")
    void dkpi08_lineCountConstraints() throws IOException {
        Path dashboardPath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "DashboardPanel.java");
        Path trendCalcPath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "components", "DashboardTrendCalculator.java");

        assertThat(Files.lines(dashboardPath).count())
                .as("DashboardPanel.java deve permanecer <= 1000 linhas")
                .isLessThanOrEqualTo(1000);

        assertThat(Files.lines(trendCalcPath).count())
                .as("DashboardTrendCalculator.java deve permanecer <= 1000 linhas")
                .isLessThanOrEqualTo(1000);
    }
}
