package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/inventory/service/InventoryService.java");
    private static final Path WAREHOUSE_SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/inventory/service/WarehouseService.java");
    private static final Path STOCK_QUERY_SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/inventory/service/StockQueryService.java");
    private static final Path STOCK_MOVEMENT_SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/inventory/service/StockMovementService.java");

    @Test
    @DisplayName("Garante que InventoryService e seus colaboradores existem e cumprem limites de linhas")
    void testFileSizesAndLimits() throws IOException {
        assertTrue(Files.exists(SERVICE_PATH), "InventoryService.java deve existir");
        assertTrue(Files.exists(WAREHOUSE_SERVICE_PATH), "WarehouseService.java deve existir");
        assertTrue(Files.exists(STOCK_QUERY_SERVICE_PATH), "StockQueryService.java deve existir");
        assertTrue(Files.exists(STOCK_MOVEMENT_SERVICE_PATH), "StockMovementService.java deve existir");

        long serviceLines = Files.lines(SERVICE_PATH).count();
        long warehouseLines = Files.lines(WAREHOUSE_SERVICE_PATH).count();
        long queryLines = Files.lines(STOCK_QUERY_SERVICE_PATH).count();
        long movementLines = Files.lines(STOCK_MOVEMENT_SERVICE_PATH).count();

        assertTrue(serviceLines <= 320,
                "InventoryService deve ter <= 320 linhas, mas tem: " + serviceLines);
        assertTrue(warehouseLines <= 215,
                "WarehouseService deve ter <= 215 linhas, mas tem: " + warehouseLines);
        assertTrue(queryLines <= 205,
                "StockQueryService deve ter <= 205 linhas, mas tem: " + queryLines);
        assertTrue(movementLines <= 380,
                "StockMovementService deve ter <= 380 linhas, mas tem: " + movementLines);
    }

    @Test
    @DisplayName("Garante que InventoryService delega para os serviços especializados de armazém, consulta e movimentos")
    void testDelegationInOrchestrator() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("warehouseService.getWarehousesByCompany(companyId)"),
                "Deve delegar getWarehousesByCompany para warehouseService");
        assertTrue(content.contains("warehouseService.getSalesWarehousesByCompany(companyId)"),
                "Deve delegar getSalesWarehousesByCompany para warehouseService");
        assertTrue(content.contains("stockQueryService.findOutOfStockProducts(companyId)"),
                "Deve delegar findOutOfStockProducts para stockQueryService");
        assertTrue(content.contains("stockQueryService.getInStockProductIdsForSale(companyId)"),
                "Deve delegar getInStockProductIdsForSale para stockQueryService");
        assertTrue(content.contains("stockMovementService.registerMovement("),
                "Deve delegar registerMovement para stockMovementService");
        assertTrue(content.contains("stockMovementService.adjustStock(request)"),
                "Deve delegar adjustStock para stockMovementService");
        assertTrue(content.contains("stockQueryService.findExpiringBatches(companyId, daysAhead)"),
                "Deve delegar findExpiringBatches para stockQueryService");
    }

    @Test
    @DisplayName("Garante conformidade com o harness permanente de concorrência e paginação")
    void testPermanentHarnessConstraints() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("CurrentUserContext.requireCompany(companyId)"),
                "Deve exigir empresa ativa");
        assertTrue(content.contains("stockMovementRepository.findPageByCompanyId(companyId, PageQuery.of(page, size))"),
                "Deve conter a chamada paginada do repositório");
        assertTrue(content.contains("Stock stock = stockRepository.findForReservation"),
                "Deve referenciar o bloqueio pessimista para reserva de stock");
        assertTrue(content.contains("BigDecimal current = stockRepository.findForReservation"),
                "Deve referenciar o bloqueio pessimista para leitura do saldo atual");
    }

    @Test
    @DisplayName("Garante construtor de 9 argumentos para retrocompatibilidade com testes legados")
    void testBackwardCompatibleConstructor() {
        InventoryService service = new InventoryService(
                null, null, null, null, null, null, null, null, null);
        assertNotNull(service, "Construtor de 9 argumentos deve instanciar");
    }
}
