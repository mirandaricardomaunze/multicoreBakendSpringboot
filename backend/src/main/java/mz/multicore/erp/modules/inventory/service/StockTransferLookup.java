package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura de transferências entre armazéns para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2). O ciclo de vida da transferência passa pelo
 * {@link StockTransferService}. As entidades devolvidas nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class StockTransferLookup {

    private final StockTransferRepository transferRepository;

    public StockTransferLookup(StockTransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    /** Transferências da empresa, das mais recentes para as mais antigas. */
    public List<StockTransfer> listForCompany(Long companyId) {
        return transferRepository.findByCompanyId(companyId);
    }
}
