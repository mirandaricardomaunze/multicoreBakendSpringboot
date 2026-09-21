package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado para o sistema de ícones (IC-01 a IC-06).
 */
public class IconSystemHarnessTest {

    @Test
    @DisplayName("IC-01: UIHelper.icon não lança excepção com códigos inválidos/nulos e devolve fallback")
    void testFallbackOnInvalidOrNullCode() {
        assertDoesNotThrow(() -> {
            Icon nullIcon = UIHelper.icon(null, 16);
            assertNotNull(nullIcon, "Ícone nulo deve devolver fallback");
            assertTrue(nullIcon.getIconWidth() > 0);
        });

        assertDoesNotThrow(() -> {
            Icon blankIcon = UIHelper.icon("   ", 16);
            assertNotNull(blankIcon, "Ícone em branco deve devolver fallback");
        });

        assertDoesNotThrow(() -> {
            Icon invalidIcon = UIHelper.icon("fas-codigo-inexistente-xyz", 16, Color.RED);
            assertNotNull(invalidIcon, "Código inexistente deve devolver fallback seguro sem crash");
            assertTrue(invalidIcon.getIconWidth() > 0);
        });
    }

    @Test
    @DisplayName("IC-02: Tokens canónicos de tamanho em UIHelper correspondem aos valores especificados")
    void testCanonicalSizeTokensExistAndMatch() {
        assertEquals(12, UIHelper.ICON_XS, "ICON_XS deve ser 12");
        assertEquals(14, UIHelper.ICON_SM, "ICON_SM deve ser 14");
        assertEquals(16, UIHelper.ICON_MD, "ICON_MD deve ser 16");
        assertEquals(20, UIHelper.ICON_LG, "ICON_LG deve ser 20");
        assertEquals(24, UIHelper.ICON_XL, "ICON_XL deve ser 24");
        assertEquals(48, UIHelper.ICON_HERO, "ICON_HERO deve ser 48");
    }

    @Test
    @DisplayName("IC-03: Criação de ícones válidos respeita o tamanho e renderização")
    void testIconCreationAndSizing() {
        Icon userIcon = UIHelper.icon("fas-users", UIHelper.ICON_MD);
        assertNotNull(userIcon);
        if (userIcon instanceof org.kordamp.ikonli.swing.FontIcon fi) {
            assertEquals(UIHelper.ICON_MD, fi.getIconSize());
        }
        assertTrue(userIcon.getIconWidth() >= UIHelper.ICON_MD);
        assertTrue(userIcon.getIconHeight() >= UIHelper.ICON_MD);

        Icon printIcon = UIHelper.icon("fas-print", UIHelper.ICON_XL, UIHelper.ACCENT_BLUE);
        assertNotNull(printIcon);
        if (printIcon instanceof org.kordamp.ikonli.swing.FontIcon fi) {
            assertEquals(UIHelper.ICON_XL, fi.getIconSize());
        }
        assertTrue(printIcon.getIconWidth() >= UIHelper.ICON_XL);
        assertTrue(printIcon.getIconHeight() >= UIHelper.ICON_XL);
    }

    @Test
    @DisplayName("IC-04: UIHelper.iconImage gera BufferedImage válido com dimensões solicitadas")
    void testIconImageGeneration() {
        Image img = UIHelper.iconImage("fas-building", 32, Color.WHITE);
        assertNotNull(img, "iconImage deve produzir imagem não nula");
        assertEquals(32, img.getWidth(null));
        assertEquals(32, img.getHeight(null));
    }

    @Test
    @DisplayName("IC-05: BadgedIcon compõe ícone base com contador e desenha sem erro")
    void testBadgedIconDimensionsAndBehavior() {
        Icon base = UIHelper.icon("fas-bell", UIHelper.ICON_MD);
        BadgedIcon badged = new BadgedIcon(base, 7, UIHelper.REJECTED_RED, Color.WHITE);

        assertEquals(7, badged.getBadgeCount());
        assertTrue(badged.getIconWidth() >= base.getIconWidth());
        assertEquals(base.getIconHeight(), badged.getIconHeight());

        BufferedImage canvas = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        assertDoesNotThrow(() -> badged.paintIcon(new JLabel(), g2, 0, 0));
        g2.dispose();

        // Helper badgedIcon
        Icon helperBadged = UIHelper.badgedIcon("fas-envelope", UIHelper.ICON_LG, UIHelper.TEXT_LIGHT, 99, UIHelper.ACCENT_BLUE);
        assertNotNull(helperBadged);
        assertTrue(helperBadged instanceof BadgedIcon);
    }

    @Test
    @DisplayName("IC-06: createIconButton gera botão acessível com tooltip e AccessibleName")
    void testIconButtonAccessibility() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        JButton btn = UIHelper.createIconButton("fas-sync-alt", UIHelper.ICON_SM, "Actualizar dados", () -> clicked.set(true));

        assertNotNull(btn);
        assertNotNull(btn.getIcon());
        assertEquals("Actualizar dados", btn.getToolTipText());
        assertEquals("Actualizar dados", btn.getAccessibleContext().getAccessibleName());

        btn.doClick();
        assertTrue(clicked.get(), "O clique no botão deve disparar a acção registada");
    }
}
