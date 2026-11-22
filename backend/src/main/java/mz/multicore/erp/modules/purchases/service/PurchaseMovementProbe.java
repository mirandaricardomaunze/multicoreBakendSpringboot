package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.company.service.CompanyMovementProbe;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Component;

/** Purchases (and imported supplier opening balances) freeze the company's base currency (FX-01). */
@Component
class PurchaseMovementProbe implements CompanyMovementProbe {

    private final PurchaseRepository purchaseRepository;

    PurchaseMovementProbe(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    public boolean hasMovements(Long companyId) {
        return purchaseRepository.existsByCompanyId(companyId);
    }
}
