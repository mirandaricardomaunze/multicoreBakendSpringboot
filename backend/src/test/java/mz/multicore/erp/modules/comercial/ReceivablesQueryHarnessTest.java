package mz.multicore.erp.modules.comercial;

import jakarta.persistence.EntityManager;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
class ReceivablesQueryHarnessTest {
    @Autowired private EntityManager entityManager;
    @Autowired private InvoiceRepository repository;

    @Test
    void performanceIndexes_applyToMappedSchemaAndCanBeReapplied() {
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
            var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                    new org.springframework.core.io.ClassPathResource(
                            "db/migration/V90__scalability_performance_composite_indexes.sql"));
            script.populate(connection);
            script.populate(connection);
            try (var indexes = connection.getMetaData().getIndexInfo(null, null, "INVOICES", false, false)) {
                List<String> columns = new ArrayList<>();
                while (indexes.next()) {
                    if ("IDX_INVOICES_COMPANY_CLIENT_STATUS".equals(indexes.getString("INDEX_NAME"))) {
                        columns.add(indexes.getString("COLUMN_NAME"));
                    }
                }
                assertThat(columns).containsExactly("COMPANY_ID", "CLIENT_ID", "STATUS");
            }
        });
    }

    @Test
    void aggregate_matchesDomainForEveryStatusAndLoadsNoEntities() {
        Company company = company("A");
        Company otherCompany = company("B");
        Client client = client("A");
        Client otherClient = client("B");
        List<Invoice> invoices = new ArrayList<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            invoices.add(invoice(company, client, status, "100.25", "30.10"));
            invoices.add(invoice(company, client, status, "100.25", "100.25"));
            invoices.add(invoice(company, client, status, "100.25", "150.00"));
            invoices.add(invoice(company, client, status, "0.00", "0.00"));
        }
        invoice(otherCompany, client, InvoiceStatus.APPROVED, "9000.00", "0.00");
        invoice(company, otherClient, InvoiceStatus.APPROVED, "8000.00", "0.00");
        BigDecimal expected = invoices.stream().filter(i -> i.getStatus().isCollectable())
                .map(Invoice::outstandingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Statistics statistics = resetStatistics();

        assertThat(repository.sumOutstandingByClient(company.getId(), client.getId(),
                InvoiceStatus.collectableStatuses())).isEqualByComparingTo(expected);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount()).isZero();
    }

    @Test
    void outstanding_fetchesClientsInOneQueryAndExcludesSettledHistoryAndOtherTenants() {
        Company company = company("A");
        Company otherCompany = company("B");
        for (int n = 0; n < 12; n++) {
            Client client = client("client-" + n);
            invoice(company, client, InvoiceStatus.PARTIALLY_PAID, "100.00", "25.00");
            invoice(company, client, InvoiceStatus.APPROVED, "50.00", "50.00");
            invoice(company, client, InvoiceStatus.CANCELLED, "700.00", "0.00");
            invoice(otherCompany, client, InvoiceStatus.APPROVED, "900.00", "0.00");
        }
        Statistics statistics = resetStatistics();

        List<Invoice> outstanding = repository.findOutstandingWithClient(company.getId(),
                InvoiceStatus.collectableStatuses());
        assertThat(outstanding).hasSize(12);
        outstanding.forEach(i -> {
            assertThat(i.getClient().getName()).startsWith("client-");
            assertThat(i.outstandingAmount()).isEqualByComparingTo("75.00");
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getCollectionFetchCount()).isZero();
    }

    @Test
    void emptyClientOrTenant_returnsZeroAndEmptyList() {
        assertThat(repository.sumOutstandingByClient(-1L, -1L,
                InvoiceStatus.collectableStatuses())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(repository.findOutstandingWithClient(-1L,
                InvoiceStatus.collectableStatuses())).isEmpty();
    }

    private Statistics resetStatistics() {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
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
        client.setEmail(name + "@example.test");
        entityManager.persist(client);
        return client;
    }

    private Invoice invoice(Company company, Client client, InvoiceStatus status,
                            String total, String paid) {
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setAmountPaid(new BigDecimal(paid));
        entityManager.persist(invoice);
        return invoice;
    }
}
