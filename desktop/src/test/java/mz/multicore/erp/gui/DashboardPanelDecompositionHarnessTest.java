package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Decomposição de DashboardPanel (Fase 12)")
class DashboardPanelDecompositionHarnessTest {

    private static Path resolveGui(String name) {
        Path direct = Path.of("desktop/src/main/java/mz/multicore/erp/gui", name);
        if (Files.exists(direct)) return direct;
        Path nested = Path.of("src/main/java/mz/multicore/erp/gui", name);
        if (Files.exists(nested)) return nested;
        return direct;
    }

    @Test
    @DisplayName("DECOMP-01: Limites físicos estritos de linhas de código")
    void decomp01_strictLineLimits() throws IOException {
        Path panelPath = resolveGui("DashboardPanel.java");
        Path coordPath = resolveGui("DashboardDataCoordinator.java");
        Path overviewPath = resolveGui("DashboardOverviewTab.java");
        Path opsPath = resolveGui("DashboardOperationsTab.java");

        assertTrue(Files.exists(panelPath), "DashboardPanel.java deve existir");
        assertTrue(Files.exists(coordPath), "DashboardDataCoordinator.java deve existir");
        assertTrue(Files.exists(overviewPath), "DashboardOverviewTab.java deve existir");
        assertTrue(Files.exists(opsPath), "DashboardOperationsTab.java deve existir");

        long panelLines = Files.lines(panelPath).count();
        long coordLines = Files.lines(coordPath).count();
        long overviewLines = Files.lines(overviewPath).count();
        long opsLines = Files.lines(opsPath).count();

        assertTrue(panelLines <= 220, "DashboardPanel.java excedeu 220 linhas (actual: " + panelLines + ")");
        assertTrue(coordLines <= 200, "DashboardDataCoordinator.java excedeu 200 linhas (actual: " + coordLines + ")");
        assertTrue(overviewLines <= 240, "DashboardOverviewTab.java excedeu 240 linhas (actual: " + overviewLines + ")");
        assertTrue(opsLines <= 150, "DashboardOperationsTab.java excedeu 150 linhas (actual: " + opsLines + ")");
    }

    @Test
    @DisplayName("DECOMP-02: Zero diálogos legados JOptionPane nos componentes de dashboard")
    void decomp02_zeroLegacyDialogs() throws IOException {
        List<String> files = List.of(
                "DashboardPanel.java",
                "DashboardDataCoordinator.java",
                "DashboardOverviewTab.java",
                "DashboardOperationsTab.java"
        );
        for (String file : files) {
            Path p = resolveGui(file);
            if (Files.exists(p)) {
                String content = Files.readString(p);
                assertFalse(content.contains("JOptionPane.show"),
                        file + " não pode conter chamadas legadas a JOptionPane");
            }
        }
    }

    @Test
    @DisplayName("DECOMP-03: Estrutura canónica das 3 abas e widgets preservada")
    void decomp03_tabStructureAndWidgets() {
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        FinanceApiClient financeApi = Mockito.mock(FinanceApiClient.class);
        ApprovalApiClient approvalApi = Mockito.mock(ApprovalApiClient.class);
        CRMApiClient crmApi = Mockito.mock(CRMApiClient.class);
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient inventoryApi = Mockito.mock(InventoryApiClient.class);

        DashboardPanel panel = new DashboardPanel(
                comercialApi, financeApi, approvalApi, crmApi, purchaseApi, inventoryApi);

        JTabbedPane tabs = null;
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JTabbedPane tp) {
                tabs = tp;
                break;
            }
        }
        assertNotNull(tabs, "DashboardPanel deve conter um JTabbedPane");
        assertEquals(3, tabs.getTabCount(), "DashboardPanel deve conter exactamente 3 abas corporativas");
        assertEquals("Visão Geral & Gráficos", tabs.getTitleAt(0));
        assertEquals("Operações", tabs.getTitleAt(1));
        assertEquals("Rentabilidade & Resultados", tabs.getTitleAt(2));

        assertNotNull(panel.getStrategicPulseWidget(), "StrategicPulseWidget não pode ser nulo");
        assertNotNull(panel.getOperationalPulseWidget(), "OperationalPulseWidget não pode ser nulo");
    }

    @Test
    @DisplayName("DECOMP-04: Construtores sobrecarregados e interatividade dos cartões de KPI")
    void decomp04_constructorsAndKpiCards() {
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        FinanceApiClient financeApi = Mockito.mock(FinanceApiClient.class);
        ApprovalApiClient approvalApi = Mockito.mock(ApprovalApiClient.class);
        CRMApiClient crmApi = Mockito.mock(CRMApiClient.class);
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient inventoryApi = Mockito.mock(InventoryApiClient.class);
        ForensicAuditApiClient auditApi = Mockito.mock(ForensicAuditApiClient.class);
        CashFlowForecastApiClient cashFlowApi = Mockito.mock(CashFlowForecastApiClient.class);
        CreditRiskApiClient creditApi = Mockito.mock(CreditRiskApiClient.class);
        PerformanceApiClient perfApi = Mockito.mock(PerformanceApiClient.class);
        DashboardApiClient dashApi = Mockito.mock(DashboardApiClient.class);

        AtomicBoolean navigated = new AtomicBoolean(false);

        // 11-arg constructor
        DashboardPanel panel11 = new DashboardPanel(
                comercialApi, financeApi, approvalApi, crmApi, purchaseApi, inventoryApi,
                auditApi, cashFlowApi, creditApi, perfApi, target -> navigated.set(true));
        assertNotNull(panel11);

        // 12-arg constructor
        DashboardPanel panel12 = new DashboardPanel(
                comercialApi, financeApi, approvalApi, crmApi, purchaseApi, inventoryApi,
                auditApi, cashFlowApi, creditApi, perfApi, dashApi, target -> navigated.set(true));
        assertNotNull(panel12);

        List<ModernPanel> kpiCards = new ArrayList<>();
        findKpiCards(panel12, kpiCards);

        assertEquals(8, kpiCards.size(), "Dashboard deve conter exactamente 8 cartões de KPI corporativos");
        for (ModernPanel card : kpiCards) {
            assertEquals(Cursor.HAND_CURSOR, card.getCursor().getType(),
                    "Cartão KPI deve apresentar HAND_CURSOR");
            assertNotNull(card.getToolTipText(), "Cartão KPI deve apresentar tooltip orientador");
        }
    }

    @Test
    @DisplayName("DECOMP-05: Ciclo de vida e actualização assíncrona segura")
    void decomp05_lifecycleAndPeriodSwitch() {
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        FinanceApiClient financeApi = Mockito.mock(FinanceApiClient.class);
        ApprovalApiClient approvalApi = Mockito.mock(ApprovalApiClient.class);
        CRMApiClient crmApi = Mockito.mock(CRMApiClient.class);
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient inventoryApi = Mockito.mock(InventoryApiClient.class);

        Mockito.when(financeApi.getAllAccounts()).thenReturn(Collections.emptyList());
        Mockito.when(comercialApi.getAllInvoices()).thenReturn(Collections.emptyList());
        Mockito.when(purchaseApi.getPurchasesByCompany(Mockito.any())).thenReturn(Collections.emptyList());
        Mockito.when(inventoryApi.getStocksByCompany(Mockito.any())).thenReturn(Collections.emptyList());
        Mockito.when(inventoryApi.findExpiringBatches(Mockito.any(), Mockito.anyInt())).thenReturn(Collections.emptyList());
        Mockito.when(approvalApi.getPendingRequests()).thenReturn(Collections.emptyList());
        Mockito.when(crmApi.getAllTickets()).thenReturn(Collections.emptyList());

        try {
            CurrentUserContext.setCurrentCompanyId(1L);
            CurrentUserContext.setCurrentUser("admin", "ADMIN");

            DashboardPanel panel = new DashboardPanel(
                    comercialApi, financeApi, approvalApi, crmApi, purchaseApi, inventoryApi);

            assertDoesNotThrow(panel::refreshData, "refreshData() deve executar sem NPE");
            assertDoesNotThrow(() -> panel.selectPeriod(DashboardPanel.PeriodFilter.ESTE_MES),
                    "selectPeriod() deve alternar período sem erro");
            assertEquals(DashboardPanel.PeriodFilter.ESTE_MES, panel.getCurrentPeriod());
            assertDoesNotThrow(() -> panel.updateWelcomeMessage("Gestor Teste", "GESTOR"),
                    "updateWelcomeMessage() deve actualizar label sem NPE");
        } finally {
            CurrentUserContext.clear();
        }
    }

    private static void findKpiCards(Container container, List<ModernPanel> list) {
        for (Component c : container.getComponents()) {
            if (c instanceof ModernPanel panel && panel.getToolTipText() != null && panel.getToolTipText().startsWith("Clique para abrir")) {
                list.add(panel);
            }
            if (c instanceof Container child) {
                findKpiCards(child, list);
            }
        }
    }
}
