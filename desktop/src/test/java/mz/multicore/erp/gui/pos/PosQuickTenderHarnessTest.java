package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.KeyBadge;
import mz.multicore.erp.gui.components.ModernButton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS: Pagamento Rápido no POS (Quick Tender) e Badges de Teclado")
class PosQuickTenderHarnessTest {

    @Test
    @DisplayName("QTH-01: Cálculo de troco exacto")
    void testExactChange() {
        BigDecimal total = new BigDecimal("1250.00");
        BigDecimal tendered = new BigDecimal("1250.00");
        BigDecimal change = tendered.subtract(total).setScale(2, RoundingMode.HALF_UP);

        assertEquals(BigDecimal.ZERO.setScale(2), change);
        assertTrue(change.compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    @DisplayName("QTH-02: Cálculo de troco com cédula superior")
    void testChangeWithHigherTender() {
        BigDecimal total = new BigDecimal("1250.00");
        BigDecimal tendered = new BigDecimal("1500.00");
        BigDecimal change = tendered.subtract(total).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("250.00"), change);
        assertTrue(change.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("QTH-03: Detecção de valor entregue insuficiente")
    void testInsufficientTender() {
        BigDecimal total = new BigDecimal("1250.00");
        BigDecimal tendered = new BigDecimal("1000.00");
        BigDecimal diff = tendered.subtract(total).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("-250.00"), diff);
        assertTrue(diff.compareTo(BigDecimal.ZERO) < 0);
        assertEquals(new BigDecimal("250.00"), diff.abs());
    }

    @Test
    @DisplayName("QTH-04: Componente KeyBadge renderiza sem excepção em modo headless")
    void testKeyBadgeRendering() {
        KeyBadge badge = KeyBadge.of("F9");
        assertEquals("F9", badge.getKeyText());
        assertNotNull(badge.getPreferredSize());
        assertTrue(badge.getPreferredSize().width >= 24);
        assertTrue(badge.getPreferredSize().height >= 18);

        badge.setSize(30, 20);
        BufferedImage img = new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertDoesNotThrow(() -> badge.paint(g2));
        g2.dispose();
    }

    @Test
    @DisplayName("QTH-05: ModernButton com suporte a tecla de atalho (setShortcut) e reserva estrita de margem sem sobreposição")
    void testModernButtonShortcut() {
        ModernButton btn = new ModernButton("Finalizar Venda");
        assertNull(btn.getShortcut());

        int initialRightInset = btn.getInsets().right;

        btn.setShortcut("F9");
        assertEquals("F9", btn.getShortcut());

        // Margem direita deve expandir para acomodar o badge [F9] sem sobrepor o texto
        int badgeWidth = btn.calculateShortcutBadgeWidth();
        assertTrue(badgeWidth > 0, "Largura do badge deve ser positiva");
        assertTrue(btn.getInsets().right >= initialRightInset + badgeWidth + 8,
                "Insets direitos devem reservar espaço para o badge + folga de 8px");

        Dimension pref = btn.getPreferredSize();
        assertNotNull(pref);
        assertTrue(pref.width >= 100);

        btn.setSize(pref);
        BufferedImage img = new BufferedImage(Math.max(pref.width, 10), Math.max(pref.height, 10), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertDoesNotThrow(() -> btn.paint(g2));
        g2.dispose();

        // Validar especificamente o botão "Remover" com atalho "Del"
        ModernButton removeBtn = new ModernButton("Remover");
        removeBtn.setShortcut("Del");
        assertEquals("Del", removeBtn.getShortcut());
        int removeBadgeW = removeBtn.calculateShortcutBadgeWidth();
        assertTrue(removeBtn.getInsets().right >= removeBtn.getBaseRightInset() + removeBadgeW + 8);
        assertTrue(removeBtn.getPreferredSize().width >= 110);

        BufferedImage imgRemove = new BufferedImage(removeBtn.getPreferredSize().width, removeBtn.getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gRemove = imgRemove.createGraphics();
        assertDoesNotThrow(() -> removeBtn.paint(gRemove));
        gRemove.dispose();
    }

    @Test
    @DisplayName("QTH-06: Decomposição de Linhas: POSPanel.java <= 1000 linhas")
    void testFileDecompositionLimit() throws Exception {
        Path posPanelPath = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "POSPanel.java");
        long lines = Files.lines(posPanelPath).count();
        assertTrue(lines <= 1000, "POSPanel.java tem " + lines + " linhas; o limite estrito é <= 1000 linhas");
    }
}
