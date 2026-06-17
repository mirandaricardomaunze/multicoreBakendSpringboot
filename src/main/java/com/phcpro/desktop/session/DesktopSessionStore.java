package com.phcpro.desktop.session;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopSessionStore {

    private DesktopSession session;

    public DesktopSession requireSession() {
        if (session == null) {
            throw new IllegalStateException("Não existe uma sessão autenticada no desktop.");
        }
        return session;
    }

    public void setSession(DesktopSession session) {
        this.session = session;
    }

    public void clear() {
        session = null;
    }
}
