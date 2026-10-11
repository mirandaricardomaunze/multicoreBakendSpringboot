package mz.multicore.erp.modules.fleet.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fleet_vehicles", uniqueConstraints = @UniqueConstraint(
        name = "uk_company_license_plate", columnNames = {"company_id", "license_plate"}))
@Getter
@Setter
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "make", nullable = false, length = 50)
    private String make;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "manufacturing_year", nullable = false)
    private int manufacturingYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 30)
    private FuelType fuelType = FuelType.DIESEL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VehicleStatus status = VehicleStatus.OPERATIONAL;

    @Column(name = "current_odometer_km", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentOdometerKm = BigDecimal.ZERO;

    @Column(name = "assigned_driver_name", length = 150)
    private String assignedDriverName;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Column(name = "inspection_expiry_date")
    private LocalDate inspectionExpiryDate;

    @Column(name = "avg_consumption_km_per_liter", nullable = false, precision = 10, scale = 2)
    private BigDecimal averageFuelConsumptionKmPerLiter = BigDecimal.ZERO;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FuelLog> fuelLogs = new ArrayList<>();
}
