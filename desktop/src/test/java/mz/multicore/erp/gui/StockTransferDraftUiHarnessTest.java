package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness estrutural do editor definido em STOCK_TRANSFER_DRAFT_LIFECYCLE_SPEC. */
class StockTransferDraftUiHarnessTest {

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", file));
    }

    @Test
    void transferUsesFullPageDocumentEditorAndSearchableLines() throws Exception {
        String panel = source("StockPanel.java");
        String form = source("StockTransferEditorForm.java");
        String actions = source("StockTransferActions.java");

        assertThat(panel).contains("new DocumentEditorHost")
                .contains("transferDocuments, transferMenu, transferBtn")
                .contains("Editar / Consultar", "Submeter", "Cancelar")
                .doesNotContain("printTransferBtn, transferMenu, transferBtn");
        assertThat(actions).contains("transferPagesLayout.show", "stockTransferApiClient.submit");
        assertThat(form).contains("ProductSearchComboBox", "isCellEditable", "tableEditable")
                .contains("headerModel", "headerTable", "Dados da Transferência")
                .contains("Uma linha de dados gerais; edite as células directamente.")
                .contains("Itens da Transferência", "Ref.", "Cód. Barras", "Qtd", "Emb.", "Cx.",
                        "% Cx.", "Valor Unit.", "IVA")
                .contains("column >= COL_PRODUCT && column <= COL_BOXES")
                .contains("Edite directamente na grelha")
                .contains("new UpdateStockTransferRequest(loaded.version()")
                .doesNotContain("ModernFormDialog", "createDialogForm");
    }

    @Test
    void apiClientExposesTheCompleteDraftWorkflow() throws Exception {
        Path clientPath = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop",
                "client", "StockTransferApiClient.java");
        String client = Files.readString(clientPath);
        assertThat(client).contains("public StockTransferDTO update", "public StockTransferDTO submit",
                "public StockTransferDTO approve", "public StockTransferDTO reject",
                "public StockTransferDTO cancel");
    }
}
