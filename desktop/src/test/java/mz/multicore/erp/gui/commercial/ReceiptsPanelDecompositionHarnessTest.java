package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-RECEIPTS-PANEL-DECOMPOSITION-001: Decomposição Modular de Recibos")
class ReceiptsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("RCP-01: ReceiptsPanel.java reduzido para <= 115 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial/ReceiptsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/commercial/ReceiptsPanel.java");
        }
        assertTrue(Files.exists(path), "ReceiptsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 115, "ReceiptsPanel.java deve possuir <= 115 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("RCP-02: Colaboradores desacoplados dentro dos limites de linhas")
    void testCollaboratorLineCounts() throws IOException {
        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/commercial");
        }

        Path dialogPath = baseDir.resolve("ReceiptPaymentDialog.java");
        assertTrue(Files.exists(dialogPath), "ReceiptPaymentDialog.java deve existir");
        List<String> dialogLines = Files.readAllLines(dialogPath);
        assertTrue(dialogLines.size() <= 85, "ReceiptPaymentDialog.java deve possuir <= 85 linhas. Atual: " + dialogLines.size());

        Path coordPath = baseDir.resolve("ReceiptsViewCoordinator.java");
        assertTrue(Files.exists(coordPath), "ReceiptsViewCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 85, "ReceiptsViewCoordinator.java deve possuir <= 85 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("RCP-03: Erradicação absoluta de JOptionPane em ReceiptsPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "ReceiptsPanel.java",
                "ReceiptPaymentDialog.java",
                "ReceiptsViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/commercial");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/commercial");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assertFalse(line.contains("JOptionPane"),
                        "Uso proibido de JOptionPane em " + file + " na linha " + (i + 1) + ": " + line);
            }
        }
    }

    @Test
    @DisplayName("RCP-04: Contrato de construtor e métodos públicos preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                ReceiptsPanel.class.getDeclaredConstructor(
                        ComercialApiClient.class,
                        Runnable.class
                ), "ReceiptsPanel deve possuir construtor com ComercialApiClient e Runnable");

        assertDoesNotThrow(() ->
                ReceiptsPanel.class.getDeclaredMethod("openPayment", Long.class, String.class, BigDecimal.class, List.class),
                "ReceiptsPanel deve possuir método openPayment(Long, String, BigDecimal, List)");

        assertDoesNotThrow(() ->
                ReceiptsPanel.class.getDeclaredMethod("refresh"),
                "ReceiptsPanel deve possuir método refresh()");
    }
}
