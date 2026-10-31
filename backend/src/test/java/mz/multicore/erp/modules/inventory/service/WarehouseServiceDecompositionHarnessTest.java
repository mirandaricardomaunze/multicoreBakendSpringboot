package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/WarehouseService.java");
    private static final Path LIFECYCLE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/WarehouseLifecycleService.java");
    private static final Path QUERY_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/WarehouseQueryService.java");

    @Test
    void warehouseServiceDelegatesToLifecycleAndQueryCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String lifecycleSource = Files.readString(LIFECYCLE_FILE);
        String querySource = Files.readString(QUERY_FILE);

        assertTrue(serviceSource.contains("lifecycleService.updateWarehouse("));
        assertTrue(serviceSource.contains("queryService.getWarehousesByCompany("));
        assertFalse(serviceSource.contains("new Warehouse()"));

        assertTrue(lifecycleSource.contains("WAREHOUSE_UPDATE"));
        assertTrue(querySource.contains("new WarehouseDTO("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int lifecycleLines = Files.readAllLines(LIFECYCLE_FILE).size();
        int queryLines = Files.readAllLines(QUERY_FILE).size();

        assertTrue(serviceLines <= 135, "WarehouseService deve ter <= 135 linhas, mas tem " + serviceLines);
        assertTrue(lifecycleLines <= 125, "WarehouseLifecycleService deve ter <= 125 linhas, mas tem " + lifecycleLines);
        assertTrue(queryLines <= 95, "WarehouseQueryService deve ter <= 95 linhas, mas tem " + queryLines);
    }
}
