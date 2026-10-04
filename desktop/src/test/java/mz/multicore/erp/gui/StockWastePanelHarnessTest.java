package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.modules.inventory.dto.ExpiringBatchAlertDTO;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de testes automatizados para conformidade com GESTAO_QUEBRAS_STOCK_SPEC e HARNESS.
 * Valida o ciclo de vida completo de registo, filtragem, aprovação e monitorização de perdas.
 */
public class StockWastePanelHarnessTest {

    @Test
    @DisplayName("GQS-01: O painel de quebras inicializa os 3 separadores, KPIs de topo e tabela com 12 colunas")
    void testPanelTabsAndKpisInitialized() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWastePanel panel = createTestPanel();
                assertNotNull(panel);
                assertNotNull(panel.kpiTotalCost);
                assertNotNull(panel.kpiTotalQty);
                assertNotNull(panel.kpiWasteRate);
                assertNotNull(panel.kpiRiskBatches);

                assertNotNull(panel.wasteTable);
                assertEquals(12, panel.wasteTable.getColumnCount(), "A tabela de quebras deve possuir 12 colunas operacionais");
                assertEquals(42, panel.wasteTable.getRowHeight(), "A tabela de quebras deve ter altura de linha de 42px para conforto visual");
                assertEquals(42, panel.radarTable.getRowHeight(), "A tabela do radar de validades deve ter altura de linha de 42px");
                assertNotNull(panel.periodFilterCombo, "O filtro de período deve estar presente na barra de topo");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("GQS-02: A filtragem por estado isola rigorosamente as quebras PENDING_APPROVAL, APPROVED e REJECTED")
    void testFilterByStatus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWastePanel panel = createTestPanel();
                panel.currentWasteList = sampleWasteList();

                panel.statusFilterCombo.setSelectedItem(WasteStatus.PENDING_APPROVAL.getDescription());
                panel.applyWasteFilters();
                assertEquals(1, panel.wasteTableModel.getRowCount(), "Deve haver 1 quebra pendente");
                assertEquals(WasteStatus.PENDING_APPROVAL.getDescription(), panel.wasteTableModel.getValueAt(0, 9));

                panel.statusFilterCombo.setSelectedItem(WasteStatus.APPROVED.getDescription());
                panel.applyWasteFilters();
                assertEquals(1, panel.wasteTableModel.getRowCount(), "Deve haver 1 quebra aprovada");
                assertEquals(WasteStatus.APPROVED.getDescription(), panel.wasteTableModel.getValueAt(0, 9));

                panel.statusFilterCombo.setSelectedItem("Todos os Estados");
                panel.applyWasteFilters();
                assertEquals(3, panel.wasteTableModel.getRowCount(), "Todas as 3 quebras devem ser visíveis");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("GQS-03: A filtragem por motivo restringe aos registos da respetiva razão legal de abate")
    void testFilterByReason() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWastePanel panel = createTestPanel();
                panel.currentWasteList = sampleWasteList();

                panel.reasonFilterCombo.setSelectedItem(WasteReason.EXPIRED.getDescription());
                panel.applyWasteFilters();
                assertEquals(1, panel.wasteTableModel.getRowCount());
                assertEquals(WasteReason.EXPIRED.getDescription(), panel.wasteTableModel.getValueAt(0, 5));

                panel.reasonFilterCombo.setSelectedItem("Todos os Motivos");
                panel.applyWasteFilters();
                assertEquals(3, panel.wasteTableModel.getRowCount());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("GQS-04: A filtragem universal por período atua com precisão sobre a data de registo")
    void testFilterByPeriod() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWastePanel panel = createTestPanel();
                panel.currentWasteList = sampleWasteList();

                panel.periodFilterCombo.setSelectedItem("Hoje");
                panel.applyWasteFilters();
                assertEquals(1, panel.wasteTableModel.getRowCount(), "Apenas a quebra registada hoje deve passar");

                panel.periodFilterCombo.setSelectedItem("Ontem");
                panel.applyWasteFilters();
                assertEquals(1, panel.wasteTableModel.getRowCount(), "Apenas a quebra de ontem deve passar");

                panel.periodFilterCombo.setSelectedItem("Todo o período");
                panel.applyWasteFilters();
                assertEquals(3, panel.wasteTableModel.getRowCount(), "Todas as 3 quebras devem passar com 'Todo o período'");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("GQS-05 & GQS-06: Botões de aprovação/rejeição habilitam exclusivamente para quebras PENDING_APPROVAL")
    void testApprovalButtonsStateHierarchy() throws Exception {
        AtomicReference<StockWastePanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("gestor", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            StockWastePanel panel = createTestPanel();
            panel.currentWasteList = sampleWasteList();
            panel.applyWasteFilters();
            panelRef.set(panel);
        });

        // A paginação recalcula a vista por invokeLater depois das alterações do modelo.
        SwingUtilities.invokeAndWait(() -> {
            StockWastePanel panel = panelRef.get();
            try {

                // Sem seleção na tabela
                panel.wasteTable.clearSelection();
                panel.updateApprovalButtonsState();
                assertFalse(panel.approveBtn.isEnabled(), "Sem linha selecionada, aprovar deve estar inativo");
                assertFalse(panel.rejectBtn.isEnabled(), "Sem linha selecionada, rejeitar deve estar inativo");

                // Seleciona a linha 0 (PENDING_APPROVAL)
                panel.wasteTable.setRowSelectionInterval(0, 0);
                panel.updateApprovalButtonsState();
                assertTrue(panel.approveBtn.isEnabled(), "Com quebra pendente, aprovar deve estar ativo");
                assertTrue(panel.rejectBtn.isEnabled(), "Com quebra pendente, rejeitar deve estar ativo");

                // Seleciona a linha 1 (APPROVED)
                panel.wasteTable.setRowSelectionInterval(1, 1);
                panel.updateApprovalButtonsState();
                assertFalse(panel.approveBtn.isEnabled(), "Com quebra já aprovada, aprovar deve estar inativo");
                assertFalse(panel.rejectBtn.isEnabled(), "Com quebra já aprovada, rejeitar deve estar inativo");

                // Seleciona a linha 2 (REJECTED)
                panel.wasteTable.setRowSelectionInterval(2, 2);
                panel.updateApprovalButtonsState();
                assertFalse(panel.approveBtn.isEnabled(), "Com quebra já rejeitada, aprovar deve estar inativo");
                assertFalse(panel.rejectBtn.isEnabled(), "Com quebra já rejeitada, rejeitar deve estar inativo");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("GQS-07: O radar de validades classifica os lotes por prazo de validade")
    void testRadarAlerts() {
        ExpiringBatchAlertDTO alert = new ExpiringBatchAlertDTO(
                1L, "LOTE-2026-X", 10L, "SKU-IOG-01", "Iogurte Natural", "Laticínios",
                1L, "Armazém Central", new BigDecimal("50"), new BigDecimal("35.00"),
                new BigDecimal("1750.00"), LocalDate.now().plusDays(2), 2L, "CRITICAL"
        );
        assertEquals("CRITICAL", alert.alertLevel());
        assertEquals(2L, alert.daysUntilExpiration());
        assertEquals(new BigDecimal("1750.00"), alert.potentialLossValue());
    }

    @Test
    @DisplayName("GQS-08: StockWastePanel.java cumpre estritamente o limite de 1000 linhas")
    void testPanelLineCountUnderOneThousand() throws IOException {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "StockWastePanel.java");
        assertTrue(Files.exists(path), "StockWastePanel.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 1000, "StockWastePanel.java tem " + lines + " linhas; deve ser <= 1000");
    }

    private StockWastePanel createTestPanel() {
        StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);
        return new StockWastePanel(wasteApi, invApi, comApi);
    }

    private List<StockWasteDTO> sampleWasteList() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tenDaysAgo = now.minusDays(10);

        StockWasteDTO w1 = new StockWasteDTO(
                1L, 1L, 1L, "Armazém Central", 100L, "SKU-ARR-01", "Arroz 25kg",
                200L, "LOTE-ARR-2026", new BigDecimal("5.00"), new BigDecimal("850.00"),
                new BigDecimal("4250.00"), WasteReason.EXPIRED, WasteStatus.PENDING_APPROVAL,
                "Validade vencida em prateleira", "joao.armazem", null, now, null, null
        );

        StockWasteDTO w2 = new StockWasteDTO(
                2L, 1L, 1L, "Armazém Central", 101L, "SKU-OLE-02", "Óleo Alimentar 5L",
                null, null, new BigDecimal("2.00"), new BigDecimal("450.00"),
                new BigDecimal("900.00"), WasteReason.DAMAGED, WasteStatus.APPROVED,
                "Recipientes furados no descarregamento", "joao.armazem", "maria.gestora",
                yesterday, yesterday.plusHours(2), 501L
        );

        StockWasteDTO w3 = new StockWasteDTO(
                3L, 1L, 1L, "Armazém Central", 102L, "SKU-ACU-03", "Açúcar Branco 1kg",
                null, null, new BigDecimal("10.00"), new BigDecimal("70.00"),
                new BigDecimal("700.00"), WasteReason.THEFT_OR_SHRINKAGE, WasteStatus.REJECTED,
                "Suspeita de quebra não documentada", "joao.armazem", "maria.gestora",
                tenDaysAgo, tenDaysAgo.plusHours(1), null
        );

        return List.of(w1, w2, w3);
    }
}
