package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.architecture.SourceLines;
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
        String biller = Files.readString(DIR.resolve("RecurringPeriodBiller.java"));
        String invoicer = Files.readString(DIR.resolve("RecurringContractInvoicer.java"));

        assertTrue(facade.contains("lifecycleService.createContract(companyId, request)"));
        assertTrue(facade.contains("batchBillingProcessor.processBatchBilling(companyId, referenceDate)"));
        assertFalse(facade.contains("new RecurringBillingLog"));
        assertTrue(lifecycle.contains("existsByCompanyIdAndContractNumber"));
        // One transaction per billed period, so a failure never undoes the periods already invoiced.
        assertTrue(processor.contains("transactions.execute"));
        assertFalse(processor.contains("new RecurringBillingLog"));
        assertTrue(biller.contains("new RecurringBillingLog"));
        assertTrue(biller.contains("lockForBilling"));
        // Billing issues a real commercial invoice instead of only logging a reference number.
        assertTrue(invoicer.contains("comercialService.createInvoice"));
        assertTrue(SourceLines.code(DIR.resolve("RecurringContractService.java")) <= 75);
        assertTrue(SourceLines.code(DIR.resolve("RecurringContractLifecycleService.java")) <= 130);
        assertTrue(SourceLines.code(DIR.resolve("RecurringBatchBillingProcessor.java")) <= 95);
        assertTrue(SourceLines.code(DIR.resolve("RecurringPeriodBiller.java")) <= 95);
        assertTrue(SourceLines.code(DIR.resolve("RecurringContractInvoicer.java")) <= 95);
    }
}
