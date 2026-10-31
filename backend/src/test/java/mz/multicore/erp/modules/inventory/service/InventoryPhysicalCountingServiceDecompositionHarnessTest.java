package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryPhysicalCountingServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/inventory/service");

    @Test
    void countingLifecycleAndQueriesStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("InventoryPhysicalCountingService.java"));
        String lifecycle = Files.readString(DIR.resolve("InventoryCountingLifecycleService.java"));
        String query = Files.readString(DIR.resolve("InventoryCountingQueryService.java"));

        assertTrue(facade.contains("lifecycleService.createSession"));
        assertTrue(facade.contains("lifecycleService.closeAndAdjustStock"));
        assertTrue(facade.contains("queryService.listSessions"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("new InventoryPhysicalItem()"));
        assertTrue(lifecycle.contains("inventoryService.registerMovement"));
        assertTrue(query.contains("new InventorySessionDTO("));
        assertTrue(Files.readAllLines(DIR.resolve("InventoryPhysicalCountingService.java")).size() <= 90);
        assertTrue(Files.readAllLines(DIR.resolve("InventoryCountingLifecycleService.java")).size() <= 195);
        assertTrue(Files.readAllLines(DIR.resolve("InventoryCountingQueryService.java")).size() <= 90);
    }
}
