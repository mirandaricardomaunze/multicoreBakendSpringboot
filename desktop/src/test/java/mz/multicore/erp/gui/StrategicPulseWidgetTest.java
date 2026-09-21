package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.StrategicPulseWidget;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowAlertDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.AlertLevel;
import mz.multicore.erp.modules.performance.model.GoalScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StrategicPulseWidgetTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testWidgetInstantiationAndFallback() {
        // D360-01: Component initializes safely without throwing NPE when clients are null
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, null);
        assertNotNull(widget);
        assertNotNull(widget.getForensicScoreLabel());
        assertNotNull(widget.getLiquidityValLabel());
        assertNotNull(widget.getCreditRiskValLabel());
        assertNotNull(widget.getPerformanceValLabel());
    }

    @Test
    void testForensicCompliancePillarDisplay() {
        // D360-02: Forensic pillar displays score and anomalies
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, null);

        ForensicAuditSummaryDTO summary = new ForensicAuditSummaryDTO(
                5, 1, 2, 2,
                BigDecimal.valueOf(15000),
                "87.5%",
                List.of()
        );

        widget.applyData(summary, null, null, null);

        assertTrue(widget.getForensicScoreLabel().getText().contains("87"),
                "Deverá conter o score de conformidade");
    }

    @Test
    void testLiquidityPrevisionalPillarDisplay() {
        // D360-03: Liquidity pillar displays projected balance and warning
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, null);

        CashFlowAlertDTO alert = new CashFlowAlertDTO("HEALTHY", "Saudável", null, BigDecimal.ZERO, "OK");
        CashFlowForecastDTO forecast = new CashFlowForecastDTO(
                LocalDate.now(),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(700000),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(600000),
                List.of(),
                List.of(),
                List.of(),
                alert
        );

        widget.applyData(null, forecast, null, null);

        assertTrue(widget.getLiquidityValLabel().getText().contains("600"),
                "Deverá conter o saldo projetado formatado");
    }

    @Test
    void testCreditRiskPillarDisplay() {
        // D360-04: Credit risk pillar displays overdue amount and count
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, null);

        CreditRiskSummaryDTO credit = new CreditRiskSummaryDTO(
                LocalDate.now(),
                BigDecimal.valueOf(250000),
                BigDecimal.valueOf(120000),
                BigDecimal.valueOf(500000),
                10, 1, 2, 1, 3, 4
        );

        widget.applyData(null, null, credit, null);

        assertTrue(widget.getCreditRiskValLabel().getText().contains("120"),
                "Deverá exibir o montante de crédito vencido em mora");
    }

    @Test
    void testSalesGoalsPillarDisplay() {
        // D360-05: Sales goals pillar displays achievement percentage
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, null);

        SalesGoalProgressDTO goal = new SalesGoalProgressDTO(
                1L, "Target Mensal", GoalScope.COMPANY, "Empresa",
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(75000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(75.0),
                15, 30,
                BigDecimal.valueOf(150000),
                true,
                BigDecimal.ZERO,
                AlertLevel.NONE
        );

        widget.applyData(null, null, null, List.of(goal));

        assertTrue(widget.getPerformanceValLabel().getText().contains("75"),
                "Deverá conter a percentagem de concretização das metas");
    }

    @Test
    void testNavigationCallbacks() {
        // D360-06: 1-click buttons trigger navigation callback with appropriate module keys
        AtomicReference<String> navigated = new AtomicReference<>();
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, navigated::set);

        widget.getForensicNavBtn().doClick();
        assertEquals("auditoria_forense", navigated.get());

        widget.getLiquidityNavBtn().doClick();
        assertEquals("previsao_tesouraria", navigated.get());

        widget.getCreditRiskNavBtn().doClick();
        assertEquals("risco_credito", navigated.get());

        widget.getPerformanceNavBtn().doClick();
        assertEquals("desempenho", navigated.get());
    }
}
