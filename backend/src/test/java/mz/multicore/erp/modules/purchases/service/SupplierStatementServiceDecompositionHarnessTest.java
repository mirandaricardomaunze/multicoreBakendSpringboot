package mz.multicore.erp.modules.purchases.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierStatementServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/purchases/service/SupplierStatementService.java");
    private static final Path BUILDER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/purchases/service/SupplierStatementMovementBuilder.java");

    @Test
    void supplierStatementServiceDelegatesToMovementBuilder() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String builderSource = Files.readString(BUILDER_FILE);

        assertTrue(serviceSource.contains("movementBuilder.buildStatement("));
        assertFalse(serviceSource.contains("new SupplierStatementLineDTO("));

        assertTrue(builderSource.contains("Factura de Fornecedor / Compra"));
        assertTrue(builderSource.contains("Pagamento / Liquidação de Factura"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int builderLines = Files.readAllLines(BUILDER_FILE).size();

        assertTrue(serviceLines <= 65, "SupplierStatementService deve ter <= 65 linhas, mas tem " + serviceLines);
        assertTrue(builderLines <= 140, "SupplierStatementMovementBuilder deve ter <= 140 linhas, mas tem " + builderLines);
    }
}
