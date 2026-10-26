package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.CollectionRadarDialog;
import mz.multicore.erp.gui.InvoiceInstallmentsDialog;
import mz.multicore.erp.gui.SalesReturnDialog;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.AgingBucketTotalDTO;
import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Coordenador de operações visuais e ações de cobrança para contas correntes. */
public final class OutstandingAccountsOperationsCoordinator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private OutstandingAccountsOperationsCoordinator() {}

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int mr, Runnable onOpenFull) {
        if (mr < 0 || mr >= model.getRowCount()) return;
        String num = String.valueOf(model.getValueAt(mr, 0));
        String date = String.valueOf(model.getValueAt(mr, 1));
        String bu = String.valueOf(model.getValueAt(mr, 2));
        String client = String.valueOf(model.getValueAt(mr, 3));
        String nuit = String.valueOf(model.getValueAt(mr, 4));
        String total = String.valueOf(model.getValueAt(mr, 5));
        String paid = String.valueOf(model.getValueAt(mr, 6));
        String balance = String.valueOf(model.getValueAt(mr, 7));
        String due = String.valueOf(model.getValueAt(mr, 8));
        String days = String.valueOf(model.getValueAt(mr, 9));
        String bucket = String.valueOf(model.getValueAt(mr, 10));
        String state = String.valueOf(model.getValueAt(mr, 11));

        peek.setTitle("Fatura " + num);
        peek.setSubtitle(client + " (NUIT " + nuit + ")");
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Data de Emissão", date, false),
                new QuickPeekPanel.PeekItem("Linha de Negócio", bu != null && !"-".equals(bu) ? bu : "Geral", false),
                new QuickPeekPanel.PeekItem("Vencimento", due, false),
                new QuickPeekPanel.PeekItem("Total Fatura", total, false),
                new QuickPeekPanel.PeekItem("Valor Pago", paid, false),
                new QuickPeekPanel.PeekItem("Saldo Devedor", balance, true),
                new QuickPeekPanel.PeekItem("Dias Atraso", days, false),
                new QuickPeekPanel.PeekItem("Antiguidade", bucket, false),
                new QuickPeekPanel.PeekItem("Estado", state, false)
        ));
        peek.getOpenFullButton().setText("Liquidar Fatura");
        peek.setOnOpenFullAction(v -> onOpenFull.run());
    }

    public static String formatAgingSummary(AgingSummaryDTO summary) {
        if (summary == null) return " ";
        StringBuilder text = new StringBuilder("<html><b>Antiguidade a ")
                .append(summary.referenceDate().format(DATE)).append(":</b> ");
        for (AgingBucketTotalDTO bucket : summary.buckets()) {
            text.append(String.format("&nbsp;%s: <b>%,.2f MT</b> (%d)&nbsp;·", bucket.label(),
                    bucket.amount(), bucket.invoiceCount()));
        }
        text.append(String.format("&nbsp;&nbsp;<b>Em atraso: %,.2f MT</b> de %,.2f MT",
                summary.overdueTotal(), summary.total()));
        return text.append("</html>").toString();
    }

    public static void openRadar(Component parent, ComercialApiClient comercialApiClient) {
        CollectionRadarDialog.show(SwingUtilities.getWindowAncestor(parent), comercialApiClient);
    }

    public static void openInstallments(Component parent, ComercialApiClient comercialApiClient,
                                        InvoiceDTO invoice, Runnable onRefresh) {
        if (invoice == null) return;
        InvoiceInstallmentsDialog.show(SwingUtilities.getWindowAncestor(parent), comercialApiClient, invoice, onRefresh);
    }

    public static void openSalesReturn(Component parent, ComercialApiClient comercialApiClient,
                                       InvoiceDTO invoice, Runnable onRefresh) {
        if (invoice == null) return;
        SalesReturnDialog.show(SwingUtilities.getWindowAncestor(parent), comercialApiClient, invoice, onRefresh);
    }

    public static void receivePayment(JComponent parent, InvoiceDTO invoice, FinanceApiClient financeApiClient,
                                      POSApiClient posApiClient, Runnable onRefresh,
                                      Consumer<Throwable> onErrorAction,
                                      BiConsumer<String, String> onErrorNotice) {
        if (invoice == null) return;
        UIHelper.loadAsync(parent, financeApiClient::getAllAccounts,
                accounts -> OutstandingPaymentDialog.show(parent, invoice, accounts, posApiClient, onRefresh, onErrorNotice, onErrorAction),
                onErrorAction);
    }
}
