package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.math.BigDecimal;

/**
 * Efeito de um documento CI numa linha de compra (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4), em moeda base:
 * quota repartida {@code C}, quantidade recebida {@code Q}, stock do artigo no armazém {@code S} no momento da
 * confirmação, parte que subiu o custo médio, parte já vendida que foi a CMV (6101) e o custo médio do artigo antes e
 * depois. Fotografia: não muda quando o stock ou o custo mudam depois.
 */
@Entity
@Table(name = "landed_cost_allocations")
@Getter
@Setter
public class LandedCostAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_line_id", nullable = false)
    private PurchaseLine purchaseLine;

    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal receivedQuantity;

    @Column(name = "stock_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockQuantity;

    @Column(name = "share_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal shareAmount;

    @Column(name = "stock_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockAmount;

    @Column(name = "sold_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal soldAmount;

    @Column(name = "unit_cost_before", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCostBefore;

    @Column(name = "unit_cost_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCostAfter;
}
