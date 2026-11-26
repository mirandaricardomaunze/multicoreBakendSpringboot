package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.publicapi.dto.PublicPage;
import mz.multicore.erp.modules.publicapi.dto.PublicProductDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicStockDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Harness API-07 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3, §3.3.1): the public catalogue shows only the key company's
 * products and the stock available in its sales warehouses (quantity − active reservations); costs, margins and
 * suppliers never leave; {@code updatedSince} filters by the last change; lists are paginated (max 100).
 */
class PublicApiCatalogTest {

    private static final long COMPANY = 7L;

    private final ProductLookup products = mock(ProductLookup.class);
    private final WarehouseLookup warehouses = mock(WarehouseLookup.class);
    private final StockLookup stocks = mock(StockLookup.class);
    private final OrderReservationLookup reservations = mock(OrderReservationLookup.class);
    private final PublicCatalogService service =
            new PublicCatalogService(products, warehouses, stocks, reservations);

    private Product arroz;
    private Product oleo;
    private Product servico;
    private Warehouse loja;
    private Warehouse transito;
    private Warehouse fechado;

    @BeforeEach
    void setUp() {
        ProductCategory mercearia = new ProductCategory();
        mercearia.setName("Mercearia");
        TaxRate iva16 = new TaxRate();
        iva16.setRate(new BigDecimal("0.1600"));

        arroz = product(1L, "ARZ-25", "Arroz 25 kg", "1000.00");
        arroz.setCategory(mercearia);
        arroz.setTaxRate(iva16);
        arroz.setBarcode("6001234567890");
        arroz.setPurchasePrice(new BigDecimal("700.00"));
        arroz.setWholesalePrice(new BigDecimal("900.00"));
        arroz.setUpdatedAt(LocalDateTime.of(2026, 11, 20, 10, 0));
        oleo = product(2L, "OLE-1", "Óleo 1 L", "150.00");
        oleo.setCreatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        servico = product(3L, "SRV-ENT", "Entrega", "200.00");
        servico.setStockTracked(false);
        servico.setCreatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        Product adiantamento = product(4L, AdvancePayments.systemSku(COMPANY), "Adiantamento", "0.00");
        when(products.listForCompany(COMPANY)).thenReturn(List.of(servico, oleo, arroz, adiantamento));

        loja = warehouse(10L, "A01", "Loja Baixa", WarehouseType.STORE, true, true);
        transito = warehouse(11L, null, "Trânsito", WarehouseType.TRANSIT, true, true);
        fechado = warehouse(12L, "A03", "Antigo", WarehouseType.DEPOT, false, true);
        Warehouse deposito = warehouse(13L, "A02", "Depósito Matola", WarehouseType.DEPOT, true, true);
        when(warehouses.listForCompany(COMPANY)).thenReturn(List.of(loja, transito, fechado, deposito));
        when(stocks.listForCompany(COMPANY)).thenReturn(List.of(
                stock(arroz, loja, "40"), stock(arroz, deposito, "100"), stock(oleo, loja, "5"),
                stock(arroz, transito, "9"), stock(arroz, fechado, "3"), stock(servico, loja, "1")));
        when(reservations.activeReservationsForCompany(COMPANY)).thenReturn(List.of(
                new ReservedStock(1L, 10L, new BigDecimal("15")), new ReservedStock(2L, 10L, new BigDecimal("8"))));
    }

    @Test
    void api07ProductsShowSaleDataWithVatAndOnlyTheCompanyCatalogue() {
        PublicPage<PublicProductDTO> page = service.listProducts(COMPANY, null, null, null);

        assertThat(page.items()).extracting(PublicProductDTO::sku).containsExactly("ARZ-25", "OLE-1", "SRV-ENT");
        PublicProductDTO first = page.items().get(0);
        assertThat(first.name()).isEqualTo("Arroz 25 kg");
        assertThat(first.category()).isEqualTo("Mercearia");
        assertThat(first.barcode()).isEqualTo("6001234567890");
        assertThat(first.unit()).isEqualTo("UN");
        assertThat(first.vatRatePercent()).isEqualByComparingTo("16");
        assertThat(first.priceWithVat()).isEqualByComparingTo("1160.00");
        assertThat(first.priceWithVat().scale()).isEqualTo(2);
        assertThat(first.updatedAt()).isNotNull();
        assertThat(page.totalItems()).isEqualTo(3);
    }

    @Test
    void api07CostsMarginsAndSuppliersNeverLeaveTheApi() {
        for (Class<?> type : List.of(PublicProductDTO.class, PublicStockDTO.class)) {
            List<String> fields = Arrays.stream(type.getRecordComponents())
                    .map(RecordComponent::getName).map(n -> n.toLowerCase(Locale.ROOT)).toList();
            assertThat(fields).noneMatch(n -> n.contains("cost") || n.contains("purchase") || n.contains("margin")
                    || n.contains("supplier") || n.contains("wholesale") || n.contains("businessunit")
                    || n.equals("id") || n.endsWith("id"));
        }
    }

    @Test
    void api07UnknownSkuOrOtherCompanySkuIsNotFound() {
        when(products.findBySkuForCompany("ARZ-25", COMPANY)).thenReturn(java.util.Optional.of(arroz));
        when(products.findBySkuForCompany("XPTO", COMPANY)).thenReturn(java.util.Optional.empty());

        assertThat(service.getProduct(COMPANY, "ARZ-25").priceWithVat()).isEqualByComparingTo("1160.00");
        assertThatThrownBy(() -> service.getProduct(COMPANY, "XPTO"))
                .isInstanceOfSatisfying(PublicApiNotFoundException.class,
                        e -> assertThat(e.code()).isEqualTo("PRODUCT_NOT_FOUND"));
        when(products.findBySkuForCompany(AdvancePayments.systemSku(COMPANY), COMPANY))
                .thenReturn(java.util.Optional.of(product(4L, AdvancePayments.systemSku(COMPANY), "Adiantamento", "0")));
        assertThatThrownBy(() -> service.getProduct(COMPANY, AdvancePayments.systemSku(COMPANY)))
                .isInstanceOf(PublicApiNotFoundException.class);
    }

    @Test
    void api07UpdatedSinceKeepsOnlyProductsChangedFromThatInstant() {
        assertThat(service.listProducts(COMPANY, "2026-11-01T00:00:00", null, null).items())
                .extracting(PublicProductDTO::sku).containsExactly("ARZ-25");
        assertThat(service.listProducts(COMPANY, "2026-10-01", null, null).items()).hasSize(3);
        assertThat(service.listProducts(COMPANY, "2026-11-20T10:00:00+02:00", null, null).items())
                .extracting(PublicProductDTO::sku).containsExactlyInAnyOrder("ARZ-25");
        assertThatThrownBy(() -> service.listProducts(COMPANY, "ontem", null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void api07StockIsAvailableQuantityPerSalesWarehouseNeverNegative() {
        PublicPage<PublicStockDTO> page = service.listStock(COMPANY, null, null, null);

        assertThat(page.items()).extracting(s -> s.sku() + "@" + s.warehouseName() + "=" + s.available().stripTrailingZeros().toPlainString())
                .containsExactly("ARZ-25@Depósito Matola=100", "ARZ-25@Loja Baixa=25", "OLE-1@Loja Baixa=0");
        assertThat(page.items().get(1).warehouseCode()).isEqualTo("A01");
    }

    @Test
    void api07StockFiltersByWarehouseCodeOrNameAndRefusesUnknownWarehouse() {
        assertThat(service.listStock(COMPANY, "a02", null, null).items())
                .extracting(PublicStockDTO::warehouseName).containsOnly("Depósito Matola");
        assertThat(service.listStock(COMPANY, "loja baixa", null, null).items()).hasSize(2);
        for (String notForSale : List.of("Trânsito", "A03", "Nenhum")) {
            assertThatThrownBy(() -> service.listStock(COMPANY, notForSale, null, null))
                    .isInstanceOfSatisfying(PublicApiNotFoundException.class,
                            e -> assertThat(e.code()).isEqualTo("WAREHOUSE_NOT_FOUND"));
        }
    }

    @Test
    void api07ListsArePaginatedFromPageZeroWithAtMostOneHundredItems() {
        List<Product> many = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            many.add(product(100L + i, String.format("P-%03d", i), "Artigo " + i, "10.00"));
        }
        when(products.listForCompany(COMPANY)).thenReturn(many);

        PublicPage<PublicProductDTO> defaults = service.listProducts(COMPANY, null, null, null);
        assertThat(defaults.items()).hasSize(50);
        assertThat(defaults.size()).isEqualTo(50);
        assertThat(defaults.totalPages()).isEqualTo(3);

        PublicPage<PublicProductDTO> capped = service.listProducts(COMPANY, null, 1, 500);
        assertThat(capped.size()).isEqualTo(100);
        assertThat(capped.page()).isEqualTo(1);
        assertThat(capped.items()).hasSize(50);
        assertThat(capped.items().get(0).sku()).isEqualTo("P-100");
        assertThat(capped.totalItems()).isEqualTo(150);
        assertThat(capped.totalPages()).isEqualTo(2);
    }

    private static Product product(Long id, String sku, String name, String price) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setUnitPrice(new BigDecimal(price));
        return p;
    }

    private static Warehouse warehouse(Long id, String code, String name, WarehouseType type,
                                       boolean active, boolean allowsSales) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setWarehouseNumber(code);
        w.setName(name);
        w.setType(type);
        w.setActive(active);
        w.setAllowsSales(allowsSales);
        return w;
    }

    private static Stock stock(Product product, Warehouse warehouse, String quantity) {
        Stock s = new Stock();
        s.setProduct(product);
        s.setWarehouse(warehouse);
        s.setQuantity(new BigDecimal(quantity));
        return s;
    }
}
