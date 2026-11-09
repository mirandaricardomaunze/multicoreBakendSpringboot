package mz.multicore.erp.modules.weighbridge.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Truck weighing ticket (SA-C18, {@code docs/BASCULA_CAMIAO_SPEC.md}): two weighings and the net weight between them. */
@Entity
@Table(name = "weighing_tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weighing_ticket_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class WeighingTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "number", nullable = false, length = 40)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WeighingStatus status = WeighingStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private WeighingDirection direction;

    @Column(name = "plate", nullable = false, length = 20)
    private String plate;

    @Column(name = "driver", length = 120)
    private String driver;

    @Column(name = "counterparty", length = 160)
    private String counterparty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "reference", length = 80)
    private String reference;

    @Column(name = "first_weight", nullable = false, precision = 14, scale = 3)
    private BigDecimal firstWeight;

    @Column(name = "first_at", nullable = false)
    private LocalDateTime firstAt;

    @Column(name = "second_weight", precision = 14, scale = 3)
    private BigDecimal secondWeight;

    @Column(name = "second_at")
    private LocalDateTime secondAt;

    @Column(name = "net_weight", precision = 14, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "manual_weight", nullable = false)
    private boolean manual;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;
}
