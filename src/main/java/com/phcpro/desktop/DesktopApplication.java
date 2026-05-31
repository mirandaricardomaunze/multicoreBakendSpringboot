package com.phcpro.desktop;

import com.phcpro.MulticoreApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class DesktopApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(MulticoreApplication.class)
                .headless(false)
                .profiles("desktop")
                .run(args);

        new DesktopLauncher(context).launch();
    }
}
