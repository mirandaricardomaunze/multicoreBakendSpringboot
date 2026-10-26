package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-CONFIG-BACKUPS-PANEL-DECOMPOSITION-001: Decomposição Modular de Backups")
class ConfigBackupsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("CBP-01: ConfigBackupsPanel.java reduzido para <= 115 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ConfigBackupsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ConfigBackupsPanel.java");
        }
        assertTrue(Files.exists(path), "ConfigBackupsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 115, "ConfigBackupsPanel.java deve possuir <= 115 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CBP-02: ConfigBackupOperations.java desacoplado com <= 125 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ConfigBackupOperations.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ConfigBackupOperations.java");
        }
        assertTrue(Files.exists(path), "ConfigBackupOperations.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 125, "ConfigBackupOperations.java deve possuir <= 125 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CBP-03: Erradicação absoluta de JOptionPane em ConfigBackupsPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "ConfigBackupsPanel.java",
                "ConfigBackupOperations.java"
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
    @DisplayName("CBP-04: Contrato de construtor e métodos preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                ConfigBackupsPanel.class.getDeclaredConstructor(ConfigPanel.class),
                "ConfigBackupsPanel deve possuir construtor com ConfigPanel");

        assertDoesNotThrow(() ->
                ConfigBackupsPanel.class.getDeclaredMethod("buildPanel"),
                "ConfigBackupsPanel deve possuir método buildPanel()");

        assertDoesNotThrow(() ->
                ConfigBackupsPanel.class.getDeclaredMethod("refresh"),
                "ConfigBackupsPanel deve possuir método refresh()");

        assertDoesNotThrow(() ->
                ConfigBackupsPanel.class.getDeclaredMethod("refreshAutoBackupStatus"),
                "ConfigBackupsPanel deve possuir método refreshAutoBackupStatus()");

        assertDoesNotThrow(() ->
                ConfigBackupsPanel.class.getDeclaredMethod("loadBackupFilesList"),
                "ConfigBackupsPanel deve possuir método loadBackupFilesList()");
    }
}
