package mz.multicore.erp.gui.pos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação estrutural do diálogo de movimentos de caixa no POS (Sangria e Suprimento).
 */
class PosCashMovementsHarnessTest {

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    @Test
    @DisplayName("PCM-01: PosCashMovementDialog implementa validações e selecção sem emojis")
    void testPosCashMovementDialogStructure() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/pos/PosCashMovementDialog.java");
        assertTrue(Files.exists(path), "PosCashMovementDialog.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("SUPRIMENTO"), "Deve suportar SUPRIMENTO");
        assertTrue(content.contains("SANGRIA"), "Deve suportar SANGRIA");
        assertTrue(content.contains("MoneyField"), "Deve usar MoneyField");
        assertTrue(content.contains("descField"), "Deve conter campo de justificação/motivo");
        assertFalse(content.contains("⭐") || content.contains("✅") || content.contains("❌"), "Sem emojis");
    }

    @Test
    @DisplayName("PCM-02: PosCashSessionActions invoca PosCashMovementDialog.show")
    void testPosCashSessionActionsIntegration() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/PosCashSessionActions.java");
        assertTrue(Files.exists(path), "PosCashSessionActions.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("PosCashMovementDialog.show"), "Deve chamar PosCashMovementDialog.show");
    }
}
