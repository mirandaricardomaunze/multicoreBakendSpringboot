package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StockWarehousesPanelDecompositionHarnessTest {

    private static final Path GUI_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    void wh01_lineCeilingsRespected() throws IOException {
        Path panel = GUI_DIR.resolve("StockWarehousesPanel.java");
        Path dialog = GUI_DIR.resolve("WarehouseFormDialog.java");
        Path coordinator = GUI_DIR.resolve("StockWarehouseViewCoordinator.java");

        assertThat(Files.lines(panel).count())
                .as("StockWarehousesPanel deve manter foco estrito de layout e tabela")
                .isLessThanOrEqualTo(195);

        assertThat(Files.lines(dialog).count())
                .as("WarehouseFormDialog deve gerenciar modal de armazém")
                .isLessThanOrEqualTo(95);

        assertThat(Files.lines(coordinator).count())
                .as("StockWarehouseViewCoordinator gerencia métricas e visual")
                .isLessThanOrEqualTo(90);
    }

    @Test
    void wh02_cleanArchitectureAndZeroJOptionPane() throws IOException {
        for (String file : new String[]{"StockWarehousesPanel.java", "WarehouseFormDialog.java", "StockWarehouseViewCoordinator.java"}) {
            String content = Files.readString(GUI_DIR.resolve(file));
            assertThat(content)
                    .as(file + " não deve usar JOptionPane legado")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    void wh03_layoutAndKpiInvariantsPreserved() throws IOException {
        String content = Files.readString(GUI_DIR.resolve("StockWarehousesPanel.java"));
        assertThat(content)
                .contains("KpiCard.createGrid(4)")
                .contains("\"Armazéns Activos\"")
                .contains("\"Capacidade Instalada\"")
                .contains("\"Valor em Armazém\"")
                .contains("\"Rede de Distribuição\"")
                .contains("TableFilter.bar(")
                .contains("UIHelper.ensureHeadersFit(owner.warehousesTable)")
                .contains("TableQuickPeekController.install(owner.warehousesTable")
                .contains("ClientTablePagination.install(owner.warehousesTable)")
                .contains("UIHelper.tableCardTop(\"Gestão de Armazéns\"");
    }
}
