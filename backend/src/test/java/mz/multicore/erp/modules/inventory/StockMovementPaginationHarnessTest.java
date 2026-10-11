package mz.multicore.erp.modules.inventory;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness permanente da paginação server-side do histórico de stock. */
class StockMovementPaginationHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void stockMovementHistoryIsPagedInDatabaseAndTenantScoped() throws Exception {
        String repository = read("backend/src/main/java/mz/multicore/erp/modules/inventory/repository/StockMovementRepository.java");
        assertThat(repository).contains(
                "Page<StockMovement> findPageByCompanyId",
                "w.company.id = :companyId",
                "countQuery =");

        String service = read("backend/src/main/java/mz/multicore/erp/modules/inventory/service/InventoryService.java");
        assertThat(service).contains(
                "CurrentUserContext.requireCompany(companyId)",
                "stockMovementRepository.findPageByCompanyId(companyId, PageQuery.of(page, size))");

        String controller = read("backend/src/main/java/mz/multicore/erp/modules/inventory/controller/InventoryController.java");
        assertThat(controller).contains("@GetMapping(\"/movements/page\")", "PageResponse<StockMovementDTO>");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
