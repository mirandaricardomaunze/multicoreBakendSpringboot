package mz.multicore.erp.modules.inventory;

import jakarta.persistence.EntityManager;
import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.OrderReservationWatcher;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.service.StockAvailabilityTracker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Harness WH-02 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1): through real Hibernate and real commits, the inventory
 * publishes one {@link StockAvailabilityChangedEvent} per product × sales warehouse whose available stock (quantity −
 * active reservations) crosses zero in a transaction — first before × last after; nothing for changes that do not cross
 * zero, non-sales warehouses, untracked products or rolled-back transactions.
 */
@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework=WARN"
})
@Import({StockAvailabilityTracker.class, OrderReservationLookup.class, OrderReservationWatcher.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RecordApplicationEvents
class StockAvailabilityChangedEventJpaTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEvents events;

    @Test
    void stockThatRunsOutPublishesOutOfStockWithTheProductAndWarehouse() {
        Fixture f = fixture(salesWarehouse(), true, "3");

        setQuantity(f, "0");

        List<StockAvailabilityChangedEvent> published = published();
        assertThat(published).hasSize(1);
        StockAvailabilityChangedEvent event = published.get(0);
        assertThat(event.companyId()).isEqualTo(f.companyId());
        assertThat(event.sku()).isEqualTo(f.sku());
        assertThat(event.productName()).isEqualTo("Farinha 1kg");
        assertThat(event.warehouseCode()).isEqualTo(f.warehouseCode());
        assertThat(event.warehouseName()).isEqualTo("Loja Baixa");
        assertThat(event.availableBefore()).isEqualByComparingTo("3");
        assertThat(event.availableAfter()).isEqualByComparingTo("0");
        assertThat(event.wentOutOfStock()).isTrue();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void stockThatComesBackPublishesBackInStock() {
        Fixture f = fixture(salesWarehouse(), true, "0");

        setQuantity(f, "12");

        assertThat(published()).extracting(StockAvailabilityChangedEvent::cameBackInStock,
                        e -> e.availableAfter().intValue())
                .containsExactly(tuple(true, 12));
    }

    @Test
    void aNewStockRowStartsFromZero() {
        Fixture f = fixture(salesWarehouse(), true, null);

        inTransaction(em -> {
            Stock stock = new Stock();
            stock.setProduct(em.find(Product.class, f.productId()));
            stock.setWarehouse(em.find(Warehouse.class, f.warehouseId()));
            stock.setQuantity(new BigDecimal("4"));
            em.persist(stock);
        });

        assertThat(published()).extracting(StockAvailabilityChangedEvent::cameBackInStock).containsExactly(true);
    }

    @Test
    void changesThatDoNotCrossZeroPublishNothing() {
        Fixture f = fixture(salesWarehouse(), true, "5");

        setQuantity(f, "2");
        setQuantity(f, "2");

        assertThat(published()).isEmpty();
    }

    @Test
    void severalMovementsInOneTransactionCompareTheFirstBeforeWithTheLastAfter() {
        Fixture going = fixture(salesWarehouse(), true, "2");
        Fixture coming = fixture(salesWarehouse(), true, "0");

        inTransaction(em -> {
            Stock stock = em.find(Stock.class, going.stockId());
            stock.setQuantity(BigDecimal.ZERO);
            em.flush();
            stock.setQuantity(new BigDecimal("3"));
        });
        inTransaction(em -> {
            Stock stock = em.find(Stock.class, coming.stockId());
            stock.setQuantity(new BigDecimal("4"));
            em.flush();
            stock.setQuantity(new BigDecimal("1"));
        });

        assertThat(published()).extracting(StockAvailabilityChangedEvent::sku,
                        StockAvailabilityChangedEvent::cameBackInStock)
                .containsExactly(tuple(coming.sku(), true));
    }

    @Test
    void nonSalesWarehousesAndUntrackedProductsPublishNothing() {
        Warehouse depot = salesWarehouse();
        depot.setAllowsSales(false);
        Warehouse transit = salesWarehouse();
        transit.setType(WarehouseType.TRANSIT);
        Warehouse inactive = salesWarehouse();
        inactive.setActive(false);

        for (Fixture f : List.of(fixture(depot, true, "3"), fixture(transit, true, "3"),
                fixture(inactive, true, "3"), fixture(salesWarehouse(), false, "3"))) {
            setQuantity(f, "0");
        }

        assertThat(published()).isEmpty();
    }

    @Test
    void availableDiscountsActiveReservations() {
        Fixture reservedOut = fixture(salesWarehouse(), true, "5");
        reserve(reservedOut, "5");
        Fixture partlyReserved = fixture(salesWarehouse(), true, "7");
        reserve(partlyReserved, "5");

        setQuantity(reservedOut, "8");
        setQuantity(partlyReserved, "5");

        assertThat(published()).extracting(StockAvailabilityChangedEvent::sku,
                        e -> e.availableBefore().intValue(), e -> e.availableAfter().intValue())
                .containsExactly(tuple(reservedOut.sku(), 0, 3), tuple(partlyReserved.sku(), 2, 0));
    }

    @Test
    void reservingTheLastUnitsPublishesOutOfStock() {
        Fixture f = fixture(salesWarehouse(), true, "5");

        inTransaction(em -> persistReservedOrder(em, f, "5"));

        assertThat(published()).extracting(StockAvailabilityChangedEvent::wentOutOfStock,
                        e -> e.availableBefore().intValue(), e -> e.availableAfter().intValue())
                .containsExactly(tuple(true, 5, 0));
    }

    @Test
    void releasingAReservationWithoutTouchingItsLinesPublishesBackInStock() {
        Fixture f = fixture(salesWarehouse(), true, "5");
        Long orderId = reserve(f, "5");

        inTransaction(em -> em.find(Order.class, orderId).setReservationActive(false));

        assertThat(published()).extracting(StockAvailabilityChangedEvent::cameBackInStock,
                        e -> e.availableBefore().intValue(), e -> e.availableAfter().intValue())
                .containsExactly(tuple(true, 0, 5));
    }

    @Test
    void freeingPartOfAReservedLinePublishesBackInStock() {
        Fixture f = fixture(salesWarehouse(), true, "5");
        Long orderId = reserve(f, "5");

        inTransaction(em -> em.find(Order.class, orderId).getLines().get(0).setReservedQuantity(new BigDecimal("3")));

        assertThat(published()).extracting(StockAvailabilityChangedEvent::cameBackInStock,
                        e -> e.availableAfter().intValue())
                .containsExactly(tuple(true, 2));
    }

    @Test
    void billingTheLastReservedUnitsIsNotOutOfStock() {
        Fixture f = fixture(salesWarehouse(), true, "5");
        Long orderId = reserve(f, "5");

        inTransaction(em -> {
            em.find(Stock.class, f.stockId()).setQuantity(BigDecimal.ZERO);
            em.find(Order.class, orderId).setReservationActive(false);
        });

        assertThat(published()).isEmpty();
    }

    @Test
    void aRolledBackTransactionPublishesNothing() {
        Fixture f = fixture(salesWarehouse(), true, "3");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.find(Stock.class, f.stockId()).setQuantity(BigDecimal.ZERO);
            status.setRollbackOnly();
        });

        assertThat(published()).isEmpty();
    }

    private List<StockAvailabilityChangedEvent> published() {
        return events.stream(StockAvailabilityChangedEvent.class).toList();
    }

    private void setQuantity(Fixture f, String quantity) {
        inTransaction(em -> em.find(Stock.class, f.stockId()).setQuantity(new BigDecimal(quantity)));
    }

    private void inTransaction(Consumer<EntityManager> work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.accept(entityManager));
    }

    private Warehouse salesWarehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Loja Baixa");
        return warehouse;
    }

    /** Company, warehouse, product and (when {@code quantity} is given) its stock row, committed without events. */
    private Fixture fixture(Warehouse warehouse, boolean stockTracked, String quantity) {
        int n = SEQ.incrementAndGet();
        Long[] ids = new Long[4];
        inTransaction(em -> {
            Company company = new Company();
            company.setName("Loja " + n);
            company.setTaxId("stock-company-" + n);
            em.persist(company);
            warehouse.setCompany(company);
            warehouse.setWarehouseNumber("A" + n);
            em.persist(warehouse);
            Product product = new Product();
            product.setSku("FAR-" + n);
            product.setBarcode("BAR-" + n);
            product.setName("Farinha 1kg");
            product.setBaseUnitCode("UN");
            product.setUnitPrice(new BigDecimal("95.00"));
            product.setStockTracked(stockTracked);
            em.persist(product);
            ids[0] = company.getId();
            ids[1] = warehouse.getId();
            ids[2] = product.getId();
        });
        if (quantity != null) {
            inTransaction(em -> {
                Stock stock = new Stock();
                stock.setProduct(em.find(Product.class, ids[2]));
                stock.setWarehouse(em.find(Warehouse.class, ids[1]));
                stock.setQuantity(new BigDecimal(quantity));
                em.persist(stock);
                ids[3] = stock.getId();
            });
        }
        events.clear();
        return new Fixture(ids[0], ids[1], ids[2], ids[3], "FAR-" + n, "A" + n);
    }

    /** Order reserving {@code quantity} of the fixture's product, committed without events; returns its id. */
    private Long reserve(Fixture f, String quantity) {
        Long[] id = new Long[1];
        inTransaction(em -> id[0] = persistReservedOrder(em, f, quantity));
        events.clear();
        return id[0];
    }

    private Long persistReservedOrder(EntityManager em, Fixture f, String quantity) {
        int n = SEQ.incrementAndGet();
        Client client = new Client();
        client.setName("Padaria Central");
        client.setTaxId("stock-client-" + n);
        client.setEmail("padaria." + n + "@example.test");
        em.persist(client);
        Order order = new Order();
        order.setCompany(em.find(Company.class, f.companyId()));
        order.setClient(client);
        order.setOrderNumber("EC-STK/" + n);
        order.setWarehouse(em.find(Warehouse.class, f.warehouseId()));
        order.setReservationActive(true);
        OrderLine line = new OrderLine();
        line.setOrder(order);
        line.setProduct(em.find(Product.class, f.productId()));
        line.setQuantity(new BigDecimal(quantity));
        line.setReservedQuantity(new BigDecimal(quantity));
        line.setUnitPrice(BigDecimal.TEN);
        line.setTaxRate(BigDecimal.ZERO);
        line.setLineTotal(BigDecimal.TEN);
        order.getLines().add(line);
        em.persist(order);
        return order.getId();
    }

    private record Fixture(Long companyId, Long warehouseId, Long productId, Long stockId,
                           String sku, String warehouseCode) {
    }
}
