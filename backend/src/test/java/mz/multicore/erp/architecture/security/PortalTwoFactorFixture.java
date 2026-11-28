package mz.multicore.erp.architecture.security;

import mz.multicore.erp.modules.users.service.AccountSecurityService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

/** Activa a verificação em dois passos nos testes do portal (PORTAL_GESTOR_SPEC §3.1). */
final class PortalTwoFactorFixture {

    private PortalTwoFactorFixture() {
    }

    /** @return o segredo TOTP, com o intervalo actual libertado para o login seguinte */
    static String enable(AccountSecurityService accountSecurity, JdbcTemplate jdbc, String username) {
        disable(jdbc, username);
        String secret = accountSecurity.beginTwoFactorSetup(username).secret();
        accountSecurity.confirmTwoFactorSetup(username, currentCode(secret));
        // O código da configuração fica gasto; o teste liberta o intervalo para o login seguinte.
        jdbc.update("update app_users set last_totp_step = null where username = ?", username);
        return secret;
    }

    static void disable(JdbcTemplate jdbc, String username) {
        jdbc.update("update app_users set totp_secret = null, last_totp_step = null where username = ?", username);
    }

    static String currentCode(String secret) {
        return TotpCodec.code(secret, Instant.now().getEpochSecond() / 30);
    }
}
