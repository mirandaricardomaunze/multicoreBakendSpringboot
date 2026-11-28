package mz.multicore.erp.modules.onlineshop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.OnlineShopRateLimiter;
import mz.multicore.erp.architecture.security.OnlineShopRoutes;
import mz.multicore.erp.modules.inventory.service.WarehouseService;
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

/**
 * Harness LO-04 (docs/LOJA_ONLINE_SPEC.md §3.2): {@code /api/shop/{code}/**} answers without login; a disabled or
 * unknown shop is "Loja indisponível."; per client IP at most 60 requests a minute and 5 order attempts an hour
 * (429 {@code RATE_LIMIT_EXCEEDED} with {@code Retry-After}); a filled honeypot refuses the order and creates nothing.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:online-shop-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnlineShopHttpIntegrationTest {

    private static final String CODE = "lo04-montra";

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

    private void openShop(boolean enabled) {
        long companyId = jdbc.queryForObject("select min(id) from companies", Long.class);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            Long pickup = warehouseService.getSalesWarehousesByCompany(companyId).get(0).getId();
            settingsService.save(new SaveOnlineShopSettingsRequest(enabled, CODE, List.of(pickup), List.of(),
                    null, null));
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
