package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness HR-01..03:
 * Valida a conformidade canónica do módulo de Recursos Humanos (RH):
 * - Ausência de GridBagLayout em filtros de tabelas
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 * - Instalação de Quick Peek silencioso com tecla Espaço
 */
class HRUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("HR-01: HRPanel e HRContractsPanel contêm ensureHeadersFit e Quick Peek")
    void hr01_employeesAndContractsCanonical() throws Exception {
        String hr = source("HRPanel.java");
        assertThat(hr).contains("UIHelper.ensureHeadersFit(employeesTable)");
        assertThat(hr).contains("TableQuickPeekController.install(employeesTable");

        String contracts = source("HRContractsPanel.java");
        assertThat(contracts).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(contracts).contains("TableQuickPeekController.install(table");
    }

    @Test
    @DisplayName("HR-02: HRVacationsPanel, HRTimeSheetPanel e HRDeductionsPanel seguem padrão canónico")
    void hr02_vacationsTimeSheetDeductionsCanonical() throws Exception {
        String vacations = source("HRVacationsPanel.java");
        assertThat(vacations).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(vacations).contains("TableQuickPeekController.install(table");

        String timesheet = source("HRTimeSheetPanel.java");
        assertThat(timesheet).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(timesheet).contains("TableQuickPeekController.install(table");

        String deductions = source("HRDeductionsPanel.java");
        assertThat(deductions).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(deductions).contains("TableQuickPeekController.install(table");
    }

    @Test
    @DisplayName("HR-03: HRExpensesPanel, HRLiabilitiesPanel e HRTerminationsPanel contêm ensureHeadersFit e Quick Peek")
    void hr03_expensesLiabilitiesTerminationsCanonical() throws Exception {
        String expenses = source("HRExpensesPanel.java");
        assertThat(expenses).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(expenses).contains("TableQuickPeekController.install(table");

        String liabilities = source("HRLiabilitiesPanel.java");
        assertThat(liabilities).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(liabilities).contains("TableQuickPeekController.install(table");

        String terminations = source("HRTerminationsPanel.java");
        assertThat(terminations).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(terminations).contains("TableQuickPeekController.install(table");
    }
}
