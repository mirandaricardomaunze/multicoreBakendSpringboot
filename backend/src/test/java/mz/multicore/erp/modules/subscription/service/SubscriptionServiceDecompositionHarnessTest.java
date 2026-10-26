package mz.multicore.erp.modules.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular - SubscriptionService (Fase 103)")
class SubscriptionServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/subscription/service/SubscriptionService.java");
    private static final Path LIFECYCLE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/subscription/service/SubscriptionLifecycleService.java");
    private static final Path PAYMENT_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/subscription/service/SubscriptionPaymentService.java");
    private static final Path RENEWAL_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/subscription/service/SubscriptionRenewalService.java");
    private static final Path QUERY_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/subscription/service/SubscriptionQueryService.java");

    @Test
    @DisplayName("Garante limites físicos estritos de linhas de código em todos os componentes")
    void shouldAdhereToStrictPhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_PATH).count();
        long lifecycleLines = Files.lines(LIFECYCLE_PATH).count();
        long paymentLines = Files.lines(PAYMENT_PATH).count();
        long renewalLines = Files.lines(RENEWAL_PATH).count();
        long queryLines = Files.lines(QUERY_PATH).count();

        assertTrue(serviceLines <= 160,
                "SubscriptionService.java deve ter <= 160 linhas, mas tem: " + serviceLines);
        assertTrue(lifecycleLines <= 195,
                "SubscriptionLifecycleService.java deve ter <= 195 linhas, mas tem: " + lifecycleLines);
        assertTrue(paymentLines <= 150,
                "SubscriptionPaymentService.java deve ter <= 150 linhas, mas tem: " + paymentLines);
        assertTrue(renewalLines <= 255,
                "SubscriptionRenewalService.java deve ter <= 255 linhas, mas tem: " + renewalLines);
        assertTrue(queryLines <= 95,
                "SubscriptionQueryService.java deve ter <= 95 linhas, mas tem: " + queryLines);
    }

    @Test
    @DisplayName("Garante delegação no orquestrador")
    void shouldMaintainDelegationPattern() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("lifecycleService.saveSubscription"),
                "SubscriptionService deve delegar saveSubscription para SubscriptionLifecycleService");
        assertTrue(content.contains("paymentService.recordPayment"),
                "SubscriptionService deve delegar recordPayment para SubscriptionPaymentService");
        assertTrue(content.contains("renewalService.initiateSelfServiceRenewal"),
                "SubscriptionService deve delegar initiateSelfServiceRenewal para SubscriptionRenewalService");
        assertTrue(content.contains("queryService.listOverview"),
                "SubscriptionService deve delegar listOverview para SubscriptionQueryService");
    }

    @Test
    @DisplayName("Verifica ausência de chamadas proibidas e UI no backend")
    void shouldNotContainForbiddenCalls() throws IOException {
        List<Path> paths = List.of(SERVICE_PATH, LIFECYCLE_PATH, PAYMENT_PATH, RENEWAL_PATH, QUERY_PATH);
        for (Path path : paths) {
            String content = Files.readString(path);
            assertFalse(content.contains("JOptionPane"),
                    path.getFileName() + " não pode conter JOptionPane");
            assertFalse(content.contains("javax.swing"),
                    path.getFileName() + " não pode conter Swing");
            assertFalse(content.contains("java.awt"),
                    path.getFileName() + " não pode conter AWT");
        }
    }

    @Test
    @DisplayName("Verifica que os colaboradores são componentes Spring anotados com @Service")
    void shouldBeProperSpringServices() throws IOException {
        assertTrue(Files.readString(LIFECYCLE_PATH).contains("@Service"),
                "SubscriptionLifecycleService deve ser @Service");
        assertTrue(Files.readString(PAYMENT_PATH).contains("@Service"),
                "SubscriptionPaymentService deve ser @Service");
        assertTrue(Files.readString(RENEWAL_PATH).contains("@Service"),
                "SubscriptionRenewalService deve ser @Service");
        assertTrue(Files.readString(QUERY_PATH).contains("@Service"),
                "SubscriptionQueryService deve ser @Service");
    }
}
