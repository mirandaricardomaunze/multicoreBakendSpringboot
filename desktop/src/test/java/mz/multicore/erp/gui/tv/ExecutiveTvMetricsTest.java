package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Indicadores do painel de TV: ritmo, previsão de fecho e estado sem meta. */
class ExecutiveTvMetricsTest {

    private static final LocalDate DAY_10_OF_30 = LocalDate.of(2026, 11, 10);
    private static final long UNIT = 424242L;

    private static InvoiceDTO invoice(String total) {
        BigDecimal amount = new BigDecimal(total);
        return new InvoiceDTO(1L, "FT-1", 1L, "Cliente", "1", amount, BigDecimal.ZERO, amount, BigDecimal.ZERO,
                InvoiceStatus.APPROVED, null, List.of(), LocalDateTime.now(), "admin", null, 0, null, UNIT, "Teste");
    }

    @Test
    @DisplayName("Mês: ritmo diário, previsão de fecho e venda necessária por dia")
    void monthMetrics() {
        ExecutiveTvMetrics m = ExecutiveTvMetrics.of(List.of(invoice("3000"), invoice("2000")), UNIT,
                new BigDecimal("30000"), ExecutiveTvDashboardDialog.Period.ESTE_MES, DAY_10_OF_30);

        assertEquals(2, m.invoiceCount());
        assertEquals(0, new BigDecimal("2500.00").compareTo(m.averageTicket()));
        assertEquals(0, new BigDecimal("500.00").compareTo(m.dailyRate()));
        assertEquals(0, new BigDecimal("15000.00").compareTo(m.projection()));
        assertEquals(50.0, m.projectionPercent(), 0.001);
        assertEquals(16.7, m.percent(), 0.001);
        assertEquals(33.3, m.expectedPercent(), 0.1);
        // Faltam 25.000 MT em 20 dias.
        assertEquals(0, new BigDecimal("1250.00").compareTo(m.requiredDailyRate()));
        assertEquals(GoalStatus.ALERT, m.status());
    }

    @Test
    @DisplayName("Sem meta: estado neutro, nunca «ALERTA» nem «meta superada»")
    void withoutGoalIsNeutral() {
        ExecutiveTvMetrics m = ExecutiveTvMetrics.of(List.of(invoice("1460")), UNIT, BigDecimal.ZERO,
                ExecutiveTvDashboardDialog.Period.ESTE_MES, DAY_10_OF_30);

        assertFalse(m.hasGoal());
        assertEquals(GoalStatus.NO_GOAL, m.status());
        assertEquals(0.0, m.percent());
        assertEquals(0, BigDecimal.ZERO.compareTo(m.requiredDailyRate()));
        JLabel badge = ExecutiveTvTheme.buildStatusBadge(m.status(), false);
        assertEquals("SEM META", badge.getText());
    }

    @Test
    @DisplayName("Meta atingida: nada é necessário por dia e o estado é «META SUPERADA»")
    void achievedGoal() {
        ExecutiveTvMetrics m = ExecutiveTvMetrics.of(List.of(invoice("12000")), UNIT, new BigDecimal("10000"),
                ExecutiveTvDashboardDialog.Period.ESTE_MES, DAY_10_OF_30);

        assertEquals(GoalStatus.ACHIEVED, m.status());
        assertTrue(m.difference().signum() > 0);
        assertEquals(0, BigDecimal.ZERO.compareTo(m.requiredDailyRate()));
    }

    @Test
    @DisplayName("Dias do período acompanham o filtro de faturas")
    void periodDays() {
        assertEquals(1, ExecutiveTvMetrics.periodDays(ExecutiveTvDashboardDialog.Period.HOJE, DAY_10_OF_30)[1]);
        assertEquals(7, ExecutiveTvMetrics.periodDays(ExecutiveTvDashboardDialog.Period.ESTA_SEMANA, DAY_10_OF_30)[1]);
        assertEquals(30, ExecutiveTvMetrics.periodDays(ExecutiveTvDashboardDialog.Period.ESTE_MES, DAY_10_OF_30)[1]);
        assertEquals(314, ExecutiveTvMetrics.periodDays(ExecutiveTvDashboardDialog.Period.ESTE_ANO, DAY_10_OF_30)[0]);
    }

    @Test
    @DisplayName("Limiares de estado iguais aos da especificação")
    void statusThresholds() {
        assertEquals(GoalStatus.ALERT, GoalStatus.of(true, 49.9));
        assertEquals(GoalStatus.WARNING, GoalStatus.of(true, 50.0));
        assertEquals(GoalStatus.ON_TRACK, GoalStatus.of(true, 80.0));
        assertEquals(GoalStatus.ACHIEVED, GoalStatus.of(true, 100.0));
        assertEquals(GoalStatus.NO_GOAL, GoalStatus.of(false, 250.0));
    }
}
