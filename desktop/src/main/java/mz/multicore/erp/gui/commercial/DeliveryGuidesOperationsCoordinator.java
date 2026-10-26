package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.quantity.PackageQuantity;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Coordenador de operações assíncronas, fluxo de aprovação e visualização de embalagens para guias de remessa.
 */
public final class DeliveryGuidesOperationsCoordinator {

    private DeliveryGuidesOperationsCoordinator() {
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int mr, Runnable onOpenFull) {
        if (mr < 0 || mr >= model.getRowCount()) return;
        String guideNum = String.valueOf(model.getValueAt(mr, 1));
        String date = String.valueOf(model.getValueAt(mr, 2));
        String orderNum = String.valueOf(model.getValueAt(mr, 3));
        String client = String.valueOf(model.getValueAt(mr, 4));
        String wh = String.valueOf(model.getValueAt(mr, 5));
        String resp = String.valueOf(model.getValueAt(mr, 6));
        String vehicle = String.valueOf(model.getValueAt(mr, 7));
        String total = String.valueOf(model.getValueAt(mr, 8));
        String state = String.valueOf(model.getValueAt(mr, 9));

        peek.setTitle("Guia " + guideNum);
        peek.setSubtitle(client);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Origem", orderNum, false),
                new QuickPeekPanel.PeekItem("Data Emissão", date, false),
                new QuickPeekPanel.PeekItem("Armazém Expedição", wh, false),
                new QuickPeekPanel.PeekItem("Motorista", resp, false),
                new QuickPeekPanel.PeekItem("Viatura", vehicle, false),
                new QuickPeekPanel.PeekItem("Valor Total", total, true),
                new QuickPeekPanel.PeekItem("Estado", state, false)
        ));
        peek.getOpenFullButton().setText("Ver Caixas e Unidades");
        peek.setOnOpenFullAction(v -> onOpenFull.run());
    }

    public static void approve(
            Component parent,
            Long id,
            String number,
            ComercialApiClient apiClient,
            InlineFeedbackPanel feedback,
            Runnable onSuccess
    ) {
        boolean confirmed = UIHelper.confirm(parent,
                "A aprovação da guia " + number + " dará saída definitiva ao stock.\n\nPretende continuar?",
                "Confirmar Aprovação");
        if (!confirmed) return;

        UIHelper.runWithProgress(parent, "A aprovar guia…", () -> apiClient.approveDeliveryGuide(id), ignored -> {
            ToastManager.success(parent, "Guia " + number + " aprovada; o stock foi actualizado.");
            if (onSuccess != null) onSuccess.run();
        }, error -> showError(feedback, "aprovar guia", error));
    }

    public static void reject(
            Component parent,
            Long id,
            String number,
            ComercialApiClient apiClient,
            InlineFeedbackPanel feedback,
            Runnable onSuccess
    ) {
        String reason = UIHelper.promptRequiredText("Rejeitar Guia de Remessa", "fas-times",
                "Guia " + number, "Motivo da rejeição:");
        if (reason == null) return;

        UIHelper.runWithProgress(parent, "A rejeitar guia…", () -> apiClient.rejectDeliveryGuide(id, reason), ignored -> {
            ToastManager.success(parent, "Guia " + number + " rejeitada; a encomenda voltou a ficar disponível.");
            if (onSuccess != null) onSuccess.run();
        }, error -> showError(feedback, "rejeitar guia", error));
    }

    public static void cancel(
            Component parent,
            Long id,
            String number,
            ComercialApiClient apiClient,
            InlineFeedbackPanel feedback,
            Runnable onSuccess
    ) {
        boolean confirmed = UIHelper.confirm(parent,
                "Cancelar a guia " + number + "?\nA encomenda voltará a ficar disponível para faturação ou nova guia.",
                "Confirmar Cancelamento");
        if (!confirmed) return;

        UIHelper.runWithProgress(parent, "A cancelar guia…", () -> apiClient.cancelDeliveryGuide(id), ignored -> {
            ToastManager.success(parent, "Guia " + number + " cancelada.");
            if (onSuccess != null) onSuccess.run();
        }, error -> showError(feedback, "cancelar guia", error));
    }

    public static void print(
            Component parent,
            Long id,
            String number,
            ComercialApiClient apiClient,
            PrintApiClient printApiClient,
            Runnable onRefresh,
            InlineFeedbackPanel feedback
    ) {
        if (printApiClient == null) {
            UIHelper.runWithProgress(parent, "A gerar guia em PDF…", () -> apiClient.renderDeliveryGuide(id),
                    pdf -> PrintPreviewDialog.show(parent, pdf, "guia-remessa-" + number),
                    error -> showError(feedback, "gerar guia", error));
            return;
        }
        SensitivePrintCoordinator.print(parent, printApiClient, SensitiveDocumentType.DELIVERY_GUIDE,
                id, "Guia de remessa " + number, "guia-remessa-" + number,
                () -> apiClient.renderDeliveryGuide(id), onRefresh);
    }

    public static void showPackagesModal(
            JComponent parent,
            Long id,
            ComercialApiClient apiClient,
            InlineFeedbackPanel feedback
    ) {
        UIHelper.loadAsync(parent, () -> apiClient.getDeliveryGuideById(id), guide -> {
            DefaultTableModel lines = new DefaultTableModel(
                    new String[]{"Produto", "Quantidade", "Composição", "Valor Unit.", "Total", "Peso kg", "% Qtd", "% Peso"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            guide.lines().forEach(line -> {
                String packages = PackageQuantity.label(line.quantity(), line.unitsPerBox());
                lines.addRow(new Object[]{
                        line.productName(),
                        line.quantity(),
                        packages,
                        line.unitPrice() != null ? line.unitPrice() : BigDecimal.ZERO,
                        line.lineTotal() != null ? line.lineTotal() : BigDecimal.ZERO,
                        line.lineGrossWeightKg(),
                        line.quantityPercentage() + "%",
                        line.weightPercentage() + "%"
                });
            });
            JTable details = new JTable(lines);
            UIHelper.styleTable(details);
            details.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
            details.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
            JScrollPane scroll = new JScrollPane(details);
            UIHelper.styleScrollPane(scroll);
            scroll.setPreferredSize(new Dimension(900, 300));
            ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Caixas e Unidades — " + guide.guideNumber(), "fas-boxes", scroll);
            dlg.asReadOnly("Fechar");
            dlg.setSize(920, 480);
            dlg.showDialog();
        }, error -> showError(feedback, "carregar caixas da guia", error));
    }

    private static void showError(InlineFeedbackPanel feedback, String action, Throwable error) {
        if (feedback != null) {
            feedback.show(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage(), null, null);
        }
    }
}
