package mz.multicore.erp.modules.publicapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.InsufficientStockException;
import mz.multicore.erp.modules.comercial.service.OrderApprovalCallback;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderLineRequest;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness API-08 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, §3.4.1): an order created by an API key is a formal order
 * (A4) of the key's company with the desktop's approval flow, system prices and VAT, and stock reserved on creation;
 * customer by NUIT (registered with {@code customerName} when new) or Consumidor Final with contact; origin saved.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:public-api-orders;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiOrderTest {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

    @Autowired private PublicOrderService orderService;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private OrderApprovalCallback approvalCallback;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private long companyId;
    private Warehouse store;
    private String sku;
    private ApiKeyPrincipal key;

    @BeforeEach
    void setUp() {
        companyId = jdbc.queryForObject("select min(id) from companies", Long.class);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            store = warehouseRepository.findAll().stream()
                    .filter(w -> w.getCompany() != null && w.getCompany().getId().equals(companyId))
                    .filter(w -> w.isActive() && w.isAllowsSales() && w.getType() != WarehouseType.TRANSIT)
                    .findFirst().orElseThrow();
            // Expensive enough to stay out of the Green Lane: the order really waits for approval, as at the desktop.
            sku = "API08-" + SEQ.incrementAndGet();
            Product product = new Product();
            product.setSku(sku);
            product.setName("Gerador API-08 " + sku);
            product.setUnitPrice(new BigDecimal("60000.00"));
            product.setStockTracked(true);
            product.getCompanies().add(companyRepository.findById(companyId).orElseThrow());
            product = productRepository.save(product);
            Stock stock = new Stock();
            stock.setProduct(product);
            stock.setWarehouse(store);
            stock.setQuantity(new BigDecimal("10"));
            stockRepository.save(stock);
        } finally {
            CurrentUserContext.clear();
        }
        key = new ApiKeyPrincipal(8008L, companyId, "Loja online API-08", EnumSet.of(ApiKeyPermission.ORDERS_WRITE));
    }

    @AfterEach
    void cleanUp() {
        CurrentUserContext.clear();
        jdbc.update("delete from api_keys where name like 'API-08%'");
    }

    @Test
    void api08WalkInOrderIsAFormalOrderWithSystemPricesApprovalReservationAndOrigin() {
        PublicOrderDTO created = orderService.create(key, walkIn("LOJA-" + sku, "3", "Ana Muianga", "+258 84 000 0001"));

        assertThat(created.number()).isNotBlank();
        assertThat(created.externalReference()).isEqualTo("LOJA-" + sku);
        assertThat(created.status()).isEqualTo(OrderStatus.PENDING_APPROVAL.name());
        assertThat(created.customerName()).isEqualTo("Ana Muianga");
        assertThat(created.customerNuit()).isNull();
        assertThat(created.lines()).singleElement().satisfies(line -> {
            assertThat(line.sku()).isEqualTo(sku);
            assertThat(line.unitPrice()).isEqualByComparingTo("60000.00");
            assertThat(line.quantity()).isEqualByComparingTo("3");
        });
        assertThat(created.totalBeforeTax()).isEqualByComparingTo("180000.00");
        assertThat(created.totalAmount()).isEqualByComparingTo(created.totalBeforeTax().add(created.taxAmount()));

        transactions.executeWithoutResult(tx -> {
            Order order = orderByNumber(created.number());
            assertThat(order.getKind()).isEqualTo(OrderKind.FORMAL_ORDER);
            assertThat(order.getCompany().getId()).isEqualTo(companyId);
            assertThat(order.isReservationActive()).isTrue();
            assertThat(order.getLines()).extracting(OrderLine::getReservedQuantity)
                    .allSatisfy(q -> assertThat(q).isEqualByComparingTo("3"));
            assertThat(order.getOrigin()).isEqualTo(OrderOrigin.API);
            assertThat(order.getOriginName()).isEqualTo("Loja online API-08");
            assertThat(order.getExternalReference()).isEqualTo("LOJA-" + sku);
            assertThat(order.getContactPhone()).isEqualTo("+258 84 000 0001");
            assertThat(order.getWalkInName()).isEqualTo("Ana Muianga");
            assertThat(order.getCreatedBy()).isEqualTo("API · Loja online API-08");
        });
        Long pendingApprovals = jdbc.queryForObject("select count(*) from approval_requests where document_type = 'ORDER'"
                + " and document_id = (select id from customer_orders where order_number = ?)", Long.class, created.number());
        assertThat(pendingApprovals).isEqualTo(1L);
    }

    @Test
    void api08StockReservedByOtherOrdersIsNotAvailableAndARefusedOrderLeavesNothing() {
        orderService.create(key, walkIn("A-" + sku, "8", "Ana", "840000001"));

        assertThatThrownBy(() -> orderService.create(key, walkIn("B-" + sku, "3", "Rui", "840000002")))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Stock disponível insuficiente");
        Long saved = jdbc.queryForObject("select count(*) from customer_orders where external_reference = ?",
                Long.class, "B-" + sku);
        assertThat(saved).isZero();
        assertThat(orderService.create(key, walkIn("C-" + sku, "2", "Rui", "840000002")).number()).isNotBlank();
    }

    @Test
    void api08NewNuitNeedsCustomerNameAndIsRegisteredThenReused() {
        String nuit = String.format("%09d", 400_000_000L + SEQ.incrementAndGet() % 99_999_999L);

        assertThatThrownBy(() -> orderService.create(key, byNuit("N1-" + sku, nuit, null)))
                .isInstanceOfSatisfying(PublicApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CUSTOMER_NAME_REQUIRED");
                    assertThat(e.status()).isEqualTo(400);
                });

        PublicOrderDTO first = orderService.create(key, byNuit("N2-" + sku, nuit, "Mercearia Bom Dia, Lda"));
        assertThat(first.customerName()).isEqualTo("Mercearia Bom Dia, Lda");
        assertThat(first.customerNuit()).isEqualTo(nuit);
        Client client = clientRepository.findByTaxIdAndCompaniesId(nuit, companyId).orElseThrow();
        assertThat(client.getName()).isEqualTo("Mercearia Bom Dia, Lda");

        PublicOrderDTO second = orderService.create(key, byNuit("N3-" + sku, nuit, null));
        assertThat(second.customerNuit()).isEqualTo(nuit);
        assertThat(clientRepository.findAll().stream().filter(c -> nuit.equals(c.getTaxId()))).hasSize(1);
    }

    @Test
    void api08WalkInNeedsContactAndUnknownSkuOrWarehouseIsNotFound() {
        assertThatThrownBy(() -> orderService.create(key, walkIn("W-" + sku, "1", "Ana", " ")))
                .isInstanceOfSatisfying(PublicApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTACT_REQUIRED"));

        CreatePublicOrderRequest unknownSku = new CreatePublicOrderRequest("S-" + sku, null, null, "Ana", "840000001",
                store.getName(), List.of(new PublicOrderLineRequest("NAO-EXISTE-API08", BigDecimal.ONE)), null);
        assertThatThrownBy(() -> orderService.create(key, unknownSku))
                .isInstanceOfSatisfying(PublicApiNotFoundException.class,
                        e -> assertThat(e.code()).isEqualTo("PRODUCT_NOT_FOUND"));

        CreatePublicOrderRequest unknownWarehouse = new CreatePublicOrderRequest("X-" + sku, null, null, "Ana",
                "840000001", "Armazém inexistente API-08", List.of(new PublicOrderLineRequest(sku, BigDecimal.ONE)), null);
        assertThatThrownBy(() -> orderService.create(key, unknownWarehouse))
                .isInstanceOfSatisfying(PublicApiNotFoundException.class,
                        e -> assertThat(e.code()).isEqualTo("WAREHOUSE_NOT_FOUND"));
    }

    @Test
    void api08RejectedOrderIsCancelledAndReleasesItsReservation() {
        PublicOrderDTO created = orderService.create(key, walkIn("R-" + sku, "10", "Ana", "840000001"));
        Long orderId = jdbc.queryForObject("select id from customer_orders where order_number = ?", Long.class,
                created.number());

        CurrentUserContext.runAsSystem(companyId, () -> approvalCallback.onRejected(orderId, "Sem condições"));

        transactions.executeWithoutResult(tx -> {
            Order order = orderByNumber(created.number());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.name());
            assertThat(order.isReservationActive()).isFalse();
            assertThat(order.getLines()).extracting(OrderLine::getReservedQuantity)
                    .allSatisfy(q -> assertThat(q).isEqualByComparingTo("0"));
        });
        assertThat(orderService.create(key, walkIn("R2-" + sku, "10", "Rui", "840000002")).number()).isNotBlank();
    }

    @Test
    void api08HttpCreatesWith201AndRefusesWithCodes() throws Exception {
        String secret;
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            secret = apiKeyService.create(new CreateApiKeyRequest("API-08 Loja",
                    Set.of(ApiKeyPermission.ORDERS_WRITE))).secret();
        } finally {
            CurrentUserContext.clear();
        }

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/public/v1/orders")
                        .header("X-Api-Key", secret).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walkIn("H-" + sku, "2", "Ana", "840000001"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(created.get("number").asText()).isNotBlank();
        assertThat(created.get("externalReference").asText()).isEqualTo("H-" + sku);
        assertThat(created.toString()).doesNotContain("\"id\"").doesNotContainIgnoringCase("purchasePrice");

        JsonNode noStock = objectMapper.readTree(mockMvc.perform(post("/api/public/v1/orders")
                        .header("X-Api-Key", secret).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walkIn("H2-" + sku, "99", "Ana", "840000001"))))
                .andExpect(status().isUnprocessableEntity()).andReturn().getResponse().getContentAsString());
        assertThat(noStock.get("code").asText()).isEqualTo("INSUFFICIENT_STOCK");

        JsonNode invalid = objectMapper.readTree(mockMvc.perform(post("/api/public/v1/orders")
                        .header("X-Api-Key", secret).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouse\":\"" + store.getName() + "\",\"lines\":[]}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        assertThat(invalid.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(invalid.get("message").asText()).isNotBlank();
    }

    private CreatePublicOrderRequest walkIn(String reference, String quantity, String contactName, String phone) {
        return new CreatePublicOrderRequest(reference, null, null, contactName, phone, store.getName(),
                List.of(new PublicOrderLineRequest(sku, new BigDecimal(quantity))), "Entregar à tarde");
    }

    private CreatePublicOrderRequest byNuit(String reference, String nuit, String customerName) {
        return new CreatePublicOrderRequest(reference, nuit, customerName, null, null, store.getName(),
                List.of(new PublicOrderLineRequest(sku, BigDecimal.ONE)), null);
    }

    private Order orderByNumber(String number) {
        return orderRepository.findByCompanyId(companyId).stream()
                .filter(o -> number.equals(o.getOrderNumber())).findFirst().orElseThrow();
    }
}
