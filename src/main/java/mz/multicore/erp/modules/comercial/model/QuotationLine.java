package mz.multicore.erp.modules.comercial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Linha da cotação — a fotografia do preço proposto. É esta linha, e não o catálogo de hoje, que a
 * encomenda gerada na conversão herda (docs/COTACAO_SPEC.md §3).
 *
 * <p>Sem lote nem número de série, ao contrário da linha da encomenda: uma proposta não promete
 * lote nenhum. O lote é decidido por FEFO quando o stock se mover, que é na faturação.
 */
@Entity
@Table(name = "quotation_lines")
@Getter
@Setter
public class QuotationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false)
    private BigDecimal taxRate;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;
}
