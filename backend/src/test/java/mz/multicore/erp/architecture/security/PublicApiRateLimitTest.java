package mz.multicore.erp.architecture.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import mz.multicore.erp.modules.publicapi.service.ApiKeyService;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Harness API-05 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2): each API key may make 60 requests per minute; the 61st
 * gets 429 with {@code Retry-After} (seconds) and JSON {@code code} {@code RATE_LIMIT_EXCEEDED} + PT-MZ message.
 * The limit is per key: one key spending its quota never blocks another.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:public-api-rate;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiRateLimitTest {

    private static final String ROUTE = "/api/public/v1/products";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void cleanUp() {
        CurrentUserContext.clear();
        jdbc.update("delete from api_keys where name like 'API-05%'");
    }

    @Test
    void api05SixtyFirstRequestInAMinuteGets429WithRetryAfter() throws Exception {
        long companyId = anyCompany();
        String busy = createKey(companyId, "API-05 Loja online");
        String other = createKey(companyId, "API-05 Contabilista");

        for (int i = 0; i < PublicApiRateLimiter.REQUESTS_PER_MINUTE; i++) {
            assertThat(mockMvc.perform(get(ROUTE).header("X-Api-Key", busy)).andReturn().getResponse().getStatus())
                    .as("pedido %d dentro do limite", i + 1).isNotEqualTo(429);
        }

        MockHttpServletResponse refused = mockMvc.perform(get(ROUTE).header("X-Api-Key", busy))
                .andReturn().getResponse();
        assertThat(refused.getStatus()).isEqualTo(429);
        long retryAfter = Long.parseLong(refused.getHeader("Retry-After"));
        assertThat(retryAfter).isBetween(1L, 60L);
        assertThat(refused.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode error = objectMapper.readTree(refused.getContentAsString());
        assertThat(error.get("code").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(error.get("message").asText()).isNotBlank();
        assertThat(refused.getContentAsString()).doesNotContain("Exception", "at mz.");

        assertThat(mockMvc.perform(get(ROUTE).header("X-Api-Key", other)).andReturn().getResponse().getStatus())
                .as("outra chave não é afectada").isNotEqualTo(429);
    }

    @Test
    void api05WindowReopensAfterTheMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-26T10:00:00Z"));
        PublicApiRateLimiter limiter = new PublicApiRateLimiter(clock);

        for (int i = 0; i < 60; i++) {
            assertThat(limiter.acquire(7L)).isEmpty();
        }
        clock.advance(Duration.ofSeconds(20));
        OptionalLong retryAfter = limiter.acquire(7L);
        assertThat(retryAfter).hasValue(40L);
        assertThat(limiter.acquire(8L)).as("limite é por chave").isEmpty();

        clock.advance(Duration.ofSeconds(40));
        assertThat(limiter.acquire(7L)).as("janela nova depois do minuto").isEmpty();
    }

    @Test
    void api05RetryAfterIsAtLeastOneSecond() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-26T10:00:00Z"));
        PublicApiRateLimiter limiter = new PublicApiRateLimiter(clock);
        for (int i = 0; i < 60; i++) {
            limiter.acquire(1L);
        }
        clock.advance(Duration.ofMillis(59_900));

        assertThat(limiter.acquire(1L)).hasValue(1L);
    }

    private String createKey(long companyId, String name) {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            return apiKeyService.create(new CreateApiKeyRequest(name, EnumSet.of(ApiKeyPermission.CATALOG_READ)))
                    .secret();
        } finally {
            CurrentUserContext.clear();
        }
    }

    private long anyCompany() {
        return jdbc.queryForObject("select min(id) from companies", Long.class);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
