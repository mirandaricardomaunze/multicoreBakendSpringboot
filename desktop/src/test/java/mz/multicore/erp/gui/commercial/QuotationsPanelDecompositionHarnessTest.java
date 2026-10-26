package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HARNESS-QUOTATIONS-PANEL-DECOMPOSITION-001: Decomposição do Painel de Cotações")
class QuotationsPanelDecompositionHarnessTest {

    private ComercialApiClient comercialApiClient;
    private ProductionApiClient productionApiClient;

    @BeforeEach
    void setUp() {
        comercialApiClient = mock(ComercialApiClient.class);
        productionApiClient = mock(ProductionApiClient.class);
    }

    @Test
    @DisplayName("QD-01: QuotationsPanel.java reduzido para <= 270 linhas")
    void testLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        }
        assertTrue(Files.exists(path), "QuotationsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 270, "QuotationsPanel.java deve possuir <= 270 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("QD-02: QuotationOperationsCoordinator e QuotationViewCoordinator desacoplados")
    void testCoordinatorsDecoupled() throws IOException {
        Path basePath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial");
        if (!Files.exists(basePath)) {
            basePath = Path.of("src/main/java/mz/multicore/erp/gui/commercial");
        }

        Path opsPath = basePath.resolve("QuotationOperationsCoordinator.java");
        assertTrue(Files.exists(opsPath), "QuotationOperationsCoordinator.java deve existir");
        List<String> opsLines = Files.readAllLines(opsPath);
        assertTrue(opsLines.size() <= 230, "QuotationOperationsCoordinator.java deve possuir <= 230 linhas. Atual: " + opsLines.size());

        Path viewPath = basePath.resolve("QuotationViewCoordinator.java");
        assertTrue(Files.exists(viewPath), "QuotationViewCoordinator.java deve existir");
        List<String> viewLines = Files.readAllLines(viewPath);
        assertTrue(viewLines.size() <= 110, "QuotationViewCoordinator.java deve possuir <= 110 linhas. Atual: " + viewLines.size());
    }

    @Test
    @DisplayName("QD-03: Erradicação absoluta de JOptionPane em QuotationsPanel e coordenadores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "QuotationsPanel.java",
                "QuotationOperationsCoordinator.java",
                "QuotationViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/commercial");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> content = Files.readAllLines(filePath);
            for (int i = 0; i < content.size(); i++) {
                assertFalse(content.get(i).contains("JOptionPane"),
                        file + " contém JOptionPane na linha " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("QD-04: Construtores preservados e ações canónicas de conversão presentes")
    void testConstructorsAndConversionActions() {
        QuotationsPanel panel5Args = new QuotationsPanel(
                comercialApiClient, List::of, List::of, List::of, () -> {}
        );
        assertNotNull(panel5Args, "Deve instanciar com 5 argumentos");

        QuotationsPanel panel6Args = new QuotationsPanel(
                comercialApiClient, List::of, List::of, List::of, () -> {}, productionApiClient
        );
        assertNotNull(panel6Args, "Deve instanciar com 6 argumentos");
    }

    @Test
    @DisplayName("QD-05: Conformidade canónica de topo de card, QuickPeek e PrintPreviewDialog")
    void testCanonicalCardStructure() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop(\"Cotações\""),
                "Deve conter cabeçalho canónico tableCardTop(\"Cotações\"");
        assertTrue(content.contains("TableQuickPeekController.install(table"),
                "Deve instalar TableQuickPeekController com tecla Espaço");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(table)"),
                "Deve calibrar colunas com ensureHeadersFit");
        assertTrue(content.contains("PrintPreviewDialog.show("),
                "Deve manter PrintPreviewDialog.show no fluxo de impressão");
        assertTrue(content.contains("Converter em Factura"),
                "Deve conter acção de conversão em Factura");
        assertTrue(content.contains("Converter em Encomenda"),
                "Deve conter acção de conversão em Encomenda");
    }
}
