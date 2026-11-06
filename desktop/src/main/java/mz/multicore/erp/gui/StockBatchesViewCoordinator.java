package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Coordenador de apresentação, filtragem em memória, resumo e exportação de lotes. */
final class StockBatchesViewCoordinator {
    private static final String MASK = "•••";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    void applyBatches(StockPanel owner, List<ProductBatchDTO> loaded) {
        owner.batchesList = loaded;
        if (owner.batchWarehouseCombo != null) {
            Object sel = owner.batchWarehouseCombo.getSelectedItem();
            owner.batchWarehouseCombo.removeAllItems();
            owner.batchWarehouseCombo.addItem("Todos os Armazéns");
            for (WarehouseDTO w : owner.warehousesList) owner.batchWarehouseCombo.addItem(w.name());
            if (sel != null) owner.batchWarehouseCombo.setSelectedItem(sel);
        }
        if (owner.batchBusinessUnitCombo != null) {
            Object selBu = owner.batchBusinessUnitCombo.getSelectedItem();
            Set<String> bus = new TreeSet<>();
            for (var b : owner.batchesList) {
                if (b.businessUnitName() != null && !b.businessUnitName().isBlank()) bus.add(b.businessUnitName());
            }
            owner.batchBusinessUnitCombo.removeAllItems();
            owner.batchBusinessUnitCombo.addItem("Todos os negócios");
            owner.batchBusinessUnitCombo.addItem("—");
            for (String n : bus) owner.batchBusinessUnitCombo.addItem(n);
            if (selBu != null) owner.batchBusinessUnitCombo.setSelectedItem(selBu);
        }
        updateBatchesSummary(owner);
        filterBatches(owner);
    }

    void updateBatchesSummary(StockPanel owner) {
        if (owner.batchesSummary == null) return;
        LocalDate today = LocalDate.now();
        long expired = 0;
        long soon = 0;
        for (var b : owner.batchesList) {
            LocalDate expiry = BatchDisplay.expiry(b.expirationDate());
            if (expiry == null || b.quantity() == null || b.quantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            long days = ChronoUnit.DAYS.between(today, expiry);
            if (days < 0) expired++;
            else if (days <= owner.EXPIRY_SOON_DAYS) soon++;
        }
        if (expired == 0 && soon == 0) {
            owner.batchesSummary.setForeground(UIHelper.APPROVED_GREEN);
            owner.batchesSummary.setText("Sem lotes vencidos ou a vencer em breve.");
        } else {
            owner.batchesSummary.setForeground(expired > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            owner.batchesSummary.setText(String.format("%d lote%s vencido%s · %d a vencer em ≤ %d dias",
                    expired, expired == 1 ? "" : "s", expired == 1 ? "" : "s", soon, owner.EXPIRY_SOON_DAYS));
        }
    }

    void filterBatches(StockPanel owner) {
        if (owner.batchesModel == null) return;
        owner.batchesModel.setRowCount(0);
        boolean hide = owner.stockHidden();
        Long whId = null;
        if (owner.batchWarehouseCombo != null) {
            int idx = owner.batchWarehouseCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < owner.warehousesList.size()) whId = owner.warehousesList.get(idx - 1).id();
        }
        String wantBu = owner.batchBusinessUnitCombo != null && owner.batchBusinessUnitCombo.getSelectedIndex() > 0
                ? String.valueOf(owner.batchBusinessUnitCombo.getSelectedItem()) : null;
        String query = owner.batchSearchField == null ? "" : owner.batchSearchField.getText().trim().toLowerCase();
        int expIdx = owner.batchExpirationCombo == null ? 0 : owner.batchExpirationCombo.getSelectedIndex();
        LocalDate today = LocalDate.now();

        for (var b : owner.batchesList) {
            if (whId != null && !b.warehouseId().equals(whId)) continue;
            String bBu = b.businessUnitName() != null && !b.businessUnitName().isBlank() ? b.businessUnitName() : "—";
            if (wantBu != null && !wantBu.equalsIgnoreCase(bBu)) continue;
            if (!query.isEmpty()) {
                String s = (b.sku() == null ? "" : b.sku()) + " " + (b.productName() == null ? "" : b.productName()) + " " + (b.batchNumber() == null ? "" : b.batchNumber());
                if (!s.toLowerCase().contains(query)) continue;
            }
            LocalDate expiry = BatchDisplay.expiry(b.expirationDate());
            long d = expiry == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(today, expiry);
            boolean match = switch (expIdx) {
                case 1 -> d < 0;
                case 2 -> d >= 0 && d <= 30;
                case 3 -> d >= 0 && d <= 90;
                case 4 -> d > 90;
                default -> true;
            };
            if (!match) continue;
            String status = d == Long.MAX_VALUE ? "Sem validade" : d < 0 ? "VENCIDO" : d <= 30 ? "VENCE EM BREVE" : "VÁLIDO";
            owner.batchesModel.addRow(new Object[]{
                    b.sku(), b.productName(), b.warehouseName(), BatchDisplay.number(b.batchNumber()),
                    expiry == null ? "—" : expiry.format(FMT),
                    d == Long.MAX_VALUE ? "—" : String.valueOf(d),
                    hide ? MASK : String.format("%,.2f", b.quantity()), status, bBu
            });
        }
    }

    void populateQuickPeek(QuickPeekPanel peek, int mr, StockPanel owner) {
        if (mr < 0 || mr >= owner.batchesModel.getRowCount()) return;
        var m = owner.batchesModel;
        peek.setTitle("Lote " + m.getValueAt(mr, 3));
        peek.setSubtitle(m.getValueAt(mr, 1) + " (" + m.getValueAt(mr, 0) + ")");
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Armazém", peek.cell(m.getValueAt(mr, 2), 2), false),
                new QuickPeekPanel.PeekItem("Nº Lote", peek.cell(m.getValueAt(mr, 3), 3), false),
                new QuickPeekPanel.PeekItem("Linha de Negócio", peek.cell(m.getValueAt(mr, 8), 8), false),
                new QuickPeekPanel.PeekItem("Validade", peek.cell(m.getValueAt(mr, 4), 4), false),
                new QuickPeekPanel.PeekItem("Dias Restantes", peek.cell(m.getValueAt(mr, 5), 5), false),
                new QuickPeekPanel.PeekItem("Quantidade", peek.cell(m.getValueAt(mr, 6), 6), true),
                new QuickPeekPanel.PeekItem("Estado", peek.cell(m.getValueAt(mr, 7), 7), false)
        ));
        peek.getOpenFullButton().setText("Ficha do Artigo");
        peek.setOnOpenFullAction(v -> {
            if (owner.batchesList != null && mr < owner.batchesList.size()) {
                var b = owner.batchesList.get(mr);
                if (b != null && b.productId() != null) owner.showProductDetailDialog(b.productId());
                else owner.createBatchEntryDialog(null);
            }
        });
    }

    void exportBatchesPdf(StockPanel owner) {
        if (owner.batchesTable.getRowCount() == 0) {
            owner.showStockNotice(FeedbackType.INFO, "Sem dados para exportar", "Não existem lotes na vista actual.");
            return;
        }
        TableExportAction.export(owner, owner.printApiClient, owner.batchesTable, "Lotes & Validades", "lotes-validades");
    }
}
