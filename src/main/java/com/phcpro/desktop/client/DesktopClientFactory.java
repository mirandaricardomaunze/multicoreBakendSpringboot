package com.phcpro.desktop.client;

import com.phcpro.desktop.config.DesktopApiConfig;
import com.phcpro.desktop.session.DesktopSessionStore;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopClientFactory {

    private final DesktopApiConfig config;
    private final DesktopSessionStore sessionStore;

    public DesktopClientFactory(Environment environment, DesktopSessionStore sessionStore) {
        this.config = DesktopApiConfig.from(environment);
        this.sessionStore = sessionStore;
    }

    public DesktopApiClient authenticatedClient() {
        return new DesktopApiClient(config, sessionStore.requireSession());
    }

    /**
     * Cliente sem sessão, para os poucos caminhos públicos ({@code /api/version}).
     * Existe para se poder perguntar a versão <b>antes</b> do login — e sem rebentar com o
     * {@code requireSession()}, que lança quando ainda não há sessão nenhuma.
     */
    public DesktopApiClient anonymousClient() {
        return new DesktopApiClient(config, null);
    }
}
