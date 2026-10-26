package mz.multicore.erp.modules.fleet.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fleet_manifests", uniqueConstraints = @UniqueConstraint(
        name = "uk_company_manifest_number", columnNames = {"company_id", "manifest_number"}))
@Getter
@Setter
public class FleetManifest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "manifest_number", nullable = false, length = 50)
    private String manifestNumber;

    @Column(name = "manifest_date", nullable = false)
    private LocalDateTime manifestDate = LocalDateTime.now();

    @Column(name = "status", nullable = false, length = 30)
    private String status = "IN_TRANSIT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "vehicle_plate", nullable = false, length = 20)
    private String vehiclePlate;

    @Column(name = "vehicle_model", length = 100)
    private String vehicleModel;

    @Column(name = "driver_name", nullable = false, length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 50)
    private String driverPhone;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(name = "departure_time", length = 50)
    private String departureTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_stops", nullable = false)
    private int totalStops = 0;

    @Column(name = "total_boxes", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBoxes = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_packages", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPackages = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_amount_to_collect", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountToCollect = BigDecimal.ZERO;

    @OneToMany(mappedBy = "manifest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<FleetManifestStop> stops = new ArrayList<>();

    public void addStop(FleetManifestStop stop) {
        stops.add(stop);
        stop.setManifest(this);
    }
}
