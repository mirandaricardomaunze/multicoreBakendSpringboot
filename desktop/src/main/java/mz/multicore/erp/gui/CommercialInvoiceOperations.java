package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operações e ciclo de vida de faturas do módulo comercial:
 * listagem paginada, verificação de vias de impressão, liquidação, anulação e exportação.
 */
final class CommercialInvoiceOperations {

    private static final DateTimeFormatter INVOICE_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CommercialInvoiceOperations() {}

    static void openInvoiceEditor(ComercialPanel owner) {
        if (owner.clientsList.isEmpty()) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Cliente necessário", "Registe um cliente antes de emitir a fatura.");
            return;
        }
        if (owner.warehousesList.isEmpty()) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Armazém necessário",
                    "Registe um armazém para a empresa actual antes de emitir a fatura.");
            return;
        }
        owner.resetInvoiceDraft();
        owner.setLastCreatedInvoice(null);
        owner.invoiceGridEditable = true;
        if (owner.invoiceBarcodeField != null) owner.invoiceBarcodeField.setEnabled(true);
        if (owner.faturacaoCards != null && owner.faturacaoHost != null) {
            owner.faturacaoCards.show(owner.faturacaoHost, "editor");
        }
    }

    static void backToInvoicesList(ComercialPanel owner) {
        if (owner.faturacaoCards != null && owner.faturacaoHost != null) {
            owner.faturacaoCards.show(owner.faturacaoHost, "list");
        }
        owner.invoiceGridEditable = false;
        if (owner.invoiceBarcodeField != null) owner.invoiceBarcodeField.setEnabled(false);
    }

    static void cancelSelectedInvoice(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.invoicesTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha na tabela a fatura que pretende anular.");
            return;
        }

        Long invoiceId = (Long) owner.invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) owner.invoicesTableModel.getValueAt(row, 1);

        String reason = UIHelper.promptRequiredText("Anular Fatura", "fas-ban",
                "Fatura " + invoiceNum, "Motivo da anulação:");
        if (reason == null) return;

        UIHelper.runWithProgress(owner, "A anular fatura…", () -> {
            owner.comercialApiClient.cancelInvoice(invoiceId, reason);
            return null;
        }, ignored -> {
            ToastManager.success(owner, "Fatura " + invoiceNum + " anulada com sucesso.");
            owner.loadInvoicesTable();
            owner.getReceiptsPanel().refresh();
        }, error -> owner.showCommercialError("anular fatura", error));
    }

    static void paySelectedInvoice(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.invoicesTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha na tabela a fatura que pretende liquidar.");
            return;
        }

        Long invoiceId = (Long) owner.invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) owner.invoicesTableModel.getValueAt(row, 1);
        String statusStr = (String) owner.invoicesTableModel.getValueAt(row, 5);
        BigDecimal invoiceTotal = (BigDecimal) owner.invoicesTableModel.getValueAt(row, 7);

        if (!"APPROVED".equalsIgnoreCase(statusStr) && !"PARTIALLY_PAID".equalsIgnoreCase(statusStr)) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Fatura não liquidável",
                    "Apenas faturas por cobrar podem receber recibo. Estado actual: " + UIHelper.humanStatus(statusStr) + ".");
            return;
        }

        UIHelper.loadAsync(owner, owner.getFinanceApiClient()::getAllAccounts,
                accounts -> owner.getReceiptsPanel().openPayment(invoiceId, invoiceNum, invoiceTotal, accounts),
                error -> owner.showCommercialLoadError("contas de tesouraria", error));
    }

    static void loadInvoicesTable(ComercialPanel owner) {
        if (owner.invoicesPager != null) owner.invoicesPager.reload();
    }

    static void loadInvoicesPage(ComercialPanel owner, int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.getInvoicePage(companyId, page, size),
                response -> {
                    owner.invoicesPager.apply(response);
                    applyInvoices(owner, response.items());
                },
                error -> owner.showCommercialLoadError("faturas", error));
    }

    static void applyInvoices(ComercialPanel owner, List<InvoiceDTO> invoices) {
        owner.invoicesTableModel.setRowCount(0);
        for (InvoiceDTO invoice : invoices) {
            String dateStr = invoice.createdAt() != null ? invoice.createdAt().format(INVOICE_DATE_FMT) : "—";
            String buLabel = invoice.businessUnitName() != null && !invoice.businessUnitName().isBlank()
                    ? invoice.businessUnitName() : "Misto";
            String clientLabel = invoice.clientName();
            if (invoice.customerName() != null && !invoice.customerName().isBlank()) {
                clientLabel += " — " + invoice.customerName();
            }
            owner.invoicesTableModel.addRow(new Object[]{
                    invoice.id(),
                    invoice.invoiceNumber(),
                    clientLabel,
                    dateStr,
                    buLabel,
                    invoice.status().name(),
                    invoice.totalAmount(), invoice.outstandingAmount(),
                    owner.printApiClient == null ? "—" : "A verificar…"
            });
        }
        if (owner.printApiClient != null && !invoices.isEmpty()) {
            List<Long> ids = invoices.stream().map(InvoiceDTO::id).toList();
            UIHelper.loadAsync(owner,
                    () -> owner.printApiClient.getPrintStatuses(SensitiveDocumentType.INVOICE, ids),
                    statuses -> applyInvoicePrintStatuses(owner, statuses),
                    error -> {
                        for (int row = 0; row < owner.invoicesTableModel.getRowCount(); row++) {
                            if (ids.contains(owner.invoicesTableModel.getValueAt(row, 0))) {
                                owner.invoicesTableModel.setValueAt("Indisponível", row, 8);
                            }
                        }
                    });
        }
    }

    static void applyInvoicePrintStatuses(ComercialPanel owner, List<DocumentPrintStatusDTO> statuses) {
        Map<Long, DocumentPrintStatusDTO> byId = statuses.stream()
                .collect(Collectors.toMap(DocumentPrintStatusDTO::documentId, status -> status));
        for (int row = 0; row < owner.invoicesTableModel.getRowCount(); row++) {
            Long id = (Long) owner.invoicesTableModel.getValueAt(row, 0);
            DocumentPrintStatusDTO status = byId.get(id);
            if (status == null || status.printCount() == 0) {
                owner.invoicesTableModel.setValueAt("Não impressa", row, 8);
            } else {
                String label = status.printCount() == 1 ? "1 via" : status.printCount() + " vias";
                owner.invoicesTableModel.setValueAt(label, row, 8);
            }
        }
    }

    static void printSelectedInvoice(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.invoicesTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
            return;
        }
        Long invoiceId = (Long) owner.invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(owner.invoicesTableModel.getValueAt(row, 1));

        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"Folha A4 Oficial (4 Vias)", "Talão Térmico (80mm)"});
        UIHelper.styleComboBox(formatCombo);
        JPanel form = UIHelper.createDialogForm("Formato de Impressão:", formatCombo);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Imprimir Fatura " + invoiceNum,
                "fas-print", "Selecione o formato de impressão da fatura", form)
                .setConfirmButton("Imprimir", "fas-print")
                .showDialog();
        if (!confirmed) return;

        boolean thermal = formatCombo.getSelectedIndex() == 1;
        SensitivePrintCoordinator.print(owner, owner.printApiClient, SensitiveDocumentType.INVOICE, invoiceId,
                "Fatura " + invoiceNum,
                "fatura-" + (thermal ? "80mm-" : "") + invoiceNum,
                () -> thermal ? owner.comercialApiClient.renderInvoiceThermal(invoiceId)
                        : owner.comercialApiClient.renderInvoice(invoiceId),
                owner::loadInvoicesTable);
    }

    static void printSelectedGuide(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.invoicesTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
            return;
        }
        Long invoiceId = (Long) owner.invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(owner.invoicesTableModel.getValueAt(row, 1));
        SensitivePrintCoordinator.print(owner, owner.printApiClient,
                SensitiveDocumentType.REMITTANCE_GUIDE, invoiceId,
                "Guia de remessa da fatura " + invoiceNum,
                "guia-remessa-" + invoiceNum,
                () -> owner.comercialApiClient.renderGuide(invoiceId), null);
    }

    static void exportInvoicesTable(ComercialPanel owner) {
        TableExportAction.export(owner, owner.printApiClient, owner.invoicesTable, "Faturas Emitidas", "faturas");
    }
}
