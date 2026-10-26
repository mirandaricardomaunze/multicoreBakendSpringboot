package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-CONFIG-SUPPORT-PANEL-DECOMPOSITION-001: Decomposição Modular de Suporte")
class ConfigSupportPanelDecompositionHarnessTest {

    @Test
    @DisplayName("CSP-01: ConfigSupportPanel.java reduzido para <= 105 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ConfigSupportPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ConfigSupportPanel.java");
        }
        assertTrue(Files.exists(path), "ConfigSupportPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 105, "ConfigSupportPanel.java deve possuir <= 105 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CSP-02: ConfigSupportTicketDialog.java desacoplado com <= 90 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ConfigSupportTicketDialog.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ConfigSupportTicketDialog.java");
        }
        assertTrue(Files.exists(path), "ConfigSupportTicketDialog.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 90, "ConfigSupportTicketDialog.java deve possuir <= 90 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CSP-03: Erradicação absoluta de JOptionPane em ConfigSupportPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "ConfigSupportPanel.java",
                "ConfigSupportTicketDialog.java"
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
    @DisplayName("CSP-04: Contrato de construtor e métodos preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                ConfigSupportPanel.class.getDeclaredConstructor(ConfigPanel.class),
                "ConfigSupportPanel deve possuir construtor com ConfigPanel");

        assertDoesNotThrow(() ->
                ConfigSupportPanel.class.getDeclaredMethod("buildPanel"),
                "ConfigSupportPanel deve possuir método buildPanel()");

        assertDoesNotThrow(() ->
                ConfigSupportPanel.class.getDeclaredMethod("refresh"),
                "ConfigSupportPanel deve possuir método refresh()");
    }
}
