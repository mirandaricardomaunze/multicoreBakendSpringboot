package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;
import mz.multicore.erp.modules.subscription.model.PlanType;
import mz.multicore.erp.modules.subscription.model.Subscription;
import mz.multicore.erp.modules.subscription.model.SubscriptionStatus;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Gestão do ciclo de vida, vigência e regras de acesso de assinaturas de empresas.
 */
@Service
public class SubscriptionLifecycleService {

    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogService auditLogService;
    private final SubscriptionQueryService queryService;

    public SubscriptionLifecycleService(SubscriptionRepository subscriptionRepository,
                                        AuditLogService auditLogService,
                                        SubscriptionQueryService queryService) {
        this.subscriptionRepository = subscriptionRepository;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
    }

    @Transactional
    public SubscriptionDTO saveSubscription(Long companyId, SaveSubscriptionRequest request) {
        PermissionGuard.requireSuperAdmin("definir a assinatura");
        Company company = queryService.requireCompany(companyId);
        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> newSubscription(companyId));
        sub.setPlan(parsePlan(request.plan()));
        sub.setMonthlyPrice(request.monthlyPrice() == null ? BigDecimal.ZERO : request.monthlyPrice());
        sub.setValidUntil(request.validUntil());
        sub.setStatus(request.validUntil() != null && request.validUntil().isBefore(LocalDate.now())
                ? SubscriptionStatus.EXPIRED : SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_UPDATE",
                String.format("Assinatura de '%s': plano %s, válida até %s.",
                        company.getName(), sub.getPlan().label(), sub.getValidUntil()));
        return queryService.toDto(company, sub);
    }

    @Transactional
    public SubscriptionDTO changeStatus(Long companyId, String status) {
        PermissionGuard.requireSuperAdmin("mudar o estado da assinatura");
        Company company = queryService.requireCompany(companyId);
        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessRuleException("A empresa não tem assinatura definida."));
        sub.setStatus(parseStatus(status));
        subscriptionRepository.save(sub);

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_STATUS",
                String.format("Assinatura de '%s' passou a %s.", company.getName(), sub.getStatus().label()));
        return queryService.toDto(company, sub);
    }

    @Transactional(readOnly = true)
    public MySubscriptionDTO getMySubscription() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Company company = queryService.requireCompany(companyId);
        Subscription sub = subscriptionRepository.findByCompanyId(companyId).orElse(null);
        if (sub == null) {
            return new MySubscriptionDTO(
                    company.getName(), false, null, "—", null, "Sem assinatura", null, null, null, null);
        }
        SubscriptionStatus effective = sub.effectiveStatus();
        Long days = sub.getValidUntil() == null ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), sub.getValidUntil());
        return new MySubscriptionDTO(
                company.getName(), true, sub.getPlan().name(), sub.getPlan().label(),
                effective.name(), effective.label(), sub.getStartDate(), sub.getValidUntil(),
                days, sub.getMonthlyPrice());
    }

    @Transactional(readOnly = true)
    public boolean allowsLogin(Long companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .map(sub -> sub.effectiveStatus().allowsLogin())
                .orElse(true);
    }

    public Subscription newSubscription(Long companyId) {
        Subscription sub = new Subscription();
        sub.setCompanyId(companyId);
        sub.setStartDate(LocalDate.now());
        sub.setCreatedBy(CurrentUserContext.getUsername());
        return sub;
    }

    public PlanType parsePlan(String value) {
        try {
            return PlanType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Plano inválido.");
        }
    }

    public SubscriptionStatus parseStatus(String value) {
        try {
            return SubscriptionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Estado de assinatura inválido.");
        }
    }
}
