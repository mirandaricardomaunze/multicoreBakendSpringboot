package com.phcpro.modules.inventory.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.comercial.model.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_batches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "warehouse_id", "batch_number"})
}, indexes = {
        @Index(name = "idx_batch_fefo", columnList = "product_id, warehouse_id, expiration_date")
})
@Getter
@Setter
public class ProductBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate = LocalDate.now();

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;
}
