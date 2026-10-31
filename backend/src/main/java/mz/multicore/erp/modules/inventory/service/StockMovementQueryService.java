package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de consultas e projeção DTO de movimentos de stock.
 */
@Service
public class StockMovementQueryService {

    private final StockMovementRepository stockMovementRepository;

    @Autowired
    public StockMovementQueryService(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockMovementRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> findMovementsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockMovementRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDTO> findMovementsPageByCompany(
            Long companyId, Integer page, Integer size) {
        CurrentUserContext.requireCompany(companyId);
        return PageResponseMapper.from(
                stockMovementRepository.findPageByCompanyId(companyId, PageQuery.of(page, size)),
                this::toDTO);
    }

    public StockMovementDTO toDTO(StockMovement m) {
        if (m == null) return null;
        return new StockMovementDTO(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getName(),
                m.getWarehouse().getId(),
                m.getWarehouse().getName(),
                m.getQuantity(),
                m.getMovementType() != null ? m.getMovementType().name() : null,
                m.getBatchNumber(),
                m.getSerialNumber(),
                m.getDescription(),
                m.getMovementDate(),
                m.getProduct().getBusinessUnit() != null ? m.getProduct().getBusinessUnit().getId() : null,
                m.getProduct().getBusinessUnit() != null ? m.getProduct().getBusinessUnit().getName() : null
        );
    }
}
