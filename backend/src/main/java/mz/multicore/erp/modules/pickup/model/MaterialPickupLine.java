package mz.multicore.erp.modules.pickup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;

import java.math.BigDecimal;

/** Quantity of one invoice line handed over on a slip, in the product base unit. */
@Entity
@Table(name = "material_pickup_lines")
@Getter
@Setter
public class MaterialPickupLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_id", nullable = false)
    private MaterialPickup pickup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_line_id", nullable = false)
    private InvoiceLine invoiceLine;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
}
