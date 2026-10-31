package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.events.PayslipPaidEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HrPayslipLifecycleService {

    private final PayslipRepository payslipRepository;
    private final HrAccessGuard guard;
    private final AuditLogService auditLogService;
    private final FinanceService financeService;
    private final PayrollLiabilityService payrollLiabilityService;
    private final PayrollDeductionService payrollDeductionService;
    private final ApplicationEventPublisher eventPublisher;

    public HrPayslipLifecycleService(
            PayslipRepository payslipRepository,
            HrAccessGuard guard,
            AuditLogService auditLogService,
            @Lazy FinanceService financeService,
            @Lazy PayrollLiabilityService payrollLiabilityService,
            @Lazy PayrollDeductionService payrollDeductionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.payslipRepository = payslipRepository;
        this.guard = guard;
        this.auditLogService = auditLogService;
        this.financeService = financeService;
        this.payrollLiabilityService = payrollLiabilityService;
        this.payrollDeductionService = payrollDeductionService;
        this.eventPublisher = eventPublisher;
    }

    public PayslipDTO approvePayslip(Long id) {
        guard.ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, guard.currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BusinessRuleException("Apenas recibos em rascunho podem ser aprovados.");
        }
        p.setStatus("APPROVED");
        Payslip saved = payslipRepository.save(p);
        auditLogService.logCurrent("PAYSLIP_APPROVE", String.format(
                "Recibo %s de %s aprovado por %s, líquido %s",
                saved.getPayslipNumber(), saved.getEmployee().getName(),
                CurrentUserContext.getUsername(), saved.getNetPay()));
        return HrPayslipIssuanceService.payslipToDTO(saved);
    }

    public PayslipDTO markPayslipPaid(Long id) {
        guard.ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, guard.currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if (!"APPROVED".equals(p.getStatus())) {
            throw new BusinessRuleException(
                    "Só se paga um recibo aprovado. Aprove-o primeiro — é a segunda vista sobre os "
                            + "números antes de o dinheiro sair.");
        }
        p.setStatus("PAID");
        p.setPaymentDate(LocalDate.now());
        Payslip saved = payslipRepository.save(p);

        financeService.registerAutoPayout(saved.getNetPay(), String.format(
                "Pagamento Salário %s - %s", saved.getPayslipNumber(), saved.getEmployee().getName()));

        payrollLiabilityService.accrueForPeriod(saved.getYear(), saved.getMonth());

        eventPublisher.publishEvent(new PayslipPaidEvent(
                guard.currentCompanyId(), saved.getId(), saved.getPayslipNumber(),
                saved.getEmployee().getName(), saved.getPaymentDate(),
                saved.getBaseSalary().add(saved.getAllowances()).add(saved.getOvertime()).add(HrPayslipIssuanceService.orZero(saved.getSalesBonus())),
                saved.getAbsenceDeduction(), saved.getIrpsDeduction(), saved.getInssDeduction(),
                saved.getEmployerInss(), saved.getOtherDeductions(), saved.getNetPay()));

        auditLogService.logCurrent("PAYSLIP_PAID", String.format(
                "Recibo %s pago a %s, líquido %s",
                saved.getPayslipNumber(), saved.getEmployee().getName(), saved.getNetPay()));
        return HrPayslipIssuanceService.payslipToDTO(saved);
    }

    public PayslipDTO cancelPayslip(Long id) {
        guard.ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, guard.currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if ("PAID".equals(p.getStatus())) {
            throw new BusinessRuleException("Não é possível cancelar um recibo já pago.");
        }
        p.setStatus("CANCELLED");
        Payslip saved = payslipRepository.save(p);
        payrollDeductionService.releaseFromPayslip(saved.getId());
        auditLogService.logCurrent("PAYSLIP_CANCEL", String.format(
                "Recibo %s cancelado (%s)", saved.getPayslipNumber(), saved.getEmployee().getName()));
        return HrPayslipIssuanceService.payslipToDTO(saved);
    }

    public List<PayslipDTO> getAllPayslips() {
        List<Payslip> payslips = payslipRepository.findAllWithEmployeeByCompanyId(guard.currentCompanyId());
        if (!guard.isHrManager()) {
            Long selfId = guard.findSelfEmployee().map(Employee::getId).orElse(null);
            payslips = payslips.stream()
                    .filter(p -> selfId != null && selfId.equals(p.getEmployee().getId()))
                    .toList();
        }
        return payslips.stream().map(HrPayslipIssuanceService::payslipToDTO).collect(Collectors.toList());
    }

    public Payslip loadPayslipForPrint(Long id) {
        Payslip payslip = payslipRepository.findByIdWithEmployeeAndCompanyId(id, guard.currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        guard.ensureCanActFor(payslip.getEmployee());
        return payslip;
    }
}
