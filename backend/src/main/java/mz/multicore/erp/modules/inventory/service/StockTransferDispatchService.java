package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.StockTransferLineBatch;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Primeiro passo do stock em trânsito: retira o stock da origem por FEFO, regista as parcelas
 * despachadas por lote e coloca a guia em {@code IN_TRANSIT}. Nunca toca no destino.
 */
@Service
public class StockTransferDispatchService {

    private final ProductBatchService productBatchService;
    private final StockTransitLedger ledger;
    private final AuditLogService auditLogService;

    public StockTransferDispatchService(ProductBatchService productBatchService,
                                        StockTransitLedger ledger,
                                        AuditLogService auditLogService) {
        this.productBatchService = productBatchService;
        this.ledger = ledger;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void dispatch(StockTransfer transfer, String dispatcher) {
        Warehouse origin = transfer.getOriginWarehouse();
        for (StockTransferLine line : transfer.getLines()) {
            Product product = line.getProduct();
            BigDecimal quantity = line.getQuantity();

            productBatchService.materialiseLegacyIfNeeded(product, origin, ledger.currentQuantity(product, origin));
            List<ProductBatchService.BatchConsumption> debits =
                    productBatchService.consumeFEFO(product, origin, quantity);

            line.getTransitBatches().clear();
            StringBuilder summary = new StringBuilder();
            for (ProductBatchService.BatchConsumption d : debits) {
                ProductBatch source = d.batch();
                StockTransferLineBatch parcel = new StockTransferLineBatch();
                parcel.setLine(line);
                parcel.setBatchNumber(source.getBatchNumber());
                parcel.setExpirationDate(source.getExpirationDate());
                parcel.setQuantity(d.quantity());
                line.getTransitBatches().add(parcel);

                ledger.recordMovement(product, origin, d.quantity().negate(), source, transfer, "saída");
                if (summary.length() > 0) summary.append(", ");
                summary.append(source.getBatchNumber());
            }
            ledger.adjustStock(product, origin, quantity.negate());
            line.setBatchNumber(summary.toString());
        }

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setDispatchedAt(LocalDateTime.now());
        transfer.setDispatchedBy(dispatcher);
        auditLogService.logCurrent("STOCK_TRANSFER_DISPATCH",
                "Guia " + transfer.getTransferNumber() + " despachada: stock em trânsito de '"
                        + origin.getName() + "' para '" + transfer.getDestinationWarehouse().getName() + "'.");
    }
}
