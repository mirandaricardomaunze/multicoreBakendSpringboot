package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.RecurringContractApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;

import javax.swing.*;
import java.awt.Component;
import java.text.DecimalFormat;
import java.time.LocalDate;

/** Coordenador de operações de faturação periódica e gestão de estados de contratos recorrentes. */
public final class RecurringContractsOperationsCoordinator {

    private RecurringContractsOperationsCoordinator() {}

    public static void runBatchBilling(Component parent, RecurringContractApiClient client,
                                       InlineFeedbackPanel feedback, DecimalFormat currencyFmt, Runnable reload) {
        boolean confirm = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(parent),
                FeedbackType.INFO,
                "Facturação Recorrente em Lote",
                "Deseja processar o ciclo de facturação de todas as avenças activas vencidas até hoje?\n\n" +
                        "Serão emitidas faturas comerciais e os contratos serão actualizados automaticamente.",
                "Processar Ciclo"
        );
        if (!confirm) return;

        if (feedback != null) feedback.show(FeedbackType.INFO, "A processar ciclo de facturação periódica...");
        new SwingWorker<BatchBillingResultDTO, Void>() {
            @Override
            protected BatchBillingResultDTO doInBackground() {
                return client.runBatchBilling(LocalDate.now());
            }

            @Override
            protected void done() {
                try {
                    BatchBillingResultDTO result = get();
                    String formattedTotal = currencyFmt != null ? currencyFmt.format(result.totalBilledAmount()) : String.valueOf(result.totalBilledAmount());
                    String msg = String.format("Facturação em lote concluída com sucesso: %d contratos processados, %d faturas geradas (%s MT).",
                            result.contractsProcessed(), result.invoicesGenerated(), formattedTotal);
                    if (feedback != null) feedback.show(FeedbackType.SUCCESS, msg);
                    if (reload != null) reload.run();
                    if (feedback != null && !result.errors().isEmpty()) {
                        feedback.show(FeedbackType.WARNING, "Avisos de facturação: " + String.join("; ", result.errors()));
                    }
                } catch (Exception ex) {
                    if (feedback != null) feedback.show(FeedbackType.ERROR, "Falha na facturação em lote: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void manageContractStatus(Component parent, RecurringContractApiClient client,
                                            RecurringContractDTO selected, InlineFeedbackPanel feedback, Runnable reload) {
        if (selected == null) return;

        JComboBox<ContractStatus> statusCombo = UIHelper.createComboBox(
                new ContractStatus[]{ContractStatus.ACTIVE, ContractStatus.SUSPENDED, ContractStatus.CANCELLED},
                status -> switch (status) {
                    case ACTIVE -> "Activar Contrato (Em Vigor)";
                    case SUSPENDED -> "Suspender Contrato";
                    case CANCELLED -> "Cancelar Contrato";
                    default -> status.name();
                }
        );
        statusCombo.setSelectedItem(selected.status());

        JPanel form = UIHelper.createDialogForm("Novo Estado *:", statusCombo);
        ModernFormDialog dialog = new ModernFormDialog(
                SwingUtilities.getWindowAncestor(parent),
                "Gestão do Contrato",
                "fas-file-contract",
                "Selecione a acção desejada para o contrato " + selected.contractNumber(),
                form
        );

        dialog.setOnSave(() -> {
            ContractStatus newStatus = (ContractStatus) statusCombo.getSelectedItem();
            if (newStatus != null && newStatus != selected.status()) {
                client.updateStatus(selected.id(), newStatus);
            }
        });

        if (dialog.showDialog()) {
            ContractStatus chosen = (ContractStatus) statusCombo.getSelectedItem();
            if (feedback != null) feedback.show(FeedbackType.SUCCESS, "Estado do contrato alterado para " + (chosen != null ? chosen.name() : ""));
            if (reload != null) reload.run();
        }
    }
}
