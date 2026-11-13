package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porta de leitura das atribuições de equipamento a colaboradores para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
@Transactional(readOnly = true)
public class WorkerEquipmentAssignmentLookup {

    private static final String NOT_FOUND = "Atribuição de equipamento não encontrada.";

    private final WorkerEquipmentAssignmentRepository assignmentRepository;

    public WorkerEquipmentAssignmentLookup(WorkerEquipmentAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    /** Atribuição da empresa com colaborador e equipamento carregados. */
    public WorkerEquipmentAssignment requireForCompany(Long assignmentId, Long companyId) {
        if (assignmentId == null || companyId == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return assignmentRepository.findByIdWithDetailsAndCompanyId(assignmentId, companyId)
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }
}
