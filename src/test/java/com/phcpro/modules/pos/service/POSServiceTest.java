package com.phcpro.modules.pos.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.comercial.service.CreditNoteService;
import com.phcpro.modules.comercial.service.WalkInClientProvider;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import com.phcpro.modules.pos.dto.POSCheckoutLineRequest;
import com.phcpro.modules.pos.dto.POSCheckoutRequest;
import com.phcpro.modules.pos.dto.PosPaymentRequest;
import com.phcpro.modules.pos.model.TillMovement;
import com.phcpro.modules.pos.model.TillMovementType;
import com.phcpro.modules.pos.model.TillSession;
import com.phcpro.modules.pos.repository.PaymentEntryRepository;
import com.phcpro.modules.pos.repository.TillMovementRepository;
import com.phcpro.modules.pos.repository.TillSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do POS. Foco nas regras de dinheiro/stock que o POSService possui:
 * bloqueio de venda sem sessão, numeração FT, saída de stock, escolha de via de
 * pagamento (legado vs multi-método), fiado parcial, e fecho de caixa (diferença
 * exige permissão; depósito do líquido na tesouraria). Dependências mockadas.
 */
class POSServiceTest {

    private TillSessionRepository tillSessionRepository;
    private TillMovementRepository tillMovementRepository;
    private InvoiceRepository invoiceRepository;
    private ClientRepository clientRepository;
    private ProductRepository productRepository;
    private WarehouseRepository warehouseRepository;
    private CompanyRepository companyRepository;
    private TreasuryAccountRepository treasuryAccountRepository;
    private InventoryService inventoryService;
    private FinanceService financeService;
    private PaymentEntryRepository paymentEntryRepository;
    private WalkInClientProvider walkInClientProvider;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private CreditNoteService creditNoteService;
    private POSService service;

    private Company company;
    private Warehouse warehouse;
    private Product product;
    private TillSession openSession;

    private static final String OPERATOR = "caixa";
    private static final Long COMPANY_ID = 1L;
    private static final Long WAREHOUSE_ID = 10L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long ACCOUNT_ID = 30L;

    @BeforeEach
    void setUp() {
        tillSessionRepository = mock(TillSessionRepository.class);
        tillMovementRepository = mock(TillMovementRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        clientRepository = mock(ClientRepository.class);
        productRepository = mock(ProductRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        companyRepository = mock(CompanyRepository.class);
        treasuryAccountRepository = mock(TreasuryAccountRepository.class);
        inventoryService = mock(InventoryService.class);
        financeService = mock(FinanceService.class);
        paymentEntryRepository = mock(PaymentEntryRepository.class);
        walkInClientProvider = mock(WalkInClientProvider.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);
        creditNoteService = mock(CreditNoteService.class);

        service = new POSService(tillSessionRepository, tillMovementRepository, invoiceRepository,
                clientRepository, productRepository, warehouseRepository, companyRepository,
                treasuryAccountRepository, inventoryService, financeService, paymentEntryRepository,
                walkInClientProvider, documentNumberService, auditLogService, creditNoteService);

        company = company(COMPANY_ID);
        warehouse = warehouse(WAREHOUSE_ID, company);
        product = product(PRODUCT_ID, new BigDecimal("100"));
        openSession = session(1L, "OPEN", new BigDecimal("100"));

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser(OPERATOR, "EMPLOYEE");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ────────────────────────── checkout ──────────────────────────

    @Test
    void checkout_semSessaoAberta_bloqueia() {
        when(tillSessionRepository.findByOperatorAndStatusAndCompanyId(OPERATOR, "OPEN", COMPANY_ID))
                .thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.checkout(checkout(legacyPayment(), null)));
        assertTrue(ex.getMessage().toLowerCase().contains("sessão de caixa"));
        verify(invoiceRepository, never()).save(any());
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkout_vendaNumerarioLegado_numeraFT_saiStock_eFicaPaga() {
        stubHappyPath();

        Invoice invoice = service.checkout(checkout(/*payments*/ null, /*treasuryAccountId*/ ACCOUNT_ID));

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals("FT-2026/1", invoice.getInvoiceNumber());
        verify(documentNumberService).next(DocumentSeries.INVOICE);
        // Saída de stock = quantidade negativa, tipo SALE.
        verify(inventoryService).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("1").negate()), eq("SALE"),
                any(), any(), any());
        // Via legada: numerário entra na gaveta, não na tesouraria.
        verify(tillMovementRepository).save(any(TillMovement.class));
        verify(financeService, never()).registerTransaction(any(), any(), any(), any());
    }

    @Test
    void checkout_semMetodoPagamento_bloqueia() {
        stubHappyPath();

        assertThrows(BusinessRuleException.class,
                () -> service.checkout(checkout(/*payments*/ null, /*treasuryAccountId*/ null)));
    }

    @Test
    void checkout_multiPagamentoExcedeTotal_bloqueia() {
        stubHappyPath();

        PosPaymentRequest huge = new PosPaymentRequest("CASH", new BigDecimal("100000"), new BigDecimal("100000"), null, null);
        assertThrows(BusinessRuleException.class,
                () -> service.checkout(checkout(List.of(huge), null)));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void checkout_fiadoParcial_ficaParcialmentePaga() {
        stubHappyPath();

        // Paga 50 em numerário de um total > 100 (preço 100 + IVA) → fica parcialmente paga.
        PosPaymentRequest cash = new PosPaymentRequest("CASH", new BigDecimal("50"), new BigDecimal("50"), null, null);
        Invoice invoice = service.checkout(checkout(List.of(cash), null));

        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
        assertEquals(0, new BigDecimal("50.00").compareTo(invoice.getAmountPaid()));
        verify(paymentEntryRepository).save(any());
        verify(tillMovementRepository).save(any(TillMovement.class)); // numerário na gaveta
    }

    @Test
    void checkout_multiMetodoNumerarioMaisCartao_separaMovimentos() {
        stubHappyPath();

        PosPaymentRequest cash = new PosPaymentRequest("CASH", new BigDecimal("50"), new BigDecimal("50"), null, null);
        PosPaymentRequest card = new PosPaymentRequest("CARD", new BigDecimal("50"), null, "AUTH-1", ACCOUNT_ID);
        Invoice invoice = service.checkout(checkout(List.of(cash, card), null));

        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus()); // 100 < 100 + IVA
        // Numerário → gaveta; cartão → tesouraria. Sem dupla contagem.
        verify(tillMovementRepository).save(any(TillMovement.class));
        verify(financeService).registerTransaction(eq(ACCOUNT_ID), eq("DEBIT"), eq(new BigDecimal("50.00")), any());
        verify(paymentEntryRepository, times(2)).save(any());
    }

    // ────────────────────────── closeSession ──────────────────────────

    @Test
    void closeSession_saldoCerto_fechaSemExigirPermissao() {
        // Operador EMPLOYEE; sem diferença não há guarda de permissão.
        when(tillSessionRepository.findById(1L)).thenReturn(Optional.of(openSession));
        when(tillMovementRepository.findByTillSessionId(1L)).thenReturn(List.of());
        when(tillSessionRepository.save(any(TillSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TillSession closed = service.closeSession(1L, new BigDecimal("100"));

        assertEquals("CLOSED", closed.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(closed.getDifference()));
    }

    @Test
    void closeSession_comDiferenca_semPermissao_bloqueia() {
        when(tillSessionRepository.findById(1L)).thenReturn(Optional.of(openSession));
        when(tillMovementRepository.findByTillSessionId(1L)).thenReturn(List.of());

        // Esperado 100, contado 90 → diferença -10 exige MANAGER/ADMIN. Operador é EMPLOYEE.
        assertThrows(BusinessRuleException.class, () -> service.closeSession(1L, new BigDecimal("90")));
        verify(tillSessionRepository, never()).save(any());
    }

    @Test
    void closeSession_jaFechada_bloqueia() {
        TillSession closed = session(1L, "CLOSED", new BigDecimal("100"));
        when(tillSessionRepository.findById(1L)).thenReturn(Optional.of(closed));

        assertThrows(BusinessRuleException.class, () -> service.closeSession(1L, new BigDecimal("100")));
    }

    @Test
    void closeSession_comDeposito_movimentaLiquidoParaTesouraria() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        when(tillSessionRepository.findById(1L)).thenReturn(Optional.of(openSession));
        TillMovement sale = new TillMovement();
        sale.setMovementType(TillMovementType.SALE);
        sale.setAmount(new BigDecimal("50"));
        when(tillMovementRepository.findByTillSessionId(1L)).thenReturn(List.of(sale));
        when(tillSessionRepository.save(any(TillSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Esperado = abertura 100 + venda 50 = 150; líquido da sessão = 50 → depósito.
        TillSession closed = service.closeSession(1L, new BigDecimal("150"), ACCOUNT_ID);

        assertEquals("CLOSED", closed.getStatus());
        verify(financeService).registerTransaction(eq(ACCOUNT_ID), eq("DEBIT"), eq(new BigDecimal("50")), any());
    }

    // ────────────────────────── helpers ──────────────────────────

    private void stubHappyPath() {
        when(tillSessionRepository.findByOperatorAndStatusAndCompanyId(OPERATOR, "OPEN", COMPANY_ID))
                .thenReturn(Optional.of(openSession));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(walkInClientProvider.getOrCreate()).thenReturn(client(5L, "Consumidor Final"));
        when(productRepository.findByIdAndCompaniesId(PRODUCT_ID, COMPANY_ID)).thenReturn(Optional.of(product));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT-2026/1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tillMovementRepository.save(any(TillMovement.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private POSCheckoutRequest checkout(List<PosPaymentRequest> payments, Long treasuryAccountId) {
        return new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, /*clientId*/ null, /*walkInName*/ null, WAREHOUSE_ID,
                treasuryAccountId,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, new BigDecimal("1"), null, null, null)),
                payments);
    }

    private List<PosPaymentRequest> legacyPayment() {
        return null;
    }

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Warehouse warehouse(long id, Company company) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName("Loja");
        w.setCompany(company);
        return w;
    }

    private static Product product(long id, BigDecimal price) {
        Product p = new Product();
        p.setId(id);
        p.setSku("SKU-1");
        p.setName("Arroz 1kg");
        p.setUnitPrice(price);
        return p;
    }

    private static Client client(long id, String name) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private TillSession session(long id, String status, BigDecimal opening) {
        TillSession s = new TillSession();
        s.setId(id);
        s.setOperator(OPERATOR);
        s.setCompany(company);
        s.setOpeningBalance(opening);
        s.setStatus(status);
        return s;
    }
}
