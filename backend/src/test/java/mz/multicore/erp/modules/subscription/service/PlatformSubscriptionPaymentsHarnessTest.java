package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.pos.dto.*;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import mz.multicore.erp.modules.pos.service.MobilePaymentService;
import mz.multicore.erp.modules.subscription.dto.*;
import mz.multicore.erp.modules.subscription.model.*;
import mz.multicore.erp.modules.subscription.repository.SubscriptionPaymentRepository;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatformSubscriptionPaymentsHarnessTest {

    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPaymentRepository paymentRepository;
    private CompanyRepository companyRepository;
    private AuditLogService auditLogService;
    private MobilePaymentService mobilePaymentService;
    private MobilePaymentRepository mobilePaymentRepository;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        paymentRepository = mock(SubscriptionPaymentRepository.class);
        companyRepository = mock(CompanyRepository.class);
        auditLogService = mock(AuditLogService.class);
        mobilePaymentService = mock(MobilePaymentService.class);
        mobilePaymentRepository = mock(MobilePaymentRepository.class);

        service = new SubscriptionService(
                subscriptionRepository,
                paymentRepository,
                companyRepository,
                auditLogService,
                mobilePaymentService,
                mobilePaymentRepository
        );

        CurrentUserContext.setCurrentUser("admin.empresa", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(10L);

        Company company = new Company();
        company.setId(10L);
        company.setName("Empresa Teste Lda");
        company.setActive(true);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("PSP-01: Consulta de planos disponíveis da plataforma")
    void testListAvailablePlans() {
        List<SubscriptionPlanDetailDTO> plans = service.listAvailablePlans();
        assertNotNull(plans);
        assertEquals(3, plans.size());
        assertTrue(plans.stream().anyMatch(p -> "BASIC".equals(p.plan()) && p.monthlyPrice().compareTo(new BigDecimal("1500.00")) == 0));
        assertTrue(plans.stream().anyMatch(p -> "PRO".equals(p.plan()) && p.monthlyPrice().compareTo(new BigDecimal("3500.00")) == 0));
        assertTrue(plans.stream().anyMatch(p -> "ENTERPRISE".equals(p.plan()) && p.monthlyPrice().compareTo(new BigDecimal("7500.00")) == 0));
    }

    @Test
    @DisplayName("PSP-02: Cálculo determinístico de valor de renovação com desconto")
    void testCalculateRenewalPrice() {
        // 1 mês BASIC -> 1500 MT (0% desc)
        assertEquals(new BigDecimal("1500.00"), service.calculateRenewalPrice("BASIC", 1));

        // 3 meses PRO -> 3 * 3500 = 10500 * 0.95 = 9975.00 MT (5% desc)
        assertEquals(new BigDecimal("9975.00"), service.calculateRenewalPrice("PRO", 3));

        // 12 meses ENTERPRISE -> 12 * 7500 = 90000 * 0.85 = 76500.00 MT (15% desc)
        assertEquals(new BigDecimal("76500.00"), service.calculateRenewalPrice("ENTERPRISE", 12));
    }

    @Test
    @DisplayName("PSP-03: Iniciação de renovação por M-Pesa com Push USSD")
    void testInitiateMpesaRenewal() {
        when(mobilePaymentService.initiatePayment(any())).thenReturn(new MobilePaymentResponse(
                "TX-SUB-12345",
                MobilePaymentProvider.MPESA,
                "841234567",
                new BigDecimal("1500.00"),
                "SUB-10",
                null,
                MobilePaymentStatus.PENDING,
                "Aguardando...",
                java.time.Instant.now()
        ));

        SelfServiceSubscriptionPaymentRequest req = new SelfServiceSubscriptionPaymentRequest(
                "BASIC", 1, "MPESA", "841234567", null, "Renovação via M-Pesa"
        );

        SubscriptionPaymentResultDTO result = service.initiateSelfServiceRenewal(req);
        assertNotNull(result);
        assertFalse(result.success(), "Deve aguardar autorização USSD");
        assertEquals("TX-SUB-12345", result.transactionId());
        verify(mobilePaymentService).initiatePayment(any());
    }

    @Test
    @DisplayName("PSP-04: Confirmação de pagamento móvel e extensão da validade")
    void testConfirmMobileRenewal() {
        String txId = "TX-SUB-999";
        when(mobilePaymentService.checkStatus(txId, 10L)).thenReturn(
                new MobilePaymentStatusResponse(txId, MobilePaymentStatus.SUCCESS, "MP260925.7777", "Aprovado")
        );

        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        tx.setCompanyId(10L);
        tx.setTransactionId(txId);
        tx.setProvider(MobilePaymentProvider.MPESA);
        tx.setAmount(new BigDecimal("1500.00"));
        tx.setFinancialReference("MP260925.7777");
        tx.setPhoneNumber("841234567");

        when(mobilePaymentRepository.findByTransactionIdAndCompanyId(txId, 10L)).thenReturn(Optional.of(tx));

        Subscription sub = new Subscription();
        sub.setCompanyId(10L);
        sub.setPlan(PlanType.BASIC);
        sub.setStatus(SubscriptionStatus.EXPIRED);
        sub.setValidUntil(LocalDate.now().minusDays(2));
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.of(sub));

        SubscriptionPaymentResultDTO result = service.confirmMobileRenewal(txId);
        assertTrue(result.success());
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertTrue(sub.getValidUntil().isAfter(LocalDate.now()));
        verify(paymentRepository).save(any(SubscriptionPayment.class));
        verify(subscriptionRepository).save(sub);
    }

    @Test
    @DisplayName("PSP-05: Registo de pagamento manual por transferência bancária")
    void testManualBankTransferRenewal() {
        Subscription sub = new Subscription();
        sub.setCompanyId(10L);
        sub.setPlan(PlanType.PRO);
        sub.setValidUntil(LocalDate.now());
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.of(sub));

        SelfServiceSubscriptionPaymentRequest req = new SelfServiceSubscriptionPaymentRequest(
                "PRO", 3, "BANK_TRANSFER", null, "TRF-BIM-884920", "Comprovativo enviado por e-mail"
        );

        SubscriptionPaymentResultDTO result = service.initiateSelfServiceRenewal(req);
        assertTrue(result.success());
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertEquals(LocalDate.now().plusMonths(3), sub.getValidUntil());
        verify(paymentRepository).save(any(SubscriptionPayment.class));
    }

    @Test
    @DisplayName("PSP-06: Preservação estrita da ativação manual pelo SuperAdmin")
    void testSuperAdminManualActivationPreserved() {
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");

        SaveSubscriptionRequest saveReq = new SaveSubscriptionRequest(
                "ENTERPRISE",
                new BigDecimal("7500.00"),
                LocalDate.now().plusMonths(6)
        );

        Subscription sub = new Subscription();
        sub.setCompanyId(10L);
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.of(sub));

        SubscriptionDTO saved = service.saveSubscription(10L, saveReq);
        assertNotNull(saved);
        assertEquals("ENTERPRISE", saved.plan());
        assertEquals(SubscriptionStatus.ACTIVE.name(), saved.status());

        // Registo manual de pagamento pelo SuperAdmin
        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("7500.00"),
                "DINHEIRO",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                "PAGO NO ESCRITÓRIO",
                null,
                "Recibo manual 001"
        );

        SubscriptionPaymentDTO payment = service.recordPayment(10L, payReq);
        assertNotNull(payment);
        assertEquals(new BigDecimal("7500.00"), payment.amount());
        verify(paymentRepository, atLeastOnce()).save(any());
    }
}
