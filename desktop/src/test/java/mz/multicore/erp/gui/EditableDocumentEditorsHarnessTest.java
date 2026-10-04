package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness estrutural da SPEC-EDITABLE-DOCUMENT-EDITORS-001.
 *
 * <p>As regras financeiras e de estado são exercitadas nos testes dos Services; este harness
 * protege a integração UI/HTTP e impede regressões para formulários modais nos fluxos activos.</p>
 */
class EditableDocumentEditorsHarnessTest {

    private static String gui(String relative) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui")
                .resolve(relative));
    }

    private static String root(String relative) throws Exception {
        return Files.readString(Path.of("..").resolve(relative));
    }

    @Test
    void ede01_cotacaoUsaEditorDePaginaComTabelaEditavel() throws Exception {
        String panel = gui(Path.of("commercial", "QuotationsPanel.java").toString());
        String form = gui(Path.of("commercial", "QuotationEditorForm.java").toString());

        assertThat(panel).contains("new CardLayout()", "new DocumentEditorHost(")
                .contains("Editar / Consultar", "getClickCount() == 2")
                .contains("apiClient.updateQuotation(loaded.id(), request)")
                .doesNotContain("new QuotationEditorDialog");
        assertThat(form).contains("UIHelper.tableCardTop(\"Itens da Cotação\"")
                .contains("Adicionar linha", "Remover item", "syncLineFromGrid")
                .doesNotContain("JPanel lineForm")
                .contains("UpdateQuotationRequest");
    }

    @Test
    void ede02_encomendaFornecedorUsaMesmoEditorParaCriarActualizarEConsultar() throws Exception {
        String panel = gui("PurchaseOrdersPanel.java");

        assertThat(panel).contains("new CardLayout()", "new DocumentEditorHost(")
                .contains("Editar / Consultar")
                .contains("UIHelper.tableCardTop(\"Linhas da Encomenda\"")
                .contains("owner.purchaseApiClient.updateOrder(editingOrder.id(), request)")
                .contains("Adicionar linha", "Remover item", "syncPoLineFromGrid")
                .doesNotContain("new ModernFormDialog");
    }

    @Test
    void ede03_putsExigemVersaoEProtegemEstadoNoBackend() throws Exception {
        String quotationController = root("backend/src/main/java/mz/multicore/erp/modules/comercial/controller/QuotationController.java");
        String quotationService = root("backend/src/main/java/mz/multicore/erp/modules/comercial/service/QuotationService.java");
        String purchaseController = root("backend/src/main/java/mz/multicore/erp/modules/purchases/controller/PurchaseController.java");
        String purchaseService = root("backend/src/main/java/mz/multicore/erp/modules/purchases/service/PurchaseOrderService.java");

        assertThat(quotationController).contains("@PutMapping(\"/{id}\")", "quotationService.update(id, request)");
        assertThat(quotationService).contains("QuotationStatus.DRAFT", "request.version() != quotation.getVersion()")
                .contains("saveAndFlush", "QUOTATION_UPDATE");
        assertThat(purchaseController).contains("@PutMapping(\"/orders/{id}\")", "purchaseOrderService.updateOrder(id, request)");
        assertThat(purchaseService).contains("PurchaseOrder.ORDERED", "request.version() != order.getVersion()")
                .contains("nz(line.getReceivedQuantity()).signum() > 0", "saveAndFlush", "PURCHASE_ORDER_UPDATE");
    }

    @Test
    void ede04_contratosVersaoMigracaoESpecPermanecemAlinhados() throws Exception {
        String quotationRequest = root("contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/UpdateQuotationRequest.java");
        String purchaseRequest = root("contracts/src/main/java/mz/multicore/erp/modules/purchases/dto/UpdatePurchaseOrderRequest.java");
        String quotationDto = root("contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/QuotationDTO.java");
        String purchaseDto = root("contracts/src/main/java/mz/multicore/erp/modules/purchases/dto/PurchaseOrderDTO.java");
        String migration = root("backend/src/main/resources/db/migration/V77__editable_document_versions.sql");
        String spec = root("docs/EDITABLE_DOCUMENT_EDITORS_SPEC.md");

        assertThat(quotationRequest).contains("Long version", "List<CreateQuotationLineRequest> lines");
        assertThat(purchaseRequest).contains("Long version", "List<CreatePurchaseOrderLineRequest> lines");
        assertThat(quotationDto).contains("long version");
        assertThat(purchaseDto).contains("long version");
        assertThat(migration).containsIgnoringCase("purchase_orders")
                .containsIgnoringCase("version");
        assertThat(spec).contains("Cotação / pró-forma", "Encomenda a fornecedor")
                .contains("QUOTATION_UPDATE", "PURCHASE_ORDER_UPDATE");
    }
}
