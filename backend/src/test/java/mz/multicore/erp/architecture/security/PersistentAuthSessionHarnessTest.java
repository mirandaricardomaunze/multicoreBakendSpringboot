package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ver docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md, SC-33 a SC-35. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:auth-sessions;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class PersistentAuthSessionHarnessTest {

    @Autowired private AuthSessionService sessions;
    @Autowired private AuthSessionStore store;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("SC-33: Uma sessão continua válida depois de reiniciar o servidor")
    void sc33_sessionSurvivesRestart() {
        AuthSessionService.AuthSession session = sessions.create(user(), true);

        AuthSessionService afterRestart = new AuthSessionService(store);
        AuthSessionService.AuthSession again = afterRestart.requireValid(session.token());

        assertThat(again.username()).isEqualTo(session.username());
        assertThat(again.restricted()).isTrue();
        assertThat(again.expiresAt()).isEqualTo(session.expiresAt());
    }

    @Test
    @DisplayName("SC-34: A base de dados guarda só o hash do token")
    void sc34_onlyTheTokenHashIsStored() {
        String username = user().getUsername();
        AuthSessionService.AuthSession session = sessions.create(user(username));

        List<String> hashes = jdbc.queryForList(
                "select token_hash from auth_sessions where username = ?", String.class, username);

        assertThat(hashes).containsExactly(AuthSessionService.hash(session.token()));
        assertThat(hashes.get(0)).isNotEqualTo(session.token()).hasSize(64);
        assertThat(jdbc.queryForObject("select count(*) from auth_sessions where token_hash = ?",
                Integer.class, session.token())).isZero();
    }

    @Test
    @DisplayName("SC-35: Terminar sessões numa instância vale em todas")
    void sc35_revocationIsSharedBetweenInstances() {
        AuthSessionService other = new AuthSessionService(store);
        String username = user().getUsername();
        AuthSessionService.AuthSession first = sessions.create(user(username));
        AuthSessionService.AuthSession second = sessions.create(user(username));

        other.revoke(first.token());
        assertThatThrownBy(() -> sessions.requireValid(first.token())).isInstanceOf(BusinessRuleException.class);
        assertThat(sessions.requireValid(second.token()).username()).isEqualTo(username);

        other.revokeUser(username);
        assertThatThrownBy(() -> sessions.requireValid(second.token())).isInstanceOf(BusinessRuleException.class);
    }

    private static AppUser user() {
        return user("sessao-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private static AppUser user(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        return user;
    }
}
