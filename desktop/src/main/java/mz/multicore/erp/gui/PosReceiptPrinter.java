package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.PosDirectPrintEngine;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.*;

/**
 * Utilitário para fluxo de impressão térmica de recibos no POS.
 */
public final class PosReceiptPrinter {
    private PosReceiptPrinter() {}

    public static void printReceiptIfConfirmed(POSPanel owner, InvoiceDTO invoice) {
        if (PosDirectPrintEngine.isDirectPrintEnabled()) {
            UIHelper.runWithProgress(owner, "A imprimir talão",
                    () -> owner.posApiClient.renderReceipt(invoice.id()),
                    pdf -> PosDirectPrintEngine.printReceiptSilent(pdf, "recibo-" + invoice.invoiceNumber(), owner),
                    ex -> owner.showPosNotice(FeedbackType.ERROR, "Falha na impressão direta", ex.getMessage()));
            return;
        }
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Imprimir recibo", "Deseja imprimir o recibo da venda " + invoice.invoiceNumber() + "?",
                "Imprimir")) return;
        UIHelper.runWithProgress(owner, "A gerar recibo",
                () -> owner.posApiClient.renderReceipt(invoice.id()),
                pdf -> PrintPreviewDialog.show(owner, pdf, "recibo-" + invoice.invoiceNumber()),
                ex -> owner.showPosNotice(FeedbackType.ERROR, "Não foi possível imprimir o recibo", ex.getMessage()));
    }
}
