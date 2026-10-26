package mz.multicore.erp.desktop.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de HRApiClient (Fase 94).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegação canónica para os 5 clientes especializados.
 */
class HRApiClientDecompositionHarnessTest {

    private static String readSource(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + relativePath, e);
        }
    }

    private static long countLines(String relativePath) {
        try {
            return Files.lines(Path.of(relativePath)).count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + relativePath, e);
        }
    }

    @Test
    @DisplayName("HRACD-01: Limites físicos estritos de linhas de código")
    void hracd01_limitesFisicosLinhasCodigo() {
        long facadeLines = countLines("src/main/java/mz/multicore/erp/desktop/client/HRApiClient.java");
        long employeeLines = countLines("src/main/java/mz/multicore/erp/desktop/client/HREmployeeApiClient.java");
        long contractLines = countLines("src/main/java/mz/multicore/erp/desktop/client/HRContractApiClient.java");
        long timesheetLines = countLines("src/main/java/mz/multicore/erp/desktop/client/HRTimeSheetApiClient.java");
        long payrollLines = countLines("src/main/java/mz/multicore/erp/desktop/client/HRPayrollApiClient.java");
        long expensesLines = countLines("src/main/java/mz/multicore/erp/desktop/client/HRExpensesAndBenefitsApiClient.java");

        assertThat(facadeLines)
                .as("HRApiClient.java deve ter <= 185 linhas (reduzido de 473 linhas)")
                .isLessThanOrEqualTo(185);

        assertThat(employeeLines)
                .as("HREmployeeApiClient.java deve ter <= 110 linhas")
                .isLessThanOrEqualTo(110);

        assertThat(contractLines)
                .as("HRContractApiClient.java deve ter <= 65 linhas")
                .isLessThanOrEqualTo(65);

        assertThat(timesheetLines)
                .as("HRTimeSheetApiClient.java deve ter <= 100 linhas")
                .isLessThanOrEqualTo(100);

        assertThat(payrollLines)
                .as("HRPayrollApiClient.java deve ter <= 130 linhas")
                .isLessThanOrEqualTo(130);

        assertThat(expensesLines)
                .as("HRExpensesAndBenefitsApiClient.java deve ter <= 105 linhas")
                .isLessThanOrEqualTo(105);
    }

    @Test
    @DisplayName("HRACD-02: Zero diálogos legados JOptionPane")
    void hracd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/desktop/client/HRApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/HREmployeeApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/HRContractApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/HRTimeSheetApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/HRPayrollApiClient.java",
                "src/main/java/mz/multicore/erp/desktop/client/HRExpensesAndBenefitsApiClient.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("HRACD-03: Delegação canónica para clientes especializados")
    void hracd03_delegacaoCanonicaParaClientesEspecializados() {
        String facade = readSource("src/main/java/mz/multicore/erp/desktop/client/HRApiClient.java");

        assertThat(facade)
                .contains("employeeApiClient.getAllEmployees()")
                .contains("contractApiClient.getAllContracts()")
                .contains("timeSheetApiClient.getMonthlySheet(")
                .contains("payrollApiClient.processMonthlyPayroll(")
                .contains("expensesAndBenefitsApiClient.getAllExpenses()");
    }

    @Test
    @DisplayName("HRACD-04: Contratos e métodos públicos preservados")
    void hracd04_contratosPreservados() {
        String facade = readSource("src/main/java/mz/multicore/erp/desktop/client/HRApiClient.java");

        assertThat(facade)
                .contains("getAllEmployees()")
                .contains("getAllContracts()")
                .contains("getMonthlySheet(")
                .contains("processMonthlyPayroll(")
                .contains("getThirteenthMonth(")
                .contains("payThirteenthMonth(")
                .contains("getVacationAllowance(")
                .contains("payVacationAllowance(")
                .contains("getPayrollLiabilityAlerts()")
                .contains("getContractAlerts()");
    }
}
