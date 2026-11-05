package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockAlertDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Serviço especializado de consultas de saldos de stock, alertas de ruptura/validade e catálogos multi-armazém.
 */
@Service
public class StockQueryService {

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductBatchService productBatchService;
    private final WarehouseService warehouseService;

    public StockQueryService(
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            ProductBatchService productBatchService,
            WarehouseService warehouseService
    ) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.productBatchService = productBatchService;
        this.warehouseService = warehouseService;
    }

    @Transactional(readOnly = true)
    public List<StockAlertDTO> findOutOfStockProducts(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Stock s : stockRepository.findByWarehouseCompanyId(companyId)) {
            if (s.getProduct() == null) continue;
            totals.merge(s.getProduct().getId(),
                    s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity(), BigDecimal::add);
        }
        List<StockAlertDTO> out = new ArrayList<>();
        for (Product p : productRepository.findDistinctByCompaniesIdOrderByName(companyId)) {
            if (!p.isStockTracked()) continue;
            BigDecimal total = totals.getOrDefault(p.getId(), BigDecimal.ZERO);
            if (total.signum() <= 0) {
                out.add(new StockAlertDTO(
                        p.getId(), p.getSku(), p.getName(), total,
                        p.getBusinessUnit() != null ? p.getBusinessUnit().getId() : null,
                        p.getBusinessUnit() != null ? p.getBusinessUnit().getName() : null));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Set<Long> getInStockProductIdsForSale(Long companyId) {
        return getInStockProductIdsForSale(companyId, null);
    }

    @Transactional(readOnly = true)
    public Set<Long> getInStockProductIdsForSale(Long companyId, Long warehouseId) {
        CurrentUserContext.requireCompany(companyId);
        Set<Long> ids = new HashSet<>();
        List<Warehouse> warehouses = warehouseService.resolveSalesWarehouses(companyId, warehouseId);
        for (Warehouse w : warehouses) {
            for (Stock s : stockRepository.findByWarehouseId(w.getId())) {
                if (s.getProduct() != null && s.getQuantity() != null && s.getQuantity().signum() > 0) {
                    ids.add(s.getProduct().getId());
                }
            }
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getStockQuantitiesForSale(Long companyId, Long warehouseId) {
        CurrentUserContext.requireCompany(companyId);
        Map<Long, BigDecimal> map = new HashMap<>();
        List<Warehouse> warehouses = warehouseService.resolveSalesWarehouses(companyId, warehouseId);
        for (Warehouse w : warehouses) {
            for (Stock s : stockRepository.findByWarehouseId(w.getId())) {
                if (s.getProduct() != null && s.getQuantity() != null) {
                    map.merge(s.getProduct().getId(), s.getQuantity(), BigDecimal::add);
                }
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockRepository.findByWarehouseCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksByWarehouse(Long warehouseId) {
        return stockRepository.findByWarehouseId(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<StockDTO> findStocksByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockRepository.findByWarehouseCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<StockDTO> findStocksByWarehouse(Long warehouseId) {
        warehouseService.requireWarehouseCompany(warehouseId);
        return stockRepository.findByWarehouseId(warehouseId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getPhysicalQuantity(Long productId, Long warehouseId) {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
    }

    @Transactional
    public BigDecimal lockAndGetPhysicalQuantity(Long productId, Long warehouseId) {
        return stockRepository.findForReservation(productId, warehouseId)
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findBatchesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return productBatchService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findExpiringBatchesByCompany(Long companyId, LocalDate before) {
        CurrentUserContext.requireCompany(companyId);
        if (before == null) {
            throw new BusinessRuleException("A data limite de validade é obrigatória.");
        }
        return productBatchService.findExpiringByCompany(companyId, before);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findBatchesByWarehouse(Long warehouseId) {
        warehouseService.requireWarehouseCompany(warehouseId);
        return productBatchService.findByWarehouse(warehouseId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductBatchDTO> findNextFEFO(Long productId, Long warehouseId) {
        warehouseService.requireWarehouseCompany(warehouseId);
        return productBatchService.findNextFEFO(productId, warehouseId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findExpiringBatches(Long companyId, int daysAhead) {
        CurrentUserContext.requireCompany(companyId);
        return productBatchService.findExpiringByCompany(companyId, LocalDate.now().plusDays(daysAhead));
    }

    public StockDTO toDTO(Stock s) {
        var product = s.getProduct();
        return new StockDTO(
                s.getId(),
                product.getId(),
                product.getSku(),
                product.getReference(),
                product.getBarcode(),
                product.getName(),
                s.getWarehouse().getId(),
                s.getWarehouse().getName(),
                s.getQuantity(),
                product.getMinStock() != null ? product.getMinStock() : BigDecimal.ZERO,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO,
                product.getUnitsPerBox(),
                product.getBusinessUnit() != null ? product.getBusinessUnit().getId() : null,
                product.getBusinessUnit() != null ? product.getBusinessUnit().getName() : null,
                product.isTracksExpiry()
        );
    }
}
