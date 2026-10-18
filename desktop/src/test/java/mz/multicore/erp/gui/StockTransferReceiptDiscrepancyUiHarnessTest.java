package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de UI para Recepção de Stock em Trânsito com Divergência")
class StockTransferReceiptDiscrepancyUiHarnessTest {

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", file));
    }

    @Test
    @DisplayName("humanStatus traduz RECEIVED_WITH_DISCREPANCY para pt-MZ")
    void humanStatus_traduzRecebidoComDivergencia() {
        assertEquals("Recebido c/ divergência", UIHelper.humanStatus("RECEIVED_WITH_DISCREPANCY"));
        assertEquals("Recebido c/ divergência", UIHelper.humanStatus("RECEBIDO C/ DIVERGÊNCIA"));
        assertEquals("Recebido c/ divergência", UIHelper.humanStatus("RECEBIDO C/ DIVERGENCIA"));
    }

    @Test
    @DisplayName("TableCellRenderers.status renderiza badge âmbar para RECEIVED_WITH_DISCREPANCY")
    void badgeRenderer_renderizaAmbarParaDivergencia() {
        TableCellRenderer renderer = TableCellRenderers.status();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Estado"}, 0);
        model.addRow(new Object[]{1L, "RECEIVED_WITH_DISCREPANCY"});
        JTable table = new JTable(model);

        Component comp = renderer.getTableCellRendererComponent(table, model.getValueAt(0, 1), false, false, 0, 1);
        assertThat(comp).isInstanceOf(javax.swing.JLabel.class);
        javax.swing.JLabel label = (javax.swing.JLabel) comp;
        assertEquals("Recebido c/ divergência", label.getText());
        assertEquals(UIHelper.PENDING_YELLOW, label.getForeground());
    }

    @Test
    @DisplayName("TableFilter.cellMatches reconhece bidirecionalmente RECEIVED_WITH_DISCREPANCY")
    void cellMatches_reconheceRecebidoComDivergencia() {
        assertTrue(TableFilter.cellMatches("RECEIVED_WITH_DISCREPANCY", "Recebido c/ divergência"));
        assertTrue(TableFilter.cellMatches("Recebido c/ divergência", "Recebido c/ divergência"));
        assertTrue(TableFilter.cellMatches("RECEIVED_WITH_DISCREPANCY", "Todos os estados"));
    }

    @Test
    @DisplayName("StockPanel inclui opção no filtro de estados e contabiliza no KPI de concluídas")
    void stockPanel_filtroContemRecebidoComDivergencia() throws Exception {
        String panel = source("StockPanel.java");

        assertThat(panel).contains("\"Recebido c/ divergência\"");
        assertThat(panel).contains("\"RECEIVED_WITH_DISCREPANCY\".equals(status)");
    }

    @Test
    @DisplayName("StockTransferReceivingDialog possui proteção contra paginação comprimida e regras canónicas")
    void dialogo_conferenciaDeRecepcao_estrutura() throws Exception {
        String dialog = source("StockTransferReceivingDialog.java");

        assertThat(dialog).contains("ClientTablePagination.DISABLED, Boolean.TRUE");
        assertThat(dialog).contains("COL_SENT");
        assertThat(dialog).contains("COL_RECEIVED");
        assertThat(dialog).contains("COL_RETURNED");
        assertThat(dialog).contains("COL_REASON");
        assertThat(dialog).contains("Conferência e Recepção de Mercadoria");
    }

    @Test
    @DisplayName("StockTransferActions usa o diálogo de conferência e trata o estado terminal")
    void actions_usaDialogoDeConferenciaEBloqueiaCancelamento() throws Exception {
        String actions = source("StockTransferActions.java");

        assertThat(actions).contains("StockTransferReceivingDialog.show");
        assertThat(actions).contains("\"RECEIVED_WITH_DISCREPANCY\".equals(selected.status())");
        assertThat(actions).contains("!\"APPROVED\".equals(selected.status()) && !\"IN_TRANSIT\".equals(selected.status()) && !\"RECEIVED\".equals(selected.status()) && !\"RECEIVED_WITH_DISCREPANCY\".equals(selected.status())");
    }
}
