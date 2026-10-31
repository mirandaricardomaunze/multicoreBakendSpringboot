package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollBonusServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/hr/service/PayrollBonusService.java");
    private static final Path CALCULATION_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/hr/service/PayrollBonusCalculationService.java");
    private static final Path PAYMENT_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/hr/service/PayrollBonusPaymentService.java");

    @Test
    void payrollBonusServiceDelegatesToCalculationAndPaymentCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String calculationSource = Files.readString(CALCULATION_FILE);
        String paymentSource = Files.readString(PAYMENT_FILE);

        assertTrue(serviceSource.contains("calculationService.thirteenthMonth("));
        assertTrue(serviceSource.contains("paymentService.payThirteenthMonth("));
        assertFalse(serviceSource.contains("new PayrollBonus()"));

        assertTrue(calculationSource.contains("monthsWorkedInYear("));
        assertTrue(paymentSource.contains("BONUS_13TH_PAY"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int calculationLines = Files.readAllLines(CALCULATION_FILE).size();
        int paymentLines = Files.readAllLines(PAYMENT_FILE).size();

        assertTrue(serviceLines <= 80, "PayrollBonusService deve ter <= 80 linhas, mas tem " + serviceLines);
        assertTrue(calculationLines <= 115, "PayrollBonusCalculationService deve ter <= 115 linhas, mas tem " + calculationLines);
        assertTrue(paymentLines <= 105, "PayrollBonusPaymentService deve ter <= 105 linhas, mas tem " + paymentLines);
    }
}
