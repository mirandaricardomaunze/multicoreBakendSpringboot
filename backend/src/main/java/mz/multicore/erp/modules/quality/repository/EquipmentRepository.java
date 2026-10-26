package mz.multicore.erp.modules.quality.repository;

import mz.multicore.erp.modules.quality.model.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(Long companyId, String code);

    List<Equipment> findByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    @Query("SELECT e FROM Equipment e WHERE e.company.id = :companyId AND " +
            "(:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Equipment> searchEquipments(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable
    );

    long countByCompanyId(Long companyId);
}
