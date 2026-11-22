package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.company.service.CompanyMovementProbe;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Component;

/** A stock movement in any of the company's warehouses freezes its base currency (FX-01). */
@Component
class StockMovementProbe implements CompanyMovementProbe {

    private final StockMovementRepository movements;

    StockMovementProbe(StockMovementRepository movements) {
        this.movements = movements;
    }

    @Override
    public boolean hasMovements(Long companyId) {
        return movements.existsByWarehouseCompanyId(companyId);
    }
}
