package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Razão físico do stock em trânsito: grava os movimentos {@code TRANSFER} de cada perna (saída,
 * entrada, devolução à origem) e ajusta o saldo com o bloqueio pessimista de reserva.
 * Ver {@code docs/STOCK_TRANSFER_TRANSIT_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Component
public class StockTransitLedger {

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockTransitLedger(StockRepository stockRepository, StockMovementRepository stockMovementRepository) {
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    /** Saldo agregado actual (ou {@code null} se não existir linha de stock) para materializar lotes legados. */
    public BigDecimal currentQuantity(Product product, Warehouse warehouse) {
        return stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity)
                .orElse(null);
    }

    public void recordMovement(Product product, Warehouse warehouse, BigDecimal qty,
                               ProductBatch batch, StockTransfer transfer, String leg) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(qty);
        movement.setMovementType(StockMovementType.TRANSFER);
        movement.setBatchNumber(batch.getBatchNumber());
        movement.setBatch(batch);
        movement.setDescription("Transferência " + transfer.getTransferNumber()
                + " — " + transfer.getOriginWarehouse().getName()
                + " → " + transfer.getDestinationWarehouse().getName() + " (" + leg + ")");
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(CurrentUserContext.getUsername());
        stockMovementRepository.save(movement);
    }

    public void adjustStock(Product product, Warehouse warehouse, BigDecimal delta) {
        Stock stock = stockRepository.findForReservation(product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    Stock s = new Stock();
                    s.setProduct(product);
                    s.setWarehouse(warehouse);
                    s.setQuantity(BigDecimal.ZERO);
                    return s;
                });
        stock.setQuantity(stock.getQuantity().add(delta));
        stockRepository.save(stock);
    }
}
