package mz.multicore.erp.modules.consignment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

@Entity
@Table(name = "consignment_lines")
@Getter
@Setter
public class ConsignmentLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private ConsignmentDelivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "delivered_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal delivered;

    @Column(name = "returned_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal returned = BigDecimal.ZERO;

    @Column(name = "damaged_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal damaged = BigDecimal.ZERO;

    public BigDecimal sold() {
        return delivered.subtract(returned).subtract(damaged).max(BigDecimal.ZERO);
    }
}
