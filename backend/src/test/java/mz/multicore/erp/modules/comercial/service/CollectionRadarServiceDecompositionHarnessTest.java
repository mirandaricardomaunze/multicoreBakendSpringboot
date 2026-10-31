package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionRadarServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/comercial/service");

    @Test
    void radarAnalysisAndReminderTemplatesStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("CollectionRadarService.java"));
        String analyzer = Files.readString(DIR.resolve("CollectionRadarAnalyzer.java"));
        String builder = Files.readString(DIR.resolve("CollectionReminderTemplateBuilder.java"));

        assertTrue(facade.contains("radarAnalyzer.buildSummary"));
        assertTrue(facade.contains("radarAnalyzer.buildItems"));
        assertTrue(facade.contains("templateBuilder.build(invoice, channel)"));
        assertFalse(facade.contains("CollectionRadarBucket.of"));
        assertTrue(analyzer.contains("CollectionRadarBucket.of"));
        assertTrue(builder.contains("new CollectionReminderTemplateDTO("));
        assertTrue(Files.readAllLines(DIR.resolve("CollectionRadarService.java")).size() <= 70);
        assertTrue(Files.readAllLines(DIR.resolve("CollectionRadarAnalyzer.java")).size() <= 125);
        assertTrue(Files.readAllLines(DIR.resolve("CollectionReminderTemplateBuilder.java")).size() <= 130);
    }
}
