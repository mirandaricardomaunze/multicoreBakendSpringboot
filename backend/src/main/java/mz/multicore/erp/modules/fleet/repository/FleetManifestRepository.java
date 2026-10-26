package mz.multicore.erp.modules.fleet.repository;

import mz.multicore.erp.modules.fleet.model.FleetManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FleetManifestRepository extends JpaRepository<FleetManifest, Long> {

    @Query("SELECT m FROM FleetManifest m WHERE m.company.id = :companyId ORDER BY m.manifestDate DESC, m.id DESC")
    List<FleetManifest> findByCompanyIdOrderByManifestDateDesc(@Param("companyId") Long companyId);

    @Query("SELECT m FROM FleetManifest m LEFT JOIN FETCH m.stops WHERE m.id = :id AND m.company.id = :companyId")
    Optional<FleetManifest> findByIdAndCompanyIdWithStops(@Param("id") Long id, @Param("companyId") Long companyId);

    Optional<FleetManifest> findByIdAndCompanyId(Long id, Long companyId);
}
