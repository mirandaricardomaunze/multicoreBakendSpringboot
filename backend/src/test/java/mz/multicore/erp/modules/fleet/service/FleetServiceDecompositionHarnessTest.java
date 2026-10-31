package mz.multicore.erp.modules.fleet.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/fleet/service/FleetService.java");
    private static final Path VEHICLE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/fleet/service/FleetVehicleManagementService.java");
    private static final Path FUEL_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/fleet/service/FleetFuelLogService.java");

    @Test
    void fleetServiceDelegatesToVehicleAndFuelLogCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String vehicleSource = Files.readString(VEHICLE_FILE);
        String fuelSource = Files.readString(FUEL_FILE);

        assertTrue(serviceSource.contains("vehicleManagementService.createVehicle("));
        assertTrue(serviceSource.contains("fuelLogService.recordFuel("));
        assertFalse(serviceSource.contains("new FuelLog()"));

        assertTrue(vehicleSource.contains("existsByCompanyIdAndLicensePlate("));
        assertTrue(fuelSource.contains("new FleetFuelLoggedEvent("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int vehicleLines = Files.readAllLines(VEHICLE_FILE).size();
        int fuelLines = Files.readAllLines(FUEL_FILE).size();

        assertTrue(serviceLines <= 95, "FleetService deve ter <= 95 linhas, mas tem " + serviceLines);
        assertTrue(vehicleLines <= 105, "FleetVehicleManagementService deve ter <= 105 linhas, mas tem " + vehicleLines);
        assertTrue(fuelLines <= 125, "FleetFuelLogService deve ter <= 125 linhas, mas tem " + fuelLines);
    }
}
