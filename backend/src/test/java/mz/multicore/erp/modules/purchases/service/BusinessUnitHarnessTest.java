package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.*;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.ReceivablesService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Harness test rigoroso para validação das regras de Gestão de Negócios (Business Units / Linhas de Distribuição):
 * 1. Criação e código único por empresa.
 * 2. Associação opcional a fornecedor fabricante principal.
 * 3. Faturação em modo misto (sucesso com produtos de negócios variados).
 * 4. Faturação restrita por negócio (bloqueio estrito se contiver produto de outro negócio).
 * 5. Faturação restrita por negócio (sucesso quando todos os produtos pertencem ao mesmo negócio).
 */
class BusinessUnitHarnessTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long WAREHOUSE_ID = 20L;
    private static final Long CLIENT_ID = 10L;

    private BusinessUnitRepository businessUnitRepository;
    private SupplierRepository supplierRepository;
    private CompanyRepository companyRepository;
    private ProductRepository productRepository;
    private AuditLogService auditLogService;
    private BusinessUnitService businessUnitService;

    // Dependências ComercialService
    private ClientRepository clientRepository;
    private InvoiceRepository invoiceRepository;
    private WarehouseRepository warehouseRepository;
    private InventoryService inventoryService;
    private DocumentNumberService documentNumberService;
    private ReceivablesService receivablesService;
    private ComercialService comercialService;

    private Company company;
    private Client client;
    private Warehouse warehouse;
    private Supplier unileverSupplier;
    private BusinessUnit unileverBu;
    private BusinessUnit batBu;
    private Product omoProduct;
    private Product rothmansProduct;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Distribuidora Nacional Lda");

        client = new Client();
        client.setId(CLIENT_ID);
        client.setName("Mercearia Central");

        warehouse = new Warehouse();
        warehouse.setId(WAREHOUSE_ID);
        warehouse.setName("Armazém Geral");
        warehouse.setCompany(company);

        unileverSupplier = new Supplier();
        unileverSupplier.setId(100L);
        unileverSupplier.setName("Unilever Moçambique Lda");
        unileverSupplier.setCompany(company);

        unileverBu = new BusinessUnit();
        unileverBu.setId(1L);
        unileverBu.setCompany(company);
        unileverBu.setCode("UNL");
        unileverBu.setName("Unilever");
        unileverBu.setSupplier(unileverSupplier);
        unileverBu.setActive(true);

        batBu = new BusinessUnit();
        batBu.setId(2L);
        batBu.setCompany(company);
        batBu.setCode("BAT");
        batBu.setName("British American Tobacco");
        batBu.setActive(true);

        omoProduct = new Product();
        omoProduct.setId(501L);
        omoProduct.setSku("OMO-1KG");
        omoProduct.setName("Detergente Omo 1kg");
        omoProduct.setUnitPrice(new BigDecimal("150.00"));
        omoProduct.setPurchasePrice(new BigDecimal("100.00"));
        omoProduct.setBusinessUnit(unileverBu);
        omoProduct.getCompanies().add(company);

        rothmansProduct = new Product();
        rothmansProduct.setId(502L);
        rothmansProduct.setSku("ROTH-20");
        rothmansProduct.setName("Cigarros Rothmans 20s");
        rothmansProduct.setUnitPrice(new BigDecimal("120.00"));
        rothmansProduct.setPurchasePrice(new BigDecimal("80.00"));
        rothmansProduct.setBusinessUnit(batBu);
        rothmansProduct.getCompanies().add(company);

        businessUnitRepository = mock(BusinessUnitRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        companyRepository = mock(CompanyRepository.class);
        productRepository = mock(ProductRepository.class);
        auditLogService = mock(AuditLogService.class);

        businessUnitService = new BusinessUnitService(
                businessUnitRepository,
                supplierRepository,
                companyRepository,
                productRepository,
                auditLogService
        );

        clientRepository = mock(ClientRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        inventoryService = mock(InventoryService.class);
        documentNumberService = mock(DocumentNumberService.class);
        receivablesService = mock(ReceivablesService.class);

        comercialService = new ComercialService(
                clientRepository,
                productRepository,
                mock(ProductCategoryRepository.class),
                invoiceRepository,
                mock(mz.multicore.erp.modules.approvals.service.ApprovalService.class),
                companyRepository,
                warehouseRepository,
                inventoryService,
                mock(ReceiptRepository.class),
                mock(mz.multicore.erp.modules.financeira.service.FinanceService.class),
                mock(mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository.class),
                mock(OrderRepository.class),
                mock(OrderLineRepository.class),
                mock(WalkInClientProvider.class),
                documentNumberService,
                auditLogService,
                mock(mz.multicore.erp.modules.fiscal.repository.TaxRateRepository.class),
                receivablesService,
                mock(ApplicationEventPublisher.class),
                null,
                businessUnitRepository
        );

        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(clientRepository.findByIdAndCompaniesId(CLIENT_ID, COMPANY_ID)).thenReturn(Optional.of(client));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT-2026/10");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(999L);
            return i;
        });

        when(businessUnitRepository.findById(1L)).thenReturn(Optional.of(unileverBu));
        when(businessUnitRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(unileverBu));
        when(businessUnitRepository.findById(2L)).thenReturn(Optional.of(batBu));
        when(businessUnitRepository.findByIdAndCompanyId(2L, COMPANY_ID)).thenReturn(Optional.of(batBu));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. GESTÃO DE UNIDADES DE NEGÓCIO (CRUD E REGRAS)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void criarUnidadeNegocio_sucessoERegistaAuditoria() {
        when(businessUnitRepository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "UNL")).thenReturn(false);
        when(supplierRepository.findById(100L)).thenReturn(Optional.of(unileverSupplier));
        when(businessUnitRepository.save(any(BusinessUnit.class))).thenAnswer(inv -> {
            BusinessUnit bu = inv.getArgument(0);
            bu.setId(10L);
            return bu;
        });

        CreateBusinessUnitRequest req = new CreateBusinessUnitRequest("unl", "Unilever Moçambique", 100L, "Linha de higiene e alimentação");
        BusinessUnitDTO dto = businessUnitService.create(req);

        assertNotNull(dto);
        assertEquals("UNL", dto.code(), "O código deve ser gravado em maiúsculas");
        assertEquals("Unilever Moçambique", dto.name());
        assertEquals(100L, dto.supplierId());
        assertEquals("Unilever Moçambique Lda", dto.supplierName());
        assertTrue(dto.active());

        verify(auditLogService).logCurrent(eq("BUSINESS_UNIT_CREATED"), contains("UNL"));
    }

    @Test
    void criarUnidadeNegocio_codigoDuplicadoNaMesmaEmpresa_bloqueia() {
        when(businessUnitRepository.existsByCompanyIdAndCodeIgnoreCase(COMPANY_ID, "BAT")).thenReturn(true);

        CreateBusinessUnitRequest req = new CreateBusinessUnitRequest("BAT", "British American Tobacco", null, null);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> businessUnitService.create(req));

        assertTrue(ex.getMessage().contains("Já existe uma linha de negócio com o código 'BAT'"));
        verify(businessUnitRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. FATURAÇÃO EM MODO MISTO (SEM RESTRIÇÃO)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void faturar_modoMistoSemRestricao_aceitaProdutosDeNegociosDiferentes() {
        when(productRepository.findByIdAndCompaniesId(501L, COMPANY_ID)).thenReturn(Optional.of(omoProduct));
        when(productRepository.findByIdAndCompaniesId(502L, COMPANY_ID)).thenReturn(Optional.of(rothmansProduct));

        // businessUnitId == null: modo misto livre
        CreateInvoiceRequest req = new CreateInvoiceRequest(
                CLIENT_ID, COMPANY_ID, WAREHOUSE_ID,
                List.of(
                        new CreateInvoiceLineRequest(501L, new BigDecimal("2"), BigDecimal.ZERO, BigDecimal.ZERO, null, null), // Unilever
                        new CreateInvoiceLineRequest(502L, new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO, null, null)  // BAT
                ),
                null,
                null // sem restrição de negócio
        );

        InvoiceDTO invoice = comercialService.createInvoice(req);

        assertNotNull(invoice);
        assertEquals(2, invoice.lines().size());
        assertNull(invoice.businessUnitId(), "Modo misto não restringe a fatura a uma linha exclusiva");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. FATURAÇÃO EM MODO RESTRITO (EXCLUSIVO POR LINHA DE NEGÓCIO)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void faturar_modoRestrito_comProdutoDeOutroNegocio_bloqueiaComMensagemExplicativa() {
        when(productRepository.findByIdAndCompaniesId(501L, COMPANY_ID)).thenReturn(Optional.of(omoProduct));
        when(productRepository.findByIdAndCompaniesId(502L, COMPANY_ID)).thenReturn(Optional.of(rothmansProduct));

        // businessUnitId = 1L (Unilever), mas tentando incluir produto 502 (BAT)
        CreateInvoiceRequest req = new CreateInvoiceRequest(
                CLIENT_ID, COMPANY_ID, WAREHOUSE_ID,
                List.of(
                        new CreateInvoiceLineRequest(501L, new BigDecimal("2"), BigDecimal.ZERO, BigDecimal.ZERO, null, null), // Unilever OK
                        new CreateInvoiceLineRequest(502L, new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO, null, null)  // BAT -> DIVERGENTE
                ),
                null,
                1L // Restrito a Unilever
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> comercialService.createInvoice(req));

        assertTrue(ex.getMessage().contains("A fatura está restrita à linha de negócio 'Unilever'"),
                "Deveria avisar da restrição da linha: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Cigarros Rothmans 20s"),
                "Deveria citar o produto rejeitado: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Desmarque o filtro de negócio para emitir produtos mistos"),
                "Deveria orientar como emitir misto: " + ex.getMessage());

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void faturar_modoRestrito_comTodosProdutosDoMesmoNegocio_sucessoEAssociaLinhaAFatura() {
        when(productRepository.findByIdAndCompaniesId(501L, COMPANY_ID)).thenReturn(Optional.of(omoProduct));

        CreateInvoiceRequest req = new CreateInvoiceRequest(
                CLIENT_ID, COMPANY_ID, WAREHOUSE_ID,
                List.of(
                        new CreateInvoiceLineRequest(501L, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, null, null) // Apenas Unilever
                ),
                null,
                1L // Restrito a Unilever
        );

        InvoiceDTO invoice = comercialService.createInvoice(req);

        assertNotNull(invoice);
        assertEquals(1L, invoice.businessUnitId());
        assertEquals("Unilever", invoice.businessUnitName());
        verify(invoiceRepository).save(any(Invoice.class));
    }
}
