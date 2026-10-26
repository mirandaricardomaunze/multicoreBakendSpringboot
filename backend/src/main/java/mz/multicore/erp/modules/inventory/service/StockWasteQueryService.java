package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultas analíticas e mapeamento DTO de quebras de stock.
 */
@Service
public class StockWasteQueryService {

    private final StockWasteRepository wasteRepository;

    public StockWasteQueryService(StockWasteRepository wasteRepository) {
        this.wasteRepository = wasteRepository;
    }

    @Transactional(readOnly = true)
    public List<StockWasteDTO> findByCompany(Long companyId) {
        return wasteRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(StockWasteQueryService::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockWasteDTO> findByCompanyAndStatus(Long companyId, WasteStatus status) {
        return wasteRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status)
                .stream()
                .map(StockWasteQueryService::toDTO)
                .toList();
    }

    public static StockWasteDTO toDTO(StockWaste w) {
        return new StockWasteDTO(
                w.getId(),
                w.getCompany() != null ? w.getCompany().getId() : null,
                w.getWarehouse() != null ? w.getWarehouse().getId() : null,
                w.getWarehouse() != null ? w.getWarehouse().getName() : "-",
                w.getProduct() != null ? w.getProduct().getId() : null,
                w.getProduct() != null ? w.getProduct().getSku() : "-",
                w.getProduct() != null ? w.getProduct().getName() : "-",
                w.getBatch() != null ? w.getBatch().getId() : null,
                w.getBatch() != null ? w.getBatch().getBatchNumber() : null,
                w.getQuantity(),
                w.getUnitCost(),
                w.getTotalCost(),
                w.getReason(),
                w.getStatus(),
                w.getNotes(),
                w.getRegisteredBy(),
                w.getApprovedBy(),
                w.getCreatedAt(),
                w.getApprovedAt(),
                w.getStockMovement() != null ? w.getStockMovement().getId() : null,
                w.getProduct() != null && w.getProduct().getBusinessUnit() != null ? w.getProduct().getBusinessUnit().getId() : null,
                w.getProduct() != null && w.getProduct().getBusinessUnit() != null ? w.getProduct().getBusinessUnit().getName() : null
        );
    }
}
