package mz.multicore.erp.modules.users.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de AppUserService (Fase 121).
 */
class AppUserServiceDecompositionHarnessTest {

    private static final String BASE = "src/main/java/mz/multicore/erp/modules/users/service/";
    private static final List<String> FILES = List.of("AppUserService.java", "UserAuthenticationService.java",
            "ManagerPinService.java", "UserAccountAdministrationService.java", "UserAdminGuard.java");

    private static String readSource(String file) {
        try {
            return Files.readString(Path.of(BASE + file));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + file, e);
        }
    }

    private static long countLines(String file) {
        try (var lines = Files.lines(Path.of(BASE + file))) {
            return lines.count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + file, e);
        }
    }

    @Test
    @DisplayName("AUSD-01: Limites físicos estritos de linhas de código")
    void ausd01_limitesFisicosLinhasCodigo() {
        assertThat(countLines("AppUserService.java")).as("Fachada <= 155").isLessThanOrEqualTo(155);
        assertThat(countLines("UserAccountAdministrationService.java")).as("Administração <= 185").isLessThanOrEqualTo(185);
        assertThat(countLines("ManagerPinService.java")).as("PIN <= 110").isLessThanOrEqualTo(110);
        assertThat(countLines("UserAuthenticationService.java")).as("Autenticação <= 65").isLessThanOrEqualTo(65);
        assertThat(countLines("UserAdminGuard.java")).as("Guard <= 35").isLessThanOrEqualTo(35);
    }

    @Test
    @DisplayName("AUSD-02: Backend headless sem Swing nem JOptionPane")
    void ausd02_backendHeadless() {
        for (String file : FILES) {
            assertThat(readSource(file)).as(file).doesNotContain("JOptionPane", "javax.swing");
        }
    }

    @Test
    @DisplayName("AUSD-03: Delegação canónica e barreiras de segurança nos colaboradores")
    void ausd03_delegacaoCanonica() {
        assertThat(readSource("AppUserService.java"))
                .contains("authenticationService.authenticate(")
                .contains("managerPinService.verifyManagerPin(")
                .contains("accountAdministrationService.toggleUserStatus(")
                .doesNotContain("passwordEncoder.matches(", "requireAdmin()");
        assertThat(readSource("UserAccountAdministrationService.java"))
                .contains("countActiveByCompanyIdAndRole(companyId, \"ADMIN\") <= 1")
                .contains("authSessionService.revokeUser(");
        assertThat(readSource("ManagerPinService.java"))
                .contains("managerPinRateLimiter.isLocked(")
                .contains("managerPinRateLimiter.recordFailure(");
        assertThat(readSource("UserAuthenticationService.java"))
                .contains("isBcryptHash(stored)");
    }

    @Test
    @DisplayName("AUSD-04: Contratos públicos e construtores retrocompatíveis preservados")
    void ausd04_contratosPreservados() {
        assertThat(readSource("AppUserService.java"))
                .contains("@Autowired")
                .contains("public AppUser authenticate(String username, String password)")
                .contains("public VerifyManagerPinResponse verifyManagerPin(String pin, String clientIp)")
                .contains("public AppUser grantCompanyAccess(String username, Long companyId)")
                .contains("AuthSessionService authSessionService)")
                .contains("AppUserCompanyAccessRepository companyAccessRepository) {");
    }
}
