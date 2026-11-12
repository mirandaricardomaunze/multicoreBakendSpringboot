package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de saldos de stock para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Só lê: qualquer alteração de saldo passa pelo {@link StockMovementService} (ledger), que regista o
 * movimento, os lotes e o saldo em conjunto. As entidades devolvidas servem só para uso dentro do backend
 * e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class StockLookup {

    private final StockRepository stockRepository;

    public StockLookup(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /** Saldo do produto no armazém, se existir registo. */
    public Optional<Stock> find(Long productId, Long warehouseId) {
        if (productId == null || warehouseId == null) {
            return Optional.empty();
        }
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId);
    }

    /** Quantidade do produto no armazém; zero quando não há registo. */
    public BigDecimal quantityOf(Long productId, Long warehouseId) {
        return find(productId, warehouseId)
                .map(Stock::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    /** Saldos de todos os armazéns da empresa (com produto, armazém e empresa carregados). */
    public List<Stock> listForCompany(Long companyId) {
        return stockRepository.findByWarehouseCompanyId(companyId);
    }

    public List<Stock> listForWarehouse(Long warehouseId) {
        return stockRepository.findByWarehouseId(warehouseId);
    }

    public List<Stock> listForProduct(Long productId) {
        return stockRepository.findByProductId(productId);
    }
}
