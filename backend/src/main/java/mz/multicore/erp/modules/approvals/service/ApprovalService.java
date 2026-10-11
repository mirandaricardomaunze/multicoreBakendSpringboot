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
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final List<ApprovalCallback> callbacks;
    private final CompanyRepository companyRepository;
    private final org.springframework.beans.factory.ObjectProvider<mz.multicore.erp.modules.inventory.repository.StockTransferRepository> stockTransferRepository;
    private final org.springframework.beans.factory.ObjectProvider<mz.multicore.erp.modules.inventory.repository.StockWasteRepository> stockWasteRepository;

    public ApprovalService(
            ApprovalRequestRepository requestRepository,
            ApprovalHistoryRepository historyRepository,
            List<ApprovalCallback> callbacks,
            CompanyRepository companyRepository
    ) {
        this(requestRepository, historyRepository, callbacks, companyRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ApprovalService(
            ApprovalRequestRepository requestRepository,
            ApprovalHistoryRepository historyRepository,
            List<ApprovalCallback> callbacks,
            CompanyRepository companyRepository,
            org.springframework.beans.factory.ObjectProvider<mz.multicore.erp.modules.inventory.repository.StockTransferRepository> stockTransferRepository,
            org.springframework.beans.factory.ObjectProvider<mz.multicore.erp.modules.inventory.repository.StockWasteRepository> stockWasteRepository
    ) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.callbacks = callbacks;
        this.companyRepository = companyRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.stockWasteRepository = stockWasteRepository;
    }

    public static final BigDecimal BASE_AUTO_APPROVE_LIMIT = new BigDecimal("50000.00");
    public static final BigDecimal BASE_MANAGER_LIMIT = new BigDecimal("250000.00");

    @Transactional
    public ApprovalRequestDTO submitRequest(String documentType, Long documentId, BigDecimal amount, String description) {
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        String requiredRole = "MANAGER";
        boolean autoApproved = false;

        if (description != null && description.contains("(Requer ADMIN)")) {
            requiredRole = "ADMIN";
            autoApproved = false;
        } else if (description != null && description.contains("(Requer MANAGER)")) {
            requiredRole = "MANAGER";
            autoApproved = false;
        } else if (description != null && description.contains("Green Lane")) {
            autoApproved = true;
            requiredRole = "NONE";
        } else if (amt.compareTo(BASE_AUTO_APPROVE_LIMIT) <= 0) {
            autoApproved = true;
            requiredRole = "NONE";
        } else if (amt.compareTo(BASE_MANAGER_LIMIT) <= 0) {
            requiredRole = "MANAGER";
            autoApproved = false;
        } else {
            requiredRole = "ADMIN";
            autoApproved = false;
        }

        return submitRequest(documentType, documentId, amt, description, requiredRole, autoApproved);
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

    /**
     * Fecha qualquer pedido de aprovação ainda PENDENTE de um documento que está a ser cancelado
     * na sua origem (ex.: cancelar uma encomenda em PENDING_APPROVAL). Marca o pedido como REJECTED
     * com o motivo do cancelamento. Não há re-verificação de role aqui — quem cancela o documento
     * já foi autorizado no Service dono da regra; e não dispara callback (a origem já trata o estado).
     */
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

    @Transactional
    public List<ApprovalRequestDTO> getPendingRequests() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        syncUnindexedPendingDocuments(companyId);
        return requestRepository.findByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private void syncUnindexedPendingDocuments(Long companyId) {
        if (companyId == null) return;
        if (stockTransferRepository != null && stockTransferRepository.getIfAvailable() != null) {
            var transferRepo = stockTransferRepository.getIfAvailable();
            for (var transfer : transferRepo.findByCompanyId(companyId)) {
                if (transfer.getStatus() != null && transfer.getStatus().isSeparatedOrPending()) {
                    List<ApprovalRequest> existing = requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                            companyId, "STOCK_TRANSFER", transfer.getId(), ApprovalStatus.PENDING);
                    if (existing.isEmpty()) {
                        String originName = transfer.getOriginWarehouse() != null ? transfer.getOriginWarehouse().getName() : "Origem";
                        String destName = transfer.getDestinationWarehouse() != null ? transfer.getDestinationWarehouse().getName() : "Destino";
                        BigDecimal amount = BigDecimal.ZERO;
                        if (transfer.getLines() != null) {
                            for (var line : transfer.getLines()) {
                                BigDecimal price = line.getProduct() != null && line.getProduct().getUnitPrice() != null ? line.getProduct().getUnitPrice() : BigDecimal.ZERO;
                                amount = amount.add(price.multiply(line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO));
                            }
                        }
                        submitRequest("STOCK_TRANSFER", transfer.getId(), amount,
                                "Transferência " + transfer.getTransferNumber() + " de '" + originName + "' para '" + destName + "'",
                                "MANAGER", false);
                    }
                }
            }
        }
        if (stockWasteRepository != null && stockWasteRepository.getIfAvailable() != null) {
            var wasteRepo = stockWasteRepository.getIfAvailable();
            for (var waste : wasteRepo.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, mz.multicore.erp.modules.inventory.model.WasteStatus.PENDING_APPROVAL)) {
                List<ApprovalRequest> existing = requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                        companyId, "STOCK_WASTE", waste.getId(), ApprovalStatus.PENDING);
                if (existing.isEmpty()) {
                    String prodName = waste.getProduct() != null ? waste.getProduct().getName() : "Artigo";
                    submitRequest("STOCK_WASTE", waste.getId(), waste.getTotalCost(),
                            "Quebra de stock #" + waste.getId() + " - " + prodName + " (" + waste.getReason() + ")",
                            "MANAGER", false);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDTO> getAllRequests() {
        return requestRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUserContext.getCurrentCompanyId())
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
