package com.phcpro.modules.subscription.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.subscription.dto.RecordPaymentRequest;
import com.phcpro.modules.subscription.dto.SaveSubscriptionRequest;
import com.phcpro.modules.subscription.dto.MySubscriptionDTO;
import com.phcpro.modules.subscription.dto.SubscriptionPaymentDTO;
import com.phcpro.modules.subscription.model.PlanType;
import com.phcpro.modules.subscription.model.Subscription;
import com.phcpro.modules.subscription.model.SubscriptionPayment;
import com.phcpro.modules.subscription.model.SubscriptionStatus;
import com.phcpro.modules.subscription.repository.SubscriptionPaymentRepository;
import com.phcpro.modules.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do {@link SubscriptionService}: exige SUPERADMIN, pagamento estende a validade e reactiva,
 * e a política de login bloqueia assinaturas expiradas/suspensas mas permite sem assinatura.
 */
class SubscriptionServiceTest {

    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPaymentRepository paymentRepository;
    private CompanyRepository companyRepository;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        paymentRepository = mock(SubscriptionPaymentRepository.class);
        companyRepository = mock(CompanyRepository.class);
        service = new SubscriptionService(subscriptionRepository, paymentRepository, companyRepository,
                mock(AuditLogService.class));
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Company company(Long id) {
        Company c = new Company();
        c.setId(id);
        c.setName("Empresa " + id);
        c.setActive(true);
        return c;
    }

    @Test
    void recordPayment_estendeValidadeEReactiva() { // SB-01
        when(companyRepository.findById(5L)).thenReturn(Optional.of(company(5L)));
        Subscription sub = new Subscription();
        sub.setCompanyId(5L);
        sub.setStatus(SubscriptionStatus.EXPIRED);
        sub.setValidUntil(LocalDate.now().minusDays(10));
        when(subscriptionRepository.findByCompanyId(5L)).thenReturn(Optional.of(sub));
        when(paymentRepository.save(any(SubscriptionPayment.class))).thenAnswer(i -> i.getArgument(0));

        LocalDate newEnd = LocalDate.now().plusMonths(1);
        SubscriptionPaymentDTO dto = service.recordPayment(5L, new RecordPaymentRequest(
                new BigDecimal("1500"), "MPESA", LocalDate.now(), LocalDate.now(), newEnd, "Mensalidade"));

        assertEquals("M-Pesa", dto.methodLabel());
        assertEquals(newEnd, sub.getValidUntil());
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        verify(subscriptionRepository).save(sub);
    }

    @Test
    void recordPayment_valorNaoPositivo_rejeita() { // SB-02
        when(companyRepository.findById(5L)).thenReturn(Optional.of(company(5L)));
        assertThrows(BusinessRuleException.class, () -> service.recordPayment(5L,
                new RecordPaymentRequest(BigDecimal.ZERO, "DINHEIRO", null, null, null, null)));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void allowsLogin_semAssinatura_permite() { // SB-03
        when(subscriptionRepository.findByCompanyId(9L)).thenReturn(Optional.empty());
        assertTrue(service.allowsLogin(9L));
    }

    @Test
    void allowsLogin_expiradaOuSuspensa_bloqueia() { // SB-04
        Subscription expired = new Subscription();
        expired.setStatus(SubscriptionStatus.ACTIVE);
        expired.setValidUntil(LocalDate.now().minusDays(1));
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(expired));
        assertFalse(service.allowsLogin(1L)); // efectivo = EXPIRED

        Subscription suspended = new Subscription();
        suspended.setStatus(SubscriptionStatus.SUSPENDED);
        suspended.setValidUntil(LocalDate.now().plusYears(1));
        when(subscriptionRepository.findByCompanyId(2L)).thenReturn(Optional.of(suspended));
        assertFalse(service.allowsLogin(2L));
    }

    @Test
    void getMySubscription_calculaDiasRestantes() { // SB-06
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(3L);
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company(3L)));
        Subscription sub = new Subscription();
        sub.setPlan(PlanType.PRO);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setValidUntil(LocalDate.now().plusDays(5));
        when(subscriptionRepository.findByCompanyId(3L)).thenReturn(Optional.of(sub));

        MySubscriptionDTO dto = service.getMySubscription();

        assertTrue(dto.hasSubscription());
        assertEquals(5L, dto.daysRemaining());
        assertEquals("PRO", dto.plan());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    void saveSubscription_semSuperadmin_bloqueia() { // SB-05
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        assertThrows(BusinessRuleException.class, () -> service.saveSubscription(1L,
                new SaveSubscriptionRequest("PRO", new BigDecimal("2000"), LocalDate.now().plusMonths(1))));
        verifyNoInteractions(subscriptionRepository);
    }
}
