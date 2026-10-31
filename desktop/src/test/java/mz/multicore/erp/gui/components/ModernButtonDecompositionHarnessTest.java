package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModernButtonDecompositionHarnessTest {

    @Test
    void preservesEllipsisStrippingShortcutBadgeAndContrast() {
        assertEquals("Nova Fatura", ModernButton.stripEllipsis("Nova Fatura..."));
        assertEquals("Exportar", ModernButton.stripEllipsis("Exportar…"));

        ModernButton btn = new ModernButton("Gravar...", Color.WHITE, Color.LIGHT_GRAY).setShortcut("F10");
        assertEquals("Gravar", btn.getText());
        assertEquals("F10", btn.getShortcut());
        assertTrue(btn.calculateShortcutBadgeWidth() > 20);
        assertTrue(UIHelper.contrastRatio(btn.getBackground(), btn.getForeground()) >= 3.0);
    }

    @Test
    void enforcesPhysicalLineLimits() throws IOException {
        Path base = resolveDesktopDir().resolve("src/main/java/mz/multicore/erp/gui/components");
        long facadeLines = Files.readAllLines(base.resolve("ModernButton.java")).size();
        long painterLines = Files.readAllLines(base.resolve("ModernButtonPainter.java")).size();

        assertTrue(facadeLines <= 230, "ModernButton.java deve ter <= 230 linhas, mas tem " + facadeLines);
        assertTrue(painterLines <= 190, "ModernButtonPainter.java deve ter <= 190 linhas, mas tem " + painterLines);
    }

    private static Path resolveDesktopDir() {
        Path cwd = Path.of("").toAbsolutePath();
        return (cwd.getFileName() != null && "desktop".equalsIgnoreCase(cwd.getFileName().toString()))
                ? cwd : cwd.resolve("desktop");
    }
}
