package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Atalhos Favoritos na Barra Lateral (SFAV-01 a SFAV-08)")
class SidebarFavoritesHarnessTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("multicore.test.headless", "true");
    }

    @Test
    @DisplayName("SFAV-01: SidebarFavoritesManager retorna favoritos por omissão se vazio")
    void testDefaultFavorites() {
        SidebarFavoritesManager manager = SidebarFavoritesManager.getInstance();
        List<String> favs = manager.getFavorites();
        assertNotNull(favs);
        assertFalse(favs.isEmpty());
        assertTrue(favs.contains("Painel Inicial") || favs.contains("POS — Caixa"));
    }

    @Test
    @DisplayName("SFAV-02: Adição e notificação ao alternar favorito (toggleFavorite)")
    void testToggleAddFavorite() {
        SidebarFavoritesManager manager = SidebarFavoritesManager.getInstance();
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            String testModule = "Clientes Teste SFAV";
            assertFalse(manager.isFavorite(testModule));

            manager.toggleFavorite(testModule);
            assertTrue(manager.isFavorite(testModule));
            assertTrue(notified.get());

            // Limpeza
            manager.removeFavorite(testModule);
            assertFalse(manager.isFavorite(testModule));
        } finally {
            manager.removeChangeListener(listener);
        }
    }

    @Test
    @DisplayName("SFAV-03: Remoção de favorito existente")
    void testRemoveFavorite() {
        SidebarFavoritesManager manager = SidebarFavoritesManager.getInstance();
        String item = "ModuloRemocaoTest";
        manager.addFavorite(item);
        assertTrue(manager.isFavorite(item));

        manager.removeFavorite(item);
        assertFalse(manager.isFavorite(item));
    }

    @Test
    @DisplayName("SFAV-04: Definição em lote com setFavorites")
    void testSetFavoritesBatch() {
        SidebarFavoritesManager manager = SidebarFavoritesManager.getInstance();
        List<String> original = manager.getFavorites();

        try {
            manager.setFavorites(List.of("POS — Caixa", "Tesouraria"));
            assertEquals(2, manager.getFavorites().size());
            assertTrue(manager.isFavorite("POS — Caixa"));
            assertTrue(manager.isFavorite("Tesouraria"));
        } finally {
            manager.setFavorites(original);
        }
    }

    @Test
    @DisplayName("SFAV-05: CollapsibleSidebar renderiza a secção FAVORITOS em headless")
    void testCollapsibleSidebarHeadlessFavorites() {
        assertDoesNotThrow(() -> {
            CollapsibleSidebar sidebar = new CollapsibleSidebar("MULTICORE", "Empresa Teste");
            sidebar.addSection("Operações");
            sidebar.addItem("fas-home", "Painel Inicial", Color.BLUE, () -> {});
            sidebar.addItem("fas-cash-register", "POS — Caixa", Color.GREEN, () -> {});
            sidebar.rebuildFavorites();
            assertNotNull(sidebar);
        });
    }

    @Test
    @DisplayName("SFAV-06: SidebarNavItem suporta evento de clique com botão direito")
    void testSidebarNavItemRightClick() {
        assertDoesNotThrow(() -> {
            SidebarNavItem item = new SidebarNavItem("fas-box", "Stock & Armazéns", Color.ORANGE, () -> {});
            MouseEvent rightClick = new MouseEvent(
                    item, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    0, 10, 10, 1, true, MouseEvent.BUTTON3
            );
            item.dispatchEvent(rightClick);
        });
    }

    @Test
    @DisplayName("SFAV-07: Tolerância e resiliência a nulos em favs")
    void testNullResilience() {
        SidebarFavoritesManager manager = SidebarFavoritesManager.getInstance();
        assertFalse(manager.isFavorite(null));
        assertDoesNotThrow(() -> manager.addFavorite(null));
        assertDoesNotThrow(() -> manager.removeFavorite(null));
        assertDoesNotThrow(() -> manager.toggleFavorite(null));
    }
}
