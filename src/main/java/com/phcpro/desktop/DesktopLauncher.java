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

    public DesktopLauncher(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public void launch() {
        EventQueue.invokeLater(() -> {
            UIHelper.initGlobalTheme();

            DesktopApiConfig apiConfig = DesktopApiConfig.from(context.getEnvironment());
            AuthApiClient authApiClient = new AuthApiClient(apiConfig);
            LoginDialog login = new LoginDialog(authApiClient);
            login.setVisible(true);

            DesktopSession session = login.getAuthenticatedSession();
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

            MainFrame mainFrame = context.getBean(MainFrame.class);
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
            mainFrame.setVisible(true);
        });
    }
}
