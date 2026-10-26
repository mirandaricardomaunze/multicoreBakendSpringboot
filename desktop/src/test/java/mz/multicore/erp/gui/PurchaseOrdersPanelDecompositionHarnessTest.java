package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.CardLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Decomposição de PurchaseOrdersPanel (Fase 13)")
class PurchaseOrdersPanelDecompositionHarnessTest {

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
        Path panelPath = resolveGui("PurchaseOrdersPanel.java");
        Path editorPath = resolveGui("PurchaseOrderEditorCoordinator.java");
        Path opsPath = resolveGui("PurchaseOrderOperationsCoordinator.java");

        assertTrue(Files.exists(panelPath), "PurchaseOrdersPanel.java deve existir");
        assertTrue(Files.exists(editorPath), "PurchaseOrderEditorCoordinator.java deve existir");
        assertTrue(Files.exists(opsPath), "PurchaseOrderOperationsCoordinator.java deve existir");

        long panelLines = Files.lines(panelPath).count();
        long editorLines = Files.lines(editorPath).count();
        long opsLines = Files.lines(opsPath).count();

        assertTrue(panelLines <= 380, "PurchaseOrdersPanel.java excedeu 380 linhas (actual: " + panelLines + ")");
        assertTrue(editorLines <= 280, "PurchaseOrderEditorCoordinator.java excedeu 280 linhas (actual: " + editorLines + ")");
        assertTrue(opsLines <= 250, "PurchaseOrderOperationsCoordinator.java excedeu 250 linhas (actual: " + opsLines + ")");
    }

    @Test
    @DisplayName("DECOMP-02: Zero diálogos legados JOptionPane nos componentes de encomendas")
    void decomp02_zeroLegacyDialogs() throws IOException {
        List<String> files = List.of(
                "PurchaseOrdersPanel.java",
                "PurchaseOrderEditorCoordinator.java",
                "PurchaseOrderOperationsCoordinator.java"
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
    @DisplayName("DECOMP-03: Contratos canónicos e homologação PHC preservados")
    void decomp03_canonicalContractsPreserved() throws IOException {
        Path panelPath = resolveGui("PurchaseOrdersPanel.java");
        String content = Files.readString(panelPath);

        // Expectativas de homologação PHC
        assertTrue(content.contains("stopPoCellEditing()"), "Deve conter stopPoCellEditing()");
        assertTrue(content.contains("for (int row = 0; row < owner.poDraftLines.size(); row++)"), "Deve conter validação for poDraftLines");
        assertTrue(content.contains("packagesPerBox()") && content.contains("unitsPerPackage()") && content.contains("multiply(unitsPerBox)"),
                "Deve conter cálculo multinível de embalagem");
        assertFalse(content.contains("editingLineIndex") || content.contains("editSelectedDraftLine") || content.contains("editLineButton"),
                "Não pode conter fluxo legado de transporte de linha");

        // Expectativas de PurchasesUnifiedHarnessTest
        assertTrue(content.contains("TableFilter.bar("));
        assertTrue(content.contains("UIHelper.tableCardTop("));
        assertTrue(content.contains("TableQuickPeekController.install(poListTable"));
        assertTrue(content.contains("UIHelper.ensureHeadersFit(poListTable)"));
        assertFalse(content.contains("poFilters = new JPanel(new GridBagLayout())"));
        assertFalse(content.contains("\"Nova Encomenda…\"") || content.contains("\"Receber Parcial…\""));

        // Expectativas de impressão de etiquetas de recepção
        assertTrue(content.contains("Imprimir Etiquetas"));
        assertTrue(content.contains("extractReceivedProducts"));
        assertFalse(content.contains("⭐") || content.contains("✅") || content.contains("❌"));

        // Validação estática de extractReceivedProducts
        PurchaseOrderLineDTO l1 = new PurchaseOrderLineDTO(1L, 301L, "Rato Óptico USB", "RAT-01",
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("150.00"),
                new BigDecimal("16.00"), new BigDecimal("1500.00"), null, null, null);
        PurchaseOrderDTO order = new PurchaseOrderDTO(
                1L, "PO-2026-0001", 1L, "Fornecedor Tech", 1L, 1L, LocalDate.now(),
                LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("1500.00"), new BigDecimal("1500.00"),
                "RECEIVED", "Entregue", List.of(l1));

        List<ProductDTO> products = PurchaseOrdersPanel.extractReceivedProducts(order);
        assertNotNull(products);
        assertEquals(1, products.size());
        assertEquals("RAT-01", products.get(0).sku());
    }

    @Test
    @DisplayName("DECOMP-04: Estrutura do painel e páginas em CardLayout")
    void decomp04_panelStructureAndCards() {
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient inventoryApi = Mockito.mock(InventoryApiClient.class);
        FinanceApiClient financeApi = Mockito.mock(FinanceApiClient.class);
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);

        ComprasPanel owner = new ComprasPanel(purchaseApi, inventoryApi, comercialApi, financeApi);
        PurchaseOrdersPanel panel = new PurchaseOrdersPanel(owner);

        JPanel built = panel.buildPanel();
        assertNotNull(built, "buildPanel() deve retornar um JPanel válido");
        assertTrue(built.getLayout() instanceof CardLayout, "O painel principal de encomendas deve usar CardLayout (LIST / EDITOR)");
    }

    @Test
    @DisplayName("DECOMP-05: Ciclo de vida e refresh assíncrono seguro")
    void decomp05_lifecycleExecution() {
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient inventoryApi = Mockito.mock(InventoryApiClient.class);
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        FinanceApiClient financeApi = Mockito.mock(FinanceApiClient.class);

        Mockito.when(purchaseApi.findOrdersByCompany(Mockito.any())).thenReturn(Collections.emptyList());

        try {
            CurrentUserContext.setCurrentCompanyId(1L);
            CurrentUserContext.setCurrentUser("admin", "ADMIN");

            ComprasPanel owner = new ComprasPanel(purchaseApi, inventoryApi, comercialApi, financeApi);
            PurchaseOrdersPanel panel = new PurchaseOrdersPanel(owner);
            panel.buildPanel();

            assertDoesNotThrow(panel::refresh, "refresh() deve executar sem NPE");
            assertDoesNotThrow(panel::refreshCombos, "refreshCombos() deve executar sem NPE");
        } finally {
            CurrentUserContext.clear();
        }
    }
}
