package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ver docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md. */
class SessionAndLoginLimitsHarnessTest {

    @Test
    @DisplayName("SC-06: Credenciais de demonstração não arrancam acessíveis na rede")
    void sc06_demoDataStaysLocal() {
        assertThatThrownBy(() -> new DemoExposureGuard(true, "0.0.0.0")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new DemoExposureGuard(true, "")).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new DemoExposureGuard(true, "127.0.0.1")).doesNotThrowAnyException();
        assertThatCode(() -> new DemoExposureGuard(false, "0.0.0.0")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SC-07: Código não traz senha de superadmin nem demonstração por omissão")
    void sc07_noPredictableDefaults() throws Exception {
        Path architecture = Path.of("src/main/java/mz/multicore/erp/architecture");
        assertThat(Files.readString(architecture.resolve("ReferenceAndAdminDataSeeder.java")))
                .contains("${app.superadmin.password:}");
        assertThat(Files.readString(architecture.resolve("DataLoader.java")))
                .contains("${app.seed-demo-data:false}");
        assertThat(Files.readString(Path.of("src/main/resources/application.properties")))
                .contains("server.address=${SERVER_ADDRESS:127.0.0.1}");
    }

    @Test
    @DisplayName("SC-08: Em produção, o servidor não arranca sem chave de cifra")
    void sc08_productionRequiresAttachmentKey() throws Exception {
        assertThatThrownBy(() -> new AttachmentCrypto("", true)).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new AttachmentCrypto("", false)).doesNotThrowAnyException();
        assertThat(Files.readString(Path.of("src/main/resources/application-prod.properties")))
                .contains("security.attachment-key.required=true");
        assertThat(Files.readString(Path.of("../docker-compose.yml")))
                .contains("ATTACHMENT_KEY: ${ATTACHMENT_KEY:?");
    }

    @Test
    @DisplayName("SC-09: Sessões expiradas são removidas sem o token voltar a ser usado")
    void sc09_expiredSessionsArePurged() {
        AuthSessionService sessions = new AuthSessionService();
        sessions.create(user("ana"));
        sessions.create(user("rui"));

        sessions.purgeExpired(Instant.now().plus(Duration.ofHours(9)));

        assertThat(sessions.activeSessions()).isZero();
    }

    @RepeatedTest(200)
    @DisplayName("SC-10: Cada utilizador tem um número limitado de sessões")
    void sc10_sessionsPerUserAreLimited() {
        AuthSessionService sessions = new AuthSessionService();
        AuthSessionService.AuthSession first = sessions.create(user("ana"));
        for (int i = 0; i < AuthSessionService.MAX_SESSIONS_PER_USER + 5; i++) sessions.create(user("ana"));
        sessions.create(user("rui"));

        assertThat(sessions.activeSessions()).isEqualTo(AuthSessionService.MAX_SESSIONS_PER_USER + 1);
        assertThatThrownBy(() -> sessions.requireValid(first.token())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("SC-11: Um login válido não apaga as falhas do IP")
    void sc11_successDoesNotResetIpCounter() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 15, 5, java.time.Clock.systemUTC());
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("vitima" + i, "10.0.0.1");
            limiter.recordSuccess("atacante", "10.0.0.1");
        }
        limiter.recordFailure("vitima4", "10.0.0.1");

        assertThatThrownBy(() -> limiter.checkAllowed("outra", "10.0.0.1")).isInstanceOf(BusinessRuleException.class);
    }

    private static AppUser user(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        return user;
    }
}
