package mz.multicore.erp.gui.pos;

import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import java.math.BigDecimal;

/**
 * Item do carrinho do ponto de venda com cálculo dinâmico de impostos e totais.
 * Suporta preço unitário personalizado/cotado quando originado de cotação ou tabela especial.
 */
public class PosCartItem {
    public ProductDTO product;
    public BigDecimal qty;
    public BigDecimal discount;
    public String batch;
    public String serial;
    public String note;
    public BigDecimal customUnitPrice;
    /** Weight that came from the scale (capture or scale label); {@code null} when nothing was weighed. */
    public BigDecimal scaleWeight;

    /** Where the quantity of a product sold by weight came from, for the anti-fraud report; {@code null} otherwise. */
    public String weightSource() {
        if (product == null || !"WEIGHT".equals(product.saleType())) return null;
        boolean fromScale = scaleWeight != null && qty != null && scaleWeight.compareTo(qty) == 0;
        return fromScale ? mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest.WEIGHT_FROM_SCALE
                : mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest.WEIGHT_TYPED;
    }

    public PosCartItem(ProductDTO product, BigDecimal qty, BigDecimal discount, String batch, String serial) {
        this(product, qty, discount, batch, serial, null);
    }

    public PosCartItem(ProductDTO product, BigDecimal qty, BigDecimal discount, String batch, String serial, BigDecimal customUnitPrice) {
        this.product = product;
        this.qty = qty;
        this.discount = discount;
        this.batch = batch;
        this.serial = serial;
        this.customUnitPrice = customUnitPrice;
    }

    public BigDecimal getEffectiveUnitPrice() {
        return (customUnitPrice != null && customUnitPrice.compareTo(BigDecimal.ZERO) > 0)
                ? customUnitPrice
                : (product != null ? product.unitPrice() : BigDecimal.ZERO);
    }

    public LineCalculator.LineAmounts amounts() {
        BigDecimal rate = (product != null && product.taxRate() != null) ? product.taxRate() : TaxRates.STANDARD_VAT;
        return LineCalculator.compute(getEffectiveUnitPrice(), qty, discount, rate);
    }

    public BigDecimal getSubtotal() {
        return amounts().net();
    }

    public BigDecimal getTax() {
        return amounts().tax();
    }

    public BigDecimal getTotal() {
        return amounts().total();
    }
}
