package mz.multicore.erp.modules.support.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/support/service/SupportService.java");
    private static final Path COMPANY_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/support/service/CompanySupportTicketService.java");
    private static final Path PLATFORM_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/support/service/PlatformSupportAdminService.java");

    @Test
    void supportServiceDelegatesToCompanyAndPlatformCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String companySource = Files.readString(COMPANY_FILE);
        String platformSource = Files.readString(PLATFORM_FILE);

        assertTrue(serviceSource.contains("companyTicketService.openTicket("));
        assertTrue(serviceSource.contains("platformAdminService.addSuperAdminReply("));
        assertFalse(serviceSource.contains("new SupportTicket()"));

        assertTrue(companySource.contains("O pedido pertence a outra empresa."));
        assertTrue(platformSource.contains("countByTicketId("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int companyLines = Files.readAllLines(COMPANY_FILE).size();
        int platformLines = Files.readAllLines(PLATFORM_FILE).size();

        assertTrue(serviceLines <= 105, "SupportService deve ter <= 105 linhas, mas tem " + serviceLines);
        assertTrue(companyLines <= 125, "CompanySupportTicketService deve ter <= 125 linhas, mas tem " + companyLines);
        assertTrue(platformLines <= 120, "PlatformSupportAdminService deve ter <= 120 linhas, mas tem " + platformLines);
    }
}
