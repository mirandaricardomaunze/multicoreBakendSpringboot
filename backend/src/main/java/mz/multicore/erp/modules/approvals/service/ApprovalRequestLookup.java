package mz.multicore.erp.modules.approvals.service;

import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura dos pedidos de aprovação para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Pedir, aprovar e rejeitar passa pelo {@link ApprovalService}.
 */
@Service
@Transactional(readOnly = true)
public class ApprovalRequestLookup {

    private final ApprovalRequestRepository requestRepository;

    public ApprovalRequestLookup(ApprovalRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /** Pedidos da empresa no estado indicado. */
    public List<ApprovalRequest> listForCompanyByStatus(Long companyId, ApprovalStatus status) {
        if (companyId == null || status == null) {
            return List.of();
        }
        return requestRepository.findByCompanyIdAndStatus(companyId, status);
    }

    /** Pedidos da empresa sobre um documento concreto (tipo + id) no estado indicado. */
    public List<ApprovalRequest> listForDocument(Long companyId, String documentType, Long documentId,
                                                 ApprovalStatus status) {
        if (companyId == null || documentType == null || documentId == null || status == null) {
            return List.of();
        }
        return requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                companyId, documentType, documentId, status);
    }
}
