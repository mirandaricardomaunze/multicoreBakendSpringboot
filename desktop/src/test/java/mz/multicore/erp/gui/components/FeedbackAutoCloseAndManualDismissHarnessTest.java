package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de teste para fecho automático temporizado e fecho manual de mensagens de feedback (Inline & Toast).
 */
class FeedbackAutoCloseAndManualDismissHarnessTest {

    @Test
    @DisplayName("FDB-01: InlineFeedback fecha automaticamente após expirar o temporizador")
    void testInlineFeedbackAutoClosesAfterTimeout() throws Exception {
        InlineFeedbackPanel panel = new InlineFeedbackPanel();
        SwingUtilities.invokeAndWait(() -> {
            panel.show(FeedbackType.SUCCESS, "Operação guardada com sucesso.", 60);
        });

        assertTrue(panel.isVisible(), "Mensagem de feedback deve aparecer inicialmente");
        assertNotNull(panel.getAutoCloseTimer(), "Temporizador de fecho automático deve estar ativo");

        // Aguardar o disparo do temporizador de fecho automático
        Thread.sleep(130);

        SwingUtilities.invokeAndWait(() -> {
            assertFalse(panel.isVisible(), "Mensagem de feedback deve fechar automaticamente após o tempo decorrido");
            assertNull(panel.getAutoCloseTimer(), "Temporizador deve ser limpo após fechar");
        });
    }

    @Test
    @DisplayName("FDB-02: Utilizador pode fechar manualmente a mensagem a qualquer momento")
    void testInlineFeedbackManualCloseByButton() throws Exception {
        InlineFeedbackPanel panel = new InlineFeedbackPanel();
        SwingUtilities.invokeAndWait(() -> {
            panel.show(FeedbackType.WARNING, "Atenção: limite de crédito quase atingido.");
        });

        assertTrue(panel.isVisible(), "Mensagem deve estar visível");
        assertNotNull(panel.getCloseButton(), "Botão de fechar deve existir");
        assertEquals("Fechar mensagem", panel.getCloseButton().getToolTipText());

        // Utilizador clica no botão de fechar manualmente
        SwingUtilities.invokeAndWait(() -> {
            panel.getCloseButton().doClick();
        });

        assertFalse(panel.isVisible(), "Mensagem deve fechar imediatamente após clique manual no botão fechar");
        assertNull(panel.getAutoCloseTimer(), "Temporizador deve ser cancelado");
    }

    @Test
    @DisplayName("FDB-03: Duração de fecho automático inteligente de acordo com o tipo de mensagem")
    void testDefaultDurationsByType() {
        assertEquals(InlineFeedbackPanel.DURATION_SUCCESS_MS, InlineFeedbackPanel.defaultDurationFor(FeedbackType.SUCCESS));
        assertEquals(InlineFeedbackPanel.DURATION_INFO_MS, InlineFeedbackPanel.defaultDurationFor(FeedbackType.INFO));
        assertEquals(InlineFeedbackPanel.DURATION_WARNING_MS, InlineFeedbackPanel.defaultDurationFor(FeedbackType.WARNING));
        assertEquals(InlineFeedbackPanel.DURATION_ERROR_MS, InlineFeedbackPanel.defaultDurationFor(FeedbackType.ERROR));
        assertTrue(InlineFeedbackPanel.defaultDurationFor(FeedbackType.ERROR) > InlineFeedbackPanel.defaultDurationFor(FeedbackType.SUCCESS),
                "Mensagens de erro devem durar mais tempo para garantir leitura completa");
    }

    @Test
    @DisplayName("FDB-04: Passar o rato por cima (hover) pausa o temporizador para não fechar enquanto o utilizador lê")
    void testHoverPausesAutoCloseTimer() throws Exception {
        InlineFeedbackPanel panel = new InlineFeedbackPanel();
        SwingUtilities.invokeAndWait(() -> {
            panel.show(FeedbackType.INFO, "Informação importante de auditoria.", 80);
        });

        assertTrue(panel.isVisible());
        assertNotNull(panel.getAutoCloseTimer());

        // Simular rato a entrar no painel
        SwingUtilities.invokeAndWait(() -> {
            var listeners = panel.getMouseListeners();
            for (var l : listeners) {
                l.mouseEntered(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 10, 10, 0, false));
            }
        });

        assertTrue(panel.isHovered(), "Estado hovered deve estar activo");
        assertFalse(panel.getAutoCloseTimer().isRunning(), "Temporizador deve pausar enquanto o utilizador tiver o rato sobre a mensagem");

        // Dormir mais tempo do que a duração inicial para provar que NÃO fecha enquanto o rato estiver sobre ela
        Thread.sleep(120);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(panel.isVisible(), "Mensagem NÃO deve fechar enquanto o rato estiver por cima");
        });
    }

    @Test
    @DisplayName("FDB-05: Possibilidade de desativar fecho automático para mensagens críticas permanentes")
    void testDisablingAutoCloseKeepsMessagePersistent() throws Exception {
        InlineFeedbackPanel panel = new InlineFeedbackPanel();
        panel.setAutoCloseEnabled(false);

        SwingUtilities.invokeAndWait(() -> {
            panel.show(FeedbackType.ERROR, "Erro crítico persistente de ligação.");
        });

        assertTrue(panel.isVisible());
        assertNull(panel.getAutoCloseTimer(), "Nenhum temporizador deve iniciar quando autoCloseEnabled for false");

        Thread.sleep(100);
        assertTrue(panel.isVisible(), "Mensagem persistente continua visível");

        // Fecha manualmente
        SwingUtilities.invokeAndWait(panel::clear);
        assertFalse(panel.isVisible());
    }

    @Test
    @DisplayName("FDB-06: Notificações Toast suportam fecho manual, ícone semântico e dismiss")
    void testToastContentAccessibilityAndDismissal() {
        AtomicBoolean dismissed = new AtomicBoolean(false);
        JPanel toastPanel = ToastManager.buildContent(FeedbackType.SUCCESS, "Artigo guardado com sucesso.", () -> dismissed.set(true));

        assertNotNull(toastPanel);
        assertThat(toastPanel.getAccessibleContext().getAccessibleName()).contains("Sucesso");

        // Simular clique de fecho
        var mouseListeners = toastPanel.getMouseListeners();
        assertTrue(mouseListeners.length > 0, "Deve conter listener de clique para dispensar");
        mouseListeners[0].mouseClicked(new MouseEvent(toastPanel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false));

        assertTrue(dismissed.get(), "Clique deve disparar o fecho da notificação toast");
    }
}
