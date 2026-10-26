package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-CONFIG-DOCUMENT-COLUMNS-PANEL-DECOMPOSITION-001: Decomposição Modular de Colunas de Documentos")
class ConfigDocumentColumnsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("CDC-01: ConfigDocumentColumnsPanel.java reduzido para <= 125 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ConfigDocumentColumnsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ConfigDocumentColumnsPanel.java");
        }
        assertTrue(Files.exists(path), "ConfigDocumentColumnsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 125, "ConfigDocumentColumnsPanel.java deve possuir <= 125 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CDC-02: ConfigDocumentColumnsOperations.java desacoplado com <= 105 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ConfigDocumentColumnsOperations.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ConfigDocumentColumnsOperations.java");
        }
        assertTrue(Files.exists(path), "ConfigDocumentColumnsOperations.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 105, "ConfigDocumentColumnsOperations.java deve possuir <= 105 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CDC-03: Erradicação absoluta de JOptionPane em ConfigDocumentColumnsPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "ConfigDocumentColumnsPanel.java",
                "ConfigDocumentColumnsOperations.java"
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
    @DisplayName("CDC-04: Contrato de construtor e métodos preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                ConfigDocumentColumnsPanel.class.getDeclaredConstructor(ConfigPanel.class),
                "ConfigDocumentColumnsPanel deve possuir construtor com ConfigPanel");

        assertDoesNotThrow(() ->
                ConfigDocumentColumnsPanel.class.getDeclaredMethod("buildPanel"),
                "ConfigDocumentColumnsPanel deve possuir método buildPanel()");

        assertDoesNotThrow(() ->
                ConfigDocumentColumnsPanel.class.getDeclaredMethod("refresh"),
                "ConfigDocumentColumnsPanel deve possuir método refresh()");

        assertDoesNotThrow(() ->
                ConfigDocumentColumnsPanel.class.getDeclaredMethod("loadDocumentColumns"),
                "ConfigDocumentColumnsPanel deve possuir método loadDocumentColumns()");
    }
}
