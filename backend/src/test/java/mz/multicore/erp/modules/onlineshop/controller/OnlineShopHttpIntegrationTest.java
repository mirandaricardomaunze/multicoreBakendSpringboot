package mz.multicore.erp.modules.onlineshop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.OnlineShopRateLimiter;
import mz.multicore.erp.architecture.security.OnlineShopRoutes;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseService;
import mz.multicore.erp.modules.onlineshop.dto.LookupOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineRequest;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Harness LO-04 (docs/LOJA_ONLINE_SPEC.md §3.2): {@code /api/shop/{code}/**} answers without login; a disabled or
 * unknown shop is "Loja indisponível."; per client IP at most 60 requests a minute and 5 order attempts an hour
 * (429 {@code RATE_LIMIT_EXCEEDED} with {@code Retry-After}); a filled honeypot refuses the order and creates nothing.
 *
 * <p>Harness LO-09 (§3.3): the customer follows the order with its number and the checkout phone
 * ({@code POST /api/shop/{code}/orders/lookup}); a wrong or malformed phone and an unknown number all answer the same
 * "Encomenda não encontrada.", so the answer never reveals whether the order exists.
 *
 * <p>Harness LO-UI-01 (§3.5): the desktop reads and saves the settings by {@code GET}/{@code PUT
 * /api/online-shop/settings}, ADMIN only (403 for the other roles), with the LO-01 messages unchanged.
 * Seeded users: ana=ADMIN, joao=MANAGER, maria=EMPLOYEE (password "password").
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:online-shop-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnlineShopHttpIntegrationTest {

    private static final String CODE = "lo04-montra";
    private static final String SERVICE_SKU = "LO09-MONTAGEM";
    private static final String NOT_FOUND = "Encomenda não encontrada.";
    private static final String SETTINGS = "/api/online-shop/settings";

    @Autowired private ProductLookup productLookup;
    @Autowired private ProductCommandService productCommandService;
    @Autowired private CompanyLookup companyLookup;
    @Autowired private MockMvc mockMvc;
    @Autowired private OnlineShopSettingsService settingsService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void lo04_montraRespondeSemLogin() throws Exception {
        openShop(true);

        MockHttpServletResponse response = mockMvc.perform(from("10.4.0.1", get(products(CODE))))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8)).isArray()).isTrue();
    }

    @Test
    void lo04_lojaDesactivadaOuInexistenteIndisponivel() throws Exception {
        openShop(false);

        for (String code : List.of(CODE, "nao-existe")) {
            MockHttpServletResponse response = mockMvc.perform(from("10.4.0.2", get(products(code))))
                    .andReturn().getResponse();
            assertThat(response.getStatus()).as(code).isEqualTo(400);
            assertThat(message(response)).as(code).isEqualTo("Loja indisponível.");
        }
    }

    @Test
    void lo04_sexagesimoPrimeiroPedidoNoMinutoRecebe429() throws Exception {
        for (int i = 0; i < OnlineShopRateLimiter.REQUESTS_PER_MINUTE; i++) {
            assertThat(mockMvc.perform(from("10.4.0.3", get(products("nao-existe")))).andReturn().getResponse()
                    .getStatus()).as("pedido %d dentro do limite", i + 1).isNotEqualTo(429);
        }

        MockHttpServletResponse refused = mockMvc.perform(from("10.4.0.3", get(products("nao-existe"))))
                .andReturn().getResponse();
        assertTooManyRequests(refused, 60L);

        assertThat(mockMvc.perform(from("10.4.0.4", get(products("nao-existe")))).andReturn().getResponse()
                .getStatus()).as("outro IP não é afectado").isNotEqualTo(429);
    }

    @Test
    void lo04_sextaEncomendaNaHoraRecebe429() throws Exception {
        for (int i = 0; i < OnlineShopRateLimiter.ORDERS_PER_HOUR; i++) {
            assertThat(mockMvc.perform(from("10.4.0.5", order("nao-existe", null))).andReturn().getResponse()
                    .getStatus()).as("encomenda %d dentro do limite", i + 1).isEqualTo(400);
        }

        MockHttpServletResponse refused = mockMvc.perform(from("10.4.0.5", order("nao-existe", null)))
                .andReturn().getResponse();
        assertTooManyRequests(refused, 3600L);
        assertThat(message(refused)).contains("5 encomendas por hora");

        assertThat(mockMvc.perform(from("10.4.0.5", get(products("nao-existe")))).andReturn().getResponse()
                .getStatus()).as("a montra continua aberta a esse IP").isNotEqualTo(429);
        assertThat(mockMvc.perform(from("10.4.0.6", order("nao-existe", null))).andReturn().getResponse()
                .getStatus()).as("outro IP não é afectado").isEqualTo(400);
    }

    @Test
    void lo04_honeypotPreenchidoRecusaSemCriarNada() throws Exception {
        openShop(true);
        long ordersBefore = jdbc.queryForObject("select count(*) from customer_orders", Long.class);

        MockHttpServletResponse refused = mockMvc.perform(from("10.4.0.7", order(CODE, "http://spam.example")))
                .andReturn().getResponse();

        assertThat(refused.getStatus()).isEqualTo(400);
        assertThat(message(refused)).isEqualTo("Não foi possível registar a encomenda.");
        assertThat(jdbc.queryForObject("select count(*) from customer_orders", Long.class)).isEqualTo(ordersBefore);

        MockHttpServletResponse human = mockMvc.perform(from("10.4.0.7", order(CODE, "  ")))
                .andReturn().getResponse();
        assertThat(message(human)).as("campo vazio segue para as regras do checkout")
                .isNotEqualTo("Não foi possível registar a encomenda.");
    }

    @Test
    void lo04_soOPostDeEncomendasContaParaOLimiteHorario() {
        assertThat(OnlineShopRoutes.isOrderPlacement("POST", "/api/shop/mercearia-baixa/orders")).isTrue();
        assertThat(OnlineShopRoutes.isOrderPlacement("post", "/api/shop/mercearia-baixa/orders/")).isTrue();
        assertThat(OnlineShopRoutes.isOrderPlacement("GET", "/api/shop/mercearia-baixa/orders")).isFalse();
        assertThat(OnlineShopRoutes.isOrderPlacement("POST", "/api/shop/mercearia-baixa/products")).isFalse();
        assertThat(OnlineShopRoutes.isOrderPlacement("POST", "/api/shop/orders")).isFalse();
        assertThat(OnlineShopRoutes.isOrderPlacement("POST", "/api/shopx/a/orders")).isFalse();
    }

    @Test
    void lo09_consultaPorNumeroETelemovelDevolveOEstado() throws Exception {
        Long pickup = openShop(true);
        JsonNode placed = placeServiceOrder("10.9.0.1", pickup);
        String number = placed.get("number").asText();

        MockHttpServletResponse found = mockMvc.perform(from("10.9.0.1", lookup(CODE, number, "+258 84 123 4567")))
                .andReturn().getResponse();

        assertThat(found.getStatus()).isEqualTo(200);
        JsonNode order = objectMapper.readTree(found.getContentAsString(StandardCharsets.UTF_8));
        assertThat(order.get("number").asText()).isEqualTo(number);
        assertThat(order.get("statusLabel").asText()).isNotBlank().isEqualTo(placed.get("statusLabel").asText());
        assertThat(order.get("totalAmount").decimalValue())
                .isEqualByComparingTo(placed.get("totalAmount").decimalValue());
        assertThat(order.get("lines").get(0).get("sku").asText()).isEqualTo(SERVICE_SKU);
        assertThat(order.has("id")).as("sem ids internos").isFalse();
        assertThat(order.has("contactPhone")).as("não devolve o contacto").isFalse();
    }

    @Test
    void lo09_telemovelErradoNaoRevelaAEncomenda() throws Exception {
        Long pickup = openShop(true);
        String number = placeServiceOrder("10.9.0.2", pickup).get("number").asText();

        List<MockHttpServletRequestBuilder> misses = List.of(
                lookup(CODE, number, "821234567"),
                lookup(CODE, number, "12345"),
                lookup(CODE, number, null),
                lookup(CODE, "ENC-NAO-EXISTE", "841234567"),
                lookup(CODE, null, "841234567"));
        String firstBody = null;
        for (MockHttpServletRequestBuilder miss : misses) {
            MockHttpServletResponse response = mockMvc.perform(from("10.9.0.2", miss)).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(message(response)).isEqualTo(NOT_FOUND);
            JsonNode body = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
            String comparable = body.get("code") + "|" + body.get("message");
            if (firstBody == null) {
                firstBody = comparable;
            }
            assertThat(comparable).as("mesma resposta quer a encomenda exista quer não").isEqualTo(firstBody);
        }
    }

    @Test
    void lo09_consultaNaoContaParaOLimiteDeEncomendas() {
        assertThat(OnlineShopRoutes.isOrderPlacement("POST", "/api/shop/mercearia-baixa/orders/lookup")).isFalse();
    }

    @Test
    void loUi01_adminLeEGravaAsDefinicoesPeloDesktop() throws Exception {
        String[] admin = login("ana");
        long companyId = Long.parseLong(admin[1]);
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        Long pickup;
        try {
            pickup = warehouseService.getSalesWarehousesByCompany(companyId).get(0).getId();
        } finally {
            CurrentUserContext.clear();
        }
        String body = "{\"enabled\":true,\"publicCode\":\" LoUi01-Definicoes \",\"pickupWarehouseIds\":[" + pickup
                + "],\"deliveryZones\":[{\"name\":\"Matola\",\"fee\":150}],\"contactPhone\":\"841234567\","
                + "\"autoCancelDays\":null}";

        MockHttpServletResponse saved = settings(put(SETTINGS), admin, body);
        assertThat(saved.getStatus()).as(saved.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        MockHttpServletResponse read = settings(get(SETTINGS), admin, "");
        assertThat(read.getStatus()).isEqualTo(200);
        JsonNode dto = objectMapper.readTree(read.getContentAsString(StandardCharsets.UTF_8));
        assertThat(dto.get("enabled").asBoolean()).isTrue();
        assertThat(dto.get("publicCode").asText()).isEqualTo("loui01-definicoes");
        assertThat(dto.get("pickupWarehouseIds").get(0).asLong()).isEqualTo(pickup);
        assertThat(dto.get("deliveryZones").get(0).get("name").asText()).isEqualTo("Matola");
        assertThat(dto.get("autoCancelDays").asInt()).as("7 por omissão").isEqualTo(7);

        MockHttpServletResponse refused = settings(put(SETTINGS), admin,
                "{\"enabled\":true,\"publicCode\":\"  \",\"pickupWarehouseIds\":[" + pickup + "]}");
        assertThat(refused.getStatus()).isEqualTo(400);
        assertThat(message(refused)).as("mensagem do backend sem alteração")
                .isEqualTo("Indique o código público da loja.");

        assertThat(settings(put(SETTINGS), admin, "{\"enabled\":false,\"publicCode\":\"loui01-definicoes\"}")
                .getStatus()).isEqualTo(200);
    }

    @Test
    void loUi01_soOAdminChegaAsDefinicoes() throws Exception {
        String companyId = login("ana")[1];
        for (String user : List.of("joao", "maria")) {
            String[] other = {login(user)[0], companyId};
            assertThat(settings(get(SETTINGS), other, "").getStatus()).as(user + " GET").isEqualTo(403);
            assertThat(settings(put(SETTINGS), other, "{\"enabled\":false}").getStatus()).as(user + " PUT")
                    .isEqualTo(403);
        }
    }

    private MockHttpServletResponse settings(MockHttpServletRequestBuilder request, String[] session, String body)
            throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + session[0])
                        .header("X-Company-Id", session[1])
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
    }

    /** @return token and the first company of the user */
    private String[] login(String username) throws Exception {
        JsonNode login = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        return new String[]{login.get("token").asText(), login.get("companies").get(0).get("id").asText()};
    }

    private JsonNode placeServiceOrder(String ip, Long pickup) throws Exception {
        publishServiceProduct();
        PlaceOnlineShopOrderRequest request = new PlaceOnlineShopOrderRequest("Ana Machava", "841234567", null,
                OnlineShopFulfilment.LEVANTAMENTO, pickup, null, null, null,
                List.of(new OnlineShopOrderLineRequest(SERVICE_SKU, BigDecimal.ONE)));
        MockHttpServletResponse response = mockMvc.perform(from(ip,
                        post(OnlineShopRoutes.PREFIX + "/" + CODE + "/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))))
                .andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(201);
        return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    /** A published service product (no stock control), so the order needs no stock fixture. */
    private void publishServiceProduct() {
        long companyId = companyId();
        if (productLookup.findBySkuForCompany(SERVICE_SKU, companyId).isPresent()) {
            return;
        }
        Product product = new Product();
        product.setSku(SERVICE_SKU);
        product.setName("Montagem ao domicílio");
        product.setUnitPrice(new BigDecimal("500.00"));
        product.setPurchasePrice(BigDecimal.ZERO);
        product.setMinStock(BigDecimal.ZERO);
        product.setSaleType(ProductSaleType.SERVICE);
        product.setStockTracked(false);
        product.setPublishedOnline(true);
        product.getCompanies().add(companyLookup.require(companyId));
        productCommandService.saveProduct(product);
    }

    private MockHttpServletRequestBuilder lookup(String code, String number, String phone) throws Exception {
        return post(OnlineShopRoutes.PREFIX + "/" + code + "/orders/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LookupOnlineShopOrderRequest(number, phone)));
    }

    private long companyId() {
        return jdbc.queryForObject("select min(id) from companies", Long.class);
    }

    /** @return the shop's pickup store */
    private Long openShop(boolean enabled) {
        long companyId = companyId();
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            Long pickup = warehouseService.getSalesWarehousesByCompany(companyId).get(0).getId();
            settingsService.save(new SaveOnlineShopSettingsRequest(enabled, CODE, List.of(pickup), List.of(),
                    null, null));
            return pickup;
        } finally {
            CurrentUserContext.clear();
        }
    }

    private MockHttpServletRequestBuilder order(String code, String website) throws Exception {
        PlaceOnlineShopOrderRequest request = new PlaceOnlineShopOrderRequest("Ana Machava", "841234567", null,
                OnlineShopFulfilment.ENTREGA, null, "Zona inexistente", "Rua X",
                null, List.of(new OnlineShopOrderLineRequest("SKU-INEXISTENTE", BigDecimal.ONE)), website);
        return post(OnlineShopRoutes.PREFIX + "/" + code + "/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private static String products(String code) {
        return OnlineShopRoutes.PREFIX + "/" + code + "/products";
    }

    private static MockHttpServletRequestBuilder from(String ip, MockHttpServletRequestBuilder builder) {
        return builder.with(request -> {
            request.setRemoteAddr(ip);
            return request;
        });
    }

    private void assertTooManyRequests(MockHttpServletResponse refused, long maxRetryAfter) throws Exception {
        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(refused.getHeader("Retry-After"))).isBetween(1L, maxRetryAfter);
        assertThat(refused.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode error = objectMapper.readTree(refused.getContentAsString(StandardCharsets.UTF_8));
        assertThat(error.get("code").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(error.get("message").asText()).isNotBlank();
    }

    private String message(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8)).get("message").asText();
    }
}
