package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceivablesServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/ReceivablesService.java");
    private static final Path VALIDATOR_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/ClientCreditLimitValidator.java");
    private static final Path CALCULATOR_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/ReceivablesAgingCalculator.java");

    @Test
    void receivablesServiceDelegatesToValidatorAndAgingCalculator() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String validatorSource = Files.readString(VALIDATOR_FILE);
        String calculatorSource = Files.readString(CALCULATOR_FILE);

        assertTrue(serviceSource.contains("creditLimitValidator.assertCreditAvailable("));
        assertTrue(serviceSource.contains("agingCalculator.getAging("));
        assertFalse(serviceSource.contains("new AgingSummaryDTO("));

        assertTrue(validatorSource.contains("Limite de crédito excedido para"));
        assertTrue(calculatorSource.contains("new ClientAgingDTO("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int validatorLines = Files.readAllLines(VALIDATOR_FILE).size();
        int calculatorLines = Files.readAllLines(CALCULATOR_FILE).size();

        assertTrue(serviceLines <= 65, "ReceivablesService deve ter <= 65 linhas, mas tem " + serviceLines);
        assertTrue(validatorLines <= 65, "ClientCreditLimitValidator deve ter <= 65 linhas, mas tem " + validatorLines);
        assertTrue(calculatorLines <= 125, "ReceivablesAgingCalculator deve ter <= 125 linhas, mas tem " + calculatorLines);
    }
}
