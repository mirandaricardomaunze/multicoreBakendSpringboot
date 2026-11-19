package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.POSCatalogItemDTO;
import mz.multicore.erp.modules.comercial.service.ComercialProductCatalogService;
import mz.multicore.erp.modules.units.dto.UnitBarcodeItemDTO;
import mz.multicore.erp.modules.units.model.ProductUnit;
import mz.multicore.erp.modules.units.repository.ProductUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the barcode of an alternative unit for the POS (SA-C02): the pallet barcode sells the pallet's base
 * quantity. Only units marked for sale count, and only products of the active company.
 */
@Service
public class ProductUnitBarcodeService {

    private final ProductUnitRepository productUnitRepository;
    private final ComercialProductCatalogService catalogService;

    public ProductUnitBarcodeService(ProductUnitRepository productUnitRepository,
                                     ComercialProductCatalogService catalogService) {
        this.productUnitRepository = productUnitRepository;
        this.catalogService = catalogService;
    }

    /** Returns {@code null} when the barcode is not a sellable unit of a product of this company. */
    @Transactional(readOnly = true)
    public UnitBarcodeItemDTO findSellable(Long companyId, String barcode, Long warehouseId) {
        CurrentUserContext.requireCompany(companyId);
        if (barcode == null || barcode.isBlank()) return null;
        ProductUnit unit = productUnitRepository.findByBarcode(barcode.trim()).filter(ProductUnit::isSellable).orElse(null);
        if (unit == null) return null;
        POSCatalogItemDTO item = catalogService.findPOSCatalogItemByProductId(unit.getProduct().getId(), warehouseId);
        if (item == null) return null;
        return new UnitBarcodeItemDTO(item, unit.getUnit().getCode(), unit.getUnit().getName(), unit.getFactorToBase());
    }
}
