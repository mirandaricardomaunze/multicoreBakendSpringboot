package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura dos prémios de objectivos de venda para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2). A liquidação no recibo passa pelo
 * {@link SalesGoalBonusCommandService}.
 */
@Service
@Transactional(readOnly = true)
public class SalesGoalBonusLookup {

    private final SalesGoalBonusRepository bonusRepository;

    public SalesGoalBonusLookup(SalesGoalBonusRepository bonusRepository) {
        this.bonusRepository = bonusRepository;
    }

    /** Prémios aprovados do colaborador que ainda não entraram em nenhum recibo de vencimento. */
    public List<SalesGoalBonus> listApprovedUnsettledForEmployee(Long companyId, Long employeeId) {
        if (companyId == null || employeeId == null) {
            return List.of();
        }
        return bonusRepository.findByCompanyIdAndEmployeeIdAndStatus(companyId, employeeId, BonusStatus.APPROVED)
                .stream().filter(b -> b.getPayslip() == null).toList();
    }
}
