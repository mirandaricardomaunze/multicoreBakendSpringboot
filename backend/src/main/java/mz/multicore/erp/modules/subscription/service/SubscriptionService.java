package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO;
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
    private final mz.multicore.erp.modules.pos.service.MobilePaymentService mobilePaymentService;
    private final mz.multicore.erp.modules.pos.repository.MobilePaymentRepository mobilePaymentRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPaymentRepository paymentRepository,
                               CompanyRepository companyRepository,
                               AuditLogService auditLogService) {
        this(subscriptionRepository, paymentRepository, companyRepository, auditLogService, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPaymentRepository paymentRepository,
                               CompanyRepository companyRepository,
                               AuditLogService auditLogService,
                               mz.multicore.erp.modules.pos.service.MobilePaymentService mobilePaymentService,
                               mz.multicore.erp.modules.pos.repository.MobilePaymentRepository mobilePaymentRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
        this.mobilePaymentService = mobilePaymentService;
        this.mobilePaymentRepository = mobilePaymentRepository;
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
        payment.setReference(request.reference() != null && !request.reference().isBlank() ? request.reference().trim() : null);
        payment.setPaymentDetails(request.paymentDetails() != null && !request.paymentDetails().isBlank() ? request.paymentDetails().trim() : null);
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

        String refPart = payment.getReference() != null ? " [Ref: " + payment.getReference() + "]" : "";
        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "SUBSCRIPTION_PAYMENT",
                String.format("Pagamento de %s (%s%s) para '%s'.",
                        payment.getAmount(), payment.getMethod().label(), refPart, company.getName()));
        return toPaymentDto(payment);
    }

    /**
     * Resumo da assinatura da empresa activa, para o próprio assinante ver (só-leitura, tenant-scoped
     * — não requer superadmin). Inclui os dias que faltam até expirar.
     */
    @Transactional(readOnly = true)
    public mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO getMySubscription() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Company company = requireCompany(companyId);
        Subscription sub = subscriptionRepository.findByCompanyId(companyId).orElse(null);
        if (sub == null) {
            return new mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO(
                    company.getName(), false, null, "—", null, "Sem assinatura", null, null, null, null);
        }
        SubscriptionStatus effective = sub.effectiveStatus();
        Long days = sub.getValidUntil() == null ? null
                : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), sub.getValidUntil());
        return new mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO(
                company.getName(), true, sub.getPlan().name(), sub.getPlan().label(),
                effective.name(), effective.label(), sub.getStartDate(), sub.getValidUntil(),
                days, sub.getMonthlyPrice());
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
                p.getMethod().label(), p.getPaidAt(), p.getPeriodStart(), p.getPeriodEnd(),
                p.getReference(), p.getPaymentDetails(), p.getNote());
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

    /**
     * Lista os planos comerciais disponíveis para os assinantes escolherem na renovação/ativação.
     */
    public List<mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO> listAvailablePlans() {
        return List.of(
                new mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO(
                        PlanType.BASIC.name(),
                        PlanType.BASIC.label(),
                        new BigDecimal("1500.00"),
                        "Ideal para pequenos retalhistas: 1 armazém, faturação, caixa POS e relatórios essenciais."
                ),
                new mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO(
                        PlanType.PRO.name(),
                        PlanType.PRO.label(),
                        new BigDecimal("3500.00"),
                        "Médias empresas: multi-armazém, multi-caixa POS com balança, compras e controlo de tesouraria."
                ),
                new mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO(
                        PlanType.ENTERPRISE.name(),
                        PlanType.ENTERPRISE.label(),
                        new BigDecimal("7500.00"),
                        "Operações completas: todos os módulos, RH & Salários (IRPS/INSS), contabilidade PGC-NIRF e auditoria."
                )
        );
    }

    /**
     * Calcula o preço com descontos progressivos por período (3m: 5%, 6m: 10%, 12m: 15%).
     */
    public BigDecimal calculateRenewalPrice(String planStr, int months) {
        if (months <= 0) months = 1;
        PlanType plan = parsePlan(planStr);
        BigDecimal monthlyPrice = switch (plan) {
            case BASIC -> new BigDecimal("1500.00");
            case PRO -> new BigDecimal("3500.00");
            case ENTERPRISE -> new BigDecimal("7500.00");
            case TRIAL -> BigDecimal.ZERO;
        };

        BigDecimal total = monthlyPrice.multiply(BigDecimal.valueOf(months));
        BigDecimal discountFactor = BigDecimal.ONE;
        if (months >= 12) {
            discountFactor = new BigDecimal("0.85"); // 15% desc
        } else if (months >= 6) {
            discountFactor = new BigDecimal("0.90"); // 10% desc
        } else if (months >= 3) {
            discountFactor = new BigDecimal("0.95"); // 5% desc
        }
        return total.multiply(discountFactor).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Inicia a renovação ou ativação self-service (via Push USSD M-Pesa / e-Mola ou transferência manual).
     */
    @Transactional
    public mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO initiateSelfServiceRenewal(
            mz.multicore.erp.modules.subscription.dto.SelfServiceSubscriptionPaymentRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Company company = requireCompany(companyId);
        int months = Math.max(1, request.months());
        PlanType plan = parsePlan(request.plan());
        BigDecimal amount = calculateRenewalPrice(plan.name(), months);

        String methodStr = request.paymentMethod() != null ? request.paymentMethod().trim().toUpperCase(Locale.ROOT) : "MANUAL";
        boolean isMobile = "MPESA".equals(methodStr) || "EMOLA".equals(methodStr);

        if (isMobile) {
            if (mobilePaymentService == null) {
                throw new BusinessRuleException("O serviço de pagamento móvel não está configurado.");
            }
            mz.multicore.erp.modules.pos.dto.MobilePaymentProvider provider = "EMOLA".equals(methodStr)
                    ? mz.multicore.erp.modules.pos.dto.MobilePaymentProvider.EMOLA
                    : mz.multicore.erp.modules.pos.dto.MobilePaymentProvider.MPESA;

            var mobileReq = new mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest(
                    provider,
                    request.phoneNumber(),
                    amount,
                    "SUB-" + companyId + "-" + System.currentTimeMillis(),
                    companyId,
                    CurrentUserContext.getUsername()
            );
            var mobileResp = mobilePaymentService.initiatePayment(mobileReq);

            return new mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO(
                    false,
                    "Solicitação USSD enviada ao cliente. Aguarde a confirmação do PIN...",
                    mobileResp.transactionId(),
                    getMySubscription()
            );
        }

        // Ativação / Pagamento por Transferência Bancária / Manual
        Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> newSubscription(companyId));
        sub.setPlan(plan);
        sub.setMonthlyPrice(amount.divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP));

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

        return new mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO(
                true,
                "Plano renovado com sucesso por " + months + " mês(es).",
                null,
                getMySubscription()
        );
    }

    /**
     * Confirmação periódica de transação de pagamento móvel para ativação do plano.
     */
    @Transactional
    public mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO confirmMobileRenewal(String transactionId) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Company company = requireCompany(companyId);

        if (mobilePaymentService == null || mobilePaymentRepository == null) {
            throw new BusinessRuleException("Serviço de pagamentos móveis indisponível.");
        }

        var statusResp = mobilePaymentService.checkStatus(transactionId, companyId);
        if (statusResp.status() == mz.multicore.erp.modules.pos.dto.MobilePaymentStatus.SUCCESS) {
            var txOpt = mobilePaymentRepository.findByTransactionIdAndCompanyId(transactionId, companyId);
            if (txOpt.isPresent()) {
                var tx = txOpt.get();
                // Verifica se já não gerou payment
                boolean alreadyRecorded = paymentRepository.findByCompanyIdOrderByPaidAtDesc(companyId).stream()
                        .anyMatch(p -> tx.getFinancialReference() != null && tx.getFinancialReference().equals(p.getReference()));

                if (!alreadyRecorded) {
                    Subscription sub = subscriptionRepository.findByCompanyId(companyId)
                            .orElseGet(() -> newSubscription(companyId));

                    LocalDate baseDate = (sub.getValidUntil() != null && sub.getValidUntil().isAfter(LocalDate.now()))
                            ? sub.getValidUntil() : LocalDate.now();
                    LocalDate newValidUntil = baseDate.plusMonths(1); // default 1 mês se valor padrão
                    sub.setValidUntil(newValidUntil);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    subscriptionRepository.save(sub);

                    SubscriptionPayment payment = new SubscriptionPayment();
                    payment.setCompanyId(companyId);
                    payment.setAmount(tx.getAmount());
                    payment.setMethod(tx.getProvider() == mz.multicore.erp.modules.pos.dto.MobilePaymentProvider.EMOLA
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
            return new mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO(
                    true,
                    "Pagamento móvel aprovado! Assinatura renovada com sucesso.",
                    transactionId,
                    getMySubscription()
            );
        } else if (statusResp.status() == mz.multicore.erp.modules.pos.dto.MobilePaymentStatus.PENDING) {
            return new mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO(
                    false,
                    "Aguardando autorização no telemóvel do cliente...",
                    transactionId,
                    getMySubscription()
            );
        } else {
            return new mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO(
                    false,
                    "Falha no pagamento: " + statusResp.message(),
                    transactionId,
                    getMySubscription()
            );
        }
    }
}
