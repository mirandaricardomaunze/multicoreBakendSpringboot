package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de lotes para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Quantidades de lote só mudam pelo {@link StockMovementService} (ledger). As entidades devolvidas
 * nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class ProductBatchLookup {

    static final String NOT_FOUND = "Lote não encontrado.";
    static final String NOT_FOUND_IN_COMPANY = "Lote não encontrado nesta empresa.";

    private final ProductBatchRepository batchRepository;

    public ProductBatchLookup(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    /**
     * Lote de um armazém da empresa; {@code "Lote não encontrado."} se não existir e
     * {@code "Lote não encontrado nesta empresa."} se for de outra empresa ou não tiver armazém.
     */
    public ProductBatch requireForCompany(Long batchId, Long companyId) {
        ProductBatch batch = (batchId == null ? Optional.<ProductBatch>empty() : batchRepository.findById(batchId))
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
        if (!belongsTo(batch, companyId)) {
            throw new BusinessRuleException(NOT_FOUND_IN_COMPANY);
        }
        return batch;
    }

    public Optional<ProductBatch> findForCompany(Long batchId, Long companyId) {
        if (batchId == null) {
            return Optional.empty();
        }
        return batchRepository.findById(batchId).filter(b -> belongsTo(b, companyId));
    }

    /** Lote do produto com a validade mais próxima (qualquer armazém). */
    public Optional<ProductBatch> findEarliestExpiring(Long productId) {
        return productId == null ? Optional.empty()
                : batchRepository.findFirstByProductIdOrderByExpirationDateAsc(productId);
    }

    /** Lote do produto com este número (qualquer armazém), o de validade mais próxima. */
    public Optional<ProductBatch> findByBatchNumber(Long productId, String batchNumber) {
        if (productId == null || batchNumber == null) {
            return Optional.empty();
        }
        return batchRepository.findFirstByProductIdAndBatchNumberOrderByExpirationDateAsc(productId, batchNumber);
    }

    /** Lotes com quantidade do produto no armazém, por ordem FEFO. */
    public List<ProductBatch> listAvailableFefo(Long productId, Long warehouseId) {
        return batchRepository.findAvailableFEFO(productId, warehouseId);
    }

    private static boolean belongsTo(ProductBatch batch, Long companyId) {
        return companyId != null && batch.getWarehouse() != null && batch.getWarehouse().getCompany() != null
                && companyId.equals(batch.getWarehouse().getCompany().getId());
    }
}
