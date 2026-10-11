package mz.multicore.erp.modules.dashboard.dto;

import java.math.BigDecimal;

/**
 * DTO consolidado do Pulso Operacional & Industrial 360° para o Dashboard.
 * Agrega métricas estratégicas em tempo real dos 4 módulos empresariais:
 * Manufatura, Frotas, Faturação Recorrente e Ativos Fixos.
 */
public record OperationalPulseDTO(
        ManufacturingPulse manufacturing,
        FleetPulse fleet,
        RecurringPulse recurring,
        AssetPulse assets
) {
    public record ManufacturingPulse(
            long plannedOrders,
            long inProgressOrders,
            long completedOrders,
            BigDecimal totalCipValue
    ) {
        public static ManufacturingPulse empty() {
            return new ManufacturingPulse(0, 0, 0, BigDecimal.ZERO);
        }
    }

    public record FleetPulse(
            long activeVehicles,
            long totalVehicles,
            BigDecimal monthlyFuelLiters,
            BigDecimal monthlyFuelCost,
            BigDecimal avgEfficiencyKmPerLiter,
            long maintenanceAlerts
    ) {
        public static FleetPulse empty() {
            return new FleetPulse(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
    }

    public record RecurringPulse(
            long activeContracts,
            BigDecimal monthlyRecurringRevenue,
            long contractsDueBilling
    ) {
        public static RecurringPulse empty() {
            return new RecurringPulse(0, BigDecimal.ZERO, 0);
        }
    }

    public record AssetPulse(
            long totalAssets,
            BigDecimal totalAcquisitionCost,
            BigDecimal accumulatedDepreciation,
            BigDecimal netBookValue
    ) {
        public static AssetPulse empty() {
            return new AssetPulse(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public static OperationalPulseDTO empty() {
        return new OperationalPulseDTO(
                ManufacturingPulse.empty(),
                FleetPulse.empty(),
                RecurringPulse.empty(),
                AssetPulse.empty()
        );
    }
}
