package mz.multicore.erp.gui;

import mz.multicore.erp.modules.inventory.dto.StockDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.TreeSet;

/**
 * Filtro e sincronização de seletores da aba Níveis de Stock no {@link StockPanel}.
 */
final class StockLevelsFilter {

    private static final String MASK = "•••";

    private StockLevelsFilter() {
    }

    static void syncCategoryCombo(StockPanel panel) {
        if (panel.stockCategoryCombo == null) return;
        Object selected = panel.stockCategoryCombo.getSelectedItem();
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StockDTO s : panel.stocksList) {
            if (s.categoryName() != null) {
                names.add(s.categoryName());
            }
        }
        panel.stockCategoryCombo.removeAllItems();
        panel.stockCategoryCombo.addItem("Todas as categorias");
        for (String n : names) panel.stockCategoryCombo.addItem(n);
        if (selected != null) panel.stockCategoryCombo.setSelectedItem(selected);
    }

    static void syncBusinessUnitCombo(StockPanel panel) {
        if (panel.stockBusinessUnitCombo == null) return;
        Object selected = panel.stockBusinessUnitCombo.getSelectedItem();
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StockDTO s : panel.stocksList) {
            if (s.businessUnitName() != null && !s.businessUnitName().isBlank()) {
                names.add(s.businessUnitName());
            }
        }
        panel.stockBusinessUnitCombo.removeAllItems();
        panel.stockBusinessUnitCombo.addItem("Todos os negócios");
        for (String n : names) panel.stockBusinessUnitCombo.addItem(n);
        panel.stockBusinessUnitCombo.addItem("Sem Linha (Livre)");
        if (selected != null) panel.stockBusinessUnitCombo.setSelectedItem(selected);
    }

    static void filterStocks(StockPanel panel) {
        panel.stockModel.setRowCount(0);
        panel.stockRowProductIds.clear();
        boolean hide = panel.stockHidden();

        int filterIdx = panel.warehouseFilterCombo.getSelectedIndex();
        Long filterWarehouseId = null;
        if (filterIdx > 0 && (filterIdx - 1) < panel.warehousesList.size()) {
            filterWarehouseId = panel.warehousesList.get(filterIdx - 1).id();
        }

        String query = panel.stockSearchField == null ? "" : panel.stockSearchField.getText().trim().toLowerCase();
        int statusIdx = panel.stockStatusCombo == null ? 0 : panel.stockStatusCombo.getSelectedIndex();
        BigDecimal lowThreshold = BigDecimal.valueOf(5);

        String wantCategory = panel.stockCategoryCombo != null && panel.stockCategoryCombo.getSelectedIndex() > 0
                ? String.valueOf(panel.stockCategoryCombo.getSelectedItem()) : null;

        String wantBusinessUnit = panel.stockBusinessUnitCombo != null && panel.stockBusinessUnitCombo.getSelectedIndex() > 0
                ? String.valueOf(panel.stockBusinessUnitCombo.getSelectedItem()) : null;

        for (StockDTO s : panel.stocksList) {
            if (filterWarehouseId != null && !filterWarehouseId.equals(s.warehouseId())) continue;

            if (wantCategory != null) {
                String cat = s.categoryName() == null ? "" : s.categoryName();
                if (!wantCategory.equalsIgnoreCase(cat)) continue;
            }

            if (wantBusinessUnit != null) {
                if ("Sem Linha (Livre)".equalsIgnoreCase(wantBusinessUnit)) {
                    if (s.businessUnitName() != null && !s.businessUnitName().isBlank()) continue;
                } else {
                    String bu = s.businessUnitName() == null ? "" : s.businessUnitName();
                    if (!wantBusinessUnit.equalsIgnoreCase(bu)) continue;
                }
            }

            if (!query.isEmpty()) {
                String sku = s.sku() == null ? "" : s.sku().toLowerCase();
                String reference = s.reference() == null ? "" : s.reference().toLowerCase();
                String barcode = s.barcode() == null ? "" : s.barcode().toLowerCase();
                String name = s.productName() == null ? "" : s.productName().toLowerCase();
                if (!sku.contains(query) && !reference.contains(query) && !barcode.contains(query) && !name.contains(query)) continue;
            }

            BigDecimal qty = s.quantity() == null ? BigDecimal.ZERO : s.quantity();
            int statusOk = switch (statusIdx) {
                case 1 -> qty.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;        // Em stock
                case 2 -> qty.compareTo(BigDecimal.ZERO) > 0
                        && qty.compareTo(lowThreshold) < 0 ? 1 : 0;                    // Stock baixo
                case 3 -> qty.compareTo(BigDecimal.ZERO) <= 0 ? 1 : 0;       // Sem stock
                default -> 1;
            };
            if (statusOk == 0) continue;

            int unitsPerBox = s.unitsPerBox() <= 0 ? 1 : s.unitsPerBox();
            BigDecimal qtyBoxes = qty.divide(
                    BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
            BigDecimal price = s.unitPrice() == null
                    ? BigDecimal.ZERO : s.unitPrice();

            BigDecimal minStk = s.minStock();
            String estado;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) estado = "ESGOTADO";
            else if (minStk != null && minStk.signum() > 0 && qty.compareTo(minStk) < 0) estado = "BAIXO";
            else estado = "EM STOCK";

            String buName = s.businessUnitName() != null && !s.businessUnitName().isBlank() ? s.businessUnitName() : "—";

            panel.stockModel.addRow(new Object[]{
                    s.barcode() == null ? "—" : s.barcode(),
                    s.reference() == null ? s.sku() : s.reference(),
                    s.productName(),
                    buName,
                    hide ? MASK : String.format("%,.2f", qty),
                    hide ? MASK : String.format("%,.2f", qtyBoxes),
                    price,
                    hide ? MASK : estado
            });
            panel.stockRowProductIds.add(s.productId());
        }
    }
}
