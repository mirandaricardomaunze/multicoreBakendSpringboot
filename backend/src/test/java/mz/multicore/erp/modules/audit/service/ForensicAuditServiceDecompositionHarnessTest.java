package mz.multicore.erp.modules.audit.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAuditServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/audit/service");

    @Test
    void anomalyCollectionAndEvaluationStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("ForensicAuditService.java"));
        String collector = Files.readString(DIR.resolve("ForensicAnomalyCollector.java"));
        String evaluator = Files.readString(DIR.resolve("ForensicSummaryEvaluator.java"));

        assertTrue(facade.contains("anomalyCollector.collectAll(companyId)"));
        assertTrue(facade.contains("summaryEvaluator.evaluate("));
        assertTrue(facade.contains("PermissionGuard.requireManagerOrAdmin"));
        assertFalse(facade.contains("ForensicCategory.DOC_CANCELLATION"));
        assertTrue(collector.contains("ForensicCategory.EXCESSIVE_DISCOUNT"));
        assertTrue(evaluator.contains("new ForensicAuditSummaryDTO("));
        assertTrue(Files.readAllLines(DIR.resolve("ForensicAuditService.java")).size() <= 80);
        assertTrue(Files.readAllLines(DIR.resolve("ForensicAnomalyCollector.java")).size() <= 155);
        assertTrue(Files.readAllLines(DIR.resolve("ForensicSummaryEvaluator.java")).size() <= 85);
    }
}
