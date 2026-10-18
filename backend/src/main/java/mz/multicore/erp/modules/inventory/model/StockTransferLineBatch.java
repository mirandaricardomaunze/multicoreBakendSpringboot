package mz.multicore.erp.modules.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parcela de lote de uma linha de transferência que saiu da origem e está em trânsito.
 * Registada no despacho e creditada no destino na recepção.
 */
@Entity
@Table(name = "stock_transfer_line_batches")
@Getter
@Setter
public class StockTransferLineBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_line_id", nullable = false)
    private StockTransferLine line;

    @Column(name = "batch_number", nullable = false, length = 120)
    private String batchNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;
}
