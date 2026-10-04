package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
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
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Harness de validação do Modo de Contingência / Offline-First do POS (PCR-01 a PCR-04).
 */
class PosContingencyHarnessTest {

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
    private ApplicationEventPublisher eventPublisher;
    private mz.multicore.erp.modules.pos.repository.StoreVoucherRepository storeVoucherRepository;
    private mz.multicore.erp.modules.comercial.repository.CreditNoteRepository creditNoteRepository;

    private POSService posService;

    private Company company;
    private Warehouse warehouse;
    private Product product;
    private TillSession openSession;
    private Client walkInClient;

    private static final String OPERATOR = "operador_pos";
    private static final Long COMPANY_ID = 10L;
    private static final Long WAREHOUSE_ID = 20L;
    private static final Long PRODUCT_ID = 30L;
    private static final Long ACCOUNT_ID = 40L;

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
        eventPublisher = mock(ApplicationEventPublisher.class);
        storeVoucherRepository = mock(mz.multicore.erp.modules.pos.repository.StoreVoucherRepository.class);
        creditNoteRepository = mock(mz.multicore.erp.modules.comercial.repository.CreditNoteRepository.class);

        posService = new POSService(
                tillSessionRepository,
                tillMovementRepository,
                invoiceRepository,
                clientRepository,
                productRepository,
                warehouseRepository,
                companyRepository,
                treasuryAccountRepository,
                inventoryService,
                financeService,
                paymentEntryRepository,
                walkInClientProvider,
                documentNumberService,
                auditLogService,
                creditNoteService,
                receivablesService,
                eventPublisher,
                storeVoucherRepository,
                creditNoteRepository
        );

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Empresa Contingência");

        warehouse = new Warehouse();
        warehouse.setId(WAREHOUSE_ID);
        warehouse.setName("Loja Balcão");
        warehouse.setCompany(company);

        product = new Product();
        product.setId(PRODUCT_ID);
        product.setSku("SKU-CONT");
        product.setName("Produto Teste");
        product.setUnitPrice(new BigDecimal("100.00"));
        product.setPurchasePrice(new BigDecimal("60.00"));

        openSession = new TillSession();
        openSession.setId(50L);
        openSession.setOperator(OPERATOR);
        openSession.setCompany(company);
        openSession.setStatus("OPEN");
        openSession.setOpeningBalance(new BigDecimal("100.00"));

        walkInClient = new Client();
        walkInClient.setId(999L);
        walkInClient.setName("Consumidor Final");

        CurrentUserContext.setCurrentUser("operador", "CASHIER");
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);

        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(tillSessionRepository.findActiveSessionForOperator(OPERATOR, "OPEN", COMPANY_ID))
                .thenReturn(Optional.of(openSession));
        when(walkInClientProvider.getOrCreate()).thenReturn(walkInClient);
        when(productRepository.findByIdAndCompaniesId(PRODUCT_ID, COMPANY_ID)).thenReturn(Optional.of(product));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT 2026/0099");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(777L);
            return i;
        });
        when(paymentEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tillMovementRepository.save(any(TillMovement.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("PCR-01: Checkout com referência de contingência grava o campo na entidade Invoice")
    void pcr01_checkoutWithContingencyReferenceSavesReference() {
        String contingencyRef = "CONT-20260917-192500-A1B2";
        when(invoiceRepository.findByCompanyIdAndContingencyReference(COMPANY_ID, contingencyRef))
                .thenReturn(Optional.empty());

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR,
                COMPANY_ID,
                null,
                "Cliente Avulso",
                WAREHOUSE_ID,
                null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, new BigDecimal("1"), null, null, null)),
                List.of(new PosPaymentRequest("CASH", new BigDecimal("100.00"), null, null, null)),
                contingencyRef
        );

        Invoice result = posService.checkout(request);

        assertNotNull(result);
        assertEquals(contingencyRef, result.getContingencyReference());
        assertEquals("FT 2026/0099", result.getInvoiceNumber());
        verify(invoiceRepository).findByCompanyIdAndContingencyReference(COMPANY_ID, contingencyRef);
        verify(documentNumberService, times(1)).next(DocumentSeries.INVOICE);
    }

    @Test
    @DisplayName("PCR-02: Idempotência — se a venda já foi sincronizada anteriormente, devolve a mesma fatura sem duplicar")
    void pcr02_idempotencyReturnsExistingInvoiceWithoutDuplicateDeduction() {
        String contingencyRef = "CONT-20260917-192500-A1B2";

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(777L);
        existingInvoice.setInvoiceNumber("FT 2026/0088");
        existingInvoice.setContingencyReference(contingencyRef);
        existingInvoice.setStatus(InvoiceStatus.PAID);
        existingInvoice.setTotalAmount(new BigDecimal("100.00"));

        when(invoiceRepository.findByCompanyIdAndContingencyReference(COMPANY_ID, contingencyRef))
                .thenReturn(Optional.of(existingInvoice));

        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR,
                COMPANY_ID,
                null,
                "Cliente Avulso",
                WAREHOUSE_ID,
                null,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, new BigDecimal("1"), null, null, null)),
                List.of(new PosPaymentRequest("CASH", new BigDecimal("100.00"), null, null, null)),
                contingencyRef
        );

        Invoice result = posService.checkout(request);

        assertSame(existingInvoice, result);
        assertEquals("FT 2026/0088", result.getInvoiceNumber());
        // Garante que não gerou novo número nem salvou nova fatura nem chamou sessão
        verify(documentNumberService, never()).next(any());
        verify(invoiceRepository, never()).save(any());
        verify(tillMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("PCR-03: Venda normal sem referência de contingência processa sequencialmente")
    void pcr03_normalCheckoutWithoutContingencyWorksNormally() {
        POSCheckoutRequest request = new POSCheckoutRequest(
                OPERATOR,
                COMPANY_ID,
                null,
                "Cliente Balcão",
                WAREHOUSE_ID,
                ACCOUNT_ID,
                List.of(new POSCheckoutLineRequest(PRODUCT_ID, new BigDecimal("1"), null, null, null)),
                null
        );

        Invoice result = posService.checkout(request);

        assertNotNull(result);
        assertNull(result.getContingencyReference());
        assertEquals("FT 2026/0099", result.getInvoiceNumber());
        verify(invoiceRepository, never()).findByCompanyIdAndContingencyReference(anyLong(), anyString());
        verify(documentNumberService, times(1)).next(DocumentSeries.INVOICE);
    }
}
