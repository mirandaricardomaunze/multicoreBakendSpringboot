package mz.multicore.erp.modules.promotions.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;
import mz.multicore.erp.modules.promotions.model.Promotion;
import mz.multicore.erp.modules.promotions.model.PromotionType;
import mz.multicore.erp.modules.promotions.repository.PromotionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PromotionLifecycleService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final CompanyLookup companyLookup;
    private final ProductLookup productLookup;
    private final AuditLogService auditLogService;

    public PromotionLifecycleService(
            PromotionRepository promotionRepository,
            CompanyLookup companyLookup,
            ProductLookup productLookup,
            AuditLogService auditLogService
    ) {
        this.promotionRepository = promotionRepository;
        this.companyLookup = companyLookup;
        this.productLookup = productLookup;
        this.auditLogService = auditLogService;
    }

    public List<PromotionDTO> findByCompany(Long companyId) {
        return promotionRepository.findByCompany(companyId).stream().map(this::toDTO).toList();
    }

    public PromotionDTO createPromotion(CreatePromotionRequest request) {
        PromotionType type = parseType(request.type());
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        Company company = companyLookup.require(request.companyId());

        Promotion promotion = new Promotion();
        promotion.setCompany(company);
        promotion.setName(request.name().trim());
        promotion.setType(type);
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setActive(true);
        promotion.setCreatedBy(CurrentUserContext.getUsername());

        applyScope(promotion, request);
        applyTypeParameters(promotion, type, request);

        Promotion saved = promotionRepository.save(promotion);
        auditLogService.logCurrent("PROMOTION_CREATE",
                "Promoção '" + saved.getName() + "' (" + type + ") criada, válida "
                        + saved.getStartDate() + " a " + saved.getEndDate() + ".");
        return toDTO(saved);
    }

    public PromotionDTO setActive(Long promotionId, boolean active) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new BusinessRuleException("Promoção não encontrada."));
        CurrentUserContext.requireCompany(promotion.getCompany().getId());
        promotion.setActive(active);
        Promotion saved = promotionRepository.save(promotion);
        auditLogService.logCurrent("PROMOTION_" + (active ? "ACTIVATE" : "DEACTIVATE"),
                "Promoção '" + saved.getName() + "' " + (active ? "activada" : "desactivada") + ".");
        return toDTO(saved);
    }

    private void applyScope(Promotion promotion, CreatePromotionRequest request) {
        boolean hasProduct = request.productId() != null;
        boolean hasCategory = request.categoryId() != null;
        if (hasProduct == hasCategory) {
            throw new BusinessRuleException("Indique exactamente um alcance: produto OU categoria.");
        }
        if (hasProduct) {
            Product product = productLookup.findForCompany(request.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado na empresa activa."));
            promotion.setProduct(product);
        } else {
            ProductCategory category = productLookup.findCategoryForCompany(request.categoryId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Categoria não encontrada na empresa activa."));
            promotion.setCategory(category);
        }
    }

    private void applyTypeParameters(Promotion promotion, PromotionType type, CreatePromotionRequest request) {
        switch (type) {
            case PERCENT -> {
                BigDecimal percent = request.percentValue();
                if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0 || percent.compareTo(HUNDRED) > 0) {
                    throw new BusinessRuleException("A percentagem de desconto deve estar entre 0 e 100.");
                }
                promotion.setPercentValue(percent);
            }
            case BUY_X_GET_Y -> {
                Integer buy = request.buyQuantity();
                Integer pay = request.payQuantity();
                if (buy == null || pay == null || buy <= 0 || pay < 0 || pay >= buy) {
                    throw new BusinessRuleException("Para 'leve X, pague Y': X e Y inteiros com Y inferior a X.");
                }
                if (promotion.getProduct() == null) {
                    throw new BusinessRuleException("A promoção 'leve X, pague Y' aplica-se a um produto específico.");
                }
                promotion.setBuyQuantity(buy);
                promotion.setPayQuantity(pay);
            }
        }
    }

    private PromotionType parseType(String raw) {
        try {
            return PromotionType.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de promoção inválido: " + raw);
        }
    }

    public PromotionDTO toDTO(Promotion p) {
        Long buId = p.getProduct() != null && p.getProduct().getBusinessUnit() != null ? p.getProduct().getBusinessUnit().getId() : null;
        String buName = p.getProduct() != null && p.getProduct().getBusinessUnit() != null ? p.getProduct().getBusinessUnit().getName() : null;
        return new PromotionDTO(
                p.getId(), p.getName(), p.getType() != null ? p.getType().name() : null,
                p.getProduct() != null ? p.getProduct().getId() : null,
                p.getProduct() != null ? p.getProduct().getName() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getPercentValue(), p.getBuyQuantity(), p.getPayQuantity(),
                p.getStartDate(), p.getEndDate(), p.isActive(), buId, buName
        );
    }
}
