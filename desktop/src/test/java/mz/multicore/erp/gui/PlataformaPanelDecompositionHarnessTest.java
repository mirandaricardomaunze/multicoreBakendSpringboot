package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.platform.PlatformClientVersionsView;
import mz.multicore.erp.gui.platform.PlatformCompaniesView;
import mz.multicore.erp.gui.platform.PlatformSubscriptionsView;
import mz.multicore.erp.gui.platform.PlatformSupportTicketsView;
import mz.multicore.erp.gui.platform.PlatformSystemMonitoringView;
import mz.multicore.erp.gui.platform.PlatformUsersView;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PlataformaPanelDecompositionHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
    private static final Path PLATFORM_ROOT = GUI_ROOT.resolve("platform");

    @Test
    @DisplayName("DECOMP-01: Limite de Linhas do Orquestrador PlataformaPanel (<= 130 linhas)")
    void orchestratorLineLimit() throws IOException {
        Path path = GUI_ROOT.resolve("PlataformaPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertThat(lines.size())
                .as("PlataformaPanel.java deve ser um orquestrador conciso com <= 130 linhas")
                .isLessThanOrEqualTo(130);
    }

    @Test
    @DisplayName("DECOMP-02: Limite de Linhas das Sub-Views (<= 300 linhas cada)")
    void subViewsLineLimit() throws IOException {
        List<String> subViews = List.of(
                "PlatformCompaniesView.java",
                "PlatformSubscriptionsView.java",
                "PlatformUsersView.java",
                "PlatformSupportTicketsView.java",
                "PlatformClientVersionsView.java",
                "PlatformSystemMonitoringView.java"
        );

        for (String file : subViews) {
            Path path = PLATFORM_ROOT.resolve(file);
            assertThat(Files.exists(path))
                    .as("%s deve existir em mz.multicore.erp.gui.platform", file)
                    .isTrue();

            List<String> lines = Files.readAllLines(path);
            assertThat(lines.size())
                    .as("%s deve possuir <= 300 linhas", file)
                    .isLessThanOrEqualTo(300);
        }
    }

    @Test
    @DisplayName("DECOMP-03: Contenção de Tabelas em ModernPanel")
    void tableContainmentInModernPanel() throws IOException {
        List<String> tableViews = List.of(
                "PlatformCompaniesView.java",
                "PlatformSubscriptionsView.java",
                "PlatformUsersView.java",
                "PlatformSupportTicketsView.java",
                "PlatformClientVersionsView.java"
        );

        for (String file : tableViews) {
            String content = Files.readString(PLATFORM_ROOT.resolve(file));
            assertThat(content)
                    .as("%s deve conter JTable contido em ModernPanel", file)
                    .contains("new JTable(")
                    .contains("ModernPanel");
        }
    }

    @Test
    @DisplayName("DECOMP-04: Instanciação e Retrocompatibilidade de Métodos")
    void instantiationAndCompatibility() {
        PlatformApiClient platformApi = Mockito.mock(PlatformApiClient.class);
        SystemMonitoringApiClient monitoringApi = Mockito.mock(SystemMonitoringApiClient.class);

        PlataformaPanel panel = new PlataformaPanel(platformApi, monitoringApi);
        assertNotNull(panel.getCompaniesView());
        assertNotNull(panel.getSubscriptionsView());
        assertNotNull(panel.getUsersView());
        assertNotNull(panel.getSupportTicketsView());
        assertNotNull(panel.getClientVersionsView());

        // Validação do método estático de renderização de thread de suporte
        SupportMessageDTO msg = new SupportMessageDTO(1L, "Admin", true, "Teste", LocalDateTime.now());
        String thread = PlataformaPanel.renderThread(List.of(msg));
        assertThat(thread).contains("[Suporte · Admin]");
        assertThat(thread).contains("Teste");
    }
}
