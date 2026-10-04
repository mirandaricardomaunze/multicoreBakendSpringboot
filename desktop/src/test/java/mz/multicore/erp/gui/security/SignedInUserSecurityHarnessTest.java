package mz.multicore.erp.gui.security;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.SignedInUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Verificação de Segurança e Permissões de UI (SignedInUser)")
class SignedInUserSecurityHarnessTest {

    @BeforeEach
    @AfterEach
    void resetContext() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("UI-SEC-01: Sem sessão iniciada todos os privilégios são recusados")
    void testNoSessionRefusesAllPrivileges() {
        assertFalse(SignedInUser.isAdmin());
        assertFalse(SignedInUser.isManagerOrAdmin());
        assertFalse(SignedInUser.isSuperAdmin());
        assertFalse(SignedInUser.canAccessForensicAudit());
        assertFalse(SignedInUser.canAccessCashFlowForecast());
        assertFalse(SignedInUser.canAccessSystemMonitoring());
    }

    @Test
    @DisplayName("UI-SEC-02: Perfil SELLER / EMPLOYEE restringe áreas sensíveis de gestão e telemetria")
    void testSellerRoleRestrictions() {
        CurrentUserContext.setCurrentUser("operador_pos", "SELLER");

        assertFalse(SignedInUser.isAdmin());
        assertFalse(SignedInUser.isManagerOrAdmin());
        assertFalse(SignedInUser.canAccessForensicAudit());
        assertFalse(SignedInUser.canAccessCashFlowForecast());
        assertFalse(SignedInUser.canAccessSystemMonitoring());
        assertFalse(SignedInUser.canManageCompanySettings());
    }

    @Test
    @DisplayName("UI-SEC-03: Perfil MANAGER acede a relatórios e gestão mas restringe monitorização de sistema")
    void testManagerRolePermissions() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        assertFalse(SignedInUser.isAdmin());
        assertTrue(SignedInUser.isManagerOrAdmin());
        assertTrue(SignedInUser.canAccessForensicAudit());
        assertTrue(SignedInUser.canAccessCashFlowForecast());
        assertTrue(SignedInUser.canApproveStockWaste());
        assertFalse(SignedInUser.canAccessSystemMonitoring());
    }

    @Test
    @DisplayName("UI-SEC-04: Perfil ADMIN possui privilégios totais na aplicação desktop")
    void testAdminRoleFullPermissions() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");

        assertTrue(SignedInUser.isAdmin());
        assertTrue(SignedInUser.isManagerOrAdmin());
        assertTrue(SignedInUser.canAccessForensicAudit());
        assertTrue(SignedInUser.canAccessCashFlowForecast());
        assertTrue(SignedInUser.canAccessSystemMonitoring());
        assertTrue(SignedInUser.canManageCompanySettings());
    }
}
