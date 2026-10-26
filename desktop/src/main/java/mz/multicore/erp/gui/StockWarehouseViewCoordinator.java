package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Coordenador de operações visuais e métricas de armazéns. */
final class StockWarehouseViewCoordinator {

    private StockWarehouseViewCoordinator() {}

    static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int mr, Runnable onOpenFull) {
        if (mr < 0 || mr >= model.getRowCount()) return;
        String name = String.valueOf(model.getValueAt(mr, 0));
        String num = String.valueOf(model.getValueAt(mr, 1));
        String type = String.valueOf(model.getValueAt(mr, 2));
        String cap = String.valueOf(model.getValueAt(mr, 3));
        String loc = String.valueOf(model.getValueAt(mr, 4));
        String resp = String.valueOf(model.getValueAt(mr, 5));
        String tel = String.valueOf(model.getValueAt(mr, 6));
        String email = String.valueOf(model.getValueAt(mr, 7));
        String status = String.valueOf(model.getValueAt(mr, 8));

        peek.setTitle(name);
        peek.setSubtitle("Nº " + num + " • " + type);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Tipo", type, false),
                new QuickPeekPanel.PeekItem("Capacidade", cap, false),
                new QuickPeekPanel.PeekItem("Localização", loc, false),
                new QuickPeekPanel.PeekItem("Responsável", resp, false),
                new QuickPeekPanel.PeekItem("Telefone", tel, false),
                new QuickPeekPanel.PeekItem("Email", email, false),
                new QuickPeekPanel.PeekItem("Estado", status, true)
        ));
        peek.getOpenFullButton().setText("Editar Armazém");
        peek.setOnOpenFullAction(v -> onOpenFull.run());
    }

    static void updateKpis(List<WarehouseDTO> list, List<StockDTO> stocksList,
                           JLabel kpiActive, JLabel kpiSubtitle,
                           JLabel kpiCap, JLabel kpiVal, JLabel kpiNet) {
        if (list == null) return;
        long totalCount = list.size();
        long activeCount = list.stream().filter(WarehouseDTO::active).count();
        long salesCount = list.stream().filter(w -> w.active() && w.allowsSales()).count();
        long storageCount = Math.max(0, activeCount - salesCount);

        kpiActive.setText(activeCount + " / " + totalCount);
        kpiSubtitle.setText(salesCount + " loja(s) POS · " + storageCount + " depósito(s)");

        BigDecimal totalCapacity = list.stream()
                .filter(WarehouseDTO::active)
                .map(w -> w.capacity() != null ? w.capacity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalCapacity.compareTo(BigDecimal.ZERO) > 0) {
            kpiCap.setText(String.format("%,.0f un", totalCapacity));
        } else {
            kpiCap.setText("—");
        }

        BigDecimal totalStockVal = BigDecimal.ZERO;
        if (stocksList != null) {
            for (StockDTO s : stocksList) {
                BigDecimal qty = s.quantity() != null ? s.quantity() : BigDecimal.ZERO;
                BigDecimal price = s.unitPrice() != null ? s.unitPrice() : BigDecimal.ZERO;
                totalStockVal = totalStockVal.add(qty.multiply(price));
            }
        }
        kpiVal.setText(UIHelper.formatMzn(totalStockVal));

        long distinctTypes = list.stream()
                .filter(WarehouseDTO::active)
                .map(WarehouseDTO::type)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        kpiNet.setText(distinctTypes + " " + (distinctTypes == 1 ? "Tipo" : "Tipos"));
    }
}
