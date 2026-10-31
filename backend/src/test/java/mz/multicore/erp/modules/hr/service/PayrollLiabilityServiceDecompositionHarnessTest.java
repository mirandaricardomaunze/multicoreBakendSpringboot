package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollLiabilityServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/hr/service");

    @Test
    void accrualDeliveryAndQueriesStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("PayrollLiabilityService.java"));
        String lifecycle = Files.readString(DIR.resolve("PayrollLiabilityLifecycleService.java"));
        String query = Files.readString(DIR.resolve("PayrollLiabilityQueryService.java"));

        assertTrue(facade.contains("lifecycleService.accrueForPeriod"));
        assertTrue(facade.contains("lifecycleService.markDelivered"));
        assertTrue(facade.contains("queryService.monthlyCost"));
        assertTrue(facade.contains("CurrentUserContext.requireCurrentCompanyId()"));
        assertFalse(facade.contains("PayrollLiabilityDeliveredEvent"));
        assertTrue(lifecycle.contains("financeService.registerAutoPayout"));
        assertTrue(query.contains("new PayrollCostDTO("));
        assertTrue(Files.readAllLines(DIR.resolve("PayrollLiabilityService.java")).size() <= 85);
        assertTrue(Files.readAllLines(DIR.resolve("PayrollLiabilityLifecycleService.java")).size() <= 165);
        assertTrue(Files.readAllLines(DIR.resolve("PayrollLiabilityQueryService.java")).size() <= 95);
    }
}
