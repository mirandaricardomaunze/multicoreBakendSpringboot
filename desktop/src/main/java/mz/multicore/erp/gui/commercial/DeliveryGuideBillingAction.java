package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.UIHelper;

import java.awt.Component;

/** Emite a fatura de uma guia aprovada, sem voltar a mover stock. */
final class DeliveryGuideBillingAction {

    private DeliveryGuideBillingAction() {}

    static void bill(Component parent, Long id, String number, ComercialApiClient apiClient,
                     InlineFeedbackPanel feedback, Runnable onSuccess) {
        if (!UIHelper.confirm(parent, "Emitir a fatura da guia " + number
                + "?\n\nO stock não volta a sair: já saiu na aprovação da guia.", "Faturar Guia")) return;
        UIHelper.runWithProgress(parent, "A faturar guia…", () -> apiClient.billDeliveryGuide(id), invoice -> {
            feedback.show(FeedbackType.SUCCESS, "Guia faturada", "Fatura " + invoice.invoiceNumber() + " emitida.", null, null);
            onSuccess.run();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível faturar a guia", error.getMessage(), null, null));
    }
}
