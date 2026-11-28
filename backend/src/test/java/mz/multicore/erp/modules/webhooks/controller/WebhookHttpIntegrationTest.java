package mz.multicore.erp.modules.webhooks.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.webhooks.service.WebhookAddressResolver;
import mz.multicore.erp.modules.webhooks.service.WebhookDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Harness WH-08 over HTTP (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.5): the internal webhook routes under
 * {@code /api/integrations/webhooks} are ADMIN-only (403 by the HTTP policy for anyone else), always on the active
 * company, rule violations and other-company/unknown ids answer 400 with the PT-MZ message, and the secret leaves only
 * in the 201 of the creation. DNS and the dispatcher are fakes: no network.
 * Seeded users: ana=ADMIN, joao=MANAGER, maria=EMPLOYEE (password "password").
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:webhook-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "webhooks.retry.enabled=false",
        "webhooks.retention.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookHttpIntegrationTest {

    private static final String HOOKS = "/api/integrations/webhooks";
    private static final String PUBLIC_URL = "https://hooks.loja-wh08.example.com/multicore";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private JdbcTemplate jdbc;
    @MockBean private WebhookAddressResolver resolver;
    @MockBean private WebhookDispatcher dispatcher;

    private record Session(String token, String companyId) {}

    /** Every response body seen by the test except the 201 of a creation: none may carry a secret. */
    private final List<String> nonCreationBodies = new ArrayList<>();

    @BeforeEach
    void publicDns() throws Exception {
        when(resolver.resolve(anyString())).thenAnswer(inv -> "privado.wh08.example.com".equals(inv.getArgument(0))
                ? List.of(InetAddress.getByAddress(new byte[]{10, 0, 0, 7}))
                : List.of(InetAddress.getByAddress(new byte[]{93, (byte) 184, (byte) 216, 34})));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from webhook_deliveries where subscription_id in "
                + "(select id from webhook_subscriptions where url like '%wh08%')");
        jdbc.update("delete from webhook_subscriptions where url like '%wh08%'");
        for (String body : nonCreationBodies) {
            assertThat(body).doesNotContain("whsec_").doesNotContainIgnoringCase("secret");
        }
    }

    @Test
    void adminCreatesListsDeactivatesAndActivatesWithTheSecretOnlyInTheCreation() throws Exception {
        Session admin = login("ana", null);

        MockHttpServletResponse createdResponse = send(post(HOOKS), admin,
                "{\"url\":\"" + PUBLIC_URL + "\",\"events\":[\"invoice.issued\",\"order.status_changed\"]}");
        assertThat(createdResponse.getStatus()).as(createdResponse.getContentAsString()).isEqualTo(201);
        JsonNode created = read(createdResponse.getContentAsString());
        String secret = created.get("secret").asText();
        assertThat(secret).startsWith("whsec_");
        long id = created.get("subscription").get("id").asLong();
        assertThat(created.get("subscription").get("active").asBoolean()).isTrue();

        String listed = ok(get(HOOKS), admin);
        assertThat(read(listed).findValuesAsText("url")).contains(PUBLIC_URL);
        assertThat(listed).doesNotContain(secret);

        assertThat(read(ok(post(HOOKS + "/" + id + "/deactivate"), admin)).get("active").asBoolean()).isFalse();
        assertThat(read(ok(post(HOOKS + "/" + id + "/activate"), admin)).get("active").asBoolean()).isTrue();
        assertThat(ok(get(HOOKS + "/" + id + "/deliveries"), admin)).isEqualTo("[]");
    }

    @Test
    void onlyTheAdminReachesTheWebhookRoutes() throws Exception {
        Session admin = login("ana", null);
        long id = createSubscription(admin, PUBLIC_URL);
        long delivery = insertDelivery(admin.companyId(), id, "FAILED");

        for (String user : new String[]{"joao", "maria"}) {
            Session other = login(user, admin.companyId());
            List<MockHttpServletRequestBuilder> routes = List.of(
                    get(HOOKS), post(HOOKS), post(HOOKS + "/" + id + "/activate"),
                    post(HOOKS + "/" + id + "/deactivate"), get(HOOKS + "/alerts"),
                    get(HOOKS + "/" + id + "/deliveries"), post(HOOKS + "/deliveries/" + delivery + "/resend"));
            for (MockHttpServletRequestBuilder route : routes) {
                assertThat(send(route, other, "{\"url\":\"https://intruso.wh08.example.com\",\"events\":"
                        + "[\"invoice.issued\"]}").getStatus()).as(user).isEqualTo(403);
            }
        }
        assertThat(jdbc.queryForObject("select count(*) from webhook_subscriptions where url like '%intruso%'",
                Long.class)).isZero();
        assertThat(jdbc.queryForObject("select active from webhook_subscriptions where id = ?", Boolean.class, id))
                .isTrue();
        assertThat(jdbc.queryForObject("select status from webhook_deliveries where id = ?", String.class, delivery))
                .isEqualTo("FAILED");
        assertThat(mockMvc.perform(get(HOOKS)).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void ruleViolationsAndOtherCompanyIdsAreBadRequestsWithThePtMessage() throws Exception {
        Session admin = login("ana", null);
        assertBadRequest(send(post(HOOKS), admin,
                "{\"url\":\"https://privado.wh08.example.com/x\",\"events\":[\"invoice.issued\"]}"),
                "Não são permitidos endereços privados ou locais no webhook.");
        assertBadRequest(send(post(HOOKS), admin, "{\"url\":\"" + PUBLIC_URL + "\",\"events\":[]}"),
                "Escolha pelo menos um evento.");
        assertThat(send(post(HOOKS), admin, "").getStatus()).isEqualTo(400);

        long id = createSubscription(admin, PUBLIC_URL);
        long delivery = insertDelivery(admin.companyId(), id, "FAILED");
        Session elsewhere = login("ana", otherCompanyThan(admin.companyId()));
        assertBadRequest(send(post(HOOKS + "/" + id + "/deactivate"), elsewhere, ""), "Subscrição não encontrada.");
        assertBadRequest(send(get(HOOKS + "/" + id + "/deliveries"), elsewhere, ""), "Subscrição não encontrada.");
        assertBadRequest(send(post(HOOKS + "/deliveries/" + delivery + "/resend"), elsewhere, ""),
                "Entrega não encontrada.");
        assertBadRequest(send(post(HOOKS + "/999999/activate"), admin, ""), "Subscrição não encontrada.");
        assertThat(read(ok(get(HOOKS), elsewhere)).findValuesAsText("url")).doesNotContain(PUBLIC_URL);
        assertThat(read(ok(get(HOOKS + "/alerts"), elsewhere)).findValuesAsText("url")).doesNotContain(PUBLIC_URL);
    }

    @Test
    void deliveryLogFiltersAndCapsPagesAndResendsOnlyFailedOnes() throws Exception {
        Session admin = login("ana", null);
        long id = createSubscription(admin, PUBLIC_URL);
        long failed = insertDelivery(admin.companyId(), id, "FAILED");
        long delivered = insertDelivery(admin.companyId(), id, "DELIVERED");

        JsonNode all = read(ok(get(HOOKS + "/" + id + "/deliveries"), admin));
        assertThat(all.findValuesAsText("id")).containsExactlyInAnyOrder(String.valueOf(failed),
                String.valueOf(delivered));
        JsonNode onlyFailed = read(ok(get(HOOKS + "/" + id + "/deliveries").param("status", "FAILED"), admin));
        assertThat(onlyFailed.findValuesAsText("id")).containsExactly(String.valueOf(failed));
        assertThat(onlyFailed.toString()).doesNotContain("evento-corpo-wh08");
        assertBadRequest(send(get(HOOKS + "/" + id + "/deliveries").param("size", "101"), admin, ""),
                "Tamanho de página inválido (1 a 100).");

        JsonNode resent = read(ok(post(HOOKS + "/deliveries/" + failed + "/resend"), admin));
        assertThat(resent.get("status").asText()).isEqualTo("PENDING");
        assertThat(resent.get("attempts").asInt()).isZero();
        assertBadRequest(send(post(HOOKS + "/deliveries/" + delivered + "/resend"), admin, ""),
                "Só se reenviam entregas falhadas.");
    }

    @Test
    void alertsListTheAutomaticallyDisabledSubscription() throws Exception {
        Session admin = login("ana", null);
        long id = createSubscription(admin, PUBLIC_URL);
        jdbc.update("update webhook_subscriptions set active = false, consecutive_failures = 20, "
                + "auto_disabled_at = ? where id = ?", Timestamp.valueOf(LocalDateTime.now()), id);

        JsonNode alerts = read(ok(get(HOOKS + "/alerts"), admin));
        assertThat(alerts.findValuesAsText("subscriptionId")).contains(String.valueOf(id));
        for (JsonNode alert : alerts) {
            if (alert.get("subscriptionId").asLong() == id) {
                assertThat(alert.get("message").asText()).isNotBlank();
                assertThat(alert.get("consecutiveFailures").asInt()).isEqualTo(20);
            }
        }

        ok(post(HOOKS + "/" + id + "/activate"), admin);
        assertThat(read(ok(get(HOOKS + "/alerts"), admin)).findValuesAsText("subscriptionId"))
                .doesNotContain(String.valueOf(id));
    }

    // --- data ---

    private long createSubscription(Session admin, String url) throws Exception {
        MockHttpServletResponse response = send(post(HOOKS), admin,
                "{\"url\":\"" + url + "\",\"events\":[\"invoice.issued\"]}");
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
        return read(response.getContentAsString()).get("subscription").get("id").asLong();
    }

    private long insertDelivery(String companyId, long subscriptionId, String status) {
        jdbc.update("insert into webhook_deliveries (company_id, subscription_id, event_id, event_type, body, status, "
                        + "attempts, last_http_status, last_error, created_at) values (?, ?, ?, 'INVOICE_ISSUED', "
                        + "'{\"evento-corpo-wh08\":true}', ?, 6, 500, 'Erro 500', ?)",
                Long.valueOf(companyId), subscriptionId, "evt_wh08_" + status + System.nanoTime(), status,
                Timestamp.valueOf(LocalDateTime.now()));
        return jdbc.queryForObject("select max(id) from webhook_deliveries where subscription_id = ?", Long.class,
                subscriptionId);
    }

    // --- HTTP ---

    private void assertBadRequest(MockHttpServletResponse response, String message) throws Exception {
        String body = response.getContentAsString();
        assertThat(response.getStatus()).as(body).isEqualTo(400);
        assertThat(read(body).path("message").asText()).as(body).isEqualTo(message);
        assertThat(body).doesNotContain("Exception").doesNotContain("at mz.");
    }

    private String ok(MockHttpServletRequestBuilder req, Session s) throws Exception {
        MockHttpServletResponse response = send(req, s, "");
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
        return response.getContentAsString();
    }

    private MockHttpServletResponse send(MockHttpServletRequestBuilder req, Session s, String body) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(auth(req, s).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn().getResponse();
        if (response.getStatus() != 201) nonCreationBodies.add(response.getContentAsString());
        return response;
    }

    private String otherCompanyThan(String companyId) throws Exception {
        for (JsonNode company : loginJson("ana").get("companies")) {
            if (!company.get("id").asText().equals(companyId)) return company.get("id").asText();
        }
        throw new AssertionError("ana devia ter uma segunda empresa");
    }

    private Session login(String username, String companyId) throws Exception {
        JsonNode login = loginJson(username);
        String active = companyId != null ? companyId : login.get("companies").get(0).get("id").asText();
        return new Session(login.get("token").asText(), active);
    }

    private JsonNode loginJson(String username) throws Exception {
        return read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Session s) {
        return req.header("Authorization", "Bearer " + s.token()).header("X-Company-Id", s.companyId());
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
