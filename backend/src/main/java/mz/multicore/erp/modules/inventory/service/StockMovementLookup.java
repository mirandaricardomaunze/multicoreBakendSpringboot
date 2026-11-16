package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura do ledger de movimentos de stock para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2). Movimentos só se registam pelo {@link StockMovementService}.
 */
@Service
@Transactional(readOnly = true)
public class StockMovementLookup {

    private final StockMovementRepository movementRepository;

    public StockMovementLookup(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    /** Movimentos dos armazéns da empresa, dos mais recentes para os mais antigos. */
    public List<StockMovement> listForCompany(Long companyId) {
        return movementRepository.findByCompanyId(companyId);
    }
}
