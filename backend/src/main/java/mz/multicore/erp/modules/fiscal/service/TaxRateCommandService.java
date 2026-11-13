package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gravação de taxas de imposto pedida por outros módulos (seeders de referência e demonstração),
 * sem lhes expor o {@link TaxRateRepository} (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
public class TaxRateCommandService {

    private final TaxRateRepository taxRateRepository;

    public TaxRateCommandService(TaxRateRepository taxRateRepository) {
        this.taxRateRepository = taxRateRepository;
    }

    @Transactional
    public TaxRate saveTaxRate(TaxRate taxRate) {
        return taxRateRepository.save(taxRate);
    }
}
