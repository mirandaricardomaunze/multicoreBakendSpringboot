package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.DeliveryGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryGuideRepository extends JpaRepository<DeliveryGuide, Long> {

    List<DeliveryGuide> findByCompanyId(Long companyId);

    /** Carrega a guia com as linhas (para decisão/impressão), garantindo o escopo da empresa. */
    @Query("select distinct g from DeliveryGuide g left join fetch g.lines "
            + "where g.id = :id and g.company.id = :companyId")
    Optional<DeliveryGuide> findByIdWithLinesAndCompanyId(@Param("id") Long id,
                                                          @Param("companyId") Long companyId);
}
