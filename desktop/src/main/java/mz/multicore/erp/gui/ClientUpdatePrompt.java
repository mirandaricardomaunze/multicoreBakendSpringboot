package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApiClientException;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;

import java.awt.Desktop;
import java.awt.Window;
import java.net.URI;

/** Quando o servidor recusa esta versão do programa (426), oferece a descarga do instalador novo. */
final class ClientUpdatePrompt {

    static final int UPGRADE_REQUIRED = 426;

    private ClientUpdatePrompt() {
    }

    static boolean isOutdated(Throwable error) {
        return error instanceof ApiClientException api && api.getStatusCode() == UPGRADE_REQUIRED;
    }

    static void offerIfOutdated(Window owner, Throwable error, String installerUrl) {
        if (!isOutdated(error)) return;
        boolean download = ModernMessageDialog.confirm(owner, FeedbackType.WARNING, "Actualização necessária",
                error.getMessage() + "\n\nDescarregue o instalador, feche o programa e execute-o. "
                        + "Os dados ficam no servidor e não se perdem.", "Descarregar actualização");
        if (download) open(owner, installerUrl);
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
