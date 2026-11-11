package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.promotions.service.PromotionService;
import org.springframework.stereotype.Component;

import mz.multicore.erp.modules.pricing.service.PriceResolutionService;
import java.math.BigDecimal;
import java.util.List;

/**
 * O servidor decide o que um caixa pode cobrar: o desktop não é prova de nada, porque a API pode
 * ser chamada directamente. Um caixa vende a preço de tabela, com promoção, com o combinado numa
 * cotação, ou até 10% de desconto. Acima disso, é precisa uma autorização de gerente.
 */
@Component
public class PosPriceGuard {

    static final BigDecimal SELLER_DISCOUNT_LIMIT = BigDecimal.TEN;

    private final ProductLookup productLookup;
    private final QuotationRepository quotationRepository;
    private final PromotionService promotionService;
    private final PosDiscountAuthorizationService authorizationService;
    private final AuditLogService auditLogService;
    /** Price lists (SA-C03); {@code null} in legacy constructors. */
    private final PriceResolutionService priceResolver;

    public PosPriceGuard(ProductLookup productLookup, QuotationRepository quotationRepository,
                         PromotionService promotionService, PosDiscountAuthorizationService authorizationService,
                         AuditLogService auditLogService) {
        this(productLookup, quotationRepository, promotionService, authorizationService, auditLogService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PosPriceGuard(ProductLookup productLookup, QuotationRepository quotationRepository,
                         PromotionService promotionService, PosDiscountAuthorizationService authorizationService,
                         AuditLogService auditLogService,
                         PriceResolutionService priceResolver) {
        this.priceResolver = priceResolver;
        this.productLookup = productLookup;
        this.quotationRepository = quotationRepository;
        this.promotionService = promotionService;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    public void requireAuthorized(POSCheckoutRequest request) {
        logTypedWeights(request);
        String role = CurrentUserContext.getRole();
        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) return;

        List<QuotationLine> quoted = quotedLines(request);
        PriceResolutionService.PriceSelection pricing = priceResolver == null || request.clientId() == null
                ? PriceResolutionService.PriceSelection.NONE
                : priceResolver.selectionFor(request.companyId(), request.clientId(), java.time.LocalDate.now());
        BigDecimal needed = BigDecimal.ZERO;
        for (POSCheckoutLineRequest line : request.lines()) {
            Product product = productLookup.findForCompany(line.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + line.productId()));
            QuotationLine agreed = quoted.stream()
                    .filter(candidate -> candidate.getProduct().getId().equals(product.getId())).findFirst().orElse(null);
            requireAllowedPrice(line, product, agreed, listPrice(pricing, product, line.quantity()));
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            if (discount.compareTo(allowedDiscount(request, line, product, agreed)) > 0) needed = needed.max(discount);
        }
        if (needed.signum() > 0) {
            String manager = authorizationService.consume(request.discountAuthorization(), request.companyId(), needed);
            auditLogService.logCurrent("POS_DISCOUNT_AUTHORIZED", "Desconto de " + needed + "% autorizado por " + manager);
        }
    }

    /** A weight typed instead of read from the scale is audited per line (anti-fraud report, SA-C21). */
    private void logTypedWeights(POSCheckoutRequest request) {
        for (POSCheckoutLineRequest line : request.lines()) {
            if (!POSCheckoutLineRequest.WEIGHT_TYPED.equals(line.weightSource())) continue;
            auditLogService.logCurrent("POS_WEIGHT_MANUAL", "Venda ao peso com peso escrito à mão: " + line.quantity()
                    + " kg do artigo " + line.productId() + ".");
        }
    }

    /** Price stored when the desktop sends none: the client's list price, otherwise the product rule. */
    static BigDecimal defaultPrice(PosPriceGuard guard, POSCheckoutRequest request, Product product, BigDecimal quantity) {
        if (guard == null || guard.priceResolver == null) return product.effectiveUnitPrice(quantity);
        var selection = guard.priceResolver.selectionFor(request.companyId(), request.clientId(), java.time.LocalDate.now());
        return guard.priceResolver.resolve(selection, product, quantity).unitPrice();
    }

    private List<QuotationLine> quotedLines(POSCheckoutRequest request) {
        if (request.quotationId() == null) return List.of();
        return quotationRepository.findByIdWithLinesAndCompanyId(request.quotationId(), request.companyId())
                .map(Quotation::getLines).orElse(List.of());
    }

    /**
     * Client list price, or {@code null}. The floor never becomes stricter than before: with a list, the lower
     * of the list price and the normal price applies.
     */
    private BigDecimal listPrice(PriceResolutionService.PriceSelection pricing, Product product, BigDecimal quantity) {
        if (priceResolver == null || !pricing.hasList()) return null;
        PriceResolutionService.ResolvedPrice resolved = priceResolver.resolve(pricing, product, quantity);
        return PriceResolutionService.SOURCE_PRICE_LIST.equals(resolved.source()) ? resolved.unitPrice() : null;
    }

    private static void requireAllowedPrice(POSCheckoutLineRequest line, Product product, QuotationLine agreed,
                                            BigDecimal listPrice) {
        BigDecimal price = line.unitPrice();
        if (price == null || price.signum() <= 0) return;
        BigDecimal floor = agreed != null ? agreed.getUnitPrice() : product.effectiveUnitPrice(line.quantity());
        if (agreed == null && listPrice != null && floor != null) floor = floor.min(listPrice);
        if (price.compareTo(floor) < 0) {
            throw new BusinessRuleException("O preço de '" + product.getName() + "' é inferior ao permitido. "
                    + "Só um gerente pode vender abaixo do preço de tabela.");
        }
    }

    private BigDecimal allowedDiscount(POSCheckoutRequest request, POSCheckoutLineRequest line, Product product, QuotationLine agreed) {
        BigDecimal allowed = SELLER_DISCOUNT_LIMIT;
        if (agreed != null && agreed.getDiscountPercentage() != null) allowed = allowed.max(agreed.getDiscountPercentage());
        Long categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        return promotionService.bestPromotion(request.companyId(), product.getId(), categoryId, line.quantity())
                .map(PromotionService.AppliedPromotion::discountPercent).map(allowed::max).orElse(allowed);
    }
}
