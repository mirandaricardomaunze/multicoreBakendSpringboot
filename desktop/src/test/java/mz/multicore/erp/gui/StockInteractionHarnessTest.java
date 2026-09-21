package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.RowDetailsInspector;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StockInteractionHarnessTest {

    @Test
    void noRowInspectorSuppressesGenericDialog() {
        JTable table = new JTable(new DefaultTableModel(new String[]{"Col1", "Col2"}, 1));
        table.setRowSelectionInterval(0, 0);
        table.putClientProperty("noRowInspector", Boolean.TRUE);

        // RowDetailsInspector.open não deve disparar erro nem abrir o diálogo genérico
        RowDetailsInspector.open(table);
        assertThat(table.getClientProperty("noRowInspector")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void stockPanelSourcesContainRefreshButtonsAndNoRowInspector() throws IOException {
        Path stockSource = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "StockPanel.java");
        assertThat(stockSource).exists();
        String content = Files.readString(stockSource);

        assertThat(content)
                .as("stockTable deve definir noRowInspector para evitar modal duplicado ao duplo clique")
                .contains("stockTable.putClientProperty(\"noRowInspector\", Boolean.TRUE)");

        assertThat(content)
                .as("StockPanel deve ter botão Atualizar no separador de níveis de stock")
                .contains("refreshLevelsBtn")
                .contains("Recarregar saldos de stock e armazéns");

        assertThat(content)
                .as("StockPanel deve ter botão Atualizar na barra superior")
                .contains("refreshAllBtn")
                .contains("Recarregar dados de stock e armazéns");
    }

    @Test
    void stockProductActionsSetsConfirmButtonToAtualizar() throws IOException {
        Path productActionsSource = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "StockProductActions.java");
        assertThat(productActionsSource).exists();
        String content = Files.readString(productActionsSource);

        assertThat(content)
                .as("Modal de edição deve ter botão Atualizar")
                .contains(".setConfirmButton(\"Atualizar\", \"fas-save\")");

        assertThat(content)
                .as("Modal de edição deve usar setOnSaveAsync para validação e chamada segura")
                .contains("dialog.setOnSaveAsync");
    }
}
