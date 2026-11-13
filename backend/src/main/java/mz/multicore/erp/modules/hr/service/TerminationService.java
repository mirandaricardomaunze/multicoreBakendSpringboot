package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TerminationRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestrador de Cessação de Vínculo e Acerto Final (Fase 110).
 * Decomposto em colaboradores especializados para conformidade estrita com SOLID/SRP.
 * Ver docs/TERMINATION_SERVICE_DECOMPOSITION_SPEC.md.
 */
@Service
public class TerminationService {

    private final TerminationSettlementCalculator calculator;
    private final TerminationLifecycleService lifecycleService;
    private final TerminationQueryService queryService;

    @Autowired
    public TerminationService(TerminationSettlementCalculator calculator,
                              TerminationLifecycleService lifecycleService,
                              TerminationQueryService queryService) {
        this.calculator = calculator;
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    /**
     * Construtor retrocompatível preservado para compatibilidade integral com testes legados.
     */
    public TerminationService(TerminationRepository terminationRepository,
                              EmployeeRepository employeeRepository,
                              VacationRepository vacationRepository,
                              CompanyLookup companyLookup,
                              EmploymentContractService contractService,
                              SalaryHistoryService salaryHistoryService,
                              PayrollDeductionService payrollDeductionService,
                              HrPolicyService hrPolicyService,
                              DocumentNumberService documentNumberService,
                              @Lazy FinanceService financeService,
                              AuditLogService auditLogService) {
        this(createCalculator(employeeRepository, contractService, salaryHistoryService,
                        vacationRepository, hrPolicyService, payrollDeductionService),
                createLifecycle(terminationRepository, employeeRepository, companyLookup,
                        contractService, documentNumberService, financeService, auditLogService,
                        createCalculator(employeeRepository, contractService, salaryHistoryService,
                                vacationRepository, hrPolicyService, payrollDeductionService)),
                new TerminationQueryService(terminationRepository));
    }

    private static TerminationSettlementCalculator createCalculator(EmployeeRepository employeeRepository,
                                                                    EmploymentContractService contractService,
                                                                    SalaryHistoryService salaryHistoryService,
                                                                    VacationRepository vacationRepository,
                                                                    HrPolicyService hrPolicyService,
                                                                    PayrollDeductionService payrollDeductionService) {
        return new TerminationSettlementCalculator(employeeRepository, contractService, salaryHistoryService,
                vacationRepository, hrPolicyService, payrollDeductionService);
    }

    private static TerminationLifecycleService createLifecycle(TerminationRepository terminationRepository,
                                                               EmployeeRepository employeeRepository,
                                                               CompanyLookup companyLookup,
                                                               EmploymentContractService contractService,
                                                               DocumentNumberService documentNumberService,
                                                               FinanceService financeService,
                                                               AuditLogService auditLogService,
                                                               TerminationSettlementCalculator calculator) {
        return new TerminationLifecycleService(terminationRepository, employeeRepository, companyLookup,
                contractService, documentNumberService, financeService, auditLogService, calculator);
    }

    @Transactional(readOnly = true)
    public TerminationDTO preview(CreateTerminationRequest request) {
        return calculator.preview(request);
    }

    @Transactional
    public TerminationDTO terminate(CreateTerminationRequest request) {
        return lifecycleService.terminate(request);
    }

    @Transactional
    public TerminationDTO paySettlement(Long id) {
        return lifecycleService.paySettlement(id);
    }

    @Transactional(readOnly = true)
    public List<TerminationDTO> list() {
        return queryService.list();
    }

    @Transactional(readOnly = true)
    public Termination loadForPrint(Long id) {
        return queryService.loadForPrint(id);
    }
}
