package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProductBatchService {

    private final ProductBatchMutationService mutationService;
    private final ProductBatchQueryService queryService;

    @Autowired
    public ProductBatchService(ProductBatchMutationService mutationService,
                               ProductBatchQueryService queryService) {
        this.mutationService = mutationService;
        this.queryService = queryService;
    }

    public ProductBatchService(ProductBatchRepository batchRepository) {
        this(new ProductBatchMutationService(batchRepository),
             new ProductBatchQueryService(batchRepository));
    }

    @Transactional
    public ProductBatch addToBatch(Product product, Warehouse warehouse, String batchNumber,
                                   LocalDate expirationDate, BigDecimal quantity) {
        return mutationService.addToBatch(product, warehouse, batchNumber, expirationDate, quantity);
    }

    @Transactional
    public List<BatchConsumption> consumeFEFO(Product product, Warehouse warehouse, BigDecimal quantity) {
        return mutationService.consumeFEFO(product, warehouse, quantity);
    }

    @Transactional
    public ProductBatch ensureLegacyBatch(Product product, Warehouse warehouse, BigDecimal quantity) {
        return mutationService.ensureLegacyBatch(product, warehouse, quantity);
    }

    @Transactional
    public void materialiseLegacyIfNeeded(Product product, Warehouse warehouse, BigDecimal stockQuantity) {
        mutationService.materialiseLegacyIfNeeded(product, warehouse, stockQuantity);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findByCompany(Long companyId) {
        return queryService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findByWarehouse(Long warehouseId) {
        return queryService.findByWarehouse(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findExpiringByCompany(Long companyId, LocalDate cutoff) {
        return queryService.findExpiringByCompany(companyId, cutoff);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumQuantity(Long productId, Long warehouseId) {
        return queryService.sumQuantity(productId, warehouseId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductBatchDTO> findNextFEFO(Long productId, Long warehouseId) {
        return queryService.findNextFEFO(productId, warehouseId);
    }

    public ProductBatchDTO toDTO(ProductBatch b) {
        return queryService.toDTO(b);
    }

    public record BatchConsumption(ProductBatch batch, BigDecimal quantity) {}
}
