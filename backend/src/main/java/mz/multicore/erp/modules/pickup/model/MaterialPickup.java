package mz.multicore.erp.modules.pickup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Pickup slip (LV series). It never moves stock: the invoice already did. */
@Entity
@Table(name = "material_pickups", uniqueConstraints = {
        @UniqueConstraint(name = "uk_material_pickup_number", columnNames = {"company_id", "pickup_number"}),
        @UniqueConstraint(name = "uk_material_pickup_request", columnNames = {"company_id", "request_key"})
})
@Getter
@Setter
public class MaterialPickup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private PickupAccount account;

    @Column(name = "pickup_number", nullable = false, length = 40)
    private String number;

    @Column(name = "picked_at", nullable = false)
    private LocalDateTime pickedAt;

    @Column(name = "collector_name", nullable = false, length = 150)
    private String collectorName;

    @Column(name = "collector_document", length = 60)
    private String collectorDocument;

    @Column(name = "vehicle_plate", length = 30)
    private String vehiclePlate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "request_key", length = 80)
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaterialPickupStatus status = MaterialPickupStatus.ISSUED;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "pickup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<MaterialPickupLine> lines = new ArrayList<>();

    public void addLine(MaterialPickupLine line) {
        line.setPickup(this);
        lines.add(line);
    }
}
