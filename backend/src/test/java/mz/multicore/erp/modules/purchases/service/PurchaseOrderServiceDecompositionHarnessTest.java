package mz.multicore.erp.modules.purchases.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/purchases/service/PurchaseOrderService.java");
    private static final Path LIFECYCLE_SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/purchases/service/PurchaseOrderLifecycleService.java");
    private static final Path RECEIPT_EXECUTOR_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/purchases/service/PurchaseOrderReceiptExecutor.java");

    @Test
    @DisplayName("Garante que PurchaseOrderService e seus colaboradores existem e cumprem limites de linhas")
    void testFileSizesAndLimits() throws IOException {
        assertTrue(Files.exists(SERVICE_PATH), "PurchaseOrderService.java deve existir");
        assertTrue(Files.exists(LIFECYCLE_SERVICE_PATH), "PurchaseOrderLifecycleService.java deve existir");
        assertTrue(Files.exists(RECEIPT_EXECUTOR_PATH), "PurchaseOrderReceiptExecutor.java deve existir");

        long serviceLines = Files.lines(SERVICE_PATH).count();
        long lifecycleLines = Files.lines(LIFECYCLE_SERVICE_PATH).count();
        long receiptLines = Files.lines(RECEIPT_EXECUTOR_PATH).count();

        assertTrue(serviceLines <= 195,
                "PurchaseOrderService deve ter <= 195 linhas, mas tem: " + serviceLines);
        assertTrue(lifecycleLines <= 220,
                "PurchaseOrderLifecycleService deve ter <= 220 linhas, mas tem: " + lifecycleLines);
        assertTrue(receiptLines <= 250,
                "PurchaseOrderReceiptExecutor deve ter <= 250 linhas, mas tem: " + receiptLines);
    }

    @Test
    @DisplayName("Garante que PurchaseOrderService delega operações para lifecycleService e receiptExecutor")
    void testDelegationInOrchestrator() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("lifecycleService.createOrder(request)"),
                "Deve delegar createOrder para lifecycleService");
        assertTrue(content.contains("lifecycleService.updateOrder(order, request)"),
                "Deve delegar updateOrder para lifecycleService");
        assertTrue(content.contains("receiptExecutor.receiveOrder(order)"),
                "Deve delegar receiveOrder para receiptExecutor");
        assertTrue(content.contains("receiptExecutor.receivePartial(id, request)"),
                "Deve delegar receivePartial para receiptExecutor");
        assertTrue(content.contains("lifecycleService.cancelOrder(order, reason)"),
                "Deve delegar cancelOrder para lifecycleService");
    }

    @Test
    @DisplayName("Garante anotações Spring @Service adequadas")
    void testSpringAnnotations() throws IOException {
        String serviceContent = Files.readString(SERVICE_PATH);
        String lifecycleContent = Files.readString(LIFECYCLE_SERVICE_PATH);
        String receiptContent = Files.readString(RECEIPT_EXECUTOR_PATH);

        assertTrue(serviceContent.contains("@Service"), "PurchaseOrderService deve ser @Service");
        assertTrue(lifecycleContent.contains("@Service"), "PurchaseOrderLifecycleService deve ser @Service");
        assertTrue(receiptContent.contains("@Service"), "PurchaseOrderReceiptExecutor deve ser @Service");
    }

    @Test
    @DisplayName("Garante construtor de 10 argumentos para retrocompatibilidade com mocks existentes")
    void testBackwardCompatibleConstructor() {
        PurchaseOrderService service = new PurchaseOrderService(
                null, null, null, null, null, null, null, null, null, null);
        assertNotNull(service, "Construtor de 10 argumentos deve instanciar");
    }
}
