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
 * (PORTAL_GESTOR_SPEC §3.1).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-login;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalLoginTest {

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
        String code = totpCode == null ? "null" : "\"" + totpCode + "\"";
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"password","channel":"%s","totpCode":%s}
                        """.formatted(username, channel, code)));
    }
}
