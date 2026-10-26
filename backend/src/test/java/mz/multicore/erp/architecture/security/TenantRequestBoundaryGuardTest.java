package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantRequestBoundaryGuardTest {

    record RequestWithCompany(Long companyId, String value) {}
    record RequestWithoutCompany(String value) {}

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(10L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void acceptsMatchingTenantInUrlAndBody() {
        assertDoesNotThrow(() -> TenantRequestBoundaryGuard.validateUrlCompany("10", Map.of("companyId", "10")));
        assertDoesNotThrow(() -> TenantRequestBoundaryGuard.validateBody(new RequestWithCompany(10L, "ok")));
    }

    @Test
    void rejectsCrossTenantInQueryPathAndBody() {
        assertThrows(BusinessRuleException.class,
                () -> TenantRequestBoundaryGuard.validateUrlCompany("11", Map.of()));
        assertThrows(BusinessRuleException.class,
                () -> TenantRequestBoundaryGuard.validateUrlCompany(null, Map.of("companyId", "11")));
        assertThrows(BusinessRuleException.class,
                () -> TenantRequestBoundaryGuard.validateBody(new RequestWithCompany(11L, "ataque")));
    }

    @Test
    void ignoresBodiesWithoutTenantAndPlatformContextWithoutCompany() {
        assertDoesNotThrow(() -> TenantRequestBoundaryGuard.validateBody(new RequestWithoutCompany("ok")));
        CurrentUserContext.clear();
        assertDoesNotThrow(() -> TenantRequestBoundaryGuard.validateBody(new RequestWithCompany(99L, "plataforma")));
    }
}
