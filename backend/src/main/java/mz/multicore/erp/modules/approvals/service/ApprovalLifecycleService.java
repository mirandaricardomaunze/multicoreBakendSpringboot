package mz.multicore.erp.modules.approvals.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalHistory;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalHistoryRepository;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ciclo de vida transacional de pedidos de aprovação: submissão com alçada, aprovação com verificação
 * de perfil hierárquico, rejeição fundamentada, cancelamento automático e disparo de callbacks.
 */
@Service
public class ApprovalLifecycleService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final List<ApprovalCallback> callbacks;
    private final CompanyRepository companyRepository;
    private final ApprovalEscalationPolicy escalationPolicy;

    public ApprovalLifecycleService(
            ApprovalRequestRepository requestRepository,
            ApprovalHistoryRepository historyRepository,
            List<ApprovalCallback> callbacks,
            CompanyRepository companyRepository,
            ApprovalEscalationPolicy escalationPolicy
    ) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.callbacks = callbacks != null ? callbacks : List.of();
        this.companyRepository = companyRepository;
        this.escalationPolicy = escalationPolicy != null ? escalationPolicy : new ApprovalEscalationPolicy();
    }

    @Transactional
    public ApprovalRequestDTO submitRequest(String documentType, Long documentId, BigDecimal amount, String description) {
        ApprovalEscalationPolicy.EscalationDecision decision = escalationPolicy.evaluate(amount, description);
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        return submitRequest(documentType, documentId, amt, description, decision.requiredRole(), decision.autoApproved());
    }

    @Transactional
    public ApprovalRequestDTO submitRequest(
            String documentType,
            Long documentId,
            BigDecimal amount,
            String description,
            String requiredRole,
            boolean autoApproved
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<ApprovalRequest> existingPending = requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                companyId, documentType, documentId, ApprovalStatus.PENDING);
        if (!existingPending.isEmpty()) {
            ApprovalRequest existing = existingPending.get(0);
            existing.setAmount(amount != null ? amount : BigDecimal.ZERO);
            existing.setDescription(description);
            return toDTO(requestRepository.save(existing));
        }

        ApprovalRequest request = new ApprovalRequest();
        request.setCompany(companyRepository.getReferenceById(companyId));
        request.setDocumentType(documentType);
        request.setDocumentId(documentId);
        request.setAmount(amount != null ? amount : BigDecimal.ZERO);
        request.setSubmitter(CurrentUserContext.getUsername());
        request.setDescription(description);
        request.setCreatedBy(CurrentUserContext.getUsername());

        if (autoApproved) {
            request.setStatus(ApprovalStatus.APPROVED);
            request.setRequiredRole("NONE");
            request = requestRepository.save(request);

            logHistory(request, "SUBMIT", description != null ? description : "Auto-aprovado pela Matriz Corporativa");
            triggerCallback(request, true, null);
        } else {
            String role = requiredRole != null && !requiredRole.isBlank() ? requiredRole.toUpperCase(java.util.Locale.ROOT) : "MANAGER";
            request.setStatus(ApprovalStatus.PENDING);
            request.setRequiredRole(role);
            request = requestRepository.save(request);

            logHistory(request, "SUBMIT", "Submetido para aprovação. Requer perfil " + role + ".");
        }

        return toDTO(request);
    }

    @Transactional
    public ApprovalRequestDTO approveRequest(Long requestId, String comments) {
        ApprovalRequest request = requestRepository.findByIdAndCompanyId(requestId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de aprovação não encontrado."));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Este pedido já foi finalizado com estado: " + request.getStatus());
        }

        String userRole = CurrentUserContext.getRole();

        // Admin can approve anything, manager can approve manager-level requests
        boolean isAuthorized = "ADMIN".equalsIgnoreCase(userRole)
                || ("MANAGER".equalsIgnoreCase(userRole) && "MANAGER".equalsIgnoreCase(request.getRequiredRole()));

        if (!isAuthorized) {
            throw new BusinessRuleException("Utilizador não autorizado. Perfil requerido: " + request.getRequiredRole() + ", Perfil atual: " + userRole);
        }

        request.setStatus(ApprovalStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());
        request = requestRepository.save(request);

        logHistory(request, "APPROVE", comments != null ? comments : "Aprovado com sucesso.");
        triggerCallback(request, true, null);

        return toDTO(request);
    }

    @Transactional
    public ApprovalRequestDTO rejectRequest(Long requestId, String reason) {
        ApprovalRequest request = requestRepository.findByIdAndCompanyId(requestId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de aprovação não encontrado."));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Este pedido já foi finalizado com estado: " + request.getStatus());
        }

        String userRole = CurrentUserContext.getRole();

        boolean isAuthorized = "ADMIN".equalsIgnoreCase(userRole)
                || ("MANAGER".equalsIgnoreCase(userRole) && "MANAGER".equalsIgnoreCase(request.getRequiredRole()));

        if (!isAuthorized) {
            throw new BusinessRuleException("Utilizador não autorizado para rejeitar. Perfil requerido: " + request.getRequiredRole());
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar um motivo para a rejeição.");
        }

        request.setStatus(ApprovalStatus.REJECTED);
        request.setRejectionReason(reason);
        request.setUpdatedAt(LocalDateTime.now());
        request = requestRepository.save(request);

        logHistory(request, "REJECT", reason);
        triggerCallback(request, false, reason);

        return toDTO(request);
    }

    @Transactional
    public void cancelPendingForDocument(String documentType, Long documentId, String reason) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        for (ApprovalRequest req : requestRepository.findByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING)) {
            if (documentType.equalsIgnoreCase(req.getDocumentType()) && documentId.equals(req.getDocumentId())) {
                req.setStatus(ApprovalStatus.REJECTED);
                req.setRejectionReason(reason);
                req.setUpdatedAt(LocalDateTime.now());
                requestRepository.save(req);
                logHistory(req, "CANCEL", "Pedido fechado por cancelamento do documento. Motivo: " + reason);
            }
        }
    }

    public ApprovalRequestDTO toDTO(ApprovalRequest request) {
        return new ApprovalRequestDTO(request.getId(), request.getDocumentType(), request.getDocumentId(),
                request.getAmount(), request.getSubmitter(), request.getStatus(), request.getRequiredRole(),
                request.getDescription(), request.getRejectionReason(), request.getCreatedAt());
    }

    private void logHistory(ApprovalRequest request, String action, String comments) {
        ApprovalHistory history = new ApprovalHistory();
        history.setApprovalRequest(request);
        history.setAction(action);
        history.setPerformedBy(CurrentUserContext.getUsername());
        history.setRole(CurrentUserContext.getRole());
        history.setComments(comments);
        historyRepository.save(history);
    }

    private void triggerCallback(ApprovalRequest request, boolean approved, String reason) {
        for (ApprovalCallback callback : callbacks) {
            if (callback.supports(request.getDocumentType())) {
                if (approved) {
                    callback.onApproved(request.getDocumentId());
                } else {
                    callback.onRejected(request.getDocumentId(), reason);
                }
                return;
            }
        }
    }
}
