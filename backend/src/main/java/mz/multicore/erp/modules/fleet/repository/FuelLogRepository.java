package mz.multicore.erp.modules.fleet.repository;

import mz.multicore.erp.modules.fleet.model.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {
    List<FuelLog> findByVehicleIdOrderByLogTimestampDesc(Long vehicleId);
    Optional<FuelLog> findTopByVehicleIdOrderByLogTimestampDesc(Long vehicleId);
}
