package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.PosDirectPrintEngine;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.*;

/**
 * Utilitário para fluxo de impressão térmica de recibos no POS.
 */
public final class PosReceiptPrinter {
    private PosReceiptPrinter() {}

    public static void printReceiptIfConfirmed(POSPanel owner, InvoiceDTO invoice) {
        if (PosDirectPrintEngine.isDirectPrintEnabled()) {
            SensitivePrintCoordinator.print(owner, owner.printApiClient,
                    mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.RECEIPT,
                    invoice.id(), "Recibo da venda " + invoice.invoiceNumber(),
                    "recibo-" + invoice.invoiceNumber(),
                    () -> owner.posApiClient.renderReceipt(invoice.id()),
                    pdf -> PosDirectPrintEngine.printReceiptSilent(
                            pdf, "recibo-" + invoice.invoiceNumber(), owner), null);
            return;
        }
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Imprimir recibo", "Deseja imprimir o recibo da venda " + invoice.invoiceNumber() + "?",
                "Imprimir")) return;
        SensitivePrintCoordinator.print(owner, owner.printApiClient,
                mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.RECEIPT,
                invoice.id(), "Recibo da venda " + invoice.invoiceNumber(),
                "recibo-" + invoice.invoiceNumber(),
                () -> owner.posApiClient.renderReceipt(invoice.id()), null);
    }
}
