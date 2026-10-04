package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.pos.service.POSService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Harness para validação da Conversão Direta de Cotações em Factura e no POS.
 * Conforme especificado em docs/COTACAO_CONVERSAO_SPEC.md e docs/COTACAO_CONVERSAO_HARNESS.md.
 */
class QuotationConversionHarnessTest {

    private QuotationRepository quotationRepository;
    private ClientRepository clientRepository;
    private ProductRepository productRepository;
    private CompanyRepository companyRepository;
    private WarehouseRepository warehouseRepository;
    private WalkInClientProvider walkInClientProvider;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private ComercialService comercialService;
    private QuotationService quotationService;

    // Dependências adicionais para POSService
    private TillSessionRepository tillSessionRepository;
    private TillMovementRepository tillMovementRepository;
    private InvoiceRepository invoiceRepository;
    private TreasuryAccountRepository treasuryAccountRepository;
    private InventoryService inventoryService;
    private FinanceService financeService;
    private PaymentEntryRepository paymentEntryRepository;
    private CreditNoteService creditNoteService;
    private ReceivablesService receivablesService;
    private ApplicationEventPublisher eventPublisher;
    private StoreVoucherRepository storeVoucherRepository;
    private CreditNoteRepository creditNoteRepository;
    private POSService posService;

    private Company company;
    private Warehouse warehouse;
    private Product product;
    private Client client;

    private static final Long COMPANY_ID = 1L;
    private static final Long QUOTATION_ID = 100L;
    private static final Long WAREHOUSE_ID = 10L;
    private static final Long PRODUCT_ID = 50L;
    private static final Long CLIENT_ID = 200L;

    @BeforeEach
    void setUp() {
        quotationRepository = mock(QuotationRepository.class);
        clientRepository = mock(ClientRepository.class);
        productRepository = mock(ProductRepository.class);
        companyRepository = mock(CompanyRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        walkInClientProvider = mock(WalkInClientProvider.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);
        comercialService = mock(ComercialService.class);

        quotationService = new QuotationService(quotationRepository, clientRepository, productRepository,
                companyRepository, warehouseRepository, walkInClientProvider, documentNumberService,
                auditLogService, comercialService);

        tillSessionRepository = mock(TillSessionRepository.class);
        tillMovementRepository = mock(TillMovementRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        treasuryAccountRepository = mock(TreasuryAccountRepository.class);
        inventoryService = mock(InventoryService.class);
        financeService = mock(FinanceService.class);
        paymentEntryRepository = mock(PaymentEntryRepository.class);
        creditNoteService = mock(CreditNoteService.class);
        receivablesService = mock(ReceivablesService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        storeVoucherRepository = mock(StoreVoucherRepository.class);
        creditNoteRepository = mock(CreditNoteRepository.class);

        posService = new POSService(
                tillSessionRepository, tillMovementRepository, invoiceRepository, clientRepository,
                productRepository, warehouseRepository, companyRepository, treasuryAccountRepository,
                inventoryService, financeService, paymentEntryRepository, walkInClientProvider,
                documentNumberService, auditLogService, creditNoteService, receivablesService,
                eventPublisher, storeVoucherRepository, creditNoteRepository, quotationRepository
        );

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Empresa Teste Lda");

        warehouse = new Warehouse();
        warehouse.setId(WAREHOUSE_ID);
        warehouse.setName("Armazém Central");
        warehouse.setCompany(company);

        product = new Product();
        product.setId(PRODUCT_ID);
        product.setSku("PROD-001");
        product.setName("Produto A");
        product.setUnitPrice(new BigDecimal("1000.00")); // Catálogo: 1000 MT

        client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Cliente Especial");
        client.setTaxId("123456789");

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("operador", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Quotation createTestQuotation(QuotationStatus status, int validityDays, BigDecimal quotedPrice) {
        Quotation q = new Quotation();
        q.setId(QUOTATION_ID);
        q.setQuotationNumber("CT-2026/001");
        q.setQuotationDate(LocalDateTime.now());
        q.setValidUntil(LocalDate.now().plusDays(validityDays));
        q.setCompany(company);
        q.setWarehouse(warehouse);
        q.setClient(client);
        q.setStatus(status);
        q.setTotalBeforeTax(quotedPrice);
        q.setTaxAmount(quotedPrice.multiply(new BigDecimal("0.16")));
        q.setTotalAmount(quotedPrice.multiply(new BigDecimal("1.16")));

        QuotationLine line = new QuotationLine();
        line.setQuotation(q);
        line.setProduct(product);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(quotedPrice); // Preço cotado especial (ex: 850 MT)
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(quotedPrice.multiply(new BigDecimal("1.16")));
        q.getLines().add(line);

        return q;
    }

    @Test
    @DisplayName("CONV-01: Conversão direta de cotação em fatura comercial com preços acordados")
    void testConvertToInvoice_Success() {
        BigDecimal quotedPrice = new BigDecimal("850.00");
        Quotation quotation = createTestQuotation(QuotationStatus.ACCEPTED, 10, quotedPrice);

        when(quotationRepository.findByIdWithLinesAndCompanyId(QUOTATION_ID, COMPANY_ID))
                .thenReturn(Optional.of(quotation));

        InvoiceDTO expectedInvoice = new InvoiceDTO(
                555L, "FT-2026/001", CLIENT_ID, "Cliente Especial", "123456789",
                quotation.getTotalBeforeTax(), quotation.getTaxAmount(), quotation.getTotalAmount(),
                BigDecimal.ZERO, InvoiceStatus.APPROVED, null, List.of(),
                LocalDateTime.now(), "operador", LocalDate.now().plusDays(30), 0,
                mz.multicore.erp.modules.comercial.model.AgingBucket.CORRENTE
        );

        when(comercialService.createInvoiceFromQuotation(quotation)).thenReturn(expectedInvoice);
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceDTO result = quotationService.convertToInvoice(QUOTATION_ID);

        assertNotNull(result);
        assertEquals(555L, result.id());
        assertEquals("FT-2026/001", result.invoiceNumber());
        assertEquals(QuotationStatus.CONVERTED, quotation.getStatus());
        assertEquals(555L, quotation.getInvoiceId());
        assertEquals("FT-2026/001", quotation.getInvoiceNumber());

        verify(comercialService).createInvoiceFromQuotation(quotation);
        verify(quotationRepository).save(quotation);
        verify(auditLogService).logCurrent(eq("QUOTATION_CONVERT_INVOICE"), contains("FT-2026/001"));
    }

    @Test
    @DisplayName("CONV-02: Bloqueio de conversão em fatura de cotação expirada")
    void testConvertToInvoice_Expired_Throws() {
        Quotation expired = createTestQuotation(QuotationStatus.DRAFT, -2, new BigDecimal("850.00"));

        when(quotationRepository.findByIdWithLinesAndCompanyId(QUOTATION_ID, COMPANY_ID))
                .thenReturn(Optional.of(expired));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> quotationService.convertToInvoice(QUOTATION_ID));

        assertTrue(ex.getMessage().contains("caducou"));
        verify(comercialService, never()).createInvoiceFromQuotation(any());
        verify(quotationRepository, never()).save(any());
    }

    @Test
    @DisplayName("CONV-03: Bloqueio de dupla conversão ou cotação rejeitada")
    void testConvertToInvoice_AlreadyConverted_Throws() {
        Quotation converted = createTestQuotation(QuotationStatus.CONVERTED, 10, new BigDecimal("850.00"));
        converted.setInvoiceNumber("FT-2026/999");

        when(quotationRepository.findByIdWithLinesAndCompanyId(QUOTATION_ID, COMPANY_ID))
                .thenReturn(Optional.of(converted));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> quotationService.convertToInvoice(QUOTATION_ID));

        assertTrue(ex.getMessage().contains("já foi convertida"));
        verify(comercialService, never()).createInvoiceFromQuotation(any());
    }

    @Test
    @DisplayName("CONV-04: Consulta de cotações abertas vigentes (findOpenByCompany) filtra caducadas")
    void testFindOpenByCompany_FiltersExpired() {
        Quotation validOpen = createTestQuotation(QuotationStatus.SENT, 5, new BigDecimal("850.00"));
        Quotation expiredOpen = createTestQuotation(QuotationStatus.SENT, -3, new BigDecimal("850.00"));
        expiredOpen.setId(101L);

        when(quotationRepository.findOpenByCompanyIdWithLines(COMPANY_ID))
                .thenReturn(List.of(validOpen, expiredOpen));

        List<QuotationDTO> openList = quotationService.findOpenByCompany(COMPANY_ID);

        assertEquals(1, openList.size());
        assertEquals(QUOTATION_ID, openList.get(0).id());
        assertFalse(openList.get(0).expired());
    }

    @Test
    @DisplayName("CONV-05: Checkout no POS com liquidação e conversão da cotação associada")
    void testPOSCheckout_WithQuotation_ConvertsQuotation() {
        // Setup sessão de caixa aberta
        TillSession session = new TillSession();
        session.setId(1L);
        session.setOperator("operador");
        session.setStatus("OPEN");
        session.setCompany(company);

        when(tillSessionRepository.findActiveSessionForOperator("operador", "OPEN", COMPANY_ID))
                .thenReturn(Optional.of(session));

        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(clientRepository.findByIdAndCompaniesId(CLIENT_ID, COMPANY_ID)).thenReturn(Optional.of(client));
        when(productRepository.findByIdAndCompaniesId(PRODUCT_ID, COMPANY_ID)).thenReturn(Optional.of(product));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT-2026/088");

        Quotation quotation = createTestQuotation(QuotationStatus.SENT, 7, new BigDecimal("750.00"));
        when(quotationRepository.findByIdAndCompanyId(QUOTATION_ID, COMPANY_ID)).thenReturn(Optional.of(quotation));

        // Checkout com linha personalizada a 750 MT e quotationId
        BigDecimal customPrice = new BigDecimal("750.00");
        List<POSCheckoutLineRequest> lines = List.of(
                new POSCheckoutLineRequest(PRODUCT_ID, BigDecimal.ONE, BigDecimal.ZERO, null, null, customPrice)
        );
        BigDecimal totalWithTax = customPrice.multiply(new BigDecimal("1.16"));
        List<PosPaymentRequest> payments = List.of(
                new PosPaymentRequest("CASH", totalWithTax, totalWithTax, "Numerário", null)
        );

        POSCheckoutRequest request = new POSCheckoutRequest(
                "operador", COMPANY_ID, CLIENT_ID, null, WAREHOUSE_ID, null, lines, payments, QUOTATION_ID
        );

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(777L);
            return i;
        });

        Invoice checkoutResult = posService.checkout(request);

        assertNotNull(checkoutResult);
        assertEquals("FT-2026/088", checkoutResult.getInvoiceNumber());

        // Verifica que a cotação foi convertida e associada à fatura emitida
        assertEquals(QuotationStatus.CONVERTED, quotation.getStatus());
        assertEquals(777L, quotation.getInvoiceId());
        assertEquals("FT-2026/088", quotation.getInvoiceNumber());
        assertNotNull(quotation.getDecidedAt());

        verify(quotationRepository).save(quotation);
        verify(auditLogService).logCurrent(eq("QUOTATION_CONVERT_POS"), contains("FT-2026/088"));
    }
}
