package mz.multicore.erp.architecture.security;

import mz.multicore.erp.modules.users.service.AccountSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PG-01 — login com canal PORTAL só para ADMIN/MANAGER;
 * PG-02 — e só com verificação em dois passos activa, com o código pedido como no desktop
 * PG-04 — as tentativas falhadas do portal contam no mesmo {@link LoginRateLimiter} do desktop
 * (PORTAL_GESTOR_SPEC §3.1).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-login;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalLoginTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final String LOCKED_PREFIX = "Demasiadas tentativas falhadas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountSecurityService accountSecurityService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void pg01PortalLoginAcceptsAdmin() throws Exception {
        String secret = enableTwoFactor("ana");
        portalLogin("ana", "PORTAL", currentCode(secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.companies.length()").value(2));
    }

    @Test
    void pg01PortalLoginAcceptsManager() throws Exception {
        String secret = enableTwoFactor("joao");
        portalLogin("joao", "PORTAL", currentCode(secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.companies[0].role").value("MANAGER"));
    }

    @Test
    void pg01PortalLoginRejectsEmployeeWithoutToken() throws Exception {
        portalLogin("maria", "PORTAL", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").value(PortalLoginPolicy.ROLE_REFUSED_MESSAGE));
    }

    @Test
    void pg01ChannelIsCaseInsensitive() throws Exception {
        portalLogin("maria", "portal", null).andExpect(status().isBadRequest());
    }

    @Test
    void pg01DesktopLoginUnchangedForEmployee() throws Exception {
        desktopLogin("maria")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void pg02PortalLoginRejectsManagerWithoutTwoFactor() throws Exception {
        disableTwoFactor("joao");
        portalLogin("joao", "PORTAL", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").value(PortalLoginPolicy.TWO_FACTOR_REFUSED_MESSAGE));
    }

    @Test
    void pg02PortalLoginAsksForCodeBeforeToken() throws Exception {
        enableTwoFactor("ana");
        portalLogin("ana", "PORTAL", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.twoFactorRequired").value(true));
    }

    @Test
    void pg02PortalLoginRejectsWrongCode() throws Exception {
        String secret = enableTwoFactor("ana");
        String wrong = currentCode(secret).equals("000000") ? "111111" : "000000";
        portalLogin("ana", "PORTAL", wrong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void pg02DesktopLoginUnchangedForManagerWithoutTwoFactor() throws Exception {
        disableTwoFactor("joao");
        desktopLogin("joao")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void pg04PortalWrongPasswordLocksPortalAndDesktop() throws Exception {
        String ip = "198.51.100.41";
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            login("joao", "errada", "PORTAL", null, ip).andExpect(status().is4xxClientError());
        }
        expectLocked(login("joao", "password", "PORTAL", null, ip));
        expectLocked(login("joao", "password", null, null, ip));
    }

    @Test
    void pg04DesktopFailuresAlsoLockPortal() throws Exception {
        String ip = "198.51.100.42";
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            login("ana", "errada", null, null, ip).andExpect(status().is4xxClientError());
        }
        expectLocked(login("ana", "password", "PORTAL", null, ip));
    }

    @Test
    void pg04PortalWrongTwoFactorCodesCountAndLock() throws Exception {
        String ip = "198.51.100.43";
        String secret = enableTwoFactor("ana");
        String wrong = currentCode(secret).equals("000000") ? "111111" : "000000";
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            login("ana", "password", "PORTAL", wrong, ip).andExpect(status().isBadRequest());
        }
        expectLocked(login("ana", "password", "PORTAL", currentCode(secret), ip));
    }

    private static void expectLocked(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith(LOCKED_PREFIX)));
    }

    private String enableTwoFactor(String username) {
        disableTwoFactor(username);
        String secret = accountSecurityService.beginTwoFactorSetup(username).secret();
        accountSecurityService.confirmTwoFactorSetup(username, currentCode(secret));
        // O código da configuração fica gasto; o teste liberta o intervalo para o login seguinte.
        jdbc.update("update app_users set last_totp_step = null where username = ?", username);
        return secret;
    }

    private void disableTwoFactor(String username) {
        jdbc.update("update app_users set totp_secret = null, last_totp_step = null where username = ?", username);
    }

    private static String currentCode(String secret) {
        return TotpCodec.code(secret, Instant.now().getEpochSecond() / 30);
    }

    private ResultActions desktopLogin(String username) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"password"}
                        """.formatted(username)));
    }

    private ResultActions portalLogin(String username, String channel, String totpCode) throws Exception {
        return login(username, "password", channel, totpCode, "127.0.0.1");
    }

    /** Cada cenário PG-04 usa a sua origem, para o bloqueio não tocar nos outros testes. */
    private ResultActions login(String username, String password, String channel, String totpCode,
                                String clientIp) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","channel":%s,"totpCode":%s}
                        """.formatted(username, password, jsonOrNull(channel), jsonOrNull(totpCode))));
    }

    private static String jsonOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
