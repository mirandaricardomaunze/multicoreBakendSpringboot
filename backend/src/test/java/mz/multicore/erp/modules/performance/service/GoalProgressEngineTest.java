package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoalProgressEngineTest {

    private InvoiceRepository invoiceRepository;
    private EmployeeRepository employeeRepository;
    private BonusCalculatorEngine bonusCalculatorEngine;
    private GoalProgressEngine progressEngine;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        bonusCalculatorEngine = new BonusCalculatorEngine();
        progressEngine = new GoalProgressEngine(invoiceRepository, employeeRepository, bonusCalculatorEngine);
    }

    private SalesGoal createMonthlyGoal(BigDecimal targetRevenue) {
        Company company = new Company();
        company.setId(1L);

        SalesGoal goal = new SalesGoal();
        goal.setId(10L);
        goal.setCompany(company);
        goal.setName("Meta Mensal Outubro");
        goal.setPeriod(GoalPeriod.MONTHLY);
        goal.setPeriodStart(LocalDate.of(2026, 10, 1));
        goal.setPeriodEnd(LocalDate.of(2026, 10, 31));
        goal.setScope(GoalScope.COMPANY);
        goal.setTargetRevenue(targetRevenue);
        goal.setBonusType(BonusType.FIXED);
        goal.setBonusValue(new BigDecimal("1000.00"));
        goal.setStatus(GoalStatus.ACTIVE);
        return goal;
    }

    private Invoice createRealisedInvoice(BigDecimal amount, BigDecimal unitCost, BigDecimal quantity) {
        Invoice inv = new Invoice();
        inv.setStatus(InvoiceStatus.PAID); // isRealisedSale() == true
        inv.setTotalAmount(amount);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(inv);
        line.setLineTotal(amount);
        line.setQuantity(quantity);
        line.setUnitCost(unitCost);
        inv.setLines(List.of(line));

        return inv;
    }

    @Test
    @DisplayName("Calcula progresso com 100% ou mais: AlertLevel deve ser NONE e isOnTrack deve ser true")
    void progressAchievedOrOnTrack() {
        SalesGoal goal = createMonthlyGoal(new BigDecimal("100000.00"));

        Invoice inv = createRealisedInvoice(new BigDecimal("120000.00"), new BigDecimal("50.00"), new BigDecimal("1000.00"));
        when(invoiceRepository.findByCompanyIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(inv));

        LocalDate asOfDate = LocalDate.of(2026, 10, 15);
        SalesGoalProgressDTO progress = progressEngine.calculateProgress(goal, asOfDate);

        assertThat(progress.currentRevenue()).isEqualByComparingTo("120000.00");
        assertThat(progress.progressPct()).isEqualByComparingTo("120.00");
        assertThat(progress.isOnTrack()).isTrue();
        assertThat(progress.alertLevel()).isEqualTo(AlertLevel.NONE);
    }

    @Test
    @DisplayName("Progresso em atraso crítico (menos de 70% do ritmo esperado) gera AlertLevel.CRITICAL")
    void progressCriticalAlert() {
        SalesGoal goal = createMonthlyGoal(new BigDecimal("100000.00"));

        // No dia 20 de 31 (64.5% do tempo), vendeu apenas 10.000 (10% da meta)
        Invoice inv = createRealisedInvoice(new BigDecimal("10000.00"), new BigDecimal("5.00"), new BigDecimal("1000.00"));
        when(invoiceRepository.findByCompanyIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(inv));

        LocalDate asOfDate = LocalDate.of(2026, 10, 20);
        SalesGoalProgressDTO progress = progressEngine.calculateProgress(goal, asOfDate);

        assertThat(progress.progressPct()).isEqualByComparingTo("10.00");
        assertThat(progress.isOnTrack()).isFalse();
        assertThat(progress.alertLevel()).isEqualTo(AlertLevel.CRITICAL);
    }

    @Test
    @DisplayName("CDC-BIZ-10: Custo histórico lineCost é usado no cálculo de margem")
    void historicalCostUsedInMargin() {
        SalesGoal goal = createMonthlyGoal(new BigDecimal("100000.00"));

        // Venda de 50.000, custo unitário 30 * qtd 1.000 = custo 30.000 -> Margem = 20.000
        Invoice inv = createRealisedInvoice(new BigDecimal("50000.00"), new BigDecimal("30.00"), new BigDecimal("1000.00"));
        when(invoiceRepository.findByCompanyIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(inv));

        SalesGoalProgressDTO progress = progressEngine.calculateProgress(goal, LocalDate.of(2026, 10, 15));
        assertThat(progress.currentMargin()).isEqualByComparingTo("20000.00");
    }
}
