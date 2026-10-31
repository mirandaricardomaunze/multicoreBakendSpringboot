package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TableFilterDecompositionHarnessTest {

    @Test
    void delegatesMatchingAndFactoryMethodsCorrectly() {
        assertTrue(TableFilter.cellMatches("PENDING", "Por faturar"));
        assertTrue(TableFilter.rowMatches(List.of("FT-2026/001", "Cliente A", "PAID"), "paga", Map.of(2, "Paga")));
        assertEquals(LocalDate.of(2026, 10, 30), TableFilter.parseCellDate("30/10/2026 14:20"));
        assertNotNull(TableFilter.periodCombo());
    }

    @Test
    void enforcesPhysicalLineLimits() throws IOException {
        Path base = resolveDesktopDir().resolve("src/main/java/mz/multicore/erp/gui/components");
        long facadeLines = Files.readAllLines(base.resolve("TableFilter.java")).size();
        long matcherLines = Files.readAllLines(base.resolve("TableFilterMatcher.java")).size();
        long barLines = Files.readAllLines(base.resolve("TableFilterBarFactory.java")).size();

        assertTrue(facadeLines <= 145, "TableFilter.java deve ter <= 145 linhas, mas tem " + facadeLines);
        assertTrue(matcherLines <= 125, "TableFilterMatcher.java deve ter <= 125 linhas, mas tem " + matcherLines);
        assertTrue(barLines <= 215, "TableFilterBarFactory.java deve ter <= 215 linhas, mas tem " + barLines);
    }

    private static Path resolveDesktopDir() {
        Path cwd = Path.of("").toAbsolutePath();
        return (cwd.getFileName() != null && "desktop".equalsIgnoreCase(cwd.getFileName().toString()))
                ? cwd : cwd.resolve("desktop");
    }
}
