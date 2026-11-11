package mz.multicore.erp.modules.advances.service;

import mz.multicore.erp.modules.advances.AdvancePayments;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Finds or creates the per-company system product "Adiantamento": a service without stock at the standard VAT
 * rate. Its list price is zero on purpose; the amount always comes from the advance being received, and the
 * pricing rules refuse it on any other document ({@code Product#requireOrdinarySale}).
 */
@Component
public class AdvanceProductProvisioner {

    private final ProductLookup productLookup;
    private final ProductCommandService productCommandService;

    public AdvanceProductProvisioner(ProductLookup productLookup, ProductCommandService productCommandService) {
        this.productLookup = productLookup;
        this.productCommandService = productCommandService;
    }

    public Product forCompany(Company company) {
        String sku = AdvancePayments.systemSku(company.getId());
        return productLookup.findBySkuForCompany(sku, company.getId()).orElseGet(() -> {
            Product product = new Product();
            product.setSku(sku);
            product.setName("Adiantamento");
            product.setDescription("Artigo de sistema: sinal ou adiantamento recebido de clientes.");
            product.setUnitPrice(BigDecimal.ZERO);
            product.setPurchasePrice(BigDecimal.ZERO);
            product.setMinStock(BigDecimal.ZERO);
            product.setSaleType(ProductSaleType.SERVICE);
            product.setStockTracked(false);
            product.getCompanies().add(company);
            return productCommandService.saveProduct(product);
        });
    }
}
