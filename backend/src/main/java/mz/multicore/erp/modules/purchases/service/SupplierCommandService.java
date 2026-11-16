package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escrita de fornecedores pedida por outros módulos (seeders), sem expor o repositório das compras
 * (docs/MODULE_DECOUPLING_SPEC.md §2). Leituras passam por {@link SupplierLookup}.
 */
@Service
@Transactional
public class SupplierCommandService {

    private final SupplierRepository supplierRepository;

    public SupplierCommandService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }
}
