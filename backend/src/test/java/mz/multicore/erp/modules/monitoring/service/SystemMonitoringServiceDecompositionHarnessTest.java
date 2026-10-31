package mz.multicore.erp.modules.monitoring.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de SystemMonitoringService (Fase 119).
 */
class SystemMonitoringServiceDecompositionHarnessTest {

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
    @DisplayName("SMSD-01: Limites físicos estritos de linhas de código")
    void smsd01_limitesFisicosLinhasCodigo() {
        long serviceLines = countLines("src/main/java/mz/multicore/erp/modules/monitoring/service/SystemMonitoringService.java");
        long collectorLines = countLines("src/main/java/mz/multicore/erp/modules/monitoring/service/SystemTelemetryCollector.java");
        long reportLines = countLines("src/main/java/mz/multicore/erp/modules/monitoring/service/SystemDiagnosticsReportBuilder.java");

        assertThat(serviceLines)
                .as("SystemMonitoringService.java deve ter <= 120 linhas")
                .isLessThanOrEqualTo(120);

        assertThat(collectorLines)
                .as("SystemTelemetryCollector.java deve ter <= 210 linhas")
                .isLessThanOrEqualTo(210);

        assertThat(reportLines)
                .as("SystemDiagnosticsReportBuilder.java deve ter <= 120 linhas")
                .isLessThanOrEqualTo(120);
    }

    @Test
    @DisplayName("SMSD-02: Zero diálogos legados JOptionPane")
    void smsd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/modules/monitoring/service/SystemMonitoringService.java",
                "src/main/java/mz/multicore/erp/modules/monitoring/service/SystemTelemetryCollector.java",
                "src/main/java/mz/multicore/erp/modules/monitoring/service/SystemDiagnosticsReportBuilder.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("SMSD-03: Delegação canónica para colaboradores especializados")
    void smsd03_delegacaoCanonicaParaColaboradores() {
        String service = readSource("src/main/java/mz/multicore/erp/modules/monitoring/service/SystemMonitoringService.java");

        assertThat(service)
                .contains("telemetryCollector.collectSystemHealth(")
                .contains("reportBuilder.buildExport(");
    }

    @Test
    @DisplayName("SMSD-04: Contratos e métodos públicos preservados")
    void smsd04_contratosPreservados() {
        String service = readSource("src/main/java/mz/multicore/erp/modules/monitoring/service/SystemMonitoringService.java");

        assertThat(service)
                .contains("public SystemHealthDTO getSystemHealth()")
                .contains("public SystemDiagnosticsExportDTO getDiagnosticsExport()")
                .contains("public SystemDiagnosticsExportDTO exportDiagnostics()");
    }
}
