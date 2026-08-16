package mz.multicore.erp.modules.promotions.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;
import mz.multicore.erp.modules.promotions.model.Promotion;
import mz.multicore.erp.modules.promotions.model.PromotionType;
import mz.multicore.erp.modules.promotions.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Motor de promoções de loja. Responsável por: gerir promoções (CRUD com permissão e auditoria) e,
 * sobretudo, traduzir uma promoção aplicável numa linha de venda num <b>desconto percentual
 * efectivo</b> — o que permite reutilizar todo o fluxo de checkout existente (que já trabalha com
 * desconto % por linha) sem mexer no cálculo do POS/faturação.
 */
@Service
public class PromotionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    public PromotionService(PromotionRepository promotionRepository,
                            CompanyRepository companyRepository,
                            ProductRepository productRepository,
                            ProductCategoryRepository categoryRepository,
                            AuditLogService auditLogService) {
        this.promotionRepository = promotionRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PromotionDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return promotionRepository.findByCompany(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public PromotionDTO createPromotion(CreatePromotionRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar promoções");
        CurrentUserContext.requireCompany(request.companyId());

        PromotionType type = parseType(request.type());
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

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

    @Transactional
    public PromotionDTO setActive(Long promotionId, boolean active) {
        PermissionGuard.requireManagerOrAdmin("alterar promoções");
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new BusinessRuleException("Promoção não encontrada."));
        CurrentUserContext.requireCompany(promotion.getCompany().getId());
        promotion.setActive(active);
        Promotion saved = promotionRepository.save(promotion);
        auditLogService.logCurrent("PROMOTION_" + (active ? "ACTIVATE" : "DEACTIVATE"),
                "Promoção '" + saved.getName() + "' " + (active ? "activada" : "desactivada") + ".");
        return toDTO(saved);
    }

    /**
     * Melhor promoção aplicável a uma linha de venda (produto + quantidade), traduzida no desconto
     * percentual efectivo. Considera só promoções activas e na janela de validade de hoje, com
     * alcance no produto ou na sua categoria. Devolve a de maior desconto, ou vazio se nenhuma.
     */
    @Transactional(readOnly = true)
    public Optional<AppliedPromotion> bestPromotion(Long companyId, Long productId, Long categoryId, BigDecimal quantity) {
        if (productId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        AppliedPromotion best = null;
        for (Promotion promotion : promotionRepository.findActiveByCompany(companyId, LocalDate.now())) {
            if (!appliesTo(promotion, productId, categoryId)) continue;
            BigDecimal percent = effectiveDiscountPercent(promotion, quantity);
            if (percent.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (best == null || percent.compareTo(best.discountPercent()) > 0) {
                best = new AppliedPromotion(promotion.getName(), percent);
            }
        }
        return Optional.ofNullable(best);
    }

    /** Converte a promoção no desconto percentual efectivo para a quantidade dada (0–100). */
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
        // Desconto efectivo = unidades grátis / unidades totais (a venda é sobre a quantidade pedida).
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

    private void applyScope(Promotion promotion, CreatePromotionRequest request) {
        boolean hasProduct = request.productId() != null;
        boolean hasCategory = request.categoryId() != null;
        if (hasProduct == hasCategory) {
            throw new BusinessRuleException("Indique exactamente um alcance: produto OU categoria.");
        }
        if (hasProduct) {
            Product product = productRepository.findByIdAndCompaniesId(request.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado na empresa activa."));
            promotion.setProduct(product);
        } else {
            ProductCategory category = categoryRepository.findByIdAndCompaniesId(request.categoryId(), request.companyId())
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

    private BigDecimal clampPercent(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (value.compareTo(HUNDRED) > 0) return HUNDRED;
        return value;
    }

    private PromotionType parseType(String raw) {
        try {
            return PromotionType.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de promoção inválido: " + raw);
        }
    }

    private PromotionDTO toDTO(Promotion p) {
        return new PromotionDTO(
                p.getId(),
                p.getName(),
                p.getType() != null ? p.getType().name() : null,
                p.getProduct() != null ? p.getProduct().getId() : null,
                p.getProduct() != null ? p.getProduct().getName() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getPercentValue(),
                p.getBuyQuantity(),
                p.getPayQuantity(),
                p.getStartDate(),
                p.getEndDate(),
                p.isActive()
        );
    }

    /** Promoção aplicada a uma linha: nome (para mostrar/recibo) e desconto percentual efectivo. */
    public record AppliedPromotion(String name, BigDecimal discountPercent) {}
}
