package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Finds or creates the per-company service product "Taxa de entrega — {zona}" of a delivery zone of the online shop
 * (docs/LOJA_ONLINE_SPEC.md §3.3, LO-08): a service without stock, never published, at the company's standard VAT
 * (no specific tax rate), whose list price is the zone fee. Created when the shop is activated and kept in line with
 * the zone (name and fee) on every save and at checkout, so the order line always carries the zone's current fee.
 */
@Component
public class DeliveryFeeProductProvisioner {

    static final String SKU_PREFIX = "LOJA-ENTREGA-";
    static final String NAME_PREFIX = "Taxa de entrega — ";
    private static final int SLUG_MAX_LENGTH = 40;

    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;

    public DeliveryFeeProductProvisioner(ProductLookup productLookup, ProductCommandService productCommandService) {
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
    }

    public Product forZone(Company company, OnlineShopDeliveryZone zone) {
        String sku = sku(company.getId(), zone.getName());
        String name = NAME_PREFIX + zone.getName();
        Product product = productLookup.findBySkuForCompany(sku, company.getId()).orElseGet(() -> {
            Product created = new Product();
            created.setSku(sku);
            created.setDescription("Artigo de sistema: taxa de entrega ao domicílio da loja online.");
            created.setPurchasePrice(BigDecimal.ZERO);
            created.setMinStock(BigDecimal.ZERO);
            created.setSaleType(ProductSaleType.SERVICE);
            created.setStockTracked(false);
            created.setPublishedOnline(false);
            created.getCompanies().add(company);
            return created;
        });
        if (product.getId() != null && name.equals(product.getName())
                && product.getUnitPrice() != null && product.getUnitPrice().compareTo(zone.getFee()) == 0) {
            return product;
        }
        product.setName(name);
        product.setUnitPrice(zone.getFee());
        return productCommandService.saveProduct(product);
    }

    /** Unique system SKU of a zone, e.g. {@code LOJA-ENTREGA-1-MATOLA}; zone names are unique per shop ignoring case. */
    static String sku(Long companyId, String zoneName) {
        String plain = Normalizer.normalize(zoneName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        if (slug.length() > SLUG_MAX_LENGTH) slug = slug.substring(0, SLUG_MAX_LENGTH);
        return SKU_PREFIX + companyId + "-" + (slug.isEmpty() ? "ZONA" : slug);
    }
}
