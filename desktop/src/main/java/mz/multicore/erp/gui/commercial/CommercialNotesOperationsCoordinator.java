package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.DebitNoteDTO;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Coordenador de operações de aprovação, rejeição e impressão sensível de notas comerciais. */
public final class CommercialNotesOperationsCoordinator {

    private CommercialNotesOperationsCoordinator() {}

    public static void approveCredit(Component owner, CreditNoteDTO note, CreditNoteApiClient client,
                                     Runnable onDone, Consumer<Throwable> onError) {
        if (note == null) return;
        UIHelper.runWithProgress(owner, "A aprovar nota de crédito…", () -> client.approve(note.id()), approved -> {
            String msg = "Nota " + approved.noteNumber() + " aprovada.";
            if ("RETURN".equals(approved.reason())) msg += "\nStock devolvido ao armazém " + approved.warehouseName() + ".";
            ToastManager.success(owner, msg);
            onDone.run();
        }, onError);
    }

    public static void rejectCredit(Component owner, CreditNoteDTO note, CreditNoteApiClient client,
                                    Runnable onDone, Consumer<Throwable> onError) {
        if (note == null) return;
        String reason = UIHelper.promptRequiredText("Rejeitar Nota de Crédito", "fas-times-circle",
                "Indique o motivo da rejeição", "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A rejeitar nota de crédito…", () -> client.reject(note.id(), reason),
                ignored -> onDone.run(), onError);
    }

    public static void approveDebit(Component owner, DebitNoteDTO note, DebitNoteApiClient client,
                                    Runnable onDone, Consumer<Throwable> onError) {
        if (note == null) return;
        UIHelper.runWithProgress(owner, "A aprovar nota de débito…", () -> client.approve(note.id()), ignored -> {
            ToastManager.success(owner, "Nota " + note.noteNumber() + " aprovada.");
            onDone.run();
        }, onError);
    }

    public static void rejectDebit(Component owner, DebitNoteDTO note, DebitNoteApiClient client,
                                   Runnable onDone, Consumer<Throwable> onError) {
        if (note == null) return;
        String reason = UIHelper.promptRequiredText("Rejeitar Nota de Débito", "fas-times-circle",
                "Indique o motivo da rejeição", "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A rejeitar nota de débito…", () -> client.reject(note.id(), reason),
                ignored -> onDone.run(), onError);
    }

    public static void sensitivePrint(JComponent owner, PrintApiClient printApiClient, SensitiveDocumentType type,
                                      Long id, String label, String fileName, Callable<byte[]> renderer,
                                      Runnable refresh, Consumer<Throwable> onError) {
        if (printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar documento…", renderer,
                    pdf -> PrintPreviewDialog.show(owner, pdf, fileName), onError);
            return;
        }
        SensitivePrintCoordinator.print(owner, printApiClient, type, id, label, fileName, renderer, refresh);
    }

    public static <T> void applyPrintStatuses(List<T> notes, DefaultTableModel model, int printCol,
                                              List<DocumentPrintStatusDTO> statuses, Function<T, Long> idExtractor) {
        Map<Long, DocumentPrintStatusDTO> byId = statuses.stream()
                .collect(Collectors.toMap(DocumentPrintStatusDTO::documentId, s -> s));
        for (int row = 0; row < notes.size() && row < model.getRowCount(); row++) {
            Long id = idExtractor.apply(notes.get(row));
            DocumentPrintStatusDTO st = byId.get(id);
            if (st == null || st.printCount() == 0) {
                model.setValueAt("Não impressa", row, printCol);
            } else {
                String label = st.printCount() == 1 ? "1 via" : st.printCount() + " vias";
                model.setValueAt(label, row, printCol);
            }
        }
    }
}
