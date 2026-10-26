package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de StockTransferService (Fase 95).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegação canónica para os colaboradores.
 */
class StockTransferServiceDecompositionHarnessTest {

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
    @DisplayName("STSD-01: Limites físicos estritos de linhas de código")
    void stsd01_limitesFisicosLinhasCodigo() {
        long serviceLines = countLines("src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferService.java");
        long executorLines = countLines("src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferMovementExecutor.java");
        long validatorLines = countLines("src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferValidator.java");
        long coordinatorLines = countLines("src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferWorkflowCoordinator.java");

        assertThat(serviceLines)
                .as("StockTransferService.java deve ter <= 425 linhas (reduzido de 712 linhas)")
                .isLessThanOrEqualTo(425);

        assertThat(executorLines)
                .as("StockTransferMovementExecutor.java deve ter <= 125 linhas")
                .isLessThanOrEqualTo(125);

        assertThat(validatorLines)
                .as("StockTransferValidator.java deve ter <= 105 linhas")
                .isLessThanOrEqualTo(105);

        assertThat(coordinatorLines)
                .as("StockTransferWorkflowCoordinator.java deve ter <= 105 linhas")
                .isLessThanOrEqualTo(105);
    }

    @Test
    @DisplayName("STSD-02: Zero diálogos legados JOptionPane")
    void stsd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferService.java",
                "src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferMovementExecutor.java",
                "src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferValidator.java",
                "src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferWorkflowCoordinator.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("STSD-03: Delegação canónica para colaboradores especializados")
    void stsd03_delegacaoCanonicaParaColaboradores() {
        String service = readSource("src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferService.java");

        assertThat(service)
                .contains("validator.validateAvailability(")
                .contains("movementExecutor.dispatchOrMove(")
                .contains("workflowCoordinator.ensureApprovalRequest(")
                .contains("workflowCoordinator.announce(");
    }

    @Test
    @DisplayName("STSD-04: Contratos e concorrência preservados")
    void stsd04_contratosPreservados() {
        String service = readSource("src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferService.java");

        assertThat(service)
                .contains("Stock stock = stockRepository.findForReservation")
                .contains("public StockTransferDTO create(")
                .contains("public StockTransferDTO update(")
                .contains("public StockTransferDTO approve(")
                .contains("public StockTransferDTO reject(")
                .contains("public StockTransferDTO cancel(")
                .contains("public StockTransferDTO linkToOrder(")
                .contains("public StockTransfer loadForPrint(")
                .contains("public StockTransfer loadForPickingPrint(");
    }
}
