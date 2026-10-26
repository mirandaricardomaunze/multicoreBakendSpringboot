package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientesPanelDecompositionHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("CLI-01: Tetos de linhas de código estritamente respeitados na decomposição")
    void cli01_lineCeilingsRespected() throws Exception {
        Path panelPath = GUI_ROOT.resolve("ClientesPanel.java");
        Path formDialogPath = GUI_ROOT.resolve("commercial/ClientFormDialog.java");
        Path coordinatorPath = GUI_ROOT.resolve("commercial/ClientesOperationsCoordinator.java");

        List<String> panelLines = Files.readAllLines(panelPath);
        List<String> formLines = Files.readAllLines(formDialogPath);
        List<String> coordLines = Files.readAllLines(coordinatorPath);

        assertThat(panelLines.size())
                .as("ClientesPanel.java deve possuir <= 245 linhas após decomposição")
                .isLessThanOrEqualTo(245);

        assertThat(formLines.size())
                .as("ClientFormDialog.java deve possuir <= 100 linhas")
                .isLessThanOrEqualTo(100);

        assertThat(coordLines.size())
                .as("ClientesOperationsCoordinator.java deve possuir <= 100 linhas")
                .isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("CLI-02: Eliminação completa de chamadas a JOptionPane em ClientesPanel e colaboradores")
    void cli02_eliminationOfJOptionPane() throws Exception {
        String panelSource = Files.readString(GUI_ROOT.resolve("ClientesPanel.java"));
        String formSource = Files.readString(GUI_ROOT.resolve("commercial/ClientFormDialog.java"));
        String coordSource = Files.readString(GUI_ROOT.resolve("commercial/ClientesOperationsCoordinator.java"));

        assertThat(panelSource).doesNotContain("JOptionPane.");
        assertThat(formSource).doesNotContain("JOptionPane.");
        assertThat(coordSource).doesNotContain("JOptionPane.");
    }

    @Test
    @DisplayName("CLI-03: Preservação de contratos canónicos de layout e exportação")
    void cli03_canonicalLayoutAndExportPreserved() throws Exception {
        String source = Files.readString(GUI_ROOT.resolve("ClientesPanel.java"));

        assertThat(source).contains("UIHelper.tableCardTop(\"Gestão de Clientes\"");
        assertThat(source).contains("ClientTablePagination.install(table)");
        assertThat(source).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(source).contains("TableQuickPeekController.install(table");
        assertThat(source).contains("TableFilter.bar(");
        assertThat(source).contains("Exportar PDF");
        assertThat(source).contains("fas-file-pdf");
        assertThat(source).contains("TableExportAction.export(this, printApiClient, table, \"Clientes\", \"clientes\")");
    }

    @Test
    @DisplayName("CLI-04: Preservação de abas especializadas e construtores retrocompatíveis")
    void cli04_specializedTabsAndConstructors() throws Exception {
        String source = Files.readString(GUI_ROOT.resolve("ClientesPanel.java"));

        assertThat(source).contains("selectCreditRiskTab()");
        assertThat(source).contains("selectStatementTab()");
        assertThat(source).contains("CreditRiskApiClient creditRiskApiClient");
        assertThat(source).contains("AccountStatementApiClient statementApiClient");
    }
}
