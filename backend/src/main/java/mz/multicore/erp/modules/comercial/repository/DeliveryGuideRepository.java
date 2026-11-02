package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryGuideRepository extends JpaRepository<DeliveryGuide, Long> {

    List<DeliveryGuide> findByCompanyId(Long companyId);

    Optional<DeliveryGuide> findByCompanyIdAndInvoiceId(Long companyId, Long invoiceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from DeliveryGuide g where g.id = :id and g.company.id = :companyId")
    Optional<DeliveryGuide> findForUpdate(@Param("id") Long id, @Param("companyId") Long companyId);

    /** Carrega a guia com as linhas (para decisão/impressão), garantindo o escopo da empresa. */
    @Query("select distinct g from DeliveryGuide g left join fetch g.lines "
            + "where g.id = :id and g.company.id = :companyId")
    Optional<DeliveryGuide> findByIdWithLinesAndCompanyId(@Param("id") Long id,
                                                          @Param("companyId") Long companyId);
}
