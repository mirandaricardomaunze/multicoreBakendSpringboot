package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.DesktopApplication;
import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.gui.components.UIHelper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * <b>Driver</b> que corre a UI real do RH contra um backend a sério e fotografa cada separador.
 *
 * <p>Não é um teste: é a forma de <i>ver</i> os ecrãs sem ter de clicar. Constrói o contexto do
 * desktop tal como o {@code DesktopApplication} o constrói, faz login por HTTP, monta o
 * {@link HRPanel} numa janela de 1382×736 — a resolução validada em 2026-08-15 — e escreve um PNG
 * por separador.
 *
 * <p><b>Porquê isto e não clicar:</b> um painel Swing que compila pode falhar de maneiras banais
 * (índice de coluna errado num renderer, modelo nulo, carregamento que rebenta) e nenhuma delas
 * aparece no compilador nem na suite. Aparecem à primeira pintura — que é exactamente o que este
 * driver força.
 *
 * <p>Correr com o backend de pé:
 * <pre>
 * java -cp "target/classes;target/test-classes;$(cat target/cp.txt)" \
 *      mz.multicore.erp.gui.HRPanelScreenshotDriver &lt;pasta-destino&gt;
 * </pre>
 */
public final class HRPanelScreenshotDriver {

    private static final int WIDTH = 1382;
    private static final int HEIGHT = 736;

    private HRPanelScreenshotDriver() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/hr-screenshots");
        //noinspection ResultOfMethodCallIgnored
        out.toFile().mkdirs();

        ConfigurableApplicationContext context = new SpringApplicationBuilder(DesktopApplication.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .profiles("desktop")
                .run();

        DesktopApiConfig config = DesktopApiConfig.from(context.getEnvironment());
        DesktopSession session = new AuthApiClient(config).login("ana", "password");
        // A ordem das empresas não é estável entre arranques: sem escolher pelo nome, o driver
        // fotografa tabelas vazias de outra empresa e parece que os ecrãs não carregam nada.
        String wanted = args.length > 1 ? args[1] : null;
        DesktopSession.CompanyAccess company = session.companies().stream()
                .filter(c -> wanted == null || c.name().toLowerCase().contains(wanted.toLowerCase()))
                .findFirst()
                .orElse(session.companies().get(0));
        session.selectCompany(company.id());
        context.getBean(DesktopSessionStore.class).setSession(session);
        System.out.println("[driver] sessão: " + session.username() + " · empresa: " + company.name());

        HRApiClient hrApiClient = context.getBean(HRApiClient.class);

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Driver RH");
            UIHelper.registerMainWindow(frame);
            HRPanel panel = new HRPanel(hrApiClient);
            frame.setContentPane(panel);
            frame.setSize(WIDTH, HEIGHT);
            frame.setVisible(true);
            panel.onPanelSelected();
        });

        // O carregamento é assíncrono (loadAsync/SwingWorker): sem esta pausa, fotografava-se a
        // tabela vazia e o driver dizia que estava tudo bem sem nada ter sido carregado.
        Thread.sleep(6000);

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = (JFrame) java.awt.Frame.getFrames()[0];
            JTabbedPane tabs = findTabs(frame.getContentPane());
            if (tabs == null) {
                throw new IllegalStateException("Não encontrei a barra de separadores do RH.");
            }
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setSelectedIndex(i);
                frame.validate();
                shoot(frame, out, i, tabs.getTitleAt(i));
            }
        });

        System.out.println("[driver] PNGs em " + out.toAbsolutePath());
        context.close();
        System.exit(0);
    }

    private static void shoot(JFrame frame, Path out, int index, String title) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            frame.paint(image.getGraphics());
            String safe = title.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase();
            File file = out.resolve(String.format("%02d-%s.png", index, safe)).toFile();
            ImageIO.write(image, "png", file);
            System.out.println("[driver] " + title + " → " + file.getName());
        } catch (Exception ex) {
            // Falhar aqui é o resultado que interessa: significa que o separador não pinta.
            System.out.println("[driver] FALHOU a fotografar \"" + title + "\": " + ex);
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
