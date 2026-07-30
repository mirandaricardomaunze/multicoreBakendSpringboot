package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

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
}
