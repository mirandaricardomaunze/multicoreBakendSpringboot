package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.LandedCostConfirmedEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.accounting.service.JournalService;
import mz.multicore.erp.modules.accounting.service.LandedCostPostingService;
import mz.multicore.erp.modules.accounting.service.PgcNirfChart;
import mz.multicore.erp.modules.accounting.service.PostingHelper;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.service.AverageCostUpdater;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeRequest;
import mz.multicore.erp.modules.purchases.model.LandedCostDocument;
import mz.multicore.erp.modules.purchases.model.LandedCostType;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.LandedCostDocumentRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LC-07 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4): confirming a CI posts, in base currency, a balanced
 * entry — debit 3201 for the part that went to stock, 6101 for the part already sold and 2432 for customs VAT and the
 * VAT of the services; credit 2201 for the debt of each cost line to its supplier.
 */
class LandedCostPostingTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long CARRIER_ID = 30L;
    private static final Long BROKER_ID = 31L;
    private static final Long CUSTOMS_ID = 32L;
    private static final Long VAT16_ID = 5L;

    private final LandedCostDocumentRepository documents = mock(LandedCostDocumentRepository.class);
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final SupplierLookup suppliers = mock(SupplierLookup.class);
    private final TaxRateLookup taxRates = mock(TaxRateLookup.class);
    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
    private final StockRepository stockRepository = mock(StockRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final LandedCostDocumentService service = new LandedCostDocumentService(documents, purchases, numbers,
            new LandedCostChargeResolver(suppliers, new PurchaseCurrencyResolver(exchangeRates), taxRates),
            new LandedCostStockEffect(new StockLookup(stockRepository),
                    new AverageCostUpdater(stockRepository, mock(ProductCommandService.class))),
            events);

    private final PostingHelper postingHelper = mock(PostingHelper.class);
    private final JournalService journal = mock(JournalService.class);
    private final LandedCostPostingService posting = new LandedCostPostingService(postingHelper, journal);

    private final Company company = new Company();
    private final Warehouse warehouse = new Warehouse();
    private final List<Stock> stocks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("comprador", "MANAGER");
        company.setId(COMPANY_ID);
        warehouse.setId(70L);
        warehouse.setCompany(company);
        when(documents.save(any(LandedCostDocument.class))).thenAnswer(invocation -> {
            LandedCostDocument document = invocation.getArgument(0);
            document.setId(50L);
            return document;
        });
        when(purchases.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1");
        stubSupplier(CARRIER_ID, "Transportes Maputo");
        stubSupplier(BROKER_ID, "Despachante Lda");
        stubSupplier(CUSTOMS_ID, "Autoridade Tributária");
        TaxRate vat16 = new TaxRate();
        vat16.setId(VAT16_ID);
        vat16.setRate(new BigDecimal("0.1600"));
        when(taxRates.requireForCompany(VAT16_ID, COMPANY_ID)).thenReturn(vat16);
        when(exchangeRates.resolveDocumentRate(eq(COMPANY_ID), eq("USD"), any(LocalDate.class), any()))
                .thenAnswer(call -> call.getArgument(3));
        when(stockRepository.findByProductIdAndWarehouseId(any(), any())).thenAnswer(call -> stocks.stream()
                .filter(s -> s.getProduct().getId().equals(call.getArgument(0))).findFirst());
        when(stockRepository.findByProductId(any())).thenAnswer(call -> stocks.stream()
                .filter(s -> s.getProduct().getId().equals(call.getArgument(0))).toList());
        when(postingHelper.canPost(COMPANY_ID)).thenReturn(true);
        when(postingHelper.account(eq(COMPANY_ID), anyString())).thenAnswer(call -> account(call.getArgument(1)));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void lc07_stockCmvEIvaADebitoEFornecedoresACreditoEquilibrado() {
        // 10 recebidos, 4 ainda em stock: frete 1000 + IVA 16% (1160) e IVA aduaneiro 800
        receivedPurchase("4");

        JournalEntry entry = confirmAndPost(
                new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null,
                        new BigDecimal("1000.00"), VAT16_ID),
                new LandedCostChargeRequest(LandedCostType.IVA_ADUANEIRO, CUSTOMS_ID, null, null,
                        new BigDecimal("800.00"), null));

        assertEquals(JournalSource.LANDED_COST, entry.getSource());
        assertEquals("CI-2026/1", entry.getSourceDocumentNumber());
        assertMoney("400.00", debit(entry, PgcNirfChart.MERCADORIAS));
        assertMoney("600.00", debit(entry, PgcNirfChart.CMVMC));
        assertMoney("960.00", debit(entry, PgcNirfChart.IVA_DEDUTIVEL));
        assertMoney("1960.00", credit(entry, PgcNirfChart.FORNECEDORES));
        assertBalanced(entry);
    }

    @Test
    void lc07_cadaLinhaDeCustoCreditaFornecedoresPelaSuaDivida() {
        receivedPurchase("4");

        JournalEntry entry = confirmAndPost(
                new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null,
                        new BigDecimal("1000.00"), VAT16_ID),
                new LandedCostChargeRequest(LandedCostType.IVA_ADUANEIRO, CUSTOMS_ID, null, null,
                        new BigDecimal("800.00"), null));

        List<JournalLine> credits = entry.getLines().stream().filter(l -> signum(l.getCredit()) > 0).toList();
        assertEquals(2, credits.size(), "uma linha de 2201 por dívida");
        assertMoney("1160.00", credits.get(0).getCredit());
        assertTrue(credits.get(0).getDescription().contains("CI-2026/1-1"));
        assertMoney("800.00", credits.get(1).getCredit());
        assertTrue(credits.get(1).getDescription().contains("Autoridade Tributária"));
    }

    @Test
    void lc07_stockSuficienteNaoLancaCmv() {
        receivedPurchase("10");

        JournalEntry entry = confirmAndPost(new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null,
                new BigDecimal("500.00"), null));

        assertMoney("500.00", debit(entry, PgcNirfChart.MERCADORIAS));
        assertEquals(0, entry.getLines().stream()
                .filter(l -> PgcNirfChart.CMVMC.equals(l.getAccount().getCode())).count());
        assertEquals(0, entry.getLines().stream()
                .filter(l -> PgcNirfChart.IVA_DEDUTIVEL.equals(l.getAccount().getCode())).count());
        assertBalanced(entry);
    }

    @Test
    void lc07_moedaEstrangeiraCreditaADividaEmMoedaBaseEFechaComOIva() {
        // 123,45 USD + IVA 19,75 = 143,20 USD × 63,9055: dívida 9151,27; custo 7889,13; IVA = diferença 1262,14
        receivedPurchase("10");

        JournalEntry entry = confirmAndPost(new LandedCostChargeRequest(LandedCostType.DESPACHANTE, BROKER_ID, "USD",
                new BigDecimal("63.9055"), new BigDecimal("123.45"), VAT16_ID));

        assertMoney("9151.27", credit(entry, PgcNirfChart.FORNECEDORES));
        assertMoney("7889.13", debit(entry, PgcNirfChart.MERCADORIAS));
        assertMoney("1262.14", debit(entry, PgcNirfChart.IVA_DEDUTIVEL));
        assertBalanced(entry);
    }

    @Test
    void lc07_naoLancaDuasVezesNemSemPlanoDeContas() {
        LandedCostConfirmedEvent event = new LandedCostConfirmedEvent(COMPANY_ID, 50L, "CI-2026/1", LocalDate.now(),
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(new LandedCostConfirmedEvent.Payable("CI-2026/1-1", "Transportes", new BigDecimal("100.00"))));
        when(postingHelper.alreadyPosted(COMPANY_ID, JournalSource.LANDED_COST, 50L)).thenReturn(true);
        posting.onLandedCostConfirmed(event);

        when(postingHelper.alreadyPosted(COMPANY_ID, JournalSource.LANDED_COST, 50L)).thenReturn(false);
        when(postingHelper.canPost(COMPANY_ID)).thenReturn(false);
        posting.onLandedCostConfirmed(event);

        verify(journal, never()).save(any(), any());
    }

    private JournalEntry confirmAndPost(LandedCostChargeRequest... charges) {
        service.register(new CreateLandedCostRequest(List.of(10L), null, null, List.of(charges)));
        ArgumentCaptor<LandedCostConfirmedEvent> event = ArgumentCaptor.forClass(LandedCostConfirmedEvent.class);
        verify(events).publishEvent(event.capture());
        posting.onLandedCostConfirmed(event.getValue());
        ArgumentCaptor<JournalEntry> entry = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journal).save(entry.capture(), eq(COMPANY_ID));
        return entry.getValue();
    }

    private void receivedPurchase(String stockQuantity) {
        Product rice = new Product();
        rice.setId(1L);
        rice.setName("Arroz 25kg");
        rice.setPurchasePrice(new BigDecimal("100.00"));
        Stock stock = new Stock();
        stock.setProduct(rice);
        stock.setWarehouse(warehouse);
        stock.setQuantity(new BigDecimal(stockQuantity));
        stocks.add(stock);
        PurchaseLine line = new PurchaseLine();
        line.setProduct(rice);
        line.setQuantity(BigDecimal.TEN);
        line.setUnitPrice(new BigDecimal("100.00"));
        Purchase purchase = new Purchase();
        purchase.setId(10L);
        purchase.setCompany(company);
        purchase.setWarehouse(warehouse);
        purchase.setPurchaseNumber("V/FT-2026/10");
        purchase.setStatus("COMPLETED");
        purchase.addLine(line);
        when(purchases.findById(10L)).thenReturn(Optional.of(purchase));
    }

    private static BigDecimal debit(JournalEntry entry, String code) {
        return entry.getLines().stream().filter(l -> code.equals(l.getAccount().getCode()))
                .map(JournalLine::getDebit).map(LandedCostPostingTest::orZero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal credit(JournalEntry entry, String code) {
        return entry.getLines().stream().filter(l -> code.equals(l.getAccount().getCode()))
                .map(JournalLine::getCredit).map(LandedCostPostingTest::orZero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void assertBalanced(JournalEntry entry) {
        BigDecimal debits = entry.getLines().stream().map(JournalLine::getDebit).map(LandedCostPostingTest::orZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = entry.getLines().stream().map(JournalLine::getCredit).map(LandedCostPostingTest::orZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, debits.compareTo(credits), () -> "débitos " + debits + " ≠ créditos " + credits);
    }

    private static int signum(BigDecimal value) {
        return orZero(value).signum();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "esperado " + expected + " mas foi " + actual);
    }

    private static Account account(String code) {
        Account account = new Account();
        account.setCode(code);
        return account;
    }

    private void stubSupplier(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        when(suppliers.requireForCompany(id, COMPANY_ID)).thenReturn(supplier);
    }
}
