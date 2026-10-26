package mz.multicore.erp.gui.commercial;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommercialMovementsPanelDecompositionHarnessTest {

    private static final Path COMM_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "commercial");

    @Test
    @DisplayName("MOV-01: Tetos de linhas de código estritamente respeitados na decomposição")
    void mov01_lineCeilingsRespected() throws Exception {
        Path panelPath = COMM_ROOT.resolve("CommercialMovementsPanel.java");
        Path dataCoordPath = COMM_ROOT.resolve("CommercialMovementsDataCoordinator.java");
        Path opsCoordPath = COMM_ROOT.resolve("CommercialMovementsOperationsCoordinator.java");

        List<String> panelLines = Files.readAllLines(panelPath);
        List<String> dataLines = Files.readAllLines(dataCoordPath);
        List<String> opsLines = Files.readAllLines(opsCoordPath);

        assertThat(panelLines.size())
                .as("CommercialMovementsPanel.java deve possuir <= 225 linhas")
                .isLessThanOrEqualTo(225);

        assertThat(dataLines.size())
                .as("CommercialMovementsDataCoordinator.java deve possuir <= 200 linhas")
                .isLessThanOrEqualTo(200);

        assertThat(opsLines.size())
                .as("CommercialMovementsOperationsCoordinator.java deve possuir <= 90 linhas")
                .isLessThanOrEqualTo(90);
    }

    @Test
    @DisplayName("MOV-02: Preservação de contratos canónicos de layout e exportação")
    void mov02_canonicalLayoutAndExportPreserved() throws Exception {
        String source = Files.readString(COMM_ROOT.resolve("CommercialMovementsPanel.java"));

        assertThat(source).contains("UIHelper.tableCardTop(");
        assertThat(source).contains("TableFilter.bar(");
        assertThat(source).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(source).contains("TableQuickPeekController.install(table");
        assertThat(source).contains("ClientTablePagination.install(table)");
        assertThat(source).doesNotContain("new JPanel(new GridBagLayout())");
    }

    @Test
    @DisplayName("MOV-03: Preservação de construtores sobrecarregados retrocompatíveis")
    void mov03_overloadedConstructorsPreserved() throws Exception {
        String source = Files.readString(COMM_ROOT.resolve("CommercialMovementsPanel.java"));

        assertThat(source).contains("public CommercialMovementsPanel(MovimentosApiClient apiClient)");
        assertThat(source).contains("public CommercialMovementsPanel(MovimentosApiClient apiClient, PrintApiClient printApiClient)");
        assertThat(source).contains("public CommercialMovementsPanel(MovimentosApiClient apiClient, PrintApiClient printApiClient, InventoryApiClient inventoryApiClient)");
    }
}
