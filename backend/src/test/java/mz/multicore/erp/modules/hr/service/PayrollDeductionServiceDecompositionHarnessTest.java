package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PayrollDeductionServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/hr/service");

    @Test
    void responsibilitiesRemainSeparated() throws Exception {
        String facade = Files.readString(DIR.resolve("PayrollDeductionService.java"));
        assertTrue(facade.lines().count() <= 140);
        assertTrue(facade.contains("applicationService.applyTo("));
        assertTrue(facade.contains("queryService.outstandingFor("));
        assertTrue(facade.contains("PayrollDeductionPolicy.validate("));
        assertTrue(Files.readAllLines(DIR.resolve("PayrollDeductionApplicationService.java")).size() <= 85);
        assertTrue(Files.readAllLines(DIR.resolve("PayrollDeductionQueryService.java")).size() <= 70);
        assertTrue(Files.readAllLines(DIR.resolve("PayrollDeductionPolicy.java")).size() <= 55);
    }
}
