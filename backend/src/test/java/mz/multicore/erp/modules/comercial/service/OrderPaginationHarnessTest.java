package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness permanente da paginação server-side de encomendas de clientes e índices de escalabilidade. */
class OrderPaginationHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void customerOrdersArePagedInDatabaseAndTenantScoped() throws Exception {
        String repository = read("backend/src/main/java/mz/multicore/erp/modules/comercial/repository/OrderRepository.java");
        assertThat(repository).contains(
                "Page<Order> findPageByCompanyId",
                "o.company.id = :companyId",
                "countQuery =");

        String service = read("backend/src/main/java/mz/multicore/erp/modules/comercial/service/ComercialService.java");
        assertThat(service).contains(
                "CurrentUserContext.requireCompany(companyId)",
                "orderRepository.findPageByCompanyId(companyId, PageQuery.of(page, size))");

        String controller = read("backend/src/main/java/mz/multicore/erp/modules/comercial/controller/ComercialController.java");
        assertThat(controller).contains("@GetMapping(\"/orders/page\")", "PageResponse<OrderDTO>");

        String client = read("desktop/src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java");
        assertThat(client).contains("getOrdersPage(Long companyId, int page, int size)", "/api/comercial/orders/page");

        String view = read("desktop/src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        assertThat(view).contains("new TablePager(owner::loadOrdersPage)");

        String migration = read("backend/src/main/resources/db/migration/V90__scalability_performance_composite_indexes.sql");
        assertThat(migration).contains(
                "idx_invoices_company_date",
                "idx_customer_orders_company_date",
                "idx_stock_movements_wh_date",
                "idx_treasury_tx_account_date");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
