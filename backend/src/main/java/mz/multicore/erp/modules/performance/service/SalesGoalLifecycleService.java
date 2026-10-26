package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.performance.dto.CreateSalesGoalRequest;
import mz.multicore.erp.modules.performance.dto.SalesGoalDTO;
import mz.multicore.erp.modules.performance.dto.UpdateSalesGoalRequest;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.repository.SalesGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Criação, edição, cancelamento e validações de regras de negócio de metas comerciais.
 */
@Service
public class SalesGoalLifecycleService {

    private final SalesGoalRepository salesGoalRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesGoalQueryService queryService;
    private final AuditLogService auditLogService;

    public SalesGoalLifecycleService(SalesGoalRepository salesGoalRepository,
                                     CompanyRepository companyRepository,
                                     EmployeeRepository employeeRepository,
                                     SalesGoalQueryService queryService,
                                     AuditLogService auditLogService) {
        this.salesGoalRepository = salesGoalRepository;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.queryService = queryService;
        this.auditLogService = auditLogService;
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
        auditLogService.logEvent(CurrentUserContext.getUsername(), company.getId(), "GOAL_CREATED",
                String.format("Meta comercial '%s' criada com alvo receita=%s, margem=%s",
                        saved.getName(), saved.getTargetRevenue(), saved.getTargetMargin()));

        return queryService.toDTO(saved);
    }

    @Transactional
    public SalesGoalDTO updateGoal(Long id, UpdateSalesGoalRequest request) {
        PermissionGuard.requireManagerOrAdmin("actualizar meta comercial");

        SalesGoal goal = salesGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));

        if (goal.getStatus() == GoalStatus.ACHIEVED || goal.getStatus() == GoalStatus.MISSED || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessRuleException(String.format("Não é permitido alterar uma meta no estado %s.", goal.getStatus()));
        }

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

        auditLogService.logEvent(CurrentUserContext.getUsername(), saved.getCompany().getId(), "GOAL_UPDATED",
                String.format("Meta '%s' alterada de [%s] para [%s]", saved.getName(), previousSummary, newSummary));

        return queryService.toDTO(saved);
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

    private void validateGoalDefinition(String name, LocalDate start, LocalDate end,
                                        BigDecimal targetRevenue, BigDecimal targetMargin,
                                        GoalScope scope, Long scopeRefId, Long companyId) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome da meta é obrigatório.");
        }
        if (start == null || end == null) {
            throw new BusinessRuleException("As datas de início e fim do período são obrigatórias.");
        }
        if (end.isBefore(start)) {
            throw new BusinessRuleException("A data de fim do período deve ser posterior ou igual à data de início.");
        }
        boolean hasRev = targetRevenue != null && targetRevenue.compareTo(BigDecimal.ZERO) > 0;
        boolean hasMargin = targetMargin != null && targetMargin.compareTo(BigDecimal.ZERO) > 0;
        if (!hasRev && !hasMargin) {
            throw new BusinessRuleException("Pelo menos um alvo deve ser definido: receita alvo ou margem bruta alvo.");
        }
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
}
