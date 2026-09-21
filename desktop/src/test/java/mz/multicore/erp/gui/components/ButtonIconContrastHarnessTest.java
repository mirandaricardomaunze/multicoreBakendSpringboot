package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado para validação de contraste de botões e ícones (BIC-01 a BIC-05).
 */
public class ButtonIconContrastHarnessTest {

    @Test
    @DisplayName("BIC-01: ModernButton com fundo de acção possui texto branco e ícone branco sincronizado")
    void testActionButtonIconAndTextAreWhite() {
        ModernButton btn = new ModernButton("Guardar", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
        btn.setIcon(UIHelper.icon("fas-check", 14));

        assertEquals(Color.WHITE, btn.getForeground(), "Texto em botão verde deve ser branco");
        Icon icon = btn.getIcon();
        assertTrue(icon instanceof FontIcon, "Ícone deve ser FontIcon");
        assertEquals(Color.WHITE, ((FontIcon) icon).getIconColor(), "Ícone em botão verde deve ser branco");
    }

    @Test
    @DisplayName("BIC-02: setColors com fundo claro sincroniza texto e ícone para cor escura de alto contraste")
    void testSetColorsSynchronizesIconWithText() {
        ModernButton btn = new ModernButton("Venda POS", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        btn.setIcon(UIHelper.icon("fas-cash-register", 14));

        // Transição para inactivo com fundo branco (Theme.LIGHT.card)
        btn.setColors(Theme.LIGHT.card, Theme.LIGHT.border);

        assertNotEquals(Color.WHITE, btn.getForeground(), "Texto sobre fundo claro deve ser escuro");
        Icon icon = btn.getIcon();
        assertTrue(icon instanceof FontIcon);
        assertEquals(btn.getForeground(), ((FontIcon) icon).getIconColor(), "Ícone deve acompanhar a cor escura do texto");

        // Transição de volta para activo
        btn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        assertEquals(Color.WHITE, btn.getForeground(), "Texto sobre azul deve voltar a branco");
        assertEquals(Color.WHITE, ((FontIcon) btn.getIcon()).getIconColor(), "Ícone sobre azul deve voltar a branco");
    }

    @Test
    @DisplayName("BIC-03: UIHelper.icon(code, size) devolve ícone branco por defeito para visibilidade em acções")
    void testDefaultIconColorIsWhite() {
        Icon ic = UIHelper.icon("fas-trash", 14);
        assertNotNull(ic);
        assertTrue(ic instanceof FontIcon);
        assertEquals(Color.WHITE, ((FontIcon) ic).getIconColor());
    }

    @Test
    @DisplayName("BIC-04: ActionMenuButton mantém o chevron com cor branca/alto contraste")
    void testActionMenuButtonChevronContrast() {
        ActionMenuButton menu = new ActionMenuButton("Documentos");
        assertEquals(Color.WHITE, menu.getForeground(), "Texto do menu de acções deve ser branco");
        Icon chevron = menu.getIcon();
        assertTrue(chevron instanceof FontIcon);
        assertEquals(Color.WHITE, ((FontIcon) chevron).getIconColor(), "Chevron do menu de acções deve ser branco");
    }

    @Test
    @DisplayName("BIC-05: Todos os botões auxiliares canónicos produzem texto e ícones brancos legíveis")
    void testAllHelperButtonsHaveWhiteIcons() {
        ModernButton[] buttons = new ModernButton[]{
                UIHelper.createPrimaryButton("Gravar"),
                UIHelper.createSuccessButton("Aprovar"),
                UIHelper.createDangerButton("Eliminar"),
                UIHelper.createWarningButton("Atenção"),
                UIHelper.createSecondaryButton("Voltar"),
                UIHelper.createRefreshButton(() -> {})
        };

        for (ModernButton btn : buttons) {
            btn.setIcon(UIHelper.icon("fas-star", 14));
            assertEquals(Color.WHITE, btn.getForeground(), "Texto do botão " + btn.getText() + " deve ser branco");
            assertTrue(btn.getIcon() instanceof FontIcon);
            assertEquals(Color.WHITE, ((FontIcon) btn.getIcon()).getIconColor(), "Ícone do botão " + btn.getText() + " deve ser branco");
        }
    }

    @Test
    @DisplayName("BIC-06: Botão de paginação do POS mantém texto e ícone legíveis mesmo desabilitado")
    void testPosPaginationButtonDisabledContrast() {
        ModernButton prev = new ModernButton("Anterior", UIHelper.BUTTON_NEUTRAL, UIHelper.BUTTON_NEUTRAL_HOVER);
        prev.setIcon(UIHelper.icon("fas-chevron-left", 12, Color.WHITE));
        prev.setEnabled(false);

        assertEquals(Color.WHITE, prev.getForeground(), "Texto base do botão neutral deve ser branco");
        assertTrue(prev.getIcon() instanceof FontIcon);
        FontIcon fi = (FontIcon) prev.getIcon();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 36, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        prev.setSize(100, 36);
        prev.paint(g2);
        g2.dispose();

        assertTrue(fi.getIconColor().getAlpha() > 0, "Ícone do botão desabilitado não pode ser invisível");
    }
}
