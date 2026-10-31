package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SalaryChange;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.SalaryChangeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class SalaryChangeMutationService {

    private final SalaryChangeRepository salaryChangeRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public SalaryChangeMutationService(SalaryChangeRepository salaryChangeRepository,
                                       EmployeeRepository employeeRepository,
                                       CompanyRepository companyRepository,
                                       AuditLogService auditLogService) {
        this.salaryChangeRepository = salaryChangeRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    public Optional<BigDecimal> salaryOn(Long employeeId, LocalDate date) {
        return salaryChangeRepository.findEffectiveOn(employeeId, CurrentUserContext.requireCurrentCompanyId(), date)
                .stream().findFirst().map(SalaryChange::getNewSalary);
    }

    public SalaryChangeDTO registerChange(CreateSalaryChangeRequest request) {
        PermissionGuard.requireManagerOrAdmin("alterar salários");
        Employee employee = findEmployee(request.employeeId());
        SalaryChangeReason reason = parseReason(request.reason());
        return record(employee, request.newSalary(), request.effectiveDate(), reason,
                blankToNull(request.jobTitle()), blankToNull(request.department()),
                blankToNull(request.notes()));
    }

    public SalaryChangeDTO record(Employee employee, BigDecimal newSalary, LocalDate effectiveDate,
                                  SalaryChangeReason reason, String jobTitle, String department,
                                  String notes) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        BigDecimal previous = salaryOn(employee.getId(), effectiveDate)
                .orElse(employee.getBaseSalary());

        if (previous != null && previous.compareTo(newSalary) == 0) {
            throw new BusinessRuleException(String.format(
                    "O salário de %s já é %s nessa data — não há alteração a registar.",
                    employee.getName(), newSalary));
        }
        if (salaryChangeRepository.existsByCompanyIdAndEmployeeIdAndEffectiveDate(
                companyId, employee.getId(), effectiveDate)) {
            throw new BusinessRuleException(String.format(
                    "%s já tem uma alteração salarial com efeito a %s.",
                    employee.getName(), effectiveDate));
        }

        SalaryChange change = new SalaryChange();
        change.setEmployee(employee);
        change.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        change.setPreviousSalary(previous);
        change.setNewSalary(newSalary);
        change.setEffectiveDate(effectiveDate);
        change.setReason(reason);
        change.setJobTitle(jobTitle);
        change.setDepartment(department);
        change.setApprovedBy(CurrentUserContext.getUsername());
        change.setNotes(notes);

        SalaryChange saved = salaryChangeRepository.save(change);
        applyToEmployeeCardIfCurrent(employee, saved);

        auditLogService.logCurrent("SALARY_CHANGE", String.format(
                "Salário de %s: %s → %s com efeito a %s (%s)%s",
                employee.getName(), previous == null ? "sem registo" : previous, newSalary,
                effectiveDate, reason.getLabel(), notes == null ? "" : ". " + notes));
        return toDTO(saved);
    }

    private void applyToEmployeeCardIfCurrent(Employee employee, SalaryChange change) {
        if (change.appliesOn(LocalDate.now())) {
            employee.setBaseSalary(change.getNewSalary());
            if (change.getJobTitle() != null) {
                employee.setRole(change.getJobTitle());
            }
            if (change.getDepartment() != null) {
                employee.setDepartment(change.getDepartment());
            }
            employeeRepository.save(employee);
        }
    }

    private SalaryChangeReason parseReason(String raw) {
        try {
            return SalaryChangeReason.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Motivo de alteração salarial desconhecido: " + raw);
        }
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public SalaryChangeDTO toDTO(SalaryChange c) {
        BigDecimal difference = c.getPreviousSalary() == null
                ? null : c.getNewSalary().subtract(c.getPreviousSalary());
        return new SalaryChangeDTO(c.getId(), c.getEmployee().getId(), c.getEmployee().getName(),
                c.getPreviousSalary(), c.getNewSalary(), difference, c.getEffectiveDate(),
                c.getReason().name(), c.getReason().getLabel(), c.getJobTitle(), c.getDepartment(),
                c.getApprovedBy(), c.getNotes());
    }
}
