package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseBusinessUnitsPanelDecompositionHarnessTest {

    private static final Path GUI_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    void bu01_lineCeilingsRespected() throws IOException {
        Path panel = GUI_DIR.resolve("PurchaseBusinessUnitsPanel.java");
        Path dialog = GUI_DIR.resolve("BusinessUnitFormDialog.java");
        Path coordinator = GUI_DIR.resolve("PurchaseBusinessUnitsOperationsCoordinator.java");

        assertThat(Files.lines(panel).count())
                .as("PurchaseBusinessUnitsPanel deve manter foco estrito de layout e tabela")
                .isLessThanOrEqualTo(180);

        assertThat(Files.lines(dialog).count())
                .as("BusinessUnitFormDialog deve gerenciar modal de linha de negócio")
                .isLessThanOrEqualTo(105);

        assertThat(Files.lines(coordinator).count())
                .as("PurchaseBusinessUnitsOperationsCoordinator gerencia operações e Quick Peek")
                .isLessThanOrEqualTo(65);
    }

    @Test
    void bu02_cleanArchitectureAndZeroJOptionPane() throws IOException {
        for (String file : new String[]{"PurchaseBusinessUnitsPanel.java", "BusinessUnitFormDialog.java", "PurchaseBusinessUnitsOperationsCoordinator.java"}) {
            String content = Files.readString(GUI_DIR.resolve(file));
            assertThat(content)
                    .as(file + " não deve usar JOptionPane legado")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    void bu03_layoutAndKpiInvariantsPreserved() throws IOException {
        String content = Files.readString(GUI_DIR.resolve("PurchaseBusinessUnitsPanel.java"));
        assertThat(content)
                .contains("KpiCard.createGrid(3, 8, 8)")
                .contains("\"Total de Negócios\"")
                .contains("\"Negócios Activos\"")
                .contains("\"Artigos Vinculados\"")
                .contains("TableFilter.bar(")
                .contains("UIHelper.ensureHeadersFit(table)")
                .contains("TableQuickPeekController.install(table")
                .contains("ClientTablePagination.install(table)")
                .contains("UIHelper.tableCardTop(\"Linhas de Negócio / Distribuição\"");
    }
}
