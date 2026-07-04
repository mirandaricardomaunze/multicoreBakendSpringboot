package com.phcpro.modules.subscription.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.subscription.dto.RecordPaymentRequest;
import com.phcpro.modules.subscription.dto.SaveSubscriptionRequest;
import com.phcpro.modules.subscription.dto.SubscriptionDTO;
import com.phcpro.modules.subscription.dto.SubscriptionPaymentDTO;
import com.phcpro.modules.subscription.model.PaymentMethod;
import com.phcpro.modules.subscription.model.PlanType;
import com.phcpro.modules.subscription.model.Subscription;
import com.phcpro.modules.subscription.model.SubscriptionPayment;
import com.phcpro.modules.subscription.model.SubscriptionStatus;
import com.phcpro.modules.subscription.repository.SubscriptionPaymentRepository;
import com.phcpro.modules.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Assinaturas e pagamentos ao nível da plataforma (superadmin). Gestão manual: define plano/preço/
 * validade, muda estado (suspender/reactivar) e regista pagamentos que estendem a validade. Uma
 * assinatura expirada ou suspensa bloqueia o login da empresa ({@link #allowsLogin}).
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPaymentRepository paymentRepository,
                               CompanyRepository companyRepository,
                               AuditLogService auditLogService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDTO> listOverview() {
        PermissionGuard.requireSuperAdmin("listar assinaturas");
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> toDto(c, subscriptionRepository.findByCompanyId(c.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPaymentDTO> listPayments(Long companyId) {
        PermissionGuard.requireSuperAdmin("consultar pagamentos");
        return paymentRepository.findByCompanyIdOrderByPaidAtDesc(companyId).stream()
                .map(this::toPaymentDto)
                .toList();
    }

    @Transactional
    public SubscriptionDTO saveSubscription(Long companyId, SaveSubscriptionRequest request) {
        PermissionGuard.requireSuperAdmin("definir a assinatura");
        Company company = requireCompany(companyId);
        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> newSubscription(companyId));
        sub.setPlan(parsePlan(request.plan()));
        sub.setMonthlyPrice(request.monthlyPrice() == null ? BigDecimal.ZERO : request.monthlyPrice());
        sub.setValidUntil(request.validUntil());
        // Estado deriva da validade (SUSPENDED só via changeStatus): válida no futuro ⇒ ACTIVE.
        sub.setStatus(request.validUntil() != null && request.validUntil().isBefore(LocalDate.now())
                ? SubscriptionStatus.EXPIRED : SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_UPDATE",
                String.format("Assinatura de '%s': plano %s, válida até %s.",
                        company.getName(), sub.getPlan().label(), sub.getValidUntil()));
        return toDto(company, sub);
    }

    @Transactional
    public SubscriptionDTO changeStatus(Long companyId, String status) {
        PermissionGuard.requireSuperAdmin("mudar o estado da assinatura");
        Company company = requireCompany(companyId);
        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessRuleException("A empresa não tem assinatura definida."));
        sub.setStatus(parseStatus(status));
        subscriptionRepository.save(sub);
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_STATUS",
                String.format("Assinatura de '%s' passou a %s.", company.getName(), sub.getStatus().label()));
        return toDto(company, sub);
    }

    @Transactional
    public SubscriptionPaymentDTO recordPayment(Long companyId, RecordPaymentRequest request) {
        PermissionGuard.requireSuperAdmin("registar um pagamento");
        Company company = requireCompany(companyId);
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessRuleException("O valor do pagamento deve ser positivo.");
        }

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setCompanyId(companyId);
        payment.setAmount(request.amount());
        payment.setMethod(parseMethod(request.method()));
        payment.setPaidAt(request.paidAt() == null ? LocalDate.now() : request.paidAt());
        payment.setPeriodStart(request.periodStart());
        payment.setPeriodEnd(request.periodEnd());
        payment.setNote(request.note());
        payment.setCreatedBy(CurrentUserContext.getUsername());
        paymentRepository.save(payment);

        // O pagamento reactiva a assinatura e estende a validade até ao fim do período coberto.
        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> newSubscription(companyId));
        if (payment.getPeriodEnd() != null
                && (sub.getValidUntil() == null || payment.getPeriodEnd().isAfter(sub.getValidUntil()))) {
            sub.setValidUntil(payment.getPeriodEnd());
        }
        sub.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_PAYMENT",
                String.format("Pagamento de %s (%s) para '%s'.",
                        payment.getAmount(), payment.getMethod().label(), company.getName()));
        return toPaymentDto(payment);
    }

    /**
     * Política de login (uso interno, sem guard): a empresa é bloqueada se tiver uma assinatura cujo
     * estado efectivo não permita login. Sem assinatura ⇒ permitido (retrocompatível).
     */
    @Transactional(readOnly = true)
    public boolean allowsLogin(Long companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .map(sub -> sub.effectiveStatus().allowsLogin())
                .orElse(true);
    }

    private Subscription newSubscription(Long companyId) {
        Subscription sub = new Subscription();
        sub.setCompanyId(companyId);
        sub.setStartDate(LocalDate.now());
        sub.setCreatedBy(CurrentUserContext.getUsername());
        return sub;
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    private SubscriptionDTO toDto(Company company, Subscription sub) {
        if (sub == null) {
            return new SubscriptionDTO(company.getId(), company.getName(), company.isActive(),
                    false, null, "—", null, "Sem assinatura", null, null, null,
                    paymentRepository.countByCompanyId(company.getId()));
        }
        SubscriptionStatus effective = sub.effectiveStatus();
        return new SubscriptionDTO(company.getId(), company.getName(), company.isActive(), true,
                sub.getPlan().name(), sub.getPlan().label(), effective.name(), effective.label(),
                sub.getStartDate(), sub.getValidUntil(), sub.getMonthlyPrice(),
                paymentRepository.countByCompanyId(company.getId()));
    }

    private SubscriptionPaymentDTO toPaymentDto(SubscriptionPayment p) {
        return new SubscriptionPaymentDTO(p.getId(), p.getAmount(), p.getMethod().name(),
                p.getMethod().label(), p.getPaidAt(), p.getPeriodStart(), p.getPeriodEnd(), p.getNote());
    }

    private PlanType parsePlan(String value) {
        try {
            return PlanType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Plano inválido.");
        }
    }

    private SubscriptionStatus parseStatus(String value) {
        try {
            return SubscriptionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Estado de assinatura inválido.");
        }
    }

    private PaymentMethod parseMethod(String value) {
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Método de pagamento inválido.");
        }
    }

    // Exposto para a UI popular os selectores sem duplicar os enums.
    public List<String> planOptions() {
        return java.util.Arrays.stream(PlanType.values()).map(Enum::name).toList();
    }

    public List<String> methodOptions() {
        return java.util.Arrays.stream(PaymentMethod.values()).map(Enum::name).toList();
    }

    public Optional<SubscriptionDTO> findByCompany(Long companyId) {
        PermissionGuard.requireSuperAdmin("consultar a assinatura");
        Company company = requireCompany(companyId);
        return Optional.of(toDto(company, subscriptionRepository.findByCompanyId(companyId).orElse(null)));
    }
}
