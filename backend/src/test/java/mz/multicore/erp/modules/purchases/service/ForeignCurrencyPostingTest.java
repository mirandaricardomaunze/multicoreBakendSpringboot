package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.PurchaseOrderLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness FX-07 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3): o lançamento automático da compra é feito na
 * moeda base, ao câmbio gravado no documento, com as mesmas contas de hoje. O facto contabilístico publicado pela
 * compra directa e pela compra nascida da recepção da encomenda leva os valores já convertidos e equilibrados
 * (líquido + IVA = total em moeda base); o pagamento no acto sai da tesouraria (sempre em moeda base) pelo
 * total em moeda base.
 */
class ForeignCurrencyPostingTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long ACCOUNT_ID = 5L;

    private final SupplierRepository supplierRepository = mock(SupplierRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
    private final FinanceService finance = mock(FinanceService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private final DirectPurchaseRegistrationService registration = new DirectPurchaseRegistrationService(
            supplierRepository, purchaseRepository,
            new ProductLookup(productRepository, mock(ProductCategoryRepository.class)),
            new WarehouseLookup(warehouseRepository), new CompanyLookup(companyRepository),
            mock(InventoryService.class), finance, numbers, events, new PurchaseCurrencyResolver(exchangeRates));

    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("comprador", "MANAGER");
        Company company = company();
        supplier = new Supplier();
        supplier.setId(7L);
        supplier.setName("Shenzhen Trading");
        supplier.setActive(true);
        supplier.setCompany(company);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(3L);
        warehouse.setCompany(company);
        product = new Product();
        product.setId(11L);
        product.setName("Telemóvel");
        product.setSku("TEL-1");
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(warehouse));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID)).thenReturn(Optional.of(product));
        when(numbers.next(anyString())).thenReturn("CP-2026/1");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(call -> call.getArgument(0));
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class),
                eq(new BigDecimal("63.9055")))).thenReturn(new BigDecimal("63.9055"));
        when(finance.accountType(ACCOUNT_ID)).thenReturn(TreasuryAccountType.BANK);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void fx07_compraDirectaEmUsdPublicaLancamentoEmMoedaBaseAoCambioDoDocumento() {
        // 10 × 100 USD + IVA 16% = 1000 + 160 = 1160 USD × 63,9055
        registration.createPurchase(usdRequest(null));

        PurchaseRegisteredEvent event = publishedEvent();
        assertEquals(new BigDecimal("74130.38"), event.totalAmount(), "1160 × 63,9055 = 74130,378 -> 74130,38");
        assertEquals(new BigDecimal("10224.88"), event.taxAmount(), "160 × 63,9055 = 10224,88");
        assertEquals(new BigDecimal("63905.50"), event.netAmount(), "líquido = total − IVA, em moeda base");
        assertEquals(0, event.netAmount().add(event.taxAmount()).compareTo(event.totalAmount()),
                "o lançamento fica equilibrado");
        assertEquals(0, BigDecimal.ZERO.compareTo(event.amountPaidNow()));
    }

    @Test
    void fx07_pagamentoNoActoSaiDaTesourariaETambemNoLancamentoEmMoedaBase() {
        registration.createPurchase(usdRequest(ACCOUNT_ID));

        PurchaseRegisteredEvent event = publishedEvent();
        assertEquals(new BigDecimal("74130.38"), event.amountPaidNow());
        assertFalse(event.cashPayment());
        verify(finance).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("74130.38")),
                anyString());
    }

    @Test
    void fx07_compraNaMoedaBaseLancaComoHoje() {
        registration.createPurchase(new CreatePurchaseRequest(7L, 3L, COMPANY_ID, ACCOUNT_ID, List.of(line())));

        PurchaseRegisteredEvent event = publishedEvent();
        assertEquals(new BigDecimal("1160.00"), event.totalAmount());
        assertEquals(new BigDecimal("160.00"), event.taxAmount());
        assertEquals(new BigDecimal("1000.00"), event.netAmount());
        assertEquals(new BigDecimal("1160.00"), event.amountPaidNow());
        verify(finance).registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("1160.00")),
                anyString());
    }

    @Test
    void fx07_compraDaRecepcaoDaEncomendaLancaEmMoedaBaseAoCambioDaEncomenda() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(99L);
        order.setOrderNumber("EC-F-2026/1");
        order.setCompany(company());
        order.setSupplier(supplier);
        order.setCurrency("ZAR");
        order.setExchangeRate(new BigDecimal("3.4567"));
        PurchaseOrderLine orderLine = new PurchaseOrderLine();
        orderLine.setProduct(product);
        orderLine.setQuantity(BigDecimal.TEN);
        orderLine.setUnitPrice(new BigDecimal("100"));
        orderLine.setTaxRate(new BigDecimal("0.16"));
        order.addLine(orderLine);

        new PurchaseOrderPayableRegistrar(purchaseRepository, numbers, events, mock(AuditLogService.class))
                .register(order, Map.of(orderLine, BigDecimal.TEN));

        PurchaseRegisteredEvent event = publishedEvent();
        assertEquals(new BigDecimal("4009.77"), event.totalAmount(), "1160 ZAR × 3,4567 = 4009,772 -> 4009,77");
        assertEquals(new BigDecimal("553.07"), event.taxAmount(), "160 × 3,4567 = 553,072 -> 553,07");
        assertEquals(new BigDecimal("3456.70"), event.netAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(event.amountPaidNow()));
    }

    private PurchaseRegisteredEvent publishedEvent() {
        ArgumentCaptor<PurchaseRegisteredEvent> captor = ArgumentCaptor.forClass(PurchaseRegisteredEvent.class);
        verify(events).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static CreatePurchaseRequest usdRequest(Long financeAccountId) {
        return new CreatePurchaseRequest(7L, 3L, COMPANY_ID, financeAccountId, List.of(line()), "USD",
                new BigDecimal("63.9055"));
    }

    private static CreatePurchaseLineRequest line() {
        return new CreatePurchaseLineRequest(11L, BigDecimal.TEN, new BigDecimal("100"), null, null, null,
                new BigDecimal("0.16"));
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        return company;
    }
}
