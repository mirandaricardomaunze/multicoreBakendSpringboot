package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.monitoring.SystemMonitoringExporter;
import mz.multicore.erp.gui.monitoring.SystemMonitoringOverviewBuilder;
import mz.multicore.erp.gui.monitoring.SystemMonitoringTabsBuilder;
import mz.multicore.erp.modules.monitoring.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Harness de validação física e funcional da decomposição do SystemMonitoringDialog (Fase 88).
 */
class SystemMonitoringDialogDecompositionHarnessTest {

    @Test
    @DisplayName("Validar limites físicos estritos de linhas de código das classes decompostas")
    void validatePhysicalLineLimits() throws IOException {
        Path basePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

        long dialogLines = Files.lines(basePath.resolve("SystemMonitoringDialog.java")).count();
        long overviewLines = Files.lines(basePath.resolve("monitoring").resolve("SystemMonitoringOverviewBuilder.java")).count();
        long tabsLines = Files.lines(basePath.resolve("monitoring").resolve("SystemMonitoringTabsBuilder.java")).count();
        long exporterLines = Files.lines(basePath.resolve("monitoring").resolve("SystemMonitoringExporter.java")).count();

        assertThat(dialogLines).as("SystemMonitoringDialog deve ter <= 210 linhas").isLessThanOrEqualTo(210);
        assertThat(overviewLines).as("SystemMonitoringOverviewBuilder deve ter <= 215 linhas").isLessThanOrEqualTo(215);
        assertThat(tabsLines).as("SystemMonitoringTabsBuilder deve ter <= 220 linhas").isLessThanOrEqualTo(220);
        assertThat(exporterLines).as("SystemMonitoringExporter deve ter <= 120 linhas").isLessThanOrEqualTo(120);
    }

    @Test
    @DisplayName("Validar ausência de JOptionPane e de emojis em todo o pacote de monitoramento")
    void validateNoJOptionPaneAndNoEmojis() throws IOException {
        Path basePath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        List<Path> files = List.of(
                basePath.resolve("SystemMonitoringDialog.java"),
                basePath.resolve("monitoring").resolve("SystemMonitoringOverviewBuilder.java"),
                basePath.resolve("monitoring").resolve("SystemMonitoringTabsBuilder.java"),
                basePath.resolve("monitoring").resolve("SystemMonitoringExporter.java")
        );

        for (Path file : files) {
            String content = Files.readString(file);
            assertThat(content)
                    .as("Arquivo %s não deve conter JOptionPane", file.getFileName())
                    .doesNotContain("JOptionPane");
            assertThat(content)
                    .as("Arquivo %s não deve conter emojis Unicode", file.getFileName())
                    .doesNotContain("⭐", "✅", "❌", "⚠️", "🔥", "🚀");
        }
    }

    @Test
    @DisplayName("Validar instanciação e estrutura do SystemMonitoringDialog")
    void validateDialogInstantiationAndStructure() {
        SystemMonitoringApiClient mockClient = mock(SystemMonitoringApiClient.class);
        when(mockClient.isSuperAdmin()).thenReturn(true);

        SystemMonitoringDialog monitoringDialog = new SystemMonitoringDialog(null, mockClient);
        JDialog dialog = monitoringDialog.getDialog();

        assertThat(dialog).isNotNull();
        assertThat(dialog.getTitle()).isEqualTo("Observabilidade & Diagnóstico do Sistema");
        assertThat(dialog.getContentPane()).isNotNull();

        // Verificar presença do JTabbedPane com as abas canónicas
        Component[] comps = ((JPanel) dialog.getContentPane()).getComponents();
        JTabbedPane tabbedPane = null;
        for (Component comp : comps) {
            if (comp instanceof JTabbedPane tp) {
                tabbedPane = tp;
                break;
            }
        }

        assertThat(tabbedPane).isNotNull();
        assertThat(tabbedPane.getTabCount()).isEqualTo(3);
        assertThat(tabbedPane.getTitleAt(0)).isEqualTo("Diagnóstico Geral");
        assertThat(tabbedPane.getTitleAt(1)).isEqualTo("Saúde Multi-Tenant");
        assertThat(tabbedPane.getTitleAt(2)).isEqualTo("Central de Alarmes");
    }

    @Test
    @DisplayName("Validar aplicação de dados de saúde no overview builder")
    void validateOverviewHealthDataApplication() {
        SystemMonitoringOverviewBuilder builder = new SystemMonitoringOverviewBuilder();
        JPanel header = builder.buildHeader();
        JPanel metrics = builder.buildMetricsGrid();

        assertThat(header).isNotNull();
        assertThat(metrics).isNotNull();

        MemoryHealthDTO mem = new MemoryHealthDTO(
                100L * 1024 * 1024,
                500L * 1024 * 1024,
                20.0,
                50L * 1024 * 1024
        );
        StorageHealthDTO storage = new StorageHealthDTO(
                100L * 1024 * 1024 * 1024,
                50L * 1024 * 1024 * 1024,
                50L * 1024 * 1024 * 1024,
                50.0,
                "C:/"
        );
        DatabaseHealthDTO db = new DatabaseHealthDTO("ONLINE", 15L, 5, 10, 50, "PostgreSQL");
        ThreadPoolHealthDTO threads = new ThreadPoolHealthDTO(25, 40, 100L, 10);
        List<SubsystemStatusDTO> subs = List.of(
                new SubsystemStatusDTO("Autenticação JWT", "HEALTHY", 10L, "Tokens activos")
        );

        SystemHealthDTO health = new SystemHealthDTO(
                "HEALTHY",
                360000L,
                "2.1.0",
                "21.0.2",
                "Windows 11",
                db,
                storage,
                mem,
                threads,
                subs,
                Instant.now()
        );

        builder.applyHealthData(health);
        builder.setUpdatingState();
        builder.setOfflineState();
        builder.setErrorState();
    }
}
