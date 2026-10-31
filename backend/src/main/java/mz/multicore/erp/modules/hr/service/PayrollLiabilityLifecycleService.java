package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.events.PayrollLiabilityDeliveredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.model.PayrollLiability;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityStatus;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityType;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayrollLiabilityRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Apuramento idempotente e entrega ao Estado das retenções da folha salarial. */
@Component
public class PayrollLiabilityLifecycleService {

    private final PayrollLiabilityRepository liabilityRepository;
    private final PayslipRepository payslipRepository;
    private final CompanyRepository companyRepository;
    private final HrPolicyService hrPolicyService;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public PayrollLiabilityLifecycleService(PayrollLiabilityRepository liabilityRepository,
                                            PayslipRepository payslipRepository,
                                            CompanyRepository companyRepository,
                                            HrPolicyService hrPolicyService,
                                            @Lazy FinanceService financeService,
                                            AuditLogService auditLogService,
                                            ApplicationEventPublisher eventPublisher) {
        this.liabilityRepository = liabilityRepository;
        this.payslipRepository = payslipRepository;
        this.companyRepository = companyRepository;
        this.hrPolicyService = hrPolicyService;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    public List<PayrollLiabilityDTO> accrueForPeriod(Long companyId, int year, int month) {
        List<Payslip> paid = payslipRepository.findByCompanyIdAndYearAndMonth(companyId, year, month)
                .stream().filter(p -> "PAID".equals(p.getStatus())).toList();

        List<PayrollLiabilityDTO> result = new ArrayList<>();
        result.add(upsert(companyId, year, month, PayrollLiabilityType.IRPS, sum(paid, Payslip::getIrpsDeduction)));
        result.add(upsert(companyId, year, month, PayrollLiabilityType.INSS_TRABALHADOR, sum(paid, Payslip::getInssDeduction)));
        result.add(upsert(companyId, year, month, PayrollLiabilityType.INSS_PATRONAL, sum(paid, Payslip::getEmployerInss)));
        return result;
    }

    private PayrollLiabilityDTO upsert(Long companyId, int year, int month,
                                       PayrollLiabilityType type, BigDecimal amount) {
        Optional<PayrollLiability> existing = liabilityRepository
                .findByCompanyIdAndYearAndMonthAndLiabilityType(companyId, year, month, type);

        if (existing.isPresent() && !existing.get().isPending()) {
            PayrollLiability delivered = existing.get();
            if (delivered.getAmount().compareTo(amount) != 0) {
                throw new BusinessRuleException(String.format(
                        "O %s de %d/%d já foi entregue ao Estado (%s). Um recibo novo neste período "
                                + "mudaria um valor já declarado — emita-o no período corrente ou "
                                + "corrija a entrega antes de continuar.",
                        type.getLabel(), month, year, delivered.getAmount()));
            }
            return PayrollLiabilityQueryService.toDTO(delivered);
        }

        PayrollLiability liability = existing.orElseGet(() -> {
            PayrollLiability fresh = new PayrollLiability();
            fresh.setCompany(companyRepository.findById(companyId)
                    .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
            fresh.setYear(year);
            fresh.setMonth(month);
            fresh.setLiabilityType(type);
            fresh.setStatus(PayrollLiabilityStatus.POR_ENTREGAR);
            return fresh;
        });
        liability.setAmount(amount);
        liability.setDueDate(hrPolicyService.deliveryDeadline(type, year, month).orElse(null));
        return PayrollLiabilityQueryService.toDTO(liabilityRepository.save(liability));
    }

    public PayrollLiabilityDTO markDelivered(Long companyId, Long id, String paymentReference) {
        PermissionGuard.requireManagerOrAdmin("entregar retenções da folha");
        PayrollLiability liability = liabilityRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Retenção não encontrada."));
        if (!liability.isPending()) {
            throw new BusinessRuleException(String.format(
                    "O %s de %d/%d já foi entregue a %s%s.",
                    liability.getLiabilityType().getLabel(), liability.getMonth(), liability.getYear(),
                    liability.getPaymentDate(),
                    liability.getPaymentReference() == null ? "" : " (ref. " + liability.getPaymentReference() + ")"));
        }
        if (liability.getAmount().signum() <= 0) {
            throw new BusinessRuleException("Não há valor a entregar nesta retenção.");
        }

        liability.setStatus(PayrollLiabilityStatus.ENTREGUE);
        liability.setPaymentDate(LocalDate.now());
        liability.setPaymentReference(blankToNull(paymentReference));
        liability.setDeliveredBy(CurrentUserContext.getUsername());
        PayrollLiability saved = liabilityRepository.save(liability);

        String description = String.format("Entrega %s %d/%d",
                saved.getLiabilityType().getLabel(), saved.getMonth(), saved.getYear());
        financeService.registerAutoPayout(saved.getAmount(), description);
        eventPublisher.publishEvent(new PayrollLiabilityDeliveredEvent(
                companyId, saved.getId(), saved.getLiabilityType().name(),
                saved.getYear(), saved.getMonth(), saved.getPaymentDate(), saved.getAmount(),
                saved.getPaymentReference()));
        auditLogService.logCurrent("PAYROLL_LIABILITY_DELIVERED", String.format(
                "%s de %d/%d entregue: %s%s",
                saved.getLiabilityType().getLabel(), saved.getMonth(), saved.getYear(),
                saved.getAmount(),
                saved.getPaymentReference() == null ? "" : " (ref. " + saved.getPaymentReference() + ")"));
        return PayrollLiabilityQueryService.toDTO(saved);
    }

    private static BigDecimal sum(List<Payslip> payslips, Function<Payslip, BigDecimal> field) {
        return payslips.stream()
                .map(p -> field.apply(p) == null ? BigDecimal.ZERO : field.apply(p))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
