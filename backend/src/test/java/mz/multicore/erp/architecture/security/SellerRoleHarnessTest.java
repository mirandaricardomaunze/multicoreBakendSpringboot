package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Harness: perfil Vendedor (SV-01 a SV-06)")
class SellerRoleHarnessTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SV-01: SELLER é um perfil válido e normalizado")
    void normalize_sellerLowerCase_returnsCanonicalRole() {
        assertEquals("SELLER", UserRole.normalize(" seller "));
    }

    @Test
    @DisplayName("SV-02: vendedor pode executar operação comercial normal")
    void requireSeller_seller_allowsOperation() {
        CurrentUserContext.setCurrentUser("vendedor", "SELLER");
        assertDoesNotThrow(() -> PermissionGuard.requireSeller("registar encomenda"));
    }

    @Test
    @DisplayName("SV-03: gestor e administrador herdam capacidade de vendedor")
    void requireSeller_managerAndAdmin_allowOperation() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        assertDoesNotThrow(() -> PermissionGuard.requireSeller("registar encomenda"));
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        assertDoesNotThrow(() -> PermissionGuard.requireSeller("registar encomenda"));
    }

    @Test
    @DisplayName("SV-04: funcionário sem perfil de venda é recusado")
    void requireSeller_employee_deniesOperation() {
        CurrentUserContext.setCurrentUser("colaborador", "EMPLOYEE");
        assertThrows(BusinessRuleException.class,
                () -> PermissionGuard.requireSeller("registar encomenda"));
    }

    @Test
    @DisplayName("SV-05: vendedor não recebe alçada de gestor")
    void requireManagerOrAdmin_seller_deniesOperation() {
        CurrentUserContext.setCurrentUser("vendedor", "SELLER");
        assertThrows(BusinessRuleException.class,
                () -> PermissionGuard.requireManagerOrAdmin("aprovar encomenda"));
    }

    @Test
    @DisplayName("SV-06: papel efectivo respeita o acesso da empresa activa")
    void roleForCompany_companyAccess_overridesLegacyRole() {
        Company company = new Company();
        company.setId(7L);
        AppUser user = new AppUser();
        user.setRole("EMPLOYEE");
        user.grantCompany(company, "SELLER");

        assertEquals("SELLER", user.getRoleForCompany(7L));
    }
}
