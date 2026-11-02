package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JComponent;

/** Repõe a dupla autenticação de quem perdeu o telemóvel. */
final class PlatformTwoFactorAction {

    private PlatformTwoFactorAction() {}

    static void reset(JComponent parent, PlatformApiClient client, String username) {
        if (!UIHelper.confirm(parent, "Repor a dupla autenticação de " + username
                + "?\n\nConfirme primeiro a identidade da pessoa.", "Repor Dupla Autenticação")) return;
        UIHelper.runWithProgress(parent, "A repor…", () -> {
            client.resetTwoFactor(username);
            return null;
        }, ignored -> ToastManager.success(parent, "Dupla autenticação reposta."),
                error -> ToastManager.show(parent, FeedbackType.ERROR, error.getMessage()));
    }
}
