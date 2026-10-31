package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AboutDialogDecompositionHarnessTest {

    @Test
    void buildsAllFourAboutDialogTabsCleanly() {
        assertNotNull(AboutDialogTabBuilder.buildAuthorTab());
        assertNotNull(AboutDialogTabBuilder.buildModulesTab());
        assertNotNull(AboutDialogTabBuilder.buildArchitectureTab());
        assertNotNull(AboutDialogTabBuilder.buildLicenseTab());
    }

    @Test
    void enforcesPhysicalLineLimits() throws IOException {
        Path base = resolveDesktopDir().resolve("src/main/java/mz/multicore/erp/gui/components");
        long facadeLines = Files.readAllLines(base.resolve("AboutDialog.java")).size();
        long tabLines = Files.readAllLines(base.resolve("AboutDialogTabBuilder.java")).size();

        assertTrue(facadeLines <= 50, "AboutDialog.java deve ter <= 50 linhas, mas tem " + facadeLines);
        assertTrue(tabLines <= 275, "AboutDialogTabBuilder.java deve ter <= 275 linhas, mas tem " + tabLines);
    }

    private static Path resolveDesktopDir() {
        Path cwd = Path.of("").toAbsolutePath();
        return (cwd.getFileName() != null && "desktop".equalsIgnoreCase(cwd.getFileName().toString()))
                ? cwd : cwd.resolve("desktop");
    }
}
