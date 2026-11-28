package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.update.ClientUpdateService;
import mz.multicore.erp.desktop.update.UpdateEligibility;
import mz.multicore.erp.desktop.update.UpdateException;
import mz.multicore.erp.desktop.update.UpdateManifest;
import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * The one-click update as the operator sees it (docs/ACTUALIZACAO_UM_CLIQUE_SPEC.md §4): check, confirm,
 * download with progress, then "Reiniciar e instalar agora" or "Instalar ao fechar".
 */
public final class ClientUpdateFlow {

    /** What the screen that offers the update knows about this workstation. */
    public record Context(boolean serverRefusedVersion, BooleanSupplier tillSessionOpen, IntSupplier suspendedSales,
                          Runnable exitNow, Consumer<String> scheduledAtClose) {
    }

    private ClientUpdateFlow() {
    }

    public static void start(Window owner, ClientUpdateService service, Context context) {
        UIHelper.runWithProgress(owner, "A verificar a actualização…", () -> {
            UpdateManifest manifest = service.fetchManifest();
            boolean installed = ClientUpdateService.installedAppExe().isPresent();
            // Only ask the server about the till when everything before it already passed.
            UpdateEligibility.Workstation probe = new UpdateEligibility.Workstation(installed,
                    ClientUpdateService.installedVersion(), context.serverRefusedVersion(), false, 0);
            String refusal = UpdateEligibility.refusal(probe, manifest);
            if (refusal == null && !context.serverRefusedVersion()) {
                refusal = UpdateEligibility.refusal(new UpdateEligibility.Workstation(installed,
                        ClientUpdateService.installedVersion(), false, context.tillSessionOpen().getAsBoolean(),
                        context.suspendedSales().getAsInt()), manifest);
            }
            if (refusal != null) throw new UpdateException(refusal);
            return manifest;
        }, manifest -> confirmAndDownload(owner, service, manifest, context),
                error -> refuse(owner, error));
    }

    private static void confirmAndDownload(Window owner, ClientUpdateService service, UpdateManifest manifest,
                                           Context context) {
        String megabytes = String.format("%.0f MB", manifest.size() / (1024.0 * 1024.0));
        boolean go = ModernMessageDialog.confirm(owner, FeedbackType.INFO, "Actualizar o programa",
                "Versão instalada: " + ClientUpdateService.installedVersion() + "\nVersão nova: " + manifest.version()
                        + " (" + megabytes + ")\n\nO instalador é descarregado e verificado agora. Depois escolhe se "
                        + "instala já ou quando fechar o programa. Os dados ficam no servidor e não se perdem.",
                "Descarregar");
        if (!go) return;
        download(owner, service, manifest, installer -> chooseWhen(owner, service, manifest, installer, context));
    }

    private static void chooseWhen(Window owner, ClientUpdateService service, UpdateManifest manifest, Path installer,
                                   Context context) {
        int choice = ModernMessageDialog.choose(owner, FeedbackType.SUCCESS, "Actualização pronta",
                "O instalador da versão " + manifest.version() + " foi descarregado e verificado.\n\n"
                        + "Reiniciar agora fecha o programa, instala e volta a abri-lo (cerca de um minuto).\n"
                        + "Instalar ao fechar espera que saia do programa no fim do trabalho.",
                "Instalar ao fechar", "Reiniciar e instalar agora");
        if (choice < 0) return;
        boolean now = choice == 1;
        try {
            service.scheduleInstall(installer, manifest, now);
        } catch (UpdateException error) {
            refuse(owner, error);
            return;
        }
        if (now) {
            context.exitNow().run();
        } else {
            context.scheduledAtClose().accept(manifest.version());
        }
    }

    private static void download(Window owner, ClientUpdateService service, UpdateManifest manifest,
                                 Consumer<Path> onReady) {
        JDialog dialog = new JDialog(owner, "Actualizar o programa", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JLabel label = new JLabel("A descarregar…");
        label.setForeground(UIHelper.TEXT_LIGHT);
        label.setFont(Typography.BODY);
        JProgressBar bar = new JProgressBar(0, 1000);
        bar.setBorderPainted(false);
        bar.setBackground(UIHelper.BG_DARK);
        bar.setForeground(UIHelper.ACCENT);
        bar.setPreferredSize(new Dimension(380, 8));
        JPanel body = new JPanel(new BorderLayout(0, Spacing.MD));
        body.setOpaque(false);
        body.add(label, BorderLayout.NORTH);
        body.add(bar, BorderLayout.CENTER);
        AtomicBoolean cancelled = new AtomicBoolean();
        JButton cancel = ButtonStyles.cancelButton();
        cancel.addActionListener(e -> {
            cancelled.set(true);
            cancel.setEnabled(false);
            label.setText("A cancelar…");
        });
        dialog.setContentPane(DialogChrome.frame("fas-arrow-circle-up", "Actualizar o programa",
                "Versão " + manifest.version() + " — descarga e verificação do instalador", body,
                DialogChrome.footer(cancel)));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        SwingWorker<Path, Long> worker = new SwingWorker<>() {
            @Override protected Path doInBackground() {
                return service.downloadAndVerify(manifest, this::publish, cancelled::get);
            }

            @Override protected void process(List<Long> received) {
                if (cancelled.get()) return;
                long last = received.get(received.size() - 1);
                bar.setValue((int) (last * 1000 / manifest.size()));
                label.setText(last >= manifest.size() ? "A verificar o instalador…"
                        : String.format("A descarregar… %d de %d MB", last >> 20, manifest.size() >> 20));
            }

            @Override protected void done() {
                dialog.dispose();
                try {
                    onReady.accept(get());
                } catch (Exception error) {
                    if (!cancelled.get()) refuse(owner, error.getCause() != null ? error.getCause() : error);
                }
            }
        };
        worker.execute();
        // Same guard as UIHelper.runWithProgress: a task that ends before the modal opens must still close it.
        Timer watchdog = new Timer(200, e -> {
            if (worker.isDone() && dialog.isVisible()) {
                ((Timer) e.getSource()).stop();
                dialog.dispose();
            }
        });
        watchdog.start();
        dialog.setVisible(true);
        watchdog.stop();
    }

    private static void refuse(Window owner, Throwable error) {
        String message = error instanceof UpdateException ? error.getMessage()
                : "Não foi possível actualizar o programa agora. Tente mais tarde.";
        ModernMessageDialog.show(owner, FeedbackType.WARNING, "Actualização do programa", message);
    }
}
