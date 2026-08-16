package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationFeedTest {

    private ApprovalApiClient approvalApiClient;
    private InventoryApiClient inventoryApiClient;
    private MySubscriptionApiClient subscriptionApiClient;
    private NotificationFeed feed;

    @BeforeEach
    void setUp() {
        approvalApiClient = mock(ApprovalApiClient.class);
        inventoryApiClient = mock(InventoryApiClient.class);
        subscriptionApiClient = mock(MySubscriptionApiClient.class);
        feed = new NotificationFeed(approvalApiClient, inventoryApiClient, subscriptionApiClient);
    }

    @Test
    void load_alertasOperacionais_agregaQuatroFontes() {
        when(approvalApiClient.getPendingRequests()).thenReturn(List.of(new ApprovalRequestDTO(
                1L, "ORDER", 10L, new BigDecimal("100"), "ana", ApprovalStatus.PENDING,
                "MANAGER", "Encomenda EC-1", null, LocalDateTime.now())));
        when(inventoryApiClient.getStocksByCompany(7L)).thenReturn(List.of(new StockDTO(
                1L, 2L, "SKU", "REF", "BAR", "Arroz", 3L, "Loja",
                new BigDecimal("2"), new BigDecimal("5"), "Mercearia", new BigDecimal("80"), 1)));
        when(inventoryApiClient.findExpiringBatches(7L, 30)).thenReturn(List.of(new ProductBatchDTO(
                2L, 2L, "SKU", "Arroz", 3L, "Loja", "L1", LocalDate.now().minusDays(1),
                LocalDate.now().minusMonths(1), BigDecimal.ONE)));
        when(subscriptionApiClient.getMySubscription()).thenReturn(new MySubscriptionDTO(
                "Empresa", true, "PRO", "Profissional", "ACTIVE", "Activa",
                LocalDate.now().minusMonths(1), LocalDate.now().plusDays(3), 3L, new BigDecimal("1000")));

        List<NotificationFeed.NotificationItem> items = feed.load(7L);

        assertEquals(4, items.size());
        assertTrue(items.stream().anyMatch(item -> "Aprovações".equals(item.type())));
        assertTrue(items.stream().anyMatch(item -> "Stock".equals(item.type())));
        assertTrue(items.stream().anyMatch(item -> "Validades".equals(item.type())));
        assertTrue(items.stream().anyMatch(item -> "Assinatura".equals(item.type())));
    }

    @Test
    void load_semAlertas_devolveListaVazia() {
        when(approvalApiClient.getPendingRequests()).thenReturn(List.of());
        when(inventoryApiClient.getStocksByCompany(7L)).thenReturn(List.of());
        when(inventoryApiClient.findExpiringBatches(7L, 30)).thenReturn(List.of());
        when(subscriptionApiClient.getMySubscription()).thenReturn(new MySubscriptionDTO(
                "Empresa", false, null, null, null, null, null, null, null, null));

        assertTrue(feed.load(7L).isEmpty());
    }
}
