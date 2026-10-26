package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Orquestrador executivo de gestão de risco de crédito, matriz de aging e cobrança formal.
 * Delega para colaboradores especializados respeitando os limites físicos de linhas.
 */
@Service
public class CreditRiskService {

    public static final String APPROVAL_DOC_TYPE_CREDIT_EXCEPTION = "CREDIT_EXCEPTION";
    public static final int MAX_ALLOWED_OVERDUE_DAYS = 30;

    private final CreditRiskAssessmentService assessmentService;
    private final CreditControlService controlService;
    private final DebtCollectionNoticeService noticeService;

    @Autowired
    public CreditRiskService(
            CreditRiskAssessmentService assessmentService,
            CreditControlService controlService,
            DebtCollectionNoticeService noticeService
    ) {
        this.assessmentService = assessmentService;
        this.controlService = controlService;
        this.noticeService = noticeService;
    }

    /**
     * Construtor de retrocompatibilidade para testes legados e instanciacao manual com repositórios.
     */
    public CreditRiskService(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalService approvalService,
            CompanyService companyService
    ) {
        this.assessmentService = new CreditRiskAssessmentService(
                invoiceRepository, clientRepository, approvalRequestRepository);
        this.controlService = new CreditControlService(
                invoiceRepository, clientRepository, approvalRequestRepository, approvalService);
        this.noticeService = new DebtCollectionNoticeService(
                invoiceRepository, clientRepository, companyService);
    }

    /**
     * Resumo executivo da carteira de crédito para o dashboard e tomadores de decisão.
     */
    @Transactional(readOnly = true)
    public CreditRiskSummaryDTO getSummary(LocalDate referenceDate) {
        return assessmentService.getSummary(referenceDate);
    }

    /**
     * Lista detalhada de risco e aging de todos os clientes com dívida ou limite activo.
     */
    @Transactional(readOnly = true)
    public List<ClientCreditRiskDTO> getClientsRisk(LocalDate referenceDate) {
        return assessmentService.getClientsRisk(referenceDate);
    }

    /**
     * Valida se a concessão de crédito é permitida tendo em conta mora e limite financeiro.
     */
    @Transactional(readOnly = true)
    public void assertCreditAllowed(Client client, BigDecimal newDebt, LocalDate referenceDate) {
        controlService.assertCreditAllowed(client, newDebt, referenceDate);
    }

    /**
     * Submete um pedido formal de excepção de crédito para aprovação pela Direcção/Gerência.
     */
    @Transactional
    public ApprovalRequestDTO requestCreditException(CreditExceptionApprovalRequest req) {
        return controlService.requestCreditException(req);
    }

    /**
     * Prepara os dados consolidados para a Notificação Formal de Cobrança de um cliente.
     */
    @Transactional(readOnly = true)
    public DebtCollectionNoticeDTO getDebtCollectionNotice(Long clientId, LocalDate referenceDate) {
        return noticeService.getDebtCollectionNotice(clientId, referenceDate);
    }
}
