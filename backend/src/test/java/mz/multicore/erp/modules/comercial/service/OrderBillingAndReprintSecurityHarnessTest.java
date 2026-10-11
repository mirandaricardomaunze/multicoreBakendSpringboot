package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.*;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.printing.SensitiveDocumentPrintService;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.modules.printing.model.DocumentPrintControl;
import mz.multicore.erp.modules.printing.repository.DocumentPrintControlRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Harness test rigoroso para validação das regras corporativas de segurança:
 * 1. A mesma encomenda NÃO pode ser faturada de novo (bloqueio atómico e validação de estado/invoiceId).
 * 2. Bloqueio de primeira via para documentos já impressos.
 * 3. Segregação estrita na reimpressão: o mesmo operador NÃO pode autorizar a própria reimpressão.
 * 4. Reimpressão autorizada exclusivamente por segundo utilizador com perfil MANAGER/ADMIN.
 */
class OrderBillingAndReprintSecurityHarnessTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long ORDER_ID = 50L;
    private static final Long INVOICE_ID = 100L;

    // Dependências ComercialService
    private OrderRepository orderRepository;
    private InvoiceRepository invoiceRepository;
    private InventoryService inventoryService;
    private DocumentNumberService documentNumberService;
    private ReceivablesService receivablesService;
    private ComercialService comercialService;

    // Dependências SensitiveDocumentPrintService
    private DocumentPrintControlRepository printControlRepository;
    private AppUserService appUserService;
    private AuditLogService auditLogService;
    private SensitiveDocumentPrintService printService;

    private Company company;
    private Client client;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("operador.caixa", "SELLER");

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Empresa Teste Lda");

        client = new Client();
        client.setId(10L);
        client.setName("Cliente Moçambique");

        warehouse = new Warehouse();
        warehouse.setId(20L);
        warehouse.setName("Armazém Central");
        warehouse.setCompany(company);

        product = new Product();
        product.setId(30L);
        product.setSku("SKU-PROD-A");
        product.setName("Produto A");
        product.setUnitPrice(new BigDecimal("100.00"));
        product.setPurchasePrice(new BigDecimal("60.00"));

        orderRepository = mock(OrderRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        inventoryService = mock(InventoryService.class);
        documentNumberService = mock(DocumentNumberService.class);
        receivablesService = mock(ReceivablesService.class);

        comercialService = new ComercialService(
                mock(ClientRepository.class),
                mock(ProductRepository.class),
                mock(ProductCategoryRepository.class),
                invoiceRepository,
                mock(mz.multicore.erp.modules.approvals.service.ApprovalService.class),
                mock(CompanyRepository.class),
                mock(WarehouseRepository.class),
                inventoryService,
                mock(ReceiptRepository.class),
                mock(FinanceService.class),
                mock(TreasuryAccountRepository.class),
                orderRepository,
                mock(OrderLineRepository.class),
                mock(WalkInClientProvider.class),
                documentNumberService,
                mock(AuditLogService.class),
                mock(mz.multicore.erp.modules.fiscal.repository.TaxRateRepository.class),
                receivablesService,
                mock(ApplicationEventPublisher.class)
        );

        printControlRepository = mock(DocumentPrintControlRepository.class);
        appUserService = mock(AppUserService.class);
        auditLogService = mock(AuditLogService.class);

        printService = new SensitiveDocumentPrintService(
                printControlRepository,
                appUserService,
                auditLogService
        );
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. SEGURANÇA CONTRA DUPLA FATURAÇÃO DE ENCOMENDA
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void faturarEncomenda_quandoJaPossuiInvoiceId_bloqueiaComMensagemClaraPtMz() {
        Order order = createOrder(ORDER_ID, "ENC-2026/001", "BILLED");
        order.setInvoiceId(INVOICE_ID);

        when(orderRepository.findForUpdate(ORDER_ID, COMPANY_ID)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> comercialService.billOrder(ORDER_ID));

        assertTrue(ex.getMessage().contains("já foi faturada anteriormente"),
                "Deveria avisar que já foi faturada: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Não é permitido faturar a mesma encomenda novamente"),
                "Deveria proibir faturar de novo: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(String.valueOf(INVOICE_ID)),
                "Deveria referenciar o ID da fatura gerada: " + ex.getMessage());

        verify(invoiceRepository, never()).save(any());
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void faturarEncomenda_quandoStatusForBilledMesmoSemInvoiceIdSalvo_bloqueia() {
        Order order = createOrder(ORDER_ID, "ENC-2026/002", "BILLED");
        order.setInvoiceId(null);

        when(orderRepository.findForUpdate(ORDER_ID, COMPANY_ID)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> comercialService.billOrder(ORDER_ID));

        assertTrue(ex.getMessage().contains("já foi faturada anteriormente"));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void faturarEncomenda_primeiraVezPendente_sucesso_marcaBilledEPreencheInvoiceId() {
        Order order = createOrder(ORDER_ID, "ENC-2026/003", "PENDING");
        when(orderRepository.findForUpdate(ORDER_ID, COMPANY_ID)).thenReturn(Optional.of(order));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT-2026/1");

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(INVOICE_ID);
            return inv;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceDTO result = comercialService.billOrder(ORDER_ID);

        assertNotNull(result);
        assertEquals(InvoiceStatus.APPROVED, result.status());
        assertEquals("BILLED", order.getStatus());
        assertEquals(INVOICE_ID, order.getInvoiceId());

        verify(orderRepository).save(order);
        verify(inventoryService).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("2.00").negate()), eq("SALE"),
                any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. SEGURANÇA E VISIBILIDADE DE IMPRESSÃO (PRIMEIRA VIA)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void primeiraImpressao_quandoNuncaImpresso_sucessoERegistaPrimeiraVia() {
        when(printControlRepository.findForUpdate(COMPANY_ID, SensitiveDocumentType.INVOICE, INVOICE_ID))
                .thenReturn(Optional.empty());

        byte[] fakePdf = new byte[]{1, 2, 3};
        byte[] printed = printService.firstPrint(SensitiveDocumentType.INVOICE, INVOICE_ID, "TERM-01", () -> fakePdf);

        assertNotNull(printed);
        ArgumentCaptor<DocumentPrintControl> captor = ArgumentCaptor.forClass(DocumentPrintControl.class);
        verify(printControlRepository).save(captor.capture());

        DocumentPrintControl saved = captor.getValue();
        assertEquals(1, saved.getPrintCount());
        assertEquals("operador.caixa", saved.getLastPrintedBy());
        assertEquals("TERM-01", saved.getLastTerminal());
        verify(auditLogService).logCurrent(eq("SENSITIVE_DOCUMENT_PRINT"), contains("impresso pela primeira vez"));
    }

    @Test
    void primeiraImpressao_quandoDocumentoJaPossuiViaEmitida_bloqueiaComMensagemDeReimpressao() {
        DocumentPrintControl existing = new DocumentPrintControl();
        existing.setCompanyId(COMPANY_ID);
        existing.setDocumentType(SensitiveDocumentType.INVOICE);
        existing.setDocumentId(INVOICE_ID);
        existing.setPrintCount(1);

        when(printControlRepository.findForUpdate(COMPANY_ID, SensitiveDocumentType.INVOICE, INVOICE_ID))
                .thenReturn(Optional.of(existing));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                printService.firstPrint(SensitiveDocumentType.INVOICE, INVOICE_ID, "TERM-01", () -> new byte[]{1}));

        assertTrue(ex.getMessage().contains("Este documento já foi impresso"));
        assertTrue(ex.getMessage().contains("Use Reimprimir"));
        verify(printControlRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. SEGREGAÇÃO DE FUNÇÕES NA REIMPRESSÃO (MESMA PESSOA NÃO PODE REIMPRIMIR)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void reimpressao_quandoMesmoUtilizadorAutenticadoTentaAutorizar_bloqueia() {
        CurrentUserContext.setCurrentUser("operador.caixa", "SELLER");

        ReprintAuthorizationRequest request = new ReprintAuthorizationRequest(
                "operador.caixa", // Mesmo utilizador autenticado
                "senha123",
                "Papel encravou na impressora",
                "TERM-01"
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                printService.reprint(SensitiveDocumentType.INVOICE, INVOICE_ID, request, () -> new byte[]{1, 2, 3}));

        assertTrue(ex.getMessage().contains("A reimpressão exige autorização de outro utilizador"),
                "Deveria proibir auto-aprovação de reimpressão: " + ex.getMessage());
        verify(appUserService, never()).authenticate(any(), any());
    }

    @Test
    void reimpressao_quandoSegundoUtilizadorNaoEGerenteNemAdmin_bloqueia() {
        CurrentUserContext.setCurrentUser("operador.caixa", "SELLER");

        // Outro operador mas apenas com papel SELLER (sem alçada gerencial)
        AppUser approverUser = new AppUser();
        approverUser.setUsername("outro.colega");
        approverUser.grantCompany(company, "SELLER");

        when(appUserService.authenticate("outro.colega", "senha456")).thenReturn(approverUser);

        ReprintAuthorizationRequest request = new ReprintAuthorizationRequest(
                "outro.colega",
                "senha456",
                "Segunda via pedida pelo cliente",
                "TERM-01"
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                printService.reprint(SensitiveDocumentType.INVOICE, INVOICE_ID, request, () -> new byte[]{1, 2, 3}));

        assertTrue(ex.getMessage().contains("O autorizador deve ser gerente ou administrador"),
                "Deveria exigir gerente/administrador: " + ex.getMessage());
        verify(printControlRepository, never()).save(any());
    }

    @Test
    void reimpressao_comSegundoUtilizadorGerenteValido_sucesso_incrementaContadorEAudita() {
        CurrentUserContext.setCurrentUser("operador.caixa", "SELLER");

        AppUser managerUser = new AppUser();
        managerUser.setUsername("gerente.loja");
        managerUser.grantCompany(company, "MANAGER");

        when(appUserService.authenticate("gerente.loja", "senhaGerente")).thenReturn(managerUser);

        DocumentPrintControl existing = new DocumentPrintControl();
        existing.setCompanyId(COMPANY_ID);
        existing.setDocumentType(SensitiveDocumentType.INVOICE);
        existing.setDocumentId(INVOICE_ID);
        existing.setPrintCount(1);
        when(printControlRepository.findForUpdate(COMPANY_ID, SensitiveDocumentType.INVOICE, INVOICE_ID))
                .thenReturn(Optional.of(existing));

        // PDF simples em bytes
        byte[] originalPdf = createSamplePdf();

        ReprintAuthorizationRequest request = new ReprintAuthorizationRequest(
                "gerente.loja",
                "senhaGerente",
                "Cliente perdeu a primeira via na entrega",
                "TERM-01"
        );

        byte[] reprinted = printService.reprint(
                SensitiveDocumentType.INVOICE, INVOICE_ID, request, () -> originalPdf);

        assertNotNull(reprinted);
        assertEquals(2, existing.getPrintCount());
        assertEquals("operador.caixa", existing.getLastPrintedBy());
        assertEquals("gerente.loja", existing.getLastAuthorizedBy());
        assertEquals("Cliente perdeu a primeira via na entrega", existing.getLastReason());

        verify(printControlRepository).save(existing);
        verify(auditLogService).logCurrent(
                eq("SENSITIVE_DOCUMENT_REPRINT"),
                contains("reimpresso; via 2; autorizado por gerente.loja"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Utilitários de Teste
    // ─────────────────────────────────────────────────────────────────────────────

    private Order createOrder(Long id, String orderNumber, String status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setCompany(company);
        order.setClient(client);
        order.setWarehouse(warehouse);
        order.setTotalBeforeTax(new BigDecimal("200.00"));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("200.00"));

        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("2.00"));
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setLineTotal(new BigDecimal("200.00"));
        line.setTaxRate(BigDecimal.ZERO);
        order.setLines(List.of(line));

        return order;
    }

    private byte[] createSamplePdf() {
        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Documento de Teste"));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
