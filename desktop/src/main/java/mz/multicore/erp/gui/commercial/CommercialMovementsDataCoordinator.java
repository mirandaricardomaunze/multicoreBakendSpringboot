package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Coordenador de dados, filtros dinâmicos e sumarização analítica para movimentos comerciais.
 */
public final class CommercialMovementsDataCoordinator {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record MovementsSummary(int count, BigDecimal sumQty, BigDecimal sumVal) {
    }

    private CommercialMovementsDataCoordinator() {
    }

    public static void syncWarehouses(
            JComponent parent,
            InventoryApiClient inventoryApiClient,
            Long companyId,
            List<MovimentoDTO> data,
            JComboBox<String> warehouseFilter
    ) {
        if (inventoryApiClient == null || companyId == null || warehouseFilter == null) return;
        UIHelper.loadAsync(parent, () -> inventoryApiClient.getWarehousesByCompany(companyId), warehouses -> {
            if (warehouses == null) return;
            Object currentSel = warehouseFilter.getSelectedItem();
            TreeSet<String> whNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (WarehouseDTO w : warehouses) {
                if (w.name() != null && !w.name().isBlank()) {
                    whNames.add(w.name());
                }
            }
            mergeMovementWarehouses(data, whNames);

            warehouseFilter.removeAllItems();
            warehouseFilter.addItem("Todos os armazéns");
            whNames.forEach(warehouseFilter::addItem);
            if (currentSel != null) {
                warehouseFilter.setSelectedItem(currentSel);
            }
        }, error -> {
            // Silencioso; manterá armazéns dos movimentos
        });
    }

    public static void populateModelAndFilters(
            List<MovimentoDTO> data,
            DefaultTableModel model,
            JComboBox<String> productFilter,
            JComboBox<String> warehouseFilter,
            boolean hasInventoryClient
    ) {
        model.setRowCount(0);
        Object selectedProduct = productFilter == null ? null : productFilter.getSelectedItem();
        Object selectedWh = warehouseFilter == null ? null : warehouseFilter.getSelectedItem();
        TreeSet<String> productNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> whNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (MovimentoDTO movement : data) {
            String artigos = movement.artigos() != null && !movement.artigos().isBlank()
                    ? movement.artigos()
                    : "—";

            if (movement.artigos() != null && !movement.artigos().isBlank()) {
                for (String p : movement.artigos().split(",")) {
                    String clean = p.trim();
                    if (!clean.isEmpty()) productNames.add(clean);
                }
            }

            extractWarehouseNames(movement.armazem(), whNames);
            BigDecimal qty = movement.quantidade() != null ? movement.quantidade() : BigDecimal.ZERO;

            model.addRow(new Object[]{
                    movement.tipo().getLabel(),
                    movement.numero() == null ? "-" : movement.numero(),
                    movement.cliente() == null ? "-" : movement.cliente(),
                    artigos,
                    qty,
                    movement.armazem() != null && !movement.armazem().isBlank() ? movement.armazem() : "—",
                    movement.data() == null ? "-" : movement.data().format(DATE_TIME),
                    UIHelper.humanStatus(movement.estado()),
                    movement.total() == null ? BigDecimal.ZERO : movement.total()
            });
        }

        if (productFilter != null) {
            productFilter.removeAllItems();
            productFilter.addItem("Todos os artigos");
            productNames.forEach(productFilter::addItem);
            if (selectedProduct != null) productFilter.setSelectedItem(selectedProduct);
        }

        if (warehouseFilter != null && (warehouseFilter.getItemCount() <= 1 || !hasInventoryClient)) {
            warehouseFilter.removeAllItems();
            warehouseFilter.addItem("Todos os armazéns");
            whNames.forEach(warehouseFilter::addItem);
            if (selectedWh != null) warehouseFilter.setSelectedItem(selectedWh);
        }
    }

    public static MovementsSummary calculateSummary(
            List<MovimentoDTO> data,
            String query,
            String selectedProduct,
            String selectedWarehouse,
            String selectedType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Map<Integer, String> exact = selectedType != null ? Map.of(0, selectedType) : Map.of();
        int count = 0;
        BigDecimal sumQty = BigDecimal.ZERO;
        BigDecimal sumVal = BigDecimal.ZERO;

        for (MovimentoDTO movement : data) {
            BigDecimal total = movement.total() == null ? BigDecimal.ZERO : movement.total();
            BigDecimal qty = movement.quantidade() != null ? movement.quantidade() : BigDecimal.ZERO;
            String date = movement.data() == null ? "-" : movement.data().format(DATE_TIME);
            String artigos = movement.artigos() != null ? movement.artigos() : "";
            String armazem = movement.armazem() != null ? movement.armazem() : "";

            List<String> cells = List.of(
                    movement.tipo().getLabel(),
                    movement.numero() == null ? "-" : movement.numero(),
                    movement.cliente() == null ? "" : movement.cliente(),
                    artigos,
                    qty.toPlainString(),
                    armazem,
                    date,
                    movement.estado() == null ? "" : movement.estado(),
                    total.toPlainString()
            );

            if (!TableFilter.rowMatches(cells, query, exact)) continue;

            if (selectedProduct != null && !artigos.toLowerCase(Locale.ROOT).contains(selectedProduct.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (selectedWarehouse != null && !armazem.toLowerCase(Locale.ROOT).contains(selectedWarehouse.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (!TableFilter.matchesDateRange(TableFilter.parseCellDate(date), fromDate, toDate)) {
                continue;
            }

            count++;
            sumQty = sumQty.add(qty);
            sumVal = sumVal.add(total);
        }

        return new MovementsSummary(count, sumQty, sumVal);
    }

    public static String formatSummary(MovementsSummary summary) {
        return String.format("%d documento(s) · Qtd Total: %,.2f · Valor Total: %,.2f MT",
                summary.count(), summary.sumQty(), summary.sumVal());
    }

    private static void mergeMovementWarehouses(List<MovimentoDTO> data, TreeSet<String> whNames) {
        if (data == null) return;
        for (MovimentoDTO m : data) {
            extractWarehouseNames(m.armazem(), whNames);
        }
    }

    private static void extractWarehouseNames(String rawArmazem, TreeSet<String> whNames) {
        if (rawArmazem == null || rawArmazem.isBlank() || rawArmazem.equals("—")) return;
        if (rawArmazem.contains("→")) {
            for (String part : rawArmazem.split("→")) {
                String c = part.trim();
                if (!c.isEmpty() && !c.equals("-")) whNames.add(c);
            }
        } else {
            whNames.add(rawArmazem.trim());
        }
    }
}
