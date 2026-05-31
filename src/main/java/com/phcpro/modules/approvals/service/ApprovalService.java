package com.phcpro.modules.approvals.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.dto.ApprovalRequestDTO;
import com.phcpro.modules.approvals.model.ApprovalHistory;
import com.phcpro.modules.approvals.model.ApprovalRequest;
import com.phcpro.modules.approvals.model.ApprovalStatus;
import com.phcpro.modules.approvals.repository.ApprovalHistoryRepository;
import com.phcpro.modules.approvals.repository.ApprovalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final List<ApprovalCallback> callbacks;

    public ApprovalService(
            ApprovalRequestRepository requestRepository,
            ApprovalHistoryRepository historyRepository,
            List<ApprovalCallback> callbacks
    ) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.callbacks = callbacks;
    }

    @Transactional
    public ApprovalRequestDTO submitRequest(String documentType, Long documentId, BigDecimal amount, String description) {
        ApprovalRequest request = new ApprovalRequest();
        request.setDocumentType(documentType);
        request.setDocumentId(documentId);
        request.setAmount(amount);
        request.setSubmitter(CurrentUserContext.getUsername());
        request.setDescription(description);
        request.setCreatedBy(CurrentUserContext.getUsername());

        // Approval routing rules:
        // <= 50.00: Auto-approved
        // > 50.00 and <= 500.00: Requires MANAGER
        // > 500.00: Requires ADMIN
        if (amount.compareTo(new BigDecimal("50.00")) <= 0) {
            request.setStatus(ApprovalStatus.APPROVED);
            request.setRequiredRole("NONE");
            request = requestRepository.save(request);

            logHistory(request, "SUBMIT", "Auto-aprovado (valor menor ou igual a 50.00)");
            triggerCallback(request, true, null);
        } else if (amount.compareTo(new BigDecimal("500.00")) <= 0) {
            request.setStatus(ApprovalStatus.PENDING);
            request.setRequiredRole("MANAGER");
            request = requestRepository.save(request);

            logHistory(request, "SUBMIT", "Submetido para aprovação. Requer perfil MANAGER.");
        } else {
            request.setStatus(ApprovalStatus.PENDING);
            request.setRequiredRole("ADMIN");
            request = requestRepository.save(request);

            logHistory(request, "SUBMIT", "Submetido para aprovação. Requer perfil ADMIN.");
        }

        return toDTO(request);
    }

    @Transactional
    public ApprovalRequestDTO approveRequest(Long requestId, String comments) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessRuleException("Pedido de aprovação não encontrado."));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Este pedido já foi finalizado com estado: " + request.getStatus());
        }

        String userRole = CurrentUserContext.getRole();
        String username = CurrentUserContext.getUsername();

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
        ApprovalRequest request = requestRepository.findById(requestId)
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

    @Transactional(readOnly = true)
    public List<ApprovalRequestDTO> getPendingRequests() {
        return requestRepository.findByStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDTO> getAllRequests() {
        return requestRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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

    private ApprovalRequestDTO toDTO(ApprovalRequest request) {
        return new ApprovalRequestDTO(
                request.getId(),
                request.getDocumentType(),
                request.getDocumentId(),
                request.getAmount(),
                request.getSubmitter(),
                request.getStatus(),
                request.getRequiredRole(),
                request.getDescription(),
                request.getRejectionReason(),
                request.getCreatedAt()
        );
    }
}
