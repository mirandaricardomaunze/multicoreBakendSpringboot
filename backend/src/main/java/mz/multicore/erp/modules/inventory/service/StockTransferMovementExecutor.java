package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Executor especializado na movimentação física atómica de stock entre armazéns,
 * alocação FEFO de lotes e persistência do histórico em StockMovement.
 */
@Component
public class StockTransferMovementExecutor {

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductBatchService productBatchService;

    public StockTransferMovementExecutor(
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            ProductBatchService productBatchService
    ) {
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productBatchService = productBatchService;
    }

    public void dispatchOrMove(StockTransfer transfer, String actor, StockTransferTransitService transitService) {
        if (transfer.isRequiresReceipt()) {
            if (transitService == null) {
                throw new BusinessRuleException("O stock em trânsito não está disponível neste momento.");
            }
            transitService.dispatch(transfer, actor);
            return;
        }
        Warehouse origin = transfer.getOriginWarehouse();
        Warehouse destination = transfer.getDestinationWarehouse();
        for (StockTransferLine line : transfer.getLines()) {
            String batchSummary = moveProduct(transfer, line.getProduct(), origin, destination, line.getQuantity());
            line.setBatchNumber(batchSummary);
        }
    }

    public String moveProduct(StockTransfer transfer, Product product, Warehouse origin,
                              Warehouse destination, BigDecimal quantity) {
        productBatchService.materialiseLegacyIfNeeded(product, origin,
                stockRepository.findByProductIdAndWarehouseId(product.getId(), origin.getId())
                        .map(Stock::getQuantity)
                        .orElse(null));

        List<ProductBatchService.BatchConsumption> debits =
                productBatchService.consumeFEFO(product, origin, quantity);

        StringBuilder summary = new StringBuilder();
        for (ProductBatchService.BatchConsumption d : debits) {
            ProductBatch sourceBatch = d.batch();
            BigDecimal moved = d.quantity();

            productBatchService.addToBatch(
                    product, destination, sourceBatch.getBatchNumber(), sourceBatch.getExpirationDate(), moved);

            saveMovement(product, origin, moved.negate(), sourceBatch, transfer);
            saveMovement(product, destination, moved, sourceBatch, transfer);

            if (!summary.isEmpty()) summary.append(", ");
            summary.append(sourceBatch.getBatchNumber());
        }

        adjustStock(product, origin, quantity.negate());
        adjustStock(product, destination, quantity);
        return summary.toString();
    }

    public void saveMovement(Product product, Warehouse warehouse, BigDecimal qty,
                             ProductBatch batch, StockTransfer transfer) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(qty);
        movement.setMovementType(StockMovementType.TRANSFER);
        movement.setBatchNumber(batch.getBatchNumber());
        movement.setBatch(batch);
        movement.setDescription("Transferência " + transfer.getTransferNumber()
                + " — " + transfer.getOriginWarehouse().getName()
                + " → " + transfer.getDestinationWarehouse().getName());
        movement.setMovementDate(transfer.getTransferDate());
        movement.setCreatedBy(transfer.getCreatedBy());
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
