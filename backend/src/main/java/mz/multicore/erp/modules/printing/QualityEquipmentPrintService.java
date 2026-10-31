package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.repository.WorkerEquipmentAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de geração de PDF para o Termo de Entrega e Responsabilidade de Equipamento / EPI.
 */
@Service
public class QualityEquipmentPrintService {

    private final WorkerEquipmentAssignmentRepository assignmentRepository;
    private final QualityEquipmentDocumentRenderer documentRenderer;

    @Autowired
    public QualityEquipmentPrintService(
            WorkerEquipmentAssignmentRepository assignmentRepository,
            QualityEquipmentDocumentRenderer documentRenderer
    ) {
        this.assignmentRepository = assignmentRepository;
        this.documentRenderer = documentRenderer;
    }

    public QualityEquipmentPrintService(WorkerEquipmentAssignmentRepository assignmentRepository) {
        this(assignmentRepository, new QualityEquipmentDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderDeliverySheet(Long assignmentId, Long companyId) {
        if (assignmentId == null) {
            throw new BusinessRuleException("Identificador de atribuição obrigatório.");
        }
        Long effectiveCompanyId = companyId != null ? companyId : CurrentUserContext.findCurrentCompanyId();
        if (effectiveCompanyId == null) effectiveCompanyId = 1L;

        WorkerEquipmentAssignment assignment = assignmentRepository
                .findByIdWithDetailsAndCompanyId(assignmentId, effectiveCompanyId)
                .orElseThrow(() -> new BusinessRuleException("Atribuição de equipamento não encontrada para impressão."));

        return documentRenderer.renderDocument(assignment);
    }
}
