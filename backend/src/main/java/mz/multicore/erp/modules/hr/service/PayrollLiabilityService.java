package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.repository.PayrollLiabilityRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestrador tenant-scoped das retenções da folha salarial (IRPS e INSS).
 * Ver docs/RH_COMPLETO_SPEC.md §B5.
 */
@Service
public class PayrollLiabilityService {

    /** Antecedência com que um prazo de entrega passa a ser avisado. */
    public static final int DUE_ALERT_DAYS = 7;

    private final PayrollLiabilityLifecycleService lifecycleService;
    private final PayrollLiabilityQueryService queryService;

    @Autowired
    public PayrollLiabilityService(PayrollLiabilityLifecycleService lifecycleService,
                                   PayrollLiabilityQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public PayrollLiabilityService(PayrollLiabilityRepository liabilityRepository,
                                   PayslipRepository payslipRepository,
                                   CompanyLookup companyLookup,
                                   HrPolicyService hrPolicyService,
                                   @Lazy FinanceService financeService,
                                   AuditLogService auditLogService,
                                   ApplicationEventPublisher eventPublisher) {
        this(new PayrollLiabilityLifecycleService(liabilityRepository, payslipRepository, companyLookup,
                        hrPolicyService, financeService, auditLogService, eventPublisher),
                new PayrollLiabilityQueryService(liabilityRepository, payslipRepository));
    }

    @Transactional
    public List<PayrollLiabilityDTO> accrueForPeriod(int year, int month) {
        return lifecycleService.accrueForPeriod(CurrentUserContext.requireCurrentCompanyId(), year, month);
    }

    @Transactional
    public PayrollLiabilityDTO markDelivered(Long id, String paymentReference) {
        return lifecycleService.markDelivered(CurrentUserContext.requireCurrentCompanyId(), id, paymentReference);
    }

    @Transactional(readOnly = true)
    public List<PayrollLiabilityDTO> list() {
        return queryService.list(CurrentUserContext.requireCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public List<PayrollLiabilityDTO> dueAlerts() {
        return queryService.dueAlerts(CurrentUserContext.requireCurrentCompanyId(), DUE_ALERT_DAYS);
    }

    @Transactional(readOnly = true)
    public PayrollCostDTO monthlyCost(int year, int month) {
        return queryService.monthlyCost(CurrentUserContext.requireCurrentCompanyId(), year, month);
    }
}
