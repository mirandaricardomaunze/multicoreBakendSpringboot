package mz.multicore.erp.desktop;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Arranque do desktop como <b>cliente-fino</b>.
 *
 * <p>Ao contrário do backend ({@code MulticoreApplication}), este contexto <b>não</b> carrega
 * base de dados, JPA nem os serviços/repositórios/controladores de {@code mz.multicore.erp.modules.*}.
 * Faz scan apenas da camada de UI ({@code mz.multicore.erp.gui}), dos clientes HTTP e sessão
 * ({@code mz.multicore.erp.desktop}) e do parser de balança ({@code mz.multicore.erp.modules.pos.scale}).
 * Toda a lógica de negócio vive no backend e é acedida por HTTP — ver {@code DesktopApiClient}
 * e o {@code baseUrl} configurado em {@code DESKTOP_API_BASE_URL}.
 *
 * <p>Consequência operacional: o desktop já <b>não precisa de acesso directo à base de dados</b>,
 * pelo que o PostgreSQL pode ficar acessível apenas ao backend.
 *
 * <p><b>Notas de desenho.</b> Usa {@code @Configuration} + {@code @EnableAutoConfiguration}
 * (e não {@code @SpringBootApplication}) para <b>não</b> ser um {@code @SpringBootConfiguration}
 * — assim não interfere na descoberta de contexto dos testes de integração do backend. E é
 * {@code @Profile("desktop")} para que, quando o {@code MulticoreApplication} faz scan de
 * {@code mz.multicore.erp} (que inclui este pacote), estas exclusões <b>não</b> vazem para o contexto
 * do backend; só ficam activas quando o perfil {@code desktop} está ligado (arranque do desktop).
 */
@Configuration
@Profile("desktop")
@EnableAutoConfiguration
@ComponentScan(basePackages = {
        "mz.multicore.erp.desktop",
        "mz.multicore.erp.gui",
        "mz.multicore.erp.modules.pos.scale"
})
public class DesktopApplication {

    public static void main(String[] args) {
        // Handler global para excepções silenciosas no EDT (Event Dispatch Thread).
        // Sem isto, erros no invokeLater são engolidos sem aparecer nos logs.
        System.setProperty("sun.awt.exception.handler", java.awt.EventQueue.class.getName());
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("[FATAL] Excepção não tratada em thread '" + thread.getName() + "':");
            throwable.printStackTrace(System.err);
            System.exit(1);
        });

        ConfigurableApplicationContext context = new SpringApplicationBuilder(DesktopApplication.class)
                .web(WebApplicationType.NONE)   // sem servidor embutido: a UI é cliente HTTP, não servidor
                .headless(false)
                .profiles("desktop")
                .run(args);

        new DesktopLauncher(context).launch();
    }
}
