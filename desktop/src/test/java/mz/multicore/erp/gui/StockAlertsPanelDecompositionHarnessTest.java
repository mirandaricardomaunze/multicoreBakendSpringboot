package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-STOCK-ALERTS-PANEL-DECOMPOSITION-001: Decomposição do Painel de Alertas de Stock")
class StockAlertsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("SAP-01: StockAlertsPanel.java reduzido para <= 165 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/StockAlertsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/StockAlertsPanel.java");
        }
        assertTrue(Files.exists(path), "StockAlertsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 165, "StockAlertsPanel.java deve possuir <= 165 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("SAP-02: StockAlertsViewCoordinator.java desacoplado com <= 130 linhas")
    void testCoordinatorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/StockAlertsViewCoordinator.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/StockAlertsViewCoordinator.java");
        }
        assertTrue(Files.exists(path), "StockAlertsViewCoordinator.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 130, "StockAlertsViewCoordinator.java deve possuir <= 130 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("SAP-03: Erradicação absoluta de JOptionPane em StockAlertsPanel e StockAlertsViewCoordinator")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "StockAlertsPanel.java",
                "StockAlertsViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
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
    @DisplayName("SAP-04: Construtor package-private preservado")
    void testConstructorPreserved() throws NoSuchMethodException {
        assertNotNull(StockAlertsPanel.class.getDeclaredConstructor(StockPanel.class));
    }

    @Test
    @DisplayName("SAP-05: Invariantes canónicas de layout e filtros preservadas")
    void testCanonicalInvariants() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/StockAlertsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/StockAlertsPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop(\"Lotes Expirados e a Expirar\""),
                "Deve conter cabeçalho canónico tableCardTop(\"Lotes Expirados e a Expirar\"");
        assertTrue(content.contains("UIHelper.tableCardTop(\"Produtos Esgotados\""),
                "Deve conter cabeçalho canónico tableCardTop(\"Produtos Esgotados\"");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(owner.alertsExpTable)"),
                "Deve calibrar colunas da tabela de expirados");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(owner.alertsOutTable)"),
                "Deve calibrar colunas da tabela de esgotados");
        assertTrue(content.contains("TableFilter.bar("),
                "Deve conter barra horizontal TableFilter.bar");
        assertFalse(content.contains("new JPanel(new GridBagLayout())"),
                "Proibido usar GridBagLayout em filtros");
    }
}
