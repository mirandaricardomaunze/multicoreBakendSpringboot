package mz.multicore.erp.modules.pricing.repository;

import mz.multicore.erp.modules.pricing.model.ClientPriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientPriceListRepository extends JpaRepository<ClientPriceList, Long> {

    @Query("select a from ClientPriceList a join fetch a.priceList where a.company.id = :companyId "
            + "and a.client.id = :clientId")
    Optional<ClientPriceList> findAssignment(@Param("companyId") Long companyId, @Param("clientId") Long clientId);

    @Query("select a from ClientPriceList a join fetch a.client c where a.priceList.id = :priceListId order by c.name")
    List<ClientPriceList> findByPriceListIdWithClient(@Param("priceListId") Long priceListId);

    long countByPriceListId(Long priceListId);
}
