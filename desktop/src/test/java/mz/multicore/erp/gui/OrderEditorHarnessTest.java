package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEditorHarnessTest {

    private static String gui(String name) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", name));
    }

    private static String root(String relative) throws Exception {
        return Files.readString(Path.of("..").resolve(relative));
    }

    @Test
    void oe01_criarEEditarPartilhamOMesmoEditorDePagina() throws Exception {
        String view = gui("CommercialOrdersView.java");
        String panel = gui("ComercialPanel.java");
        String actions = gui("CommercialOrderEditorActions.java");

        assertThat(view).contains("owner.orderEditor = new DocumentEditorHost(")
                .contains("owner.encomendasHost.add(owner.orderEditor, \"editor\")")
                .contains("owner::openSelectedOrderEditor")
                .doesNotContain("JDialog orderEditor", "ModernFormDialog orderEditor");
        assertThat(panel).contains("void openOrderEditor()", "void openSelectedOrderEditor()")
                .contains("CommercialOrderEditorActions.openSelected(this)");
        assertThat(actions).contains("static void load(ComercialPanel owner, OrderDTO order)")
                .contains("owner.orderEditor.setEditorTitle(\"Editar Encomenda \"");
    }

    @Test
    void oe02_tabelaPermiteActualizarERemoverItensSemModal() throws Exception {
        String view = gui("CommercialOrdersView.java");
        String panel = gui("ComercialPanel.java");
        String actions = gui("CommercialOrderEditorActions.java");

        assertThat(view).contains("Adicionar linha", "Remover item")
                .contains("owner.orderGridEditable && (c <= 3 || c == 9 || c == 10)")
                .contains("UIHelper.tableCardTop(\"Itens da Encomenda\"")
                .doesNotContain("editItemBtn");
        assertThat(panel).contains("void addDraftOrderLine()")
                .contains("void removeSelectedDraftOrderLine()");
        assertThat(actions).contains("static void syncLineFromGrid")
                .contains("owner.draftOrderLines.set(row");
    }

    @Test
    void oe03_actualizacaoUsaPutEControloDeVersao() throws Exception {
        String controller = root("backend/src/main/java/mz/multicore/erp/modules/comercial/controller/ComercialController.java");
        String service = root("backend/src/main/java/mz/multicore/erp/modules/comercial/service/ComercialService.java");
        String client = root("desktop/src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java");
        String request = root("contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/UpdateOrderRequest.java");
        String response = root("contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/OrderDTO.java");

        assertThat(controller).contains("@PutMapping(\"/orders/{id}\")", "comercialService.updateOrder(id, request)");
        assertThat(client).contains("public OrderDTO updateOrder(Long id, UpdateOrderRequest request)")
                .contains(".put(\"/api/comercial/orders/\" + id");
        assertThat(request).contains("Long version", "List<CreateInvoiceLineRequest> lines");
        assertThat(response).contains("long version");
        assertThat(service).contains("request.version() != order.getVersion()")
                .contains("PENDING_APPROVAL", "AWAITING_SEPARATION")
                .contains("revalidateReservation(order, warehouse, replacement)")
                .contains("auditLogService.logCurrent(\"ORDER_UPDATE\"");
    }

    @Test
    void oe04_editorProtegeAlteracoesNaoGravadas() throws Exception {
        String host = gui(Path.of("components", "DocumentEditorHost.java").toString());
        String view = gui("CommercialOrdersView.java");

        assertThat(host).contains("Há alterações por gravar", "setEditorTitle", "setSaveText")
                .contains("KeyEvent.VK_S", "KeyEvent.VK_ESCAPE");
        assertThat(view).contains("owner::isOrderEditorDirty")
                .contains("owner.markOrderEditorDirty()");
    }
}
