package mz.multicore.erp.architecture.concurrency;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness permanente de docs/ROBUSTEZ_MULTIUTILIZADOR_SPEC.md. */
class MultiUserRobustnessHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void productionCapacityIsTunableObservableAndGraceful() throws Exception {
        String prod = read("backend/src/main/resources/application-prod.properties");

        assertThat(prod).contains(
                "spring.datasource.hikari.maximum-pool-size=${DB_POOL_MAX_SIZE:20}",
                "spring.datasource.hikari.minimum-idle=${DB_POOL_MIN_IDLE:5}",
                "server.tomcat.threads.max=${HTTP_MAX_THREADS:200}",
                "server.tomcat.max-connections=${HTTP_MAX_CONNECTIONS:8192}",
                "server.shutdown=graceful",
                "management.endpoint.health.probes.enabled=true",
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus");
    }

    @Test
    void publicProxyDoesNotExposeOperationalEndpoints() throws Exception {
        String caddy = read("deployment/Caddyfile");
        assertThat(caddy).contains("handle /api/*");
        assertThat(caddy).doesNotContain("handle /actuator/*", "handle /metrics/*");
    }

    @Test
    void backendRemainsStatelessAndCriticalStateLivesInDatabase() throws Exception {
        assertThat(read("backend/src/main/java/mz/multicore/erp/architecture/security/SecurityConfig.java"))
                .contains("SessionCreationPolicy.STATELESS");
        assertThat(read("backend/src/main/java/mz/multicore/erp/modules/numbering/repository/DocumentSequenceRepository.java"))
                .contains("LockModeType.PESSIMISTIC_WRITE", "lockByCompanyAndSeriesAndYear");
        assertThat(read("backend/src/main/java/mz/multicore/erp/modules/inventory/repository/StockRepository.java"))
                .contains("LockModeType.PESSIMISTIC_WRITE", "findForReservation");
    }

    @Test
    void mutableCriticalAggregatesKeepOptimisticVersions() throws Exception {
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/inventory/model/Stock.java");
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/inventory/model/ProductBatch.java");
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/financeira/model/TreasuryAccount.java");
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/comercial/model/Order.java");
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/comercial/model/Quotation.java");
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/inventory/model/StockTransfer.java");
        assertVersioned("backend/src/main/java/mz/multicore/erp/modules/purchases/model/PurchaseOrder.java");
    }

    @Test
    void highContentionEntryPointsKeepBoundedRetry() throws Exception {
        String retry = read("backend/src/main/java/mz/multicore/erp/architecture/concurrency/ConcurrencyRetry.java");
        assertThat(retry).contains("MAX_ATTEMPTS = 3", "catch (ConcurrencyFailureException");
        assertThat(read("backend/src/main/java/mz/multicore/erp/modules/pos/controller/POSController.java"))
                .contains("concurrencyRetry.run(() ->");
        assertThat(read("backend/src/main/java/mz/multicore/erp/modules/comercial/controller/ComercialController.java"))
                .contains("concurrencyRetry.run(() ->");
    }

    private void assertVersioned(String relativePath) throws Exception {
        assertThat(read(relativePath)).as(relativePath + " deve detectar lost updates").contains("@Version");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
