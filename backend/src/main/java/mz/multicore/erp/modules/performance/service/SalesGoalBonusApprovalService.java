package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.performance.dto.AdjustBonusRequest;
import mz.multicore.erp.modules.performance.dto.ApproveBonusRequest;
import mz.multicore.erp.modules.performance.dto.SalesGoalBonusDTO;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SalesGoalBonusApprovalService {

    private final SalesGoalBonusRepository bonusRepository;
    private final AuditLogService auditLogService;

    public SalesGoalBonusApprovalService(SalesGoalBonusRepository bonusRepository, AuditLogService auditLogService) {
        this.bonusRepository = bonusRepository;
        this.auditLogService = auditLogService;
    }

    public List<SalesGoalBonusDTO> listBonuses(Long companyId, BonusStatus status) {
        List<SalesGoalBonus> list = status != null
                ? bonusRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status)
                : bonusRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        return list.stream().map(this::toDTO).toList();
    }

    public SalesGoalBonusDTO approveBonus(Long id, ApproveBonusRequest request) {
        SalesGoalBonus bonus = bonusRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo de prémio não encontrado."));
        if (bonus.getStatus() == BonusStatus.PAID) {
            throw new BusinessRuleException("Não é permitido alterar um prémio já pago.");
        }

        BigDecimal amount = (request != null && request.approvedAmount() != null && request.approvedAmount().compareTo(BigDecimal.ZERO) >= 0)
                ? request.approvedAmount()
                : bonus.getCalculatedAmount();

        bonus.setApprovedAmount(amount);
        if (request != null && request.justification() != null && !request.justification().isBlank()) {
            bonus.setJustification(request.justification().trim());
        }
        bonus.setStatus(BonusStatus.APPROVED);
        bonus.setApprovedBy(CurrentUserContext.getUsername());

        SalesGoalBonus saved = bonusRepository.save(bonus);
        auditLogService.logEvent(CurrentUserContext.getUsername(), saved.getCompany().getId(), "BONUS_APPROVED",
                String.format("Prémio de %s MZN aprovado para %s na meta '%s'",
                        saved.getApprovedAmount(),
                        saved.getEmployee() != null ? saved.getEmployee().getName() : "Equipa",
                        saved.getGoal().getName()));
        return toDTO(saved);
    }

    public SalesGoalBonusDTO adjustBonus(Long id, AdjustBonusRequest request) {
        if (request == null || request.adjustedAmount() == null || request.adjustedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Indique um valor válido para o ajuste do prémio.");
        }
        if (request.justification() == null || request.justification().trim().length() < 3) {
            throw new BusinessRuleException("A justificação do ajuste é obrigatória (mínimo 3 caracteres).");
        }

        SalesGoalBonus bonus = bonusRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo de prémio não encontrado."));
        if (bonus.getStatus() == BonusStatus.PAID) {
            throw new BusinessRuleException("Não é permitido alterar um prémio já pago.");
        }

        BigDecimal oldAmount = bonus.getApprovedAmount();
        bonus.setApprovedAmount(request.adjustedAmount());
        bonus.setJustification(request.justification().trim());
        bonus.setApprovedBy(CurrentUserContext.getUsername());

        SalesGoalBonus saved = bonusRepository.save(bonus);
        auditLogService.logEvent(CurrentUserContext.getUsername(), saved.getCompany().getId(), "BONUS_ADJUSTED",
                String.format("Prémio para %s ajustado de %s para %s MZN. Justificação: %s",
                        saved.getEmployee() != null ? saved.getEmployee().getName() : "Equipa",
                        oldAmount, saved.getApprovedAmount(), saved.getJustification()));
        return toDTO(saved);
    }

    public SalesGoalBonusDTO toDTO(SalesGoalBonus b) {
        return new SalesGoalBonusDTO(
                b.getId(), b.getGoal().getId(), b.getGoal().getName(),
                b.getEmployee() != null ? b.getEmployee().getId() : null,
                b.getEmployee() != null ? b.getEmployee().getName() : "Equipa",
                b.getCompany().getId(), b.getCalculatedAmount(), b.getApprovedAmount(),
                b.getJustification(), b.getStatus(),
                b.getPayslip() != null ? b.getPayslip().getId() : null,
                b.getCreatedBy(), b.getApprovedBy()
        );
    }
}
