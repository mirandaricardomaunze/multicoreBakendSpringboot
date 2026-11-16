package mz.multicore.erp.modules.comercial;

import jakarta.persistence.EntityManager;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pesquisa de encomendas abertas feita na base (EN-07): estados, número, cliente e empresa. */
@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework=WARN"
})
class OrderSearchQueryHarnessTest {
    @Autowired private EntityManager entityManager;
    @Autowired private OrderRepository repository;

    @Test
    void searchOpen_filtraPorEstadoNumeroClienteEEmpresa() {
        Company company = company("A");
        Company other = company("B");
        Client padaria = client("Padaria Central");
        Client mercado = client("Mercado X");
        order(company, padaria, "EC-2026/1", "PENDING", null);
        order(company, mercado, "EC-2026/2", "PENDING", null);
        order(company, padaria, "EC-2026/3", "BILLED", 50L);
        order(company, padaria, "EC-2026/4", "PENDING", 99L);
        order(other, padaria, "EC-2026/5", "PENDING", null);
        entityManager.flush();

        List<String> pending = List.of("PENDING");
        assertThat(numbers(repository.searchOpen(company.getId(), pending, ""))).containsExactlyInAnyOrder("EC-2026/1", "EC-2026/2");
        assertThat(numbers(repository.searchOpen(company.getId(), pending, "padaria"))).containsExactly("EC-2026/1");
        assertThat(numbers(repository.searchOpen(company.getId(), pending, "ec-2026/2"))).containsExactly("EC-2026/2");
        assertThat(repository.searchOpen(company.getId(), pending, "zzz")).isEmpty();
        assertThat(numbers(repository.searchOpen(company.getId(), List.of("PENDING", "BILLED"), "padaria")))
                .containsExactlyInAnyOrder("EC-2026/1");
    }

    private static List<String> numbers(List<Order> orders) {
        return orders.stream().map(Order::getOrderNumber).toList();
    }

    private Company company(String name) {
        Company company = new Company();
        company.setName(name);
        company.setTaxId("company-" + name);
        entityManager.persist(company);
        return company;
    }

    private Client client(String name) {
        Client client = new Client();
        client.setName(name);
        client.setTaxId("client-" + name);
        client.setEmail(name.replace(' ', '.') + "@example.test");
        entityManager.persist(client);
        return client;
    }

    private void order(Company company, Client client, String number, String status, Long invoiceId) {
        Order order = new Order();
        order.setCompany(company);
        order.setClient(client);
        order.setOrderNumber(number);
        order.setStatus(status);
        order.setInvoiceId(invoiceId);
        entityManager.persist(order);
    }
}
