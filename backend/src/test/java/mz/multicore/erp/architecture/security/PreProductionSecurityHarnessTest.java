package mz.multicore.erp.architecture.security;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AccountSecurityService;
import mz.multicore.erp.modules.users.service.DemoPasswordPurge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Ver docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md. */
@DataJpaTest
@Import(BCryptPasswordEncoder.class)
class PreProductionSecurityHarnessTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";

    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder encoder;

    private final AuthSessionService sessions = mock(AuthSessionService.class);
    private final AuditLogService audit = mock(AuditLogService.class);

    @Test
    @DisplayName("SC-25: Um código aceite não entra segunda vez")
    void sc25_codeIsSingleUse() {
        AccountSecurityService service = accountService();
        String secret = enroll(service, "ana");
        AppUser user = users.findByUsername("ana").orElseThrow();
        String next = TotpCodec.code(secret, step() + 1);

        assertThat(service.consumeCode(user, next)).isTrue();
        assertThat(service.consumeCode(user, next)).isFalse();
    }

    @Test
    @DisplayName("SC-25: Um código mais antigo do que o aceite também é recusado")
    void sc25_olderCodeRejected() {
        AccountSecurityService service = accountService();
        String secret = enroll(service, "rui");
        AppUser user = users.findByUsername("rui").orElseThrow();

        assertThat(service.consumeCode(user, TotpCodec.code(secret, step() + 1))).isTrue();
        assertThat(service.consumeCode(user, TotpCodec.code(secret, step()))).isFalse();
    }

    @Test
    @DisplayName("SC-25: Só um pedido ganha o mesmo intervalo")
    void sc25_claimIsAtomic() {
        AppUser user = saveUser("eva", encoder.encode("x"), false);

        assertThat(users.claimTotpStep(user.getId(), 500L)).isEqualTo(1);
        assertThat(users.claimTotpStep(user.getId(), 500L)).isZero();
        assertThat(users.claimTotpStep(user.getId(), 501L)).isEqualTo(1);
    }

    @Test
    @DisplayName("SC-26: O código usado na configuração não serve para o login")
    void sc26_setupCodeIsSpent() {
        AccountSecurityService service = accountService();
        saveUser("zeca", encoder.encode("x"), false);
        String secret = service.beginTwoFactorSetup("zeca").secret();
        String code = TotpCodec.code(secret, step());
        service.confirmTwoFactorSetup("zeca", code);

        assertThat(service.consumeCode(users.findByUsername("zeca").orElseThrow(), code)).isFalse();
    }

    @Test
    @DisplayName("SC-27: A senha de demonstração é substituída e a sessão termina")
    void sc27_demoPasswordReplaced() {
        saveUser("caixa1", encoder.encode("password"), false);
        AppUser strong = saveUser("forte", encoder.encode("uma-senha-longa-e-propria"), false);

        purge("").run(null);

        AppUser demo = users.findByUsername("caixa1").orElseThrow();
        assertThat(encoder.matches("password", demo.getPassword())).isFalse();
        assertThat(demo.isPasswordChangeRequired()).isTrue();
        verify(sessions).revokeUser("caixa1");
        assertThat(encoder.matches("uma-senha-longa-e-propria", strong.getPassword())).isTrue();
    }

    @Test
    @DisplayName("SC-28: Superadmin de demonstração sem variável recusa o arranque")
    void sc28_superAdminNeedsRecovery() {
        AppUser root = saveUser("superadmin", encoder.encode("superadmin"), true);
        String before = root.getPassword();

        assertThatThrownBy(() -> purge("").run(null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPERADMIN_PASSWORD");
        assertThat(users.findByUsername("superadmin").orElseThrow().getPassword()).isEqualTo(before);
    }

    @Test
    @DisplayName("SC-28: Com a variável, o superadmin recebe essa senha e tem de a trocar")
    void sc28_superAdminRecovered() {
        saveUser("superadmin", encoder.encode("superadmin"), true);

        purge("senha-nova-do-operador-1").run(null);

        AppUser root = users.findByUsername("superadmin").orElseThrow();
        assertThat(encoder.matches("senha-nova-do-operador-1", root.getPassword())).isTrue();
        assertThat(root.isPasswordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("SC-29: Senha antiga em texto simples passa a hash; a de demonstração é substituída")
    void sc29_plainTextHashed() {
        saveUser("antiga", "minha-senha-antiga", false);
        saveUser("antiga-demo", "admin", false);

        purge("").run(null);

        assertThat(encoder.matches("minha-senha-antiga", users.findByUsername("antiga").orElseThrow().getPassword())).isTrue();
        assertThat(users.findByUsername("antiga").orElseThrow().isPasswordChangeRequired()).isFalse();
        assertThat(encoder.matches("admin", users.findByUsername("antiga-demo").orElseThrow().getPassword())).isFalse();
    }

    private AccountSecurityService accountService() {
        return new AccountSecurityService(users, encoder, new AttachmentCrypto(KEY), sessions, audit, false);
    }

    private DemoPasswordPurge purge(String superAdminPassword) {
        return new DemoPasswordPurge(users, encoder, sessions, audit, superAdminPassword);
    }

    private String enroll(AccountSecurityService service, String username) {
        saveUser(username, encoder.encode("x"), false);
        String secret = service.beginTwoFactorSetup(username).secret();
        service.confirmTwoFactorSetup(username, TotpCodec.code(secret, step()));
        return secret;
    }

    private AppUser saveUser(String username, String password, boolean platformAdmin) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(username);
        user.setPassword(password);
        user.setRole("ADMIN");
        user.setActive(true);
        user.setPlatformAdmin(platformAdmin);
        user.setCreatedBy("TEST");
        return users.saveAndFlush(user);
    }

    private static long step() {
        return Instant.now().getEpochSecond() / 30;
    }
}
