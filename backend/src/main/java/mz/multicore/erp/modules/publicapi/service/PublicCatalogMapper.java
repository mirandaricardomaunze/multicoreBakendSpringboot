package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.publicapi.dto.PublicProductDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicStockDTO;
import mz.multicore.erp.modules.units.service.ProductUnitService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Entities to public API DTOs (SPEC §3.3.1): only sale data; the VAT-inclusive price uses the sales documents' math. */
final class PublicCatalogMapper {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PublicCatalogMapper() {
    }

    static PublicProductDTO toProduct(Product product) {
        BigDecimal rate = product.effectiveTaxRate();
        BigDecimal priceWithVat = LineCalculator.compute(product.getUnitPrice(), BigDecimal.ONE, BigDecimal.ZERO, rate)
                .total();
        LocalDateTime changed = lastChange(product);
        return new PublicProductDTO(
                product.getSku(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getBarcode(),
                ProductUnitService.baseUnitCodeOf(product),
                rate.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP),
                priceWithVat,
                changed == null ? null : changed.atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    static PublicStockDTO toStock(Stock stock, BigDecimal reserved) {
        BigDecimal quantity = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
        return new PublicStockDTO(
                stock.getProduct().getSku(),
                stock.getWarehouse().getWarehouseNumber(),
                stock.getWarehouse().getName(),
                quantity.subtract(reserved).max(BigDecimal.ZERO));
    }

    /** Last change of the product: its update, or its creation when it was never updated. */
    static LocalDateTime lastChange(Product product) {
        return product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt();
    }
}
