package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import mz.multicore.erp.modules.quality.service.WorkerEquipmentAssignmentLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de geração de PDF para o Termo de Entrega e Responsabilidade de Equipamento / EPI.
 */
@Service
public class QualityEquipmentPrintService {

    private final WorkerEquipmentAssignmentLookup assignmentLookup;
    private final QualityEquipmentDocumentRenderer documentRenderer;

    @Autowired
    public QualityEquipmentPrintService(
            WorkerEquipmentAssignmentLookup assignmentLookup,
            QualityEquipmentDocumentRenderer documentRenderer
    ) {
        this.assignmentLookup = assignmentLookup;
        this.documentRenderer = documentRenderer;
    }

    public QualityEquipmentPrintService(WorkerEquipmentAssignmentLookup assignmentLookup) {
        this(assignmentLookup, new QualityEquipmentDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderDeliverySheet(Long assignmentId, Long companyId) {
        if (assignmentId == null) {
            throw new BusinessRuleException("Identificador de atribuição obrigatório.");
        }
        Long effectiveCompanyId = companyId != null ? companyId : CurrentUserContext.findCurrentCompanyId();
        if (effectiveCompanyId == null) effectiveCompanyId = 1L;

        WorkerEquipmentAssignment assignment = assignmentLookup.requireForCompany(assignmentId, effectiveCompanyId);

        return documentRenderer.renderDocument(assignment);
    }
}
