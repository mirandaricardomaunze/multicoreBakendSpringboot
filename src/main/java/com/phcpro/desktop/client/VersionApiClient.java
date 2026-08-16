package com.phcpro.desktop.client;

import com.phcpro.architecture.version.ClientVersion;
import com.phcpro.architecture.version.SemanticVersion;
import com.phcpro.architecture.version.ServerVersionDTO;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Pergunta ao servidor qual é a versão dele.
 *
 * <p>{@code /api/version} é público, pelo que isto funciona antes do login e mesmo quando o
 * servidor já recusa este cliente por ser antigo — é a única porta que fica aberta a quem
 * precisa exactamente de saber que tem de actualizar.
 */
@Component
@Profile("desktop")
public class VersionApiClient {

    private final DesktopClientFactory clientFactory;

    public VersionApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ServerVersionDTO getServerVersion() {
        return clientFactory.anonymousClient().get("/api/version", ServerVersionDTO.class);
    }

    /**
     * Versão nova disponível no servidor, ou {@code null} se esta já está actualizada.
     *
     * <p><b>Nunca lança:</b> se o servidor não responder, ou responder algo estranho, não há
     * aviso nenhum — um problema a verificar a versão não pode impedir a loja de trabalhar.
     */
    public String newerVersionAvailable() {
        try {
            ServerVersionDTO server = getServerVersion();
            if (server == null || server.serverVersion() == null) return null;
            String mine = ClientVersion.current();
            return SemanticVersion.isOlderThan(mine, server.serverVersion())
                    ? server.serverVersion()
                    : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
