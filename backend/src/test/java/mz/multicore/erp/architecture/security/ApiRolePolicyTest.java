package mz.multicore.erp.architecture.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRolePolicyTest {

    @Test
    void employee_hasOnlyMinimumSelfServiceAccess() {
        assertTrue(ApiRolePolicy.allows("EMPLOYEE", "GET", "/api/license/status"));
        assertTrue(ApiRolePolicy.allows("EMPLOYEE", "GET", "/api/subscription/current"));
        assertFalse(ApiRolePolicy.allows("EMPLOYEE", "GET", "/api/comercial/clients"));
        assertFalse(ApiRolePolicy.allows("EMPLOYEE", "GET", "/api/finance/accounts"));
        assertFalse(ApiRolePolicy.allows("EMPLOYEE", "GET", "/api/hr/employees"));
        assertTrue(ApiRolePolicy.allows("EMPLOYEE", "GET",
                "/api/hr/occupational-health/employee/10/summary"));
        assertFalse(ApiRolePolicy.allows("EMPLOYEE", "GET", "/api/unknown"));
    }

    @Test
    void seller_hasCommercialAccessWithoutManagementAccess() {
        assertTrue(ApiRolePolicy.allows("SELLER", "GET", "/api/comercial/products/pos-catalog/page"));
        assertTrue(ApiRolePolicy.allows("SELLER", "POST", "/api/pos/checkout"));
        assertTrue(ApiRolePolicy.allows("SELLER", "POST", "/api/comercial/orders"));
        assertTrue(ApiRolePolicy.allows("SELLER", "POST", "/api/comercial/quotations"));
        assertTrue(ApiRolePolicy.allows("SELLER", "GET", "/api/support/tickets"));
        assertTrue(ApiRolePolicy.allows("SELLER", "GET", "/api/promotions/best"));
        assertTrue(ApiRolePolicy.allows("SELLER", "GET", "/api/print/invoice/10"));
        assertFalse(ApiRolePolicy.allows("SELLER", "POST", "/api/promotions"));
        assertFalse(ApiRolePolicy.allows("SELLER", "POST", "/api/comercial/quotations/10/convert-to-invoice"));
        assertFalse(ApiRolePolicy.allows("SELLER", "POST", "/api/comercial/orders/10/approve"));
        assertFalse(ApiRolePolicy.allows("SELLER", "POST", "/api/pos/returns"));
        assertFalse(ApiRolePolicy.allows("SELLER", "GET", "/api/finance/accounts"));
        assertFalse(ApiRolePolicy.allows("SELLER", "GET", "/api/users"));
    }

    @Test
    void manager_hasOperationalManagementWithoutAdministration() {
        assertTrue(ApiRolePolicy.allows("MANAGER", "GET", "/api/finance/accounts"));
        assertTrue(ApiRolePolicy.allows("MANAGER", "POST", "/api/comercial/orders/10/approve"));
        assertTrue(ApiRolePolicy.allows("MANAGER", "GET", "/api/hr/employees"));
        assertFalse(ApiRolePolicy.allows("MANAGER", "GET", "/api/users"));
        assertFalse(ApiRolePolicy.allows("MANAGER", "POST", "/api/backup/physical"));
    }

    @Test
    void admin_hasTenantWideAccess() {
        assertTrue(ApiRolePolicy.allows("ADMIN", "GET", "/api/users"));
        assertTrue(ApiRolePolicy.allows("ADMIN", "POST", "/api/backup/physical"));
        assertTrue(ApiRolePolicy.allows("ADMIN", "GET", "/api/unknown"));
    }
}
