package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Liquidação de prémios pedida pela emissão de recibos do RH, sem lhe expor o
 * {@link SalesGoalBonusRepository} (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
public class SalesGoalBonusCommandService {

    private final SalesGoalBonusRepository bonusRepository;

    public SalesGoalBonusCommandService(SalesGoalBonusRepository bonusRepository) {
        this.bonusRepository = bonusRepository;
    }

    /** Associa os prémios ao recibo emitido e marca-os como pagos. */
    @Transactional
    public void settleOnPayslip(List<SalesGoalBonus> bonuses, Payslip payslip) {
        for (SalesGoalBonus bonus : bonuses) {
            bonus.setPayslip(payslip);
            bonus.setStatus(BonusStatus.PAID);
            bonusRepository.save(bonus);
        }
    }
}
