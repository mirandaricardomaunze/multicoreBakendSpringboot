package mz.multicore.erp.modules.approvals.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalHistoryRepository;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
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
        service = new ApprovalService(requestRepository, historyRepository, List.of(), new CompanyLookup(companyRepository));
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
        // A filtragem por documento é feita na base: só o pedido do documento chega ao serviço.
        when(requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(1L, "ORDER", 5L, ApprovalStatus.PENDING))
                .thenReturn(List.of(target));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelPendingForDocument("ORDER", 5L, "Cliente desistiu");

        assertEquals(ApprovalStatus.REJECTED, target.getStatus());
        assertEquals(ApprovalStatus.PENDING, otherType.getStatus());
        assertEquals(ApprovalStatus.PENDING, otherDoc.getStatus());
        verify(requestRepository, times(1)).save(target);
        verify(historyRepository, times(1)).save(any());
    }

    // ─── Segregação de funções: quem pede não aprova ─────────────────────────

    private ApprovalRequest submittedBy(String submitter) {
        ApprovalRequest r = req("ORDER", 5L);
        r.setId(77L);
        r.setSubmitter(submitter);
        r.setRequiredRole("MANAGER");
        when(requestRepository.findByIdAndCompanyId(77L, 1L)).thenReturn(java.util.Optional.of(r));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return r;
    }

    @Test
    void approveRequest_gerenteAprovaOProprioPedido_recusa() {
        ApprovalRequest r = submittedBy("gerente");

        mz.multicore.erp.architecture.exception.BusinessRuleException error = org.junit.jupiter.api.Assertions.assertThrows(
                mz.multicore.erp.architecture.exception.BusinessRuleException.class,
                () -> service.approveRequest(77L, "ok"));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("que submeteu"), error.getMessage());
        assertEquals(ApprovalStatus.PENDING, r.getStatus());
    }

    @Test
    void approveRequest_outroGerente_aprova() {
        ApprovalRequest r = submittedBy("vendedor.ana");

        service.approveRequest(77L, "ok");

        assertEquals(ApprovalStatus.APPROVED, r.getStatus());
    }

    @Test
    void approveRequest_adminAprovaOProprioPedido_permitido() {
        // Loja com um só responsável: o ADMIN não fica bloqueado pelas próprias encomendas.
        CurrentUserContext.setCurrentUser("dono", "ADMIN");
        ApprovalRequest r = submittedBy("dono");

        service.approveRequest(77L, "ok");

        assertEquals(ApprovalStatus.APPROVED, r.getStatus());
    }

    @Test
    void findPendingRequestId_procuraNaBasePorDocumento() {
        ApprovalRequest r = req("ORDER", 5L);
        r.setId(42L);
        when(requestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(1L, "ORDER", 5L, ApprovalStatus.PENDING))
                .thenReturn(List.of(r));

        assertEquals(java.util.Optional.of(42L), service.findPendingRequestId("ORDER", 5L));
        verify(requestRepository, never()).findByCompanyIdAndStatus(any(), any());
    }

    @Test // CR-04
    void wasApprovedByPerson_soContaAUltimaAprovacaoESoSeNaoForAutomatica() {
        ApprovalRequest greenLane = req("ORDER", 5L);
        greenLane.setRequiredRole("NONE");
        ApprovalRequest manager = req("ORDER", 6L);
        manager.setRequiredRole("MANAGER");
        when(requestRepository.findFirstByCompanyIdAndDocumentTypeAndDocumentIdAndStatusOrderByIdDesc(1L, "ORDER", 5L, ApprovalStatus.APPROVED))
                .thenReturn(java.util.Optional.of(greenLane));
        when(requestRepository.findFirstByCompanyIdAndDocumentTypeAndDocumentIdAndStatusOrderByIdDesc(1L, "ORDER", 6L, ApprovalStatus.APPROVED))
                .thenReturn(java.util.Optional.of(manager));

        org.junit.jupiter.api.Assertions.assertFalse(service.wasApprovedByPerson("ORDER", 5L));
        org.junit.jupiter.api.Assertions.assertTrue(service.wasApprovedByPerson("ORDER", 6L));
        org.junit.jupiter.api.Assertions.assertFalse(service.wasApprovedByPerson("ORDER", 7L));
    }

    private ApprovalRequest req(String type, Long docId) {
        ApprovalRequest r = new ApprovalRequest();
        r.setDocumentType(type);
        r.setDocumentId(docId);
        r.setStatus(ApprovalStatus.PENDING);
        return r;
    }
}
