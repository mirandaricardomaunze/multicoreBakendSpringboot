package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalReplenishmentServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/InternalReplenishmentService.java");
    private static final Path CONVERSION_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/ReplenishmentOrderConversionService.java");

    @Test
    void internalReplenishmentServiceDelegatesToConversionCollaborator() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String conversionSource = Files.readString(CONVERSION_FILE);

        assertTrue(serviceSource.contains("conversionService.convertToTransfer("));
        assertTrue(serviceSource.contains("conversionService.recordOrderFromTransfer("));
        assertTrue(serviceSource.contains("conversionService.onTransferResolved("));
        assertFalse(serviceSource.contains("new CreateStockTransferRequest("));

        assertTrue(conversionSource.contains("usesWarehouseTransfer()"));
        assertTrue(conversionSource.contains("createReplenishmentRecord("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int conversionLines = Files.readAllLines(CONVERSION_FILE).size();

        assertTrue(serviceLines <= 80, "InternalReplenishmentService deve ter <= 80 linhas, mas tem " + serviceLines);
        assertTrue(conversionLines <= 155, "ReplenishmentOrderConversionService deve ter <= 155 linhas, mas tem " + conversionLines);
    }
}
