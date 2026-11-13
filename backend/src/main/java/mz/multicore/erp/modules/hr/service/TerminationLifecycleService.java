package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SettlementStatus;
import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TerminationRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TerminationLifecycleService {
    private final TerminationRepository terminationRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final EmploymentContractService contractService;
    private final DocumentNumberService documentNumberService;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final TerminationSettlementCalculator calculator;

    public TerminationLifecycleService(TerminationRepository terminationRepository,
                                       EmployeeRepository employeeRepository,
                                       CompanyRepository companyRepository,
                                       EmploymentContractService contractService,
                                       DocumentNumberService documentNumberService,
                                       @Lazy FinanceService financeService,
                                       AuditLogService auditLogService,
                                       TerminationSettlementCalculator calculator) {
        this.terminationRepository = terminationRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.contractService = contractService;
        this.documentNumberService = documentNumberService;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.calculator = calculator;
    }

    @Transactional
    public TerminationDTO terminate(CreateTerminationRequest request) {
        PermissionGuard.requireManagerOrAdmin("cessar vínculos e pagar acertos finais");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Employee employee = employeeRepository.findByIdAndCompanyId(request.employeeId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
        if (terminationRepository.existsByCompanyIdAndEmployeeId(companyId, employee.getId())) {
            throw new BusinessRuleException(String.format(
                    "%s já tem uma cessação registada. Um colaborador cessa-se uma vez.",
                    employee.getName()));
        }
        if (request.terminationDate().isBefore(employee.getHireDate())) {
            throw new BusinessRuleException("A saída não pode ser anterior à admissão.");
        }

        TerminationSettlementCalculator.Settlement settlement = calculator.compute(employee, request);

        Termination termination = new Termination();
        termination.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        termination.setEmployee(employee);
        termination.setContract(settlement.contract());
        termination.setSettlementNumber(documentNumberService.next(DocumentSeries.TERMINATION));
        termination.setTerminationDate(request.terminationDate());
        termination.setReason(settlement.reason());
        termination.setNoticeServed(request.noticeServed());
        termination.setNotes(TerminationQueryService.blankToNull(request.notes()));
        settlement.lines().forEach(termination::addLine);
        termination.recalculateTotals();

        Termination saved = terminationRepository.save(termination);

        if (settlement.contract() != null) {
            contractService.terminateContract(settlement.contract().getId(), request.terminationDate(),
                    settlement.reason().getLabel());
        }
        employee.setStatus("TERMINATED");
        employee.setContractEndDate(request.terminationDate());
        employeeRepository.save(employee);

        auditLogService.logCurrent("TERMINATION_CREATE", String.format(
                "Cessação de %s em %s (%s): acerto %s, ganhos %s, descontos %s, líquido %s",
                employee.getName(), request.terminationDate(), settlement.reason().getLabel(),
                saved.getSettlementNumber(), saved.getTotalEarnings(), saved.getTotalDeductions(),
                saved.getNetAmount()));
        return TerminationQueryService.toDTO(saved, settlement.warnings());
    }

    @Transactional
    public TerminationDTO paySettlement(Long id) {
        PermissionGuard.requireManagerOrAdmin("cessar vínculos e pagar acertos finais");
        Termination termination = terminationRepository.findByIdWithLines(id, CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Acerto final não encontrado."));
        if (termination.getStatus() == SettlementStatus.PAGO) {
            throw new BusinessRuleException(String.format(
                    "O acerto %s já foi pago a %s.",
                    termination.getSettlementNumber(), termination.getPaymentDate()));
        }
        if (termination.getNetAmount().signum() <= 0) {
            throw new BusinessRuleException(String.format(
                    "O acerto %s não tem valor a pagar (líquido %s MT). %s continua a dever este valor "
                            + "à empresa — cobre-o fora da folha.",
                    termination.getSettlementNumber(), DisplayNumbers.money(termination.getNetAmount()),
                    termination.getEmployee().getName()));
        }

        termination.setStatus(SettlementStatus.PAGO);
        termination.setPaymentDate(LocalDate.now());
        Termination saved = terminationRepository.save(termination);

        financeService.registerAutoPayout(saved.getNetAmount(), String.format(
                "Acerto final %s - %s", saved.getSettlementNumber(), saved.getEmployee().getName()), mz.multicore.erp.architecture.events.PayoutPurpose.STAFF_COST);
        auditLogService.logCurrent("TERMINATION_PAID", String.format(
                "Acerto final %s pago a %s, líquido %s",
                saved.getSettlementNumber(), saved.getEmployee().getName(), saved.getNetAmount()));
        return TerminationQueryService.toDTO(saved, List.of());
    }
}
