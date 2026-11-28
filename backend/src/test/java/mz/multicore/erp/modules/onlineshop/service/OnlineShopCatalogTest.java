package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopProductDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LO-02 (docs/LOJA_ONLINE_SPEC.md §3.1): the public shop lists only the company's active products with
 * "Publicar na loja online" switched on; the switch is off by default; name, category and VAT-inclusive price come
 * from the product.
 * Harness LO-03 (§3.2): each product shows only Disponível/Esgotado — available when stock minus active reservations
 * is positive in at least one pickup shop of the company — and the DTO never carries quantities, costs or suppliers.
 */
class OnlineShopCatalogTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long PICKUP_A = 10L;
    private static final Long PICKUP_B = 11L;
    private static final Long CENTRAL_WAREHOUSE = 12L;

    private final OnlineShopSettingsRepository settingsRepository = mock(OnlineShopSettingsRepository.class);
    private final ProductLookup productLookup = mock(ProductLookup.class);
    private final ProductCommandService productCommandService = mock(ProductCommandService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final StockLookup stockLookup = mock(StockLookup.class);
    private final OrderReservationLookup reservationLookup = mock(OrderReservationLookup.class);

    private final OnlineShopCatalogService service = new OnlineShopCatalogService(
            settingsRepository, productLookup, productCommandService, auditLogService, stockLookup, reservationLookup);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(settingsRepository.findByPublicCode("mercearia-baixa")).thenReturn(Optional.of(shop(true)));
        when(productCommandService.saveProduct(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void lo02_artigoNovoNaoEstaPublicado() {
        assertFalse(new Product().isPublishedOnline());
    }

    @Test
    void lo02_soArtigosPublicadosAparecemNaLoja() {
        Product arroz = product(1L, "ARROZ-5KG", "Arroz 5 kg", "100.00", true);
        Product oleo = product(2L, "OLEO-1L", "Óleo 1 L", "50.00", false);
        Product advance = product(3L, AdvancePayments.systemSku(COMPANY_ID), "Adiantamento", "0", true);
        when(productLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(arroz, oleo, advance));
        when(stockLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(stock(arroz, PICKUP_A, "3")));

        List<OnlineShopProductDTO> catalog = service.listProducts("mercearia-baixa");

        assertEquals(List.of(new OnlineShopProductDTO("ARROZ-5KG", "Arroz 5 kg", "Mercearia",
                new BigDecimal("116.00"), true)), catalog);
    }

    @Test
    void lo02_codigoIgnoraMaiusculasEEspacos() {
        when(productLookup.listForCompany(COMPANY_ID))
                .thenReturn(List.of(product(1L, "ARROZ-5KG", "Arroz 5 kg", "100.00", true)));

        assertEquals(1, service.listProducts("  Mercearia-Baixa ").size());
    }

    @Test
    void lo02_lojaDesactivadaOuInexistenteIndisponivel() {
        when(settingsRepository.findByPublicCode("fechada")).thenReturn(Optional.of(shop(false)));

        assertEquals("Loja indisponível.",
                assertThrows(BusinessRuleException.class, () -> service.listProducts("fechada")).getMessage());
        assertEquals("Loja indisponível.",
                assertThrows(BusinessRuleException.class, () -> service.listProducts("nao-existe")).getMessage());
        assertEquals("Loja indisponível.",
                assertThrows(BusinessRuleException.class, () -> service.listProducts(null)).getMessage());
    }

    @Test
    void lo02_publicarEDespublicarArtigoDaEmpresaComAuditoria() {
        Product arroz = product(1L, "ARROZ-5KG", "Arroz 5 kg", "100.00", false);
        when(productLookup.requireForCompany(1L, COMPANY_ID)).thenReturn(arroz);

        service.setPublished(1L, true);
        assertTrue(arroz.isPublishedOnline());

        service.setPublished(1L, false);
        assertFalse(arroz.isPublishedOnline());
        verify(auditLogService, org.mockito.Mockito.times(2)).logCurrent(eq("ONLINE_SHOP_PUBLISH"), anyString());
    }

    @Test
    void lo02_artigoAdiantamentoNaoSePublica() {
        Product advance = product(3L, AdvancePayments.systemSku(COMPANY_ID), "Adiantamento", "0", false);
        when(productLookup.requireForCompany(3L, COMPANY_ID)).thenReturn(advance);

        assertThrows(BusinessRuleException.class, () -> service.setPublished(3L, true));
        assertFalse(advance.isPublishedOnline());
        verify(productCommandService, never()).saveProduct(any());
    }

    @Test
    void lo03_disponivelComStockLivreNumaLojaDeLevantamento() {
        Product arroz = product(1L, "ARROZ-5KG", "Arroz 5 kg", "100.00", true);
        when(productLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(arroz));
        when(stockLookup.listForCompany(COMPANY_ID))
                .thenReturn(List.of(stock(arroz, PICKUP_A, "0"), stock(arroz, PICKUP_B, "2")));

        assertTrue(service.listProducts("mercearia-baixa").get(0).available());
    }

    @Test
    void lo03_esgotadoQuandoAsReservasConsomemOStock() {
        Product arroz = product(1L, "ARROZ-5KG", "Arroz 5 kg", "100.00", true);
        when(productLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(arroz));
        when(stockLookup.listForCompany(COMPANY_ID))
                .thenReturn(List.of(stock(arroz, PICKUP_A, "4"), stock(arroz, PICKUP_B, "1")));
        when(reservationLookup.activeReservationsForCompany(COMPANY_ID)).thenReturn(List.of(
                new ReservedStock(1L, PICKUP_A, new BigDecimal("4")),
                new ReservedStock(1L, PICKUP_B, new BigDecimal("1"))));

        assertFalse(service.listProducts("mercearia-baixa").get(0).available());
    }

    @Test
    void lo03_stockForaDasLojasDeLevantamentoNaoConta() {
        Product arroz = product(1L, "ARROZ-5KG", "Arroz 5 kg", "100.00", true);
        when(productLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(arroz));
        when(stockLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(stock(arroz, CENTRAL_WAREHOUSE, "100")));

        assertFalse(service.listProducts("mercearia-baixa").get(0).available());
    }

    @Test
    void lo03_servicoSemControloDeStockEstaSempreDisponivel() {
        Product montagem = product(5L, "MONTAGEM", "Montagem", "200.00", true);
        montagem.setStockTracked(false);
        when(productLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(montagem));

        assertTrue(service.listProducts("mercearia-baixa").get(0).available());
    }

    @Test
    void lo03_artigoDaLojaNaoMostraQuantidadesCustosNemFornecedores() {
        List<String> fields = Arrays.stream(OnlineShopProductDTO.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase(java.util.Locale.ROOT)).toList();

        assertEquals(List.of("sku", "name", "category", "pricewithvat", "available"), fields);
    }

    private static Stock stock(Product product, Long warehouseId, String quantity) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse(warehouse);
        stock.setQuantity(new BigDecimal(quantity));
        return stock;
    }

    private static OnlineShopSettings shop(boolean enabled) {
        Company company = new Company();
        company.setId(COMPANY_ID);
        OnlineShopSettings settings = new OnlineShopSettings();
        settings.setCompany(company);
        settings.setEnabled(enabled);
        settings.setPublicCode("mercearia-baixa");
        settings.setPickupWarehouseIds(new java.util.ArrayList<>(List.of(PICKUP_A, PICKUP_B)));
        return settings;
    }

    private static Product product(Long id, String sku, String name, String price, boolean published) {
        ProductCategory category = new ProductCategory();
        category.setName("Mercearia");
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setUnitPrice(new BigDecimal(price));
        product.setCategory(category);
        product.setPublishedOnline(published);
        return product;
    }
}
