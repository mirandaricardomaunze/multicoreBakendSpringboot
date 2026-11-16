package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura das ordens de produção para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
@Transactional(readOnly = true)
public class ProductionOrderLookup {

    private final ProductionOrderRepository productionOrderRepository;

    public ProductionOrderLookup(ProductionOrderRepository productionOrderRepository) {
        this.productionOrderRepository = productionOrderRepository;
    }

    /** Ordens de produção da empresa, das mais recentes para as mais antigas. */
    public List<ProductionOrder> listForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return productionOrderRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }
}
