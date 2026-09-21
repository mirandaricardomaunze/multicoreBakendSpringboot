package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness estrutural de docs/RH_SUBSIDIOS_UI_HARNESS.md (SUH-01..SUH-08). */
class HRSubsidiesUiHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("desktop")) ? Path.of("") : Path.of("..");

    @Test
    void client_exposesAllTypedBonusEndpoints() throws Exception {
        String source = source("desktop/src/main/java/mz/multicore/erp/desktop/client/HRApiClient.java");
        assertThat(source).contains(
                "getThirteenthMonth(int year)",
                "payThirteenthMonth(int year)",
                "getVacationAllowance(Long vacationId)",
                "payVacationAllowance(Long vacationId)",
                "/api/hr/payroll/thirteenth-month/",
                "/api/hr/payroll/vacation-allowance/");
    }

    @Test
    void bonusActions_useBackendDtosAsyncCallsAndExplicitConfirmation() throws Exception {
        String source = source("desktop/src/main/java/mz/multicore/erp/gui/HRBonusActions.java");
        assertThat(source).contains(
                "ThirteenthMonthDTO",
                "VacationAllowanceDTO",
                "UIHelper.loadAsync",
                "UIHelper.runWithProgress",
                "ModernMessageDialog.confirm");
        assertThat(source).doesNotContain("MONTHLY_DIVISOR", "monthsWorkedInYear", "dailyRate =");
    }

    @Test
    void actions_areDiscoverableWithoutAddingAnotherHrTab() throws Exception {
        String hr = source("desktop/src/main/java/mz/multicore/erp/gui/HRPanel.java");
        String vacations = source("desktop/src/main/java/mz/multicore/erp/gui/HRVacationsPanel.java");
        assertThat(hr).contains("\"13.º Mês\"", "bonusActions::openThirteenthMonth");
        assertThat(vacations).contains("\"Subsídio\"", "openVacationAllowance()");
        assertThat(hr).doesNotContain("addTab(\"Subsídios\"");
    }

    private static String source(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}
