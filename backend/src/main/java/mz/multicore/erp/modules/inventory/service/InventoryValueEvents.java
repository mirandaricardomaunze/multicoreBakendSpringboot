package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.InventoryValueChangedEvent;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Wastes and inventory adjustments change the value of stock without a sale or a purchase: the ledger mirrors them
 * at the product cost, so the goods account follows the physical stock ({@code docs/CONTABILIDADE_SPEC.md}).
 */
final class InventoryValueEvents {

    private InventoryValueEvents() {
    }

    static void publish(ApplicationEventPublisher publisher, Product product, Warehouse warehouse, BigDecimal quantity,
                        StockMovementType type, StockMovement movement, String description) {
        if (publisher == null || movement == null || warehouse == null || warehouse.getCompany() == null) return;
        if (type != StockMovementType.WASTE && type != StockMovementType.ADJUSTMENT) return;
        BigDecimal cost = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();
        BigDecimal value = cost.multiply(quantity.abs());
        if (value.signum() == 0) return;
        boolean out = quantity.signum() < 0;
        publisher.publishEvent(new InventoryValueChangedEvent(warehouse.getCompany().getId(),
                type == StockMovementType.WASTE ? JournalSource.STOCK_WASTE : JournalSource.STOCK_ADJUSTMENT,
                movement.getId(), description == null ? product.getName() : description, LocalDate.now(),
                out ? value : BigDecimal.ZERO, out ? BigDecimal.ZERO : value, null));
    }
}
