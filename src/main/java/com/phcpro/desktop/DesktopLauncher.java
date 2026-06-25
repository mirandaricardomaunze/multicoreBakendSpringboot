package com.phcpro.desktop;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.client.AuthApiClient;
import com.phcpro.desktop.config.DesktopApiConfig;
import com.phcpro.desktop.session.DesktopSession;
import com.phcpro.desktop.session.DesktopSessionStore;
import com.phcpro.gui.LoginDialog;
import com.phcpro.gui.MainFrame;
import com.phcpro.gui.components.UIHelper;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DesktopLauncher {

    private final ConfigurableApplicationContext context;
    private AuthApiClient authApiClient;
    private DesktopSession session;
    private MainFrame currentFrame;

    public DesktopLauncher(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public void launch() {
        EventQueue.invokeLater(() -> {
            UIHelper.loadAndApplySavedTheme();

            DesktopApiConfig apiConfig = DesktopApiConfig.from(context.getEnvironment());
            authApiClient = new AuthApiClient(apiConfig);
            LoginDialog login = new LoginDialog(authApiClient);
            login.setVisible(true);

            session = login.getAuthenticatedSession();
            if (session == null) {
                context.close();
                System.exit(0);
                return;
            }

            if (session.companies().isEmpty()) {
                context.close();
                throw new IllegalStateException("O utilizador autenticado não possui acesso a nenhuma empresa.");
            }
            session.selectCompany(session.companies().get(0).id());
            CurrentUserContext.setCurrentUser(session.username(), session.activeRole());
            CurrentUserContext.setCurrentCompanyId(session.activeCompanyId());
            context.getBean(DesktopSessionStore.class).setSession(session);

            // Trocar de tema reconstrói a janela já com a paleta nova (cobre ícones/pintura custom).
            UIHelper.onThemeChanged = () -> EventQueue.invokeLater(this::rebuildMainFrame);

            // Arranque após login: janela maximizada (loja trabalha em ecrã cheio).
            showMainFrame(null, java.awt.Frame.MAXIMIZED_BOTH);
        });
    }

    private void rebuildMainFrame() {
        // Preserva tamanho/posição E estado (maximizado/normal) ao reconstruir por troca de tema.
        java.awt.Rectangle bounds = currentFrame != null ? currentFrame.getBounds() : null;
        int extendedState = currentFrame != null ? currentFrame.getExtendedState() : java.awt.Frame.MAXIMIZED_BOTH;
        if (currentFrame != null) {
            currentFrame.dispose(); // dispose programático → não dispara o logout (que é em windowClosing)
        }
        showMainFrame(bounds, extendedState);
    }

    private void showMainFrame(java.awt.Rectangle bounds, int extendedState) {
        MainFrame mainFrame = context.getBean(MainFrame.class); // prototype → instância nova com o tema activo
        mainFrame.applyAuthenticatedUser(session.displayName(), session.activeRole());
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                try {
                    authApiClient.logout(session);
                } finally {
                    context.getBean(DesktopSessionStore.class).clear();
                    CurrentUserContext.clear();
                    context.close();
                }
            }
        });
        if (bounds != null) {
            mainFrame.setBounds(bounds); // preserva tamanho/posição ao reconstruir
        }
        mainFrame.setExtendedState(extendedState); // maximizado no arranque; estado preservado na reconstrução
        currentFrame = mainFrame;
        mainFrame.setVisible(true);
    }
}
