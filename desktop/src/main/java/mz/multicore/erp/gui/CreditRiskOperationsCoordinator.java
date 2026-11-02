package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;

import javax.swing.*;
import java.awt.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Coordenador de operações assíncronas de cobrança e carregamento de risco.
 */
public final class CreditRiskOperationsCoordinator {

    public record RiskFetchResult(CreditRiskSummaryDTO summary, List<ClientCreditRiskDTO> clients) {}

    private CreditRiskOperationsCoordinator() {}

    public static void loadRiskData(Component parent, CreditRiskApiClient client, LocalDate refDate,
                                    JProgressBar busyBar, Consumer<RiskFetchResult> onSuccess) {
        if (busyBar != null) busyBar.setVisible(true);

        new SwingWorker<RiskFetchResult, Void>() {
            @Override
            protected RiskFetchResult doInBackground() {
                CreditRiskSummaryDTO summary = client != null ? client.getSummary(refDate) : null;
                List<ClientCreditRiskDTO> clients = client != null ? client.getClientsRisk(refDate) : List.of();
                return new RiskFetchResult(summary, clients);
            }

            @Override
            protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    RiskFetchResult res = get();
                    if (onSuccess != null) onSuccess.accept(res);
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao carregar risco de crédito: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void emitDebtCollectionNoticePdf(Component parent, CreditRiskApiClient client,
                                                   ClientCreditRiskDTO selected, LocalDate refDate,
                                                   JProgressBar busyBar) {
        if (selected == null) {
            ToastManager.show(parent, FeedbackType.WARNING, "Seleccione um cliente na tabela para emitir a carta de cobrança.");
            return;
        }

        if (selected.totalOverdue() == null || selected.totalOverdue().signum() <= 0) {
            ToastManager.show(parent, FeedbackType.INFO,
                    String.format("O cliente %s não possui faturas vencidas em mora.", selected.clientName()));
            return;
        }

        if (busyBar != null) busyBar.setVisible(true);

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return client.getNoticePdf(selected.clientId(), refDate);
            }

            @Override
            protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(parent, pdfBytes, "notificacao-cobranca-" + selected.clientTaxId() + ".pdf");
                    } else {
                        ToastManager.show(parent, FeedbackType.ERROR, "Não foi possível gerar a carta de cobrança.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar carta de cobrança: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
