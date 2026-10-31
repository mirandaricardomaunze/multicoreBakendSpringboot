package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.ExpenseClaim;
import mz.multicore.erp.modules.hr.model.ExpenseStatus;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.ExpenseClaimRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HrEmployeeManagementService {

    private final EmployeeRepository employeeRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final HrAccessGuard guard;
    private final ApprovalService approvalService;
    private final AuditLogService auditLogService;
    private final AppUserService appUserService;

    public HrEmployeeManagementService(
            EmployeeRepository employeeRepository,
            ExpenseClaimRepository expenseClaimRepository,
            HrAccessGuard guard,
            @Lazy ApprovalService approvalService,
            AuditLogService auditLogService,
            AppUserService appUserService
    ) {
        this.employeeRepository = employeeRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.guard = guard;
        this.approvalService = approvalService;
        this.auditLogService = auditLogService;
        this.appUserService = appUserService;
    }

    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) {
        Employee employee = guard.findEmployee(request.employeeId());
        guard.ensureCanActFor(employee);

        ExpenseClaim claim = new ExpenseClaim();
        claim.setEmployee(employee);
        claim.setAmount(request.amount());
        claim.setCategory(request.category());
        claim.setDescription(request.description());
        claim.setStatus(ExpenseStatus.PENDING_APPROVAL);

        claim = expenseClaimRepository.save(claim);

        String approvalDesc = String.format("Despesa de %s (%s) - %s",
                employee.getName(), claim.getCategory(), claim.getDescription());
        approvalService.submitRequest("EXPENSE", claim.getId(), claim.getAmount(), approvalDesc);

        auditLogService.logCurrent("EXPENSE_SUBMIT", String.format(
                "Despesa de %s submetida por %s: %s %s (%s)",
                employee.getName(), CurrentUserContext.getUsername(),
                claim.getAmount(), claim.getCategory(), claim.getDescription()));

        return toExpenseDTO(claim);
    }

    public List<ExpenseClaimDTO> getAllExpenses() {
        return expenseClaimRepository.findAllWithEmployeeByCompanyId(guard.currentCompanyId())
                .stream()
                .map(this::toExpenseDTO)
                .collect(Collectors.toList());
    }

    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findByCompanyIdOrderByName(guard.currentCompanyId())
                .stream()
                .map(this::employeeToDTO)
                .collect(Collectors.toList());
    }

    public EmployeeDTO createEmployee(UpsertEmployeeRequest request) {
        guard.ensureHrManager();
        Long companyId = guard.currentCompanyId();
        validateEmployeeUniqueness(companyId, null, request);

        Employee employee = new Employee();
        employee.setCompany(guard.currentCompany());
        employee.setStatus("ACTIVE");
        applyEmployee(employee, request);
        Employee saved = employeeRepository.save(employee);
        auditLogService.logCurrent("EMPLOYEE_CREATE", String.format(
                "Colaborador %s (nº %s) criado", saved.getName(), saved.getEmployeeNumber()));
        return employeeToDTO(saved);
    }

    public EmployeeDTO updateEmployee(Long id, UpsertEmployeeRequest request) {
        guard.ensureHrManager();
        Long companyId = guard.currentCompanyId();
        Employee employee = guard.findEmployee(id);
        validateEmployeeUniqueness(companyId, id, request);
        applyEmployee(employee, request);
        Employee saved = employeeRepository.save(employee);
        auditLogService.logCurrent("EMPLOYEE_UPDATE", String.format(
                "Colaborador %s (nº %s) actualizado", saved.getName(), saved.getEmployeeNumber()));
        return employeeToDTO(saved);
    }

    public EmployeeDTO changeEmployeeStatus(Long id, String status) {
        guard.ensureHrManager();
        Employee employee = guard.findEmployee(id);
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "SUSPENDED", "TERMINATED").contains(normalized)) {
            throw new BusinessRuleException("Estado laboral inválido.");
        }
        employee.setStatus(normalized);
        Employee saved = employeeRepository.save(employee);
        auditLogService.logCurrent("EMPLOYEE_STATUS", String.format(
                "Estado laboral de %s (nº %s) alterado para %s",
                saved.getName(), saved.getEmployeeNumber(), normalized));
        return employeeToDTO(saved);
    }

    private void validateEmployeeUniqueness(Long companyId, Long employeeId, UpsertEmployeeRequest request) {
        boolean numberExists = employeeId == null
                ? employeeRepository.existsByCompanyIdAndEmployeeNumberIgnoreCase(companyId, request.employeeNumber())
                : employeeRepository.existsByCompanyIdAndEmployeeNumberIgnoreCaseAndIdNot(companyId, request.employeeNumber(), employeeId);
        if (numberExists) {
            throw new BusinessRuleException("Já existe um colaborador com este número interno.");
        }
        boolean emailExists = employeeId == null
                ? employeeRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, request.email())
                : employeeRepository.existsByCompanyIdAndEmailIgnoreCaseAndIdNot(companyId, request.email(), employeeId);
        if (emailExists) {
            throw new BusinessRuleException("Já existe um colaborador com este email.");
        }
        if (request.contractEndDate() != null && request.contractEndDate().isBefore(request.hireDate())) {
            throw new BusinessRuleException("O fim do contrato não pode ser anterior à admissão.");
        }
    }

    private void applyEmployee(Employee employee, UpsertEmployeeRequest request) {
        employee.setEmployeeNumber(request.employeeNumber().trim());
        employee.setName(request.name().trim());
        employee.setEmail(request.email().trim().toLowerCase());
        employee.setPhone(blankToNull(request.phone()));
        employee.setPhoto(request.photo());
        employee.setTaxId(blankToNull(request.taxId()));
        employee.setInssNumber(blankToNull(request.inssNumber()));
        employee.setDependentsCount(request.dependentsCount());
        employee.setDepartment(request.department().trim());
        employee.setRole(request.role().trim().toUpperCase());
        if (employee.getId() != null && employee.getBaseSalary() != null
                && request.baseSalary() != null
                && employee.getBaseSalary().compareTo(request.baseSalary()) != 0) {
            throw new BusinessRuleException(
                    "O salário não se altera na ficha do colaborador. Registe uma alteração salarial "
                            + "com data de efeito e motivo, em RH → Evolução Salarial.");
        }
        if (employee.getBaseSalary() == null) {
            employee.setBaseSalary(request.baseSalary());
        }
        employee.setHireDate(request.hireDate());
        employee.setContractEndDate(request.contractEndDate());
        employee.setBankName(blankToNull(request.bankName()));
        employee.setBankAccount(blankToNull(request.bankAccount()));
        employee.setAppUser(resolveAppUser(employee, blankToNull(request.username())));
    }

    private AppUser resolveAppUser(Employee employee, String username) {
        if (username == null) {
            return null;
        }
        AppUser user = appUserService.findByUsername(username);
        if (user == null) {
            throw new BusinessRuleException("Utilizador \"" + username + "\" não existe.");
        }
        Long companyId = guard.currentCompanyId();
        if (!user.hasCompany(companyId)) {
            throw new BusinessRuleException(
                    "O utilizador \"" + username + "\" não tem acesso a esta empresa.");
        }
        boolean taken = employee.getId() == null
                ? employeeRepository.existsByCompanyIdAndAppUserUsername(companyId, username)
                : employeeRepository.existsByCompanyIdAndAppUserUsernameAndIdNot(companyId, username, employee.getId());
        if (taken) {
            throw new BusinessRuleException(
                    "O utilizador \"" + username + "\" já está associado a outro colaborador.");
        }
        return user;
    }

    public EmployeeDTO employeeToDTO(Employee e) {
        return new EmployeeDTO(
                e.getId(), e.getEmployeeNumber(), e.getName(), e.getEmail(), e.getPhone(), e.getPhoto(),
                e.getTaxId(), e.getInssNumber(), e.getDependentsCount(), e.getDepartment(), e.getBaseSalary(), e.getRole(),
                e.getHireDate(), e.getContractEndDate(), e.getStatus(),
                e.getAppUser() != null ? e.getAppUser().getUsername() : null,
                e.getBankName(), e.getBankAccount()
        );
    }

    private ExpenseClaimDTO toExpenseDTO(ExpenseClaim claim) {
        return new ExpenseClaimDTO(
                claim.getId(), claim.getEmployee().getId(), claim.getEmployee().getName(),
                claim.getAmount(), claim.getCategory(), claim.getDescription(),
                claim.getStatus(), claim.getRejectionReason(),
                claim.getCreatedAt() != null ? claim.getCreatedAt() : LocalDateTime.now()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
