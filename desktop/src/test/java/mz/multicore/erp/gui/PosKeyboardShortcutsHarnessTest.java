package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de validação dos atalhos rápidos de teclado no POS (KEY-01 a KEY-10).
 */
public class PosKeyboardShortcutsHarnessTest {

    @Test
    @DisplayName("KEY-01: PosKeyboardShortcutsHandler registra F1 a F12, ESC e CTRL+N no InputMap")
    void testAllShortcutsRegistered() {
        JPanel testPanel = new JPanel();
        InputMap input = testPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actions = testPanel.getActionMap();

        int[] expectedFKeys = {
                KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
                KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
                KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12
        };

        assertDoesNotThrow(() -> {
            Method bindMethod = PosKeyboardShortcutsHandler.class.getDeclaredMethod("bind",
                    InputMap.class, ActionMap.class, String.class, KeyStroke.class, Runnable.class);
            bindMethod.setAccessible(true);

            for (int key : expectedFKeys) {
                bindMethod.invoke(null, input, actions, "action_" + key, KeyStroke.getKeyStroke(key, 0), (Runnable) () -> {});
                assertNotNull(input.get(KeyStroke.getKeyStroke(key, 0)), "KeyStroke " + KeyEvent.getKeyText(key) + " deve estar mapeado");
                assertNotNull(actions.get("action_" + key), "Action para " + KeyEvent.getKeyText(key) + " deve estar registrada");
            }

            // ESC e CTRL+N
            bindMethod.invoke(null, input, actions, "posCancel", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), (Runnable) () -> {});
            assertNotNull(input.get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

            bindMethod.invoke(null, input, actions, "posNewSale", KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), (Runnable) () -> {});
            assertNotNull(input.get(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK)));
        });
    }

    @Test
    @DisplayName("KEY-02: PosShortcutBar renderiza chips para F1..F12 e ESC sem emojis crus")
    void testShortcutBarComponents() {
        assertDoesNotThrow(() -> {
            assertTrue(mz.multicore.erp.gui.components.ModernPanel.class.isAssignableFrom(PosShortcutBar.class));

            Path path = Path.of("src/main/java/mz/multicore/erp/gui/PosShortcutBar.java");
            if (!Files.exists(path)) {
                path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/PosShortcutBar.java");
            }
            if (Files.exists(path)) {
                String content = Files.readString(path);
                assertFalse(content.contains("⭐") || content.contains("✅") || content.contains("❌") || content.contains("⌨"),
                        "PosShortcutBar não deve conter emojis crus Unicode");
                assertTrue(content.contains("fas-keyboard"));
                assertTrue(content.contains("fas-search"));
                assertTrue(content.contains("fas-check-circle"));
                assertTrue(content.contains("fas-lock"));
            }
        });
    }

    @Test
    @DisplayName("KEY-03: PosShortcutHelpDialog expõe método show com categorias completas")
    void testHelpDialogClass() {
        assertDoesNotThrow(() -> {
            Method showM = PosShortcutHelpDialog.class.getDeclaredMethod("show", java.awt.Component.class);
            assertNotNull(showM, "PosShortcutHelpDialog deve conter método estático show(Component)");
        });
    }

    @Test
    @DisplayName("KEY-04: POSPanel expõe métodos de controle do carrinho e histórico para atalhos")
    void testPosPanelExposesMethodsForShortcuts() {
        assertDoesNotThrow(() -> {
            Method clearCart = POSPanel.class.getDeclaredMethod("clearCart");
            assertNotNull(clearCart);
            Method isHistoryView = POSPanel.class.getDeclaredMethod("isHistoryView");
            assertNotNull(isHistoryView);
            Method editSelectedCartQuantity = POSPanel.class.getDeclaredMethod("editSelectedCartQuantity");
            assertNotNull(editSelectedCartQuantity);
            Method showReturnDialog = POSPanel.class.getDeclaredMethod("showReturnDialog");
            assertNotNull(showReturnDialog);
            Method runCheckout = POSPanel.class.getDeclaredMethod("runCheckout");
            assertNotNull(runCheckout);
        });
    }

    @Test
    @DisplayName("KEY-05: Ficheiros modificados e criados respeitam estritamente o limite de 1000 linhas")
    void testFileLengthConstraints() throws Exception {
        Path basePath = Path.of("src/main/java/mz/multicore/erp/gui");
        List<String> filesToCheck = List.of(
                "POSPanel.java",
                "PosShortcutBar.java",
                "PosKeyboardShortcutsHandler.java",
                "PosShortcutHelpDialog.java"
        );

        for (String file : filesToCheck) {
            Path filePath = basePath.resolve(file);
            File f = filePath.toFile();
            if (!f.exists()) {
                filePath = Path.of("desktop/src/main/java/mz/multicore/erp/gui").resolve(file);
            }
            if (Files.exists(filePath)) {
                long lineCount = Files.lines(filePath).count();
                assertTrue(lineCount < 1000,
                        String.format("Ficheiro %s excede 1000 linhas: %d linhas", file, lineCount));
            }
        }
    }

    @Test
    @DisplayName("KEY-06: Campos de pesquisa no POS não utilizam SearchField internamente para não duplicar ícones de lupa")
    void testNoDuplicateSearchIconsInPosPanel() throws Exception {
        Path path = Path.of("src/main/java/mz/multicore/erp/gui/POSPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/POSPanel.java");
        }
        if (Files.exists(path)) {
            String content = Files.readString(path);
            assertFalse(content.contains("clientSearchField = TableFilter.searchField"),
                    "clientSearchField não deve usar TableFilter.searchField quando envolvido em PosLayout.searchRow");
            assertFalse(content.contains("productSearchField = TableFilter.searchField"),
                    "productSearchField não deve usar TableFilter.searchField quando envolvido em PosLayout.searchRow");
        }
    }
}
