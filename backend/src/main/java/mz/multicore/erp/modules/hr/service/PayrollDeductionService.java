package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.model.*;
import mz.multicore.erp.modules.hr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Fachada para compromissos salariais, aplicação em recibos e consultas de saldo. */
@Service
public class PayrollDeductionService {
    private final PayrollDeductionRepository deductionRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final PayrollDeductionApplicationService applicationService;
    private final PayrollDeductionQueryService queryService;

    @Autowired
    public PayrollDeductionService(PayrollDeductionRepository deductionRepository,
                                   EmployeeRepository employeeRepository,
                                   CompanyRepository companyRepository,
                                   @Lazy FinanceService financeService,
                                   AuditLogService auditLogService,
                                   PayrollDeductionApplicationService applicationService,
                                   PayrollDeductionQueryService queryService) {
        this.deductionRepository = deductionRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.applicationService = applicationService;
        this.queryService = queryService;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public PayrollDeductionService(PayrollDeductionRepository deductionRepository,
                                   PayslipDeductionLineRepository lineRepository,
                                   EmployeeRepository employeeRepository,
                                   CompanyRepository companyRepository,
                                   @Lazy FinanceService financeService,
                                   AuditLogService auditLogService) {
        this.deductionRepository = deductionRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.applicationService = new PayrollDeductionApplicationService(deductionRepository, lineRepository);
        this.queryService = new PayrollDeductionQueryService(deductionRepository, applicationService);
    }

    @Transactional
    public PayrollDeductionDTO create(CreatePayrollDeductionRequest request) {
        PermissionGuard.requireManagerOrAdmin("gerir descontos e adiantamentos");
        Employee employee = employeeRepository.findByIdAndCompanyId(request.employeeId(), companyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
        PayrollDeductionKind kind = parseKind(request.kind());
        PayrollDeductionPolicy.validate(kind, request);
        PayrollDeduction deduction = new PayrollDeduction();
        deduction.setCompany(companyRepository.findById(companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        deduction.setEmployee(employee);
        deduction.setKind(kind);
        deduction.setDescription(request.description().trim());
        deduction.setPrincipalAmount(PayrollDeductionPolicy.principal(kind, request));
        deduction.setInstallments(PayrollDeductionPolicy.installments(kind, request));
        deduction.setInstallmentAmount(PayrollDeductionPolicy.installmentAmount(kind, request));
        deduction.setStartDate(request.startDate());
        deduction.setEndDate(request.endDate());
        deduction.setActive(true);
        deduction.setNotes(blankToNull(request.notes()));
        PayrollDeduction saved = deductionRepository.save(deduction);
        if (kind.paysOutOnCreation()) {
            financeService.registerAutoPayout(saved.getPrincipalAmount(), String.format(
                    "%s a %s - %s", kind.getLabel(), employee.getName(), saved.getDescription()));
            saved.setPaidOut(true);
            deductionRepository.save(saved);
        }
        auditLogService.logCurrent("PAYROLL_DEDUCTION_CREATE", String.format(
                "%s de %s para %s: %s, %s prestação(ões) de %s a partir de %s",
                kind.getLabel(), saved.getPrincipalAmount() == null ? "valor variável" : saved.getPrincipalAmount(),
                employee.getName(), saved.getDescription(), saved.getInstallments() == null
                        ? "sem limite de" : saved.getInstallments(), saved.getInstallmentAmount(), saved.getStartDate()));
        return queryService.toDTO(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        PermissionGuard.requireManagerOrAdmin("gerir descontos e adiantamentos");
        PayrollDeduction deduction = deductionRepository.findByIdAndCompanyId(id, companyId())
                .orElseThrow(() -> new BusinessRuleException("Desconto não encontrado."));
        deduction.setActive(false);
        deductionRepository.save(deduction);
        auditLogService.logCurrent("PAYROLL_DEDUCTION_DEACTIVATE", String.format(
                "Desconto \"%s\" de %s desactivado com %s por liquidar", deduction.getDescription(),
                deduction.getEmployee().getName(), deduction.outstanding(applicationService.applied(deduction))));
    }

    public BigDecimal applyTo(Payslip payslip, BigDecimal availableAmount) {
        return applicationService.applyTo(payslip, availableAmount);
    }
    public void releaseFromPayslip(Long payslipId) { applicationService.releaseFromPayslip(payslipId); }
    public List<PayslipDeductionLineDTO> linesOf(Long payslipId) { return applicationService.linesOf(payslipId); }
    public List<PayrollDeductionDTO> list() { return queryService.list(); }
    public List<PayrollDeductionDTO> listForEmployee(Long employeeId) { return queryService.listForEmployee(employeeId); }
    public List<PayrollDeductionDTO> outstandingFor(Long employeeId) { return queryService.outstandingFor(employeeId); }

    private PayrollDeductionKind parseKind(String raw) {
        try { return PayrollDeductionKind.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new BusinessRuleException("Tipo de desconto desconhecido: " + raw); }
    }
    private Long companyId() { return CurrentUserContext.requireCurrentCompanyId(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
