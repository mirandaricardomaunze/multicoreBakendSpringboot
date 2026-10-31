package mz.multicore.erp.modules.pos.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobilePaymentServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/pos/service/MobilePaymentService.java");
    private static final Path PROCESSOR_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/pos/service/MobilePaymentTransactionProcessor.java");
    private static final Path VALIDATOR_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/pos/service/MobilePaymentPhoneValidator.java");

    @Test
    void mobilePaymentServiceDelegatesToProcessorAndValidator() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String processorSource = Files.readString(PROCESSOR_FILE);
        String validatorSource = Files.readString(VALIDATOR_FILE);

        assertTrue(serviceSource.contains("transactionProcessor.initiatePayment("));
        assertTrue(serviceSource.contains("phoneValidator.normalizePhoneNumber("));
        assertFalse(serviceSource.contains("new MobilePaymentTransaction()"));

        assertTrue(processorSource.contains("Solicitação USSD enviada ao cliente"));
        assertTrue(validatorSource.contains("Número incompatível com M-Pesa"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int processorLines = Files.readAllLines(PROCESSOR_FILE).size();
        int validatorLines = Files.readAllLines(VALIDATOR_FILE).size();

        assertTrue(serviceLines <= 80, "MobilePaymentService deve ter <= 80 linhas, mas tem " + serviceLines);
        assertTrue(processorLines <= 135, "MobilePaymentTransactionProcessor deve ter <= 135 linhas, mas tem " + processorLines);
        assertTrue(validatorLines <= 75, "MobilePaymentPhoneValidator deve ter <= 75 linhas, mas tem " + validatorLines);
    }
}
