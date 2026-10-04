package mz.multicore.erp.modules.pos;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.CreditNoteLineDTO;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.ReceivablesService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.pos.dto.*;
import mz.multicore.erp.modules.pos.model.*;
import mz.multicore.erp.modules.pos.repository.*;
import mz.multicore.erp.modules.pos.service.POSService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Suíte de testes automatizados para o Harness de Devoluções e Trocas com Vale de Compras (HARNESS-POS-VOUCHER-001).
 */
public class PosVoucherHarnessTest {

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
    private ReceivablesService receivablesService;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private StoreVoucherRepository storeVoucherRepository;
    private CreditNoteRepository creditNoteRepository;

    private POSService service;
    private Company company;
    private Warehouse warehouse;
    private Client client;
    private Product product;
    private TillSession openSession;

    private static final String OPERATOR = "ana.caixa";
    private static final Long COMPANY_ID = 1L;
    private static final Long WAREHOUSE_ID = 10L;
    private static final Long INVOICE_ID = 100L;
    private static final Long CLIENT_ID = 50L;
    private static final Long PRODUCT_ID = 200L;

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
        receivablesService = mock(ReceivablesService.class);
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        storeVoucherRepository = mock(StoreVoucherRepository.class);
        creditNoteRepository = mock(CreditNoteRepository.class);

        service = new POSService(
                tillSessionRepository, tillMovementRepository, invoiceRepository,
                clientRepository, productRepository, warehouseRepository, companyRepository,
                treasuryAccountRepository, inventoryService, financeService, paymentEntryRepository,
                walkInClientProvider, documentNumberService, auditLogService, creditNoteService,
                receivablesService, eventPublisher, storeVoucherRepository, creditNoteRepository
        );

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Multicore Lda");

        warehouse = new Warehouse();
        warehouse.setId(WAREHOUSE_ID);
        warehouse.setName("Armazém Principal");
        warehouse.setCompany(company);

        client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Maria Manjate");

        product = new Product();
        product.setId(PRODUCT_ID);
        product.setSku("SKU-OXFORD");
        product.setName("Camisa Oxford");
        product.setUnitPrice(new BigDecimal("1500.00"));

        openSession = new TillSession();
        openSession.setId(1L);
        openSession.setOperator(OPERATOR);
        openSession.setStatus("OPEN");
        openSession.setCompany(company);
        openSession.setOpeningBalance(new BigDecimal("2000.00"));

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser(OPERATOR, "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("VOUCH-01: Devolução com método STORE_CREDIT cria nota de crédito e emite StoreVoucher")
    void testReturnSaleWithStoreCredit() {
        Invoice invoice = createTestInvoice();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        CreditNoteDTO cnDTO = new CreditNoteDTO(
                1L, "NC-2026-0001", LocalDateTime.now(), COMPANY_ID, CLIENT_ID, "Maria Manjate",
                INVOICE_ID, "FT-2026-0001", WAREHOUSE_ID, "Armazém Principal", "Tamanho incorreto",
                "APPROVED", new BigDecimal("1500.00"), BigDecimal.ZERO, new BigDecimal("1500.00"),
                "Devolução", "ADMIN", LocalDateTime.now(), null, List.of()
        );
        when(creditNoteService.create(any())).thenReturn(cnDTO);
        when(creditNoteService.approve(1L)).thenReturn(cnDTO);
        when(storeVoucherRepository.save(any(StoreVoucher.class))).thenAnswer(inv -> {
            StoreVoucher v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });

        POSReturnRequest request = new POSReturnRequest(
                OPERATOR, COMPANY_ID, INVOICE_ID, WAREHOUSE_ID, "Tamanho incorreto",
                "STORE_CREDIT", null, List.of(new CreateCreditNoteLineRequest(1L, BigDecimal.ONE))
        );

        POSReturnResultDTO result = service.returnSale(request);

        assertNotNull(result);
        assertNotNull(result.creditNote());
        assertEquals("NC-2026-0001", result.creditNote().noteNumber());
        assertNotNull(result.voucher(), "Deve retornar o voucher gerado");
        assertEquals(new BigDecimal("1500.00"), result.voucher().initialAmount());
        assertEquals(new BigDecimal("1500.00"), result.voucher().remainingAmount());
        assertEquals("ACTIVE", result.voucher().status());
        assertTrue(result.voucher().code().startsWith("VALE-"));
        assertEquals("Maria Manjate", result.voucher().clientName());

        ArgumentCaptor<StoreVoucher> captor = ArgumentCaptor.forClass(StoreVoucher.class);
        verify(storeVoucherRepository).save(captor.capture());
        StoreVoucher saved = captor.getValue();
        assertEquals(new BigDecimal("1500.00"), saved.getRemainingAmount());
        assertEquals(LocalDate.now().plusDays(90), saved.getExpiresAt());
    }

    @Test
    @DisplayName("VOUCH-02: Devolução com método CASH não emite voucher e regista reembolso em gaveta")
    void testReturnSaleWithCash() {
        Invoice invoice = createTestInvoice();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(tillSessionRepository.findActiveSessionForOperator(OPERATOR, "OPEN", COMPANY_ID))
                .thenReturn(Optional.of(openSession));
        when(tillMovementRepository.findByTillSessionId(1L)).thenReturn(List.of());

        CreditNoteDTO cnDTO = new CreditNoteDTO(
                1L, "NC-2026-0002", LocalDateTime.now(), COMPANY_ID, CLIENT_ID, "Maria Manjate",
                INVOICE_ID, "FT-2026-0001", WAREHOUSE_ID, "Armazém Principal", "Defeito",
                "APPROVED", new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("500.00"),
                "Devolução", "ADMIN", LocalDateTime.now(), null, List.of()
        );
        when(creditNoteService.create(any())).thenReturn(cnDTO);
        when(creditNoteService.approve(1L)).thenReturn(cnDTO);

        POSReturnRequest request = new POSReturnRequest(
                OPERATOR, COMPANY_ID, INVOICE_ID, WAREHOUSE_ID, "Defeito",
                "CASH", null, List.of(new CreateCreditNoteLineRequest(1L, BigDecimal.ONE))
        );

        POSReturnResultDTO result = service.returnSale(request);

        assertNotNull(result);
        assertEquals("NC-2026-0002", result.creditNote().noteNumber());
        assertNull(result.voucher(), "Método CASH não deve emitir voucher");

        verify(tillMovementRepository).save(any(TillMovement.class));
        verify(storeVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("VOUCH-03: Consulta de vale existente por código retorna DTO válido")
    void testGetVoucherSuccess() {
        StoreVoucher voucher = new StoreVoucher();
        voucher.setId(10L);
        voucher.setCode("VALE-ABCD-1234");
        voucher.setInitialAmount(new BigDecimal("1000.00"));
        voucher.setRemainingAmount(new BigDecimal("750.00"));
        voucher.setCompany(company);
        voucher.setClientName("Maria Manjate");
        voucher.setExpiresAt(LocalDate.now().plusDays(30));
        voucher.setStatus(StoreVoucherStatus.PARTIALLY_USED);

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-ABCD-1234", COMPANY_ID))
                .thenReturn(Optional.of(voucher));

        StoreVoucherDTO dto = service.getVoucher("VALE-ABCD-1234", COMPANY_ID);

        assertNotNull(dto);
        assertEquals("VALE-ABCD-1234", dto.code());
        assertEquals(new BigDecimal("750.00"), dto.remainingAmount());
        assertEquals("PARTIALLY_USED", dto.status());
        assertTrue(dto.isActive());
        assertFalse(dto.isExpired());
    }

    @Test
    @DisplayName("VOUCH-04: Consulta de vale inexistente lança BusinessRuleException")
    void testGetVoucherNotFound() {
        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-INEXISTENTE", COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () ->
                service.getVoucher("VALE-INEXISTENTE", COMPANY_ID));
    }

    @Test
    @DisplayName("VOUCH-05: Checkout com resgate integral de voucher atualiza status para FULLY_REDEEMED")
    void testCheckoutRedeemVoucherFull() {
        mockCheckoutPrerequisites();

        StoreVoucher voucher = new StoreVoucher();
        voucher.setId(5L);
        voucher.setCode("VALE-TOTAL-1000");
        voucher.setInitialAmount(new BigDecimal("1000.00"));
        voucher.setRemainingAmount(new BigDecimal("1000.00"));
        voucher.setCompany(company);
        voucher.setExpiresAt(LocalDate.now().plusDays(60));
        voucher.setStatus(StoreVoucherStatus.ACTIVE);

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-TOTAL-1000", COMPANY_ID))
                .thenReturn(Optional.of(voucher));

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("STORE_CREDIT", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "VALE-TOTAL-1000", null))
        );

        Invoice inv = service.checkout(request);

        assertNotNull(inv);
        assertEquals(StoreVoucherStatus.FULLY_REDEEMED, voucher.getStatus());
        assertEquals(new BigDecimal("0.00"), voucher.getRemainingAmount());
        verify(storeVoucherRepository).save(voucher);
        verify(paymentEntryRepository).save(any(PaymentEntry.class));
    }

    @Test
    @DisplayName("VOUCH-06: Checkout com resgate parcial de voucher atualiza status para PARTIALLY_USED")
    void testCheckoutRedeemVoucherPartial() {
        mockCheckoutPrerequisites();

        StoreVoucher voucher = new StoreVoucher();
        voucher.setId(6L);
        voucher.setCode("VALE-SOBRA-2000");
        voucher.setInitialAmount(new BigDecimal("2000.00"));
        voucher.setRemainingAmount(new BigDecimal("2000.00"));
        voucher.setCompany(company);
        voucher.setExpiresAt(LocalDate.now().plusDays(60));
        voucher.setStatus(StoreVoucherStatus.ACTIVE);

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-SOBRA-2000", COMPANY_ID))
                .thenReturn(Optional.of(voucher));

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("STORE_CREDIT", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "VALE-SOBRA-2000", null))
        );

        Invoice inv = service.checkout(request);

        assertNotNull(inv);
        assertEquals(StoreVoucherStatus.PARTIALLY_USED, voucher.getStatus());
        assertEquals(new BigDecimal("1000.00"), voucher.getRemainingAmount());
        verify(storeVoucherRepository).save(voucher);
    }

    @Test
    @DisplayName("VOUCH-07: Resgate de voucher com saldo insuficiente lança BusinessRuleException")
    void testCheckoutRedeemVoucherInsufficientBalance() {
        mockCheckoutPrerequisites();

        StoreVoucher voucher = new StoreVoucher();
        voucher.setId(7L);
        voucher.setCode("VALE-CURTO-500");
        voucher.setInitialAmount(new BigDecimal("500.00"));
        voucher.setRemainingAmount(new BigDecimal("300.00"));
        voucher.setCompany(company);
        voucher.setExpiresAt(LocalDate.now().plusDays(60));
        voucher.setStatus(StoreVoucherStatus.PARTIALLY_USED);

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-CURTO-500", COMPANY_ID))
                .thenReturn(Optional.of(voucher));

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("STORE_CREDIT", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "VALE-CURTO-500", null))
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.checkout(request));
        assertTrue(ex.getMessage().contains("Saldo insuficiente no vale"));
    }

    @Test
    @DisplayName("VOUCH-08: Resgate de voucher já resgatado lança BusinessRuleException")
    void testCheckoutRedeemVoucherAlreadyRedeemed() {
        mockCheckoutPrerequisites();

        StoreVoucher voucher = new StoreVoucher();
        voucher.setId(8L);
        voucher.setCode("VALE-GASTO");
        voucher.setInitialAmount(new BigDecimal("1000.00"));
        voucher.setRemainingAmount(BigDecimal.ZERO);
        voucher.setCompany(company);
        voucher.setExpiresAt(LocalDate.now().plusDays(60));
        voucher.setStatus(StoreVoucherStatus.FULLY_REDEEMED);

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-GASTO", COMPANY_ID))
                .thenReturn(Optional.of(voucher));

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("STORE_CREDIT", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "VALE-GASTO", null))
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.checkout(request));
        assertTrue(ex.getMessage().contains("não está disponível para utilização"));
    }

    @Test
    @DisplayName("VOUCH-09: Resgate de voucher expirado marca como EXPIRED e lança BusinessRuleException")
    void testCheckoutRedeemVoucherExpired() {
        mockCheckoutPrerequisites();

        StoreVoucher voucher = new StoreVoucher();
        voucher.setId(9L);
        voucher.setCode("VALE-ANTIGO");
        voucher.setInitialAmount(new BigDecimal("1000.00"));
        voucher.setRemainingAmount(new BigDecimal("1000.00"));
        voucher.setCompany(company);
        voucher.setExpiresAt(LocalDate.now().minusDays(5));
        voucher.setStatus(StoreVoucherStatus.ACTIVE);

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-ANTIGO", COMPANY_ID))
                .thenReturn(Optional.of(voucher));

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("STORE_CREDIT", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "VALE-ANTIGO", null))
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.checkout(request));
        assertTrue(ex.getMessage().contains("expirou"));
        assertEquals(StoreVoucherStatus.EXPIRED, voucher.getStatus());
        verify(storeVoucherRepository).save(voucher);
    }

    @Test
    @DisplayName("VOUCH-10: Multi-tenant impede resgate de vale de outra empresa")
    void testCheckoutRedeemVoucherTenantIsolation() {
        mockCheckoutPrerequisites();

        when(storeVoucherRepository.findByCodeAndCompanyId("VALE-OUTRO-TENANT", COMPANY_ID))
                .thenReturn(Optional.empty());

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR, COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null)),
                List.of(new PosPaymentRequest("STORE_CREDIT", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "VALE-OUTRO-TENANT", null))
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.checkout(request));
        assertTrue(ex.getMessage().contains("não encontrado ou inválido"));
    }

    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setInvoiceNumber("FT-2026-0001");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setCustomerName("Maria Manjate");
        invoice.setSalesChannel(SalesChannel.POS);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setTotalAmount(new BigDecimal("1500.00"));

        InvoiceLine line = new InvoiceLine();
        line.setId(1L);
        line.setInvoice(invoice);
        line.setProduct(product);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("1500.00"));
        line.setLineTotal(new BigDecimal("1500.00"));
        invoice.getLines().add(line);
        return invoice;
    }

    private void mockCheckoutPrerequisites() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(clientRepository.findByIdAndCompaniesId(CLIENT_ID, COMPANY_ID)).thenReturn(Optional.of(client));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.findByIdAndCompaniesId(PRODUCT_ID, COMPANY_ID)).thenReturn(Optional.of(product));
        when(tillSessionRepository.findActiveSessionForOperator(OPERATOR, "OPEN", COMPANY_ID))
                .thenReturn(Optional.of(openSession));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT-2026-0099");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}
