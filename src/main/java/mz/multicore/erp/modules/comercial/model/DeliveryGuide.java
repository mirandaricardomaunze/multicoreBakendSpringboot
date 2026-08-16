package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Guia de Remessa ao cliente — documento que acompanha a mercadoria expedida a partir de uma
 * encomenda. Dá saída ao stock (SALE) apenas na aprovação. Numeração série {@code GR}, única por
 * empresa. Ver docs/GUIA_REMESSA_ENCOMENDA_SPEC.md.
 */
@Entity
@Table(name = "delivery_guides", uniqueConstraints = @UniqueConstraint(
        name = "uk_delivery_guides_company_number", columnNames = {"company_id", "guide_number"}))
@Getter
@Setter
public class DeliveryGuide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_number", nullable = false)
    private String guideNumber;

    @Column(name = "guide_date", nullable = false)
    private LocalDateTime guideDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Encomenda de origem (a guia expede exactamente as suas linhas). */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Rótulo do comprador quando a encomenda é de "Consumidor Final" (sem cliente registado). */
    @Column(name = "walk_in_name", length = 120)
    private String walkInName;

    /** Armazém de onde a mercadoria sai (o mesmo da encomenda). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryGuideStatus status = DeliveryGuideStatus.PENDING_APPROVAL;

    // ─── Transporte (livre; impresso na guia) ───────────────────────────────
    @Column(name = "responsible", length = 120)
    private String responsible;

    @Column(name = "vehicle", length = 120)
    private String vehicle;

    @Column(name = "notes", length = 500)
    private String notes;

    // ─── Decisão (aprovação / rejeição) ─────────────────────────────────────
    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryGuideLine> lines = new ArrayList<>();

    public void addLine(DeliveryGuideLine line) {
        lines.add(line);
        line.setGuide(this);
    }
}
