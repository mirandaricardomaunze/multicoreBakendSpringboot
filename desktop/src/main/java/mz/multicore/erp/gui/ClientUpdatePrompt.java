package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApiClientException;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.update.ClientUpdateService;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;

import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.net.URI;

/**
 * Quando o servidor recusa esta versão do programa (426), oferece a actualização de um clique no programa
 * instalado, ou a descarga pelo navegador quando corre a partir do jar.
 */
final class ClientUpdatePrompt {

    static final int UPGRADE_REQUIRED = 426;

    private ClientUpdatePrompt() {
    }

    static boolean isOutdated(Throwable error) {
        return error instanceof ApiClientException api && api.getStatusCode() == UPGRADE_REQUIRED;
    }

    static void offerIfOutdated(Window owner, Throwable error, DesktopApiConfig config) {
        if (!isOutdated(error)) return;
        if (ClientUpdateService.installedAppExe().isEmpty()) {
            boolean download = ModernMessageDialog.confirm(owner, FeedbackType.WARNING, "Actualização necessária",
                    error.getMessage() + "\n\nDescarregue o instalador, feche o programa e execute-o. "
                            + "Os dados ficam no servidor e não se perdem.", "Descarregar actualização");
            if (download) open(owner, config.installerUrl());
            return;
        }
        boolean update = ModernMessageDialog.confirm(owner, FeedbackType.WARNING, "Actualização necessária",
                error.getMessage() + "\n\nO programa pode actualizar-se sozinho. Os dados ficam no servidor e não "
                        + "se perdem.", "Actualizar agora");
        if (!update) return;
        // Refused by the server, this workstation cannot sell: no till or suspended sale to protect (spec §4.1).
        ClientUpdateFlow.start(owner, new ClientUpdateService(config), new ClientUpdateFlow.Context(true,
                () -> false, () -> 0,
                () -> owner.dispatchEvent(new WindowEvent(owner, WindowEvent.WINDOW_CLOSING)),
                version -> ModernMessageDialog.show(owner, FeedbackType.INFO, "Actualização agendada",
                        "A versão " + version + " será instalada quando fechar o programa.")));
    }

    private static void open(Window owner, String installerUrl) {
        try {
            Desktop.getDesktop().browse(URI.create(installerUrl));
        } catch (Exception ex) {
            ModernMessageDialog.show(owner, FeedbackType.INFO, "Descarregar actualização",
                    "Abra este endereço no navegador:\n" + installerUrl);
        }
    }
}
