package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.dto.EmailDispatchResultDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Coordenador de operações assíncronas, emissão documental e Quick Peek de Conta Corrente de Clientes. */
public final class CustomerStatementOperationsCoordinator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CustomerStatementOperationsCoordinator() {}

    public static void loadClients(ComercialApiClient client, JProgressBar busyBar,
                                  JComboBox<CustomerStatementPanel.ClientComboItem> combo, Runnable onDone) {
        if (busyBar != null) busyBar.setVisible(true);
        new SwingWorker<List<ClientDTO>, Void>() {
            @Override protected List<ClientDTO> doInBackground() {
                try { return client.getClients(); } catch (Exception e) { return List.of(); }
            }
            @Override protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    List<ClientDTO> list = get();
                    combo.removeAllItems();
                    for (ClientDTO c : list) combo.addItem(new CustomerStatementPanel.ClientComboItem(c.id(), c.name(), c.taxId()));
                    if (!list.isEmpty() && onDone != null) onDone.run();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public static void updateSummary(CustomerStatementDTO stmt, JLabel opening, JLabel debits, JLabel credits,
                                     JLabel closing, JLabel overdue, JComboBox<String> buCombo) {
        if (stmt == null) return;
        opening.setText(formatMoney(stmt.openingBalance()));
        debits.setText(formatMoney(stmt.totalDebits()));
        credits.setText(formatMoney(stmt.totalCredits()));
        closing.setText(formatMoney(stmt.closingBalance()));
        overdue.setText(formatMoney(stmt.overdueAmount()));
        closing.setForeground(stmt.closingBalance().signum() > 0 ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
        if (buCombo != null) {
            Set<String> bus = new TreeSet<>();
            if (stmt.lines() != null) {
                for (CustomerStatementLineDTO line : stmt.lines()) {
                    if (line.businessUnitName() != null && !line.businessUnitName().isBlank()) bus.add(line.businessUnitName());
                }
            }
            String selectedBu = (String) buCombo.getSelectedItem();
            buCombo.removeAllItems();
            buCombo.addItem("Todos os negócios");
            for (String b : bus) buCombo.addItem(b);
            if (selectedBu != null) buCombo.setSelectedItem(selectedBu);
        }
    }

    public static void renderLines(DefaultTableModel tableModel, CustomerStatementDTO stmt, String selectedBu) {
        if (stmt == null) return;
        tableModel.setRowCount(0);
        boolean filterBu = selectedBu != null && !selectedBu.startsWith("Todos");
        tableModel.addRow(new Object[]{
                stmt.startDate().format(DATE_FMT), "TRANS", "-", "SALDO-ANT",
                "Saldo Anterior Transmitido", "-", BigDecimal.ZERO, BigDecimal.ZERO, stmt.openingBalance()
        });
        if (stmt.lines() != null) {
            for (CustomerStatementLineDTO line : stmt.lines()) {
                String bu = line.businessUnitName() != null && !line.businessUnitName().isBlank() ? line.businessUnitName() : "-";
                if (filterBu && !bu.equalsIgnoreCase(selectedBu)) continue;
                tableModel.addRow(new Object[]{
                        line.date().format(DATE_FMT), line.documentType(), bu, line.documentNumber(),
                        line.description(), line.reference(), line.debit(), line.credit(), line.runningBalance()
                });
            }
        }
    }

    public static void emitStatementPdf(JComponent parent, AccountStatementApiClient client, Long clientId,
                                       String taxId, LocalDate start, LocalDate end, JProgressBar busyBar) {
        if (busyBar != null) busyBar.setVisible(true);
        new SwingWorker<byte[], Void>() {
            @Override protected byte[] doInBackground() {
                return client.getCustomerStatementPdf(clientId, start, end);
            }
            @Override protected void done() {
                if (busyBar != null) busyBar.setVisible(false);
                try {
                    byte[] pdfBytes = get();
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        PrintPreviewDialog.show(parent, pdfBytes, "extrato-cliente-" + (taxId != null ? taxId : "doc") + ".pdf");
                    } else {
                        ToastManager.show(parent, FeedbackType.ERROR, "Documento de extrato vazio.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar extrato em PDF: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void emitStatementEmail(JComponent parent, AccountStatementApiClient client, Long clientId,
                                         String clientName, LocalDate start, LocalDate end) {
        SendStatementEmailDialog dlg = new SendStatementEmailDialog(UIHelper.mainWindow, client, clientId, clientName, null, start, end);
        if (dlg.showDialog()) {
            EmailDispatchResultDTO res = dlg.getLastResult();
            if (res != null && res.success()) ToastManager.success(parent, res.message());
        }
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel tableModel, int modelRow) {
        if (tableModel == null || modelRow < 0 || modelRow >= tableModel.getRowCount()) return;
        String date = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String type = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String bu = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String docNo = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String desc = String.valueOf(tableModel.getValueAt(modelRow, 4));
        String ref = String.valueOf(tableModel.getValueAt(modelRow, 5));
        Object debitVal = tableModel.getValueAt(modelRow, 6);
        Object creditVal = tableModel.getValueAt(modelRow, 7);
        Object balanceVal = tableModel.getValueAt(modelRow, 8);

        peek.setHeaderIcon("fas-file-invoice", UIHelper.ACCENT_BLUE);
        peek.setTitle(docNo != null && !"-".equals(docNo) ? docNo : desc);
        peek.setSubtitle(desc);
        peek.setStatus(type, "FT".equals(type) || "ND".equals(type) ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Data", date, false));
        items.add(new QuickPeekPanel.PeekItem("Tipo de Documento", type, false));
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", bu != null && !"-".equals(bu) ? bu : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Referência", ref != null ? ref : "-", false));
        if (debitVal instanceof BigDecimal d && d.signum() > 0) items.add(new QuickPeekPanel.PeekItem("Débito (+)", formatMoney(d), false));
        if (creditVal instanceof BigDecimal c && c.signum() > 0) items.add(new QuickPeekPanel.PeekItem("Crédito (-)", formatMoney(c), false));
        if (balanceVal instanceof BigDecimal b) items.add(new QuickPeekPanel.PeekItem("Saldo Acumulado", formatMoney(b), true));
        peek.setItems(items);
        peek.setOnOpenFullAction(null);
    }

    public static LocalDate parseDate(String text, LocalDate fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        try { return LocalDate.parse(text.trim()); } catch (Exception e) {
            try { return LocalDate.parse(text.trim(), DATE_FMT); } catch (Exception e2) { return fallback; }
        }
    }

    public static String formatMoney(BigDecimal val) {
        if (val == null) return "0,00 MT";
        return String.format(DisplayLocale.NUMBERS, "%,.2f MT", val);
    }
}
