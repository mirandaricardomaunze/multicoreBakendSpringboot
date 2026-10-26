package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.WasteStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Provedor especializado de alertas operacionais de inventário, validades, quebras e trânsito.
 */
final class NotificationInventoryAlertProvider {

    private NotificationInventoryAlertProvider() {}

    static void addLowStock(List<NotificationItem> items, Long companyId,
                            InventoryApiClient inventoryApiClient, BigDecimal defaultLowStock) {
        if (inventoryApiClient == null || companyId == null) return;
        for (StockDTO stock : inventoryApiClient.getStocksByCompany(companyId)) {
            BigDecimal threshold = stock.minStock() != null && stock.minStock().signum() > 0
                    ? stock.minStock() : defaultLowStock;
            if (stock.quantity() == null || stock.quantity().compareTo(threshold) >= 0) continue;
            String warehouse = stock.warehouseName() == null ? "Armazém" : stock.warehouseName();
            String detail = warehouse + " — disponível " + stock.quantity() + ", mínimo " + threshold;
            items.add(new NotificationItem("Stock", "Stock baixo: " + stock.productName(), detail,
                    "Repor stock", "stock", 2));
        }
    }

    static void addExpiries(List<NotificationItem> items, Long companyId,
                            InventoryApiClient inventoryApiClient, int expiryAlertDays) {
        if (inventoryApiClient == null || companyId == null) return;
        LocalDate today = LocalDate.now();
        for (ProductBatchDTO batch : inventoryApiClient.findExpiringBatches(companyId, expiryAlertDays)) {
            if (batch.expirationDate() == null) continue;
            boolean expired = batch.expirationDate().isBefore(today);
            String batchNumber = batch.batchNumber() == null ? "sem número" : batch.batchNumber();
            String detail = "Lote " + batchNumber + " · "
                    + (batch.warehouseName() == null ? "Armazém" : batch.warehouseName());
            items.add(new NotificationItem("Validades",
                    (expired ? "Lote vencido: " : "Lote a vencer: ") + batch.productName(),
                    detail, batch.expirationDate().format(UIHelper.DATE_FMT), "stock", expired ? 3 : 2));
        }
    }

    static void addStockWasteAlerts(List<NotificationItem> items, Long companyId,
                                   StockWasteApiClient stockWasteApiClient) {
        if (stockWasteApiClient == null || companyId == null) return;
        try {
            List<StockWasteDTO> pending = stockWasteApiClient.findByCompany(companyId, WasteStatus.PENDING_APPROVAL);
            if (pending != null && !pending.isEmpty()) {
                BigDecimal totalLoss = pending.stream()
                        .map(StockWasteDTO::totalCost)
                        .filter(v -> v != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String title = String.format("%d Quebra(s) de Stock pendente(s) de validação", pending.size());
                String detail = String.format("Perda estimada: %s. Alçada gerencial requerida.", UIHelper.formatMzn(totalLoss));
                items.add(new NotificationItem("Quebras de Stock", title, detail, "Aprovação pendente", "stock_waste", 2));
            }
        } catch (Exception ignored) {
            // Degrada suavemente se API estiver indisponível
        }
    }

    static void addInTransitOverdue(List<NotificationItem> items, Long companyId,
                                   StockTransferApiClient stockTransferApiClient, long inTransitOverdueHours) {
        if (stockTransferApiClient == null || companyId == null) return;
        try {
            List<StockTransferDTO> inTransitList = stockTransferApiClient.findInTransit(companyId, null);
            if (inTransitList == null || inTransitList.isEmpty()) return;
            LocalDateTime now = LocalDateTime.now();
            for (StockTransferDTO t : inTransitList) {
                LocalDateTime dispatched = t.dispatchedAt() != null ? t.dispatchedAt() : t.transferDate();
                if (dispatched == null) continue;
                long hours = Duration.between(dispatched, now).toHours();
                if (hours >= inTransitOverdueHours) {
                    long days = hours / 24;
                    String timeDesc = days > 1 ? days + " dias" : hours + " horas";
                    String driver = t.driverName() != null && !t.driverName().isBlank()
                            ? t.driverName()
                            : (t.responsible() != null && !t.responsible().isBlank() ? t.responsible() : "Transportador");
                    String detail = String.format("%s → %s · Despachada há %s por %s · Recepção pendente no destino",
                            t.originWarehouseName(), t.destinationWarehouseName(), timeDesc, driver);
                    int priority = days >= 5 ? 3 : 2;
                    items.add(new NotificationItem(
                            "Stock em Trânsito",
                            "Guia " + t.transferNumber() + " em trânsito há " + timeDesc,
                            detail,
                            dispatched.format(UIHelper.DATE_FMT),
                            "stock_transfers",
                            priority
                    ));
                }
            }
        } catch (Exception ignored) {
            // Degrada suavemente em caso de indisponibilidade da API
        }
    }
}
