package mz.multicore.erp.modules.units.repository;

import mz.multicore.erp.modules.units.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {

    Optional<UnitOfMeasure> findByCodeAndCompanyIsNull(String code);

    Optional<UnitOfMeasure> findByCodeAndCompanyId(String code, Long companyId);

    /** Active system and company units, ordered by dimension and code. */
    @Query("select u from UnitOfMeasure u where u.active = true and (u.company is null or u.company.id = :companyId) "
            + "order by u.dimension, u.systemUnit desc, u.code")
    List<UnitOfMeasure> findVisible(@Param("companyId") Long companyId);
}
