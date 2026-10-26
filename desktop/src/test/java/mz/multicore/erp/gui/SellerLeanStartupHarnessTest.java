package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness do arranque enxuto do Vendedor (SLS-01 a SLS-04)")
class SellerLeanStartupHarnessTest {

    private static String mainFrameSource() throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/MainFrame.java"));
    }

    @Test
    @DisplayName("SLS-01: SELLER não constrói painéis de gestão")
    void sellerDoesNotConstructManagementPanels() throws Exception {
        String source = mainFrameSource();
        assertTrue(source.contains("stockPanel = sellerSession ? null : new StockPanel"));
        assertTrue(source.contains("financeiroPanel = sellerSession ? null : new FinanceiroPanel"));
        assertTrue(source.contains("hrPanel         = sellerSession ? null : new HRPanel"));
        assertTrue(source.contains("configPanel     = sellerSession ? null : new ConfigPanel"));
        assertTrue(source.contains("forensicAuditPanel = sellerSession ? null : new ForensicAuditPanel"));
        assertTrue(source.contains("aiAssistantPanel = sellerSession ? null : new AiManagementAssistantPanel"));
    }

    @Test
    @DisplayName("SLS-02: SELLER mantém apenas os módulos operacionais autorizados")
    void sellerKeepsAuthorizedOperationalPanels() throws Exception {
        String source = mainFrameSource();
        assertTrue(source.contains("new SellerDashboardPanel"));
        assertTrue(source.contains("comercialPanel  = new ComercialPanel"));
        assertTrue(source.contains("crmPanel        = new CRMPanel"));
        assertTrue(source.contains("clientesPanel   = new ClientesPanel"));
        assertTrue(source.contains("posPanel        = new POSPanel"));
        assertFalse(source.contains("posPanel        = sellerSession ? null"));
    }

    @Test
    @DisplayName("SLS-03: CardLayout ignora painéis não construídos")
    void cardLayoutIgnoresMissingPanels() throws Exception {
        String source = mainFrameSource();
        assertTrue(source.contains("private void addCard(JComponent panel, String name)"));
        assertTrue(source.contains("if (panel != null) contentPanel.add(panel, name);"));
        assertFalse(source.contains("contentPanel.add(stockPanel, \"stock\")"));
        assertFalse(source.contains("contentPanel.add(financeiroPanel, \"financeiro\")"));
    }

    @Test
    @DisplayName("SLS-04: callbacks opcionais verificam a existência do painel")
    void optionalCallbacksAreNullSafe() throws Exception {
        String source = mainFrameSource();
        assertTrue(source.contains("if (stockPanel != null) stockPanel.showWarehouseTransfers();"));
    }
}
