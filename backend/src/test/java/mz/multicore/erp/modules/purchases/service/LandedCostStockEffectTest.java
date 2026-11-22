package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.service.AverageCostUpdater;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostAllocationDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostDocumentDTO;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LC-06 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4): for each purchase line with received quantity
 * {@code Q} and share {@code C}, the part for the stock is {@code C × min(1, S / Q)} with {@code S} the stock of the
 * product in that warehouse; it raises the average cost. The rest is the part already sold, which goes to cost of
 * goods sold. Stock quantities and past sales are left untouched.
 */
class LandedCostStockEffectTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long CARRIER_ID = 30L;
    private static final Long CUSTOMS_ID = 32L;

    private final LandedCostDocumentRepository documents = mock(LandedCostDocumentRepository.class);
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final DocumentNumberService numbers = mock(DocumentNumberService.class);
    private final SupplierLookup suppliers = mock(SupplierLookup.class);
    private final StockRepository stockRepository = mock(StockRepository.class);
    private final ProductCommandService productCommands = mock(ProductCommandService.class);
    private final LandedCostDocumentService service = new LandedCostDocumentService(documents, purchases, numbers,
            new LandedCostChargeResolver(suppliers, new PurchaseCurrencyResolver(mock(ExchangeRateService.class)),
                    mock(TaxRateLookup.class)),
            new LandedCostStockEffect(new StockLookup(stockRepository),
                    new AverageCostUpdater(stockRepository, productCommands)),
            mock(org.springframework.context.ApplicationEventPublisher.class));

    private final Company company = new Company();
    private final Warehouse warehouse = warehouse(70L);
    private final List<Stock> stocks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("comprador", "MANAGER");
        company.setId(COMPANY_ID);
        when(documents.save(any(LandedCostDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchases.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(numbers.next(DocumentSeries.LANDED_COST)).thenReturn("CI-2026/1");
        stubSupplier(CARRIER_ID, "Transportes Maputo");
        stubSupplier(CUSTOMS_ID, "Autoridade Tributária");
        when(stockRepository.findByProductIdAndWarehouseId(any(), any())).thenAnswer(call -> stocks.stream()
                .filter(s -> s.getProduct().getId().equals(call.getArgument(0))
                        && s.getWarehouse().getId().equals(call.getArgument(1)))
                .findFirst());
        when(stockRepository.findByProductId(any())).thenAnswer(call -> stocks.stream()
                .filter(s -> s.getProduct().getId().equals(call.getArgument(0))).toList());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void lc06_stockSuficienteVaiTodoAoStockESobeOCustoMedio() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        stock(rice, warehouse, "10");
        purchase(10L, line(rice, "10", "100.00"));

        LandedCostAllocationDTO line = single(register(freight("500.00")));

        assertMoney("500.00", line.shareAmount());
        assertMoney("500.00", line.stockAmount());
        assertMoney("0.00", line.soldAmount());
        assertMoney("100.00", line.unitCostBefore());
        assertMoney("150.00", line.unitCostAfter());
        assertMoney("150.00", rice.getPurchasePrice());
        verify(productCommands).saveProduct(rice);
    }

    @Test
    void lc06_parteJaVendidaVaiACmvEAParteDoStockSobeOMedio() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        stock(rice, warehouse, "4");
        purchase(10L, line(rice, "10", "100.00"));

        LandedCostAllocationDTO line = single(register(freight("1000.00")));

        assertEquals(0, new BigDecimal("10").compareTo(line.receivedQuantity()));
        assertEquals(0, new BigDecimal("4").compareTo(line.stockQuantity()));
        assertMoney("400.00", line.stockAmount());
        assertMoney("600.00", line.soldAmount());
        // novo médio = (S × médio + parte) / S = (4 × 100 + 400) / 4
        assertMoney("200.00", rice.getPurchasePrice());
    }

    @Test
    void lc06_semStockAQuotaTodaVaiACmvEOMedioNaoMuda() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        purchase(10L, line(rice, "10", "100.00"));

        LandedCostAllocationDTO line = single(register(freight("1000.00")));

        assertMoney("0.00", line.stockAmount());
        assertMoney("1000.00", line.soldAmount());
        assertMoney("100.00", line.unitCostAfter());
        assertMoney("100.00", rice.getPurchasePrice());
        verify(productCommands, never()).saveProduct(any());
    }

    @Test
    void lc06_variasLinhasRepartemPorValorEAsPartesSomamOCusto() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        Product oil = product(2L, "Óleo 20L", "600.00");
        stock(rice, warehouse, "10");
        stock(oil, warehouse, "2");
        purchase(10L, line(rice, "10", "100.00"), line(oil, "5", "600.00"));

        LandedCostDocumentDTO dto = register(freight("1000.00"));

        LandedCostAllocationDTO riceLine = dto.allocations().get(0);
        LandedCostAllocationDTO oilLine = dto.allocations().get(1);
        assertMoney("250.00", riceLine.shareAmount());
        assertMoney("750.00", oilLine.shareAmount());
        assertMoney("300.00", oilLine.stockAmount());
        assertMoney("450.00", oilLine.soldAmount());
        assertMoney("125.00", rice.getPurchasePrice());
        assertMoney("750.00", oil.getPurchasePrice());
        BigDecimal total = dto.allocations().stream()
                .map(a -> a.stockAmount().add(a.soldAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney("1000.00", total);
    }

    @Test
    void lc06_ivaAduaneiroNaoEntraNoCustoDoStock() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        stock(rice, warehouse, "10");
        purchase(10L, line(rice, "10", "100.00"));

        LandedCostAllocationDTO line = single(service.register(new CreateLandedCostRequest(List.of(10L), null, null,
                List.of(new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null,
                                new BigDecimal("500.00"), null),
                        new LandedCostChargeRequest(LandedCostType.IVA_ADUANEIRO, CUSTOMS_ID, null, null,
                                new BigDecimal("800.00"), null)))));

        assertMoney("500.00", line.shareAmount());
        assertMoney("150.00", rice.getPurchasePrice());
    }

    @Test
    void lc06_oMedioDaEmpresaContaOStockDeTodosOsArmazens() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        stock(rice, warehouse, "10");
        stock(rice, warehouse(71L), "10");
        purchase(10L, line(rice, "10", "100.00"));

        LandedCostAllocationDTO line = single(register(freight("1000.00")));

        assertMoney("1000.00", line.stockAmount());
        // custo médio é por artigo na empresa: (20 × 100 + 1000) / 20
        assertMoney("150.00", rice.getPurchasePrice());
    }

    @Test
    void lc06_naoMexeEmQuantidadesNemEmVendasPassadas() {
        Product rice = product(1L, "Arroz 25kg", "100.00");
        Stock onHand = stock(rice, warehouse, "4");
        purchase(10L, line(rice, "10", "100.00"));

        register(freight("1000.00"));

        assertEquals(0, new BigDecimal("4").compareTo(onHand.getQuantity()));
        verify(stockRepository, never()).save(any());
    }

    private LandedCostDocumentDTO register(LandedCostChargeRequest charge) {
        return service.register(new CreateLandedCostRequest(List.of(10L), null, null, List.of(charge)));
    }

    private static LandedCostChargeRequest freight(String amount) {
        return new LandedCostChargeRequest(LandedCostType.FRETE, CARRIER_ID, null, null, new BigDecimal(amount), null);
    }

    private static LandedCostAllocationDTO single(LandedCostDocumentDTO dto) {
        assertEquals(1, dto.allocations().size());
        return dto.allocations().get(0);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "esperado " + expected + " mas foi " + actual);
    }

    private Warehouse warehouse(Long id) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setCompany(company);
        return w;
    }

    private static Product product(Long id, String name, String cost) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPurchasePrice(new BigDecimal(cost));
        return product;
    }

    private Stock stock(Product product, Warehouse at, String quantity) {
        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse(at);
        stock.setQuantity(new BigDecimal(quantity));
        stocks.add(stock);
        return stock;
    }

    private static PurchaseLine line(Product product, String quantity, String unitPrice) {
        PurchaseLine line = new PurchaseLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal(unitPrice));
        return line;
    }

    private void purchase(Long id, PurchaseLine... lines) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setCompany(company);
        purchase.setWarehouse(warehouse);
        purchase.setPurchaseNumber("V/FT-2026/" + id);
        for (PurchaseLine line : lines) {
            purchase.addLine(line);
        }
        when(purchases.findById(id)).thenReturn(Optional.of(purchase));
    }

    private void stubSupplier(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        when(suppliers.requireForCompany(id, COMPANY_ID)).thenReturn(supplier);
    }
}
