package com.phcpro.modules.promotions.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.model.ProductCategory;
import com.phcpro.modules.comercial.repository.ProductCategoryRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.promotions.dto.CreatePromotionRequest;
import com.phcpro.modules.promotions.model.Promotion;
import com.phcpro.modules.promotions.model.PromotionType;
import com.phcpro.modules.promotions.repository.PromotionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do motor de promoções: tradução em desconto percentual efectivo (percentagem e
 * "leve X, pague Y"), escolha da melhor promoção activa e validações de criação. Mocks nas
 * dependências.
 */
class PromotionServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long CATEGORY_ID = 7L;

    private PromotionRepository promotionRepository;
    private PromotionService service;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        service = new PromotionService(
                promotionRepository,
                mock(CompanyRepository.class),
                mock(ProductRepository.class),
                mock(ProductCategoryRepository.class),
                mock(AuditLogService.class));
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void percent_devolveAPercentagemConfigurada() {
        Promotion promo = percentPromotion(new BigDecimal("15"));
        assertEquals(0, new BigDecimal("15.00").compareTo(
                service.effectiveDiscountPercent(promo, new BigDecimal("3")).setScale(2)));
    }

    @Test
    void leve3pague2_em3unidades_da33porcento() {
        Promotion promo = buyXGetY(3, 2);
        // 1 grátis em 3 = 33.33%
        assertEquals(new BigDecimal("33.33"),
                service.effectiveDiscountPercent(promo, new BigDecimal("3")));
    }

    @Test
    void leve3pague2_em4unidades_da25porcento() {
        Promotion promo = buyXGetY(3, 2);
        // floor(4/3)=1 grupo → 1 grátis em 4 = 25%
        assertEquals(new BigDecimal("25.00"),
                service.effectiveDiscountPercent(promo, new BigDecimal("4")));
    }

    @Test
    void leve3pague2_em2unidades_naoAtingeOgrupo_da0() {
        Promotion promo = buyXGetY(3, 2);
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.effectiveDiscountPercent(promo, new BigDecimal("2"))));
    }

    @Test
    void bestPromotion_escolheMaiorDesconto_porProdutoOuCategoria() {
        Promotion porProduto = percentPromotion(new BigDecimal("10"));
        porProduto.setProduct(product(PRODUCT_ID));
        Promotion porCategoria = percentPromotion(new BigDecimal("20"));
        porCategoria.setCategory(category(CATEGORY_ID));
        when(promotionRepository.findActiveByCompany(eq(COMPANY_ID), any(LocalDate.class)))
                .thenReturn(List.of(porProduto, porCategoria));

        Optional<PromotionService.AppliedPromotion> best =
                service.bestPromotion(COMPANY_ID, PRODUCT_ID, CATEGORY_ID, new BigDecimal("1"));

        assertTrue(best.isPresent());
        assertEquals(0, new BigDecimal("20").compareTo(best.get().discountPercent()));
    }

    @Test
    void bestPromotion_semCorrespondencia_devolveVazio() {
        Promotion outra = percentPromotion(new BigDecimal("10"));
        outra.setProduct(product(999L));
        when(promotionRepository.findActiveByCompany(eq(COMPANY_ID), any(LocalDate.class)))
                .thenReturn(List.of(outra));

        assertTrue(service.bestPromotion(COMPANY_ID, PRODUCT_ID, CATEGORY_ID, new BigDecimal("1")).isEmpty());
    }

    @Test
    void createPromotion_percentagemForaDoIntervalo_lancaBusinessRule() {
        CreatePromotionRequest req = new CreatePromotionRequest(
                COMPANY_ID, "Promo", "PERCENT", PRODUCT_ID, null,
                new BigDecimal("150"), null, null,
                LocalDate.now(), LocalDate.now().plusDays(10));
        // Falha na validação antes de tocar nos repositórios de empresa/produto.
        assertThrows(BusinessRuleException.class, () -> service.createPromotion(req));
    }

    @Test
    void createPromotion_semAlcanceOuComAmbos_lancaBusinessRule() {
        CreatePromotionRequest nenhum = new CreatePromotionRequest(
                COMPANY_ID, "Promo", "PERCENT", null, null,
                new BigDecimal("10"), null, null,
                LocalDate.now(), LocalDate.now().plusDays(10));
        assertThrows(BusinessRuleException.class, () -> service.createPromotion(nenhum));
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private Promotion percentPromotion(BigDecimal percent) {
        Promotion p = new Promotion();
        p.setType(PromotionType.PERCENT);
        p.setPercentValue(percent);
        return p;
    }

    private Promotion buyXGetY(int buy, int pay) {
        Promotion p = new Promotion();
        p.setType(PromotionType.BUY_X_GET_Y);
        p.setBuyQuantity(buy);
        p.setPayQuantity(pay);
        return p;
    }

    private Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        return p;
    }

    private ProductCategory category(Long id) {
        ProductCategory c = new ProductCategory();
        c.setId(id);
        return c;
    }
}
