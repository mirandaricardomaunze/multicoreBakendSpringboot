package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.SourceLines;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectPurchaseServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/purchases/service");

    @Test
    void purchaseRegistrationAndQueryStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("DirectPurchaseService.java"));
        String registration = Files.readString(DIR.resolve("DirectPurchaseRegistrationService.java"));
        String query = Files.readString(DIR.resolve("DirectPurchaseQueryService.java"));

        assertTrue(facade.contains("registrationService.createPurchase(request)"));
        assertTrue(facade.contains("queryService.findPurchasesByCompany(companyId)"));
        assertFalse(facade.contains("PurchaseRegisteredEvent"));
        assertTrue(registration.contains("PurchaseRegisteredEvent"));
        assertTrue(query.contains("new PurchaseDTO("));
        assertTrue(SourceLines.code(DIR.resolve("DirectPurchaseService.java")) <= 85);
        assertTrue(SourceLines.code(DIR.resolve("DirectPurchaseRegistrationService.java")) <= 145);
        assertTrue(SourceLines.code(DIR.resolve("DirectPurchaseQueryService.java")) <= 75);
    }
}
