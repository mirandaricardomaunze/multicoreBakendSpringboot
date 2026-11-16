package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.SalaryChangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SalaryHistoryService {

    private final SalaryChangeRepository salaryChangeRepository;
    private final SalaryChangeMutationService mutationService;

    @Autowired
    public SalaryHistoryService(SalaryChangeRepository salaryChangeRepository,
                                SalaryChangeMutationService mutationService) {
        this.salaryChangeRepository = salaryChangeRepository;
        this.mutationService = mutationService;
    }

    public SalaryHistoryService(SalaryChangeRepository salaryChangeRepository,
                                EmployeeRepository employeeRepository,
                                CompanyLookup companyLookup,
                                AuditLogService auditLogService) {
        this(salaryChangeRepository,
                new SalaryChangeMutationService(salaryChangeRepository, employeeRepository, companyLookup, auditLogService));
    }

    @Transactional(readOnly = true)
    public List<SalaryChangeDTO> getHistory(Long employeeId) {
        return salaryChangeRepository.findByEmployee(employeeId, CurrentUserContext.requireCurrentCompanyId()).stream()
                .map(mutationService::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> salaryOn(Long employeeId, LocalDate date) {
        return mutationService.salaryOn(employeeId, date);
    }

    @Transactional
    public SalaryChangeDTO registerChange(CreateSalaryChangeRequest request) {
        PermissionGuard.requireManagerOrAdmin("alterar salários");
        return mutationService.registerChange(request);
    }

    @Transactional
    public SalaryChangeDTO record(Employee employee, BigDecimal newSalary, LocalDate effectiveDate,
                                  SalaryChangeReason reason, String jobTitle, String department,
                                  String notes) {
        return mutationService.record(employee, newSalary, effectiveDate, reason, jobTitle, department, notes);
    }
}
