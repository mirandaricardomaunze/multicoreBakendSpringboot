package mz.multicore.erp.modules.performance.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesGoalBonusServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/performance/service");

    @Test
    void bonusApprovalAndPayrollIntegrationStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("SalesGoalBonusService.java"));
        String approval = Files.readString(DIR.resolve("SalesGoalBonusApprovalService.java"));
        String payroll = Files.readString(DIR.resolve("SalesGoalBonusPayrollService.java"));

        assertTrue(facade.contains("approvalService.approveBonus(id, request)"));
        assertTrue(facade.contains("payrollService.integrateBonusWithPayroll(bonusId, year, month)"));
        assertFalse(facade.contains("BONUS_PAID_IN_PAYSLIP"));
        assertTrue(approval.contains("BONUS_APPROVED"));
        assertTrue(payroll.contains("BONUS_PAID_IN_PAYSLIP"));
        assertTrue(Files.readAllLines(DIR.resolve("SalesGoalBonusService.java")).size() <= 90);
        assertTrue(Files.readAllLines(DIR.resolve("SalesGoalBonusApprovalService.java")).size() <= 125);
        assertTrue(Files.readAllLines(DIR.resolve("SalesGoalBonusPayrollService.java")).size() <= 130);
    }
}
