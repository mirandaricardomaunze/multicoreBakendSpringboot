package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.GlobalSearchDialog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação da barra de pesquisa rápida Omnibar (Ctrl+K).
 */
class GlobalOmnibarHarnessTest {

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    @Test
    @DisplayName("GOB-01: MainFrame tem atalho Ctrl+K registado e constrói índice de busca")
    void testMainFrameShortcutRegistration() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/MainFrame.java");
        assertTrue(Files.exists(path), "MainFrame.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("KeyEvent.VK_K"), "Deve ter atalho VK_K registado");
        assertTrue(content.contains("openGlobalSearch"), "Deve chamar openGlobalSearch");
        assertTrue(content.contains("buildSearchIndex"), "Deve conter método buildSearchIndex");
        assertTrue(content.contains("act_new_invoice"), "Deve conter acção de emissão de fatura");
        assertTrue(content.contains("act_receive_po"), "Deve conter acção de recepção de mercadoria");
        assertTrue(content.contains("act_pos_cash_move"), "Deve conter acção de sangria/suprimento");
    }

    @Test
    @DisplayName("GOB-02: SearchItem suporta filtros insensíveis a maiúsculas e palavras-chave")
    void testSearchItemMatching() {
        AtomicBoolean actionRan = new AtomicBoolean(false);
        GlobalSearchDialog.SearchItem item = new GlobalSearchDialog.SearchItem(
                "test_item", "Emitir Factura Comercial", "Vendas", "Ctrl+F",
                null, null, () -> actionRan.set(true), List.of("fatura", "venda", "ft", "recibo")
        );

        assertTrue(item.matches("factura"));
        assertTrue(item.matches("FACTURA"));
        assertTrue(item.matches("ft"));
        assertTrue(item.matches("Vendas"));
        assertFalse(item.matches("inexistente_termo_xyz"));

        item.action().run();
        assertTrue(actionRan.get(), "A acção associada ao item deve ser executada");
    }
}
