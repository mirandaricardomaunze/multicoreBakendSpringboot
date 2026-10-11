package mz.multicore.erp.modules.fleet.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_fuel_logs")
@Getter
@Setter
public class FuelLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "log_timestamp", nullable = false)
    private LocalDateTime logTimestamp = LocalDateTime.now();

    @Column(name = "odometer_km", nullable = false, precision = 15, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "liters_filled", nullable = false, precision = 12, scale = 2)
    private BigDecimal litersFilled;

    @Column(name = "price_per_liter", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerLiter;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "gas_station_name", length = 150)
    private String gasStationName;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "distance_traveled_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal distanceTraveledSinceLastFillKm = BigDecimal.ZERO;

    @Column(name = "consumption_rate_km_per_liter", nullable = false, precision = 10, scale = 2)
    private BigDecimal consumptionRateKmPerLiter = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
