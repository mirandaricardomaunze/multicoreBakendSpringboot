package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesGoalService {

    private final SalesGoalRepository salesGoalRepository;
    private final SalesGoalBonusRepository bonusRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final PayrollPeriodService payrollPeriodService;
    private final GoalProgressEngine progressEngine;
    private final BonusCalculatorEngine bonusCalculatorEngine;
    private final AuditLogService auditLogService;

    public SalesGoalService(SalesGoalRepository salesGoalRepository,
                            SalesGoalBonusRepository bonusRepository,
                            CompanyRepository companyRepository,
                            EmployeeRepository employeeRepository,
                            PayslipRepository payslipRepository,
                            PayrollPeriodService payrollPeriodService,
                            GoalProgressEngine progressEngine,
                            BonusCalculatorEngine bonusCalculatorEngine,
                            AuditLogService auditLogService) {
        this.salesGoalRepository = salesGoalRepository;
        this.bonusRepository = bonusRepository;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.payslipRepository = payslipRepository;
        this.payrollPeriodService = payrollPeriodService;
        this.progressEngine = progressEngine;
        this.bonusCalculatorEngine = bonusCalculatorEngine;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SalesGoalDTO> listGoals(Long companyId, GoalStatus status, GoalPeriod period) {
        List<SalesGoal> list;
        if (status != null && period != null) {
            list = salesGoalRepository.findByCompanyIdAndStatusAndPeriodOrderByPeriodStartDesc(companyId, status, period);
        } else if (status != null) {
            list = salesGoalRepository.findByCompanyIdAndStatusOrderByPeriodStartDesc(companyId, status);
        } else if (period != null) {
            list = salesGoalRepository.findByCompanyIdAndPeriodOrderByPeriodStartDesc(companyId, period);
        } else {
            list = salesGoalRepository.findByCompanyIdOrderByPeriodStartDesc(companyId);
        }
        return list.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public SalesGoalDTO getGoal(Long id, Long companyId) {
        SalesGoal goal = findGoal(id, companyId);
        return toDTO(goal);
    }

    @Transactional
    public SalesGoalDTO createGoal(CreateSalesGoalRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar meta comercial");

        validateGoalDefinition(request.name(), request.periodStart(), request.periodEnd(),
                request.targetRevenue(), request.targetMargin(), request.scope(), request.scopeRefId(), request.companyId());

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        if (salesGoalRepository.existsByCompanyIdAndNameAndPeriodStart(company.getId(), request.name().trim(), request.periodStart())) {
            throw new BusinessRuleException("Já existe uma meta com este nome para o mesmo início de período nesta empresa.");
        }

        SalesGoal goal = new SalesGoal();
        goal.setCompany(company);
        goal.setName(request.name().trim());
        goal.setPeriod(request.period() != null ? request.period() : GoalPeriod.MONTHLY);
        goal.setPeriodStart(request.periodStart());
        goal.setPeriodEnd(request.periodEnd());
        goal.setScope(request.scope() != null ? request.scope() : GoalScope.COMPANY);
        goal.setScopeRefId(request.scopeRefId());
        goal.setScopeLabel(resolveScopeLabel(request.scope(), request.scopeRefId(), request.scopeLabel(), company.getId()));
        goal.setTargetRevenue(request.targetRevenue());
        goal.setTargetMargin(request.targetMargin());
        goal.setTargetMarginPct(request.targetMarginPct());
        goal.setBonusType(request.bonusType() != null ? request.bonusType() : BonusType.FIXED);
        goal.setBonusValue(request.bonusValue() != null ? request.bonusValue() : BigDecimal.ZERO);
        goal.setBonusCap(request.bonusCap());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setAutoApplyBonus(request.autoApplyBonus() != null ? request.autoApplyBonus() : true);
        goal.setCreatedBy(CurrentUserContext.getUsername());

        SalesGoal saved = salesGoalRepository.save(goal);

        // CDC-BIZ-11: Log de auditoria
        auditLogService.logEvent(CurrentUserContext.getUsername(), company.getId(), "GOAL_CREATED",
                String.format("Meta comercial '%s' criada com alvo receita=%s, margem=%s",
                        saved.getName(), saved.getTargetRevenue(), saved.getTargetMargin()));

        return toDTO(saved);
    }

    @Transactional
    public SalesGoalDTO updateGoal(Long id, UpdateSalesGoalRequest request) {
        PermissionGuard.requireManagerOrAdmin("actualizar meta comercial");

        SalesGoal goal = salesGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));

        // CDC-BIZ-04: Meta ACHIEVED/MISSED/CANCELLED não pode ser alterada
        if (goal.getStatus() == GoalStatus.ACHIEVED || goal.getStatus() == GoalStatus.MISSED || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessRuleException(String.format("Não é permitido alterar uma meta no estado %s.", goal.getStatus()));
        }

        // CDC-BIZ-01
        BigDecimal newRevenue = request.targetRevenue() != null ? request.targetRevenue() : goal.getTargetRevenue();
        BigDecimal newMargin = request.targetMargin() != null ? request.targetMargin() : goal.getTargetMargin();
        if ((newRevenue == null || newRevenue.compareTo(BigDecimal.ZERO) <= 0) &&
                (newMargin == null || newMargin.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessRuleException("Pelo menos um alvo deve ser definido: receita alvo ou margem alvo.");
        }

        String previousSummary = String.format("nome='%s', receita=%s, margem=%s, bonus=%s, status=%s",
                goal.getName(), goal.getTargetRevenue(), goal.getTargetMargin(), goal.getBonusValue(), goal.getStatus());

        if (request.name() != null && !request.name().isBlank()) {
            goal.setName(request.name().trim());
        }
        goal.setTargetRevenue(request.targetRevenue());
        goal.setTargetMargin(request.targetMargin());
        if (request.targetMarginPct() != null) {
            goal.setTargetMarginPct(request.targetMarginPct());
        }
        if (request.bonusType() != null) {
            goal.setBonusType(request.bonusType());
        }
        if (request.bonusValue() != null) {
            goal.setBonusValue(request.bonusValue());
        }
        goal.setBonusCap(request.bonusCap());
        if (request.autoApplyBonus() != null) {
            goal.setAutoApplyBonus(request.autoApplyBonus());
        }
        if (request.status() != null) {
            goal.setStatus(request.status());
        }

        SalesGoal saved = salesGoalRepository.save(goal);

        String newSummary = String.format("nome='%s', receita=%s, margem=%s, bonus=%s, status=%s",
                saved.getName(), saved.getTargetRevenue(), saved.getTargetMargin(), saved.getBonusValue(), saved.getStatus());

        // CDC-BIZ-11
        auditLogService.logEvent(CurrentUserContext.getUsername(), saved.getCompany().getId(), "GOAL_UPDATED",
                String.format("Meta '%s' alterada de [%s] para [%s]", saved.getName(), previousSummary, newSummary));

        return toDTO(saved);
    }

    @Transactional
    public void cancelGoal(Long id) {
        PermissionGuard.requireManagerOrAdmin("cancelar meta comercial");

        SalesGoal goal = salesGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));

        if (goal.getStatus() == GoalStatus.CANCELLED) {
            return;
        }

        GoalStatus previousStatus = goal.getStatus();
        goal.setStatus(GoalStatus.CANCELLED);
        salesGoalRepository.save(goal);

        auditLogService.logEvent(CurrentUserContext.getUsername(), goal.getCompany().getId(), "GOAL_CANCELLED",
                String.format("Meta '%s' cancelada (estado anterior: %s)", goal.getName(), previousStatus));
    }

    @Transactional(readOnly = true)
    public SalesGoalProgressDTO getGoalProgress(Long id) {
        SalesGoal goal = salesGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));
        return progressEngine.calculateProgress(goal, LocalDate.now());
    }

    @Transactional
    public List<SalesGoalProgressDTO> getActiveGoalsSummary(Long companyId) {
        List<SalesGoal> active = salesGoalRepository.findByCompanyIdAndStatus(companyId, GoalStatus.ACTIVE);
        List<SalesGoalProgressDTO> summaries = new ArrayList<>();

        for (SalesGoal goal : active) {
            SalesGoalProgressDTO progress = progressEngine.calculateProgress(goal, LocalDate.now());
            summaries.add(progress);

            // Check if achieved
            if (progress.progressPct().compareTo(BigDecimal.valueOf(100)) >= 0) {
                goal.setStatus(GoalStatus.ACHIEVED);
                salesGoalRepository.save(goal);

                auditLogService.logEvent("SYSTEM", companyId, "GOAL_ACHIEVED",
                        String.format("Meta '%s' atingida com %s%% de progresso", goal.getName(), progress.progressPct()));

                if (goal.isAutoApplyBonus()) {
                    generateBonusesForAchievedGoal(goal, progress);
                }
            } else if (LocalDate.now().isAfter(goal.getPeriodEnd())) {
                goal.setStatus(GoalStatus.MISSED);
                salesGoalRepository.save(goal);

                auditLogService.logEvent("SYSTEM", companyId, "GOAL_MISSED",
                        String.format("Meta '%s' não atingida ao fim do período (%s%% de progresso)", goal.getName(), progress.progressPct()));
            }
        }

        return summaries;
    }

    @Transactional
    public void generateBonusesForAchievedGoal(SalesGoal goal, SalesGoalProgressDTO progress) {
        if (goal == null || goal.getStatus() != GoalStatus.ACHIEVED) {
            return;
        }

        BigDecimal calculatedBonus = bonusCalculatorEngine.calculateBonus(goal, progress.currentRevenue(), progress.currentMargin());
        if (calculatedBonus.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (goal.getScope() == GoalScope.EMPLOYEE && goal.getScopeRefId() != null) {
            Employee employee = employeeRepository.findById(goal.getScopeRefId()).orElse(null);
            if (employee != null && bonusRepository.findByGoalIdAndEmployeeId(goal.getId(), employee.getId()).isEmpty()) {
                createBonusRecord(goal, employee, calculatedBonus);
            }
        } else {
            // Company/Warehouse scope: distribute equally to active employees of the company/scope
            List<Employee> activeEmployees = employeeRepository.findByCompanyIdAndStatus(goal.getCompany().getId(), "ACTIVE");
            if (!activeEmployees.isEmpty()) {
                BigDecimal share = calculatedBonus.divide(BigDecimal.valueOf(activeEmployees.size()), 2, RoundingMode.HALF_UP);
                for (Employee emp : activeEmployees) {
                    if (bonusRepository.findByGoalIdAndEmployeeId(goal.getId(), emp.getId()).isEmpty()) {
                        createBonusRecord(goal, emp, share);
                    }
                }
            }
        }
    }

    private void createBonusRecord(SalesGoal goal, Employee employee, BigDecimal amount) {
        SalesGoalBonus bonus = new SalesGoalBonus();
        bonus.setGoal(goal);
        bonus.setEmployee(employee);
        bonus.setCompany(goal.getCompany());
        bonus.setCalculatedAmount(amount);
        bonus.setApprovedAmount(amount);
        bonus.setStatus(BonusStatus.PENDING);
        bonus.setCreatedBy("SYSTEM");
        bonusRepository.save(bonus);

        auditLogService.logEvent("SYSTEM", goal.getCompany().getId(), "BONUS_CALCULATED",
                String.format("Prémio de %s MZN calculado para meta '%s' (colaborador: %s)",
                        amount, goal.getName(), employee != null ? employee.getName() : "Geral"));
    }

    // ─── Bonus Management ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalesGoalBonusDTO> listBonuses(Long companyId, BonusStatus status) {
        List<SalesGoalBonus> list = status != null
                ? bonusRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status)
                : bonusRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        return list.stream().map(this::toDTO).toList();
    }

    @Transactional
    public SalesGoalBonusDTO approveBonus(Long id, ApproveBonusRequest request) {
        // CDC-BIZ-07: Apenas ADMIN pode aprovar
        PermissionGuard.requireAdmin("aprovar prémio de desempenho");

        SalesGoalBonus bonus = bonusRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo de prémio não encontrado."));

        // CDC-BIZ-08: Um prémio já PAID não pode ser alterado
        if (bonus.getStatus() == BonusStatus.PAID) {
            throw new BusinessRuleException("Não é permitido alterar um prémio já pago.");
        }

        BigDecimal amount = (request != null && request.approvedAmount() != null && request.approvedAmount().compareTo(BigDecimal.ZERO) >= 0)
                ? request.approvedAmount()
                : bonus.getCalculatedAmount();

        bonus.setApprovedAmount(amount);
        if (request != null && request.justification() != null && !request.justification().isBlank()) {
            bonus.setJustification(request.justification().trim());
        }
        bonus.setStatus(BonusStatus.APPROVED);
        bonus.setApprovedBy(CurrentUserContext.getUsername());

        SalesGoalBonus saved = bonusRepository.save(bonus);

        auditLogService.logEvent(CurrentUserContext.getUsername(), saved.getCompany().getId(), "BONUS_APPROVED",
                String.format("Prémio de %s MZN aprovado para %s na meta '%s'",
                        saved.getApprovedAmount(),
                        saved.getEmployee() != null ? saved.getEmployee().getName() : "Equipa",
                        saved.getGoal().getName()));

        return toDTO(saved);
    }

    @Transactional
    public SalesGoalBonusDTO adjustBonus(Long id, AdjustBonusRequest request) {
        // CDC-BIZ-07: Apenas ADMIN pode ajustar
        PermissionGuard.requireAdmin("ajustar prémio de desempenho");

        if (request == null || request.adjustedAmount() == null || request.adjustedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Indique um valor válido para o ajuste do prémio.");
        }
        if (request.justification() == null || request.justification().trim().length() < 3) {
            throw new BusinessRuleException("A justificação do ajuste é obrigatória (mínimo 3 caracteres).");
        }

        SalesGoalBonus bonus = bonusRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo de prémio não encontrado."));

        // CDC-BIZ-08
        if (bonus.getStatus() == BonusStatus.PAID) {
            throw new BusinessRuleException("Não é permitido alterar um prémio já pago.");
        }

        BigDecimal oldAmount = bonus.getApprovedAmount();
        bonus.setApprovedAmount(request.adjustedAmount());
        bonus.setJustification(request.justification().trim());
        bonus.setApprovedBy(CurrentUserContext.getUsername());

        SalesGoalBonus saved = bonusRepository.save(bonus);

        // CDC-BIZ-11
        auditLogService.logEvent(CurrentUserContext.getUsername(), saved.getCompany().getId(), "BONUS_ADJUSTED",
                String.format("Prémio para %s ajustado de %s para %s MZN. Justificação: %s",
                        saved.getEmployee() != null ? saved.getEmployee().getName() : "Equipa",
                        oldAmount, saved.getApprovedAmount(), saved.getJustification()));

        return toDTO(saved);
    }

    @Transactional
    public SalesGoalBonusDTO integrateBonusWithPayroll(Long bonusId, int year, int month) {
        PermissionGuard.requireAdmin("integrar prémio de desempenho na folha salarial");

        SalesGoalBonus bonus = bonusRepository.findById(bonusId)
                .orElseThrow(() -> new BusinessRuleException("Prémio não encontrado."));

        if (bonus.getStatus() != BonusStatus.APPROVED) {
            throw new BusinessRuleException("Apenas prémios no estado APROVADO podem ser integrados na folha salarial.");
        }
        if (bonus.getEmployee() == null) {
            throw new BusinessRuleException("Este prémio não tem colaborador associado para recibo de vencimento.");
        }

        Long companyId = bonus.getCompany().getId();

        // CDC-BIZ-09: A integração requer que o PayrollPeriod esteja ABERTO
        if (payrollPeriodService.isClosed(year, month)) {
            throw new BusinessRuleException(String.format("O período de folha %02d/%d está fechado. Reabra o período ou integre num mês aberto.", month, year));
        }

        Payslip payslip = payslipRepository.findByEmployeeIdAndYearAndMonth(bonus.getEmployee().getId(), year, month)
                .orElseThrow(() -> new BusinessRuleException(String.format("Não existe recibo de vencimento emitido para %s em %02d/%d.", bonus.getEmployee().getName(), month, year)));

        if ("PAID".equals(payslip.getStatus())) {
            throw new BusinessRuleException("O recibo deste colaborador para este mês já foi pago.");
        }

        // Add to payslip salesBonus and netPay
        BigDecimal currentBonus = payslip.getSalesBonus() != null ? payslip.getSalesBonus() : BigDecimal.ZERO;
        payslip.setSalesBonus(currentBonus.add(bonus.getApprovedAmount()));
        payslip.setNetPay(payslip.getNetPay().add(bonus.getApprovedAmount()));
        payslipRepository.save(payslip);

        bonus.setPayslip(payslip);
        bonus.setStatus(BonusStatus.PAID);
        SalesGoalBonus saved = bonusRepository.save(bonus);

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "BONUS_PAID_IN_PAYSLIP",
                String.format("Prémio de %s MZN integrado no recibo %s (%02d/%d) de %s",
                        bonus.getApprovedAmount(), payslip.getPayslipNumber(), month, year, bonus.getEmployee().getName()));

        return toDTO(saved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private SalesGoal findGoal(Long id, Long companyId) {
        return salesGoalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));
    }

    private void validateGoalDefinition(String name, LocalDate start, LocalDate end,
                                        BigDecimal targetRevenue, BigDecimal targetMargin,
                                        GoalScope scope, Long scopeRefId, Long companyId) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome da meta é obrigatório.");
        }
        if (start == null || end == null) {
            throw new BusinessRuleException("As datas de início e fim do período são obrigatórias.");
        }
        // CDC-BIZ-02
        if (end.isBefore(start)) {
            throw new BusinessRuleException("A data de fim do período deve ser posterior ou igual à data de início.");
        }
        // CDC-BIZ-01
        boolean hasRev = targetRevenue != null && targetRevenue.compareTo(BigDecimal.ZERO) > 0;
        boolean hasMargin = targetMargin != null && targetMargin.compareTo(BigDecimal.ZERO) > 0;
        if (!hasRev && !hasMargin) {
            throw new BusinessRuleException("Pelo menos um alvo deve ser definido: receita alvo ou margem bruta alvo.");
        }
        // CDC-BIZ-03
        if (scope == GoalScope.EMPLOYEE) {
            if (scopeRefId == null) {
                throw new BusinessRuleException("Para meta com escopo de colaborador, é obrigatório selecionar o trabalhador.");
            }
            Employee emp = employeeRepository.findById(scopeRefId)
                    .orElseThrow(() -> new BusinessRuleException("Colaborador selecionado não foi encontrado."));
            if (!emp.getCompany().getId().equals(companyId)) {
                throw new BusinessRuleException("O colaborador deve pertencer à mesma empresa da meta.");
            }
        }
    }

    private String resolveScopeLabel(GoalScope scope, Long refId, String customLabel, Long companyId) {
        if (customLabel != null && !customLabel.isBlank()) {
            return customLabel.trim();
        }
        if (scope == GoalScope.COMPANY) {
            return "Toda a Empresa";
        }
        if (scope == GoalScope.EMPLOYEE && refId != null) {
            return employeeRepository.findById(refId).map(Employee::getName).orElse("Colaborador #" + refId);
        }
        return scope.name() + (refId != null ? " #" + refId : "");
    }

    private SalesGoalDTO toDTO(SalesGoal g) {
        return new SalesGoalDTO(
                g.getId(),
                g.getCompany().getId(),
                g.getName(),
                g.getPeriod(),
                g.getPeriodStart(),
                g.getPeriodEnd(),
                g.getScope(),
                g.getScopeRefId(),
                g.getScopeLabel(),
                g.getTargetRevenue(),
                g.getTargetMargin(),
                g.getTargetMarginPct(),
                g.getBonusType(),
                g.getBonusValue(),
                g.getBonusCap(),
                g.getStatus(),
                g.isAutoApplyBonus(),
                g.getCreatedBy()
        );
    }

    private SalesGoalBonusDTO toDTO(SalesGoalBonus b) {
        return new SalesGoalBonusDTO(
                b.getId(),
                b.getGoal().getId(),
                b.getGoal().getName(),
                b.getEmployee() != null ? b.getEmployee().getId() : null,
                b.getEmployee() != null ? b.getEmployee().getName() : "Equipa",
                b.getCompany().getId(),
                b.getCalculatedAmount(),
                b.getApprovedAmount(),
                b.getJustification(),
                b.getStatus(),
                b.getPayslip() != null ? b.getPayslip().getId() : null,
                b.getCreatedBy(),
                b.getApprovedBy()
        );
    }
}
