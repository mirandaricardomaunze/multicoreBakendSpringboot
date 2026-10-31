package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.hr.model.*;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Gestor transacional do ciclo de vida dos contratos de trabalho (criação, ativação, renovação e rescisão).
 */
@Service
public class ContractLifecycleService {

    private final EmploymentContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final DocumentNumberService documentNumberService;
    private final SalaryHistoryService salaryHistoryService;
    private final AuditLogService auditLogService;
    private final ContractValidationService validationService;
    private final ContractQueryService queryService;

    @Autowired
    public ContractLifecycleService(
            EmploymentContractRepository contractRepository,
            EmployeeRepository employeeRepository,
            CompanyRepository companyRepository,
            DocumentNumberService documentNumberService,
            SalaryHistoryService salaryHistoryService,
            AuditLogService auditLogService,
            ContractValidationService validationService,
            ContractQueryService queryService
    ) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.documentNumberService = documentNumberService;
        this.salaryHistoryService = salaryHistoryService;
        this.auditLogService = auditLogService;
        this.validationService = validationService;
        this.queryService = queryService;
    }

    @Transactional
    public EmploymentContractDTO createContract(CreateContractRequest request) {
        PermissionGuard.requireManagerOrAdmin("gerir contratos");
        Employee employee = findEmployee(request.employeeId());
        ContractType type = validationService.parseType(request.contractType());
        validationService.validateDates(type, request.startDate(), request.endDate(), request.probationEndDate(), request.termReason());

        EmploymentContract contract = new EmploymentContract();
        contract.setEmployee(employee);
        contract.setCompany(currentCompany());
        contract.setContractNumber(documentNumberService.next(DocumentSeries.EMPLOYMENT_CONTRACT));
        contract.setContractType(type);
        contract.setStatus(ContractStatus.RASCUNHO);
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setProbationEndDate(request.probationEndDate());
        contract.setAgreedSalary(request.agreedSalary());
        contract.setWeeklyHours(request.weeklyHours() <= 0 ? 40 : request.weeklyHours());
        contract.setJobTitle(request.jobTitle().trim());
        contract.setWorkLocation(blankToNull(request.workLocation()));
        contract.setTermReason(blankToNull(request.termReason()));

        EmploymentContract saved = contractRepository.save(contract);
        auditLogService.logCurrent("CONTRACT_CREATE", String.format(
                "Contrato %s (%s) criado para %s, início %s",
                saved.getContractNumber(), type.getLabel(), employee.getName(), saved.getStartDate()));
        return queryService.toDTO(saved);
    }

    @Transactional
    public EmploymentContractDTO activateContract(Long id) {
        PermissionGuard.requireManagerOrAdmin("gerir contratos");
        EmploymentContract contract = findContract(id);
        if (contract.getStatus() != ContractStatus.RASCUNHO) {
            throw new BusinessRuleException("Só um contrato em rascunho pode passar a vigente.");
        }
        validationService.ensureNoOverlap(contract, currentCompanyId());
        contract.setStatus(ContractStatus.VIGENTE);

        EmploymentContract saved = contractRepository.save(contract);
        syncEmployeeFromContract(saved);
        auditLogService.logCurrent("CONTRACT_ACTIVATE", String.format(
                "Contrato %s de %s passou a vigente (%s a %s)",
                saved.getContractNumber(), saved.getEmployee().getName(),
                saved.getStartDate(), saved.getEndDate() == null ? "sem termo" : saved.getEndDate()));
        return queryService.toDTO(saved);
    }

    @Transactional
    public EmploymentContractDTO renewContract(Long id, RenewContractRequest request) {
        PermissionGuard.requireManagerOrAdmin("gerir contratos");
        EmploymentContract previous = findContract(id);
        if (previous.getStatus() == ContractStatus.RASCUNHO) {
            throw new BusinessRuleException("Um contrato em rascunho não se renova — altere-o.");
        }
        if (!request.startDate().isAfter(previous.getStartDate())) {
            throw new BusinessRuleException("A renovação tem de começar depois do contrato anterior.");
        }
        validationService.validateDates(previous.getContractType(), request.startDate(), request.endDate(), null,
                request.termReason() == null ? previous.getTermReason() : request.termReason());

        EmploymentContract renewal = new EmploymentContract();
        renewal.setEmployee(previous.getEmployee());
        renewal.setCompany(previous.getCompany());
        renewal.setContractNumber(documentNumberService.next(DocumentSeries.EMPLOYMENT_CONTRACT));
        renewal.setContractType(previous.getContractType());
        renewal.setStatus(ContractStatus.RASCUNHO);
        renewal.setStartDate(request.startDate());
        renewal.setEndDate(request.endDate());
        renewal.setProbationEndDate(null);
        renewal.setAgreedSalary(request.agreedSalary() == null ? previous.getAgreedSalary() : request.agreedSalary());
        renewal.setWeeklyHours(previous.getWeeklyHours());
        renewal.setJobTitle(previous.getJobTitle());
        renewal.setWorkLocation(previous.getWorkLocation());
        renewal.setTermReason(request.termReason() == null ? previous.getTermReason() : blankToNull(request.termReason()));
        renewal.setRenewedFrom(previous);

        if (previous.getStatus() == ContractStatus.VIGENTE) {
            previous.setStatus(ContractStatus.CESSADO);
            previous.setTerminationDate(request.startDate().minusDays(1));
            previous.setTerminationReason("Renovado");
            contractRepository.save(previous);
        }

        EmploymentContract saved = contractRepository.save(renewal);
        auditLogService.logCurrent("CONTRACT_RENEW", String.format(
                "Contrato %s de %s renovado por %s (início %s)",
                previous.getContractNumber(), previous.getEmployee().getName(),
                saved.getContractNumber(), saved.getStartDate()));
        return queryService.toDTO(saved);
    }

    @Transactional
    public EmploymentContractDTO terminateContract(Long id, LocalDate terminationDate, String reason) {
        PermissionGuard.requireManagerOrAdmin("gerir contratos");
        EmploymentContract contract = findContract(id);
        if (contract.getStatus().isTerminal()) {
            throw new BusinessRuleException("Este contrato já está cessado.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("O motivo da cessação é obrigatório.");
        }
        LocalDate effective = terminationDate == null ? LocalDate.now() : terminationDate;
        if (effective.isBefore(contract.getStartDate())) {
            throw new BusinessRuleException("A cessação não pode ser anterior ao início do contrato.");
        }
        contract.setStatus(ContractStatus.CESSADO);
        contract.setTerminationDate(effective);
        contract.setTerminationReason(reason.trim());

        EmploymentContract saved = contractRepository.save(contract);
        auditLogService.logCurrent("CONTRACT_TERMINATE", String.format(
                "Contrato %s de %s cessado em %s: %s",
                saved.getContractNumber(), saved.getEmployee().getName(), effective, reason.trim()));
        return queryService.toDTO(saved);
    }

    private void syncEmployeeFromContract(EmploymentContract contract) {
        Employee employee = contract.getEmployee();
        try {
            salaryHistoryService.record(employee, contract.getAgreedSalary(), contract.getStartDate(),
                    SalaryChangeReason.CONTRATO, contract.getJobTitle(), null,
                    "Contrato " + contract.getContractNumber());
        } catch (BusinessRuleException alreadyAtThisSalary) {
            // Salário inalterado face ao vigente: nada a registar.
        }
    }

    private EmploymentContract findContract(Long id) {
        return contractRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado."));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
    }

    private Company currentCompany() {
        return companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada."));
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
