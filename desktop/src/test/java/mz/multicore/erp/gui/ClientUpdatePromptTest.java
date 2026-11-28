package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApiClientException;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientUpdatePromptTest {

    @Test
    void soOferecaDescargaQuandoOServidorPedeActualizacao() {
        assertTrue(ClientUpdatePrompt.isOutdated(new ApiClientException(426, "Actualize o programa")));
        assertFalse(ClientUpdatePrompt.isOutdated(new ApiClientException(400, "Senha errada")));
        assertFalse(ClientUpdatePrompt.isOutdated(new IllegalStateException("Autenticação cancelada.")));
    }

    @Test
    void instaladorVemDoProprioServidor() {
        assertEquals("https://erp.exemplo.co.mz/downloads/Multicore-instalador.exe",
                new DesktopApiConfig("https://erp.exemplo.co.mz/").installerUrl());
    }

    /** AU-07: the manifest address comes only from the configured server, never from a server response. */
    @Test
    void manifestoVemDoProprioServidor() {
        assertEquals("https://erp.exemplo.co.mz/downloads/Multicore-instalador.json",
                new DesktopApiConfig("https://erp.exemplo.co.mz/").manifestUrl());
    }
}
