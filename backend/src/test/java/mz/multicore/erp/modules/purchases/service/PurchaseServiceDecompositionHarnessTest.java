package mz.multicore.erp.modules.purchases.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "modules", "purchases", "service");

    @Test
    @DisplayName("PurchaseService orchestrator deve ter no máximo 175 linhas")
    void orchestratorUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("PurchaseService.java"));
        assertTrue(lines <= 175, "PurchaseService deve ter <= 175 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("SupplierManagementService deve ter no máximo 150 linhas")
    void supplierManagementServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("SupplierManagementService.java"));
        assertTrue(lines <= 150, "SupplierManagementService deve ter <= 150 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("DirectPurchaseService deve ter no máximo 240 linhas")
    void directPurchaseServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("DirectPurchaseService.java"));
        assertTrue(lines <= 240, "DirectPurchaseService deve ter <= 240 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("SupplierPaymentService deve ter no máximo 130 linhas")
    void supplierPaymentServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("SupplierPaymentService.java"));
        assertTrue(lines <= 130, "SupplierPaymentService deve ter <= 130 linhas, mas tem: " + lines);
    }

    private long countLines(Path path) throws IOException {
        return Files.lines(path).count();
    }
}
