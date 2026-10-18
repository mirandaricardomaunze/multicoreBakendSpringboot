package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness desktop: perfil Vendedor (SV-50 a SV-54)")
class DesktopSellerRoleHarnessTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SV-50: SELLER é apresentado como Vendedor")
    void humanRole_seller_returnsPortugueseLabel() {
        assertEquals("Vendedor", UIHelper.humanRole("SELLER"));
    }

    @Test
    @DisplayName("SV-51: desktop reconhece vendedor na hierarquia comercial")
    void sellerSession_hasSellerCapabilityWithoutManagerCapability() {
        CurrentUserContext.setCurrentUser("vendedor", "seller");
        assertTrue(SignedInUser.isSellerOrAbove());
        assertFalse(SignedInUser.isManagerOrAdmin());
        assertFalse(SignedInUser.isAdmin());
    }

    @Test
    @DisplayName("SV-52: gestor e administrador herdam capacidade comercial")
    void elevatedRoles_haveSellerCapability() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        assertTrue(SignedInUser.isSellerOrAbove());
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        assertTrue(SignedInUser.isSellerOrAbove());
    }

    @Test
    @DisplayName("SV-53: vendedor navega apenas nas areas comerciais")
    void sellerNavigation_isRestrictedToCommercialAreas() {
        assertTrue(SignedInUser.canNavigate("SELLER", "dashboard"));
        assertTrue(SignedInUser.canNavigate("SELLER", "pos"));
        assertTrue(SignedInUser.canNavigate("SELLER", "comercial"));
        assertTrue(SignedInUser.canNavigate("SELLER", "clientes"));
        assertTrue(SignedInUser.canNavigate("SELLER", "crm"));
        assertFalse(SignedInUser.canNavigate("SELLER", "financeiro"));
        assertFalse(SignedInUser.canNavigate("SELLER", "hr"));
        assertFalse(SignedInUser.canNavigate("SELLER", "config"));
    }

    @Test
    @DisplayName("SV-54: colaborador tem navegacao minima")
    void employeeNavigation_isMinimal() {
        assertTrue(SignedInUser.canNavigate("EMPLOYEE", "dashboard"));
        assertTrue(SignedInUser.canNavigate("EMPLOYEE", "notifications"));
        assertFalse(SignedInUser.canNavigate("EMPLOYEE", "pos"));
        assertFalse(SignedInUser.canNavigate("EMPLOYEE", "comercial"));
        assertFalse(SignedInUser.canNavigate("EMPLOYEE", "financeiro"));
    }
}
