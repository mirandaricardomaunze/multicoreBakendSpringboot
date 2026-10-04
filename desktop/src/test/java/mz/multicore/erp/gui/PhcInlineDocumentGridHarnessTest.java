package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness estrutural da SPEC-PHC-INLINE-GRID-001. */
class PhcInlineDocumentGridHarnessTest {

    private static String gui(String relative) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui")
                .resolve(relative));
    }

    @Test
    void phc01_encomendaClienteEditaCelulasSemFormularioDeLinha() throws Exception {
        String view = gui("CommercialOrdersView.java");
        String actions = gui("CommercialOrderEditorActions.java");
        assertThat(view).contains("owner.orderGridEditable && (c <= 3 || c == 9 || c == 10)")
                .contains("new DefaultCellEditor(owner.orderProductCombo)")
                .contains("Adicionar linha", "Edição directa na grelha")
                .doesNotContain("editItemBtn");
        assertThat(actions).contains("static void syncLineFromGrid", "static void addBlankLine")
                .contains("product.packagesPerBox()", "product.unitsPerPackage()");
    }

    @Test
    void phc02_cotacaoECompraUsamPesquisaEQuantidadesNaGrelha() throws Exception {
        String quotation = gui(Path.of("commercial", "QuotationEditorForm.java").toString());
        String purchase = gui("PurchaseOrdersPanel.java");
        assertThat(quotation).contains("return tableEditable && (column <= 3 || column == 6)")
                .contains("syncLineFromGrid", "Adicionar linha")
                .doesNotContain("JPanel lineForm");
        assertThat(purchase).contains("return lineGridEditable && (c <= 3 || (c >= 5 && c <= 8))")
                .contains("syncPoLineFromGrid", "Adicionar linha")
                .contains("Preço Unit.", "Lote", "Validade", "Série");
    }

    @Test
    void phc03_preparacaoDeFaturaEOperacoesCurtasMantemEdicaoDirecta() throws Exception {
        String invoices = gui("CommercialInvoicesView.java");
        String notes = gui(Path.of("commercial", "CommercialNotesPanel.java").toString());
        String purchases = gui("PurchaseOrdersPanel.java");
        assertThat(invoices).contains("owner.invoiceGridEditable && (c <= 3 || c == 7 || c == 8)")
                .contains("owner.syncInvoiceLineFromGrid", "Preparação tabular")
                .contains("depois de emitida a fatura é imutável");
        assertThat(notes).contains("return column == 6")
                .contains("new CreateCreditNoteLineRequest");
        assertThat(purchases).contains("return c == 4")
                .contains("A receber agora");
    }
}
