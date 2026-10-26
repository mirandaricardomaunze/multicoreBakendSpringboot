package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO;
import mz.multicore.erp.modules.subscription.model.PaymentMethod;
import mz.multicore.erp.modules.subscription.model.Subscription;
import mz.multicore.erp.modules.subscription.model.SubscriptionPayment;
import mz.multicore.erp.modules.subscription.model.SubscriptionStatus;
import mz.multicore.erp.modules.subscription.repository.SubscriptionPaymentRepository;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Registo administrativo de pagamentos e histórico de liquidações de assinaturas.
 */
@Service
public class SubscriptionPaymentService {

    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogService auditLogService;
    private final SubscriptionQueryService queryService;
    private final SubscriptionLifecycleService lifecycleService;

    public SubscriptionPaymentService(SubscriptionPaymentRepository paymentRepository,
                                      SubscriptionRepository subscriptionRepository,
                                      AuditLogService auditLogService,
                                      SubscriptionQueryService queryService,
                                      SubscriptionLifecycleService lifecycleService) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPaymentDTO> listPayments(Long companyId) {
        PermissionGuard.requireSuperAdmin("consultar pagamentos");
        return paymentRepository.findByCompanyIdOrderByPaidAtDesc(companyId).stream()
                .map(this::toPaymentDto)
                .toList();
    }

    @Transactional
    public SubscriptionPaymentDTO recordPayment(Long companyId, RecordPaymentRequest request) {
        PermissionGuard.requireSuperAdmin("registar um pagamento");
        Company company = queryService.requireCompany(companyId);
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
        payment.setReference(request.reference() != null && !request.reference().isBlank() ? request.reference().trim() : null);
        payment.setPaymentDetails(request.paymentDetails() != null && !request.paymentDetails().isBlank() ? request.paymentDetails().trim() : null);
        payment.setNote(request.note());
        payment.setCreatedBy(CurrentUserContext.getUsername());
        paymentRepository.save(payment);

        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> lifecycleService.newSubscription(companyId));
        if (payment.getPeriodEnd() != null
                && (sub.getValidUntil() == null || payment.getPeriodEnd().isAfter(sub.getValidUntil()))) {
            sub.setValidUntil(payment.getPeriodEnd());
        }
        sub.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);

        String refPart = payment.getReference() != null ? " [Ref: " + payment.getReference() + "]" : "";
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_PAYMENT",
                String.format("Pagamento de %s (%s%s) para '%s'.",
                        payment.getAmount(), payment.getMethod().label(), refPart, company.getName()));
        return toPaymentDto(payment);
    }

    public SubscriptionPaymentDTO toPaymentDto(SubscriptionPayment p) {
        return new SubscriptionPaymentDTO(p.getId(), p.getAmount(), p.getMethod().name(),
                p.getMethod().label(), p.getPaidAt(), p.getPeriodStart(), p.getPeriodEnd(),
                p.getReference(), p.getPaymentDetails(), p.getNote());
    }

    public PaymentMethod parseMethod(String value) {
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Método de pagamento inválido.");
        }
    }
}
