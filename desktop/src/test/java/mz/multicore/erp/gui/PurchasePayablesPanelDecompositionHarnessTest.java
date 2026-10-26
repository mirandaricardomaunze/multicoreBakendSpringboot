package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-PURCHASE-PAYABLES-PANEL-DECOMPOSITION-001: Decomposição do Painel de Contas a Pagar")
class PurchasePayablesPanelDecompositionHarnessTest {

    @Test
    @DisplayName("PPP-01: PurchasePayablesPanel.java reduzido para <= 115 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/PurchasePayablesPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/PurchasePayablesPanel.java");
        }
        assertTrue(Files.exists(path), "PurchasePayablesPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 115, "PurchasePayablesPanel.java deve possuir <= 115 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("PPP-02: Colaboradores desacoplados com limites rigorosos")
    void testCollaboratorsLineCount() throws IOException {
        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        Path dialogPath = baseDir.resolve("SupplierPaymentDialog.java");
        assertTrue(Files.exists(dialogPath), "SupplierPaymentDialog.java deve existir");
        List<String> dialogLines = Files.readAllLines(dialogPath);
        assertTrue(dialogLines.size() <= 95, "SupplierPaymentDialog.java deve possuir <= 95 linhas. Atual: " + dialogLines.size());

        Path coordPath = baseDir.resolve("PurchasePayablesViewCoordinator.java");
        assertTrue(Files.exists(coordPath), "PurchasePayablesViewCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 110, "PurchasePayablesViewCoordinator.java deve possuir <= 110 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("PPP-03: Erradicação absoluta de JOptionPane em PurchasePayablesPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "PurchasePayablesPanel.java",
                "SupplierPaymentDialog.java",
                "PurchasePayablesViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
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
    @DisplayName("PPP-04: Contrato canónico de construtor e métodos operacionais preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                PurchasePayablesPanel.class.getDeclaredConstructor(ComprasPanel.class),
                "PurchasePayablesPanel deve possuir construtor com ComprasPanel");
        assertDoesNotThrow(() ->
                PurchasePayablesPanel.class.getDeclaredMethod("buildPanel"),
                "PurchasePayablesPanel deve possuir método buildPanel()");
        assertDoesNotThrow(() ->
                PurchasePayablesPanel.class.getDeclaredMethod("refresh"),
                "PurchasePayablesPanel deve possuir método refresh()");
    }
}
