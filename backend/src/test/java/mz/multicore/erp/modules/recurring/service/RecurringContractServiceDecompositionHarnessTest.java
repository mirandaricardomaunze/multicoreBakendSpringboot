package mz.multicore.erp.modules.recurring.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringContractServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/recurring/service");

    @Test
    void contractLifecycleAndBatchBillingStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("RecurringContractService.java"));
        String lifecycle = Files.readString(DIR.resolve("RecurringContractLifecycleService.java"));
        String processor = Files.readString(DIR.resolve("RecurringBatchBillingProcessor.java"));

        assertTrue(facade.contains("lifecycleService.createContract(companyId, request)"));
        assertTrue(facade.contains("batchBillingProcessor.processBatchBilling(companyId, referenceDate)"));
        assertFalse(facade.contains("new RecurringBillingLog"));
        assertTrue(lifecycle.contains("existsByCompanyIdAndContractNumber"));
        assertTrue(processor.contains("new RecurringBillingLog"));
        assertTrue(Files.readAllLines(DIR.resolve("RecurringContractService.java")).size() <= 75);
        assertTrue(Files.readAllLines(DIR.resolve("RecurringContractLifecycleService.java")).size() <= 130);
        assertTrue(Files.readAllLines(DIR.resolve("RecurringBatchBillingProcessor.java")).size() <= 95);
    }
}
