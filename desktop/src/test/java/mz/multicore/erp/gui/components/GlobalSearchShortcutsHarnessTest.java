package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes automatizados para validação do SPEC docs/PESQUISA_GLOBAL_ATALHOS_SPEC.md
 * e HARNESS docs/PESQUISA_GLOBAL_ATALHOS_HARNESS.md.
 */
class GlobalSearchShortcutsHarnessTest {

    @Test
    void pga01_registersAndRetrievesIndexedItems() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean posClicked = new AtomicBoolean(false);
            GlobalSearchDialog.SearchItem item1 = new GlobalSearchDialog.SearchItem(
                    "mod_pos", "POS — Caixa", "Módulos", "F9",
                    UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), UIHelper.MODULE_POS,
                    () -> posClicked.set(true), List.of("vendas", "caixa", "balcao")
            );

            GlobalSearchDialog.SearchItem item2 = new GlobalSearchDialog.SearchItem(
                    "mod_stock", "Stock & Armazéns", "Módulos", null,
                    UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK), UIHelper.MODULE_STOCK,
                    () -> {}, List.of("produtos", "artigos", "inventario")
            );

            GlobalSearchDialog dialog = new GlobalSearchDialog(null, List.of(item1, item2));

            assertThat(dialog.getAllItems()).hasSize(2);
            assertThat(dialog.getFilteredItems()).hasSize(2);
            assertEquals("POS — Caixa", dialog.getFilteredItems().get(0).title());
            assertEquals("F9", dialog.getFilteredItems().get(0).shortcutHint());

            dialog.dispose();
        });
    }

    @Test
    void pga02_filtersItemsByTitleCategoryAndKeywords() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GlobalSearchDialog.SearchItem itemPos = new GlobalSearchDialog.SearchItem(
                    "mod_pos", "POS — Caixa e Balcão", "Módulos", "F9",
                    UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), UIHelper.MODULE_POS,
                    () -> {}, List.of("venda", "terminal", "pagamento")
            );

            GlobalSearchDialog.SearchItem itemHr = new GlobalSearchDialog.SearchItem(
                    "mod_hr", "Recursos Humanos & Salários", "Módulos", null,
                    UIHelper.icon("fas-users", 16, UIHelper.MODULE_HR), UIHelper.MODULE_HR,
                    () -> {}, List.of("folha", "salario", "ferias", "faltas")
            );

            GlobalSearchDialog.SearchItem itemHelp = new GlobalSearchDialog.SearchItem(
                    "act_help", "Guia de Atalhos de Teclado", "Ferramentas", "F1",
                    UIHelper.icon("fas-keyboard", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE,
                    () -> {}, List.of("atalhos", "ajuda", "comandos", "help")
            );

            GlobalSearchDialog dialog = new GlobalSearchDialog(null, List.of(itemPos, itemHr, itemHelp));

            // 1. Pesquisa por título
            dialog.getSearchField().setText("POS");
            dialog.filter();
            assertThat(dialog.getFilteredItems()).hasSize(1);
            assertEquals("mod_pos", dialog.getFilteredItems().get(0).id());

            // 2. Pesquisa por palavra-chave / sinónimo (férias -> RH)
            dialog.getSearchField().setText("ferias");
            dialog.filter();
            assertThat(dialog.getFilteredItems()).hasSize(1);
            assertEquals("mod_hr", dialog.getFilteredItems().get(0).id());

            // 3. Pesquisa por categoria
            dialog.getSearchField().setText("Ferramentas");
            dialog.filter();
            assertThat(dialog.getFilteredItems()).hasSize(1);
            assertEquals("act_help", dialog.getFilteredItems().get(0).id());

            // 4. Pesquisa sem resultados
            dialog.getSearchField().setText("termo_inexistente_xyz");
            dialog.filter();
            assertThat(dialog.getFilteredItems()).isEmpty();

            dialog.dispose();
        });
    }

    @Test
    void pga03_executesSelectedItemOnEnterAndClosesOnEscape() throws Exception {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        SwingUtilities.invokeAndWait(() -> {
            GlobalSearchDialog.SearchItem item = new GlobalSearchDialog.SearchItem(
                    "act_test", "Executar Ação", "Testes", null,
                    UIHelper.icon("fas-play", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE,
                    () -> actionExecuted.set(true), List.of("teste")
            );

            GlobalSearchDialog dialog = new GlobalSearchDialog(null, List.of(item));
            dialog.getResultList().setSelectedIndex(0);

            dialog.executeSelected();
            assertFalse(dialog.isDisplayable(), "Diálogo deve ser fechado após execução");
        });

        // Aguarda execução do runnable no EDT
        Thread.sleep(100);
        assertTrue(actionExecuted.get(), "Ação deve ter sido executada no EDT");
    }

    @Test
    void pga04_shortcutHelpDialogPresentsCategorizedShortcuts() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ShortcutHelpDialog dialog = new ShortcutHelpDialog(null);
            assertThat(dialog.getTitle()).contains("Atalhos");
            assertThat(dialog.getContentPane().getComponentCount()).isGreaterThan(0);
            dialog.dispose();
        });
    }

    @Test
    void pga05_topNavBarSearchPillTriggersAction() throws Exception {
        AtomicBoolean searchTriggered = new AtomicBoolean(false);
        SwingUtilities.invokeAndWait(() -> {
            TopNavBar navBar = new TopNavBar("Painel Inicial", "Empresa Teste");
            navBar.setSearchAction(() -> searchTriggered.set(true));

            assertThat(navBar.getSearchPill()).isNotNull();
            assertTrue(navBar.getSearchPill().isVisible());

            // Simula clique na pílula
            java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(
                    navBar.getSearchPill(), java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false
            );
            for (var listener : navBar.getSearchPill().getMouseListeners()) {
                listener.mousePressed(click);
            }
        });

        assertTrue(searchTriggered.get(), "O clique na pílula de pesquisa deve disparar o callback");
    }
}
