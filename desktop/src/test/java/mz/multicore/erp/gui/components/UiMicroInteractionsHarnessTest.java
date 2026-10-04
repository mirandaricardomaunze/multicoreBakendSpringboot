package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Micro-Interações e Transições Visuais")
class UiMicroInteractionsHarnessTest {

    @Test
    @DisplayName("Garante interpolação matemática precisa no blendColors")
    void testColorBlendingMathematics() {
        Color black = new Color(0, 0, 0, 255);
        Color white = new Color(200, 200, 200, 255);

        Color start = UIHelper.blendColors(black, white, 0.0f);
        assertEquals(black.getRed(), start.getRed());
        assertEquals(black.getGreen(), start.getGreen());
        assertEquals(black.getBlue(), start.getBlue());

        Color end = UIHelper.blendColors(black, white, 1.0f);
        assertEquals(white.getRed(), end.getRed());
        assertEquals(white.getGreen(), end.getGreen());
        assertEquals(white.getBlue(), end.getBlue());

        Color mid = UIHelper.blendColors(black, white, 0.5f);
        assertEquals(100, mid.getRed());
        assertEquals(100, mid.getGreen());
        assertEquals(100, mid.getBlue());

        // Clamping seguro
        Color belowZero = UIHelper.blendColors(black, white, -0.5f);
        assertEquals(0, belowZero.getRed());

        Color aboveOne = UIHelper.blendColors(black, white, 1.5f);
        assertEquals(200, aboveOne.getRed());

        // Canal Alpha
        Color transparent = new Color(50, 50, 50, 0);
        Color opaque = new Color(50, 50, 50, 200);
        Color blendedAlpha = UIHelper.blendColors(transparent, opaque, 0.5f);
        assertEquals(100, blendedAlpha.getAlpha());
    }

    @Test
    @DisplayName("Garante transição suave de hover e ciclo de vida do ModernButton")
    void testModernButtonHoverMicroInteraction() {
        Color base = new Color(59, 130, 246);
        Color hover = new Color(37, 99, 235);
        ModernButton btn = new ModernButton("Finalizar Venda", base, hover);

        assertEquals(0.0f, btn.getHoverProgress(), 0.001f);

        // Disparo de hover em headless mode (avança de forma síncrona/segura)
        btn.animateHover(true);
        assertEquals(1.0f, btn.getHoverProgress(), 0.001f);
        assertEquals(hover.getRGB(), btn.getBackground().getRGB());

        btn.animateHover(false);
        assertEquals(0.0f, btn.getHoverProgress(), 0.001f);
        assertEquals(base.getRGB(), btn.getBackground().getRGB());

        // Limpeza de recursos em removeNotify
        assertDoesNotThrow(btn::removeNotify);
    }

    @Test
    @DisplayName("Garante protecção anti-reticências e dimensões tácteis no ModernButton")
    void testModernButtonTactileFeelAndEllipsisProtection() {
        ModernButton btn = new ModernButton("Guardar Alterações...");
        assertEquals("Guardar Alterações", btn.getText());

        btn.setText("Processar Documento…");
        assertEquals("Processar Documento", btn.getText());

        Dimension pref = btn.getPreferredSize();
        assertTrue(pref.width >= 76, "Largura mínima deve garantir folga para não truncar");
        assertTrue(pref.height >= UIHelper.FORM_CONTROL_HEIGHT, "Altura deve coincidir com os campos de formulário");

        Dimension min = btn.getMinimumSize();
        assertTrue(min.width >= pref.width, "Tamanho mínimo não deve permitir colapso");
    }

    @Test
    @DisplayName("Valida geometria, ícone e contraste dos Pill Badges (StatusBadge)")
    void testStatusBadgePillGeometricsAndColors() {
        StatusBadge successBadge = StatusBadge.success("Pago");
        assertNotNull(successBadge.getIcon(), "Badge de sucesso deve incluir ícone vetorial");
        assertEquals("PAGO", successBadge.getText());
        assertTrue(successBadge.getPreferredSize().height >= 22, "Pílula deve ter altura ergonómica");

        StatusBadge warnBadge = StatusBadge.warning("Pendente");
        assertNotNull(warnBadge.getIcon());
        assertEquals("PENDENTE", warnBadge.getText());

        StatusBadge dangerBadge = StatusBadge.danger("Cancelado");
        assertNotNull(dangerBadge.getIcon());
        assertEquals("CANCELADO", dangerBadge.getText());

        StatusBadge neutralBadge = StatusBadge.neutral("Arquivado");
        assertNull(neutralBadge.getIcon());
        assertEquals("ARQUIVADO", neutralBadge.getText());
    }

    @Test
    @DisplayName("Valida montagem de conteúdo e dismiss do ToastManager")
    void testToastManagerContentAndDismissal() {
        AtomicBoolean dismissed = new AtomicBoolean(false);
        JPanel toastCard = ToastManager.buildContent(FeedbackType.SUCCESS, "Fatura emitida com sucesso!", () -> dismissed.set(true));

        assertNotNull(toastCard);
        assertTrue(toastCard instanceof ModernPanel);
        assertEquals("Sucesso: Fatura emitida com sucesso!", toastCard.getAccessibleContext().getAccessibleName());

        // Simula clique no botão de fechar / cartão
        dismissed.set(false);
        toastCard.getMouseListeners()[0].mouseClicked(null);
        assertTrue(dismissed.get(), "Clique no toast deve accionar o callback de dismiss suave");
    }
}
