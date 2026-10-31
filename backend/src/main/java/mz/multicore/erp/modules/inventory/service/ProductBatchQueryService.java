package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ProductBatchQueryService {

    private final ProductBatchRepository batchRepository;

    public ProductBatchQueryService(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public List<ProductBatchDTO> findByCompany(Long companyId) {
        return batchRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public List<ProductBatchDTO> findByWarehouse(Long warehouseId) {
        return batchRepository.findByWarehouseId(warehouseId).stream().map(this::toDTO).toList();
    }

    public List<ProductBatchDTO> findExpiringByCompany(Long companyId, LocalDate cutoff) {
        return batchRepository.findExpiringByCompanyId(companyId, cutoff).stream().map(this::toDTO).toList();
    }

    public BigDecimal sumQuantity(Long productId, Long warehouseId) {
        return batchRepository.sumQuantityByProductAndWarehouse(productId, warehouseId);
    }

    public Optional<ProductBatchDTO> findNextFEFO(Long productId, Long warehouseId) {
        List<ProductBatch> available = batchRepository.findAvailableFEFO(productId, warehouseId);
        if (available.isEmpty()) return Optional.empty();
        return Optional.of(toDTO(available.get(0)));
    }

    public ProductBatchDTO toDTO(ProductBatch b) {
        return new ProductBatchDTO(
                b.getId(),
                b.getProduct().getId(),
                b.getProduct().getSku(),
                b.getProduct().getName(),
                b.getWarehouse().getId(),
                b.getWarehouse().getName(),
                b.getBatchNumber(),
                b.getExpirationDate(),
                b.getEntryDate(),
                b.getQuantity(),
                b.getProduct().getBusinessUnit() != null ? b.getProduct().getBusinessUnit().getId() : null,
                b.getProduct().getBusinessUnit() != null ? b.getProduct().getBusinessUnit().getName() : null
        );
    }
}
