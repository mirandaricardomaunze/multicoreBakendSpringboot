package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Coordenador operacional para pagamento, emissão de documentos sensíveis e Quick Peek de rescisões.
 */
public final class HRTerminationsOperationsCoordinator {

    private HRTerminationsOperationsCoordinator() {}

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int row) {
        if (model == null || row < 0 || row >= model.getRowCount()) return;
        peek.setHeaderIcon("fas-user-minus", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(model.getValueAt(row, 2)));
        peek.setSubtitle("Acerto " + model.getValueAt(row, 1) + " • " + model.getValueAt(row, 4));
        peek.setStatus(String.valueOf(model.getValueAt(row, 9)), UIHelper.ACCENT_BLUE);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Data de Saída", String.valueOf(model.getValueAt(row, 3)), false));
        items.add(new QuickPeekPanel.PeekItem("Motivo", String.valueOf(model.getValueAt(row, 4)), false));
        items.add(new QuickPeekPanel.PeekItem("Aviso Prévio", String.valueOf(model.getValueAt(row, 5)), false));
        items.add(new QuickPeekPanel.PeekItem("Total de Ganhos", String.valueOf(model.getValueAt(row, 6)), false));
        items.add(new QuickPeekPanel.PeekItem("Total de Descontos", String.valueOf(model.getValueAt(row, 7)), false));
        items.add(new QuickPeekPanel.PeekItem("Valor Líquido", String.valueOf(model.getValueAt(row, 8)), true));
        peek.setItems(items);
    }

    public static void paySettlement(HRPanel owner, TerminationDTO sel, Runnable reloadCallback) {
        if (sel == null) return;
        String msg = String.format("Pagar o acerto %s a %s, no valor de %,.2f MT?",
                sel.settlementNumber(), sel.employeeName(), sel.netAmount());
        boolean confirmed = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO, "Pagar Acerto Final", msg, "Pagar");
        if (!confirmed) return;

        UIHelper.runWithProgress(owner, "A pagar o acerto…",
                () -> owner.hrApiClient.paySettlement(sel.id()),
                ignored -> { if (reloadCallback != null) reloadCallback.run(); }, owner::showActionError);
    }

    public static void printSettlement(HRPanel owner, TerminationDTO sel, Runnable reloadCallback) {
        if (sel == null) return;
        sensitivePrint(owner, SensitiveDocumentType.TERMINATION_SETTLEMENT,
                sel.id(), "Acerto final " + sel.settlementNumber(),
                "acerto-final-" + sel.settlementNumber(),
                () -> owner.hrApiClient.renderSettlement(sel.id()), reloadCallback);
    }

    public static void printCertificate(HRPanel owner, TerminationDTO sel, Runnable reloadCallback) {
        if (sel == null) return;
        sensitivePrint(owner, SensitiveDocumentType.TERMINATION_CERTIFICATE,
                sel.id(), "Certificado de trabalho " + sel.settlementNumber(),
                "certificado-" + sel.settlementNumber(),
                () -> owner.hrApiClient.renderWorkCertificate(sel.id()), reloadCallback);
    }

    private static void sensitivePrint(HRPanel owner, SensitiveDocumentType type,
                                      Long id, String label, String fileName,
                                      Callable<byte[]> renderer, Runnable reloadCallback) {
        if (owner.printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar documento…", renderer,
                    pdf -> PrintPreviewDialog.show(owner, pdf, fileName), owner::showActionError);
            return;
        }
        SensitivePrintCoordinator.print(owner, owner.printApiClient, type, id, label, fileName,
                renderer, reloadCallback);
    }
}
