package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Orquestrador transacional de contratos de trabalho.
 * A invariante central é um só contrato vigente numa data.
 * Delega validações a {@link ContractValidationService}, ciclo de vida a {@link ContractLifecycleService}
 * e consultas a {@link ContractQueryService}.
 */
@Service
public class EmploymentContractService {

    /** Antecedência com que um fim de contrato passa a ser avisado. */
    public static final int CONTRACT_ENDING_ALERT_DAYS = 30;
    /** Antecedência com que um fim de período experimental passa a ser avisado. */
    public static final int PROBATION_ENDING_ALERT_DAYS = 7;

    private final ContractLifecycleService lifecycleService;
    private final ContractQueryService queryService;

    @Autowired
    public EmploymentContractService(
            ContractLifecycleService lifecycleService,
            ContractQueryService queryService
    ) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    public EmploymentContractService(
            EmploymentContractRepository contractRepository,
            EmployeeRepository employeeRepository,
            CompanyLookup companyLookup,
            DocumentNumberService documentNumberService,
            SalaryHistoryService salaryHistoryService,
            AuditLogService auditLogService
    ) {
        ContractValidationService vService = new ContractValidationService(contractRepository);
        ContractQueryService qService = new ContractQueryService(contractRepository);
        ContractLifecycleService lService = new ContractLifecycleService(
                contractRepository, employeeRepository, companyLookup,
                documentNumberService, salaryHistoryService, auditLogService,
                vService, qService
        );
        this.lifecycleService = lService;
        this.queryService = qService;
    }

    @Transactional(readOnly = true)
    public List<EmploymentContractDTO> getAllContracts() {
        return queryService.getAllContracts();
    }

    @Transactional(readOnly = true)
    public List<EmploymentContractDTO> getContractsOfEmployee(Long employeeId) {
        return queryService.getContractsOfEmployee(employeeId);
    }

    @Transactional(readOnly = true)
    public Optional<EmploymentContract> findContractInPeriod(Long employeeId, LocalDate from, LocalDate to) {
        return queryService.findContractInPeriod(employeeId, from, to);
    }

    @Transactional
    public EmploymentContractDTO createContract(CreateContractRequest request) {
        return lifecycleService.createContract(request);
    }

    @Transactional
    public EmploymentContractDTO activateContract(Long id) {
        return lifecycleService.activateContract(id);
    }

    @Transactional
    public EmploymentContractDTO renewContract(Long id, RenewContractRequest request) {
        return lifecycleService.renewContract(id, request);
    }

    @Transactional
    public EmploymentContractDTO terminateContract(Long id, LocalDate terminationDate, String reason) {
        return lifecycleService.terminateContract(id, terminationDate, reason);
    }

    @Transactional(readOnly = true)
    public ContractAlertsDTO getContractAlerts() {
        return queryService.getContractAlerts(CONTRACT_ENDING_ALERT_DAYS, PROBATION_ENDING_ALERT_DAYS);
    }
}
