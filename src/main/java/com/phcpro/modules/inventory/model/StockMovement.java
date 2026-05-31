package com.phcpro.modules.inventory.model;

import com.phcpro.modules.comercial.model.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate = LocalDateTime.now();

    @Column(name = "movement_type", nullable = false)
    private String movementType; // PURCHASE, SALE, TRANSFER, CANCELLATION, ADJUSTMENT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity; // Positive for entry, negative for exit

    @Column(name = "batch_number")
    private String batchNumber; // Lote (denormalizado para histórico)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ProductBatch batch; // referência ao lote (FEFO/rastreabilidade)

    @Column(name = "serial_number")
    private String serialNumber; // Série

    @Column(name = "description")
    private String description;
}
