package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Decomposição de ComprasPanel (Fase 9)")
class ComprasPanelDecompositionHarnessTest {

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
        Path mainPath = resolveGui("ComprasPanel.java");
        Path metaPath = resolveGui("PurchaseMetadataCoordinator.java");
        Path opsPath = resolveGui("PurchaseInvoiceOperations.java");

        assertTrue(Files.exists(mainPath), "ComprasPanel.java deve existir");
        assertTrue(Files.exists(metaPath), "PurchaseMetadataCoordinator.java deve existir");
        assertTrue(Files.exists(opsPath), "PurchaseInvoiceOperations.java deve existir");

        long mainLines = Files.lines(mainPath).count();
        long metaLines = Files.lines(metaPath).count();
        long opsLines = Files.lines(opsPath).count();

        assertTrue(mainLines <= 450, "ComprasPanel.java excedeu o teto de 450 linhas (actual: " + mainLines + ")");
        assertTrue(metaLines <= 160, "PurchaseMetadataCoordinator.java excedeu o teto de 160 linhas (actual: " + metaLines + ")");
        assertTrue(opsLines <= 190, "PurchaseInvoiceOperations.java excedeu o teto de 190 linhas (actual: " + opsLines + ")");
    }

    @Test
    @DisplayName("DECOMP-02: Zero diálogos legados JOptionPane nos componentes de compras")
    void decomp02_zeroLegacyDialogs() throws IOException {
        List<String> files = List.of(
                "ComprasPanel.java",
                "PurchaseMetadataCoordinator.java",
                "PurchaseInvoiceOperations.java"
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
    @DisplayName("DECOMP-03: Estrutura canónica das abas corporativas preservada")
    void decomp03_tabStructurePreserved() {
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);
        FinanceApiClient finApi = Mockito.mock(FinanceApiClient.class);
        AccountStatementApiClient stmtApi = Mockito.mock(AccountStatementApiClient.class);

        ComprasPanel panel = new ComprasPanel(purchaseApi, invApi, comApi, finApi, stmtApi);

        assertNotNull(panel.tabbedPane, "JTabbedPane deve ser instanciado");
        assertEquals(7, panel.tabbedPane.getTabCount(), "Todas as 7 abas de compras devem estar presentes");
        assertEquals("Faturas de Compra (V/FT)", panel.tabbedPane.getTitleAt(0));
        assertEquals("Reposição", panel.tabbedPane.getTitleAt(1));
        assertEquals("Encomendas a Fornecedor (EC-F)", panel.tabbedPane.getTitleAt(2));
        assertEquals("Contas a Pagar", panel.tabbedPane.getTitleAt(3));
        assertEquals("Gestão de Fornecedores", panel.tabbedPane.getTitleAt(4));
        assertEquals("Gestão de Negócios", panel.tabbedPane.getTitleAt(5));
        assertEquals("Conta Corrente & Reconciliação", panel.tabbedPane.getTitleAt(6));
    }

    @Test
    @DisplayName("DECOMP-04: Editor canónico de embalagem preservado no formulário de compra")
    void decomp04_packageEditorPreserved() {
        PurchaseApiClient purchaseApi = Mockito.mock(PurchaseApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);
        FinanceApiClient finApi = Mockito.mock(FinanceApiClient.class);

        ComprasPanel panel = new ComprasPanel(purchaseApi, invApi, comApi, finApi);

        assertNotNull(panel.purchasePackageEditor, "PackageQuantityEditor deve existir em ComprasPanel");
        assertNotNull(panel.quantityField, "quantityField deve estar inicializado");
        assertNotNull(panel.priceField, "priceField deve estar inicializado");
        assertNotNull(panel.purchaseVatField, "purchaseVatField deve estar inicializado");
    }
}
