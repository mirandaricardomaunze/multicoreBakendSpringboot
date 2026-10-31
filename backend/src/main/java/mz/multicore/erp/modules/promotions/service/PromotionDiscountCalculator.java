package mz.multicore.erp.modules.promotions.service;

import mz.multicore.erp.modules.promotions.model.Promotion;
import mz.multicore.erp.modules.promotions.repository.PromotionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class PromotionDiscountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;

    public PromotionDiscountCalculator(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public Optional<PromotionService.AppliedPromotion> bestPromotion(
            Long companyId, Long productId, Long categoryId, BigDecimal quantity
    ) {
        if (productId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        PromotionService.AppliedPromotion best = null;
        for (Promotion promotion : promotionRepository.findActiveByCompany(companyId, LocalDate.now())) {
            if (!appliesTo(promotion, productId, categoryId)) continue;
            BigDecimal percent = effectiveDiscountPercent(promotion, quantity);
            if (percent.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (best == null || percent.compareTo(best.discountPercent()) > 0) {
                best = new PromotionService.AppliedPromotion(promotion.getName(), percent);
            }
        }
        return Optional.ofNullable(best);
    }

    public BigDecimal effectiveDiscountPercent(Promotion promotion, BigDecimal quantity) {
        return switch (promotion.getType()) {
            case PERCENT -> clampPercent(promotion.getPercentValue());
            case BUY_X_GET_Y -> buyXGetYPercent(promotion, quantity);
        };
    }

    private BigDecimal buyXGetYPercent(Promotion promotion, BigDecimal quantity) {
        int units = quantity.setScale(0, RoundingMode.FLOOR).intValueExact();
        int buy = promotion.getBuyQuantity() == null ? 0 : promotion.getBuyQuantity();
        int pay = promotion.getPayQuantity() == null ? 0 : promotion.getPayQuantity();
        if (units <= 0 || buy <= 0 || pay < 0 || pay >= buy) return BigDecimal.ZERO;
        int groups = units / buy;
        int freeUnits = groups * (buy - pay);
        if (freeUnits <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(freeUnits)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
    }

    private boolean appliesTo(Promotion promotion, Long productId, Long categoryId) {
        if (promotion.getProduct() != null) {
            return promotion.getProduct().getId().equals(productId);
        }
        if (promotion.getCategory() != null) {
            return categoryId != null && promotion.getCategory().getId().equals(categoryId);
        }
        return false;
    }

    private BigDecimal clampPercent(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (value.compareTo(HUNDRED) > 0) return HUNDRED;
        return value;
    }
}
