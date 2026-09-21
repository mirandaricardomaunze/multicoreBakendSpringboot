package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.gui.components.SmartAlertsDialog;
import mz.multicore.erp.gui.components.SmartAlertsDialog.SmartAlert;
import mz.multicore.erp.gui.components.SmartAlertsDialog.Urgency;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RadarAlertasProactivoHarnessTest {

    @Test
    @DisplayName("RAP-01: NotificationFeed mantém retrocompatibilidade com construtores existentes")
    void testConstructorsAndBackwardsCompatibility() {
        ApprovalApiClient approval = mock(ApprovalApiClient.class);
        InventoryApiClient inv = mock(InventoryApiClient.class);
        MySubscriptionApiClient sub = mock(MySubscriptionApiClient.class);
        HRApiClient hr = mock(HRApiClient.class);
        PerformanceApiClient perf = mock(PerformanceApiClient.class);
        CreditRiskApiClient risk = mock(CreditRiskApiClient.class);
        StockWasteApiClient waste = mock(StockWasteApiClient.class);

        // 4-args
        assertDoesNotThrow(() -> new NotificationFeed(approval, inv, sub, hr));
        // 5-args
        assertDoesNotThrow(() -> new NotificationFeed(approval, inv, sub, hr, perf));
        // 7-args
        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, perf, risk, waste);
        assertNotNull(feed);
    }

    @Test
    @DisplayName("RAP-02: Geração de alerta de Risco de Crédito Crítico e rota para risco_credito")
    void testCreditRiskCriticalAlertGenerated() {
        ApprovalApiClient approval = mock(ApprovalApiClient.class);
        InventoryApiClient inv = mock(InventoryApiClient.class);
        MySubscriptionApiClient sub = mock(MySubscriptionApiClient.class);
        HRApiClient hr = mock(HRApiClient.class);
        CreditRiskApiClient risk = mock(CreditRiskApiClient.class);

        CreditRiskSummaryDTO summary = new CreditRiskSummaryDTO(
                LocalDate.now(),
                new BigDecimal("500000.00"),
                new BigDecimal("120000.00"),
                new BigDecimal("1000000.00"),
                20,
                3, // blocked
                2, // critical
                4, // high
                5,
                6
        );
        when(risk.getSummary(any(LocalDate.class))).thenReturn(summary);

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, risk, null);
        List<NotificationFeed.NotificationItem> items = feed.load(1L);

        NotificationFeed.NotificationItem riskItem = items.stream()
                .filter(i -> "Risco de Crédito".equals(i.type()))
                .findFirst()
                .orElse(null);

        assertNotNull(riskItem, "Deve gerar alerta de Risco de Crédito");
        assertEquals(3, riskItem.priority(), "Prioridade deve ser 3 (Crítico)");
        assertEquals("risco_credito", riskItem.moduleCard());
        assertTrue(riskItem.title().contains("5 cliente(s)"), "Deve somar críticos (2) e bloqueados (3)");
        assertTrue(riskItem.detail().contains("120") && riskItem.detail().contains("000,00"),
                "Deve detalhar o total em mora: " + riskItem.detail());
    }

    @Test
    @DisplayName("RAP-03: Geração de alerta para Quebras de Stock pendentes de aprovação")
    void testPendingStockWasteAlertGenerated() {
        ApprovalApiClient approval = mock(ApprovalApiClient.class);
        InventoryApiClient inv = mock(InventoryApiClient.class);
        MySubscriptionApiClient sub = mock(MySubscriptionApiClient.class);
        HRApiClient hr = mock(HRApiClient.class);
        StockWasteApiClient waste = mock(StockWasteApiClient.class);

        StockWasteDTO w1 = new StockWasteDTO(
                1L, 1L, 1L, "Armazém Central", 10L, "SKU-01", "Iogurte Natural",
                null, null, new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("500.00"),
                WasteReason.EXPIRED, WasteStatus.PENDING_APPROVAL, "Lote expirado",
                "operador", null, LocalDateTime.now(), null, null
        );
        StockWasteDTO w2 = new StockWasteDTO(
                2L, 1L, 1L, "Armazém Central", 11L, "SKU-02", "Queijo Fresco",
                null, null, new BigDecimal("5.00"), new BigDecimal("100.00"), new BigDecimal("500.00"),
                WasteReason.DAMAGED, WasteStatus.PENDING_APPROVAL, "Caixa danificada",
                "operador", null, LocalDateTime.now(), null, null
        );

        when(waste.findByCompany(eq(1L), eq(WasteStatus.PENDING_APPROVAL)))
                .thenReturn(List.of(w1, w2));

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, null, waste);
        List<NotificationFeed.NotificationItem> items = feed.load(1L);

        NotificationFeed.NotificationItem wasteItem = items.stream()
                .filter(i -> "Quebras de Stock".equals(i.type()))
                .findFirst()
                .orElse(null);

        assertNotNull(wasteItem, "Deve gerar alerta de Quebras de Stock");
        assertEquals("stock_waste", wasteItem.moduleCard());
        assertTrue(wasteItem.title().contains("2 Quebra(s)"));
        assertTrue(wasteItem.detail().contains("1") && wasteItem.detail().contains("000,00"),
                "Deve somar perda de 1000 MT: " + wasteItem.detail());
    }

    @Test
    @DisplayName("RAP-04: Mapeamento de ações rápidas no SmartAlertsDialog")
    void testSmartAlertActionMapping() {
        NotificationFeed.NotificationItem creditItem = new NotificationFeed.NotificationItem(
                "Risco de Crédito", "Risco Crítico", "Total mora", "Cobrança urgente", "risco_credito", 3);
        SmartAlert alertCredit = SmartAlert.fromNotification(creditItem);
        assertEquals(Urgency.CRITICAL, alertCredit.urgency());
        assertEquals("Cobrar / Ver Risco", alertCredit.actionLabel());
        assertEquals("risco_credito", alertCredit.targetModule());

        NotificationFeed.NotificationItem wasteItem = new NotificationFeed.NotificationItem(
                "Quebras de Stock", "2 Quebras", "Perda", "Aprovação pendente", "stock_waste", 2);
        SmartAlert alertWaste = SmartAlert.fromNotification(wasteItem);
        assertEquals(Urgency.WARNING, alertWaste.urgency());
        assertEquals("Aprovar Quebras", alertWaste.actionLabel());
        assertEquals("stock_waste", alertWaste.targetModule());
    }

    @Test
    @DisplayName("RAP-05: Degradação suave quando serviço de risco ou quebras falha")
    void testGracefulDegradationOnClientError() {
        ApprovalApiClient approval = mock(ApprovalApiClient.class);
        InventoryApiClient inv = mock(InventoryApiClient.class);
        MySubscriptionApiClient sub = mock(MySubscriptionApiClient.class);
        HRApiClient hr = mock(HRApiClient.class);
        CreditRiskApiClient risk = mock(CreditRiskApiClient.class);
        StockWasteApiClient waste = mock(StockWasteApiClient.class);

        when(risk.getSummary(any())).thenThrow(new RuntimeException("Connection reset"));
        when(waste.findByCompany(any(), any())).thenThrow(new RuntimeException("Timeout"));

        NotificationFeed feed = new NotificationFeed(approval, inv, sub, hr, null, risk, waste);
        assertDoesNotThrow(() -> {
            List<NotificationFeed.NotificationItem> items = feed.load(1L);
            assertNotNull(items);
        }, "Falhas nos clientes remotos não podem interromper o carregamento das notificações");
    }

    @Test
    @DisplayName("RAP-06: Seleção de sub-abas especializadas em ClientesPanel e StockPanel")
    void testSubTabSelectionNavigation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                ComercialApiClient comApi = mock(ComercialApiClient.class);
                PrintApiClient printApi = mock(PrintApiClient.class);
                CreditRiskApiClient creditRiskApi = mock(CreditRiskApiClient.class);

                ClientesPanel clientesPanel = new ClientesPanel(comApi, printApi, creditRiskApi);
                assertDoesNotThrow(clientesPanel::selectCreditRiskTab);

                InventoryApiClient invApi = mock(InventoryApiClient.class);
                StockWasteApiClient wasteApi = mock(StockWasteApiClient.class);
                StockPanel stockPanel = new StockPanel(invApi, comApi, null, null, null, printApi, wasteApi);
                assertDoesNotThrow(stockPanel::selectWasteTab);
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("RAP-07: Decomposição de linhas: MainFrame e StockPanel abaixo de 1000 linhas")
    void testLineCountAndVisualUniformity() throws IOException {
        Path mainFramePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "MainFrame.java");
        long mainLines = Files.lines(mainFramePath).count();
        assertTrue(mainLines <= 1000, "MainFrame.java tem " + mainLines + " linhas; deve ser <= 1000");

        Path stockPanelPath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "StockPanel.java");
        long stockLines = Files.lines(stockPanelPath).count();
        assertTrue(stockLines <= 1000, "StockPanel.java tem " + stockLines + " linhas; deve ser <= 1000");
    }
}
