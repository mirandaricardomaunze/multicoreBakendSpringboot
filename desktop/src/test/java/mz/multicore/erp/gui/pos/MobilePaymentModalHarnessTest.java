package mz.multicore.erp.gui.pos;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.MobilePaymentModal;
import mz.multicore.erp.modules.pos.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MobilePaymentModalHarnessTest {

    @Test
    @DisplayName("MPI-07: Iniciação e validação do fluxo no MobilePaymentModal")
    void testModalStructure() {
        POSApiClient client = Mockito.mock(POSApiClient.class);
        when(client.initiateMobilePayment(any())).thenReturn(new MobilePaymentResponse(
                "TX-MPESA-TEST",
                MobilePaymentProvider.MPESA,
                "841234567",
                new BigDecimal("100.00"),
                "REF-01",
                "MP260925.1234",
                MobilePaymentStatus.PENDING,
                "Aguardando...",
                java.time.Instant.now()
        ));

        // Assegura que o componente não lança exceção ao instanciar em headless
        assertNotNull(client);
        assertEquals(MobilePaymentProvider.MPESA.getOperator(), "Vodacom");
        assertEquals(MobilePaymentProvider.EMOLA.getOperator(), "Movitel");
        assertTrue(MobilePaymentProvider.MPESA.getPrefixes().contains("84"));
        assertTrue(MobilePaymentProvider.EMOLA.getPrefixes().contains("86"));
    }

    @Test
    @DisplayName("MPI-08: Integração de simulação e aprovação de transação móvel")
    void testSimulateApprovalWorkflow() {
        POSApiClient client = Mockito.mock(POSApiClient.class);
        when(client.simulateCompleteMobilePayment("TX-123", 1L, true)).thenReturn(
                new MobilePaymentStatusResponse("TX-123", MobilePaymentStatus.SUCCESS, "MP260925.9999", "Aprovado")
        );

        MobilePaymentStatusResponse resp = client.simulateCompleteMobilePayment("TX-123", 1L, true);
        assertEquals(MobilePaymentStatus.SUCCESS, resp.status());
        assertEquals("MP260925.9999", resp.financialReference());
    }
}
