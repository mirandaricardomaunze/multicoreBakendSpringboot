package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryCountServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/InventoryCountService.java");
    private static final Path LIFECYCLE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/InventoryCountLifecycleService.java");
    private static final Path QUERY_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/InventoryCountQueryService.java");

    @Test
    void inventoryCountServiceDelegatesToLifecycleAndQueryCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String lifecycleSource = Files.readString(LIFECYCLE_FILE);
        String querySource = Files.readString(QUERY_FILE);

        assertTrue(serviceSource.contains("lifecycleService.createSession("));
        assertTrue(serviceSource.contains("lifecycleService.applySession("));
        assertTrue(serviceSource.contains("queryService.listSessions("));
        assertFalse(serviceSource.contains("new InventoryCount()"));

        assertTrue(lifecycleSource.contains("INVENTORY_COUNT_APPLY"));
        assertTrue(querySource.contains("new InventoryCountDTO("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int lifecycleLines = Files.readAllLines(LIFECYCLE_FILE).size();
        int queryLines = Files.readAllLines(QUERY_FILE).size();

        assertTrue(serviceLines <= 85, "InventoryCountService deve ter <= 85 linhas, mas tem " + serviceLines);
        assertTrue(lifecycleLines <= 125, "InventoryCountLifecycleService deve ter <= 125 linhas, mas tem " + lifecycleLines);
        assertTrue(queryLines <= 85, "InventoryCountQueryService deve ter <= 85 linhas, mas tem " + queryLines);
    }
}
