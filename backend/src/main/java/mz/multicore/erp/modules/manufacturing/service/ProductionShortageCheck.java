package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderLine;

import java.math.BigDecimal;

/** Checks every raw material before a production order consumes anything, so a shortage never leaves it half done. */
final class ProductionShortageCheck {

    private ProductionShortageCheck() {
    }

    static void assertAvailable(StockLookup stockLookup, ProductionOrder order) {
        if (order.getSourceWarehouse() == null) return;
        Long warehouseId = order.getSourceWarehouse().getId();
        for (ProductionOrderLine line : order.getLines()) {
            if (!line.getComponentProduct().isStockTracked()) continue;
            BigDecimal available = stockLookup.findForReservation(line.getComponentProduct().getId(), warehouseId)
                    .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
            if (available.compareTo(line.getPlannedQuantity()) < 0) {
                throw new BusinessRuleException("Falta " + line.getComponentProduct().getName() + ": a ordem precisa de "
                        + DisplayNumbers.qty(line.getPlannedQuantity()) + " e há " + DisplayNumbers.qty(available)
                        + " em " + order.getSourceWarehouse().getName() + ".");
            }
        }
    }
}
