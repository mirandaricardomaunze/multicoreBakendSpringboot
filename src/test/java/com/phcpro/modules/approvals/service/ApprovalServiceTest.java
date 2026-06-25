package com.phcpro.modules.approvals.service;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.model.ApprovalRequest;
import com.phcpro.modules.approvals.model.ApprovalStatus;
import com.phcpro.modules.approvals.repository.ApprovalHistoryRepository;
import com.phcpro.modules.approvals.repository.ApprovalRequestRepository;
import com.phcpro.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do fecho de pedidos de aprovação por cancelamento do documento de origem
 * (ex.: cancelar uma encomenda em PENDING_APPROVAL). Só o pedido do documento indicado
 * é fechado; os restantes ficam intactos.
 */
class ApprovalServiceTest {

    private ApprovalRequestRepository requestRepository;
    private ApprovalHistoryRepository historyRepository;
    private CompanyRepository companyRepository;
    private ApprovalService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(ApprovalRequestRepository.class);
        historyRepository = mock(ApprovalHistoryRepository.class);
        companyRepository = mock(CompanyRepository.class);
        service = new ApprovalService(requestRepository, historyRepository, List.of(), companyRepository);
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void cancelPendingForDocument_fechaApenasPedidosDoDocumento() {
        ApprovalRequest target = req("ORDER", 5L);
        ApprovalRequest otherType = req("INVOICE", 5L);
        ApprovalRequest otherDoc = req("ORDER", 9L);
        when(requestRepository.findByCompanyIdAndStatus(1L, ApprovalStatus.PENDING))
                .thenReturn(List.of(target, otherType, otherDoc));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelPendingForDocument("ORDER", 5L, "Cliente desistiu");

        assertEquals(ApprovalStatus.REJECTED, target.getStatus());
        assertEquals(ApprovalStatus.PENDING, otherType.getStatus());
        assertEquals(ApprovalStatus.PENDING, otherDoc.getStatus());
        verify(requestRepository, times(1)).save(target);
        verify(historyRepository, times(1)).save(any());
    }

    private ApprovalRequest req(String type, Long docId) {
        ApprovalRequest r = new ApprovalRequest();
        r.setDocumentType(type);
        r.setDocumentId(docId);
        r.setStatus(ApprovalStatus.PENDING);
        return r;
    }
}
