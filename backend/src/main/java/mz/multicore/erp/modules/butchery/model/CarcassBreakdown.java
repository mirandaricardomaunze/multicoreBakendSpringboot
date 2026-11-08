package mz.multicore.erp.modules.butchery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A carcass cut into sellable pieces (F3, {@code docs/DESMANCHA_SPEC.md}). Posting moves stock at once: the
 * carcass leaves and every cut enters with its share of the cost. Weight not obtained is the loss.
 */
@Entity
@Table(name = "carcass_breakdowns", uniqueConstraints = {
        @UniqueConstraint(name = "uk_carcass_breakdown_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class CarcassBreakdown extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "number", nullable = false, length = 40)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private BreakdownTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_product_id", nullable = false)
    private Product sourceProduct;

    @Column(name = "input_kg", nullable = false, precision = 14, scale = 3)
    private BigDecimal inputKg;

    @Column(name = "input_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal inputCost;

    @Column(name = "output_kg", nullable = false, precision = 14, scale = 3)
    private BigDecimal outputKg;

    @Column(name = "loss_tolerance_pct", precision = 6, scale = 2)
    private BigDecimal lossTolerancePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CarcassBreakdownStatus status = CarcassBreakdownStatus.POSTED;

    @Column(name = "reference", length = 120)
    private String reference;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;

    @OneToMany(mappedBy = "breakdown", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CarcassBreakdownLine> lines = new ArrayList<>();
}
