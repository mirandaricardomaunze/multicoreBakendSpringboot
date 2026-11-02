package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatus;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ver docs/POS_PRE_PILOTO_SPEC.md. */
class MobilePaymentSimulationOffHarnessTest {

    private final MobilePaymentRepository repository = mock(MobilePaymentRepository.class);
    private final MobilePaymentService service = new MobilePaymentService(repository, false);

    @Test
    @DisplayName("PP-01: Sem simulação, nenhum pagamento móvel é iniciado")
    void pp01_initiateIsRefused() {
        InitiateMobilePaymentRequest request = new InitiateMobilePaymentRequest(
                MobilePaymentProvider.MPESA, "841234567", new BigDecimal("250.00"), "POS-1", 1L, "ana");

        assertThrows(BusinessRuleException.class, () -> service.initiatePayment(request));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("PP-02: Sem simulação, uma transacção pendente nunca é confirmada sozinha")
    void pp02_pendingIsNeverApproved() {
        MobilePaymentTransaction tx = pending();
        when(repository.findByTransactionId("TX-1")).thenReturn(Optional.of(tx));

        assertEquals(MobilePaymentStatus.PENDING, service.checkStatus("TX-1", 1L).status());
        assertNull(tx.getFinancialReference());
    }

    @Test
    @DisplayName("PP-03: Sem simulação, o endpoint de conclusão forçada é recusado")
    void pp03_forcedCompletionIsRefused() {
        assertThrows(BusinessRuleException.class, () -> service.simulateComplete("TX-1", 1L, true));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("PP-04: Simulação desligada por omissão e fixa em produção")
    void pp04_simulationIsOffByDefault() throws Exception {
        assertTrue(Files.readString(Path.of("src/main/resources/application.properties"))
                .contains("pos.mobile-payment.simulation-enabled=${POS_MOBILE_PAYMENT_SIMULATION:false}"));
        assertTrue(Files.readString(Path.of("src/main/resources/application-prod.properties"))
                .contains("pos.mobile-payment.simulation-enabled=false"));
        assertFalse(service.availability().simulationEnabled());
    }

    private static MobilePaymentTransaction pending() {
        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        tx.setCompanyId(1L);
        tx.setTransactionId("TX-1");
        tx.setProvider(MobilePaymentProvider.MPESA);
        tx.setStatus(MobilePaymentStatus.PENDING);
        tx.setCreatedAt(LocalDateTime.now().minusSeconds(3));
        return tx;
    }
}
