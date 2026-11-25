package mz.multicore.erp.architecture.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AccountSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PG-03 — o token de portal expira 12 h depois do login ou 30 min depois do último pedido, o que vier primeiro;
 * logout invalida-o; as sessões do desktop não mudam (PORTAL_GESTOR_SPEC §3.1).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-session;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalSessionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthSessionService sessions;
    @Autowired private AuthSessionStore store;
    @Autowired private AccountSecurityService accountSecurityService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void pg03PortalLoginIssuesPortalSessionWithIdleExpiry() throws Exception {
        JsonNode login = portalLogin("ana");
        Instant issued = Instant.now();
        Instant expiresAt = Instant.parse(login.get("expiresAt").asText());

        assertThat(Duration.between(issued, expiresAt)).isBetween(Duration.ofMinutes(29), Duration.ofMinutes(31));
        assertThat(jdbc.queryForObject("select portal from auth_sessions where token_hash = ?", Boolean.class,
                AuthSessionService.hash(login.get("token").asText()))).isTrue();
    }

    @Test
    void pg03DesktopLoginKeepsItsEightHourSession() throws Exception {
        JsonNode login = json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"maria\",\"password\":\"password\"}"))
                .andExpect(status().isOk()));
        Instant expiresAt = Instant.parse(login.get("expiresAt").asText());

        assertThat(Duration.between(Instant.now(), expiresAt)).isGreaterThan(Duration.ofHours(7));
        assertThat(jdbc.queryForObject("select portal from auth_sessions where token_hash = ?", Boolean.class,
                AuthSessionService.hash(login.get("token").asText()))).isFalse();
    }

    @Test
    void pg03PortalTokenExpiresAfterThirtyMinutesWithoutUse() {
        MutableClock clock = new MutableClock();
        AuthSessionService service = new AuthSessionService(store, clock);
        String token = service.create(user(), false, true).token();

        clock.advance(Duration.ofMinutes(29));
        assertThat(service.requireValid(token).portal()).isTrue();

        clock.advance(Duration.ofMinutes(31));
        assertExpired(service, token);
    }

    @Test
    void pg03PortalTokenStaysAliveWhileUsedButNotBeyondTwelveHours() {
        MutableClock clock = new MutableClock();
        AuthSessionService service = new AuthSessionService(store, clock);
        String token = service.create(user(), false, true).token();

        for (int minutes = 20; minutes < 12 * 60; minutes += 20) {
            clock.advance(Duration.ofMinutes(20));
            service.requireValid(token);
        }
        clock.advance(Duration.ofMinutes(20)); // 12 h depois do login, usado há 20 min
        assertExpired(service, token);
    }

    @Test
    void pg03DesktopTokenHasNoIdleTimeout() {
        MutableClock clock = new MutableClock();
        AuthSessionService service = new AuthSessionService(store, clock);
        String token = service.create(user(), false).token();

        clock.advance(Duration.ofHours(2));
        assertThat(service.requireValid(token).portal()).isFalse();
    }

    @Test
    void pg03LogoutInvalidatesPortalToken() throws Exception {
        String token = portalLogin("joao").get("token").asText();
        assertThat(sessions.requireValid(token).portal()).isTrue();

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertExpired(sessions, token);
    }

    private static void assertExpired(AuthSessionService service, String token) {
        assertThatThrownBy(() -> service.requireValid(token))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sessão inválida ou expirada.");
    }

    private JsonNode portalLogin(String username) throws Exception {
        String secret = PortalTwoFactorFixture.enable(accountSecurityService, jdbc, username);
        String code = PortalTwoFactorFixture.currentCode(secret);
        return json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password","channel":"PORTAL","totpCode":"%s"}
                                """.formatted(username, code)))
                .andExpect(status().isOk()));
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static AppUser user() {
        AppUser user = new AppUser();
        user.setUsername("portal-" + UUID.randomUUID().toString().substring(0, 8));
        return user;
    }

    /** Relógio que o teste avança à mão. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
