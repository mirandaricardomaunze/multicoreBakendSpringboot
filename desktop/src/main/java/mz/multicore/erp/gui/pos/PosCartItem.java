package mz.multicore.erp.gui.pos;

import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import java.math.BigDecimal;

/**
 * Item do carrinho do ponto de venda com cálculo dinâmico de impostos e totais.
 */
public class PosCartItem {
    public ProductDTO product;
    public BigDecimal qty;
    public BigDecimal discount;
    public String batch;
    public String serial;
    public String note;

    public PosCartItem(ProductDTO product, BigDecimal qty, BigDecimal discount, String batch, String serial) {
        this.product = product;
        this.qty = qty;
        this.discount = discount;
        this.batch = batch;
        this.serial = serial;
    }

    public LineCalculator.LineAmounts amounts() {
        BigDecimal rate = product.taxRate() != null ? product.taxRate() : TaxRates.STANDARD_VAT;
        return LineCalculator.compute(product.unitPrice(), qty, discount, rate);
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
