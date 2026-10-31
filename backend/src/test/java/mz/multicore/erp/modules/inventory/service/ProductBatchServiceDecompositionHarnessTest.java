package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductBatchServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/ProductBatchService.java");
    private static final Path MUTATION_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/ProductBatchMutationService.java");
    private static final Path QUERY_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/inventory/service/ProductBatchQueryService.java");

    @Test
    void productBatchServiceDelegatesToMutationAndQueryCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String mutationSource = Files.readString(MUTATION_FILE);
        String querySource = Files.readString(QUERY_FILE);

        assertTrue(serviceSource.contains("mutationService.addToBatch("));
        assertTrue(serviceSource.contains("mutationService.consumeFEFO("));
        assertTrue(serviceSource.contains("queryService.findNextFEFO("));
        assertFalse(serviceSource.contains("new ProductBatch()"));

        assertTrue(mutationSource.contains("LEGACY_EXPIRATION"));
        assertTrue(querySource.contains("new ProductBatchDTO("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int mutationLines = Files.readAllLines(MUTATION_FILE).size();
        int queryLines = Files.readAllLines(QUERY_FILE).size();

        assertTrue(serviceLines <= 95, "ProductBatchService deve ter <= 95 linhas, mas tem " + serviceLines);
        assertTrue(mutationLines <= 140, "ProductBatchMutationService deve ter <= 140 linhas, mas tem " + mutationLines);
        assertTrue(queryLines <= 85, "ProductBatchQueryService deve ter <= 85 linhas, mas tem " + queryLines);
    }
}
