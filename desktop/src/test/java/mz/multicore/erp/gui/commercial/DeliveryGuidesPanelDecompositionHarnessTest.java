package mz.multicore.erp.gui.commercial;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryGuidesPanelDecompositionHarnessTest {

    private static final Path COMM_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "commercial");

    @Test
    @DisplayName("DG-01: Tetos de linhas de código estritamente respeitados na decomposição")
    void dg01_lineCeilingsRespected() throws Exception {
        Path panelPath = COMM_ROOT.resolve("DeliveryGuidesPanel.java");
        Path coordinatorPath = COMM_ROOT.resolve("DeliveryGuidesOperationsCoordinator.java");

        List<String> panelLines = Files.readAllLines(panelPath);
        List<String> coordLines = Files.readAllLines(coordinatorPath);

        assertThat(panelLines.size())
                .as("DeliveryGuidesPanel.java deve possuir <= 250 linhas após decomposição")
                .isLessThanOrEqualTo(250);

        assertThat(coordLines.size())
                .as("DeliveryGuidesOperationsCoordinator.java deve possuir <= 175 linhas")
                .isLessThanOrEqualTo(175);
    }

    @Test
    @DisplayName("DG-02: Eliminação completa de chamadas a JOptionPane em DeliveryGuidesPanel e coordenador")
    void dg02_eliminationOfJOptionPane() throws Exception {
        String panelSource = Files.readString(COMM_ROOT.resolve("DeliveryGuidesPanel.java"));
        String coordSource = Files.readString(COMM_ROOT.resolve("DeliveryGuidesOperationsCoordinator.java"));

        assertThat(panelSource).doesNotContain("JOptionPane.");
        assertThat(coordSource).doesNotContain("JOptionPane.");
    }

    @Test
    @DisplayName("DG-03: Preservação de menus semânticos, filtros e contratos de layout")
    void dg03_canonicalLayoutAndMenusPreserved() throws Exception {
        String source = Files.readString(COMM_ROOT.resolve("DeliveryGuidesPanel.java"));

        assertThat(source).contains("ActionMenuButton");
        assertThat(source).contains("Aprovar");
        assertThat(source).contains("Rejeitar");
        assertThat(source).contains("Cancelar");
        assertThat(source).contains("TableFilter.bar(");
        assertThat(source).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(source).contains("TableQuickPeekController.install(table");
        assertThat(source).contains("PeriodFilter");
        assertThat(source).contains("periodCombo");
        assertThat(source).contains("createRefreshButton(this::refresh)");
        assertThat(source).contains("UIHelper.tableCardTop(");
    }

    @Test
    @DisplayName("DG-04: Preservação de todos os construtores sobrecarregados")
    void dg04_overloadedConstructorsPreserved() throws Exception {
        String source = Files.readString(COMM_ROOT.resolve("DeliveryGuidesPanel.java"));

        assertThat(source).contains("public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh)");
        assertThat(source).contains("public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh,");
        assertThat(source).contains("PrintApiClient printApiClient");
    }
}
