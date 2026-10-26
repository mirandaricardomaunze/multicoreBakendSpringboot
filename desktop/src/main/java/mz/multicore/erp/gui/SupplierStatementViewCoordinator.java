package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/** Coordenador de apresentação, renderização de linhas de extrato e carregamento assíncrono. */
public final class SupplierStatementViewCoordinator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SupplierStatementViewCoordinator() {}

    public static void populateKpisAndBusinessUnits(
            JLabel opening, JLabel debits, JLabel credits, JLabel closing,
            JComboBox<String> buCombo, SupplierStatementDTO stmt
    ) {
        if (stmt == null) return;
        opening.setText(SupplierStatementOperationsCoordinator.formatMoney(stmt.openingBalance()));
        debits.setText(SupplierStatementOperationsCoordinator.formatMoney(stmt.totalDebits()));
        credits.setText(SupplierStatementOperationsCoordinator.formatMoney(stmt.totalCredits()));
        closing.setText(SupplierStatementOperationsCoordinator.formatMoney(stmt.closingBalance()));
        closing.setForeground(stmt.closingBalance().signum() > 0 ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        if (buCombo != null) {
            Set<String> bus = new TreeSet<>();
            if (stmt.lines() != null) {
                for (SupplierStatementLineDTO l : stmt.lines()) {
                    if (l.businessUnitName() != null && !l.businessUnitName().isBlank()) bus.add(l.businessUnitName());
                }
            }
            String selectedBu = (String) buCombo.getSelectedItem();
            buCombo.removeAllItems();
            buCombo.addItem("Todos os negócios");
            for (String b : bus) buCombo.addItem(b);
            if (selectedBu != null) buCombo.setSelectedItem(selectedBu);
        }
    }

    public static void renderLines(DefaultTableModel tableModel, SupplierStatementDTO stmt, String selectedBu) {
        if (stmt == null || tableModel == null) return;
        tableModel.setRowCount(0);
        boolean filterBu = selectedBu != null && !selectedBu.startsWith("Todos");
        tableModel.addRow(new Object[]{
                stmt.startDate().format(DATE_FMT), "TRANS", "-", "SALDO-ANT",
                "Saldo Anterior a Pagar", "-", BigDecimal.ZERO, BigDecimal.ZERO, stmt.openingBalance()
        });
        if (stmt.lines() != null) {
            for (SupplierStatementLineDTO l : stmt.lines()) {
                String bu = l.businessUnitName() != null && !l.businessUnitName().isBlank() ? l.businessUnitName() : "-";
                if (filterBu && !bu.equalsIgnoreCase(selectedBu)) continue;
                tableModel.addRow(new Object[]{
                        l.date().format(DATE_FMT), l.documentType(), bu, l.documentNumber(),
                        l.description(), l.reference(), l.debit(), l.credit(), l.runningBalance()
                });
            }
        }
    }

    public static void loadSuppliersAsync(
            PurchaseApiClient client, JComboBox<SupplierStatementPanel.SupplierComboItem> combo,
            JProgressBar busy, Runnable onLoaded
    ) {
        busy.setVisible(true);
        new SwingWorker<List<SupplierDTO>, Void>() {
            @Override protected List<SupplierDTO> doInBackground() {
                try { return client.getSuppliers(); } catch (Exception e) { return List.of(); }
            }
            @Override protected void done() {
                busy.setVisible(false);
                try {
                    List<SupplierDTO> list = get();
                    combo.removeAllItems();
                    for (SupplierDTO s : list) combo.addItem(new SupplierStatementPanel.SupplierComboItem(s.id(), s.name(), s.taxId()));
                    if (!list.isEmpty() && onLoaded != null) onLoaded.run();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public static void loadStatementAsync(
            AccountStatementApiClient client, JComponent parent, Long supplierId,
            LocalDate start, LocalDate end, JProgressBar busy, Consumer<SupplierStatementDTO> onLoaded
    ) {
        busy.setVisible(true);
        new SwingWorker<SupplierStatementDTO, Void>() {
            @Override protected SupplierStatementDTO doInBackground() { return client.getSupplierStatement(supplierId, start, end); }
            @Override protected void done() {
                busy.setVisible(false);
                try {
                    SupplierStatementDTO dto = get();
                    if (onLoaded != null) onLoaded.accept(dto);
                } catch (Exception ex) {
                    ToastManager.show(parent, FeedbackType.ERROR, "Erro ao carregar extrato de fornecedor: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
