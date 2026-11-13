package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatus;
import mz.multicore.erp.modules.pos.service.MobilePaymentService;
import mz.multicore.erp.modules.pos.service.PosPaymentLookup;
import mz.multicore.erp.modules.subscription.dto.SelfServiceSubscriptionPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO;
import mz.multicore.erp.modules.subscription.model.PaymentMethod;
import mz.multicore.erp.modules.subscription.model.PlanType;
import mz.multicore.erp.modules.subscription.model.Subscription;
import mz.multicore.erp.modules.subscription.model.SubscriptionPayment;
import mz.multicore.erp.modules.subscription.model.SubscriptionStatus;
import mz.multicore.erp.modules.subscription.repository.SubscriptionPaymentRepository;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Renovação self-service de assinaturas com descontos progressivos e pagamentos móveis (M-Pesa / e-Mola).
 */
@Service
public class SubscriptionRenewalService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final AuditLogService auditLogService;
    private final SubscriptionQueryService queryService;
    private final SubscriptionLifecycleService lifecycleService;
    private final MobilePaymentService mobilePaymentService;
    private final PosPaymentLookup posPaymentLookup;

    public SubscriptionRenewalService(SubscriptionRepository subscriptionRepository,
                                      SubscriptionPaymentRepository paymentRepository,
                                      AuditLogService auditLogService,
                                      SubscriptionQueryService queryService,
                                      SubscriptionLifecycleService lifecycleService,
                                      MobilePaymentService mobilePaymentService,
                                      PosPaymentLookup posPaymentLookup) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
        this.mobilePaymentService = mobilePaymentService;
        this.posPaymentLookup = posPaymentLookup;
    }

    public List<SubscriptionPlanDetailDTO> listAvailablePlans() {
        return List.of(
                new SubscriptionPlanDetailDTO(
                        PlanType.BASIC.name(),
                        PlanType.BASIC.label(),
                        new BigDecimal("1500.00"),
                        "Ideal para pequenos retalhistas: 1 armazém, faturação, caixa POS e relatórios essenciais."
                ),
                new SubscriptionPlanDetailDTO(
                        PlanType.PRO.name(),
                        PlanType.PRO.label(),
                        new BigDecimal("3500.00"),
                        "Médias empresas: multi-armazém, multi-caixa POS com balança, compras e controlo de tesouraria."
                ),
                new SubscriptionPlanDetailDTO(
                        PlanType.ENTERPRISE.name(),
                        PlanType.ENTERPRISE.label(),
                        new BigDecimal("7500.00"),
                        "Operações completas: todos os módulos, RH & Salários (IRPS/INSS), contabilidade PGC-NIRF e auditoria."
                )
        );
    }

    public BigDecimal calculateRenewalPrice(String planStr, int months) {
        if (months <= 0) months = 1;
        PlanType plan = lifecycleService.parsePlan(planStr);
        BigDecimal monthlyPrice = switch (plan) {
            case BASIC -> new BigDecimal("1500.00");
            case PRO -> new BigDecimal("3500.00");
            case ENTERPRISE -> new BigDecimal("7500.00");
            case TRIAL -> BigDecimal.ZERO;
        };

        BigDecimal total = monthlyPrice.multiply(BigDecimal.valueOf(months));
        BigDecimal discountFactor = BigDecimal.ONE;
        if (months >= 12) {
            discountFactor = new BigDecimal("0.85");
        } else if (months >= 6) {
            discountFactor = new BigDecimal("0.90");
        } else if (months >= 3) {
            discountFactor = new BigDecimal("0.95");
        }
        return total.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public SubscriptionPaymentResultDTO initiateSelfServiceRenewal(SelfServiceSubscriptionPaymentRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Company company = queryService.requireCompany(companyId);
        int months = Math.max(1, request.months());
        PlanType plan = lifecycleService.parsePlan(request.plan());
        BigDecimal amount = calculateRenewalPrice(plan.name(), months);

        String methodStr = request.paymentMethod() != null ? request.paymentMethod().trim().toUpperCase(Locale.ROOT) : "MANUAL";
        boolean isMobile = "MPESA".equals(methodStr) || "EMOLA".equals(methodStr);

        if (isMobile) {
            if (mobilePaymentService == null) {
                throw new BusinessRuleException("O serviço de pagamento móvel não está configurado.");
            }
            MobilePaymentProvider provider = "EMOLA".equals(methodStr)
                    ? MobilePaymentProvider.EMOLA : MobilePaymentProvider.MPESA;

            var mobileReq = new InitiateMobilePaymentRequest(
                    provider,
                    request.phoneNumber(),
                    amount,
                    "SUB-" + companyId + "-" + System.currentTimeMillis(),
                    companyId,
                    CurrentUserContext.getUsername()
            );
            var mobileResp = mobilePaymentService.initiatePayment(mobileReq);

            return new SubscriptionPaymentResultDTO(
                    false,
                    "Solicitação USSD enviada ao cliente. Aguarde a confirmação do PIN...",
                    mobileResp.transactionId(),
                    lifecycleService.getMySubscription()
            );
        }

        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> lifecycleService.newSubscription(companyId));
        sub.setPlan(plan);
        sub.setMonthlyPrice(amount.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP));

        LocalDate baseDate = (sub.getValidUntil() != null && sub.getValidUntil().isAfter(LocalDate.now()))
                ? sub.getValidUntil() : LocalDate.now();
        LocalDate newValidUntil = baseDate.plusMonths(months);
        sub.setValidUntil(newValidUntil);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setCompanyId(companyId);
        payment.setAmount(amount);
        payment.setMethod(PaymentMethod.TRANSFERENCIA);
        payment.setPaidAt(LocalDate.now());
        payment.setPeriodStart(baseDate);
        payment.setPeriodEnd(newValidUntil);
        payment.setReference(request.reference());
        payment.setPaymentDetails("Renovação Self-Service (" + months + " meses)");
        payment.setNote(request.notes());
        payment.setCreatedBy(CurrentUserContext.getUsername());
        paymentRepository.save(payment);

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_RENEWAL",
                String.format("Plano '%s' renovado por %d mês(es) até %s para '%s'.",
                        plan.label(), months, newValidUntil, company.getName()));

        return new SubscriptionPaymentResultDTO(
                true,
                "Plano renovado com sucesso por " + months + " mês(es).",
                null,
                lifecycleService.getMySubscription()
        );
    }

    @Transactional
    public SubscriptionPaymentResultDTO confirmMobileRenewal(String transactionId) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Company company = queryService.requireCompany(companyId);

        if (mobilePaymentService == null || posPaymentLookup == null) {
            throw new BusinessRuleException("Serviço de pagamentos móveis indisponível.");
        }

        var statusResp = mobilePaymentService.checkStatus(transactionId, companyId);
        if (statusResp.status() == MobilePaymentStatus.SUCCESS) {
            var txOpt = posPaymentLookup.findMobilePaymentForCompany(transactionId, companyId);
            if (txOpt.isPresent()) {
                var tx = txOpt.get();
                boolean alreadyRecorded = paymentRepository.findByCompanyIdOrderByPaidAtDesc(companyId).stream()
                        .anyMatch(p -> tx.getFinancialReference() != null && tx.getFinancialReference().equals(p.getReference()));

                if (!alreadyRecorded) {
                    Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                            .orElseGet(() -> lifecycleService.newSubscription(companyId));

                    LocalDate baseDate = (sub.getValidUntil() != null && sub.getValidUntil().isAfter(LocalDate.now()))
                            ? sub.getValidUntil() : LocalDate.now();
                    LocalDate newValidUntil = baseDate.plusMonths(1);
                    sub.setValidUntil(newValidUntil);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    subscriptionRepository.save(sub);

                    SubscriptionPayment payment = new SubscriptionPayment();
                    payment.setCompanyId(companyId);
                    payment.setAmount(tx.getAmount());
                    payment.setMethod(tx.getProvider() == MobilePaymentProvider.EMOLA
                            ? PaymentMethod.EMOLA : PaymentMethod.MPESA);
                    payment.setPaidAt(LocalDate.now());
                    payment.setPeriodStart(baseDate);
                    payment.setPeriodEnd(newValidUntil);
                    payment.setReference(tx.getFinancialReference());
                    payment.setPaymentDetails("Pagamento Móvel Aprovado (" + tx.getPhoneNumber() + ")");
                    payment.setNote("Transação ID: " + tx.getTransactionId());
                    payment.setCreatedBy(CurrentUserContext.getUsername());
                    paymentRepository.save(payment);

                    auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_PAYMENT_MOBILE",
                            String.format("Pagamento móvel aprovado (%s MT) via %s. Assinatura ativa até %s.",
                                    tx.getAmount(), tx.getProvider().getDisplayName(), newValidUntil));
                }
            }
            return new SubscriptionPaymentResultDTO(
                    true,
                    "Pagamento móvel aprovado! Assinatura renovada com sucesso.",
                    transactionId,
                    lifecycleService.getMySubscription()
            );
        } else if (statusResp.status() == MobilePaymentStatus.PENDING) {
            return new SubscriptionPaymentResultDTO(
                    false,
                    "Aguardando autorização no telemóvel do cliente...",
                    transactionId,
                    lifecycleService.getMySubscription()
            );
        } else {
            return new SubscriptionPaymentResultDTO(
                    false,
                    "Falha no pagamento: " + statusResp.message(),
                    transactionId,
                    lifecycleService.getMySubscription()
            );
        }
    }
}
