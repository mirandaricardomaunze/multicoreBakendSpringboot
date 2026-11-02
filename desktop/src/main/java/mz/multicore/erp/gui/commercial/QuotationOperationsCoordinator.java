package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Coordenador de operações, ciclo de vida e impressão de propostas e cotações.
 */
final class QuotationOperationsCoordinator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private QuotationOperationsCoordinator() {}

    static void send(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number) {
        UIHelper.runWithProgress(owner, "A registar envio…", () -> apiClient.sendQuotation(id), ignored -> {
            owner.showSuccess("Cotação " + number + " marcada como enviada ao cliente.");
            owner.refresh();
        }, error -> owner.showError("marcar a cotação como enviada", error));
    }

    static void accept(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number) {
        UIHelper.runWithProgress(owner, "A registar aceitação…", () -> apiClient.acceptQuotation(id), ignored -> {
            owner.showSuccess("Cotação " + number + " aceite; pode agora convertê-la em encomenda.");
            owner.refresh();
        }, error -> owner.showError("registar a aceitação", error));
    }

    static void reject(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number) {
        String reason = UIHelper.promptRequiredText("Cotação Recusada", "fas-times",
                "Cotação " + number, "Motivo da recusa do cliente:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A registar recusa…", () -> apiClient.rejectQuotation(id, reason),
                ignored -> {
                    owner.showSuccess("Cotação " + number + " registada como recusada.");
                    owner.refresh();
                }, error -> owner.showError("registar a recusa", error));
    }

    static void cancel(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number) {
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.WARNING,
                "Confirmar Cancelamento", "Cancelar a cotação " + number + "?", "Cancelar Cotação")) return;

        UIHelper.runWithProgress(owner, "A cancelar cotação…", () -> apiClient.cancelQuotation(id), ignored -> {
            owner.showSuccess("Cotação " + number + " cancelada.");
            owner.refresh();
        }, error -> owner.showError("cancelar a cotação", error));
    }

    static void extendValidity(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number, String current) {
        DateField validity = new DateField();
        JPanel form = UIHelper.createDialogForm("Validade actual:", new JLabel(current), "Nova validade:", validity);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Estender Validade", "fas-calendar-alt",
                "Estender volta a garantir ao cliente os preços desta proposta", form)
                .setConfirmButton("Estender", "fas-check").showDialog();
        if (!confirmed) return;
        LocalDate newValidUntil = validity.value();

        UIHelper.runWithProgress(owner, "A estender validade…",
                () -> apiClient.extendQuotationValidity(id, newValidUntil), updated -> {
                    owner.showSuccess("Cotação " + number + " válida até " + updated.validUntil().format(DATE) + ".");
                    owner.refresh();
                }, error -> owner.showError("estender a validade", error));
    }

    static void convert(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number, Runnable ordersRefresh) {
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Confirmar Conversão",
                "Converter a cotação " + number + " numa encomenda?\n\n"
                        + "A encomenda mantém exactamente os preços cotados e fica pendente de aprovação.\n"
                        + "A cotação não poderá ser convertida outra vez.", "Converter em Encomenda")) return;

        UIHelper.runWithProgress(owner, "A converter em encomenda…", () -> apiClient.convertQuotation(id),
                order -> {
                    owner.showSuccess("Cotação " + number + " convertida na encomenda " + order.orderNumber()
                            + "; total " + order.totalAmount() + " MT, pendente de aprovação.");
                    owner.refresh();
                    if (ordersRefresh != null) ordersRefresh.run();
                }, error -> owner.showError("converter a cotação", error));
    }

    static void convertToInvoice(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number, Runnable ordersRefresh) {
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Confirmar Emissão de Fatura",
                "Converter a cotação " + number + " directamente numa Fatura comercial (FT)?\n\n"
                        + "A fatura mantém exactamente os preços cotados, efectua a baixa de stock e cria a conta a receber.\n"
                        + "A cotação não poderá ser convertida outra vez.", "Emitir Fatura")) return;

        UIHelper.runWithProgress(owner, "A converter em fatura…", () -> apiClient.convertQuotationToInvoice(id),
                invoice -> {
                    owner.showSuccess("Cotação " + number + " convertida na fatura " + invoice.invoiceNumber()
                            + "; total " + invoice.totalAmount() + " MT.");
                    owner.refresh();
                    if (ordersRefresh != null) ordersRefresh.run();
                }, error -> owner.showError("converter a cotação em fatura", error));
    }

    static void print(QuotationsPanel owner, ComercialApiClient apiClient, Long id, String number) {
        JComboBox<String> formatCombo = UIHelper.createComboBox(
                new String[]{"Folha A4 Oficial", "Talão Térmico (80mm)"}, value -> value);
        JPanel form = UIHelper.createDialogForm("Formato de Impressão:", formatCombo);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Imprimir Cotação " + number,
                "fas-print", "Selecione o formato de impressão da cotação", form)
                .setConfirmButton("Imprimir", "fas-print")
                .showDialog();
        if (!confirmed) return;

        boolean thermal = formatCombo.getSelectedIndex() == 1;
        String taskMsg = thermal ? "A gerar talão térmico 80mm…" : "A gerar cotação em PDF A4…";
        UIHelper.runWithProgress(owner, taskMsg,
                () -> thermal ? apiClient.renderQuotationThermal(id) : apiClient.renderQuotation(id),
                pdf -> PrintPreviewDialog.show(owner, pdf, "cotacao-" + (thermal ? "80mm-" : "") + number),
                error -> owner.showError("gerar a cotação em PDF", error));
    }

    static void generateProductionOrder(QuotationsPanel owner, ProductionApiClient productionApiClient,
                                        ComercialApiClient apiClient, Long quotationId,
                                        Supplier<List<WarehouseDTO>> warehouses) {
        if (productionApiClient == null) {
            owner.showNotice(FeedbackType.INFO, "Manufatura Indisponível",
                    "O módulo de Produção não está inicializado nesta sessão.");
            return;
        }

        UIHelper.loadAsync(owner, () -> apiClient.getQuotationById(quotationId), quotation -> {
            List<CommercialProductionOrderDialog.DocumentItem> items = new ArrayList<>();
            for (var line : quotation.lines()) {
                items.add(new CommercialProductionOrderDialog.DocumentItem(
                        line.productId(), line.productName(), line.quantity()));
            }
            CommercialProductionOrderDialog.open(
                    owner,
                    productionApiClient,
                    "Cotação " + quotation.quotationNumber(),
                    items,
                    warehouses.get(),
                    owner::refresh
            );
        }, error -> owner.showError("carregar cotação", error));
    }
}
