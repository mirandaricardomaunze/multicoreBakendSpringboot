package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de CustomerOrderFulfillmentService (Fase 120).
 */
class CustomerOrderFulfillmentServiceDecompositionHarnessTest {

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
    @DisplayName("COFSD-01: Limites físicos estritos de linhas de código")
    void cofsd01_limitesFisicosLinhasCodigo() {
        long serviceLines = countLines("src/main/java/mz/multicore/erp/modules/comercial/service/CustomerOrderFulfillmentService.java");
        long pickingLines = countLines("src/main/java/mz/multicore/erp/modules/comercial/service/OrderPickingCoordinator.java");
        long lifecycleLines = countLines("src/main/java/mz/multicore/erp/modules/comercial/service/OrderFulfillmentLifecycleService.java");

        assertThat(serviceLines)
                .as("CustomerOrderFulfillmentService.java deve ter <= 160 linhas")
                .isLessThanOrEqualTo(160);

        assertThat(pickingLines)
                .as("OrderPickingCoordinator.java deve ter <= 195 linhas")
                .isLessThanOrEqualTo(195);

        assertThat(lifecycleLines)
                .as("OrderFulfillmentLifecycleService.java deve ter <= 135 linhas")
                .isLessThanOrEqualTo(135);
    }

    @Test
    @DisplayName("COFSD-02: Zero diálogos legados JOptionPane")
    void cofsd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/modules/comercial/service/CustomerOrderFulfillmentService.java",
                "src/main/java/mz/multicore/erp/modules/comercial/service/OrderPickingCoordinator.java",
                "src/main/java/mz/multicore/erp/modules/comercial/service/OrderFulfillmentLifecycleService.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("COFSD-03: Delegação canónica para colaboradores especializados")
    void cofsd03_delegacaoCanonicaParaColaboradores() {
        String service = readSource("src/main/java/mz/multicore/erp/modules/comercial/service/CustomerOrderFulfillmentService.java");

        assertThat(service)
                .contains("pickingCoordinator.printForPicking(")
                .contains("pickingCoordinator.reprint(")
                .contains("pickingCoordinator.getPickingDetails(")
                .contains("pickingCoordinator.printForPickingA4(")
                .contains("lifecycleService.completeSeparationWithConfirmation(")
                .contains("lifecycleService.completeSeparation(")
                .contains("lifecycleService.bill(");
    }

    @Test
    @DisplayName("COFSD-04: Contratos e métodos públicos preservados")
    void cofsd04_contratosPreservados() {
        String service = readSource("src/main/java/mz/multicore/erp/modules/comercial/service/CustomerOrderFulfillmentService.java");

        assertThat(service)
                .contains("public OrderDTO submit(")
                .contains("public byte[] printForPicking(")
                .contains("public byte[] reprint(")
                .contains("public OrderPickingDetailsDTO getPickingDetails(")
                .contains("public byte[] printForPickingA4(")
                .contains("public OrderDTO completeSeparationWithConfirmation(")
                .contains("public OrderDTO completeSeparation(")
                .contains("public InvoiceDTO bill(")
                .contains("public List<OrderEventDTO> events(");
    }
}
