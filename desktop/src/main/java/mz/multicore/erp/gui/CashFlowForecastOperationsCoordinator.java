package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;

import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.function.Consumer;

/** Coordenador de operações assíncronas e impressão em PDF para o Fluxo de Caixa Previsional. */
public final class CashFlowForecastOperationsCoordinator {

    private CashFlowForecastOperationsCoordinator() {}

    public static void loadForecast(Component parent, CashFlowForecastApiClient client,
                                   JProgressBar busyBar, Consumer<CashFlowForecastDTO> onLoaded) {
        if (busyBar != null) busyBar.setVisible(true);

        new SwingWorker<CashFlowForecastDTO, Void>() {
            @Override
            protected CashFlowForecastDTO doInBackground() {
                try {
                    return client.getForecast();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    CashFlowForecastDTO data = get();
                    if (data != null) {
                        if (onLoaded != null) onLoaded.accept(data);
                    } else {
                        ToastManager.show(parent, FeedbackType.WARNING, "Não foi possível carregar a projeção de tesouraria.");
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public static void printForecastPdf(Component parent, CashFlowForecastApiClient client, JProgressBar busyBar) {
        if (busyBar != null) busyBar.setVisible(true);

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                try {
                    return client.getForecastPdf();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(parent, pdfBytes, "fluxo-caixa-previsional.pdf");
                    } else {
                        ToastManager.show(parent, FeedbackType.ERROR, "Não foi possível gerar o PDF de fluxo de caixa.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar PDF: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static String formatMoney(BigDecimal val) {
        if (val == null) return "0,00 MT";
        return String.format(DisplayLocale.NUMBERS, "%,.2f MT", val);
    }
}
