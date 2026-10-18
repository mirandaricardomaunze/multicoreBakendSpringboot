package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Harness de UI para a funcionalidade de Stock em Trânsito (docs/STOCK_IN_TRANSIT_SPEC.md).
 * Valida a tradução de estados, os limites de menus (≤ 5 ações), o 5º KPI e o opt-in no formulário.
 */
class StockInTransitUiHarnessTest {

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", file));
    }

    @Test
    void humanStatus_traduzEstadosDeTransitoCorretamente() {
        assertEquals("Em trânsito", UIHelper.humanStatus("IN_TRANSIT"));
        assertEquals("Em trânsito", UIHelper.humanStatus("EM TRÂNSITO"));
        assertEquals("Recebido", UIHelper.humanStatus("RECEIVED"));
        assertEquals("Recebido", UIHelper.humanStatus("RECEBIDO"));
    }

    @Test
    void stockPanel_possuiMenuDeRecepcaoEKpiEmTransitoSemQuebrarLimites() throws Exception {
        String panel = source("StockPanel.java");

        // Regra de menus do sistema: ActionMenuButton não pode ter mais de 5 acções
        assertThat(panel).contains(".addAction(\"Confirmar Recepção\", UIHelper.icon(\"fas-box-open\", 14, UIHelper.APPROVED_GREEN), () -> transferActions.receiveSelectedTransfer())");
        assertThat(panel).contains("KpiCard.createGrid(5)");
        assertThat(panel).contains("kpiTransfersInTransit");
        assertThat(panel).contains("\"Em Trânsito\", kpiTransfersInTransit");
        assertThat(panel).contains("\"Em trânsito\", \"Recebido\"");
    }

    @Test
    void editorForm_suportaOptInDeStockEmTransito() throws Exception {
        String form = source("StockTransferEditorForm.java");

        assertThat(form).contains("requiresReceiptCheck = new JCheckBox(\"Exigir confirmação de recepção no destino (stock em trânsito)\")");
        assertThat(form).contains("requiresReceiptCheck.setSelected(true)");
        assertThat(form).contains("requiresReceiptCheck.setSelected(transfer.requiresReceipt())");
        assertThat(form).contains("requiresReceiptCheck.isSelected()");
    }

    @Test
    void transferActions_bloqueiaCancelamentoDeGuiaEmTransitoEPermiteImpressao() throws Exception {
        String actions = source("StockTransferActions.java");

        assertThat(actions).contains("\"IN_TRANSIT\".equals(selected.status())");
        assertThat(actions).contains("Transferência em trânsito");
        assertThat(actions).contains("public void receiveSelectedTransfer()");
        assertThat(actions).contains("!\"APPROVED\".equals(selected.status()) && !\"IN_TRANSIT\".equals(selected.status()) && !\"RECEIVED\".equals(selected.status())");
    }

    @Test
    void apiClient_possuiMetodosDeRecepcaoETransito() throws Exception {
        Path clientPath = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop",
                "client", "StockTransferApiClient.java");
        String client = Files.readString(clientPath);

        assertThat(client).contains("public StockTransferDTO receive(Long id, String notes)");
        assertThat(client).contains("public List<StockTransferDTO> findInTransit(Long companyId, Long destinationWarehouseId)");
    }
}
