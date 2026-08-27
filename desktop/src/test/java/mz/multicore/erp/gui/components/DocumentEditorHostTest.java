package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes da regra pura da guarda de alterações do {@link DocumentEditorHost}. A construção do
 * componente (conteúdo/toolbar) valida-se por compilação + verificação manual (DE-50..55). DE-01/02.
 */
class DocumentEditorHostTest {

    @Test // DE-01
    void confirmaDescarte_quandoHaAlteracoes() {
        assertTrue(DocumentEditorHost.shouldConfirmDiscard(true));
    }

    @Test // DE-02
    void naoConfirma_quandoNadaPorGravar() {
        assertFalse(DocumentEditorHost.shouldConfirmDiscard(false));
    }

    @Test // SCUI-01/02
    void atalhosGuardamEVoltamSemAlteracoes() {
        AtomicInteger saves = new AtomicInteger();
        AtomicInteger backs = new AtomicInteger();
        DocumentEditorHost host = new DocumentEditorHost("Documento", new JPanel(),
                saves::incrementAndGet, backs::incrementAndGet, () -> false);

        Action save = host.getActionMap().get("saveDocument");
        Action back = host.getActionMap().get("backToList");
        save.actionPerformed(new ActionEvent(host, ActionEvent.ACTION_PERFORMED, "save"));
        back.actionPerformed(new ActionEvent(host, ActionEvent.ACTION_PERFORMED, "back"));

        assertEquals(1, saves.get());
        assertEquals(1, backs.get());
    }
}
