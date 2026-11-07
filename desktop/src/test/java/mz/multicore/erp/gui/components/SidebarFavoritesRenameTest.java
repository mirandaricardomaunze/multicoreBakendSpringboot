package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/UI_ORGANIZACAO_NAVEGACAO_SPEC.md: Armazéns e Categorias saíram de Stock. */
class SidebarFavoritesRenameTest {

    @Test
    @DisplayName("O favorito antigo 'Stock & Armazéns' continua a abrir o Stock")
    void oldStockFavoriteBecomesStock() {
        assertThat(SidebarFavoritesManager.renamedLabel("Stock & Armazéns")).isEqualTo("Stock");
        assertThat(SidebarFavoritesManager.renamedLabel("Compras")).isEqualTo("Compras");
    }
}
