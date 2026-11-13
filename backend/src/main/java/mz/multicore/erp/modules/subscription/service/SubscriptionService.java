package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.pos.service.MobilePaymentService;
import mz.multicore.erp.modules.pos.service.PosPaymentLookup;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.dto.SelfServiceSubscriptionPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPlanDetailDTO;
import mz.multicore.erp.modules.subscription.repository.SubscriptionPaymentRepository;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Assinaturas e licenciamento ao nível da plataforma (superadmin e self-service).
 * Orquestrador desacoplado delegando para serviços especializados.
 */
@Service
public class SubscriptionService {

    private final SubscriptionLifecycleService lifecycleService;
    private final SubscriptionPaymentService paymentService;
    private final SubscriptionRenewalService renewalService;
    private final SubscriptionQueryService queryService;

    public SubscriptionService(SubscriptionLifecycleService lifecycleService,
                               SubscriptionPaymentService paymentService,
                               SubscriptionRenewalService renewalService,
                               SubscriptionQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.paymentService = paymentService;
        this.renewalService = renewalService;
        this.queryService = queryService;
    }

    /** Construtor legado de 4 parâmetros. */
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPaymentRepository paymentRepository,
                               CompanyLookup companyLookup,
                               AuditLogService auditLogService) {
        this(subscriptionRepository, paymentRepository, companyLookup, auditLogService, null, null);
    }

    /** Construtor legado de 6 parâmetros. */
    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPaymentRepository paymentRepository,
                               CompanyLookup companyLookup,
                               AuditLogService auditLogService,
                               MobilePaymentService mobilePaymentService,
                               PosPaymentLookup posPaymentLookup) {
        SubscriptionQueryService qService = new SubscriptionQueryService(
                subscriptionRepository, paymentRepository, companyLookup);
        SubscriptionLifecycleService lService = new SubscriptionLifecycleService(
                subscriptionRepository, auditLogService, qService);
        SubscriptionPaymentService pService = new SubscriptionPaymentService(
                paymentRepository, subscriptionRepository, auditLogService, qService, lService);
        SubscriptionRenewalService rService = new SubscriptionRenewalService(
                subscriptionRepository, paymentRepository, auditLogService, qService, lService,
                mobilePaymentService, posPaymentLookup);

        this.queryService = qService;
        this.lifecycleService = lService;
        this.paymentService = pService;
        this.renewalService = rService;
    }

    // ─── Visão Global e Consultas ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubscriptionDTO> listOverview() {
        return queryService.listOverview();
    }

    public Optional<SubscriptionDTO> findByCompany(Long companyId) {
        return queryService.findByCompany(companyId);
    }

    public List<String> planOptions() {
        return queryService.planOptions();
    }

    public List<String> methodOptions() {
        return queryService.methodOptions();
    }

    // ─── Ciclo de Vida e Licenciamento ────────────────────────────────────────

    @Transactional
    public SubscriptionDTO saveSubscription(Long companyId, SaveSubscriptionRequest request) {
        return lifecycleService.saveSubscription(companyId, request);
    }

    @Transactional
    public SubscriptionDTO changeStatus(Long companyId, String status) {
        return lifecycleService.changeStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public MySubscriptionDTO getMySubscription() {
        return lifecycleService.getMySubscription();
    }

    @Transactional(readOnly = true)
    public boolean allowsLogin(Long companyId) {
        return lifecycleService.allowsLogin(companyId);
    }

    // ─── Pagamentos Administrativos ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubscriptionPaymentDTO> listPayments(Long companyId) {
        return paymentService.listPayments(companyId);
    }

    @Transactional
    public SubscriptionPaymentDTO recordPayment(Long companyId, RecordPaymentRequest request) {
        return paymentService.recordPayment(companyId, request);
    }

    // ─── Renovação Self-Service e Pagamentos Móveis ───────────────────────────

    public List<SubscriptionPlanDetailDTO> listAvailablePlans() {
        return renewalService.listAvailablePlans();
    }

    public BigDecimal calculateRenewalPrice(String planStr, int months) {
        return renewalService.calculateRenewalPrice(planStr, months);
    }

    @Transactional
    public SubscriptionPaymentResultDTO initiateSelfServiceRenewal(SelfServiceSubscriptionPaymentRequest request) {
        return renewalService.initiateSelfServiceRenewal(request);
    }

    @Transactional
    public SubscriptionPaymentResultDTO confirmMobileRenewal(String transactionId) {
        return renewalService.confirmMobileRenewal(transactionId);
    }
}
