package mz.multicore.erp.gui.quality;

import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;

import java.awt.Component;

public final class QualityPrintHelper {

    private QualityPrintHelper() {}

    public static void printDeliverySheet(Component parent, QualityApiClient client, Long assignmentId, Long companyId) {
        if (client == null) return;
        UIHelper.runWithProgress(parent, "A gerar Termo de Entrega em PDF...", () -> {
            return client.getDeliverySheetPdf(assignmentId, companyId);
        }, pdf -> {
            PrintPreviewDialog.show(parent, pdf, "termo-entrega-" + assignmentId, "Termo de Entrega de Equipamento / EPI");
        }, err -> ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar Termo de Entrega: " + err.getMessage()));
    }
}
