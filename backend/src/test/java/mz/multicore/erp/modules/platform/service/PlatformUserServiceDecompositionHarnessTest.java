package mz.multicore.erp.modules.platform.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUserServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/platform/service/PlatformUserService.java");
    private static final Path ACCOUNT_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/platform/service/PlatformUserAccountService.java");
    private static final Path ACCESS_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/platform/service/PlatformUserAccessService.java");

    @Test
    void platformUserServiceDelegatesToAccountAndAccessCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String accountSource = Files.readString(ACCOUNT_FILE);
        String accessSource = Files.readString(ACCESS_FILE);

        assertTrue(serviceSource.contains("accountService.createUser("));
        assertTrue(serviceSource.contains("accessService.grantAccess("));
        assertFalse(serviceSource.contains("new AppUser()"));

        assertTrue(accountSource.contains("PLATFORM_USER_CREATE"));
        assertTrue(accessSource.contains("PLATFORM_USER_REVOKE"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int accountLines = Files.readAllLines(ACCOUNT_FILE).size();
        int accessLines = Files.readAllLines(ACCESS_FILE).size();

        assertTrue(serviceLines <= 110, "PlatformUserService deve ter <= 110 linhas, mas tem " + serviceLines);
        assertTrue(accountLines <= 145, "PlatformUserAccountService deve ter <= 145 linhas, mas tem " + accountLines);
        assertTrue(accessLines <= 85, "PlatformUserAccessService deve ter <= 85 linhas, mas tem " + accessLines);
    }
}
