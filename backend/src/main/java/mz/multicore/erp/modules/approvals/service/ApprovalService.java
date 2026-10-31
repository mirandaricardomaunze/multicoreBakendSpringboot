package mz.multicore.erp.modules.approvals.service;

import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.repository.ApprovalHistoryRepository;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fachada unificada de gestão de aprovações corporativas.
 * Delega em {@link ApprovalLifecycleService} e {@link ApprovalQueryService}.
 * Ver {@code docs/APPROVAL_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Service
public class ApprovalService {

    public static final BigDecimal BASE_AUTO_APPROVE_LIMIT = ApprovalEscalationPolicy.BASE_AUTO_APPROVE_LIMIT;
    public static final BigDecimal BASE_MANAGER_LIMIT = ApprovalEscalationPolicy.BASE_MANAGER_LIMIT;

    private final ApprovalLifecycleService lifecycleService;
    private final ApprovalQueryService queryService;

    @Autowired
    public ApprovalService(ApprovalLifecycleService lifecycleService, ApprovalQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    /** Construtor retrocompatível (testes unitários e mocks legados). */
    public ApprovalService(ApprovalRequestRepository requestRepository, ApprovalHistoryRepository historyRepository,
                           List<ApprovalCallback> callbacks, CompanyRepository companyRepository) {
        this(requestRepository, historyRepository, callbacks, companyRepository, null, null);
    }

    /** Construtor retrocompatível com providers de repositório. */
    public ApprovalService(ApprovalRequestRepository requestRepository, ApprovalHistoryRepository historyRepository,
                           List<ApprovalCallback> callbacks, CompanyRepository companyRepository,
                           ObjectProvider<StockTransferRepository> stockTransferRepository,
                           ObjectProvider<StockWasteRepository> stockWasteRepository) {
        this(buildLifecycle(requestRepository, historyRepository, callbacks, companyRepository),
             buildQuery(requestRepository, historyRepository, callbacks, companyRepository,
                        stockTransferRepository, stockWasteRepository));
    }

    private static ApprovalLifecycleService buildLifecycle(ApprovalRequestRepository reqRepo, ApprovalHistoryRepository histRepo,
                                                          List<ApprovalCallback> cb, CompanyRepository compRepo) {
        return new ApprovalLifecycleService(reqRepo, histRepo, cb, compRepo, new ApprovalEscalationPolicy());
    }

    private static ApprovalQueryService buildQuery(ApprovalRequestRepository reqRepo, ApprovalHistoryRepository histRepo,
                                                   List<ApprovalCallback> cb, CompanyRepository compRepo,
                                                   ObjectProvider<StockTransferRepository> stRepo,
                                                   ObjectProvider<StockWasteRepository> swRepo) {
        return new ApprovalQueryService(reqRepo, buildLifecycle(reqRepo, histRepo, cb, compRepo), stRepo, swRepo);
    }

    @Transactional
    public ApprovalRequestDTO submitRequest(String documentType, Long documentId, BigDecimal amount, String description) {
        return lifecycleService.submitRequest(documentType, documentId, amount, description);
    }

    @Transactional
    public ApprovalRequestDTO submitRequest(String documentType, Long documentId, BigDecimal amount,
                                            String description, String requiredRole, boolean autoApproved) {
        return lifecycleService.submitRequest(documentType, documentId, amount, description, requiredRole, autoApproved);
    }

    @Transactional
    public ApprovalRequestDTO approveRequest(Long requestId, String comments) {
        return lifecycleService.approveRequest(requestId, comments);
    }

    @Transactional
    public ApprovalRequestDTO rejectRequest(Long requestId, String reason) {
        return lifecycleService.rejectRequest(requestId, reason);
    }

    @Transactional
    public void cancelPendingForDocument(String documentType, Long documentId, String reason) {
        lifecycleService.cancelPendingForDocument(documentType, documentId, reason);
    }

    @Transactional
    public List<ApprovalRequestDTO> getPendingRequests() {
        return queryService.getPendingRequests();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDTO> getAllRequests() {
        return queryService.getAllRequests();
    }
}
