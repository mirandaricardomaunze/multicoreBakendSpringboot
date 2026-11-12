package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de leitura de quebras de stock para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Registo e aprovação passam pelo {@link StockWasteService}. As entidades devolvidas servem só para uso
 * dentro do backend e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class StockWasteLookup {

    private final StockWasteRepository wasteRepository;

    public StockWasteLookup(StockWasteRepository wasteRepository) {
        this.wasteRepository = wasteRepository;
    }

    /** Quebras da empresa, das mais recentes para as mais antigas. */
    public List<StockWaste> listForCompany(Long companyId) {
        return wasteRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /** Quebras da empresa num estado, das mais recentes para as mais antigas. */
    public List<StockWaste> listForCompanyWithStatus(Long companyId, WasteStatus status) {
        return wasteRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status);
    }

    /** Quebras da empresa criadas no intervalo [from, to], das mais recentes para as mais antigas. */
    public List<StockWaste> listForCompanyInPeriod(Long companyId, LocalDateTime from, LocalDateTime to) {
        return wasteRepository.findByCompanyIdAndPeriod(companyId, from, to);
    }

    /** Todas as quebras de todas as empresas (auditoria forense global, sem empresa activa). */
    public List<StockWaste> listAll() {
        return wasteRepository.findAll();
    }
}
