package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.printing.DebtCollectionNoticePrintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes de cobertura do Centro Executivo de Risco de Crédito e Cobrança Formal.
 */
class CreditRiskHarnessTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate HOJE = LocalDate.of(2026, 9, 15);

    private InvoiceRepository invoiceRepository;
    private ClientRepository clientRepository;
    private ApprovalRequestRepository approvalRequestRepository;
    private ApprovalService approvalService;
    private CompanyService companyService;

    private CreditRiskService creditRiskService;
    private DebtCollectionNoticePrintService printService;
    private Company company;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        clientRepository = mock(ClientRepository.class);
        approvalRequestRepository = mock(ApprovalRequestRepository.class);
        approvalService = mock(ApprovalService.class);
        companyService = mock(CompanyService.class);

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Multicore Test Enterprise");
        company.setTaxId("400123999");
        company.setAddress("Av. 24 de Julho, Maputo");
        company.setPhone("+258 84 123 4567");
        company.setEmail("financeiro@multicore.co.mz");

        when(companyService.getCompanyById(COMPANY_ID)).thenReturn(company);

        creditRiskService = new CreditRiskService(
                invoiceRepository,
                clientRepository,
                approvalRequestRepository,
                approvalService,
                companyService
        );

        printService = new DebtCollectionNoticePrintService(creditRiskService, companyService);

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente_risco", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Client createClient(Long id, String name, BigDecimal limit) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        c.setTaxId("NUIT-" + id);
        c.setEmail(name.toLowerCase().replace(" ", "") + "@cliente.mz");
        c.setAddress("Bairro Central, Maputo");
        c.setCreditLimit(limit);
        c.setPaymentTermsDays(30);
        return c;
    }

    private Invoice createInvoice(Client client, String number, BigDecimal total, BigDecimal paid, LocalDate dueDate) {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(number);
        inv.setClient(client);
        inv.setTotalAmount(total);
        inv.setAmountPaid(paid != null ? paid : BigDecimal.ZERO);
        inv.setStatus(InvoiceStatus.APPROVED);
        inv.setDueDate(dueDate);
        inv.setCompany(company);
        return inv;
    }

    @Test
    void lowRiskWhenNoOverdueAndWithinLimit() {
        Client c = createClient(1L, "Cliente Bom", new BigDecimal("100000.00"));
        when(clientRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID)).thenReturn(List.of(c));

        Invoice inv = createInvoice(c, "FT-001", new BigDecimal("20000.00"), BigDecimal.ZERO, HOJE.plusDays(10));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));

        List<ClientCreditRiskDTO> list = creditRiskService.getClientsRisk(HOJE);
        assertThat(list).hasSize(1);
        ClientCreditRiskDTO dto = list.get(0);

        assertEquals(CreditRiskLevel.LOW, dto.riskLevel());
        assertFalse(dto.isBlocked());
        assertEquals(0, dto.maxDaysOverdue());
        assertEquals(new BigDecimal("20000.00"), dto.totalDebt());
        assertEquals(BigDecimal.ZERO, dto.totalOverdue());
        assertEquals(new BigDecimal("80000.00"), dto.availableCredit());
    }

    @Test
    void highRiskAndBlockedWhenOverdueBetween31And60Days() {
        Client c = createClient(2L, "Cliente Em Atraso", new BigDecimal("50000.00"));
        when(clientRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID)).thenReturn(List.of(c));

        Invoice inv = createInvoice(c, "FT-002", new BigDecimal("15000.00"), BigDecimal.ZERO, HOJE.minusDays(45));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));

        List<ClientCreditRiskDTO> list = creditRiskService.getClientsRisk(HOJE);
        assertThat(list).hasSize(1);
        ClientCreditRiskDTO dto = list.get(0);

        assertEquals(CreditRiskLevel.HIGH, dto.riskLevel());
        assertTrue(dto.isBlocked());
        assertEquals(45, dto.maxDaysOverdue());
        assertEquals(new BigDecimal("15000.00"), dto.totalOverdue());
        assertTrue(dto.blockReason().contains("Facturas em mora"));
    }

    @Test
    void criticalRiskWhenOverdueOver60Days() {
        Client c = createClient(3L, "Cliente Muito Antigo", new BigDecimal("50000.00"));
        when(clientRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID)).thenReturn(List.of(c));

        Invoice inv = createInvoice(c, "FT-003", new BigDecimal("30000.00"), BigDecimal.ZERO, HOJE.minusDays(95));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));

        List<ClientCreditRiskDTO> list = creditRiskService.getClientsRisk(HOJE);
        assertThat(list).hasSize(1);
        ClientCreditRiskDTO dto = list.get(0);

        assertEquals(CreditRiskLevel.CRITICAL, dto.riskLevel());
        assertTrue(dto.isBlocked());
        assertEquals(95, dto.maxDaysOverdue());
        assertEquals(new BigDecimal("30000.00"), dto.maisDe90());
    }

    @Test
    void assertCreditAllowed_blocksWhenOverdueOver30Days() {
        Client c = createClient(4L, "Cliente Bloqueado", new BigDecimal("50000.00"));
        Invoice inv = createInvoice(c, "FT-004", new BigDecimal("10000.00"), BigDecimal.ZERO, HOJE.minusDays(35));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(approvalRequestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                eq(COMPANY_ID), eq(CreditRiskService.APPROVAL_DOC_TYPE_CREDIT_EXCEPTION), eq(4L), eq(ApprovalStatus.APPROVED)))
                .thenReturn(List.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                creditRiskService.assertCreditAllowed(c, new BigDecimal("5000.00"), HOJE));

        assertTrue(ex.getMessage().contains("Venda a crédito bloqueada"));
        assertTrue(ex.getMessage().contains("35 dias"));
    }

    @Test
    void assertCreditAllowed_allowsWhenManagerExceptionApproved() {
        Client c = createClient(5L, "Cliente Com Excepcao", new BigDecimal("50000.00"));
        Invoice inv = createInvoice(c, "FT-005", new BigDecimal("10000.00"), BigDecimal.ZERO, HOJE.minusDays(35));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));

        ApprovalRequest approvedReq = new ApprovalRequest();
        approvedReq.setStatus(ApprovalStatus.APPROVED);
        when(approvalRequestRepository.findByCompanyIdAndDocumentTypeAndDocumentIdAndStatus(
                eq(COMPANY_ID), eq(CreditRiskService.APPROVAL_DOC_TYPE_CREDIT_EXCEPTION), eq(5L), eq(ApprovalStatus.APPROVED)))
                .thenReturn(List.of(approvedReq));

        assertDoesNotThrow(() -> creditRiskService.assertCreditAllowed(c, new BigDecimal("5000.00"), HOJE));
    }

    @Test
    void requestCreditException_submitsApprovalRequestSuccessfully() {
        Client c = createClient(6L, "Cliente Empresa X", new BigDecimal("20000.00"));
        when(clientRepository.findByIdAndCompaniesId(6L, COMPANY_ID)).thenReturn(Optional.of(c));

        ApprovalRequestDTO returnedDto = new ApprovalRequestDTO(
                101L, "CREDIT_EXCEPTION", 6L, new BigDecimal("15000.00"),
                "gerente_risco", ApprovalStatus.PENDING, "MANAGER",
                "Excepção autorizada para Cliente Empresa X", null, null
        );

        when(approvalService.submitRequest(eq("CREDIT_EXCEPTION"), eq(6L), eq(new BigDecimal("15000.00")), any()))
                .thenReturn(returnedDto);

        CreditExceptionApprovalRequest req = new CreditExceptionApprovalRequest(
                6L, new BigDecimal("15000.00"), "Cliente aguarda pagamento do Estado no fim do mês", "Vendedor João"
        );

        ApprovalRequestDTO result = creditRiskService.requestCreditException(req);
        assertNotNull(result);
        assertEquals(101L, result.id());
        assertEquals(ApprovalStatus.PENDING, result.status());
    }

    @Test
    void getDebtCollectionNoticeAndPdfRenderTest() {
        Client c = createClient(7L, "Sociedade Comercial Lda", new BigDecimal("80000.00"));
        when(clientRepository.findByIdAndCompaniesId(7L, COMPANY_ID)).thenReturn(Optional.of(c));

        Invoice inv1 = createInvoice(c, "FT-2026/0010", new BigDecimal("25000.00"), new BigDecimal("5000.00"), HOJE.minusDays(40));
        Invoice inv2 = createInvoice(c, "FT-2026/0015", new BigDecimal("12500.00"), BigDecimal.ZERO, HOJE.minusDays(15));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv1, inv2));

        DebtCollectionNoticeDTO notice = creditRiskService.getDebtCollectionNotice(7L, HOJE);
        assertNotNull(notice);
        assertEquals("Sociedade Comercial Lda", notice.clientName());
        assertEquals(new BigDecimal("32500.00"), notice.totalOverdue());
        assertEquals(40, notice.maxDaysOverdue());
        assertEquals(2, notice.overdueInvoices().size());

        // Testar renderização do PDF oficial em OpenPDF
        byte[] pdfBytes = printService.render(COMPANY_ID, 7L, HOJE);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF deve ter conteúdo renderizado");
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}
