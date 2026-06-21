package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.CreateInvoiceLineRequest;
import com.phcpro.modules.comercial.dto.CreateInvoiceRequest;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.model.Order;
import com.phcpro.modules.comercial.model.OrderLine;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.comercial.repository.OrderLineRepository;
import com.phcpro.modules.comercial.repository.OrderRepository;
import com.phcpro.modules.comercial.repository.ProductCategoryRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.comercial.repository.ReceiptRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
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
 * Testes do ComercialService. Foco na nova regra de faturação: emitir fatura é operação
 * directa de quem tem perfil MANAGER/ADMIN (não passa pela Engine de Aprovações); o stock
 * baixa no acto; só o desconto sensível (>10%) volta a exigir aprovação. Cobre também
 * faturação de encomenda e anulação com reposição de stock. Dependências mockadas.
 */
class ComercialServiceTest {

    private ClientRepository clientRepository;
    private ProductRepository productRepository;
    private ProductCategoryRepository productCategoryRepository;
    private InvoiceRepository invoiceRepository;
    private ApprovalService approvalService;
    private CompanyRepository companyRepository;
    private WarehouseRepository warehouseRepository;
    private InventoryService inventoryService;
    private ReceiptRepository receiptRepository;
    private FinanceService financeService;
    private TreasuryAccountRepository treasuryAccountRepository;
    private OrderRepository orderRepository;
    private OrderLineRepository orderLineRepository;
    private WalkInClientProvider walkInClientProvider;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private ComercialService service;

    private Company company;
    private Client client;
    private Warehouse warehouse;
    private Product product;

    private static final Long COMPANY_ID = 1L;
    private static final Long CLIENT_ID = 5L;
    private static final Long WAREHOUSE_ID = 10L;
    private static final Long PRODUCT_ID = 100L;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        productRepository = mock(ProductRepository.class);
        productCategoryRepository = mock(ProductCategoryRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        approvalService = mock(ApprovalService.class);
        companyRepository = mock(CompanyRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        inventoryService = mock(InventoryService.class);
        receiptRepository = mock(ReceiptRepository.class);
        financeService = mock(FinanceService.class);
        treasuryAccountRepository = mock(TreasuryAccountRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderLineRepository = mock(OrderLineRepository.class);
        walkInClientProvider = mock(WalkInClientProvider.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);

        service = new ComercialService(clientRepository, productRepository, productCategoryRepository,
                invoiceRepository, approvalService, companyRepository, warehouseRepository, inventoryService,
                receiptRepository, financeService, treasuryAccountRepository, orderRepository, orderLineRepository,
                walkInClientProvider, documentNumberService, auditLogService);

        company = company(COMPANY_ID);
        client = client(CLIENT_ID, "Cliente Loja");
        warehouse = warehouse(WAREHOUSE_ID, company);
        product = product(PRODUCT_ID, new BigDecimal("100"));

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ────────────────────────── createInvoice ──────────────────────────

    @Test
    void createInvoice_direta_ficaAprovada_baixaStock_eNaoSubmeteAprovacao() {
        stubInvoiceLookups();

        InvoiceDTO dto = service.createInvoice(invoiceRequest(new BigDecimal("2"), /*discount*/ null));

        assertEquals(InvoiceStatus.APPROVED, dto.status());
        assertEquals("FT-2026/1", dto.invoiceNumber());
        verify(documentNumberService).next(DocumentSeries.INVOICE);
        // Saída de stock imediata: quantidade negativa, tipo SALE.
        verify(inventoryService).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("2").negate()), eq("SALE"),
                any(), any(), any());
        // NÃO passa pela Engine de Aprovações.
        verify(approvalService, never()).submitRequest(any(), any(), any(), any());
        verify(auditLogService).logCurrent(eq("INVOICE_ISSUE"), any());
    }

    @Test
    void createInvoice_descontoAcimaDe10_exigeAprovacao_eNaoBaixaStock() {
        stubInvoiceLookups();

        InvoiceDTO dto = service.createInvoice(invoiceRequest(new BigDecimal("2"), new BigDecimal("15")));

        assertEquals(InvoiceStatus.PENDING_DISCOUNT_APPROVAL, dto.status());
        // Stock só baixa na aprovação (callback) — aqui não há movimento.
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
        verify(approvalService).submitRequest(eq("INVOICE"), any(), any(), any());
    }

    @Test
    void createInvoice_semPerfilAutorizado_bloqueia() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> service.createInvoice(invoiceRequest(new BigDecimal("1"), null)));
        verify(invoiceRepository, never()).save(any());
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    // ────────────────────────── billOrder ──────────────────────────

    @Test
    void billOrder_encomendaPendente_faturaAprovada_baixaStock_eMarcaBilled() {
        Order order = pendingOrder(new BigDecimal("3"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceDTO dto = service.billOrder(1L);

        assertEquals(InvoiceStatus.APPROVED, dto.status());
        assertEquals("BILLED", order.getStatus());
        verify(inventoryService).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("3").negate()), eq("SALE"),
                any(), any(), any());
    }

    @Test
    void billOrder_encomendaJaFaturada_bloqueia() {
        Order order = pendingOrder(new BigDecimal("3"));
        order.setStatus("BILLED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> service.billOrder(1L));
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    // ────────────────────────── cancelInvoice ──────────────────────────

    @Test
    void cancelInvoice_aprovada_repoeStock_eFicaCancelada() {
        Invoice invoice = approvedInvoice(new BigDecimal("2"));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(receiptRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancelInvoice(1L, "Cliente desistiu");

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        // Reposição de stock = quantidade positiva, tipo REVERSAL.
        verify(inventoryService).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("2")), eq("REVERSAL"),
                any(), any(), any());
    }

    @Test
    void cancelInvoice_semPerfilAutorizado_bloqueia() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.cancelInvoice(1L, "motivo"));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_semMotivo_bloqueia() {
        assertThrows(BusinessRuleException.class, () -> service.cancelInvoice(1L, "  "));
    }

    // ────────────────────────── helpers ──────────────────────────

    private void stubInvoiceLookups() {
        when(clientRepository.findByIdAndCompaniesId(CLIENT_ID, COMPANY_ID)).thenReturn(Optional.of(client));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(productRepository.findByIdAndCompaniesId(PRODUCT_ID, COMPANY_ID)).thenReturn(Optional.of(product));
        when(documentNumberService.next(DocumentSeries.INVOICE)).thenReturn("FT-2026/1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateInvoiceRequest invoiceRequest(BigDecimal qty, BigDecimal discount) {
        return new CreateInvoiceRequest(CLIENT_ID, COMPANY_ID, WAREHOUSE_ID,
                List.of(new CreateInvoiceLineRequest(PRODUCT_ID, qty, new BigDecimal("0.16"),
                        discount, null, null)));
    }

    private Order pendingOrder(BigDecimal qty) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("EC-2026/1");
        order.setClient(client);
        order.setCompany(company);
        order.setWarehouse(warehouse);
        order.setStatus("PENDING");
        order.setTotalBeforeTax(new BigDecimal("300"));
        order.setTaxAmount(new BigDecimal("48"));
        order.setTotalAmount(new BigDecimal("348"));
        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal("348"));
        order.addLine(line);
        return order;
    }

    private Invoice approvedInvoice(BigDecimal qty) {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("FT-2026/1");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setWarehouse(warehouse);
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setTotalAmount(new BigDecimal("232"));
        com.phcpro.modules.comercial.model.InvoiceLine line = new com.phcpro.modules.comercial.model.InvoiceLine();
        line.setProduct(product);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal("232"));
        invoice.addLine(line);
        return invoice;
    }

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Client client(long id, String name) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
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
}
