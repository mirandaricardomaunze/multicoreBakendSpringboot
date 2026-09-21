package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.PrintOptions;
import mz.multicore.erp.gui.components.PrintOptions.Orientation;
import mz.multicore.erp.gui.components.PrintOptions.Fit;
import mz.multicore.erp.gui.components.ToastManager;

import java.awt.Component;

/**
 * Motor de impressão direta/silenciosa de talões térmicos no POS.
 */
public class PosDirectPrintEngine {

    private static boolean directPrintEnabled = false;

    public static boolean isDirectPrintEnabled() {
        return directPrintEnabled;
    }

    public static void setDirectPrintEnabled(boolean enabled) {
        directPrintEnabled = enabled;
    }

    /**
     * Envia o PDF para a impressora padrão do sistema sem exibir diálogo modal prévio.
     * Se falhar ou se não houver impressora, faz fallback seguro informando o operador.
     */
    public static boolean printReceiptSilent(byte[] pdfBytes, String jobName, Component parent) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            ToastManager.show(parent, FeedbackType.WARNING, "Documento de venda vazio para impressão.");
            return false;
        }

        try {
            String defaultPrinter = PdfPrinter.defaultPrinterName();
            PrintOptions options = new PrintOptions(
                    defaultPrinter,
                    1,
                    Orientation.PORTRAIT,
                    Fit.SHRINK_TO_FIT,
                    "",
                    false
            );

            PdfPrinter.print(pdfBytes, options, jobName == null ? "Talao-POS" : jobName);
            ToastManager.show(parent, FeedbackType.SUCCESS, "Talão de venda enviado para a impressora.");
            return true;
        } catch (Exception e) {
            ToastManager.show(parent, FeedbackType.ERROR, "Falha na impressão direta: " + e.getMessage());
            return false;
        }
    }
}
