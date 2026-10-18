package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.SmartAlertsDialog;
import mz.multicore.erp.gui.components.SmartAlertsDialog.SmartAlert;
import mz.multicore.erp.gui.components.SmartAlertsDialog.Urgency;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Harness de validação dos alertas proactivos de stock em trânsito em atraso.
 * Conforme docs/STOCK_IN_TRANSIT_OVERDUE_ALERT_SPEC.md.
 */
class StockInTransitOverdueAlertHarnessTest {

    private final ApprovalApiClient approval = mock(ApprovalApiClient.class);
    private final InventoryApiClient inv = mock(InventoryApiClient.class);
    private final MySubscriptionApiClient sub = mock(MySubscriptionApiClient.class);
    private final HRApiClient hr = mock(HRApiClient.class);
    private final StockTransferApiClient transferClient = mock(StockTransferApiClient.class);

    @Test
    @DisplayName("Guia recente em trânsito (< 48h) não gera alerta proactivo")
    void guiaRecente_naoGeraAlerta() {
        StockTransferDTO recent = createTransfer(1L, "TRF-2026/01", LocalDateTime.now().minusHours(24));
        when(transferClient.findInTransit(1L, null)).thenReturn(List.of(recent));

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, null, null, null, transferClient);
        List<NotificationItem> items = feed.load(1L);

        boolean hasTransitAlert = items.stream().anyMatch(i -> "stock_transfers".equals(i.moduleCard()));
        assertFalse(hasTransitAlert, "Não deve alertar guias dentro da janela de tolerância de 48 horas.");
    }

    @Test
    @DisplayName("Guia em trânsito há mais de 48h gera alerta de Atenção (Priority 2)")
    void guiaMais48h_geraAlertaAtencao() {
        StockTransferDTO delayed = createTransfer(2L, "TRF-2026/02", LocalDateTime.now().minusHours(52));
        when(transferClient.findInTransit(1L, null)).thenReturn(List.of(delayed));

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, null, null, null, transferClient);
        List<NotificationItem> items = feed.load(1L);

        NotificationItem alert = items.stream()
                .filter(i -> "stock_transfers".equals(i.moduleCard()))
                .findFirst()
                .orElse(null);

        assertNotNull(alert, "Deve gerar alerta para guia atrasada.");
        assertEquals("Stock em Trânsito", alert.type());
        assertTrue(alert.title().contains("TRF-2026/02"));
        assertTrue(alert.title().contains("2 dias"));
        assertTrue(alert.detail().contains("Depósito Central → Loja Matola"));
        assertTrue(alert.detail().contains("Carlos Motorista"));
        assertEquals(2, alert.priority(), "Atraso de 2 dias deve ter prioridade 2 (Atenção).");

        SmartAlert smartAlert = SmartAlert.fromNotification(alert);
        assertEquals(Urgency.WARNING, smartAlert.urgency());
        assertEquals("Verificar Recepção", smartAlert.actionLabel());
    }

    @Test
    @DisplayName("Guia em trânsito há mais de 5 dias gera alerta Crítico (Priority 3)")
    void guiaMais5Dias_geraAlertaCritico() {
        StockTransferDTO severelyDelayed = createTransfer(3L, "TRF-2026/03", LocalDateTime.now().minusDays(6));
        when(transferClient.findInTransit(1L, null)).thenReturn(List.of(severelyDelayed));

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, null, null, null, transferClient);
        List<NotificationItem> items = feed.load(1L);

        NotificationItem alert = items.stream()
                .filter(i -> "stock_transfers".equals(i.moduleCard()))
                .findFirst()
                .orElse(null);

        assertNotNull(alert);
        assertEquals(3, alert.priority(), "Atraso igual ou superior a 5 dias deve ser Crítico (Priority 3).");

        SmartAlert smartAlert = SmartAlert.fromNotification(alert);
        assertEquals(Urgency.CRITICAL, smartAlert.urgency());
        assertEquals("Verificar Recepção", smartAlert.actionLabel());
    }

    @Test
    @DisplayName("Falha na chamada de API de transferências degrada suavemente sem crashar o feed")
    void falhaApi_degradaSuavemente() {
        when(transferClient.findInTransit(any(), any())).thenThrow(new RuntimeException("Timeout de rede"));

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, null, null, null, transferClient);
        assertDoesNotThrow(() -> feed.load(1L));
    }

    private StockTransferDTO createTransfer(Long id, String number, LocalDateTime dispatchedAt) {
        return new StockTransferDTO(
                id,
                number,
                dispatchedAt,
                1L,
                10L,
                "Depósito Central",
                20L,
                "Loja Matola",
                "IN_TRANSIT",
                "Carlos Motorista",
                "ABC-123-MC",
                "Transporte urgente",
                "gestor",
                dispatchedAt,
                null,
                List.of(),
                null,
                null,
                "Carlos Motorista",
                "ABC-123-MC",
                1L,
                null,
                null,
                true,
                dispatchedAt,
                null,
                null,
                null
        );
    }
}
