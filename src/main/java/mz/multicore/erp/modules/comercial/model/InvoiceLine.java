package mz.multicore.erp.modules.comercial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_lines")
@Getter
@Setter
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false)
    private BigDecimal taxRate; // e.g. 0.23 for 23%

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO; // e.g. 10.00 for 10%

    @Column(name = "batch_number")
    private String batchNumber; // Lote

    @Column(name = "serial_number")
    private String serialNumber; // Série

    /**
     * Custo unitário <b>no acto da venda</b> — fotografia, não referência.
     *
     * <p>A margem lia o preço de compra <i>actual</i> do produto: bastava o fornecedor mudar de
     * preço para a margem de vendas antigas mudar sozinha, e o histórico deixava de bater certo
     * com o que aconteceu. Nulo nas linhas anteriores à V37 — ver {@link #effectiveUnitCost()}.
     */
    @Column(name = "unit_cost", precision = 14, scale = 2)
    private BigDecimal unitCost;

    /**
     * Custo unitário a usar no cálculo de margem: o gravado na linha, senão o preço de compra
     * actual do produto (mesmo padrão de {@code Product.effectiveTaxRate()}).
     *
     * <p>O recurso ao preço actual existe só para as linhas anteriores à V37, que não têm
     * fotografia. Para essas, a margem continua a ser uma <b>estimativa</b>.
     */
    public BigDecimal effectiveUnitCost() {
        if (unitCost != null) return unitCost;
        if (product == null || product.getPurchasePrice() == null) return BigDecimal.ZERO;
        return product.getPurchasePrice();
    }

    /** Custo total da linha (custo unitário × quantidade), nunca nulo. */
    public BigDecimal lineCost() {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        return effectiveUnitCost().multiply(qty);
    }
}
