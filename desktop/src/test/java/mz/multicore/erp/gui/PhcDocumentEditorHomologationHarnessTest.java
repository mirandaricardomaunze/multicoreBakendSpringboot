package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness estrutural da SPEC-PHC-DOCUMENT-HOMOLOGATION-001. */
class PhcDocumentEditorHomologationHarnessTest {

    private static String gui(String relative) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui")
                .resolve(relative));
    }

    private static String root(String relative) throws Exception {
        return Files.readString(Path.of("..").resolve(relative));
    }

    @Test
    void hmg01_fluxoAntigoDeTransportarLinhaParaFormularioFoiRemovido() throws Exception {
        String orderPanel = gui("ComercialPanel.java");
        String orderActions = gui("CommercialOrderEditorActions.java");
        String purchase = gui("PurchaseOrdersPanel.java");

        assertThat(orderPanel).doesNotContain("editingDraftOrderLine", "editSelectedDraftOrderLine",
                "orderLineSaveButton", "orderPackageEditor");
        assertThat(orderActions).doesNotContain("addOrUpdateLine", "editSelectedLine",
                "Actualizar item");
        assertThat(purchase).doesNotContain("editingLineIndex", "editSelectedDraftLine",
                "editLineButton", "Actualizar item");
    }

    @Test
    void hmg02_todosOsEditoresFechamACelulaEValidamAntesDeGravar() throws Exception {
        String order = gui("CommercialOrderSubmission.java");
        String invoice = gui("ComercialPanel.java");
        String purchase = gui("PurchaseOrdersPanel.java");
        String quotation = gui(Path.of("commercial", "QuotationEditorForm.java").toString());
        String transfer = gui("StockTransferEditorForm.java");

        assertThat(order).contains("CommercialOrderEditorActions.stopCellEditing(owner)")
                .contains("for (int row = 0; row < owner.draftOrderLines.size(); row++)");
        assertThat(invoice).contains("stopInvoiceCellEditing()")
                .contains("for (int row = 0; row < draftLines.size(); row++)");
        assertThat(purchase).contains("stopPoCellEditing()")
                .contains("for (int row = 0; row < owner.poDraftLines.size(); row++)");
        assertThat(quotation).contains("stopCellEditing()")
                .contains("for (int row = 0; row < draftLines.size(); row++)");
        assertThat(transfer).contains("stopCellEditing()")
                .contains("for (int row = 0; row < linesModel.getRowCount(); row++)");
    }

    @Test
    void hmg03_composicaoDeEmbalagemChegaAQuantidadeTotal() throws Exception {
        String orders = gui("CommercialOrderEditorActions.java");
        String invoices = gui("ComercialPanel.java");
        String purchase = gui("PurchaseOrdersPanel.java");
        String quotation = gui(Path.of("commercial", "QuotationEditorForm.java").toString());
        String transfer = gui("StockTransferEditorForm.java");

        for (String source : new String[]{orders, invoices, purchase, quotation, transfer}) {
            assertThat(source).contains("packagesPerBox()", "unitsPerPackage()")
                    .contains("multiply(unitsPerBox)");
        }
    }

    @Test
    void hmg04_facturaEmitidaNaoTemEndpointDeEdicao() throws Exception {
        String controller = root("backend/src/main/java/mz/multicore/erp/modules/comercial/controller/ComercialController.java");
        String spec = root("docs/PHC_DOCUMENT_EDITOR_HOMOLOGATION_SPEC.md");

        assertThat(controller).contains("@PostMapping(\"/invoices\")")
                .contains("@PostMapping(\"/invoices/{id}/cancel\")")
                .doesNotContain("@PutMapping(\"/invoices/{id}\")");
        assertThat(spec).contains("Caixa → Embalagem → Unidade", "actualização, leitura", "PDFs")
                .contains("não cria", "base operacional");
    }
}
