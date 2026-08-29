package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.DesktopApplication;
import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.UIHelper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Fotografa <b>qualquer</b> painel da aplicação, por nome, contra um backend a sério.
 *
 * <p>Generaliza o {@link HRPanelScreenshotDriver}, que só sabia montar o RH. Os painéis recebem
 * clientes de API no construtor e esses clientes são beans — pelo que o driver resolve os
 * argumentos a partir do contexto do Spring, por tipo, e monta o painel sem precisar de saber nada
 * sobre ele. Um parâmetro sem bean (os opcionais, como o atalho de transferências) entra a
 * {@code null}, que é o que a própria aplicação faz quando o desliga.
 *
 * <p><b>Porquê generalizar:</b> o defeito do selector de "Por página" — que truncava o valor em
 * todas as tabelas paginadas do sistema — foi encontrado no RH por acaso, porque foi o único módulo
 * que alguém abriu. Os defeitos de apresentação não respeitam fronteiras de módulo; a ferramenta
 * para os ver também não devia.
 *
 * <pre>
 * java -cp ... PanelScreenshotDriver &lt;pasta&gt; &lt;empresa&gt; ComercialPanel POSPanel ComprasPanel
 * </pre>
 */
public final class PanelScreenshotDriver {

    private static final int WIDTH = 1382;
    private static final int HEIGHT = 736;

    private PanelScreenshotDriver() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/panels");
        //noinspection ResultOfMethodCallIgnored
        out.toFile().mkdirs();
        String wanted = args.length > 1 ? args[1] : null;

        ConfigurableApplicationContext context = new SpringApplicationBuilder(DesktopApplication.class)
                .web(WebApplicationType.NONE).headless(false).profiles("desktop").run();

        DesktopApiConfig config = DesktopApiConfig.from(context.getEnvironment());
        DesktopSession session = new AuthApiClient(config).login("ana", "password");
        DesktopSession.CompanyAccess company = session.companies().stream()
                .filter(c -> wanted == null || c.name().toLowerCase().contains(wanted.toLowerCase()))
                .findFirst().orElse(session.companies().get(0));
        session.selectCompany(company.id());
        context.getBean(DesktopSessionStore.class).setSession(session);
        System.out.println("[driver] " + session.username() + " · " + company.name()
                + " · perfil " + company.role());

        for (int i = 2; i < args.length; i++) {
            shootPanel(context, out, args[i], session, company);
        }

        System.out.println("[driver] PNGs em " + out.toAbsolutePath());
        context.close();
        System.exit(0);
    }

    private static void shootPanel(ConfigurableApplicationContext context, Path out,
                                   String simpleName, DesktopSession session,
                                   DesktopSession.CompanyAccess company) {
        try {
            Class<?> type = Class.forName("mz.multicore.erp.gui." + simpleName);
            Constructor<?> ctor = widestConstructor(type);
            Object[] argsForCtor = new Object[ctor.getParameterCount()];
            for (int i = 0; i < argsForCtor.length; i++) {
                Class<?> param = ctor.getParameterTypes()[i];
                try {
                    argsForCtor[i] = context.getBean(param);
                } catch (RuntimeException noBean) {
                    // Opcional: a aplicação também o desliga assim.
                    argsForCtor[i] = null;
                }
            }
            JPanel[] panel = new JPanel[1];
            JFrame[] window = new JFrame[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIHelper.loadAndApplySavedTheme();
                    // Tem de ser DENTRO da EDT: o CurrentUserContext é um ThreadLocal, e é nesta
                    // thread que o painel se constrói. Populá-lo na thread principal não chega —
                    // painéis que perguntam pela empresa activa ao construir (o Comercial e o POS)
                    // rebentavam com "Selecione uma empresa antes de continuar", e parecia defeito
                    // do painel. É a mesma razão por que o UIHelper recopia o contexto para dentro
                    // de cada worker de fundo.
                    CurrentUserContext.setCurrentUser(session.username(), company.role());
                    CurrentUserContext.setCurrentCompanyId(company.id());
                    window[0] = new JFrame("Driver " + simpleName);
                    UIHelper.registerMainWindow(window[0]);
                    panel[0] = (JPanel) ctor.newInstance(argsForCtor);
                    window[0].setContentPane(panel[0]);
                    window[0].setSize(WIDTH, HEIGHT);
                    window[0].setVisible(true);
                    selectIfPossible(panel[0]);
                } catch (Exception ex) {
                    // A causa é o que interessa; o InvocationTargetException por si não diz nada.
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    System.out.println("[driver] FALHOU a montar " + simpleName + ": " + cause);
                    for (StackTraceElement line : cause.getStackTrace()) {
                        if (line.getClassName().startsWith("mz.multicore")) {
                            System.out.println("[driver]     em " + line);
                            break;
                        }
                    }
                }
            });
            if (panel[0] == null) {
                return;
            }
            // O carregamento é assíncrono: fotografar já dava tabelas vazias e um falso "está bem".
            Thread.sleep(7000);

            SwingUtilities.invokeAndWait(() -> {
                // A janela criada acima, não Frame.getFrames()[0]: a partir do segundo painel esse
                // aponta para uma janela já fechada e o driver fotografava a anterior, ou nada.
                JFrame frame = window[0];
                JTabbedPane tabs = findTabs(frame.getContentPane());
                if (tabs == null) {
                    shoot(frame, out, simpleName, "unico");
                    return;
                }
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    tabs.setSelectedIndex(i);
                    frame.validate();
                    shoot(frame, out, simpleName, String.format("%02d-%s", i, tabs.getTitleAt(i)));
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.Window open : java.awt.Window.getWindows()) {
                    open.dispose();
                }
            });
        } catch (Exception ex) {
            System.out.println("[driver] FALHOU " + simpleName + ": " + ex);
        }
    }

    private static Constructor<?> widestConstructor(Class<?> type) {
        Constructor<?> widest = type.getConstructors()[0];
        for (Constructor<?> candidate : type.getConstructors()) {
            if (candidate.getParameterCount() > widest.getParameterCount()) {
                widest = candidate;
            }
        }
        return widest;
    }

    /** Os painéis carregam quando são seleccionados; sem isto fotografava-se tudo vazio. */
    private static void selectIfPossible(JPanel panel) {
        for (String name : new String[]{"onPanelSelected", "refresh", "reload"}) {
            try {
                Method method = panel.getClass().getMethod(name);
                method.invoke(panel);
                return;
            } catch (Exception ignored) {
                // o painel não tem esse gancho; segue para o próximo
            }
        }
    }

    private static void shoot(JFrame frame, Path out, String panel, String title) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            frame.paint(image.getGraphics());
            String safe = (panel + "-" + title).replaceAll("[^A-Za-z0-9]+", "-").toLowerCase();
            ImageIO.write(image, "png", out.resolve(safe + ".png").toFile());
            System.out.println("[driver] " + panel + " · " + title + " → " + safe + ".png");
        } catch (Exception ex) {
            // Falhar aqui é o resultado que interessa: o separador não pinta.
            System.out.println("[driver] FALHOU a fotografar " + panel + "/" + title + ": " + ex);
            ex.printStackTrace(System.out);
        }
    }

    private static JTabbedPane findTabs(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTabbedPane tabs) {
                return tabs;
            }
            if (child instanceof Container nested) {
                JTabbedPane found = findTabs(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
