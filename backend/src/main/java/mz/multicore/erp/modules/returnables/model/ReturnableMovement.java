package mz.multicore.erp.modules.returnables.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;

/** One line of the returnables account of a client. The balance only changes through these movements. */
@Entity
@Table(name = "returnable_movements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_returnable_movement_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class ReturnableMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ReturnableItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private ReturnableMovementType type;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "reference", length = 80)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;
}
