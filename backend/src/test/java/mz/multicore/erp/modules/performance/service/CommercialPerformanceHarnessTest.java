package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.service.PayrollPeriodService;
import mz.multicore.erp.modules.performance.dto.*;
import mz.multicore.erp.modules.performance.model.*;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import mz.multicore.erp.modules.performance.repository.SalesGoalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Harness permanente do Centro de Desempenho Comercial (CDC).
 * Valida todas as regras de negócio canónicas CDC-BIZ-01 a CDC-BIZ-12 conforme SPEC.
 */
class CommercialPerformanceHarnessTest {

    private SalesGoalRepository salesGoalRepository;
    private SalesGoalBonusRepository bonusRepository;
    private CompanyRepository companyRepository;
    private EmployeeRepository employeeRepository;
    private PayslipRepository payslipRepository;
    private PayrollPeriodService payrollPeriodService;
    private GoalProgressEngine progressEngine;
    private BonusCalculatorEngine bonusCalculatorEngine;
    private AuditLogService auditLogService;
    private SalesGoalService salesGoalService;
    private InvoiceRepository invoiceRepository;
    private EmployeeRankingService employeeRankingService;

    @BeforeEach
    void setUp() {
        salesGoalRepository = mock(SalesGoalRepository.class);
        bonusRepository = mock(SalesGoalBonusRepository.class);
        companyRepository = mock(CompanyRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        payslipRepository = mock(PayslipRepository.class);
        payrollPeriodService = mock(PayrollPeriodService.class);
        auditLogService = mock(AuditLogService.class);
        invoiceRepository = mock(InvoiceRepository.class);

        bonusCalculatorEngine = new BonusCalculatorEngine();
        progressEngine = new GoalProgressEngine(invoiceRepository, employeeRepository, bonusCalculatorEngine);
        salesGoalService = new SalesGoalService(
                salesGoalRepository, bonusRepository, companyRepository, employeeRepository,
                payslipRepository, payrollPeriodService, progressEngine, bonusCalculatorEngine, auditLogService
        );
        employeeRankingService = new EmployeeRankingService(invoiceRepository, employeeRepository, salesGoalRepository);

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");

        when(salesGoalRepository.save(any(SalesGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bonusRepository.save(any(SalesGoalBonus.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("CDC-BIZ-01: targetRevenue e targetMargin não podem ser ambos nulos ou zero")
    void cdcBiz01_targetRevenueOrMarginRequired() {
        CreateSalesGoalRequest req = new CreateSalesGoalRequest(
                1L, "Meta Sem Alvo", GoalPeriod.MONTHLY,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                GoalScope.COMPANY, null, null,
                null, null, null,
                BonusType.FIXED, BigDecimal.ZERO, null, true
        );

        assertThatThrownBy(() -> salesGoalService.createGoal(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Pelo menos um alvo deve ser definido");
    }

    @Test
    @DisplayName("CDC-BIZ-02: periodEnd deve ser posterior ou igual a periodStart")
    void cdcBiz02_periodEndAfterPeriodStart() {
        CreateSalesGoalRequest req = new CreateSalesGoalRequest(
                1L, "Meta Data Invalida", GoalPeriod.MONTHLY,
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), // Data fim antes do início
                GoalScope.COMPANY, null, null,
                new BigDecimal("50000.00"), null, null,
                BonusType.FIXED, BigDecimal.ZERO, null, true
        );

        assertThatThrownBy(() -> salesGoalService.createGoal(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("data de fim do período deve ser posterior");
    }

    @Test
    @DisplayName("CDC-BIZ-03: Para scope EMPLOYEE, scopeRefId deve ser colaborador válido da mesma empresa")
    void cdcBiz03_employeeScopeValidation() {
        Company otherCompany = new Company();
        otherCompany.setId(99L);

        Employee empOtherCompany = new Employee();
        empOtherCompany.setId(55L);
        empOtherCompany.setCompany(otherCompany);

        when(employeeRepository.findById(55L)).thenReturn(Optional.of(empOtherCompany));

        CreateSalesGoalRequest req = new CreateSalesGoalRequest(
                1L, "Meta João", GoalPeriod.MONTHLY,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                GoalScope.EMPLOYEE, 55L, null,
                new BigDecimal("50000.00"), null, null,
                BonusType.FIXED, BigDecimal.ZERO, null, true
        );

        assertThatThrownBy(() -> salesGoalService.createGoal(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mesma empresa");
    }

    @Test
    @DisplayName("CDC-BIZ-04: Meta ACHIEVED, MISSED ou CANCELLED não pode ser editada")
    void cdcBiz04_immutableTerminalStatus() {
        SalesGoal goal = new SalesGoal();
        goal.setId(10L);
        goal.setStatus(GoalStatus.ACHIEVED);
        when(salesGoalRepository.findById(10L)).thenReturn(Optional.of(goal));

        UpdateSalesGoalRequest update = new UpdateSalesGoalRequest(
                "Novo Nome", new BigDecimal("100000.00"), null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> salesGoalService.updateGoal(10L, update))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Não é permitido alterar uma meta no estado ACHIEVED");
    }

    @Test
    @DisplayName("CDC-BIZ-05: bonusCap limita o prémio calculado")
    void cdcBiz05_bonusCapApplied() {
        SalesGoal goal = new SalesGoal();
        goal.setBonusType(BonusType.PERCENTAGE_OF_REVENUE);
        goal.setBonusValue(new BigDecimal("10.00")); // 10%
        goal.setBonusCap(new BigDecimal("2000.00")); // Cap de 2.000

        BigDecimal bonus = bonusCalculatorEngine.calculateBonus(goal, new BigDecimal("50000.00"), BigDecimal.ZERO);
        assertThat(bonus).isEqualByComparingTo("2000.00"); // 5.000 limitado a 2.000
    }

    @Test
    @DisplayName("CDC-BIZ-06: Apenas MANAGER ou ADMIN pode criar meta")
    void cdcBiz06_requireManagerOrAdminForGoalCreation() {
        CurrentUserContext.setCurrentUser("vendedor", "SELLER");

        CreateSalesGoalRequest req = new CreateSalesGoalRequest(
                1L, "Meta Seller", GoalPeriod.MONTHLY,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                GoalScope.COMPANY, null, null,
                new BigDecimal("50000.00"), null, null,
                BonusType.FIXED, BigDecimal.ZERO, null, true
        );

        assertThatThrownBy(() -> salesGoalService.createGoal(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sem permissão para criar meta comercial");
    }

    @Test
    @DisplayName("CDC-BIZ-07: Apenas ADMIN pode aprovar prémios")
    void cdcBiz07_requireAdminForBonusApproval() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");

        assertThatThrownBy(() -> salesGoalService.approveBonus(1L, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sem permissão para aprovar prémio de desempenho");
    }

    @Test
    @DisplayName("CDC-BIZ-08: Um SalesGoalBonus já PAID não pode ser alterado")
    void cdcBiz08_paidBonusCannotBeModified() {
        SalesGoalBonus bonus = new SalesGoalBonus();
        bonus.setId(20L);
        bonus.setStatus(BonusStatus.PAID);
        when(bonusRepository.findById(20L)).thenReturn(Optional.of(bonus));

        assertThatThrownBy(() -> salesGoalService.adjustBonus(20L, new AdjustBonusRequest(new BigDecimal("500.00"), "Ajuste")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Não é permitido alterar um prémio já pago");
    }

    @Test
    @DisplayName("CDC-BIZ-09: A integração na folha requer que o PayrollPeriod esteja ABERTO")
    void cdcBiz09_payrollPeriodMustBeOpenForBonusIntegration() {
        Company company = new Company();
        company.setId(1L);

        Employee employee = new Employee();
        employee.setId(7L);
        employee.setName("Ana Silva");

        SalesGoalBonus bonus = new SalesGoalBonus();
        bonus.setId(30L);
        bonus.setCompany(company);
        bonus.setEmployee(employee);
        bonus.setStatus(BonusStatus.APPROVED);
        bonus.setApprovedAmount(new BigDecimal("4000.00"));

        when(bonusRepository.findById(30L)).thenReturn(Optional.of(bonus));
        when(payrollPeriodService.isClosed(2026, 10)).thenReturn(true); // Fechado!

        assertThatThrownBy(() -> salesGoalService.integrateBonusWithPayroll(30L, 2026, 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("período de folha 10/2026 está fechado");
    }

    @Test
    @DisplayName("CDC-BIZ-11: Alteração de meta gera entrada no log de auditoria com detalhes")
    void cdcBiz11_auditLogRecordedOnUpdate() {
        Company company = new Company();
        company.setId(1L);

        SalesGoal goal = new SalesGoal();
        goal.setId(10L);
        goal.setName("Meta Outubro");
        goal.setCompany(company);
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setTargetRevenue(new BigDecimal("50000.00"));
        goal.setBonusValue(BigDecimal.ZERO);

        when(salesGoalRepository.findById(10L)).thenReturn(Optional.of(goal));

        UpdateSalesGoalRequest update = new UpdateSalesGoalRequest(
                "Meta Outubro Atualizada", new BigDecimal("70000.00"), null, null, null, null, null, null, null
        );

        salesGoalService.updateGoal(10L, update);

        verify(auditLogService, times(1)).logEvent(
                eq("admin"), eq(1L), eq("GOAL_UPDATED"), any(String.class)
        );
    }

    @Test
    @DisplayName("CDC-BIZ-12: O ranking usa apenas faturas com isRealisedSale() = true")
    void cdcBiz12_rankingOnlyCountsRealisedSales() {
        Invoice draftInvoice = new Invoice();
        draftInvoice.setStatus(InvoiceStatus.DRAFT); // NOT realised
        draftInvoice.setTotalAmount(new BigDecimal("999999.00"));
        draftInvoice.setCreatedBy("joao");

        Invoice paidInvoice = new Invoice();
        paidInvoice.setStatus(InvoiceStatus.PAID); // Realised
        paidInvoice.setTotalAmount(new BigDecimal("5000.00"));
        paidInvoice.setCreatedBy("joao");
        paidInvoice.setLines(List.of());

        when(invoiceRepository.findByCompanyIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(draftInvoice, paidInvoice));

        List<EmployeeRankingDTO> ranking = employeeRankingService.getRanking(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(ranking).hasSize(1);
        // O valor deve ser apenas 5.000 da fatura PAID, nunca os 999.999 da fatura DRAFT
        assertThat(ranking.get(0).revenue()).isEqualByComparingTo("5000.00");
    }
}
