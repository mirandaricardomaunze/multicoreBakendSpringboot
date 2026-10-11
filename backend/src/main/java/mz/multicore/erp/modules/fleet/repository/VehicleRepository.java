package mz.multicore.erp.modules.fleet.repository;

import mz.multicore.erp.modules.fleet.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCompanyIdOrderByLicensePlateAsc(Long companyId);
    Optional<Vehicle> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndLicensePlate(Long companyId, String licensePlate);
}
