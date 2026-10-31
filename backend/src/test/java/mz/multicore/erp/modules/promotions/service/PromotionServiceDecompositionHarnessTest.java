package mz.multicore.erp.modules.promotions.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.promotions.model.Promotion;
import mz.multicore.erp.modules.promotions.model.PromotionType;
import mz.multicore.erp.modules.promotions.repository.PromotionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/promotions/service");

    @Test
    void promotionLifecycleAndDiscountCalculationStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("PromotionService.java"));
        String lifecycle = Files.readString(DIR.resolve("PromotionLifecycleService.java"));
        String calculator = Files.readString(DIR.resolve("PromotionDiscountCalculator.java"));

        assertTrue(facade.contains("lifecycleService.createPromotion(request)"));
        assertTrue(facade.contains("discountCalculator.bestPromotion("));
        assertFalse(facade.contains("buyXGetYPercent("));
        assertTrue(lifecycle.contains("PROMOTION_CREATE"));
        assertTrue(calculator.contains("buyXGetYPercent"));

        PromotionRepository repo = mock(PromotionRepository.class);
        Product product = new Product();
        product.setId(10L);
        Promotion promo = new Promotion();
        promo.setName("Leve 3 Pague 2");
        promo.setType(PromotionType.BUY_X_GET_Y);
        promo.setProduct(product);
        promo.setBuyQuantity(3);
        promo.setPayQuantity(2);
        when(repo.findActiveByCompany(1L, LocalDate.now())).thenReturn(List.of(promo));

        PromotionDiscountCalculator calc = new PromotionDiscountCalculator(repo);
        Optional<PromotionService.AppliedPromotion> best = calc.bestPromotion(1L, 10L, null, new BigDecimal("3"));
        assertTrue(best.isPresent());
        assertEquals(0, best.get().discountPercent().compareTo(new BigDecimal("33.33")));

        assertTrue(Files.readAllLines(DIR.resolve("PromotionService.java")).size() <= 90);
        assertTrue(Files.readAllLines(DIR.resolve("PromotionLifecycleService.java")).size() <= 160);
        assertTrue(Files.readAllLines(DIR.resolve("PromotionDiscountCalculator.java")).size() <= 90);
    }
}
