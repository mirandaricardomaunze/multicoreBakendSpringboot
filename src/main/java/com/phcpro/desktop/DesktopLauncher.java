package com.phcpro.desktop;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.LoginDialog;
import com.phcpro.gui.MainFrame;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.EventQueue;

public class DesktopLauncher {

    private final ConfigurableApplicationContext context;

    public DesktopLauncher(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public void launch() {
        EventQueue.invokeLater(() -> {
            UIHelper.initGlobalTheme();

            AppUserService userService = context.getBean(AppUserService.class);
            LoginDialog login = new LoginDialog(userService);
            login.setVisible(true);

            AppUser authenticated = login.getAuthenticatedUser();
            if (authenticated == null) {
                context.close();
                System.exit(0);
                return;
            }

            CurrentUserContext.setCurrentUser(authenticated.getName(), authenticated.getRole());

            MainFrame mainFrame = context.getBean(MainFrame.class);
            mainFrame.applyAuthenticatedUser(authenticated.getName(), authenticated.getRole());
            mainFrame.setVisible(true);
        });
    }
}
