package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.DesktopApplication;
import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Fotografa os <b>diálogos</b> do RH que vivem fora dos separadores: evolução salarial, documentos
 * do colaborador e justificação de faltas.
 *
 * <p>Irmão do {@link HRPanelScreenshotDriver}, que só cobre separadores, e do
 * {@code OccupationalHealthScreensDriver}. Existe porque estes ecrãs são <b>diálogos por
 * colaborador</b> — decisão do §B4 — e por isso nunca aparecem numa fotografia de separador. Foi
 * exactamente aí que se esconderam os defeitos de layout da saúde ocupacional.
 *
 * <p>Instala o tema como o {@code DesktopLauncher} faz. Sem isso, fotografa com o Look&amp;Feel de
 * fábrica e faz parecer defeito dos ecrãs o que é falta de arranque do driver.
 */
public final class HREmployeeDialogsDriver {

    private HREmployeeDialogsDriver() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/hr-dialogs");
        //noinspection ResultOfMethodCallIgnored
        out.toFile().mkdirs();

        ConfigurableApplicationContext context = new SpringApplicationBuilder(DesktopApplication.class)
                .web(WebApplicationType.NONE).headless(false).profiles("desktop").run();

        DesktopApiConfig config = DesktopApiConfig.from(context.getEnvironment());
        DesktopSession session = new AuthApiClient(config).login("ana", "password");
        String wanted = args.length > 1 ? args[1] : null;
        DesktopSession.CompanyAccess company = session.companies().stream()
                .filter(c -> wanted == null || c.name().toLowerCase().contains(wanted.toLowerCase()))
                .findFirst().orElse(session.companies().get(0));
        session.selectCompany(company.id());
        context.getBean(DesktopSessionStore.class).setSession(session);

        HRApiClient hr = context.getBean(HRApiClient.class);
        EmployeeDTO employee = hr.getAllEmployees().stream().findFirst().orElseThrow();
        System.out.println("[driver] " + company.name() + " · colaborador " + employee.name());

        HREmployeeActions[] actions = new HREmployeeActions[1];
        SwingUtilities.invokeAndWait(() -> {
            UIHelper.loadAndApplySavedTheme();
            JFrame frame = new JFrame("Driver Diálogos RH");
            UIHelper.registerMainWindow(frame);
            HRPanel panel = new HRPanel(hr);
            frame.setContentPane(panel);
            frame.setSize(1382, 736);
            frame.setVisible(true);
            actions[0] = new HREmployeeActions(panel, () -> employee, () -> null, () -> { });
        });
        Thread.sleep(3000);

        shoot(out, "01-evolucao-salarial", () -> actions[0].openSalaryHistory());
        shoot(out, "02-documentos", () -> actions[0].openDocuments());

        System.out.println("[driver] PNGs em " + out.toAbsolutePath());
        context.close();
        System.exit(0);
    }

    private static void shoot(Path out, String name, Runnable open) throws Exception {
        SwingUtilities.invokeLater(open);
        Thread.sleep(6000);
        SwingUtilities.invokeAndWait(() -> {
            Window dialog = null;
            for (Window window : Window.getWindows()) {
                boolean candidate = window.isVisible() && window instanceof java.awt.Dialog;
                if (candidate && (dialog == null || window.getWidth() * window.getHeight()
                        > dialog.getWidth() * dialog.getHeight())) {
                    dialog = window;
                }
            }
            if (dialog == null) {
                System.out.println("[driver] FALHOU: " + name + " não abriu diálogo nenhum");
                return;
            }
            try {
                BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                dialog.paint(image.getGraphics());
                ImageIO.write(image, "png", out.resolve(name + ".png").toFile());
                System.out.printf("[driver] %s → %s.png (%dx%d)%n",
                        name, name, dialog.getWidth(), dialog.getHeight());
            } catch (Exception ex) {
                System.out.println("[driver] FALHOU a fotografar " + name + ": " + ex);
            }
            dialog.setVisible(false);
            dialog.dispose();
        });
    }
}
