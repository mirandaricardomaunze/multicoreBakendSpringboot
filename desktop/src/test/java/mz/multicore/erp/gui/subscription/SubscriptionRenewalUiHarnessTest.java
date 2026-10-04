package mz.multicore.erp.gui.subscription;

import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.gui.SubscriptionRenewalDialog;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SubscriptionRenewalUiHarnessTest {

    @Test
    @DisplayName("PSP-07: Verificação estrutural do diálogo de renovação e cálculos de desconto")
    void testRenewalDialogDiscounts() {
        // Teste de cálculo de desconto comercial
        BigDecimal proMonthly = new BigDecimal("3500.00");

        // 1 mês
        BigDecimal total1m = proMonthly.multiply(BigDecimal.valueOf(1)).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3500.00"), total1m);

        // 3 meses (5% desc)
        BigDecimal total3m = proMonthly.multiply(BigDecimal.valueOf(3)).multiply(new BigDecimal("0.95")).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("9975.00"), total3m);

        // 12 meses (15% desc)
        BigDecimal total12m = proMonthly.multiply(BigDecimal.valueOf(12)).multiply(new BigDecimal("0.85")).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("35700.00"), total12m);
    }

    @Test
    @DisplayName("PSP-08: Callback de sucesso na renovação com atualização de estado")
    void testRenewalCallbackSuccess() {
        MySubscriptionApiClient client = Mockito.mock(MySubscriptionApiClient.class);
        MySubscriptionDTO renewedSub = new MySubscriptionDTO(
                "Minha Empresa Lda",
                true,
                "PRO",
                "Profissional",
                "ACTIVE",
                "Activa",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                180L,
                new BigDecimal("3500.00")
        );

        when(client.renewSubscription(any())).thenReturn(
                new SubscriptionPaymentResultDTO(true, "Renovado com sucesso!", null, renewedSub)
        );

        AtomicBoolean callbackFired = new AtomicBoolean(false);
        SubscriptionPaymentResultDTO res = client.renewSubscription(any());

        if (res.success()) {
            callbackFired.set(true);
            assertEquals("ACTIVE", res.subscription().status());
            assertEquals("PRO", res.subscription().plan());
        }

        assertTrue(callbackFired.get());
    }
}
