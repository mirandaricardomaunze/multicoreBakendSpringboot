package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
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
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.inventory.service.WeightedAverageCost;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.CurrencyBalanceDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;
import mz.multicore.erp.modules.purchases.model.DocumentCurrency;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.PurchaseOrderLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderReceiptRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Harness FX-05 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3): encomenda e compra guardam moeda e
 * câmbio (fotografia); na moeda base o câmbio é 1; as compras antigas migram com câmbio 1 (FX-05). O documento
 * guarda também o total em moeda base (total × câmbio, 2 casas HALF_UP) e o stock e o custo médio entram ao
 * preço × câmbio (FX-06). Contas a pagar e extracto mostram a moeda, a dívida na moeda do documento e o
 * equivalente em moeda base, baixado ao câmbio do documento e sem resíduos na liquidação (FX-08).
 */
class ForeignCurrencyPurchaseTest {

    private static final Long COMPANY_ID = 1L;

    private final SupplierRepository supplierRepository = mock(SupplierRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final PurchaseOrderRepository orderRepository = mock(PurchaseOrderRepository.class);
    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final PurchaseCurrencyResolver resolver = new PurchaseCurrencyResolver(exchangeRates);

    private final PurchaseOrderService orders = new PurchaseOrderService(orderRepository,
            new PurchaseOrderLifecycleService(orderRepository, supplierRepository,
                    new ProductLookup(productRepository, mock(ProductCategoryRepository.class)),
                    new WarehouseLookup(warehouseRepository), new CompanyLookup(companyRepository), numbers,
                    mock(AuditLogService.class), resolver),
            mock(PurchaseOrderReceiptExecutor.class));

    private final DirectPurchaseService purchases = new DirectPurchaseService(
            new DirectPurchaseRegistrationService(supplierRepository, purchaseRepository,
                    new ProductLookup(productRepository, mock(ProductCategoryRepository.class)),
                    new WarehouseLookup(warehouseRepository), new CompanyLookup(companyRepository),
                    inventory, mock(FinanceService.class), numbers,
                    mock(ApplicationEventPublisher.class), resolver),
            new DirectPurchaseQueryService(purchaseRepository));

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("comprador", "MANAGER");
        Company company = company();
        Supplier supplier = new Supplier();
        supplier.setId(7L);
        supplier.setName("Shenzhen Trading");
        supplier.setActive(true);
        supplier.setCompany(company);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(3L);
        warehouse.setCompany(company);
        Product product = new Product();
        product.setId(11L);
        product.setName("Telemóvel");
        product.setSku("TEL-1");
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(warehouse));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID)).thenReturn(Optional.of(product));
        when(numbers.next(anyString())).thenReturn("DOC-2026/1");
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(call -> call.getArgument(0));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void fx05_encomendaEmUsdGuardaMoedaECambioEscrito() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class),
                eq(new BigDecimal("63.9")))).thenReturn(new BigDecimal("63.9000"));

        PurchaseOrderDTO dto = orders.createOrder(orderRequest("usd", new BigDecimal("63.9")));

        assertEquals("USD", dto.currency());
        assertEquals(new BigDecimal("63.9000"), dto.exchangeRate());
        PurchaseOrder saved = savedOrder();
        assertEquals("USD", saved.getCurrency());
        assertEquals(new BigDecimal("63.9000"), saved.getExchangeRate());
        // preços e totais ficam na moeda do documento
        assertEquals(new BigDecimal("1000.00"), dto.totalAmount());
    }

    @Test
    void fx05_encomendaSemCambioEscritoUsaOPropostoPelaTabela() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("ZAR"), eq(LocalDate.now()), isNull()))
                .thenReturn(new BigDecimal("3.4500"));

        PurchaseOrderDTO dto = orders.createOrder(orderRequest("ZAR", null));

        assertEquals("ZAR", dto.currency());
        assertEquals(new BigDecimal("3.4500"), dto.exchangeRate());
    }

    @Test
    void fx05_semMoedaOuNaMoedaBase_cambioEUm_semConsultarATabela() {
        PurchaseOrderDTO semMoeda = orders.createOrder(orderRequest(null, null));
        PurchaseOrderDTO emMzn = orders.createOrder(orderRequest("MZN", new BigDecimal("2")));

        for (PurchaseOrderDTO dto : List.of(semMoeda, emMzn)) {
            assertEquals("MZN", dto.currency());
            assertEquals(new BigDecimal("1.0000"), dto.exchangeRate());
        }
        verifyNoInteractions(exchangeRates);
    }

    @Test
    void fx05_moedaForaDaListaERecusada() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> orders.createOrder(orderRequest("GBP", BigDecimal.TEN)));

        assertThat(error.getMessage()).contains("GBP");
    }

    @Test
    void fx05_semCambioNaTabelaNemEscrito_documentoNaoEGravado() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("EUR"), any(LocalDate.class), isNull()))
                .thenThrow(new BusinessRuleException("Escreva o câmbio do documento."));

        assertThrows(BusinessRuleException.class, () -> purchases.createPurchase(purchaseRequest("EUR", null)));
        assertThrows(BusinessRuleException.class, () -> orders.createOrder(orderRequest("EUR", null)));
    }

    @Test
    void fx05_compraDirectaGuardaMoedaECambio() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("EUR"), any(LocalDate.class),
                eq(new BigDecimal("70.25")))).thenReturn(new BigDecimal("70.2500"));

        PurchaseDTO dto = purchases.createPurchaseDTO(purchaseRequest("EUR", new BigDecimal("70.25")));

        assertEquals("EUR", dto.currency());
        assertEquals(new BigDecimal("70.2500"), dto.exchangeRate());
    }

    @Test
    void fx05_compraNaMoedaBaseComportaSeComoHoje() {
        PurchaseDTO dto = purchases.createPurchaseDTO(new CreatePurchaseRequest(7L, 3L, COMPANY_ID, null,
                List.of(purchaseLine())));

        assertEquals("MZN", dto.currency());
        assertEquals(new BigDecimal("1.0000"), dto.exchangeRate());
        assertEquals(new BigDecimal("1000.00"), dto.totalAmount());
        verifyNoInteractions(exchangeRates);
    }

    @Test
    void fx05_recepcaoDaEncomendaCriaCompraNaMoedaECambioDaEncomenda() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(99L);
        order.setOrderNumber("EC-F-2026/1");
        order.setCompany(company());
        order.setSupplier(supplierRepository.findById(7L).orElseThrow());
        order.setCurrency("USD");
        order.setExchangeRate(new BigDecimal("63.9000"));
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setProduct(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID).orElseThrow());
        line.setQuantity(BigDecimal.TEN);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxRate(BigDecimal.ZERO);
        order.addLine(line);

        new PurchaseOrderPayableRegistrar(purchaseRepository, numbers, mock(ApplicationEventPublisher.class),
                mock(AuditLogService.class)).register(order, Map.of(line, BigDecimal.TEN));

        ArgumentCaptor<Purchase> saved = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(saved.capture());
        assertEquals("USD", saved.getValue().getCurrency());
        assertEquals(new BigDecimal("63.9000"), saved.getValue().getExchangeRate());
    }

    @Test
    void fx05_documentosNovosNascemNaMoedaBaseComCambioUm() {
        assertEquals("MZN", new Purchase().getCurrency());
        assertEquals(new BigDecimal("1.0000"), new Purchase().getExchangeRate());
        assertEquals("MZN", new PurchaseOrder().getCurrency());
        assertEquals(new BigDecimal("1.0000"), new PurchaseOrder().getExchangeRate());
    }

    @Test
    void fx05_migracaoPoeComprasAntigasNaMoedaBaseComCambioUm_eERepetivel() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:h2:mem:fx05;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement st = db.createStatement()) {
            st.execute("create table companies (id bigint primary key, base_currency varchar(3) default 'MZN' not null)");
            st.execute("create table purchase_orders (id bigint primary key, company_id bigint not null)");
            st.execute("create table purchases (id bigint primary key, company_id bigint not null)");
            st.execute("insert into companies (id, base_currency) values (1, 'MZN'), (2, 'USD')");
            st.execute("insert into purchase_orders (id, company_id) values (10, 1), (20, 2)");
            st.execute("insert into purchases (id, company_id) values (11, 1), (21, 2)");

            String runScript = "runscript from '" + migration("__purchase_document_currency.sql").toAbsolutePath()
                    .toString().replace("'", "''")
                    + "' charset 'UTF-8'";
            st.execute(runScript);
            st.execute(runScript);

            for (String table : List.of("purchase_orders", "purchases")) {
                try (ResultSet rs = st.executeQuery("select company_id, currency, exchange_rate from " + table
                        + " order by id")) {
                    rs.next();
                    assertEquals("MZN", rs.getString("currency"));
                    assertEquals(0, rs.getBigDecimal("exchange_rate").compareTo(BigDecimal.ONE));
                    rs.next();
                    assertEquals("USD", rs.getString("currency"), "fica na moeda base da sua empresa");
                    assertEquals(0, rs.getBigDecimal("exchange_rate").compareTo(BigDecimal.ONE));
                }
            }
            assertThat(Files.readString(migration("__purchase_document_currency.sql"))).doesNotContainIgnoringCase("exchange_rates");
        }
    }

    @Test
    void fx06_compraEmUsdGuardaTotalEmMoedaBaseArredondadoA2CasasHalfUp() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class),
                eq(new BigDecimal("63.9055")))).thenReturn(new BigDecimal("63.9055"));
        // 3 × 33,33 = 99,99 USD × 63,9055 = 6389,910945 -> 6389,91
        CreatePurchaseRequest request = new CreatePurchaseRequest(7L, 3L, COMPANY_ID, null,
                List.of(new CreatePurchaseLineRequest(11L, new BigDecimal("3"), new BigDecimal("33.33"), null, null,
                        null, BigDecimal.ZERO)), "USD", new BigDecimal("63.9055"));

        PurchaseDTO dto = purchases.createPurchaseDTO(request);

        assertEquals(new BigDecimal("99.99"), dto.totalAmount(), "o total fica na moeda do documento");
        assertEquals(new BigDecimal("6389.91"), dto.baseTotalAmount());
        assertEquals(new BigDecimal("6389.91"), savedPurchase().getBaseTotalAmount());
    }

    @Test
    void fx06_compraEmUsdEntraNoStockENoCustoMedioAoPrecoVezesCambio() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class),
                eq(new BigDecimal("63.9")))).thenReturn(new BigDecimal("63.9000"));

        purchases.createPurchase(purchaseRequest("USD", new BigDecimal("63.9")));

        ArgumentCaptor<BigDecimal> unitCost = ArgumentCaptor.forClass(BigDecimal.class);
        verify(inventory).registerPurchase(any(Product.class), any(Warehouse.class), eq(BigDecimal.TEN),
                unitCost.capture(), isNull(), isNull(), anyString(), isNull());
        assertEquals(0, unitCost.getValue().compareTo(new BigDecimal("6390")), "100 USD × 63,90 = 6390 MT");
    }

    @Test
    void fx06_custoMedioPonderadoUsaOCustoConvertido() {
        Product product = new Product();
        product.setId(11L);
        product.setPurchasePrice(new BigDecimal("6000.00"));
        Warehouse warehouse = warehouseRepository.findById(3L).orElseThrow();
        Stock stock = new Stock();
        stock.setWarehouse(warehouse);
        stock.setQuantity(BigDecimal.TEN);
        StockRepository stocks = mock(StockRepository.class);
        when(stocks.findByProductId(11L)).thenReturn(List.of(stock));
        Purchase usd = new Purchase();
        usd.applyDocumentCurrency(new DocumentCurrency("USD", new BigDecimal("63.9000")));

        WeightedAverageCost.apply(stocks, product, warehouse,
                BigDecimal.TEN, usd.toBaseUnitCost(new BigDecimal("100")));

        // (10 × 6000 + 10 × 6390) / 20 = 6195,00 MT
        assertEquals(new BigDecimal("6195.00"), product.getPurchasePrice());
    }

    @Test
    void fx06_encomendaEmZarGuardaTotalEmMoedaBase() {
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("ZAR"), any(LocalDate.class),
                eq(new BigDecimal("3.45")))).thenReturn(new BigDecimal("3.4500"));

        PurchaseOrderDTO dto = orders.createOrder(orderRequest("ZAR", new BigDecimal("3.45")));

        assertEquals(new BigDecimal("1000.00"), dto.totalAmount());
        assertEquals(new BigDecimal("3450.00"), dto.baseTotalAmount());
        assertEquals(new BigDecimal("3450.00"), savedOrder().getBaseTotalAmount());
    }

    @Test
    void fx06_recepcaoDaEncomendaEmZarDaEntradaNoStockAoCustoConvertido() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("EC-F-2026/3");
        order.setStatus(PurchaseOrder.ORDERED);
        order.setCompany(company());
        order.setSupplier(supplierRepository.findById(7L).orElseThrow());
        order.setWarehouse(warehouseRepository.findById(3L).orElseThrow());
        order.applyDocumentCurrency(new DocumentCurrency("ZAR", new BigDecimal("3.4500")));
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setProduct(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID).orElseThrow());
        line.setQuantity(BigDecimal.TEN);
        line.setReceivedQuantity(BigDecimal.ZERO);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxRate(BigDecimal.ZERO);
        order.addLine(line);

        new PurchaseOrderReceiptExecutor(orderRepository, inventory, mock(AuditLogService.class),
                mock(GoodsReceiptDiscrepancyRepository.class), mock(PurchaseOrderReceiptRepository.class),
                mock(PurchaseOrderPayableRegistrar.class)).receiveOrder(order);

        verify(inventory).registerPurchase(any(Product.class), any(Warehouse.class), eq(BigDecimal.TEN),
                argThat(cost -> cost.compareTo(new BigDecimal("345")) == 0), isNull(), isNull(), anyString(),
                isNull());
    }

    @Test
    void fx06_compraCriadaPelaRecepcaoGuardaTotalEmMoedaBase() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("EC-F-2026/2");
        order.setCompany(company());
        order.setSupplier(supplierRepository.findById(7L).orElseThrow());
        order.applyDocumentCurrency(new DocumentCurrency("EUR", new BigDecimal("70.2500")));
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setProduct(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID).orElseThrow());
        line.setQuantity(BigDecimal.TEN);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxRate(BigDecimal.ZERO);
        order.addLine(line);

        new PurchaseOrderPayableRegistrar(purchaseRepository, numbers, mock(ApplicationEventPublisher.class),
                mock(AuditLogService.class)).register(order, Map.of(line, new BigDecimal("4")));

        Purchase saved = savedPurchase();
        assertEquals(new BigDecimal("400.00"), saved.getTotalAmount());
        assertEquals(new BigDecimal("28100.00"), saved.getBaseTotalAmount());
    }

    @Test
    void fx06_naMoedaBaseOTotalEmMoedaBaseEIgualAoTotal_eOCustoEOPreco() {
        PurchaseDTO dto = purchases.createPurchaseDTO(new CreatePurchaseRequest(7L, 3L, COMPANY_ID, null,
                List.of(purchaseLine())));

        assertEquals(dto.totalAmount(), dto.baseTotalAmount());
        verify(inventory).registerPurchase(any(Product.class), any(Warehouse.class), eq(BigDecimal.TEN),
                argThat(cost -> cost.compareTo(new BigDecimal("100")) == 0), isNull(), isNull(), anyString(),
                isNull());
    }

    @Test
    void fx06_migracaoPreencheTotalEmMoedaBaseDosDocumentosExistentes_eERepetivel() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:h2:mem:fx06;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement st = db.createStatement()) {
            for (String table : List.of("purchase_orders", "purchases")) {
                st.execute("create table " + table + " (id bigint primary key, total_amount numeric(12,2) not null,"
                        + " exchange_rate numeric(18,4) default 1 not null)");
                st.execute("insert into " + table + " (id, total_amount, exchange_rate) values"
                        + " (1, 1500.00, 1), (2, 99.99, 63.9055)");
            }

            String runScript = "runscript from '" + migration("__purchase_base_total.sql").toAbsolutePath()
                    .toString().replace("'", "''") + "' charset 'UTF-8'";
            st.execute(runScript);
            st.execute(runScript);

            for (String table : List.of("purchase_orders", "purchases")) {
                try (ResultSet rs = st.executeQuery("select base_total_amount from " + table + " order by id")) {
                    rs.next();
                    assertEquals(new BigDecimal("1500.00"), rs.getBigDecimal(1));
                    rs.next();
                    assertEquals(new BigDecimal("6389.91"), rs.getBigDecimal(1));
                }
            }
        }
    }

    @Test
    void fx08_contasAPagarMostramMoedaDividaNaMoedaEEquivalenteEmMoedaBase() {
        Purchase usd = foreignPurchase(1L, "CP-2026/1", "USD", "63.9000", "1000.00", "400.00");
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(usd));

        List<PayableDTO> payables = new SupplierPaymentService(purchaseRepository, mock(FinanceService.class),
                mock(ApplicationEventPublisher.class), mock(AuditLogService.class), purchases)
                .findPayablesByCompany(COMPANY_ID);

        assertThat(payables).hasSize(1);
        PayableDTO payable = payables.get(0);
        assertEquals("USD", payable.currency());
        assertEquals(new BigDecimal("63.9000"), payable.exchangeRate());
        assertEquals(new BigDecimal("600.00"), payable.outstanding(), "dívida na moeda do documento");
        assertEquals(new BigDecimal("63900.00"), payable.baseTotalAmount());
        assertEquals(new BigDecimal("38340.00"), payable.baseOutstanding(), "600 USD × 63,90 em moeda base");
    }

    @Test
    void fx08_dividaEmMoedaBaseBaixaAoCambioDoDocumento_eLiquidadaNaoDeixaResiduos() {
        // 99,99 USD × 63,9055 = 6389,91 MT; três pagamentos de 33,33 USD
        Purchase partial = foreignPurchase(1L, "CP-2026/2", "USD", "63.9055", "99.99", "33.33");
        assertEquals(new BigDecimal("2129.97"), partial.getBaseAmountPaid());
        assertEquals(new BigDecimal("4259.94"), partial.getBaseOutstanding());

        Purchase settled = foreignPurchase(2L, "CP-2026/3", "USD", "63.9055", "99.99", "99.99");
        assertEquals(new BigDecimal("6389.91"), settled.getBaseAmountPaid(), "baixa exactamente o saldo em MT");
        assertEquals(0, settled.getBaseOutstanding().signum());
    }

    @Test
    void fx08_extractoDoFornecedorTemSaldoEmMoedaBaseEDividaPorMoeda() {
        Purchase mzn = foreignPurchase(1L, "CP-2026/4", "MZN", "1.0000", "500.00", "0.00");
        Purchase usd = foreignPurchase(2L, "CP-2026/5", "USD", "63.9000", "1000.00", "400.00");
        LocalDate today = LocalDate.now();

        SupplierStatementDTO statement = new SupplierStatementMovementBuilder().buildStatement(
                usd.getSupplier(), today.minusDays(1), today, List.of(mzn, usd));

        SupplierStatementLineDTO usdInvoice = statement.lines().stream()
                .filter(l -> "V/FT".equals(l.documentType()) && "CP-2026/5".equals(l.documentNumber()))
                .findFirst().orElseThrow();
        assertEquals("USD", usdInvoice.currency());
        assertEquals(new BigDecimal("63.9000"), usdInvoice.exchangeRate());
        assertEquals(new BigDecimal("1000.00"), usdInvoice.documentAmount());
        assertEquals(new BigDecimal("63900.00"), usdInvoice.credit(), "o extracto soma em moeda base");
        SupplierStatementLineDTO usdPayment = statement.lines().stream()
                .filter(l -> "PG".equals(l.documentType())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("400.00"), usdPayment.documentAmount());
        assertEquals(new BigDecimal("25560.00"), usdPayment.debit());
        assertEquals(new BigDecimal("38840.00"), statement.closingBalance());

        assertThat(statement.outstandingByCurrency()).containsExactly(
                new CurrencyBalanceDTO("MZN", new BigDecimal("500.00"), new BigDecimal("500.00")),
                new CurrencyBalanceDTO("USD", new BigDecimal("600.00"), new BigDecimal("38340.00")));
    }

    private Purchase foreignPurchase(Long id, String number, String currency, String rate, String total, String paid) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setPurchaseNumber(number);
        purchase.setCompany(company());
        purchase.setSupplier(supplierRepository.findById(7L).orElseThrow());
        purchase.applyDocumentCurrency(new DocumentCurrency(currency, new BigDecimal(rate)));
        purchase.applyTotal(new BigDecimal(total));
        purchase.setAmountPaid(new BigDecimal(paid));
        return purchase;
    }

    private Purchase savedPurchase() {
        ArgumentCaptor<Purchase> saved = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(saved.capture());
        return saved.getValue();
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        return company;
    }

    private PurchaseOrder savedOrder() {
        ArgumentCaptor<PurchaseOrder> saved = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(orderRepository).save(saved.capture());
        return saved.getValue();
    }

    private static CreatePurchaseOrderRequest orderRequest(String currency, BigDecimal rate) {
        return new CreatePurchaseOrderRequest(7L, 3L, COMPANY_ID, null, null,
                List.of(new CreatePurchaseOrderLineRequest(11L, BigDecimal.TEN, new BigDecimal("100"),
                        null, null, null, BigDecimal.ZERO)), currency, rate);
    }

    private static CreatePurchaseRequest purchaseRequest(String currency, BigDecimal rate) {
        return new CreatePurchaseRequest(7L, 3L, COMPANY_ID, null, List.of(purchaseLine()), currency, rate);
    }

    private static CreatePurchaseLineRequest purchaseLine() {
        return new CreatePurchaseLineRequest(11L, BigDecimal.TEN, new BigDecimal("100"), null, null, null,
                BigDecimal.ZERO);
    }

    private static Path migration(String suffix) throws IOException {
        Path dir = Path.of("src/main/resources/db/migration");
        if (!Files.isDirectory(dir)) dir = Path.of("backend").resolve(dir);
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> matches = files.filter(p -> p.getFileName().toString().endsWith(suffix)).toList();
            assertThat(matches).as("uma migração Flyway terminada em " + suffix).hasSize(1);
            return matches.get(0);
        }
    }
}
