package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-PURCHASE-SUPPLIERS-PANEL-DECOMPOSITION-001: Decomposição do Painel de Fornecedores")
class PurchaseSuppliersPanelDecompositionHarnessTest {

    @Test
    @DisplayName("PSP-01: PurchaseSuppliersPanel.java reduzido para <= 95 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/PurchaseSuppliersPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/PurchaseSuppliersPanel.java");
        }
        assertTrue(Files.exists(path), "PurchaseSuppliersPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 95, "PurchaseSuppliersPanel.java deve possuir <= 95 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("PSP-02: Colaboradores desacoplados com limites rigorosos")
    void testCollaboratorsLineCount() throws IOException {
        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        Path formPath = baseDir.resolve("SupplierFormDialog.java");
        assertTrue(Files.exists(formPath), "SupplierFormDialog.java deve existir");
        List<String> formLines = Files.readAllLines(formPath);
        assertTrue(formLines.size() <= 75, "SupplierFormDialog.java deve possuir <= 75 linhas. Atual: " + formLines.size());

        Path coordPath = baseDir.resolve("PurchaseSuppliersViewCoordinator.java");
        assertTrue(Files.exists(coordPath), "PurchaseSuppliersViewCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 80, "PurchaseSuppliersViewCoordinator.java deve possuir <= 80 linhas. Atual: " + coordLines.size());
    }

    @Test
    @DisplayName("PSP-03: Erradicação absoluta de JOptionPane em PurchaseSuppliersPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "PurchaseSuppliersPanel.java",
                "SupplierFormDialog.java",
                "PurchaseSuppliersViewCoordinator.java"
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
    @DisplayName("PSP-04: Contrato de construtor e método buildPanel() preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                PurchaseSuppliersPanel.class.getDeclaredConstructor(ComprasPanel.class),
                "PurchaseSuppliersPanel deve possuir construtor com ComprasPanel");
        assertDoesNotThrow(() ->
                PurchaseSuppliersPanel.class.getDeclaredMethod("buildPanel"),
                "PurchaseSuppliersPanel deve possuir método buildPanel()");
    }
}
