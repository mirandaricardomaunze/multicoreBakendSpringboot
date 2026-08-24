package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
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
    private HRApiClient hrApiClient;
    private NotificationFeed feed;

    @BeforeEach
    void setUp() {
        approvalApiClient = mock(ApprovalApiClient.class);
        inventoryApiClient = mock(InventoryApiClient.class);
        subscriptionApiClient = mock(MySubscriptionApiClient.class);
        hrApiClient = mock(HRApiClient.class);
        feed = new NotificationFeed(approvalApiClient, inventoryApiClient, subscriptionApiClient, hrApiClient);
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

    // ─── RHC-17: fim de contrato (≤30 dias) e fim de experiência (≤7) chegam ao sino ──────

    @Test
    void load_contratoAExpirar_entraNoSino() {
        silenciarOutrasFontes();
        when(hrApiClient.getContractAlerts()).thenReturn(new ContractAlertsDTO(
                List.of(contrato("CTR-2026/3", "Ana Sitoe", LocalDate.now().plusDays(12), 12L, null)),
                List.of()));

        List<NotificationFeed.NotificationItem> items = feed.load(7L);

        assertEquals(1, items.size());
        assertEquals("Contratos", items.get(0).type());
        assertTrue(items.get(0).title().contains("Ana Sitoe"));
        assertTrue(items.get(0).title().contains("12 dia(s)"));
        assertTrue(items.get(0).detail().contains("CTR-2026/3"));
    }

    @Test
    void load_experienciaATerminar_entraNoSinoComPrioridadeAlta() {
        // Confirmar (ou não) alguém no fim da experiência não tem antecedência: é sempre urgente.
        silenciarOutrasFontes();
        when(hrApiClient.getContractAlerts()).thenReturn(new ContractAlertsDTO(
                List.of(),
                List.of(contrato("CTR-2026/9", "Rita Nhaca", null, null, LocalDate.now().plusDays(5)))));

        List<NotificationFeed.NotificationItem> items = feed.load(7L);

        assertEquals(1, items.size());
        assertTrue(items.get(0).title().contains("Período experimental"));
        assertTrue(items.get(0).title().contains("Rita Nhaca"));
        assertEquals(3, items.get(0).priority());
    }

    // ─── RHC-52: retenções por entregar chegam ao sino ────────────────────────

    @Test
    void load_retencaoEmAtraso_entraNoSinoComPrioridadeAlta() {
        silenciarOutrasFontes();
        when(hrApiClient.getPayrollLiabilityAlerts()).thenReturn(List.of(
                retencao("IRPS retido", LocalDate.now().minusDays(4), -4L, true)));

        List<NotificationFeed.NotificationItem> items = feed.load(7L);

        assertEquals(1, items.size());
        assertEquals("Retenções", items.get(0).type());
        assertTrue(items.get(0).title().contains("em atraso"));
        assertEquals(3, items.get(0).priority());
    }

    @Test
    void load_retencaoSemPrazoConfigurado_apareceNaMesma() { // §6
        // É a que nunca chega a estar atrasada — e por isso é a que desaparecia. Sem esta linha, o
        // dinheiro do Estado ficava outra vez invisível numa empresa que não configurou os prazos.
        silenciarOutrasFontes();
        when(hrApiClient.getPayrollLiabilityAlerts()).thenReturn(List.of(
                retencao("INSS — quota do trabalhador", null, null, false)));

        List<NotificationFeed.NotificationItem> items = feed.load(7L);

        assertEquals(1, items.size());
        assertTrue(items.get(0).title().contains("sem prazo definido"));
        assertTrue(items.get(0).detail().contains("Valores Legais"));
        assertEquals("Prazo por configurar", items.get(0).when());
    }

    private static mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO retencao(
            String label, LocalDate dueDate, Long daysUntilDue, boolean overdue) {
        return new mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO(
                1L, 2026, 8, "IRPS", label, new BigDecimal("3000.00"), dueDate, daysUntilDue,
                overdue, "POR_ENTREGAR", "Por entregar", null, null, null);
    }

    private void silenciarOutrasFontes() {
        when(approvalApiClient.getPendingRequests()).thenReturn(List.of());
        when(inventoryApiClient.getStocksByCompany(7L)).thenReturn(List.of());
        when(inventoryApiClient.findExpiringBatches(7L, 30)).thenReturn(List.of());
        when(subscriptionApiClient.getMySubscription()).thenReturn(new MySubscriptionDTO(
                "Empresa", false, null, null, null, null, null, null, null, null));
    }

    private static EmploymentContractDTO contrato(String number, String name, LocalDate end,
                                                  Long daysUntilEnd, LocalDate probationEnd) {
        return new EmploymentContractDTO(1L, number, 5L, name, "TERMO_CERTO", "A termo certo",
                "VIGENTE", "Vigente", LocalDate.now().minusMonths(6), end, probationEnd,
                new BigDecimal("30000"), 40, "Operadora", null, "Acréscimo de actividade",
                null, null, null, null, false, probationEnd != null, daysUntilEnd);
    }
}
