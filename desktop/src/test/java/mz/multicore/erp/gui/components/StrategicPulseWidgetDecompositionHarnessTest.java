package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategicPulseWidgetDecompositionHarnessTest {

    @Test
    void appliesFallbackAndDelegatesToPresenterAndCardFactory() {
        StrategicPulseWidget widget = new StrategicPulseWidget(null, null, null, null, card -> {});
        widget.applyData(null, null, null, List.of());

        assertEquals("100.0%", widget.getForensicScoreLabel().getText());
        assertEquals("0,00 MT", widget.getLiquidityValLabel().getText());
        assertEquals("0,00 MT", widget.getCreditRiskValLabel().getText());
        assertEquals("100.0%", widget.getPerformanceValLabel().getText());
    }

    @Test
    void enforcesPhysicalLineLimits() throws IOException {
        Path base = resolveDesktopDir().resolve("src/main/java/mz/multicore/erp/gui/components");
        long facadeLines = Files.readAllLines(base.resolve("StrategicPulseWidget.java")).size();
        long cardLines = Files.readAllLines(base.resolve("StrategicPulseCardFactory.java")).size();
        long presenterLines = Files.readAllLines(base.resolve("StrategicPulseDataPresenter.java")).size();

        assertTrue(facadeLines <= 165, "StrategicPulseWidget.java deve ter <= 165 linhas, mas tem " + facadeLines);
        assertTrue(cardLines <= 115, "StrategicPulseCardFactory.java deve ter <= 115 linhas, mas tem " + cardLines);
        assertTrue(presenterLines <= 130, "StrategicPulseDataPresenter.java deve ter <= 130 linhas, mas tem " + presenterLines);
    }

    private static Path resolveDesktopDir() {
        Path cwd = Path.of("").toAbsolutePath();
        return (cwd.getFileName() != null && "desktop".equalsIgnoreCase(cwd.getFileName().toString()))
                ? cwd : cwd.resolve("desktop");
    }
}
