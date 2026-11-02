package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.promotions.service.PromotionService;
import org.springframework.stereotype.Component;

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

    private final ProductRepository productRepository;
    private final QuotationRepository quotationRepository;
    private final PromotionService promotionService;
    private final PosDiscountAuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public PosPriceGuard(ProductRepository productRepository, QuotationRepository quotationRepository,
                         PromotionService promotionService, PosDiscountAuthorizationService authorizationService,
                         AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.quotationRepository = quotationRepository;
        this.promotionService = promotionService;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    public void requireAuthorized(POSCheckoutRequest request) {
        String role = CurrentUserContext.getRole();
        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) return;

        List<QuotationLine> quoted = quotedLines(request);
        BigDecimal needed = BigDecimal.ZERO;
        for (POSCheckoutLineRequest line : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(line.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + line.productId()));
            QuotationLine agreed = quoted.stream()
                    .filter(candidate -> candidate.getProduct().getId().equals(product.getId())).findFirst().orElse(null);
            requireAllowedPrice(line, product, agreed);
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            if (discount.compareTo(allowedDiscount(request, line, product, agreed)) > 0) needed = needed.max(discount);
        }
        if (needed.signum() > 0) {
            String manager = authorizationService.consume(request.discountAuthorization(), request.companyId(), needed);
            auditLogService.logCurrent("POS_DISCOUNT_AUTHORIZED", "Desconto de " + needed + "% autorizado por " + manager);
        }
    }

    private List<QuotationLine> quotedLines(POSCheckoutRequest request) {
        if (request.quotationId() == null) return List.of();
        return quotationRepository.findByIdWithLinesAndCompanyId(request.quotationId(), request.companyId())
                .map(Quotation::getLines).orElse(List.of());
    }

    private static void requireAllowedPrice(POSCheckoutLineRequest line, Product product, QuotationLine agreed) {
        BigDecimal price = line.unitPrice();
        if (price == null || price.signum() <= 0) return;
        BigDecimal floor = agreed != null ? agreed.getUnitPrice() : product.effectiveUnitPrice(line.quantity());
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
