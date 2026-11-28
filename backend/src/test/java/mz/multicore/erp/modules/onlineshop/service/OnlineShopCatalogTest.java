package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopProductDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
 */
class OnlineShopCatalogTest {

    private static final Long COMPANY_ID = 1L;

    private final OnlineShopSettingsRepository settingsRepository = mock(OnlineShopSettingsRepository.class);
    private final ProductLookup productLookup = mock(ProductLookup.class);
    private final ProductCommandService productCommandService = mock(ProductCommandService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final OnlineShopCatalogService service = new OnlineShopCatalogService(
            settingsRepository, productLookup, productCommandService, auditLogService);

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

        List<OnlineShopProductDTO> catalog = service.listProducts("mercearia-baixa");

        assertEquals(List.of(new OnlineShopProductDTO("ARROZ-5KG", "Arroz 5 kg", "Mercearia",
                new BigDecimal("116.00"))), catalog);
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

    private static OnlineShopSettings shop(boolean enabled) {
        Company company = new Company();
        company.setId(COMPANY_ID);
        OnlineShopSettings settings = new OnlineShopSettings();
        settings.setCompany(company);
        settings.setEnabled(enabled);
        settings.setPublicCode("mercearia-baixa");
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
