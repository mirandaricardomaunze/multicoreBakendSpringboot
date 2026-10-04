package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.dto.*;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MobilePaymentHarnessTest {

    private MobilePaymentRepository repository;
    private MobilePaymentService service;

    @BeforeEach
    void setUp() {
        repository = mock(MobilePaymentRepository.class);
        service = new MobilePaymentService(repository);
    }

    @Test
    @DisplayName("MPI-01: Validação e normalização de números Vodacom M-Pesa")
    void testNormalizeVodacomPhone() {
        assertEquals("841234567", service.normalizePhoneNumber("+258 84 123 4567"));
        assertEquals("859876543", service.normalizePhoneNumber("258-85-987-6543"));
        assertEquals("840001122", service.normalizePhoneNumber("840001122"));

        assertDoesNotThrow(() -> service.validateProviderPhone(MobilePaymentProvider.MPESA, "841234567"));
        assertDoesNotThrow(() -> service.validateProviderPhone(MobilePaymentProvider.MPESA, "851234567"));
    }

    @Test
    @DisplayName("MPI-02: Validação e normalização de números Movitel e-Mola")
    void testNormalizeMovitelPhone() {
        assertEquals("861234567", service.normalizePhoneNumber("+258 (86) 123-4567"));
        assertEquals("879876543", service.normalizePhoneNumber("87 987 6543"));

        assertDoesNotThrow(() -> service.validateProviderPhone(MobilePaymentProvider.EMOLA, "861234567"));
        assertDoesNotThrow(() -> service.validateProviderPhone(MobilePaymentProvider.EMOLA, "879876543"));
    }

    @Test
    @DisplayName("MPI-03: Rejeição de número incompatível com a operadora selecionada")
    void testProviderPhoneMismatch() {
        // Número Movitel (86) passado para M-Pesa
        BusinessRuleException ex1 = assertThrows(BusinessRuleException.class, () ->
                service.validateProviderPhone(MobilePaymentProvider.MPESA, "861234567"));
        assertTrue(ex1.getMessage().contains("incompatível com M-Pesa"));

        // Número Vodacom (84) passado para e-Mola
        BusinessRuleException ex2 = assertThrows(BusinessRuleException.class, () ->
                service.validateProviderPhone(MobilePaymentProvider.EMOLA, "841234567"));
        assertTrue(ex2.getMessage().contains("incompatível com e-Mola"));
    }

    @Test
    @DisplayName("MPI-04: Iniciação de transação e geração de ID único com status PENDING")
    void testInitiatePaymentSuccess() {
        when(repository.save(any(MobilePaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        InitiateMobilePaymentRequest req = new InitiateMobilePaymentRequest(
                MobilePaymentProvider.MPESA,
                "+258 84 765 4321",
                new BigDecimal("250.00"),
                "POS-1001",
                1L,
                "operador.pos"
        );

        MobilePaymentResponse resp = service.initiatePayment(req);

        assertNotNull(resp);
        assertNotNull(resp.transactionId());
        assertTrue(resp.transactionId().startsWith("TX-MPESA-"));
        assertEquals("847654321", resp.phoneNumber());
        assertEquals(new BigDecimal("250.00"), resp.amount());
        assertEquals(MobilePaymentStatus.PENDING, resp.status());

        ArgumentCaptor<MobilePaymentTransaction> captor = ArgumentCaptor.forClass(MobilePaymentTransaction.class);
        verify(repository).save(captor.capture());
        MobilePaymentTransaction saved = captor.getValue();
        assertEquals(1L, saved.getCompanyId());
        assertEquals(MobilePaymentProvider.MPESA, saved.getProvider());
    }

    @Test
    @DisplayName("MPI-05: Consulta de status e transição para SUCCESS com referência financeira")
    void testCheckStatusTransitionToSuccess() {
        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        tx.setCompanyId(1L);
        tx.setTransactionId("TX-MPESA-20260925-1234");
        tx.setProvider(MobilePaymentProvider.MPESA);
        tx.setStatus(MobilePaymentStatus.PENDING);
        // Criado há 3 segundos para acionar simulação de sucesso
        tx.setCreatedAt(LocalDateTime.now().minusSeconds(3));

        when(repository.findByTransactionId("TX-MPESA-20260925-1234")).thenReturn(Optional.of(tx));

        MobilePaymentStatusResponse status = service.checkStatus("TX-MPESA-20260925-1234", 1L);

        assertEquals(MobilePaymentStatus.SUCCESS, status.status());
        assertNotNull(status.financialReference());
        assertTrue(status.financialReference().startsWith("MP"));
        verify(repository).save(tx);
    }

    @Test
    @DisplayName("MPI-06: Isolamento Multi-Tenant da transação")
    void testMultiTenantIsolation() {
        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        tx.setCompanyId(2L);
        tx.setTransactionId("TX-EMOLA-20260925-8888");

        when(repository.findByTransactionId("TX-EMOLA-20260925-8888")).thenReturn(Optional.of(tx));

        // Empresa 1 tentando acessar transação da Empresa 2
        assertThrows(BusinessRuleException.class, () ->
                service.checkStatus("TX-EMOLA-20260925-8888", 1L));
    }
}
