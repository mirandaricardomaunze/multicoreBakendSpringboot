package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseLineRepository extends JpaRepository<PurchaseLine, Long> {

    @Query("""
            select pl from PurchaseLine pl
            join fetch pl.purchase p
            join fetch p.supplier s
            where p.company.id = :companyId
              and p.status <> 'CANCELLED'
            order by p.purchaseDate desc
            """)
    List<PurchaseLine> findRecentPurchasesByCompany(@Param("companyId") Long companyId);
}
