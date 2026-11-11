package mz.multicore.erp.modules.promotions.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;
import mz.multicore.erp.modules.promotions.model.Promotion;
import mz.multicore.erp.modules.promotions.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Motor de promoções de loja: gere promoções e traduz promoções aplicáveis em desconto percentual efectivo.
 */
@Service
public class PromotionService {

    private final PromotionLifecycleService lifecycleService;
    private final PromotionDiscountCalculator discountCalculator;

    @Autowired
    public PromotionService(
            PromotionLifecycleService lifecycleService,
            PromotionDiscountCalculator discountCalculator
    ) {
        this.lifecycleService = lifecycleService;
        this.discountCalculator = discountCalculator;
    }

    public PromotionService(
            PromotionRepository promotionRepository,
            CompanyLookup companyLookup,
            ProductRepository productRepository,
            ProductCategoryRepository categoryRepository,
            AuditLogService auditLogService
    ) {
        this(
                new PromotionLifecycleService(
                        promotionRepository, companyLookup, productRepository, categoryRepository, auditLogService),
                new PromotionDiscountCalculator(promotionRepository)
        );
    }

    @Transactional(readOnly = true)
    public List<PromotionDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return lifecycleService.findByCompany(companyId);
    }

    @Transactional
    public PromotionDTO createPromotion(CreatePromotionRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar promoções");
        CurrentUserContext.requireCompany(request.companyId());
        return lifecycleService.createPromotion(request);
    }

    @Transactional
    public PromotionDTO setActive(Long promotionId, boolean active) {
        PermissionGuard.requireManagerOrAdmin("alterar promoções");
        return lifecycleService.setActive(promotionId, active);
    }

    @Transactional(readOnly = true)
    public Optional<AppliedPromotion> bestPromotion(Long companyId, Long productId, Long categoryId, BigDecimal quantity) {
        return discountCalculator.bestPromotion(companyId, productId, categoryId, quantity);
    }

    public BigDecimal effectiveDiscountPercent(Promotion promotion, BigDecimal quantity) {
        return discountCalculator.effectiveDiscountPercent(promotion, quantity);
    }

    /** Promoção aplicada a uma linha: nome (para mostrar/recibo) e desconto percentual efectivo. */
    public record AppliedPromotion(String name, BigDecimal discountPercent) {}
}
