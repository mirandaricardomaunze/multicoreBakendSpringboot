package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Coordenador de operações assíncronas, emissão documental e Quick Peek de Conta Corrente de Fornecedores. */
public final class SupplierStatementOperationsCoordinator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SupplierStatementOperationsCoordinator() {}

    public static void emitStatementPdf(JComponent parent, AccountStatementApiClient client, Long supplierId,
                                       String taxId, LocalDate start, LocalDate end, JProgressBar busyBar) {
        if (busyBar != null) busyBar.setVisible(true);
        new SwingWorker<byte[], Void>() {
            @Override protected byte[] doInBackground() {
                return client.getSupplierStatementPdf(supplierId, start, end);
            }
            @Override protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        String filename = "extrato-fornecedor-" + (taxId != null ? taxId : "doc") + ".pdf";
                        PrintPreviewDialog.show(parent, pdfBytes, filename);
                    } else {
                        ToastManager.show(parent, FeedbackType.ERROR, "Documento de extrato vazio.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar extrato em PDF: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel tableModel, int modelRow) {
        if (tableModel == null || modelRow < 0 || modelRow >= tableModel.getRowCount()) return;
        String date = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String type = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String bu = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String docNo = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String desc = String.valueOf(tableModel.getValueAt(modelRow, 4));
        String ref = String.valueOf(tableModel.getValueAt(modelRow, 5));
        Object paymentsVal = tableModel.getValueAt(modelRow, 6);
        Object purchasesVal = tableModel.getValueAt(modelRow, 7);
        Object balanceVal = tableModel.getValueAt(modelRow, 8);

        peek.setHeaderIcon("fas-truck-loading", UIHelper.ACCENT_BLUE);
        peek.setTitle(docNo != null && !"-".equals(docNo) ? docNo : desc);
        peek.setSubtitle(desc);
        peek.setStatus(type, "FT".equals(type) || "ND".equals(type) ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Data", date, false));
        items.add(new QuickPeekPanel.PeekItem("Tipo de Documento", type, false));
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", bu != null && !"-".equals(bu) ? bu : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Referência", ref != null ? ref : "-", false));
        if (paymentsVal instanceof BigDecimal p && p.signum() > 0) {
            items.add(new QuickPeekPanel.PeekItem("Pagamentos (-)", formatMoney(p), false));
        }
        if (purchasesVal instanceof BigDecimal c && c.signum() > 0) {
            items.add(new QuickPeekPanel.PeekItem("Compras (+)", formatMoney(c), false));
        }
        if (balanceVal instanceof BigDecimal b) {
            items.add(new QuickPeekPanel.PeekItem("Saldo a Pagar", formatMoney(b), true));
        }
        peek.setItems(items);
        peek.setOnOpenFullAction(null);
    }

    public static LocalDate parseDate(String text, LocalDate fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            try { return LocalDate.parse(text.trim(), DATE_FMT); }
            catch (Exception e2) { return fallback; }
        }
    }

    public static String formatMoney(BigDecimal val) {
        if (val == null) return "0,00 MT";
        return String.format(DisplayLocale.NUMBERS, "%,.2f MT", val);
    }
}
