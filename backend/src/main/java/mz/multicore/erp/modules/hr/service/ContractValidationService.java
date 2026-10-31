package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.hr.model.ContractType;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Validador de regras laborais e invariantes de sobreposição de contratos de trabalho.
 */
@Service
public class ContractValidationService {

    private final EmploymentContractRepository contractRepository;

    @Autowired
    public ContractValidationService(EmploymentContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public void validateDates(ContractType type, LocalDate startDate, LocalDate endDate,
                              LocalDate probationEndDate, String termReason) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("O fim do contrato não pode ser anterior ao início.");
        }
        if (type.requiresEndDate() && endDate == null) {
            throw new BusinessRuleException(String.format(
                    "Um contrato %s tem de ter data de fim.", type.getLabel().toLowerCase()));
        }
        if (!type.isFixedTerm() && endDate != null) {
            throw new BusinessRuleException("Um contrato sem termo não pode ter data de fim.");
        }
        if (type.isFixedTerm() && (termReason == null || termReason.isBlank())) {
            throw new BusinessRuleException(
                    "Um contrato a termo tem de indicar o motivo do termo (exigência legal).");
        }
        if (probationEndDate != null && probationEndDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                    "O período experimental não pode terminar antes do início do contrato.");
        }
        if (probationEndDate != null && endDate != null && probationEndDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                    "O período experimental não pode terminar depois do contrato.");
        }
    }

    public void ensureNoOverlap(EmploymentContract contract, Long companyId) {
        LocalDate rangeEnd = contract.getEndDate() == null ? LocalDate.of(9999, 12, 31) : contract.getEndDate();
        List<EmploymentContract> clashes = contractRepository.findOverlapping(
                contract.getEmployee().getId(), companyId,
                contract.getStartDate(), rangeEnd,
                contract.getId() == null ? -1L : contract.getId());
        if (!clashes.isEmpty()) {
            EmploymentContract clash = clashes.get(0);
            throw new BusinessRuleException(String.format(
                    "%s já tem o contrato %s vigente neste período (%s a %s). Cesse-o ou renove-o.",
                    contract.getEmployee().getName(), clash.getContractNumber(), clash.getStartDate(),
                    clash.getEndDate() == null ? "sem termo" : clash.getEndDate()));
        }
    }

    public ContractType parseType(String raw) {
        try {
            return ContractType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de contrato desconhecido: " + raw);
        }
    }
}
