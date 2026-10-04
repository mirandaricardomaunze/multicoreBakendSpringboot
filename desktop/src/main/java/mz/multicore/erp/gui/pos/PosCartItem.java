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
