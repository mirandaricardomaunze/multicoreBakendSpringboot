package com.phcpro;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.LoginDialog;
import com.phcpro.gui.MainFrame;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.awt.EventQueue;

@SpringBootApplication
@EnableJpaAuditing
public class MulticoreApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(MulticoreApplication.class)
                .headless(false)
                .run(args);

        EventQueue.invokeLater(() -> {
            com.phcpro.gui.components.UIHelper.initGlobalTheme();

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
