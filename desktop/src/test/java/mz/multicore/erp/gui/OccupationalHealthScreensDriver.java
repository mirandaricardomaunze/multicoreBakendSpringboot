package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.DesktopApplication;
import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * <b>Driver</b> dos ecrãs de saúde ocupacional, que vivem em <i>diálogos</i> e por isso escapam ao
 * {@link HRPanelScreenshotDriver} — esse fotografa separadores.
 *
 * <p>Mesma razão de ser: um diálogo que compila pode falhar à primeira pintura por um índice de
 * coluna errado, um modelo nulo ou um valor por formatar, e nada disso aparece no compilador nem na
 * suite. Estes três ecrãs nasceram com oito colunas onde antes havia seis, uma combobox de
 * prestadores e dois separadores internos — precisamente o tipo de mudança que rebenta ao pintar.
 *
 * <p>Percorre o caminho verdadeiro: entra por HTTP, monta o {@link HRPanel} real e chama a acção
 * como o utilizador a chama. Os diálogos são modais, pelo que se dispara na EDT e se fotografa de
 * fora — pintando a janela para uma imagem, sem depender do que está à frente no ecrã.
 *
 * <pre>
 * java -cp "desktop/target/classes;desktop/target/test-classes;$(cat target/cp.txt)" \
 *      mz.multicore.erp.gui.OccupationalHealthScreensDriver &lt;pasta&gt; [empresa]
 * </pre>
 */
public final class OccupationalHealthScreensDriver {

    private OccupationalHealthScreensDriver() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/oh-screenshots");
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
        List<EmployeeDTO> employees = hr.getAllEmployees();
        if (employees.isEmpty()) {
            throw new IllegalStateException("Sem colaboradores na empresa " + company.name());
        }
        EmployeeDTO employee = employees.get(0);
        System.out.println("[driver] " + session.username() + " · " + company.name()
                + " · colaborador " + employee.name());

        HREmployeeActions[] actions = new HREmployeeActions[1];
        SwingUtilities.invokeAndWait(() -> {
            // O tema instala-se em UIManager, e é o DesktopLauncher que o faz no arranque real.
            // Sem esta linha o driver fotografa os diálogos com o Look&Feel de fábrica — fundo azul
            // claro — e faz parecer defeito do ecrã aquilo que é falta de arranque do driver.
            UIHelper.loadAndApplySavedTheme();
            JFrame frame = new JFrame("Driver Saúde Ocupacional");
            UIHelper.registerMainWindow(frame);
            // O driver fotografa ecrãs; não imprime. Sem cliente de impressão, o botão
            // "Exportar PDF" diz-lhe isso em vez de estoirar.
            HRPanel panel = new HRPanel(hr, null);
            frame.setContentPane(panel);
            frame.setSize(1382, 736);
            frame.setVisible(true);
            // NA EDT, e é obrigatório: o CurrentUserContext é um ThreadLocal, e o SignedInUser lê-o
            // na thread onde a acção corre. Populado na main, a EDT não vê nada, o papel sai vazio
            // e um ADMIN verdadeiro leva com "apenas gestores podem consultar" — falha que parece
            // de permissões e é de threading. Foi o que este driver apanhou à primeira tentativa.
            CurrentUserContext.setCurrentUser(session.username(), company.role());
            CurrentUserContext.setCurrentCompanyId(company.id());
            actions[0] = new HREmployeeActions(panel, () -> employee, () -> null, () -> {});
        });

        List<OccupationalHealthExamDTO> history = hr.getOccupationalHealthHistory(employee.id());

        shootDialog(out, "01-historico", () -> actions[0].openOccupationalHealth(), 0);
        shootDialog(out, "02-formulario", () -> actions[0].openOccupationalHealthForm(employee), 0);
        // Dois separadores dentro do mesmo diálogo: "Sem exame" e "Custos do ano". Fotografar só o
        // primeiro deixava o segundo por ver, que é exactamente como um ecrã passa despercebido.
        shootDialog(out, "03-conformidade-sem-exame", () -> actions[0].openOccupationalHealthCompliance(), 0);
        shootDialog(out, "04-conformidade-custos", () -> actions[0].openOccupationalHealthCompliance(), 1);
        if (!history.isEmpty()) {
            shootDialog(out, "05-pagamento",
                    () -> actions[0].payOccupationalHealthExam(employee, history, 0), 0);
        }

        System.out.println("[driver] PNGs em " + out.toAbsolutePath());
        context.close();
        System.exit(0);
    }

    /**
     * Dispara uma acção que abre um diálogo modal e fotografa-o.
     *
     * <p>O modal bloqueia quem o abre, por isso vai em {@code invokeLater}: a EDT arranca um ciclo
     * de eventos secundário e continua a responder, o que permite pintá-lo a seguir. A pausa existe
     * porque o carregamento é assíncrono — sem ela fotografava-se a barra de progresso e o driver
     * dizia que estava tudo bem.
     */
    /** O maior diálogo visível — a barra de progresso é pequena e não pode ganhar a escolha. */
    private static Window largestDialog() {
        Window found = null;
        for (Window window : Window.getWindows()) {
            boolean candidate = window.isVisible() && window instanceof java.awt.Dialog;
            if (candidate && (found == null || window.getWidth() * window.getHeight()
                    > found.getWidth() * found.getHeight())) {
                found = window;
            }
        }
        return found;
    }

    private static void selectTab(java.awt.Container container, int index) {
        if (container == null) {
            return;
        }
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof javax.swing.JTabbedPane tabs && tabs.getTabCount() > index) {
                tabs.setSelectedIndex(index);
                return;
            }
            if (child instanceof java.awt.Container nested) {
                selectTab(nested, index);
            }
        }
    }

    private static void shootDialog(Path out, String name, Runnable open, int tab) throws Exception {
        SwingUtilities.invokeLater(open);
        Thread.sleep(6000);
        if (tab > 0) {
            SwingUtilities.invokeAndWait(() -> selectTab(largestDialog(), tab));
            Thread.sleep(600);
        }
        SwingUtilities.invokeAndWait(() -> {
            Window dialog = largestDialog();
            if (dialog == null) {
                System.out.println("[driver] FALHOU: nenhum diálogo visível para \"" + name + "\"");
                return;
            }
            try {
                BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                dialog.paint(image.getGraphics());
                File file = out.resolve(name + ".png").toFile();
                ImageIO.write(image, "png", file);
                System.out.println("[driver] " + name + " → " + file.getName()
                        + " (" + dialog.getWidth() + "x" + dialog.getHeight() + ")");
            } catch (Exception ex) {
                System.out.println("[driver] FALHOU a fotografar \"" + name + "\": " + ex);
                ex.printStackTrace(System.out);
            } finally {
                dialog.dispose();
            }
        });
    }
}
