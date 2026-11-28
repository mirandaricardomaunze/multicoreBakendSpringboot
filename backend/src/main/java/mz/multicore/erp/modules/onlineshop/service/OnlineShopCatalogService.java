package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopProductDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Catalogue of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.1, LO-02): only the company's active products
 * with "Publicar na loja online" switched on (never the system advance product), and the switch itself. Products are
 * read and written through the comercial module's lookup and command services.
 */
@Service
public class OnlineShopCatalogService {

    static final String SHOP_UNAVAILABLE = "Loja indisponível.";

    private final OnlineShopSettingsRepository settingsRepository;
    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;
    private final AuditLogService auditLogService;

    public OnlineShopCatalogService(OnlineShopSettingsRepository settingsRepository, ProductLookup productLookup,
                                    ProductCommandService productCommandService, AuditLogService auditLogService) {
        this.settingsRepository = settingsRepository;
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
        this.auditLogService = auditLogService;
    }

    /** Published products of the shop behind {@code /loja/{code}}; disabled or unknown shop → "Loja indisponível.". */
    @Transactional(readOnly = true)
    public List<OnlineShopProductDTO> listProducts(String publicCode) {
        Long companyId = requireOpenShop(publicCode).getCompany().getId();
        return productLookup.listForCompany(companyId).stream()
                .filter(OnlineShopCatalogService::isOnSale)
                .map(OnlineShopCatalogService::toDTO)
                .toList();
    }

    /** Switches "Publicar na loja online" of a product of the active company, audited. */
    @Transactional
    public void setPublished(Long productId, boolean published) {
        Product product = productLookup.requireForCompany(productId, CurrentUserContext.requireCurrentCompanyId());
        if (published) {
            product.requireOrdinarySale();
        }
        product.setPublishedOnline(published);
        productCommandService.saveProduct(product);
        auditLogService.logCurrent("ONLINE_SHOP_PUBLISH", "Artigo " + product.getSku()
                + (published ? " publicado na" : " retirado da") + " loja online");
    }

    private OnlineShopSettings requireOpenShop(String publicCode) {
        if (publicCode == null || publicCode.isBlank()) {
            throw new BusinessRuleException(SHOP_UNAVAILABLE);
        }
        return settingsRepository.findByPublicCode(publicCode.trim().toLowerCase(Locale.ROOT))
                .filter(OnlineShopSettings::isEnabled)
                .orElseThrow(() -> new BusinessRuleException(SHOP_UNAVAILABLE));
    }

    private static boolean isOnSale(Product product) {
        return product.isPublishedOnline() && !AdvancePayments.isSystemSku(product.getSku());
    }

    private static OnlineShopProductDTO toDTO(Product product) {
        BigDecimal priceWithVat = LineCalculator.compute(product.getUnitPrice(), BigDecimal.ONE, BigDecimal.ZERO,
                product.effectiveTaxRate()).total();
        return new OnlineShopProductDTO(product.getSku(), product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : null, priceWithVat);
    }
}
