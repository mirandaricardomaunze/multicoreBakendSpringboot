package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.model.TransferStatus;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validador de regras de negócio, permissões e disponibilidade de stock para transferências.
 */
@Component
public class StockTransferValidator {

    private final StockRepository stockRepository;
    private final ProductBatchService productBatchService;

    public StockTransferValidator(StockRepository stockRepository, ProductBatchService productBatchService) {
        this.stockRepository = stockRepository;
        this.productBatchService = productBatchService;
    }

    public void validateAvailability(StockTransfer transfer) {
        Map<Long, BigDecimal> requested = new LinkedHashMap<>();
        Map<Long, Product> products = new LinkedHashMap<>();
        for (StockTransferLine line : transfer.getLines()) {
            Long productId = line.getProduct().getId();
            requested.merge(productId, line.getQuantity(), BigDecimal::add);
            products.putIfAbsent(productId, line.getProduct());
        }
        for (var entry : requested.entrySet()) {
            Product product = products.get(entry.getKey());
            BigDecimal available = availableForExit(entry.getKey(), transfer.getOriginWarehouse().getId());
            if (available.compareTo(entry.getValue()) < 0) {
                throw new BusinessRuleException(String.format(
                        "Stock insuficiente de '%s' no armazém de origem '%s'. Requerido: %s, Disponível: %s",
                        product.getName(), transfer.getOriginWarehouse().getName(), entry.getValue(), available));
            }
        }
    }

    public BigDecimal availableForExit(Long productId, Long warehouseId) {
        BigDecimal fromBatches = productBatchService.sumQuantity(productId, warehouseId);
        final BigDecimal batches = fromBatches == null ? BigDecimal.ZERO : fromBatches;
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(stock -> stock.getQuantity().max(batches))
                .orElse(batches);
    }

    public void requireApproverRole() {
        PermissionGuard.requireManagerOrAdmin("aprovar ou rejeitar guias de transferência");
    }

    public void requireDraft(StockTransfer transfer, String operation) {
        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new BusinessRuleException("Apenas guias em rascunho podem ser " + operation
                    + ". Estado atual: " + transfer.getStatus().getLabel());
        }
    }

    public String requiredDriver(String driverName, String responsible) {
        String driver = blankToNull(driverName) != null ? blankToNull(driverName) : blankToNull(responsible);
        if (driver == null) {
            throw new BusinessRuleException("O nome do motorista é obrigatório para emitir a guia de transferência.");
        }
        return driver;
    }

    public String requiredPlate(String vehiclePlate, String vehicle) {
        String plate = blankToNull(vehiclePlate) != null ? blankToNull(vehiclePlate) : blankToNull(vehicle);
        if (plate == null) {
            throw new BusinessRuleException("A matrícula do veículo é obrigatória para emitir a guia de transferência.");
        }
        return plate;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
