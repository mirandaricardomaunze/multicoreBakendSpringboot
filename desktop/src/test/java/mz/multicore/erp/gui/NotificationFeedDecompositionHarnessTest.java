package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de NotificationFeed (Fase 78).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegações canónicas.
 */
class NotificationFeedDecompositionHarnessTest {

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
    @DisplayName("NFD-01: Limites físicos estritos de linhas de código")
    void nfd01_limitesFisicosLinhasCodigo() {
        long feedLines = countLines("src/main/java/mz/multicore/erp/gui/NotificationFeed.java");
        long invLines = countLines("src/main/java/mz/multicore/erp/gui/NotificationInventoryAlertProvider.java");
        long hrLines = countLines("src/main/java/mz/multicore/erp/gui/NotificationHRAlertProvider.java");

        assertThat(feedLines)
                .as("NotificationFeed.java deve ter <= 200 linhas (reduzido de 537)")
                .isLessThanOrEqualTo(200);

        assertThat(invLines)
                .as("NotificationInventoryAlertProvider.java deve ter <= 150 linhas")
                .isLessThanOrEqualTo(150);

        assertThat(hrLines)
                .as("NotificationHRAlertProvider.java deve ter <= 175 linhas")
                .isLessThanOrEqualTo(175);
    }

    @Test
    @DisplayName("NFD-02: Zero diálogos legados JOptionPane")
    void nfd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/NotificationFeed.java",
                "src/main/java/mz/multicore/erp/gui/NotificationInventoryAlertProvider.java",
                "src/main/java/mz/multicore/erp/gui/NotificationHRAlertProvider.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("NFD-03: Preservação de construtores e record público canónico")
    void nfd03_contratosPreservados() {
        String feed = readSource("src/main/java/mz/multicore/erp/gui/NotificationFeed.java");

        assertThat(feed)
                .contains("public record NotificationItem(")
                .contains("public List<NotificationItem> load(Long companyId)")
                .contains("public NotificationFeed(ApprovalApiClient approvalApiClient, InventoryApiClient inventoryApiClient,")
                .contains("QualityApiClient qualityApiClient");
    }

    @Test
    @DisplayName("NFD-04: Delegação canónica aos provedores desacoplados")
    void nfd04_delegacaoCanonica() {
        String feed = readSource("src/main/java/mz/multicore/erp/gui/NotificationFeed.java");

        assertThat(feed)
                .contains("NotificationInventoryAlertProvider.addStockWasteAlerts(")
                .contains("NotificationInventoryAlertProvider.addLowStock(")
                .contains("NotificationInventoryAlertProvider.addExpiries(")
                .contains("NotificationInventoryAlertProvider.addInTransitOverdue(")
                .contains("NotificationHRAlertProvider.addContracts(")
                .contains("NotificationHRAlertProvider.addPayrollLiabilities(")
                .contains("NotificationHRAlertProvider.addEmployeeDocuments(")
                .contains("NotificationHRAlertProvider.addOccupationalHealth(")
                .contains("NotificationHRAlertProvider.addQualityAlerts(");
    }
}
