package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.dto.UserSecurityRequestsDTOs;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AppUserService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * HARNESS-SEC-RL-001: Validação de Rate Limiting e Proteção Ativa contra Força Bruta.
 *
 * <p>Testa o bloqueio de tentativas de autenticação e adivinhação de PIN de supervisor/gestor:
 * <ul>
 *   <li>RL-01: Bloqueio de login por utilizador após 5 falhas</li>
 *   <li>RL-02: Reset do contador de login após sucesso</li>
 *   <li>RL-03: Bloqueio de login por IP após exceder limite de IP (Credential Stuffing)</li>
 *   <li>RL-04: Bloqueio de verificação de PIN de gerente após 3 falhas consecutivas</li>
 *   <li>RL-05: Reset do contador de PIN após sucesso</li>
 *   <li>RL-06: Isolamento de PIN entre diferentes empresas e endereços IP</li>
 *   <li>RL-07: Expiração automática do bloqueio após a janela de lockout</li>
 * </ul>
 */
class RateLimitingAndBruteForceHarnessTest {

    private MutableClock clock;
    private LoginRateLimiter loginRateLimiter;
    private ManagerPinRateLimiter managerPinRateLimiter;
    private AppUserService appUserService;
    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;
    private CompanyRepository companyRepository;
    private AppUserCompanyAccessRepository companyAccessRepository;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-03T10:00:00Z"));
        // 5 tentativas utilizador, 15 min lockout, 10 tentativas IP (para teste ágil)
        loginRateLimiter = new LoginRateLimiter(5, 15, 10, clock);
        // 3 tentativas PIN, 5 min lockout
        managerPinRateLimiter = new ManagerPinRateLimiter(3, 5, clock);

        appUserRepository = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        companyRepository = mock(CompanyRepository.class);
        companyAccessRepository = mock(AppUserCompanyAccessRepository.class);

        appUserService = new AppUserService(
                appUserRepository,
                passwordEncoder,
                companyRepository,
                companyAccessRepository,
                managerPinRateLimiter
        );
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("RL-01: Bloqueio de login por utilizador após 5 falhas seguidas")
    void testLoginUserLockoutAfterMaxAttempts() {
        String username = "operador.caixa";
        String ip = "192.168.1.10";

        // 4 tentativas falhadas consecutivas não devem bloquear
        for (int i = 1; i <= 4; i++) {
            assertDoesNotThrow(() -> loginRateLimiter.checkAllowed(username, ip));
            loginRateLimiter.recordFailure(username, ip);
        }

        // A 5ª tentativa falhada atinge o limite máximo
        assertDoesNotThrow(() -> loginRateLimiter.checkAllowed(username, ip));
        loginRateLimiter.recordFailure(username, ip);

        // A 6ª tentativa deve ser sumariamente bloqueada com mensagem clara
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loginRateLimiter.checkAllowed(username, ip));
        assertTrue(ex.getMessage().contains("Demasiadas tentativas falhadas"));
        assertTrue(ex.getMessage().contains("minuto(s)"));
    }

    @Test
    @DisplayName("RL-02: Reset de contador de falhas de login após sucesso")
    void testLoginSuccessResetsAttemptCounter() {
        String username = "gestor.loja";
        String ip = "192.168.1.20";

        // 4 falhas
        for (int i = 1; i <= 4; i++) {
            loginRateLimiter.recordFailure(username, ip);
        }

        // Sucesso na 5ª tentativa
        loginRateLimiter.recordSuccess(username, ip);

        // Próximas tentativas devem começar de zero (4 falhas adicionais sem bloqueio)
        for (int i = 1; i <= 4; i++) {
            assertDoesNotThrow(() -> loginRateLimiter.checkAllowed(username, ip));
            loginRateLimiter.recordFailure(username, ip);
        }
        assertDoesNotThrow(() -> loginRateLimiter.checkAllowed(username, ip));
    }

    @Test
    @DisplayName("RL-03: Bloqueio de IP após tentativas abusivas de múltiplos utilizadores (Credential Stuffing)")
    void testLoginIpLockoutOnCredentialStuffing() {
        String attackerIp = "10.0.0.99";

        // Tenta 10 utilizadores diferentes a partir do mesmo IP
        for (int i = 1; i <= 10; i++) {
            String user = "victim" + i;
            assertDoesNotThrow(() -> loginRateLimiter.checkAllowed(user, attackerIp));
            loginRateLimiter.recordFailure(user, attackerIp);
        }

        // Agora qualquer utilizador vindo deste IP deve ser bloqueado
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loginRateLimiter.checkAllowed("novo.usuario", attackerIp));
        assertTrue(ex.getMessage().contains("endereço IP"));

        // Mas utilizadores de outro IP legítimo continuam permitidos
        assertDoesNotThrow(() -> loginRateLimiter.checkAllowed("novo.usuario", "192.168.1.50"));
    }

    @Test
    @DisplayName("RL-04: Bloqueio de PIN de gerente após 3 falhas consecutivas")
    void testManagerPinLockoutAfterThreeFailures() {
        Long companyId = 1L;
        String clientIp = "192.168.1.100";

        when(appUserRepository.findAll()).thenReturn(Collections.emptyList());

        // 1ª tentativa falhada
        UserSecurityRequestsDTOs.VerifyManagerPinResponse res1 = appUserService.verifyManagerPin("1111", clientIp);
        assertFalse(res1.approved());
        assertFalse(managerPinRateLimiter.isLocked(companyId, clientIp));

        // 2ª tentativa falhada
        UserSecurityRequestsDTOs.VerifyManagerPinResponse res2 = appUserService.verifyManagerPin("2222", clientIp);
        assertFalse(res2.approved());
        assertFalse(managerPinRateLimiter.isLocked(companyId, clientIp));

        // 3ª tentativa falhada -> Activa o bloqueio
        UserSecurityRequestsDTOs.VerifyManagerPinResponse res3 = appUserService.verifyManagerPin("3333", clientIp);
        assertFalse(res3.approved());
        assertTrue(res3.message().contains("Demasiadas tentativas de PIN incorreto"));
        assertTrue(managerPinRateLimiter.isLocked(companyId, clientIp));

        // 4ª tentativa: mesmo se o utilizador digitasse um PIN correto ou qualquer valor, é bloqueado de imediato
        UserSecurityRequestsDTOs.VerifyManagerPinResponse res4 = appUserService.verifyManagerPin("4444", clientIp);
        assertFalse(res4.approved());
        assertTrue(res4.message().contains("Bloqueado por"));
        assertTrue(res4.message().contains("minuto(s)"));
    }

    @Test
    @DisplayName("RL-05: Reset de contador de PIN de gerente quando verificado com sucesso")
    void testManagerPinSuccessResetsFailureCounter() {
        Long companyId = 1L;
        String clientIp = "192.168.1.101";

        AppUser manager = new AppUser();
        manager.setUsername("gerente");
        manager.setName("Gerente Geral");
        manager.setRole("MANAGER");
        manager.setActive(true);
        manager.setManagerPinHash("$2a$10$pinHash");
        when(appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(any())).thenReturn(List.of(manager));
        when(appUserRepository.findAll()).thenReturn(List.of(manager));
        when(passwordEncoder.matches("9999", "$2a$10$pinHash")).thenReturn(true);
        when(passwordEncoder.matches("0000", "$2a$10$pinHash")).thenReturn(false);

        // 2 falhas
        appUserService.verifyManagerPin("0000", clientIp);
        appUserService.verifyManagerPin("0000", clientIp);
        assertFalse(managerPinRateLimiter.isLocked(companyId, clientIp));

        // 3ª tentativa: acerta o PIN
        UserSecurityRequestsDTOs.VerifyManagerPinResponse okRes = appUserService.verifyManagerPin("9999", clientIp);
        assertTrue(okRes.approved());
        assertTrue(okRes.message().contains("verificado com sucesso"));

        // Contador foi limpo: pode errar novamente sem bloqueio imediato
        appUserService.verifyManagerPin("0000", clientIp);
        assertFalse(managerPinRateLimiter.isLocked(companyId, clientIp));
    }

    @Test
    @DisplayName("RL-06: Isolamento multi-tenant e multi-IP de PIN")
    void testManagerPinTenantAndIpIsolation() {
        String ip1 = "10.0.1.10";
        String ip2 = "10.0.1.20";

        // IP 1 falha 3 vezes e bloqueia
        managerPinRateLimiter.recordFailure(1L, ip1);
        managerPinRateLimiter.recordFailure(1L, ip1);
        managerPinRateLimiter.recordFailure(1L, ip1);
        assertTrue(managerPinRateLimiter.isLocked(1L, ip1));

        // IP 2 no mesmo ou outro tenant NÃO deve estar bloqueado
        assertFalse(managerPinRateLimiter.isLocked(1L, ip2));
        assertFalse(managerPinRateLimiter.isLocked(2L, ip1));
    }

    @Test
    @DisplayName("RL-07: Expiração de bloqueio de login e PIN após janela de tempo")
    void testLockoutExpirationAfterTimeWindow() {
        String username = "operador.expirado";
        String ip = "192.168.1.150";

        // Bloqueia utilizador (5 falhas)
        for (int i = 0; i < 5; i++) {
            loginRateLimiter.recordFailure(username, ip);
        }
        assertThrows(BusinessRuleException.class, () -> loginRateLimiter.checkAllowed(username, ip));

        // Bloqueia PIN (3 falhas)
        for (int i = 0; i < 3; i++) {
            managerPinRateLimiter.recordFailure(1L, ip);
        }
        assertTrue(managerPinRateLimiter.isLocked(1L, ip));

        // Avança o relógio em 16 minutos (lockout de login é 15 min, de PIN é 5 min)
        clock.advance(Duration.ofMinutes(16));

        // Ambos devem estar desbloqueados agora
        assertDoesNotThrow(() -> loginRateLimiter.checkAllowed(username, ip));
        assertFalse(managerPinRateLimiter.isLocked(1L, ip));
    }

    /**
     * Relógio mutável em memória para testes determinísticos de tempo.
     */
    private static class MutableClock extends Clock {
        private final AtomicReference<Instant> currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initialInstant) {
            this.currentInstant = new AtomicReference<>(initialInstant);
            this.zone = ZoneId.of("UTC");
        }

        void advance(Duration duration) {
            currentInstant.updateAndGet(i -> i.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant.get();
        }
    }
}
