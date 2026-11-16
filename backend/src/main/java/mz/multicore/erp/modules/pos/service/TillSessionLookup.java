package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de leitura das sessões de caixa do POS (e dos seus movimentos) para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2). Abertura, fecho e movimentos passam pelo
 * {@link PosSessionAndShiftService}. As entidades devolvidas servem só para uso dentro do backend.
 */
@Service
@Transactional(readOnly = true)
public class TillSessionLookup {

    private final TillSessionRepository sessionRepository;
    private final TillMovementRepository movementRepository;

    public TillSessionLookup(TillSessionRepository sessionRepository, TillMovementRepository movementRepository) {
        this.sessionRepository = sessionRepository;
        this.movementRepository = movementRepository;
    }

    /** Todas as sessões de caixa da empresa. */
    public List<TillSession> listForCompany(Long companyId) {
        return sessionRepository.findByCompanyId(companyId);
    }

    /** Movimentos de caixa da empresa com data no intervalo [from, to]. */
    public List<TillMovement> listMovementsForCompany(Long companyId, LocalDateTime from, LocalDateTime to) {
        return movementRepository.findByTillSessionCompanyIdAndMovementDateBetween(companyId, from, to);
    }
}
